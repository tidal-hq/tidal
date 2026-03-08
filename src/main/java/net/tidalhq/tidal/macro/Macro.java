package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.Crop;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.InputUtil;
import net.tidalhq.tidal.util.PlayerUtil;

public abstract class Macro {
    protected final MacroContext ctx;

    private boolean wasSneaking;
    private boolean paused;

    private static final int WARP_DELAY_MIN = 20;
    private static final int WARP_DELAY_MAX = 60;

    private static final int START_DELAY_MIN = 60;
    private static final int START_DELAY_MAX = 100;

    private boolean pendingWarp;
    private int warpDelayTicks;

    private boolean pendingStart;
    private int startDelayTicks;

    private State state;

    public State getState() { return state; }
    public void setState(State state) {
        this.state = state;
        onSetState(state);
    }

    protected Macro(MacroContext ctx) {
        this.ctx = ctx;
    }

    protected void onSetState(State newState) {}

    public boolean onEnable() {
        Location target = this.getTargetLocation();
        if (!ctx.tablistState().getCurrentLocation().equals(target)) {
            ctx.notifier().info("warping to " + target.getName());
            PlayerUtil.warp(target);
        }

        Crop crop = this.getTargetCrop();
        PlayerUtil.setToolForCrop(crop);

        pendingStart = true;
        startDelayTicks = START_DELAY_MIN + (int)(Math.random() * (START_DELAY_MAX - START_DELAY_MIN));
        setState(State.WARPING);
        return true;
    }

    public void onDisable() {
        paused = false;
        setState(null);
        InputUtil.reset();
    }

    public void pause() {
        paused = true;
        InputUtil.reset();
        onPause();
    }

    public void resume() {
        paused = false;
        onResume();
    }

    public boolean isPaused() { return paused; }

    protected void onPause() {}
    protected void onResume() {}

    public void onTick() {
        if (paused) return;

        if (pendingStart) {
            startDelayTicks--;
            if (startDelayTicks <= 0) {
                pendingStart = false;
                setState(State.NONE);
            }
            return;
        }

        handleWarp();
        updateState();
        invokeState();

        boolean shouldSneak = getState() != State.WARPING
                && getState() != State.DROPPING
                && !ctx.client().player.isOnGround();

        if (shouldSneak && !wasSneaking) {
            InputUtil.sneak();
            this.wasSneaking = true;
        }

        if (!shouldSneak && wasSneaking) {
            InputUtil.unSneak();
            this.wasSneaking = false;
        }
    }

    public void handleWarp() {
        if (!pendingWarp) return;

        warpDelayTicks--;

        if (warpDelayTicks <= 0) {
            pendingWarp = false;
            PlayerUtil.warp(this.getTargetLocation());
            setState(State.NONE);
        }
    }

    public void onDeath() {
        this.pendingWarp = true;
        this.warpDelayTicks = WARP_DELAY_MIN + (int)(Math.random() * (WARP_DELAY_MAX - WARP_DELAY_MIN));

        setState(State.WARPING);
        InputUtil.reset();
    }

    public abstract void updateState();
    public abstract void invokeState();
    public abstract Location getTargetLocation();
    public abstract String getName();
    public abstract Crop getTargetCrop();
}