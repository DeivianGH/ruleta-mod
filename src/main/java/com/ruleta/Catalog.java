package com.ruleta;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Todo lo que puede salir en la ruleta. Agrega o quita entradas aqui. */
public final class Catalog {
    /** Banderas de dificultad activas (las leen los eventos de spawn, el tick y los mixins). */
    public static final Set<String> FLAGS = ConcurrentHashMap.newKeySet();
    private static final List<RuletaEntry> ALL = new ArrayList<>();
    private static final Consumer<MinecraftServer> NOOP = s -> {};
    private static final Random RNG = new Random();

    private static void add(RuletaColor c, String name, String text, int seconds,
                            Consumer<MinecraftServer> start, Consumer<MinecraftServer> stop) {
        ALL.add(new RuletaEntry(c, name, text, seconds * 20, start, stop));
    }

    static {
        // ---------- ROJA: cambios de dificultad (temporales) ----------
        add(RuletaColor.ROJA, "Creepers electrificados", "Los creepers que aparezcan estaran cargados.", 600,
                s -> FLAGS.add("charged"), s -> FLAGS.remove("charged"));
        add(RuletaColor.ROJA, "Mobs veloces", "Los monstruos nuevos tienen Velocidad II.", 600,
                s -> FLAGS.add("fast"), s -> FLAGS.remove("fast"));
        add(RuletaColor.ROJA, "Sin regeneracion natural", "Ya no regeneras vida comiendo.", 600,
                s -> s.getGameRules().get(GameRules.NATURAL_REGENERATION).set(false, s),
                s -> s.getGameRules().get(GameRules.NATURAL_REGENERATION).set(true, s));
        add(RuletaColor.ROJA, "Hambre constante", "Tu barra de hambre baja muy rapido.", 300,
                s -> FLAGS.add("hunger"), s -> FLAGS.remove("hunger"));
        add(RuletaColor.ROJA, "Puertas trampa",
                "Abrir una puerta te hace 1.000.000 de dano. Solo un totem (en la mano) te salva.", 300,
                s -> FLAGS.add("doordeath"), s -> FLAGS.remove("doordeath"));
        add(RuletaColor.ROJA, "Fuego eterno del Nether",
                "El fuego del Nether no se apaga solo ni a golpes. Solo el agua lo detiene.", 600,
                s -> FLAGS.add("eternalfire"), s -> FLAGS.remove("eternalfire"));

        // ---------- NARANJA: eventos ----------
        add(RuletaColor.NARANJA, "Tormenta electrica", "El cielo se oscurece...", 0,
                s -> s.getOverworld().setWeather(0, 6000, true, true), NOOP);
        add(RuletaColor.NARANJA, "Noche eterna", "Se hizo de noche de golpe.", 0,
                s -> s.getOverworld().setTimeOfDay(18000), NOOP);
        add(RuletaColor.NARANJA, "Invasion zombi", "Cada jugador tiene 5 zombis encima.", 0,
                s -> zombies(s, 5), NOOP);

        // ---------- AMARILLA: mensajes ----------
        add(RuletaColor.AMARILLA, "Mensaje del admin", "El admin tiene algo que decir.", 0, NOOP, NOOP);
        add(RuletaColor.AMARILLA, "Advertencia", "Cuidado, lo peor todavia no ha llegado.", 0, NOOP, NOOP);
        add(RuletaColor.AMARILLA, "Ultima hora", "Quedan pocos minutos, esfuerzense.", 0, NOOP, NOOP);

        // ---------- VERDE: premios ----------
        add(RuletaColor.VERDE, "Manzanas doradas", "Todos reciben 2 manzanas doradas.", 0,
                s -> giveAll(s, new ItemStack(Items.GOLDEN_APPLE, 2)), NOOP);
        add(RuletaColor.VERDE, "Comida gratis", "Todos reciben 16 filetes.", 0,
                s -> giveAll(s, new ItemStack(Items.COOKED_BEEF, 16)), NOOP);
        add(RuletaColor.VERDE, "Totem para todos", "Todos reciben un totem de la inmortalidad.", 0,
                s -> giveAll(s, new ItemStack(Items.TOTEM_OF_UNDYING, 1)), NOOP);

        // ---------- AZUL: clima y tiempo ----------
        add(RuletaColor.AZUL, "Cielo despejado", "Sale el sol.", 0,
                s -> s.getOverworld().setWeather(6000, 0, false, false), NOOP);
        add(RuletaColor.AZUL, "Amanecer", "Vuelve el dia.", 0,
                s -> s.getOverworld().setTimeOfDay(1000), NOOP);
        add(RuletaColor.AZUL, "Lluvia", "Empieza a llover.", 0,
                s -> s.getOverworld().setWeather(0, 6000, true, false), NOOP);

        // ---------- CIAN: bendiciones (efectos buenos 60 s) ----------
        add(RuletaColor.CIAN, "Velocidad", "Todos corren mas rapido por 60 segundos.", 0,
                s -> effectAll(s, StatusEffects.SPEED, 1200, 1), NOOP);
        add(RuletaColor.CIAN, "Super salto", "Todos saltan mucho mas por 60 segundos.", 0,
                s -> effectAll(s, StatusEffects.JUMP_BOOST, 1200, 3), NOOP);
        add(RuletaColor.CIAN, "Vision nocturna", "Todos ven en la oscuridad por 2 minutos.", 0,
                s -> effectAll(s, StatusEffects.NIGHT_VISION, 2400, 0), NOOP);

        // ---------- MORADA: maldiciones (efectos malos) ----------
        add(RuletaColor.MORADA, "Levitacion general", "Todos flotan durante 5 segundos.", 0,
                s -> effectAll(s, StatusEffects.LEVITATION, 100, 0), NOOP);
        add(RuletaColor.MORADA, "Ceguera", "Nadie ve nada durante 10 segundos.", 0,
                s -> effectAll(s, StatusEffects.BLINDNESS, 200, 0), NOOP);
        add(RuletaColor.MORADA, "Lentitud", "Todos se mueven muy lento por 30 segundos.", 0,
                s -> effectAll(s, StatusEffects.SLOWNESS, 600, 2), NOOP);

        // ---------- ROSADA: sorpresas ----------
        add(RuletaColor.ROSADA, "Curacion total", "Todos quedan con vida y hambre al maximo.", 0,
                Catalog::healAll, NOOP);
        add(RuletaColor.ROSADA, "Lluvia de experiencia", "Todos suben 5 niveles.", 0,
                s -> { for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) p.addExperienceLevels(5); }, NOOP);
    }

    public static List<RuletaEntry> forColor(RuletaColor c) {
        return ALL.stream().filter(e -> e.color() == c).toList();
    }

    private static void effectAll(MinecraftServer s, RegistryEntry<StatusEffect> eff, int ticks, int amp) {
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList())
            p.addStatusEffect(new StatusEffectInstance(eff, ticks, amp));
    }

    private static void giveAll(MinecraftServer s, ItemStack stack) {
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList())
            p.getInventory().offerOrDrop(stack.copy());
    }

    private static void healAll(MinecraftServer s) {
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.setHealth(p.getMaxHealth());
            p.getHungerManager().setFoodLevel(20);
            p.getHungerManager().setSaturationLevel(20f);
        }
    }

    private static void zombies(MinecraftServer s, int perPlayer) {
        for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            ServerWorld w = p.getServerWorld();
            for (int i = 0; i < perPlayer; i++) {
                ZombieEntity z = EntityType.ZOMBIE.create(w);
                if (z == null) continue;
                z.refreshPositionAndAngles(p.getX() + RNG.nextInt(9) - 4, p.getY(),
                        p.getZ() + RNG.nextInt(9) - 4, RNG.nextFloat() * 360f, 0f);
                w.spawnEntity(z);
            }
        }
    }
}
