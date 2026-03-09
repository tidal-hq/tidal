package net.tidalhq.tidal.pathfind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PathfindingSession {

    private static final int MAX_RECOMPUTES = 3;

    private final AsyncPathfinder pathfinder = new AsyncPathfinder();
    private final PathExecutor executor      = new PathExecutor();

    private final Consumer<PathResult> onFailure;
    private final Runnable onArrival;

    private BlockPos goal;
    private CompletableFuture<PathResult> pendingSearch;
    private int recomputeCount;
    private boolean active;

    public PathfindingSession(Consumer<PathResult> onFailure, Runnable onArrival) {
        this.onFailure = onFailure;
        this.onArrival = onArrival;
    }

    public void navigateTo(BlockPos goal) {
        this.goal           = goal;
        this.recomputeCount = 0;
        this.active         = true;
        requestPath();
    }

    public void stop() {
        active = false;
        executor.stop();
        if (pendingSearch != null) {
            pendingSearch.cancel(false);
            pendingSearch = null;
        }
    }

    public boolean isActive()   { return active; }
    public boolean isFinished() { return executor.isFinished(); }

    public void onTick() {
        if (!active) return;

        if (pendingSearch != null && pendingSearch.isDone()) {
            PathResult result;
            try {
                result = pendingSearch.get();
            } catch (java.util.concurrent.CancellationException e) {
                pendingSearch = null;
                return;
            } catch (Exception e) {
                net.tidalhq.tidal.Tidal.LOGGER.error("[pathfind] Search threw unexpectedly", e);
                pendingSearch = null;
                active = false;
                onFailure.accept(PathResult.failure(PathResult.Status.NO_PATH));
                return;
            }
            pendingSearch = null;

            if (!result.isSuccess()) {
                if (result.getStatus() == PathResult.Status.GOAL_NOT_LOADED) {
                    net.tidalhq.tidal.Tidal.LOGGER.info("[pathfind] Goal not loaded, retrying...");
                    requestPath();
                    return;
                }
                active = false;
                onFailure.accept(result);
                return;
            }

            result.getPath().ifPresent(executor::follow);
        }

        executor.onTick();

        if (executor.isFinished()) {
            active = false;
            onArrival.run();
            return;
        }

        if (executor.isStuck()) {
            if (recomputeCount >= MAX_RECOMPUTES) {
                active = false;
                onFailure.accept(PathResult.failure(PathResult.Status.NO_PATH));
                return;
            }
            recomputeCount++;
            executor.stop();
            requestPath();
        }
    }

    private void requestPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || goal == null) return;

        BlockPos start = client.player.getBlockPos();
        pendingSearch  = pathfinder.findPath(start, goal);
    }

    public double getProgress() {
        int total = executor.getPathLength();
        if (total == 0) return 1.0;
        return (double) executor.getNodeIndex() / total;
    }

    public void shutdown() {
        pathfinder.shutdown();
    }
}