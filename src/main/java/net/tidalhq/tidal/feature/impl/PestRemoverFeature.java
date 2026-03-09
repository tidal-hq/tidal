package net.tidalhq.tidal.feature.impl;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.FeatureTaskCompleteEvent;
import net.tidalhq.tidal.event.impl.ParticleSpawnedEvent;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.macro.MacroContext;
import net.tidalhq.tidal.state.TablistState;
import net.tidalhq.tidal.util.InputUtil;
import net.tidalhq.tidal.util.PlayerUtil;

public class PestRemoverFeature extends Feature {
    public enum PestRemoverState {
        IDLE,
        SEARCHING_FOR_PEST,
        LOCATING_PEST,
        FOLLOWING_TRAIL,
        KILLING_PEST,
        RETURNING
    }

    private static final int THRESHOLD = 6;
    private static final double PARTICLE_PROXIMITY = 5.0;
    private static final double PARTICLE_CHAIN_MAX = 2.0;
    private static final double TARGET_REACHED_DIST = 3.0;
    private static final double TARGET_EXTRAPOLATE = 10.0;
    private static final double MIN_TRAIL_LENGTH = 0.1;
    private static final long FIRE_WAIT_MS = 500;
    private static final long FIRST_PARTICLE_TIMEOUT_MS = 3000;
    private static final long PARTICLE_TRAIL_TIMEOUT_MS = 2000;
    private static final int MIN_PARTICLES = 3;

    private Vec3d firstParticle;
    private Vec3d lastParticle;
    private long lastParticleTime;
    private long fireTime;
    private int particleCount;
    private Vec3d currentTarget;

    private PestRemoverState pestState = PestRemoverState.IDLE;

    public PestRemoverFeature(MacroContext ctx) {
        super(ctx);
    }

    @Override public String getId() { return "pest_remover"; }
    @Override public String getDisplayName() { return "Pest Remover"; }
    @Override public Category getCategory() { return Category.FARMING; }
    @Override public boolean shouldPauseMacroExecution() { return true; }
    @Override public boolean shouldStartAtMacroStart() { return true; }

    @Subscribe
    public void onParticleSpawned(ParticleSpawnedEvent event) {
        if (!isRunning() || pestState != PestRemoverState.FOLLOWING_TRAIL) return;
        if (event.getParameters().getType() != ParticleTypes.ANGRY_VILLAGER) return;

        long now = System.currentTimeMillis();
        Vec3d pos = new Vec3d(event.getX(), event.getY(), event.getZ());

        if (firstParticle == null) {
            if (ctx.client().player.getEntityPos().distanceTo(pos) > PARTICLE_PROXIMITY) return;
            firstParticle = pos;
            lastParticle = pos;
            lastParticleTime = now;
            particleCount = 1;
            return;
        }

        if (lastParticle.distanceTo(pos) > PARTICLE_CHAIN_MAX) return;
        lastParticle = pos;
        lastParticleTime = now;
        particleCount++;
    }

    private Vec3d calculateTarget() {
        if (firstParticle == null || lastParticle == null) return null;
        if (firstParticle.distanceTo(lastParticle) < MIN_TRAIL_LENGTH) return null;
        Vec3d direction = lastParticle.subtract(firstParticle).normalize();
        return lastParticle.add(direction.multiply(TARGET_EXTRAPOLATE));
    }

    private void resetTracking() {
        firstParticle = null;
        lastParticle = null;
        lastParticleTime = 0;
        currentTarget = null;
        particleCount = 0;
        fireTime = 0;
    }

    @Override
    public void onTick() {
        if (ctx.client().player == null) return;

        switch (pestState) {
            case IDLE:
                if (TablistState.getInstance().getPestsCount() > THRESHOLD) {
                    ctx.notifier().info("Pest threshold reached — pausing macro");
                    pestState = PestRemoverState.SEARCHING_FOR_PEST;
                    ctx.featureManager().requestMacroPause(this);
                }
                break;

            case SEARCHING_FOR_PEST:
                ctx.notifier().info("Searching for pest...");
                pestState = PestRemoverState.LOCATING_PEST;
                break;

            case LOCATING_PEST:
                if (!PlayerUtil.swapToVacuum()) {
                    fail("couldn't find vacuum in hotbar");
                    break;
                }
                ctx.notifier().info("Firing vacuum locator...");
                resetTracking();
                InputUtil.press(ctx.client().options.attackKey);
                fireTime = System.currentTimeMillis();
                lastParticleTime = Long.MAX_VALUE;
                pestState = PestRemoverState.FOLLOWING_TRAIL;
                break;

            case FOLLOWING_TRAIL:
                long now = System.currentTimeMillis();

                if (now - fireTime < FIRE_WAIT_MS) break;

                if (firstParticle == null) {
                    if (now - fireTime > FIRST_PARTICLE_TIMEOUT_MS) {
                        InputUtil.release(ctx.client().options.attackKey);
                        ctx.notifier().warning("No particles detected, retrying...");
                        pestState = PestRemoverState.LOCATING_PEST;
                    }
                    break;
                }

                if (now - lastParticleTime < PARTICLE_TRAIL_TIMEOUT_MS) break;

                InputUtil.release(ctx.client().options.attackKey);

                if (particleCount < MIN_PARTICLES) {
                    ctx.notifier().warning("Too few particles (" + particleCount + "), retrying...");
                    resetTracking();
                    pestState = PestRemoverState.LOCATING_PEST;
                    break;
                }

                currentTarget = calculateTarget();
                if (currentTarget == null) {
                    ctx.notifier().warning("Couldn't calculate pest position, retrying...");
                    resetTracking();
                    pestState = PestRemoverState.LOCATING_PEST;
                    break;
                }

                ctx.notifier().info("Target calculated — moving to pest...");
                break;

            case KILLING_PEST:
                // TODO: implement kill logic
                ctx.notifier().info("Pest destroyed!");
                pestState = PestRemoverState.RETURNING;
                break;

            case RETURNING:
                // TODO: implement return logic
                ctx.notifier().info("Returning to farm...");
                resetTracking();
                pestState = PestRemoverState.IDLE;
                ctx.notifier().info("Back at farm - resuming macro");
                ctx.eventBus().post(new FeatureTaskCompleteEvent(this));
                break;
        }
    }

    @Override
    protected void onStop() {
        pestState = PestRemoverState.IDLE;
        resetTracking();
        InputUtil.release(ctx.client().options.attackKey);
    }
}