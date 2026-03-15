package net.tidalhq.tidal.pathfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientEndTickEvent;
import net.tidalhq.tidal.util.InputUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PathExecutor {

    private static final double REACH_DIST       = 2.5;
    private static final double FINAL_REACH_DIST = 1.5;
    private static final int    STUCK_TICKS      = 60;
    private static final int    LOOKAHEAD        = 8;

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private List<BlockPos>               path      = List.of();
    private int                          index     = 0;
    private boolean                      running   = false;
    @Nullable private Consumer<BlockPos> onArrival = null;
    @Nullable private RotationController rotation  = null;

    private Vec3d   lastPos           = Vec3d.ZERO;
    private int     stuckTimer        = 0;
    private boolean jumpedForWaypoint = false;

    private static final PathExecutor instance = new PathExecutor();
    public static PathExecutor getInstance() { return instance; }

    private PathExecutor() {
        EventBus.getInstance().register(this);
    }

    public void start(List<BlockPos> path, RotationController rotation,
                      @Nullable Consumer<BlockPos> onArrival) {
        stop();
        if (path == null || path.isEmpty()) return;

        this.path              = path;
        this.rotation          = rotation;
        this.index             = 0;
        this.onArrival         = onArrival;
        this.running           = true;
        this.lastPos           = playerPos();
        this.stuckTimer        = 0;
        this.jumpedForWaypoint = false;
    }

    public void start(List<BlockPos> path, RotationController rotation) {
        start(path, rotation, null);
    }

    public void stop() {
        running = false;
        InputUtil.reset();
        if (rotation != null) rotation.clearTarget();
    }

    public boolean isRunning() { return running; }

    @Nullable public RotationController getRotation() { return running ? rotation : null; }

    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        if (!running) return;
        if (client.player == null || client.world == null) { stop(); return; }
        if (index >= path.size()) { finish(); return; }

        Vec3d pos = playerPos();

        // Stuck detection
        if (pos.distanceTo(lastPos) < 0.01) {
            if (++stuckTimer >= STUCK_TICKS) { stop(); return; }
        } else {
            stuckTimer = 0;
            lastPos    = pos;
        }

        advanceIndex(pos);
        if (index >= path.size()) { finish(); return; }

        BlockPos waypoint = path.get(index);
        Vec3d    target   = Vec3d.ofCenter(waypoint);

        boolean isFinal = index == path.size() - 1;
        double  reach   = isFinal ? FINAL_REACH_DIST : REACH_DIST;
        if (horizontalDist(pos, target) < reach) { finish(); return; }

        float desiredYaw = yawToward(target);
        if (rotation != null) {
            rotation.setTarget(desiredYaw, client.player.getPitch());
        }

        float yawDiff = Math.abs(MathHelper.wrapDegrees(desiredYaw - client.player.getYaw()));
        if (yawDiff < 35f) {
            InputUtil.press(client.options.forwardKey);
        } else {
            InputUtil.release(client.options.forwardKey);
        }

        int dy = waypoint.getY() - client.player.getBlockY();
        boolean needsJump = dy > 0 && dy <= 1;
        if (needsJump && !jumpedForWaypoint && client.player.isOnGround()) {
            InputUtil.press(client.options.jumpKey);
            jumpedForWaypoint = true;
        } else {
            InputUtil.release(client.options.jumpKey);
        }
    }

    private void advanceIndex(Vec3d pos) {
        while (index < path.size()) {
            boolean isFinal = index == path.size() - 1;
            double  reach   = isFinal ? FINAL_REACH_DIST : REACH_DIST;
            if (horizontalDist(pos, Vec3d.ofCenter(path.get(index))) < reach) {
                index++;
                jumpedForWaypoint = false;
            } else {
                break;
            }
        }

        if (index >= path.size()) return;

        if (isBehind(pos, Vec3d.ofCenter(path.get(index)))) {
            int    best     = index;
            double bestDist = Double.MAX_VALUE;
            for (int i = index + 1; i < Math.min(index + LOOKAHEAD, path.size()); i++) {
                Vec3d candidate = Vec3d.ofCenter(path.get(i));
                if (!isBehind(pos, candidate)) {
                    double d = horizontalDist(pos, candidate);
                    if (d < bestDist) { bestDist = d; best = i; }
                }
            }
            if (best != index) {
                index             = best;
                jumpedForWaypoint = false;
            }
        }
    }

    private boolean isBehind(Vec3d pos, Vec3d target) {
        if (client.player == null) return false;
        return Math.abs(MathHelper.wrapDegrees(yawToward(target) - client.player.getYaw())) > 90f;
    }

    private void finish() {
        running = false;
        InputUtil.reset();
        if (rotation != null) rotation.clearTarget();
        if (onArrival != null && !path.isEmpty()) {
            onArrival.accept(path.getLast());
        }
    }

    private static float yawToward(Vec3d target) {
        if (client.player == null) return 0f;
        Vec3d  pos = playerPos();
        double dx  = target.x - pos.x;
        double dz  = target.z - pos.z;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static Vec3d playerPos() {
        if (client.player == null) return Vec3d.ZERO;
        return client.player.getEntityPos();
    }

    private static double horizontalDist(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}