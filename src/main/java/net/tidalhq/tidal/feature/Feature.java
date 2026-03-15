package net.tidalhq.tidal.feature;

import net.minecraft.util.math.BlockPos;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.config.ConfigSerializable;
import net.tidalhq.tidal.registry.Registerable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Feature implements Registerable, ConfigSerializable {
    protected final FeatureContext ctx;
    private boolean enabled;

    protected Feature(FeatureContext ctx) {
        this.ctx = ctx;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    public abstract String getId();
    public abstract String getName();
    public abstract String getDescription();
    public abstract Category getCategory();

    public List<ConfigOption<?>> getOptions() {
        return List.of();
    }

    public boolean isEnabled() { return enabled; }

    void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public Map<String, String> serialize() {
        Map<String, String> data = new HashMap<>();
        data.put("enabled", String.valueOf(enabled));

        for (ConfigOption<?> option : getOptions()) {
            data.put(option.getKey(), option.serialize());
        }

        return data;
    }

    @Override
    public void deserialize(Map<String, String> values) {
        String enabledRaw = values.get("enabled");
        if (enabledRaw != null) {
            boolean shouldEnable = Boolean.parseBoolean(enabledRaw);
            if (shouldEnable != enabled) {
                enabled = shouldEnable;
                if (enabled) onEnable(); else onDisable();
            }
        }
    }
}