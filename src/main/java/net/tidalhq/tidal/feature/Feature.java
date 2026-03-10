package net.tidalhq.tidal.feature;

import net.minecraft.util.math.BlockPos;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.config.ConfigSerializable;
import net.tidalhq.tidal.pathfind.PathResult;
import net.tidalhq.tidal.pathfind.PathfindingSession;
import net.tidalhq.tidal.registry.Registerable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base Feature class
 * holds context, enabled state, and pathfinder session
 */
public abstract class Feature implements Registerable, ConfigSerializable {
    protected final FeatureContext ctx;
    private boolean enabled;

    private net.tidalhq.tidal.pathfind.PathfindingSession activeSession;

    protected Feature(FeatureContext ctx) {
        this.ctx = ctx;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    public void onNavigationTick() {
        if (activeSession != null && activeSession.isActive()) {
            activeSession.onTick();
        }
    }

    protected void navigateTo(
            BlockPos goal,
            Runnable onArrival,
            Consumer<PathResult> onFailure
    ) {
        stopNavigation();
        activeSession = new PathfindingSession(onFailure, onArrival);
        activeSession.navigateTo(goal);
    }

    protected void stopNavigation() {
        if (activeSession != null) {
            activeSession.stop();
            activeSession = null;
        }
    }

    protected boolean isNavigating() {
        return activeSession != null && activeSession.isActive();
    }

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
            enabled = Boolean.parseBoolean(enabledRaw);
        }

        for (ConfigOption<?> option : getOptions()) {
            String raw = values.get(option.getKey());
            if (raw != null) {
                option.deserialize(raw);
            }
        }
    }
}