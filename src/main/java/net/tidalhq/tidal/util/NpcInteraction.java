package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.pathfinder.PathExecutor;
import net.tidalhq.tidal.pathfinder.RotationController;
import net.tidalhq.tidal.pathfinder.WalkPathfinder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class NpcInteraction {

    private static final double INTERACT_RANGE = 3.5;
    private static final int    GUI_WAIT_MAX   = 60;
    private static final int    FACE_WAIT_MAX  = 20;
    private static final int    MAX_PATH_RETRIES = 2;

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public enum State { IDLE, PATHFINDING, FACING, INTERACTING, WAITING_FOR_GUI, DONE, FAILED }

    private final BlockPos           npcPos;
    private final RotationController rotation;
    private Predicate<Screen>        guiPredicate = screen -> true;
    private Consumer<Screen>         guiCallback  = screen -> {};
    private Consumer<String>         onFail       = reason -> {};

    private State   state       = State.IDLE;
    private int     waitTicks   = 0;
    private int     pathRetries = 0;

    private NpcInteraction(BlockPos npcPos, RotationController rotation) {
        this.npcPos   = npcPos;
        this.rotation = rotation;
    }

    public static NpcInteraction create(BlockPos npcPos, RotationController rotation) {
        return new NpcInteraction(npcPos, rotation);
    }

    public NpcInteraction onGui(Predicate<Screen> predicate, Consumer<Screen> callback) {
        this.guiPredicate = predicate;
        this.guiCallback  = callback;
        return this;
    }

    public NpcInteraction onFail(Consumer<String> callback) { this.onFail = callback; return this; }
    public NpcInteraction onFail(Runnable callback)         { this.onFail = r -> callback.run(); return this; }

    public NpcInteraction start() {
        state       = State.PATHFINDING;
        waitTicks   = 0;
        pathRetries = 0;
        beginPathfinding();
        return this;
    }

    public void stop() {
        PathExecutor.getInstance().stop();
        rotation.clearTarget();
        state = State.IDLE;
    }

    public boolean isDone()   { return state == State.DONE; }
    public boolean isFailed() { return state == State.FAILED; }
    public State   getState() { return state; }

    public void tick() {
        switch (state) {
            case IDLE, DONE, FAILED -> {}

            case PATHFINDING -> {
                if (!PathExecutor.getInstance().isRunning()) {
                    if (isInRange()) {
                        state     = State.FACING;
                        waitTicks = 0;
                    } else if (pathRetries < MAX_PATH_RETRIES) {
                        beginPathfinding();
                    } else {
                        fail("could not reach NPC at " + npcPos + " after " + MAX_PATH_RETRIES + " attempts");
                    }
                }
            }

            case FACING -> {
                Vec3d npcCenter = Vec3d.ofCenter(npcPos);
                rotation.setTarget(yawToward(npcCenter), pitchToward(npcCenter));

                if (rotation.isOnTarget() || ++waitTicks >= FACE_WAIT_MAX) {
                    state     = State.INTERACTING;
                    waitTicks = 0;
                }
            }

            case INTERACTING -> {
                rotation.clearTarget();
                rightClickNpc();
                state     = State.WAITING_FOR_GUI;
                waitTicks = 0;
            }

            case WAITING_FOR_GUI -> {
                Screen screen = client.currentScreen;
                if (screen != null && guiPredicate.test(screen)) {
                    state = State.DONE;
                    guiCallback.accept(screen);
                } else if (++waitTicks >= GUI_WAIT_MAX) {
                    fail("GUI did not open after " + GUI_WAIT_MAX + " ticks (screen="
                            + (screen == null ? "null" : screen.getTitle().getString()) + ")");
                }
            }
        }
    }

    private void beginPathfinding() {
        BlockPos start  = BlockUtil.getPlayerFeetPos();
        BlockPos target = nearestWalkableApproach(npcPos);

        if (isInRange()) {
            state     = State.FACING;
            waitTicks = 0;
            return;
        }

        Thread t = new Thread(() -> {
            List<BlockPos> path = WalkPathfinder.findPath(start, target);
            client.execute(() -> {
                if (path.isEmpty()) {
                    if (isInRange()) {
                        state     = State.FACING;
                        waitTicks = 0;
                    } else if (pathRetries < MAX_PATH_RETRIES) {
                        beginPathfinding();
                    } else {
                        fail("no path found to NPC at " + npcPos);
                    }
                    return;
                }
                PathExecutor.getInstance().start(path, rotation,
                        arrived -> client.execute(() -> {
                            state     = State.FACING;
                            waitTicks = 0;
                        }));
            });
        }, "tidal-npc-path");
        t.setDaemon(true);
        t.start();
    }

    private void fail(String reason) {
        state = State.FAILED;
        PathExecutor.getInstance().stop();
        rotation.clearTarget();
        onFail.accept(reason);
    }

    private boolean isInRange() {
        if (client.player == null) return false;
        return client.player.getBlockPos().isWithinDistance(npcPos, INTERACT_RANGE);
    }

    private void rightClickNpc() {
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        Box searchBox = new Box(npcPos).expand(2.0);
        List<Entity> nearby = client.world.getEntitiesByClass(
                Entity.class, searchBox, e -> !(e instanceof PlayerEntity));

        if (!nearby.isEmpty()) {
            Entity npc = nearby.stream()
                    .min((a, b) -> Double.compare(
                            a.squaredDistanceTo(Vec3d.ofCenter(npcPos)),
                            b.squaredDistanceTo(Vec3d.ofCenter(npcPos))))
                    .orElse(nearby.get(0));
            client.interactionManager.interactEntity(client.player, npc, Hand.MAIN_HAND);
        } else {
            InputUtil.press(client.options.useKey);
        }
    }

    private static BlockPos nearestWalkableApproach(BlockPos npcPos) {
        int[][] offsets = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};
        for (int[] o : offsets) {
            BlockPos candidate = npcPos.add(o[0], 0, o[1]);
            if (isWalkable(candidate)) return candidate;
        }
        return npcPos;
    }

    private static boolean isWalkable(BlockPos pos) {
        if (client.world == null) return false;
        return client.world.getBlockState(pos.down()).isSolid()
                && client.world.getBlockState(pos).isAir()
                && client.world.getBlockState(pos.up()).isAir();
    }

    private static float yawToward(Vec3d target) {
        if (client.player == null) return 0f;
        double dx = target.x - client.player.getX();
        double dz = target.z - client.player.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static float pitchToward(Vec3d target) {
        if (client.player == null) return 0f;
        double dx    = target.x - client.player.getX();
        double dy    = target.y - (client.player.getY() + client.player.getEyeHeight(client.player.getPose()));
        double dz    = target.z - client.player.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, horiz));
    }
}