package net.tidalhq.tidal.pathfind;

import net.minecraft.util.math.BlockPos;

public final class PathNode {
    public enum MoveType {
        WALK,
        JUMP,
        DESCEND,
    }

    private final BlockPos pos;
    private final MoveType moveType;

    public PathNode(BlockPos pos, MoveType moveType) {
        this.pos      = pos;
        this.moveType = moveType;
    }

    public BlockPos getPos() { return pos; }

    public MoveType getMoveType() { return moveType; }

    public double worldX() { return pos.getX() + 0.5; }

    public double worldY() { return pos.getY(); }

    public double worldZ() { return pos.getZ() + 0.5; }

    @Override
    public String toString() {
        return "PathNode{" + pos + ", " + moveType + "}";
    }
}