package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.macro.MacroContext;

public abstract class Feature {
    protected final MacroContext ctx;
    private boolean enabled;

    protected Feature(MacroContext ctx) {
        this.ctx = ctx;
    }

    public void onEnable() {
        ctx.eventBus().register(this);
    }

    public void onDisable() {
        ctx.eventBus().unregister(this);
    }

    public void onTick() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public abstract String getId();
    public abstract String getDisplayName();
    public abstract Category getCategory();
}