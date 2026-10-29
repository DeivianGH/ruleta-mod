package com.ruleta;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RuletaMod implements ModInitializer {
    public static final String ID = "ruleta";
    public static final int SPIN_TICKS = 100; // 5 segundos de giro

    public static final Item WAND = new AdminWandItem();
    private static final Random RNG = new Random();

    private record Pending(RuletaEntry entry, String custom, int applyAt) {}
    private record Active(RuletaEntry entry, int endAt) {}

    private static Pending pending;
    private static final List<Active> active = new ArrayList<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, Identifier.of(ID, "ruleta_admin"), WAND);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(WAND));

        // Paquetes
        PayloadTypeRegistry.playS2C().register(Payloads.Open.ID, Payloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Spin.ID, Payloads.Spin.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.Request.ID, Payloads.Request.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Request.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            if (!player.hasPermissionLevel(2)) {
                player.sendMessage(Text.literal("No tienes permiso.").formatted(Formatting.RED), false);
                return;
            }
            if (!start(player.getServer(), payload.color(), payload.option(), payload.custom())) {
                player.sendMessage(Text.literal("La ruleta ya esta girando.").formatted(Formatting.RED), false);
            }
        });

        // Comando de respaldo: /ruleta <roja|naranja|amarilla|verde|azul|cian|morada|rosada|random>
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            dispatcher.register(CommandManager.literal("ruleta")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("color", StringArgumentType.word())
                    .suggests((c, b) -> {
                        List<String> names = new ArrayList<>(List.of("random"));
                        for (RuletaColor rc : RuletaColor.values()) names.add(rc.id());
                        return CommandSource.suggestMatching(names, b);
                    })
                    .executes(ctx -> {
                        String arg = StringArgumentType.getString(ctx, "color");
                        RuletaColor c = RuletaColor.byName(arg);
                        if (c == null && !arg.equalsIgnoreCase("random")) {
                            ctx.getSource().sendError(Text.literal("Color invalido."));
                            return 0;
                        }
                        boolean ok = start(ctx.getSource().getServer(), c == null ? -1 : c.ordinal(), -1, "");
                        if (!ok) ctx.getSource().sendError(Text.literal("La ruleta ya esta girando."));
                        return ok ? 1 : 0;
                    }))));

        // --- Dificultad: puertas trampa (1.000.000 de dano, solo el totem salva) ---
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || player.isSpectator()) return ActionResult.PASS;
            if (!Catalog.FLAGS.contains("doordeath")) return ActionResult.PASS;
            BlockState st = world.getBlockState(hit.getBlockPos());
            if (st.getBlock() instanceof DoorBlock && !st.isOf(Blocks.IRON_DOOR)) {
                player.sendMessage(Text.literal("La puerta era una trampa!").formatted(Formatting.DARK_RED), true);
                player.damage(world.getDamageSources().generic(), 1_000_000f);
            }
            return ActionResult.PASS;
        });

        // --- Dificultad: fuego eterno del Nether (no se quita a golpes) ---
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            if (!world.isClient && Catalog.FLAGS.contains("eternalfire") && world.getRegistryKey() == World.NETHER
                    && world.getBlockState(pos.offset(dir)).getBlock() instanceof AbstractFireBlock) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // --- ...solo el agua lo apaga: cubo de agua (la pocion arrojadiza de agua ya lo hace en vanilla) ---
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient && Catalog.FLAGS.contains("eternalfire") && world.getRegistryKey() == World.NETHER
                    && player.getStackInHand(hand).isOf(Items.WATER_BUCKET)) {
                HitResult hr = player.raycast(5.0, 0f, false);
                BlockPos c = (hr instanceof BlockHitResult b && hr.getType() == HitResult.Type.BLOCK)
                        ? b.getBlockPos().offset(b.getSide()) : player.getBlockPos();
                boolean any = false;
                for (BlockPos q : BlockPos.iterate(c.add(-3, -3, -3), c.add(3, 3, 3))) {
                    if (world.getBlockState(q).getBlock() instanceof AbstractFireBlock) {
                        world.removeBlock(q.toImmutable(), false);
                        any = true;
                    }
                }
                if (any) world.playSound(null, c, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 1f);
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        // Dificultad: creepers cargados y mobs veloces
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof CreeperEntity c && Catalog.FLAGS.contains("charged")) {
                NbtCompound n = new NbtCompound();
                c.writeNbt(n);
                n.putBoolean("powered", true);
                c.readNbt(n);
            }
            if (entity instanceof HostileEntity h && Catalog.FLAGS.contains("fast")) {
                h.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1_000_000, 1, false, false));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);

        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            pending = null;
            active.clear();
            Catalog.FLAGS.clear();
        });
    }

    /** Lanza la ruleta. Devuelve false si ya hay una girando. */
    public static boolean start(MinecraftServer server, int colorId, int option, String custom) {
        if (pending != null) return false;
        RuletaColor color = colorId < 0 ? RuletaColor.byId(RNG.nextInt(RuletaColor.values().length))
                                        : RuletaColor.byId(colorId);
        List<RuletaEntry> list = Catalog.forColor(color);
        if (list.isEmpty()) return false;
        RuletaEntry entry = (option < 0 || option >= list.size()) ? list.get(RNG.nextInt(list.size()))
                                                                  : list.get(option);

        pending = new Pending(entry, custom == null ? "" : custom.trim(), server.getTicks() + SPIN_TICKS);

        // Ruleta visible para TODOS los jugadores
        Payloads.Spin spin = new Payloads.Spin(color.ordinal(), entry.name(), SPIN_TICKS);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, spin);
        }
        return true;
    }

    private void tick(MinecraftServer server) {
        int now = server.getTicks();

        if (pending != null && now >= pending.applyAt()) {
            apply(server, pending);
            pending = null;
        }

        Iterator<Active> it = active.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            if (now >= a.endAt()) {
                a.entry().stop().accept(server);
                broadcast(server, Text.literal("[RULETA] ").formatted(Formatting.GOLD, Formatting.BOLD)
                        .append(Text.literal("Termino: " + a.entry().name()).formatted(Formatting.GRAY)));
                it.remove();
            }
        }

        if (now % 40 == 0 && Catalog.FLAGS.contains("hunger")) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 2, false, false));
        }
    }

    private void apply(MinecraftServer server, Pending p) {
        RuletaEntry e = p.entry();
        e.start().accept(server);

        String text = (e.color() == RuletaColor.AMARILLA && !p.custom().isBlank()) ? p.custom() : e.chatText();

        broadcast(server, Text.literal("[RULETA] ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("\u25CF ").formatted(e.color().fmt))
                .append(Text.literal(e.color().category + " (" + e.color().label + "): ").formatted(e.color().fmt))
                .append(Text.literal(e.name()).formatted(Formatting.WHITE, Formatting.BOLD)));
        broadcast(server, Text.literal(text).formatted(Formatting.GRAY));

        if (e.durationTicks() > 0) {
            active.add(new Active(e, server.getTicks() + e.durationTicks()));
        }
    }

    private static void broadcast(MinecraftServer server, Text t) {
        server.getPlayerManager().broadcast(t, false);
    }
}
