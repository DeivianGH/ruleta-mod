package com.ruleta;

import net.minecraft.util.Formatting;
import java.util.Locale;

/** Los 8 colores de la ruleta. Cada color = una categoria. Edita aqui para cambiar el reparto. */
public enum RuletaColor {
    ROJA("Roja", "Momento Deivil", 0xFFE53935, Formatting.RED),
    NARANJA("Naranja", "Dificultad", 0xFFFB8C00, Formatting.GOLD),
    AMARILLA("Amarilla", "Mensaje personalizado", 0xFFFDD835, Formatting.YELLOW),
    VERDE("Verde", "Mejoras", 0xFF43A047, Formatting.GREEN),
    CIAN("Cian", "Misiones", 0xFF00ACC1, Formatting.AQUA),
    MORADA("Morada", "Anuncios", 0xFF8E24AA, Formatting.DARK_PURPLE),
    ROSADA("Rosada", "Minijuego", 0xFFEC407A, Formatting.LIGHT_PURPLE);

    public final String label;
    public final String category;
    public final int argb;
    public final Formatting fmt;

    RuletaColor(String label, String category, int argb, Formatting fmt) {
        this.label = label;
        this.category = category;
        this.argb = argb;
        this.fmt = fmt;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static RuletaColor byId(int i) {
        return values()[Math.floorMod(i, values().length)];
    }

    public static RuletaColor byName(String s) {
        for (RuletaColor c : values()) if (c.id().equals(s.toLowerCase(Locale.ROOT))) return c;
        return null;
    }
}
