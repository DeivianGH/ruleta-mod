package com.ruleta.client;

import com.ruleta.RuletaColor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Ajustes locales independientes por color; quedan listos para ampliar con mas campos. */
final class RuletaClientConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ruleta.properties");
    private static final Properties VALUES = load();

    private RuletaClientConfig() {}

    static String title(RuletaColor color) { return VALUES.getProperty(key(color, "title"), ""); }
    static String message(RuletaColor color) { return VALUES.getProperty(key(color, "message"), ""); }

    static void save(RuletaColor color, String title, String message) {
        VALUES.setProperty(key(color, "title"), title);
        VALUES.setProperty(key(color, "message"), message);
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                VALUES.store(out, "Ajustes de la ruleta por color");
            }
        } catch (IOException ignored) {
            // El panel sigue funcionando durante la sesion aunque la carpeta no permita escribir.
        }
    }

    private static Properties load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) { properties.load(in); }
            catch (IOException ignored) { /* Se usan valores predeterminados. */ }
        }
        return properties;
    }

    private static String key(RuletaColor color, String field) { return "color." + color.id() + "." + field; }
}
