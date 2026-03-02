package net.tidalhq.tidal.util;

import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class BlockUtil {
    private static final List<Block> initialWalkables = Arrays.asList(
            Blocks.AIR, Blocks.WATER, Blocks.KELP, Blocks.KELP_PLANT,
            Blocks.SEAGRASS, Blocks.TALL_SEAGRASS
    );

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static float getUnitX(float yaw) {
        float yaw360 = (yaw % 360 + 360) % 360;
        if (yaw360 < 30) return 0;
        else if (yaw360 < 150) return -1f;
        else if (yaw360 < 210) return 0;
        else if (yaw360 < 330) return 1f;
        else return 0;
    }

    public static float getUnitZ(float yaw) {
        float yaw360 = (yaw % 360 + 360) % 360;
        if (yaw360 < 60) return 1f;
        else if (yaw360 < 120) return 0;
        else if (yaw360 < 240) return -1f;
        else if (yaw360 < 300) return 0;
        else return 1f;
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        assert client.player != null;

        return BlockPos.ofFloored(
                client.player.getX() + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (client.player.getY() % 1 > 0.7 ? Math.ceil(client.player.getY()) : client.player.getY()) + y,
                client.player.getZ() + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
    }

    // Overloaded method that takes a PlayerEntity parameter
    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw, PlayerEntity player) {
        return BlockPos.ofFloored(
                player.getX() + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
                (player.getY() % 1 > 0.7 ? Math.ceil(player.getY()) : player.getY()) + y,
                player.getZ() + getUnitZ(yaw) * z + getUnitX(yaw) * x
        );
    }

    public static Block getBlock(BlockPos pos) {
        assert client.world != null;
        return client.world.getBlockState(pos).getBlock();
    }

    public static boolean canWalkThrough(World world, BlockPos blockPos, PlayerEntity player, Direction direction) {
        var state =  world.getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof SignBlock || block instanceof WallSignBlock) {
            return true;
        }
        return canWalkThroughBottom(world, blockPos, player, direction) &&
                canWalkThroughAbove(world, blockPos.up(), player, direction);
    }

    private static boolean canWalkThroughBottom(World world, BlockPos blockPos, PlayerEntity player, Direction direction) {
        var state = world.getBlockState(blockPos);
        Block block = state.getBlock();

        if (player.getY() % 1 >= 0.5 && player.getY() % 1 <= 0.75)
            return true;

        if (initialWalkables.contains(block))
            return true;

        if (block instanceof DoorBlock && direction != null) {
            return canWalkThroughDoor(world, blockPos, player, direction);
        }

        if (block instanceof FenceBlock || block instanceof FenceGateBlock)
            return block instanceof FenceGateBlock && state.get(FenceGateBlock.OPEN);

        if (block instanceof TrapdoorBlock) {
            return state.get(TrapdoorBlock.OPEN) || state.get(TrapdoorBlock.HALF) == net.minecraft.block.enums.BlockHalf.BOTTOM;
        }

        if (block == Blocks.SNOW) {
            return state.get(SnowBlock.LAYERS) <= 5;
        }

        if (block instanceof SlabBlock) {
            if (player.getY() % 1 < 0.5) {
                if (state.get(SlabBlock.TYPE) == SlabType.DOUBLE) return false;
                return state.get(SlabBlock.TYPE) == SlabType.BOTTOM;
            }
        }

        if (block instanceof CarpetBlock)
            return true;

        if (block instanceof StairsBlock) {
            Direction facing = state.get(StairsBlock.FACING);
            BlockPos posDiff = blockPos.subtract(BlockPos.ofFloored(player.getEntityPos()));
            if (state.get(StairsBlock.HALF) == net.minecraft.block.enums.BlockHalf.TOP)
                return false;
            if (facing == Direction.NORTH && posDiff.getZ() < 0) return true;
            if (facing == Direction.SOUTH && posDiff.getZ() > 0) return true;
            if (facing == Direction.WEST && posDiff.getX() < 0) return true;
            return facing == Direction.EAST && posDiff.getX() > 0;
        }

        // Check if passable (air, water, etc)
        return state.isAir() || !state.isSolid();
    }

    private static boolean canWalkThroughAbove(World world, BlockPos blockPos, PlayerEntity player, Direction direction) {
        var state = world.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof CarpetBlock)
            return false;

        if (block instanceof DoorBlock && direction != null) {
            return canWalkThroughDoor(world, blockPos.down(), player, direction);
        }

        if (block instanceof FenceBlock || block instanceof FenceGateBlock)
            return block instanceof FenceGateBlock && state.get(FenceGateBlock.OPEN);

        if (block instanceof TrapdoorBlock) {
            Direction playerFacing = player.getHorizontalFacing();
            Direction doorFacing = state.get(TrapdoorBlock.FACING);
            boolean standingOnDoor = false;

            if (state.get(TrapdoorBlock.OPEN)) {
                return canWalkThroughDoorWithDirection(direction, playerFacing, doorFacing, standingOnDoor);
            } else {
                return state.get(TrapdoorBlock.HALF) == net.minecraft.block.enums.BlockHalf.TOP;
            }
        }

        return state.isAir() || !state.isSolid();
    }

    public static boolean canWalkThroughDoor(World world, BlockPos blockPos, PlayerEntity player, Direction direction) {
        Block block = world.getBlockState(blockPos).getBlock();
        if (!(block instanceof DoorBlock)) return true;

        Direction playerFacing = player.getHorizontalFacing();
        Direction doorFacing = world.getBlockState(blockPos).get(DoorBlock.FACING);
        boolean standingOnDoor = false;

        return canWalkThroughDoorWithDirection(direction, playerFacing, doorFacing, standingOnDoor);
    }

    private static boolean canWalkThroughDoorWithDirection(Direction direction, Direction playerFacing,
                                                           Direction doorFacing, boolean standingOnDoor) {
        switch (direction) {
            case NORTH: // Forward
                if (doorFacing == playerFacing.getOpposite() && standingOnDoor) return false;
                if (!standingOnDoor && doorFacing == playerFacing) return false;
                break;
            case SOUTH: // Backward
                if (doorFacing == playerFacing && standingOnDoor) return false;
                if (!standingOnDoor && doorFacing == playerFacing.getOpposite()) return false;
                break;
            case WEST: // Left
                if (doorFacing == playerFacing.rotateYClockwise() && standingOnDoor) return false;
                if (!standingOnDoor && doorFacing == playerFacing.rotateYCounterclockwise()) return false;
                break;
            case EAST: // Right
                if (doorFacing == playerFacing.rotateYCounterclockwise() && standingOnDoor) return false;
                if (!standingOnDoor && doorFacing == playerFacing.rotateYClockwise()) return false;
                break;
        }
        return true;
    }

    // Helper methods for checking specific directions
    public static boolean canWalkForward(World world, PlayerEntity player) {
        BlockPos forwardPos = getRelativeBlockPos(0, 0, 1, player.getYaw(), player);
        return canWalkThrough(world, forwardPos, player, Direction.NORTH);
    }

    public static boolean canWalkBackward(World world, PlayerEntity player) {
        BlockPos backwardPos = getRelativeBlockPos(0, 0, -1, player.getYaw(), player);
        return canWalkThrough(world, backwardPos, player, Direction.SOUTH);
    }

    public static boolean canWalkLeft(World world, PlayerEntity player) {
        BlockPos leftPos = getRelativeBlockPos(-1, 0, 0, player.getYaw(), player);
        return canWalkThrough(world, leftPos, player, Direction.WEST);
    }

    public static boolean canWalkRight(World world, PlayerEntity player) {
        BlockPos rightPos = getRelativeBlockPos(1, 0, 0, player.getYaw(), player);
        return canWalkThrough(world, rightPos, player, Direction.EAST);
    }
}