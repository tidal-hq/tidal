package net.tidalhq.tidal.pathfind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.tidalhq.tidal.util.InputUtil;

public class PathExecutor {

    private static final double ARRIVAL_RADIUS      = 0.6;
    private static final int    STUCK_TIMEOUT_TICKS = 60;  // ~3 seconds
    private static final double STUCK_MIN_PROGRESS  = 0.1; // blocks per stuck-check window

    private Path path;
    private int nodeIndex;

    private int stuckTicks;
    private double lastDistanceToNode;

    private boolean finished;
    private boolean stuck;

    public void follow(Path path) {
        this.path              = path;
        this.nodeIndex         = 0;
        this.stuckTicks        = 0;
        this.lastDistanceToNode = Double.MAX_VALUE;
        this.finished          = path.isEmpty();
        this.stuck             = false;
    }

    public void stop() {
        path     = null;
        finished = false;
        stuck    = false;
        InputUtil.reset();
    }

    public boolean isIdle() { return path == null || finished; }

    public boolean isFinished() { return finished; }

    public boolean isStuck() { return stuck; }

    public void onTick() {
        if (path == null || finished || stuck) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        PathNode target = path.getNodes().get(nodeIndex);

        double playerX = client.player.getX();
        double playerZ = client.player.getZ();
        double targetX = target.worldX();
        double targetZ = target.worldZ();

        double dx = targetX - playerX;
        double dz = targetZ - playerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < ARRIVAL_RADIUS) {
            nodeIndex++;
            stuckTicks        = 0;
            lastDistanceToNode = Double.MAX_VALUE;

            if (nodeIndex >= path.getNodes().size()) {
                finished = true;
                InputUtil.reset();
                return;
            }

            target    = path.getNodes().get(nodeIndex);
            targetX   = target.worldX();
            targetZ   = target.worldZ();
            dx        = targetX - playerX;
            dz        = targetZ - playerZ;
            distance  = Math.sqrt(dx * dx + dz * dz);
        }

        stuckTicks++;
        if (stuckTicks >= STUCK_TIMEOUT_TICKS) {
            if (lastDistanceToNode - distance < STUCK_MIN_PROGRESS) {
                stuck = true;
                InputUtil.reset();
                return;
            }
            stuckTicks        = 0;
            lastDistanceToNode = distance;
        }

        applyMovementKeys(client, dx, dz, target.getMoveType());
    }

    private void applyMovementKeys(MinecraftClient client, double dx, double dz, PathNode.MoveType moveType) {
        GameOptions options = client.options;

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        client.player.setYaw(targetYaw);
        client.player.setPitch(0f);

        InputUtil.release(
                options.backKey,
                options.leftKey,
                options.rightKey,
                options.jumpKey
        );
        InputUtil.press(options.forwardKey);

        if (moveType == PathNode.MoveType.JUMP && client.player.isOnGround()) {
            InputUtil.press(options.jumpKey);
        }
    }

    public int getNodeIndex() { return nodeIndex; }

    public int getPathLength() { return path != null ? path.length() : 0; }

    public PathNode getCurrentTarget() {
        if (path == null || finished || nodeIndex >= path.getNodes().size()) return null;
        return path.getNodes().get(nodeIndex);
    }
}