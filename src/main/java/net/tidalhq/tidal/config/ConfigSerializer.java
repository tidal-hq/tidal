package net.tidalhq.tidal.config;

import net.tidalhq.tidal.registry.Registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class ConfigSerializer<T extends ConfigSerializable> {

    private static final Logger LOGGER = Logger.getLogger("tidal.config");

    private final Path filePath;

    public ConfigSerializer(Path filePath) {
        this.filePath = filePath;
    }

    public void save(Registry<T> registry) {

        List<String> lines = new ArrayList<>();

        for (T obj : registry.getRegisteredObjects()) {

            String prefix = obj.getId() + ".";

            for (var entry : obj.serialize().entrySet()) {
                lines.add(prefix + entry.getKey() + "=" + entry.getValue());
            }
        }

        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, lines);
        } catch (IOException e) {
            LOGGER.warning("Failed to save config: " + e.getMessage());
        }
    }

    public void load(Registry<T> registry) {

        if (!Files.exists(filePath)) return;

        Map<String, Map<String, String>> grouped = new HashMap<>();

        try {
            for (String line : Files.readAllLines(filePath)) {

                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int eq = line.indexOf('=');
                if (eq < 0) continue;

                String key = line.substring(0, eq);
                String value = line.substring(eq + 1);

                int dot = key.indexOf('.');
                if (dot < 0) continue;

                String id = key.substring(0, dot);
                String option = key.substring(dot + 1);

                grouped
                        .computeIfAbsent(id, k -> new HashMap<>())
                        .put(option, value);
            }

        } catch (IOException e) {
            LOGGER.warning("Failed to read config: " + e.getMessage());
            return;
        }

        for (T obj : registry.getRegisteredObjects()) {

            Map<String, String> values = grouped.get(obj.getId());
            if (values != null) {
                obj.deserialize(values);
            }
        }
    }
}