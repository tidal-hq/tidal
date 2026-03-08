package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.event.impl.FeatureFailedEvent;
import net.tidalhq.tidal.event.impl.FeatureTaskCompleteEvent;
import net.tidalhq.tidal.macro.MacroContext;

public abstract class Feature {
    protected final MacroContext ctx;

    private boolean enabled = false;
    private FeatureState state = FeatureState.IDLE;

    public enum FeatureState { IDLE, RUNNING, PAUSED }

    protected Feature(MacroContext ctx) {
        this.ctx = ctx;
    }

    protected final void fail(String reason) {
        ctx.notifier().danger(getDisplayName() + " failed: " + reason);
        ctx.eventBus().post(new FeatureFailedEvent(this, reason));
        stop();
    }

    public final void enable() {
        if (enabled) return;
        enabled = true;
        ctx.eventBus().register(this);
        onEnabled();
    }

    public final void disable() {
        if (!enabled) return;
        if (state != FeatureState.IDLE) forceStop();
        enabled = false;
        ctx.eventBus().unregister(this);
        onDisabled();
    }

    public final void start() {
        if (!enabled || state != FeatureState.IDLE) return;
        state = FeatureState.RUNNING;
        onStart();
    }

    public final void stop() {
        if (state == FeatureState.IDLE) return;
        state = FeatureState.IDLE;
        onStop();
    }

    public final void pause() {
        if (state != FeatureState.RUNNING) return;
        state = FeatureState.PAUSED;
        onPause();
    }

    public final void resume() {
        if (state != FeatureState.PAUSED) return;
        state = FeatureState.RUNNING;
        onResume();
    }

    private void forceStop() {
        state = FeatureState.IDLE;
        onStop();
    }

    protected void onEnabled() {}
    protected void onDisabled() {}
    protected void onStart() {}
    protected void onStop() {}
    protected void onPause() {}
    protected void onResume() {}
    public void onTick() {}

    public final boolean isEnabled() { return enabled; }
    public final boolean isRunning() { return state == FeatureState.RUNNING; }
    public final boolean isPaused() { return state == FeatureState.PAUSED; }
    public final boolean isIdle() { return state == FeatureState.IDLE; }
    public final FeatureState getState() { return state; }

    public abstract String getId();
    public abstract String getDisplayName();
    public abstract Category getCategory();

    public boolean shouldPauseMacroExecution() { return false; }
    public boolean shouldStartAtMacroStart() { return false; }
}