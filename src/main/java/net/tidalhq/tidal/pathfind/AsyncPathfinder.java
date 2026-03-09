package net.tidalhq.tidal.pathfind;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncPathfinder {
    private static final int DEFAULT_MAX_NODES = 8_000;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "tidal-pathfinder");
                t.setDaemon(true);
                return t;
            });

    private final int maxNodes;
    private CompletableFuture<PathResult> pending;

    public AsyncPathfinder() {
        this(DEFAULT_MAX_NODES);
    }

    public AsyncPathfinder(int maxNodes) {
        this.maxNodes = maxNodes;
    }

    public CompletableFuture<PathResult> findPath(BlockPos start, BlockPos goal) {
        if (pending != null && !pending.isDone()) {
            pending.cancel(false);
        }

        ChunkSnapshotWorldView snapshot = ChunkSnapshotWorldView.capture(start, goal);

        pending = CompletableFuture.supplyAsync(() -> {
            AStarPathfinder pathfinder = new AStarPathfinder(snapshot, maxNodes);
            return pathfinder.findPath(start, goal);
        }, executor);

        return pending;
    }

    public boolean isSearching() {
        return pending != null && !pending.isDone();
    }

    public void shutdown() {
        executor.shutdown();
    }
}