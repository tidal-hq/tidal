package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.config.ConfigOption;

import java.util.List;

public abstract class Feature {

    protected final FeatureContext ctx;
    private boolean enabled;

    protected Feature(FeatureContext ctx) {
        this.ctx = ctx;
    }

    public void onEnable() {}

    public void onDisable() {}

    public void onTick() {}

    public void onNavigationTick() {}

    public abstract String getId();
    public abstract String getDisplayName();
    public abstract Category getCategory();

    public List<ConfigOption<?>> getOptions() {
        return List.of();
    }

    public boolean isEnabled() { return enabled; }

    void setEnabled(boolean enabled) { this.enabled = enabled; }
}