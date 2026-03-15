package net.tidalhq.tidal.pathfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class RotationController {

    private static final float MAX_DEG_PER_TICK = 22f;
    private static final float MIN_DEG_PER_TICK = 2f;
    private static final float DEAD_ZONE        = 3f;

    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Random rng = new Random();

    private boolean hasTarget   = false;
    private float   targetYaw   = 0f;
    private float   targetPitch = 0f;
    private float   speedNoise  = 0f;

    public RotationController() {}


    public void setTarget(float yaw, float pitch) {
        this.targetYaw   = yaw;
        this.targetPitch = MathHelper.clamp(pitch, -90f, 90f);
        this.hasTarget   = true;
    }

    public void clearTarget() {
        hasTarget = false;
    }

    public boolean hasTarget() { return hasTarget; }

    public boolean isOnTarget() {
        if (!hasTarget || client.player == null) return false;
        float yawDiff   = Math.abs(MathHelper.wrapDegrees(targetYaw - client.player.getYaw()));
        float pitchDiff = Math.abs(targetPitch - client.player.getPitch());
        return yawDiff <= DEAD_ZONE && pitchDiff <= DEAD_ZONE;
    }


    public double consumeDeltaX() {
        if (client.player == null || !hasTarget) return 0;
        float diff = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        if (Math.abs(diff) <= DEAD_ZONE) return 0;
        return computeStep(diff) / sensitivityScale();
    }

    public double consumeDeltaY() {
        if (client.player == null || !hasTarget) return 0;
        float diff = MathHelper.clamp(targetPitch - client.player.getPitch(), -90f, 90f);
        if (Math.abs(diff) <= DEAD_ZONE) return 0;
        return computeStep(diff) / sensitivityScale();
    }


    private float computeStep(float diff) {
        speedNoise = speedNoise * 0.85f + (rng.nextFloat() - 0.5f) * 3f;
        float raw  = Math.abs(diff) * 0.35f + MIN_DEG_PER_TICK + speedNoise;
        float step = MathHelper.clamp(raw, MIN_DEG_PER_TICK, MAX_DEG_PER_TICK);
        step = Math.min(step, Math.abs(diff));
        return Math.copySign(step, diff);
    }

    private static double sensitivityScale() {
        double s = client.options.getMouseSensitivity().getValue();
        double f = s * 0.6 + 0.2;
        return f * f * f * 8.0;
    }
}
