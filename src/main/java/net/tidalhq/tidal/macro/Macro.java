package net.tidalhq.tidal.macro;

import net.tidalhq.tidal.Crop;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.InputUtil;
import net.tidalhq.tidal.util.PlayerUtil;

public abstract class Macro {

    protected final MacroContext ctx;

    private static final int WARP_DELAY_MIN  = 20;
    private static final int WARP_DELAY_MAX  = 60;
    private static final int START_DELAY_MIN = 60;
    private static final int START_DELAY_MAX = 100;

    private MacroPhase currentPhase;

    private boolean pendingWarp;
    private int     warpCountdown;

    private boolean pendingStart;
    private int     startCountdown;

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
        Location target = getTargetLocation();
        if (!ctx.gameState().getCurrentLocation().equals(target)) {
            ctx.notifier().info("Warping to " + target.getName());
            PlayerUtil.warp(target);
        }

        Crop crop = getTargetCrop();
        if (!PlayerUtil.setToolForCrop(crop)) {
            ctx.notifier().danger("No suitable tool in hotbar for " + crop.name());
            return false;
        }

        scheduleStart();
        setPhase(CorePhase.WARPING);
        return true;
    }

    public void onDisable() {
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
        if (pendingStart) {
            if (--startCountdown <= 0) {
                pendingStart = false;
                setPhase(CorePhase.IDLE);
            }
            return;
        }

        tickWarp();
        updatePhase();
        applyInputs();
        tickSneak();
    }


    private void tickWarp() {
        if (!pendingWarp) return;
        if (--warpCountdown <= 0) {
            pendingWarp = false;
            PlayerUtil.warp(getTargetLocation());
            setPhase(CorePhase.IDLE);
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

    private void scheduleStart() {
        pendingStart   = true;
        startCountdown = randomBetween(START_DELAY_MIN, START_DELAY_MAX);
    }

    private static int randomBetween(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    public abstract void updatePhase();

    public abstract void applyInputs();

    public abstract Location getTargetLocation();

    public abstract String getName();

    public abstract Crop getTargetCrop();
}