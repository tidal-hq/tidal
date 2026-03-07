package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.InputUtil;
import net.tidalhq.tidal.util.PlayerUtil;

public abstract class Macro {
    protected final MacroContext ctx;

    private boolean wasSneaking;

    private final static int WARP_DELAY_MIN = 20;
    private final static int WARP_DELAY_MAX = 60;

    private boolean pendingWarp;
    private int warpDelayTicks;

    private State state;
    public State getState() { return state; }
    public void setState(State state) {
        this.state = state;

        onSetState(state);
    }

    protected Macro(MacroContext ctx) {
        this.ctx = ctx;
    }

    protected void onSetState(State newState) {
    }

    public void onEnable() {
        Location target = this.getTargetLocation();
//        if (!ctx.serverState().isConnectedToHypixel()) return;
        if (!ctx.tablistState().getCurrentLocation().equals(target)) PlayerUtil.warp(target);
    }

    public void onDisable() {
        setState(null);
    }

    public void onPause() {}
    public void onResume() {}

    public void onTick() {
        handleWarp();
        updateState();
        invokeState();

        boolean isInActiveState = getState() != State.WARPING && getState() != State.DROPPING;
        boolean shouldSneak = !isInActiveState || !ctx.client().player.isOnGround();

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
            // Maybe another delay here?
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
}