package net.tidalhq.tidal.pathfind;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public final class Path {

    private final List<PathNode> nodes;
    private final BlockPos start;
    private final BlockPos goal;

    Path(List<PathNode> nodes, BlockPos start, BlockPos goal) {
        this.nodes = Collections.unmodifiableList(nodes);
        this.start = start;
        this.goal  = goal;
    }

    public List<PathNode> getNodes() { return nodes; }

    public BlockPos getStart() { return start; }

    public BlockPos getGoal() { return goal; }

    public int length() { return nodes.size(); }

    public boolean isEmpty() { return nodes.isEmpty(); }

    @Override
    public String toString() {
        return "Path{start=" + start + ", goal=" + goal + ", steps=" + nodes.size() + "}";
    }
}