package net.tidalhq.tidal.pathfind;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;


public class AStarPathfinder {

    private static final double COST_WALK    = 1.0;
    private static final double COST_JUMP    = 1.5;
    private static final double COST_DESCEND = 1.2;
    private static final double COST_FALL    = 1.4;

    private static final int MAX_FALL_DISTANCE = 3;

    private final ChunkSnapshotWorldView world;
    private final int maxNodes;

    public AStarPathfinder(ChunkSnapshotWorldView world, int maxNodes) {
        this.world    = world;
        this.maxNodes = maxNodes;
    }

    public PathResult findPath(BlockPos start, BlockPos goal) {
        if (world instanceof ChunkSnapshotWorldView snap && !snap.isInSnapshot(goal)) {
            return PathResult.failure(PathResult.Status.GOAL_NOT_LOADED);
        }

        if (!world.isStandable(goal)) {
            if (world instanceof ChunkSnapshotWorldView snap) {
                net.tidalhq.tidal.Tidal.LOGGER.info(
                        "[pathfind] GOAL_SOLID at {} — floor={} foot={} head={}",
                        goal,
                        snap.getBlockName(goal.down()),
                        snap.getBlockName(goal),
                        snap.getBlockName(goal.up())
                );
            }
            return PathResult.failure(PathResult.Status.GOAL_SOLID);
        }
        if (start.equals(goal)) {
            return PathResult.success(new Path(List.of(), start, goal));
        }

        PriorityQueue<AStarNode> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Long, AStarNode> allNodes = new HashMap<>();

        AStarNode startNode = new AStarNode(start, null, PathNode.MoveType.WALK, 0.0, heuristic(start, goal));
        open.add(startNode);
        allNodes.put(start.asLong(), startNode);

        int expanded = 0;

        while (!open.isEmpty()) {
            if (expanded++ >= maxNodes) {
                return PathResult.failure(PathResult.Status.TIMED_OUT);
            }

            AStarNode current = open.poll();
            current.closed = true;

            if (current.pos.equals(goal)) {
                return PathResult.success(reconstructPath(current, start, goal));
            }

            for (Successor successor : getSuccessors(current.pos)) {
                long key = successor.pos.asLong();
                AStarNode existing = allNodes.get(key);

                double tentativeG = current.g + successor.cost;

                if (existing == null) {
                    AStarNode next = new AStarNode(
                            successor.pos,
                            current,
                            successor.moveType,
                            tentativeG,
                            heuristic(successor.pos, goal)
                    );
                    allNodes.put(key, next);
                    open.add(next);
                } else if (!existing.closed && tentativeG < existing.g) {
                    existing.g       = tentativeG;
                    existing.f       = tentativeG + heuristic(existing.pos, goal);
                    existing.parent  = current;
                    existing.moveType = successor.moveType;
                    open.add(existing);
                }
            }
        }

        return PathResult.failure(PathResult.Status.NO_PATH);
    }

    private List<Successor> getSuccessors(BlockPos pos) {
        List<Successor> successors = new ArrayList<>(6);

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos flat = pos.offset(dir);

            if (world.isStandable(flat) && world.canWalkThrough(flat, dir) && !world.isDangerous(flat)) {
                successors.add(new Successor(flat, PathNode.MoveType.WALK, COST_WALK));
            }

            BlockPos jumpUp = flat.up();
            if (world.isStandable(jumpUp)
                    && world.canWalkThrough(jumpUp, dir)
                    && world.canWalkThrough(pos.up(), dir)
                    && !world.isDangerous(jumpUp)) {
                successors.add(new Successor(jumpUp, PathNode.MoveType.JUMP, COST_JUMP));
            }

            BlockPos stepDown = flat.down();
            if (world.isStandable(stepDown)
                    && world.canWalkThrough(stepDown, dir)
                    && !world.isDangerous(stepDown)) {
                successors.add(new Successor(stepDown, PathNode.MoveType.DESCEND, COST_DESCEND));
            }

            for (int fallDist = 2; fallDist <= MAX_FALL_DISTANCE; fallDist++) {
                BlockPos landing = flat.down(fallDist);
                if (world.isStandable(landing) && !world.isDangerous(landing)) {
                    if (world.canWalkThrough(flat, dir)) {
                        double fallCost = COST_FALL + (fallDist - 2) * 0.5;
                        successors.add(new Successor(landing, PathNode.MoveType.DESCEND, fallCost));
                    }
                    break;
                }
                if (!world.canWalkThrough(landing, dir)) break;
            }
        }

        return successors;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dz = Math.abs(a.getZ() - b.getZ());
        // Octile: straight + diagonal component (but we don't use diagonals,
        // so Chebyshev = max(dx,dz) is tighter and still admissible).
        return Math.max(dx, dz);
    }

    private static Path reconstructPath(AStarNode goal, BlockPos start, BlockPos goalPos) {
        List<PathNode> nodes = new ArrayList<>();
        AStarNode current = goal;

        while (current.parent != null) {
            nodes.add(new PathNode(current.pos, current.moveType));
            current = current.parent;
        }

        Collections.reverse(nodes);
        return new Path(nodes, start, goalPos);
    }
    private static final class AStarNode {
        final BlockPos pos;
        AStarNode parent;
        PathNode.MoveType moveType;
        double g; // cost from start
        double f; // g + h
        boolean closed;

        AStarNode(BlockPos pos, AStarNode parent, PathNode.MoveType moveType, double g, double h) {
            this.pos      = pos;
            this.parent   = parent;
            this.moveType = moveType;
            this.g        = g;
            this.f        = g + h;
        }
    }

    private record Successor(BlockPos pos, PathNode.MoveType moveType, double cost) {}
}