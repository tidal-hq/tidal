package net.tidalhq.tidal.pathfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class WalkPathfinder {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final int[][] DIRS = {
            { 1,  0}, {-1,  0}, { 0,  1}, { 0, -1},
            { 1,  1}, { 1, -1}, {-1,  1}, {-1, -1}
    };

    private static final int MAX_NODES = 8192;
    private static final int MAX_FALL  = 5;

    private record Node(BlockPos pos, Node parent, double g, double f) {}

    public static int jumpHeight() {
        if (client.player == null) return 1;
        var effect = client.player.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (effect == null) return 1;
        return 1 + effect.getAmplifier() + 1;
    }

    public static List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        if (client.world == null) return Collections.emptyList();

        int maxClimb = jumpHeight();

        PriorityQueue<Node> open   = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<BlockPos, Double> bestG = new HashMap<>();
        Set<BlockPos>         closed = new HashSet<>();

        open.add(new Node(start, null, 0, heuristic(start, goal)));
        bestG.put(start, 0.0);

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < MAX_NODES) {
            Node current = open.poll();

            if (current.pos().equals(goal)) return reconstructPath(current);
            if (closed.contains(current.pos())) continue;
            closed.add(current.pos());

            for (BlockPos neighbour : neighbours(current.pos(), maxClimb)) {
                if (closed.contains(neighbour)) continue;

                double stepCost = isDiagonal(current.pos(), neighbour) ? 1.414 : 1.0;
                int dy = neighbour.getY() - current.pos().getY();
                if (dy > 0) stepCost += 0.4 * dy;
                if (dy < 0) stepCost += 0.1 * (-dy);

                double ng = current.g() + stepCost;
                if (ng >= bestG.getOrDefault(neighbour, Double.MAX_VALUE)) continue;

                bestG.put(neighbour, ng);
                open.add(new Node(neighbour, current, ng, ng + heuristic(neighbour, goal)));
            }
        }

        return Collections.emptyList();
    }

    private static List<BlockPos> neighbours(BlockPos pos, int maxClimb) {
        List<BlockPos> result = new ArrayList<>(48);

        for (int[] d : DIRS) {
            for (int rise = 1; rise <= maxClimb; rise++) {
                BlockPos candidate = pos.add(d[0], rise, d[1]);
                if (!hasHeadroom(pos, rise)) break;
                tryAdd(result, pos, candidate, maxClimb);
            }

            tryAdd(result, pos, pos.add(d[0], 0, d[1]), maxClimb);

            for (int fall = 1; fall <= MAX_FALL; fall++) {
                BlockPos candidate = pos.add(d[0], -fall, d[1]);
                if (isWalkable(candidate)) {
                    tryAdd(result, pos, candidate, maxClimb);
                    break;
                }
                if (isSolid(candidate)) break;
            }
        }

        return result;
    }

    private static boolean hasHeadroom(BlockPos feet, int rise) {
        for (int y = 1; y <= rise; y++) {
            if (!isPassable(feet.up(y)) || !isPassable(feet.up(y + 1))) return false;
        }
        return true;
    }

    private static void tryAdd(List<BlockPos> list, BlockPos from, BlockPos to, int maxClimb) {
        if (!isWalkable(to)) return;

        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int dy = to.getY() - from.getY();

        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
            if (!isPassable(from.add(dx, dy, 0)) || !isPassable(from.add(0, dy, dz))) return;
        }

        list.add(to);
    }

    private static boolean isWalkable(BlockPos feet) {
        if (client.world == null) return false;
        var floorState = client.world.getBlockState(feet.down());
        if (floorState.isAir()) return false;
        if (!floorState.isSolid()
                && !floorState.isOf(net.minecraft.block.Blocks.SOUL_SAND)
                && !floorState.isOf(net.minecraft.block.Blocks.SOUL_SOIL)) return false;
        return isPassable(feet) && isPassable(feet.up());
    }

    private static boolean isPassable(BlockPos pos) {
        if (client.world == null) return false;
        var state = client.world.getBlockState(pos);
        return state.isAir() || !state.isSolid();
    }

    private static boolean isSolid(BlockPos pos) {
        if (client.world == null) return false;
        return client.world.getBlockState(pos).isSolid();
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dz = Math.abs(a.getZ() - b.getZ());
        double dy = Math.abs(a.getY() - b.getY());
        return (dx + dz) + (1.414 - 2) * Math.min(dx, dz) + dy;
    }

    private static boolean isDiagonal(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) == 1 && Math.abs(a.getZ() - b.getZ()) == 1;
    }

    private static List<BlockPos> reconstructPath(Node node) {
        LinkedList<BlockPos> path = new LinkedList<>();
        Node n = node;
        while (n.parent() != null) {
            path.addFirst(n.pos());
            n = n.parent();
        }
        return path;
    }
}