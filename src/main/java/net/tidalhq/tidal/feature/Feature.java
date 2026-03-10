package net.tidalhq.tidal.feature;

import net.minecraft.util.math.BlockPos;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.pathfind.PathResult;
import net.tidalhq.tidal.pathfind.PathfindingSession;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base Feature class
 * holds context, enabled state, and pathfinder session
 */
public abstract class Feature {
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
    public abstract String getDisplayName();
    public abstract Category getCategory();

    public List<ConfigOption<?>> getOptions() {
        return List.of();
    }

    public boolean isEnabled() { return enabled; }

    void setEnabled(boolean enabled) { this.enabled = enabled; }
}