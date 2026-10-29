package com.ruleta;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Todo lo que puede salir en la ruleta. Agrega o quita entradas aqui. */
public final class Catalog {
    /** Banderas de dificultad activas (las leen los eventos de spawn, el tick y los mixins). */
    public static final Set<String> FLAGS = ConcurrentHashMap.newKeySet();
    private static final List<RuletaEntry> ALL = new ArrayList<>();
    private static final Consumer<MinecraftServer> NOOP = s -> {};

    private static void add(RuletaColor c, String name, String text, int seconds,
                            Consumer<MinecraftServer> start, Consumer<MinecraftServer> stop) {
        ALL.add(new RuletaEntry(c, name, text, seconds * 20, start, stop));
    }

    static {
        // ---------- NARANJA: cambios de dificultad (temporales) ----------
        add(RuletaColor.NARANJA, "Creepers electrificados", "Los creepers que aparezcan estaran cargados.", 600,
                s -> FLAGS.add("charged"), s -> FLAGS.remove("charged"));
        add(RuletaColor.NARANJA, "Mobs veloces", "Los monstruos nuevos tienen Velocidad II.", 600,
                s -> FLAGS.add("fast"), s -> FLAGS.remove("fast"));
        add(RuletaColor.NARANJA, "Sin regeneracion natural", "Ya no regeneras vida comiendo.", 600,
                s -> s.getGameRules().get(GameRules.NATURAL_REGENERATION).set(false, s),
                s -> s.getGameRules().get(GameRules.NATURAL_REGENERATION).set(true, s));
        add(RuletaColor.NARANJA, "Hambre constante", "Tu barra de hambre baja muy rapido.", 300,
                s -> FLAGS.add("hunger"), s -> FLAGS.remove("hunger"));
        add(RuletaColor.NARANJA, "Puertas trampa",
                "Abrir una puerta te hace 1.000.000 de dano. Solo un totem (en la mano) te salva.", 300,
                s -> FLAGS.add("doordeath"), s -> FLAGS.remove("doordeath"));
        add(RuletaColor.NARANJA, "Fuego eterno del Nether",
                "El fuego del Nether no se apaga solo ni a golpes. Solo el agua lo detiene.", 600,
                s -> FLAGS.add("eternalfire"), s -> FLAGS.remove("eternalfire"));

        // ---------- ROJA: evento Deivil ----------
        add(RuletaColor.ROJA, "Momento Deivil", "El Momento Deivil ha comenzado.", 15,
                s -> FLAGS.add("deivil"), s -> FLAGS.remove("deivil"));

        // ---------- AMARILLA: mensajes ----------
        add(RuletaColor.AMARILLA, "Mensaje del admin", "El admin tiene algo que decir.", 0, NOOP, NOOP);
        add(RuletaColor.AMARILLA, "Advertencia", "Cuidado, lo peor todavia no ha llegado.", 0, NOOP, NOOP);
        add(RuletaColor.AMARILLA, "Ultima hora", "Quedan pocos minutos, esfuerzense.", 0, NOOP, NOOP);

        // ---------- VERDE: mensajes de mejoras; los efectos se configuraran despues ----------
        add(RuletaColor.VERDE, "Mejora anunciada", "Se anuncia una mejora. Sus efectos estan pendientes de configuracion.", 0, NOOP, NOOP);

        // ---------- CIAN: mensajes de misiones; no se ejecutan mecanicas ----------
        add(RuletaColor.CIAN, "Mision anunciada", "Se anuncia una mision. El contenido esta pendiente de configuracion.", 0, NOOP, NOOP);

        // ---------- MORADA: mensajes para anuncios y eventos administrativos ----------
        add(RuletaColor.MORADA, "Anuncio", "Anuncio del servidor.", 0, NOOP, NOOP);

        // ---------- ROSADA: anuncio de minijuego, sin mecanicas ----------
        add(RuletaColor.ROSADA, "Minijuego anunciado", "Se anuncia un minijuego. Sus mecanicas se configuraran despues.", 0, NOOP, NOOP);
    }

    public static List<RuletaEntry> forColor(RuletaColor c) {
        return ALL.stream().filter(e -> e.color() == c).toList();
    }

}
