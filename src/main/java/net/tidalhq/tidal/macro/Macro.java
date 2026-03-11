package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.Crop;
import net.tidalhq.tidal.registry.Registerable;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.InputUtil;
import net.tidalhq.tidal.util.PlayerUtil;

public abstract class Macro implements Registerable {

    protected final MacroContext ctx;

    private static final int WARP_DELAY_MIN = 20;
    private static final int WARP_DELAY_MAX = 60;

    private MacroPhase currentPhase;

    private boolean pendingWarp;
    private int     warpCountdown;
    private boolean initialWarpPending;

    private boolean wasSneaking;

    protected Macro(MacroContext ctx) {
        this.ctx = ctx;
    }

    public MacroPhase getPhase() {
        return currentPhase;
    }

    protected final void setPhase(MacroPhase newPhase) {
        MacroPhase previous = this.currentPhase;
        this.currentPhase = newPhase;
        onPhaseChanged(previous, newPhase);
    }

    protected void onPhaseChanged(MacroPhase previous, MacroPhase next) {}

    public boolean onEnable() {
        initialWarpPending = true;
        return true;
    }

    public void onDisable() {
        initialWarpPending = false;
        setPhase(null);
        InputUtil.reset();
    }

    public void onDeath() {
        pendingWarp   = true;
        warpCountdown = randomBetween(WARP_DELAY_MIN, WARP_DELAY_MAX);
        setPhase(CorePhase.WARPING);
        InputUtil.reset();
    }

    public void onTick() {
        tickWarp();
        updatePhase();
        applyInputs();
        tickSneak();
    }

    private void tickWarp() {
        if (pendingWarp) {
            if (--warpCountdown <= 0) {
                pendingWarp = false;
                PlayerUtil.warp(getTargetLocation());
                setPhase(CorePhase.IDLE);
            }
            return;
        }

        if (initialWarpPending) {
            initialWarpPending = false;
            Location target = getTargetLocation();
            if (!ctx.gameState().getCurrentLocation().equals(target)) {
                ctx.notifier().info("Warping to " + target.getName());
                PlayerUtil.warp(target);
            }
        }
    }

    private void tickSneak() {
        MacroPhase phase = currentPhase;
        boolean shouldSneak = phase != null
                && !phase.isIdle()
                && !ctx.world().isPlayerOnGround();

        if (shouldSneak && !wasSneaking) {
            InputUtil.sneak();
            wasSneaking = true;
        } else if (!shouldSneak && wasSneaking) {
            InputUtil.unSneak();
            wasSneaking = false;
        }
    }

    private static int randomBetween(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    public abstract void updatePhase();
    public abstract void applyInputs();
    public abstract Location getTargetLocation();
    public abstract String getName();
    public abstract String getDescription();
    public abstract String getId();
    public abstract Crop getTargetCrop();
}