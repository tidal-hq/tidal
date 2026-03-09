package net.tidalhq.tidal.config;

import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class ConfigSerializer {

    private static final Logger LOGGER = Logger.getLogger("tidal.config");

    private final Path filePath;

    public ConfigSerializer(Path filePath) {
        this.filePath = filePath;
    }
    public void save(FeatureRegistry registry) {
        List<String> lines = new ArrayList<>();

        for (Feature feature : registry.all()) {
            String prefix = feature.getId() + ".";
            lines.add(prefix + "enabled=" + feature.isEnabled());

            for (ConfigOption<?> option : feature.getOptions()) {
                lines.add(prefix + option.getKey() + "=" + option.serialize());
            }
        }

        try {
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, String.join(System.lineSeparator(), lines));
        } catch (IOException e) {
            LOGGER.warning("Failed to save config: " + e.getMessage());
        }
    }

    public void load(FeatureRegistry registry, net.tidalhq.tidal.feature.FeatureManager featureManager) {
        if (!Files.exists(filePath)) return;

        Map<String, String> entries = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                entries.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to read config: " + e.getMessage());
            return;
        }

        for (Feature feature : registry.all()) {
            String prefix = feature.getId() + ".";

            String enabledRaw = entries.get(prefix + "enabled");
            if (enabledRaw != null) {
                featureManager.setEnabled(feature.getId(), Boolean.parseBoolean(enabledRaw));
            }

            for (ConfigOption<?> option : feature.getOptions()) {
                String raw = entries.get(prefix + option.getKey());
                if (raw != null) {
                    try {
                        option.deserialize(raw);
                    } catch (Exception e) {
                        LOGGER.warning("Could not deserialize option '"
                                + prefix + option.getKey() + "': " + e.getMessage());
                    }
                }
            }
        }
    }
}