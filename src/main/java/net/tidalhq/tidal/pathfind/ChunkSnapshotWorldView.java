package net.tidalhq.tidal.pathfind;

import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ChunkSnapshotWorldView {

    private static final int PADDING          = 16;
    private static final int VERTICAL_PADDING = 4;

    private final Map<Long, BlockState> states;

    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    private ChunkSnapshotWorldView(
            Map<Long, BlockState> states,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        this.states = states;
        this.minX   = minX;
        this.minY   = minY;
        this.minZ   = minZ;
        this.maxX   = maxX;
        this.maxY   = maxY;
        this.maxZ   = maxZ;
    }
    public String getBlockName(BlockPos pos) {
        BlockState state = getState(pos);
        return state == null ? "null" : state.getBlock().toString();
    }
    public static ChunkSnapshotWorldView capture(BlockPos start, BlockPos goal) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = client.world;
        if (world == null) throw new IllegalStateException("Cannot capture snapshot: world is null");

        int minX = Math.min(start.getX(), goal.getX()) - PADDING;
        int minY = Math.min(start.getY(), goal.getY()) - VERTICAL_PADDING;
        int minZ = Math.min(start.getZ(), goal.getZ()) - PADDING;
        int maxX = Math.max(start.getX(), goal.getX()) + PADDING;
        int maxY = Math.max(start.getY(), goal.getY()) + VERTICAL_PADDING;
        int maxZ = Math.max(start.getZ(), goal.getZ()) + PADDING;

        int volume = (maxX - minX + 1) * (maxY - minY + 4) * (maxZ - minZ + 1);
        Map<Long, BlockState> states = new HashMap<>(volume * 2);


        for (int x = minX; x <= maxX; x++) {
            for (int y = minY - 1; y <= maxY + 2; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    states.put(pos.asLong(), world.getBlockState(pos));
                }
            }
        }

        return new ChunkSnapshotWorldView(states, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean canWalkThrough(BlockPos pos, Direction direction) {
        BlockState footState = getState(pos);
        BlockState headState = getState(pos.up());
        if (footState == null || headState == null) return false; // outside snapshot = impassable

        Block foot = footState.getBlock();
        Block head = headState.getBlock();

        if (foot instanceof SignBlock || foot instanceof WallSignBlock) {
            return isHeadClear(headState);
        }

        return isFootClear(footState, direction) && isHeadClear(headState);
    }

    public boolean isStandable(BlockPos pos) {
        BlockState floorState = getState(pos.down());
        if (floorState == null) return false;

        Block floor = floorState.getBlock();
        if (isNonSurface(floor)) return false;

        return canWalkThrough(pos, null);
    }

    public boolean isInSnapshot(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private static boolean isNonSurface(Block block) {
        return block == Blocks.AIR
                || block == Blocks.VOID_AIR
                || block == Blocks.CAVE_AIR
                || block == Blocks.WATER
                || block == Blocks.LAVA
                || block == Blocks.TALL_GRASS
                || block == Blocks.SHORT_GRASS
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == Blocks.DEAD_BUSH
                || block == Blocks.SEAGRASS
                || block == Blocks.TALL_SEAGRASS
                || block == Blocks.KELP
                || block == Blocks.KELP_PLANT
                || block instanceof FlowerBlock
                || block instanceof TallPlantBlock;
    }

    public boolean isDangerous(BlockPos pos) {
        BlockState state = getState(pos);
        if (state == null) return false;
        Block block = state.getBlock();
        return block == Blocks.LAVA
                || block == Blocks.FIRE
                || block == Blocks.SOUL_FIRE
                || block == Blocks.MAGMA_BLOCK
                || block == Blocks.CACTUS
                || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.WITHER_ROSE;
    }

    private BlockState getState(BlockPos pos) {
        return states.get(pos.asLong());
    }

    private boolean isFootClear(BlockState state, Direction direction) {
        Block block = state.getBlock();

        if (isPassableVoid(block)) return true;

        if (block instanceof DoorBlock && direction != null) {
            return state.get(DoorBlock.OPEN);
        }
        if (block instanceof FenceGateBlock) {
            return state.get(FenceGateBlock.OPEN);
        }
        if (block instanceof FenceBlock) return false;
        if (block instanceof WallBlock)  return false;

        if (block instanceof TrapdoorBlock) {
            return state.get(TrapdoorBlock.OPEN)
                    || state.get(TrapdoorBlock.HALF) == BlockHalf.BOTTOM;
        }

        if (block instanceof SlabBlock) {
            return state.get(SlabBlock.TYPE) == SlabType.BOTTOM;
        }

        if (block instanceof CarpetBlock) return true;
        if (block instanceof SnowBlock)   return state.get(SnowBlock.LAYERS) <= 5;

        return state.isAir() || !state.isSolid();
    }

    private boolean isHeadClear(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CarpetBlock)   return false;
        if (block instanceof FenceBlock)    return false;
        if (block instanceof WallBlock)     return false;
        if (block instanceof FenceGateBlock) return state.get(FenceGateBlock.OPEN);
        if (block instanceof TrapdoorBlock) {
            return state.get(TrapdoorBlock.OPEN)
                    || state.get(TrapdoorBlock.HALF) == BlockHalf.TOP;
        }
        return state.isAir() || !state.isSolid();
    }

    private static boolean isPassableVoid(Block block) {
        return block == Blocks.AIR
                || block == Blocks.WATER
                || block == Blocks.KELP
                || block == Blocks.KELP_PLANT
                || block == Blocks.SEAGRASS
                || block == Blocks.TALL_SEAGRASS;
    }
}