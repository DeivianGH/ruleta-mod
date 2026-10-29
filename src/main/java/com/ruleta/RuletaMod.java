package com.ruleta;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
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
    public static final Item HEART = new HeartItem();
    public static final Item[] TOTEMS = new Item[10];
    public static final String[] TOTEM_NAMES = {"Amatista", "Bosque", "Coral", "Ceniza", "Tempestad", "Sol", "Delfin Dorado", "Delfin Solar", "Delfin Marino", "Delfin Abisal"};
    private static final Random RNG = new Random();

    private record Pending(RuletaEntry entry, String title, String custom, int applyAt) {}
    private record Active(RuletaEntry entry, int endAt) {}

    private static Pending pending;
    private static Item pendingPrize;
    private static int pendingPrizeAt;
    private static final List<Active> active = new ArrayList<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, Identifier.of(ID, "ruleta_admin"), WAND);
        Registry.register(Registries.ITEM, Identifier.of(ID, "corazon"), HEART);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> { e.add(WAND); e.add(HEART); });
        for (int i = 0; i < TOTEMS.length; i++) {
            TOTEMS[i] = new CustomTotemItem(i);
            Registry.register(Registries.ITEM, Identifier.of(ID, "totem_" + (i + 1)), TOTEMS[i]);
        }
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> {
            for (Item totem : TOTEMS) e.add(totem);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) return true;
            for (Hand hand : Hand.values()) {
                ItemStack stack = player.getStackInHand(hand);
                for (int i = 0; i < TOTEMS.length; i++) {
                    if (stack.isOf(TOTEMS[i])) {
                        stack.decrement(1);
                        player.setHealth(1.0f);
                        player.clearFire();
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
                        switch (i) {
                            case 0 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 0));
                            case 1 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 0));
                            case 2 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 600, 1));
                            case 3 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0));
                            case 4 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 0));
                            case 5 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 400, 0));
                            case 6 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 600, 0));
                            case 7 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 600, 0));
                            case 8 -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 600, 0));
                            default -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 600, 0));
                        }
                        if (Catalog.FLAGS.contains("deivil")) {
                            broadcast(player.getServer(), Text.literal(player.getName().getString() + " consumió el Tótem de " + TOTEM_NAMES[i])
                                    .formatted(Formatting.RED, Formatting.BOLD));
                        } else {
                            broadcast(player.getServer(), Text.literal(player.getName().getString() + " consumió el Tótem de " + TOTEM_NAMES[i])
                                    .formatted(Formatting.GOLD, Formatting.BOLD));
                        }
                        return false;
                    }
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity dead)) return;
            MinecraftServer server = dead.getServer();
            String name = dead.getName().getString();
            broadcast(server, Text.literal(name + " MURIÓ").formatted(Formatting.RED, Formatting.BOLD));
            Payloads.Death payload = new Payloads.Death(name, 80);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) ServerPlayNetworking.send(player, payload);
        });

        // Paquetes
        PayloadTypeRegistry.playS2C().register(Payloads.Open.ID, Payloads.Open.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Spin.ID, Payloads.Spin.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Death.ID, Payloads.Death.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.Atmosphere.ID, Payloads.Atmosphere.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.Request.ID, Payloads.Request.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.Control.ID, Payloads.Control.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Request.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            if (!player.hasPermissionLevel(2)) {
                player.sendMessage(Text.literal("No tienes permiso.").formatted(Formatting.RED), false);
                return;
            }
            if (!start(player.getServer(), payload.color(), payload.option(), payload.title(), payload.custom())) {
                player.sendMessage(Text.literal("La ruleta ya esta girando.").formatted(Formatting.RED), false);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.Control.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            if (!player.hasPermissionLevel(2)) return;
            MinecraftServer server = player.getServer();
            if (payload.action() == 0) {
                if (pending != null) pending = null;
                pendingPrize = null;
                for (Active a : active) a.entry().stop().accept(server);
                active.clear();
                Catalog.FLAGS.remove("deivil");
                sendAtmosphere(server, false);
                broadcast(server, Text.literal("[RULETA] Detenida por un administrador.").formatted(Formatting.RED, Formatting.BOLD));
            } else if (payload.action() == 1) {
                RuletaColor color = RuletaColor.byId(payload.color());
                String title = payload.title().isBlank() ? color.category : payload.title().substring(0, Math.min(64, payload.title().length()));
                String message = payload.text().substring(0, Math.min(240, payload.text().length()));
                Formatting messageColor = Catalog.FLAGS.contains("deivil") ? Formatting.RED : color.fmt;
                broadcast(server, Text.literal("[" + title + "] ").formatted(messageColor, Formatting.BOLD)
                        .append(Text.literal(message).formatted(messageColor, Formatting.BOLD)));
            } else if (payload.action() == 2) {
                for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList())
                    target.damage(target.getDamageSources().generic(), 100_000.0f);
                broadcast(server, Text.literal("[EVENTO] Botón mortal activado: 100.000 de daño.")
                        .formatted(Catalog.FLAGS.contains("deivil") ? Formatting.RED : Formatting.DARK_RED, Formatting.BOLD));
            } else if (payload.action() == 3) {
                startObjectRoulette(server);
            }
        });

        // Comando de respaldo: /ruleta <roja|naranja|amarilla|verde|cian|morada|rosada|random>
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
                        boolean ok = start(ctx.getSource().getServer(), c == null ? -1 : c.ordinal(), -1, "", "");
                        if (!ok) ctx.getSource().sendError(Text.literal("La ruleta ya esta girando."));
                        return ok ? 1 : 0;
                    }))));

        // --- Dificultad: puertas trampa (1.000.000 de dano, solo el totem salva) ---
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || player.isSpectator()) return ActionResult.PASS;
            if (!Catalog.FLAGS.contains("doordeath")) return ActionResult.PASS;
            BlockState st = world.getBlockState(hit.getBlockPos());
            if (st.getBlock() instanceof DoorBlock && !st.isOf(Blocks.IRON_DOOR)) {
                player.sendMessage(Text.literal("La puerta era una trampa!").formatted(
                        Catalog.FLAGS.contains("deivil") ? Formatting.RED : Formatting.DARK_RED, Formatting.BOLD), true);
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
            pendingPrize = null;
            active.clear();
            Catalog.FLAGS.clear();
        });
    }

    /** Lanza la ruleta. Devuelve false si ya hay una girando. */
    public static boolean start(MinecraftServer server, int colorId, int option, String title, String custom) {
        if (pending != null || pendingPrize != null) return false;
        RuletaColor color = colorId < 0 ? RuletaColor.byId(RNG.nextInt(RuletaColor.values().length))
                                        : RuletaColor.byId(colorId);
        List<RuletaEntry> list = Catalog.forColor(color);
        if (list.isEmpty()) return false;
        RuletaEntry entry = (option < 0 || option >= list.size()) ? list.get(RNG.nextInt(list.size()))
                                                                  : list.get(option);

        String safeTitle = title == null ? "" : title.trim();
        String safeMessage = custom == null ? "" : custom.trim();
        pending = new Pending(entry, safeTitle.substring(0, Math.min(64, safeTitle.length())),
                safeMessage.substring(0, Math.min(240, safeMessage.length())), server.getTicks() + SPIN_TICKS);

        // Ruleta visible para TODOS los jugadores
        Payloads.Spin spin = new Payloads.Spin(color.ordinal(), entry.name(), SPIN_TICKS, "");
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, spin);
        }
        return true;
    }

    private static void startObjectRoulette(MinecraftServer server) {
        if (pending != null || pendingPrize != null) return;
        int pick = RNG.nextInt(TOTEMS.length + 1);
        pendingPrize = pick == TOTEMS.length ? HEART : TOTEMS[pick];
        pendingPrizeAt = server.getTicks() + SPIN_TICKS;
        String itemId = Registries.ITEM.getId(pendingPrize).toString();
        String name = pendingPrize == HEART ? "Corazón consumible" : "Tótem de " + TOTEM_NAMES[pick];
        Payloads.Spin spin = new Payloads.Spin(RuletaColor.AMARILLA.ordinal(), name, SPIN_TICKS, itemId);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) ServerPlayNetworking.send(player, spin);
    }

    private void tick(MinecraftServer server) {
        int now = server.getTicks();

        if (pending != null && now >= pending.applyAt()) {
            apply(server, pending);
            pending = null;
        }
        if (pendingPrize != null && now >= pendingPrizeAt) {
            String name = pendingPrize == HEART ? "Corazón consumible" : "Tótem de " + TOTEM_NAMES[((CustomTotemItem) pendingPrize).variant()];
            Formatting color = Catalog.FLAGS.contains("deivil") ? Formatting.RED : Formatting.YELLOW;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList())
                player.getInventory().offerOrDrop(new ItemStack(pendingPrize));
            broadcast(server, Text.literal("[RULETA DE OBJETOS] Resultado: " + name).formatted(color, Formatting.BOLD));
            pendingPrize = null;
        }

        Iterator<Active> it = active.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            if (now >= a.endAt()) {
                a.entry().stop().accept(server);
                RuletaColor color = a.entry().color();
                Formatting messageColor = Catalog.FLAGS.contains("deivil") || color == RuletaColor.ROJA ? Formatting.RED : color.fmt;
                broadcast(server, Text.literal("[" + color.category + "] Termino: " + a.entry().name()).formatted(messageColor, Formatting.BOLD));
                if (color == RuletaColor.ROJA) sendAtmosphere(server, false);
                it.remove();
            }
        }

        if (now % 40 == 0 && Catalog.FLAGS.contains("hunger")) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 60, 2, false, false));
        }
        if (now % 60 == 0 && Catalog.FLAGS.contains("deivil")) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                var world = player.getServerWorld();
                world.spawnParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1.0, player.getZ(), 5, 1.2, 1.0, 1.2, 0.015);
                world.playSound(null, player.getBlockPos(), SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.25f, 0.65f);
            }
        }
    }

    private void apply(MinecraftServer server, Pending p) {
        RuletaEntry e = p.entry();
        e.start().accept(server);
        if (e.color() == RuletaColor.ROJA) sendAtmosphere(server, true);

        String text = !p.custom().isBlank() ? p.custom() : e.chatText();
        String title = !p.title().isBlank() ? p.title() : e.name();
        Formatting color = Catalog.FLAGS.contains("deivil") || e.color() == RuletaColor.ROJA ? Formatting.RED : e.color().fmt;

        broadcast(server, Text.literal("[" + e.color().category + "] ").formatted(color, Formatting.BOLD)
                .append(Text.literal("\u25CF " + e.color().label + ": ").formatted(color, Formatting.BOLD))
                .append(Text.literal(title).formatted(color, Formatting.BOLD)));
        broadcast(server, Text.literal(text).formatted(color, Formatting.BOLD));

        if (e.durationTicks() > 0) {
            active.add(new Active(e, server.getTicks() + e.durationTicks()));
        }
    }

    private static void broadcast(MinecraftServer server, Text t) {
        server.getPlayerManager().broadcast(t, false);
    }

    private static void sendAtmosphere(MinecraftServer server, boolean active) {
        Payloads.Atmosphere payload = new Payloads.Atmosphere(active);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) ServerPlayNetworking.send(player, payload);
    }
}
