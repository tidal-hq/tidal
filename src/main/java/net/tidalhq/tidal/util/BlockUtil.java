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

    public static boolean isAtFarmStart(World world, PlayerEntity player) {
        BlockPos behindPos = getRelativeBlockPos(0, -1, -1, player.getYaw(), player);
        Block behindBlock = world.getBlockState(behindPos).getBlock();

        boolean hasFarmlandBehind = isFarmland(behindBlock);

        BlockPos frontGroundPos = getRelativeBlockPos(0, -1, 1, player.getYaw(), player);
        BlockPos frontCropPos = getRelativeBlockPos(0, 0, 1, player.getYaw(), player);
        BlockPos frontAbovePos = getRelativeBlockPos(0, 1, 1, player.getYaw(), player);

        Block frontGroundBlock = world.getBlockState(frontGroundPos).getBlock();
        Block frontCropBlock = world.getBlockState(frontCropPos).getBlock();
        Block frontAboveBlock = world.getBlockState(frontAbovePos).getBlock();

        boolean hasFarmlandAhead = isFarmland(frontGroundBlock);

        boolean hasCropAhead = isCrop(frontCropBlock) ||
                isCrop(frontAboveBlock) ||
                frontCropBlock == Blocks.AIR;

        boolean leftReady = isLeftCropReady(world, player);
        boolean rightReady = isRightCropReady(world, player);
        boolean anySideReady = leftReady || rightReady;

        BlockPos standingOnPos = BlockPos.ofFloored(player.getX(), player.getY() - 0.5, player.getZ());
        Block standingOnBlock = world.getBlockState(standingOnPos).getBlock();
        boolean standingOnFarmland = isFarmland(standingOnBlock);

        boolean isOnPath = standingOnBlock instanceof SlabBlock ||
                standingOnBlock instanceof SoulSandBlock;
        boolean gapBehind = !hasFarmlandBehind && !isOnPath;

        boolean hasWaterAhead = false;
        boolean hasWaterBehind = false;

        for (int i = -1; i <= 1; i++) {
            BlockPos checkPos = getRelativeBlockPos(0, -1, i, player.getYaw(), player);
            if (world.getBlockState(checkPos).getBlock() == Blocks.WATER ||
                    world.getBlockState(checkPos).getBlock() == Blocks.BUBBLE_COLUMN) {
                if (i < 0) hasWaterBehind = true;
                if (i > 0) hasWaterAhead = true;
            }
        }

        if (hasFarmlandBehind && !anySideReady && hasFarmlandAhead) {
            return true;
        }

        if (gapBehind && hasFarmlandAhead && standingOnFarmland) {
            return true;
        }

        if (hasWaterBehind && !hasWaterAhead && hasFarmlandAhead) {
            return true;
        }

        if (standingOnFarmland && !anySideReady && hasFarmlandAhead && hasCropAhead) {
            return true;
        }

        if (isFarmland(frontGroundBlock)) {
            if (gapBehind && standingOnFarmland) {
                return true;
            }
        }

        return false;
    }

    private static boolean isFarmland(Block block) {
        return block instanceof FarmlandBlock ||
                block == Blocks.SOUL_SAND ||
                block == Blocks.SOUL_SOIL ||
                block == Blocks.MYCELIUM;
    }

    private static boolean isCrop(Block block) {
        return block == Blocks.MELON ||
                block == Blocks.MELON_STEM ||
                block == Blocks.PUMPKIN ||
                block == Blocks.PUMPKIN_STEM ||
                block == Blocks.WHEAT ||
                block == Blocks.POTATOES ||
                block == Blocks.CARROTS ||
                block == Blocks.SUGAR_CANE ||
                block == Blocks.CACTUS ||
                block == Blocks.COCOA ||
                block == Blocks.NETHER_WART ||
                block == Blocks.SUNFLOWER ||
                block == Blocks.ROSE_BUSH ||
                block == Blocks.BROWN_MUSHROOM ||
                block == Blocks.RED_MUSHROOM ||
                block instanceof CropBlock;
    }

    public static boolean isCropReady(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return true;
        }

        if (block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM) {
            return hasFruitNearby(world, pos, block == Blocks.MELON_STEM);
        }

        if (block instanceof CropBlock crop) {
            return crop.isMature(state);
        }

        if (block instanceof NetherWartBlock) {
            return state.get(NetherWartBlock.AGE) >= 3;
        }

        if (block instanceof CocoaBlock) {
            return state.get(CocoaBlock.AGE) >= 2;
        }

        if (block == Blocks.SUGAR_CANE) {
            return isTopOfSugarCane(world, pos);
        }

        if (block == Blocks.CACTUS) {
            return isTopOfCactus(world, pos);
        }

        if (block == Blocks.SUNFLOWER ||
                block == Blocks.ROSE_BUSH) {
            return true;
        }

        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
            return true;
        }

        return false;
    }

    private static boolean isTopOfSugarCane(World world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        if (belowState.isOf(Blocks.SUGAR_CANE) && world.getBlockState(pos.up()).isAir()) {
            BlockPos belowBelowPos = belowPos.down();
            return world.getBlockState(belowBelowPos).isOf(Blocks.SUGAR_CANE);
        }
        return false;
    }

    private static boolean isTopOfCactus(World world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        if (belowState.isOf(Blocks.CACTUS) && world.getBlockState(pos.up()).isAir()) {
            BlockPos belowBelowPos = belowPos.down();
            return world.getBlockState(belowBelowPos).isOf(Blocks.CACTUS);
        }
        return false;
    }

    private static boolean hasFruitNearby(World world, BlockPos stemPos, boolean isMelon) {
        Block fruitType = isMelon ? Blocks.MELON : Blocks.PUMPKIN;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos checkPos = stemPos.offset(dir);
            if (world.getBlockState(checkPos).getBlock() == fruitType) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSideCropReady(World world, PlayerEntity player, boolean checkLeft) {
        int checkDistance = 3;
        float xOffset = checkLeft ? -1 : 1;

        for (int i = 1; i <= checkDistance; i++) {
            BlockPos checkPos = getRelativeBlockPos(xOffset, 0, i, player.getYaw(), player);

            if (isCropReady(world, checkPos)) {
                return true;
            }

            BlockPos abovePos = checkPos.up();
            if (isCropReady(world, abovePos)) {
                return true;
            }

            BlockPos aboveAbovePos = checkPos.up(2);
            if (isCropReady(world, aboveAbovePos)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isLeftCropReady(World world, PlayerEntity player) {
        return isSideCropReady(world, player, true);
    }

    public static boolean isRightCropReady(World world, PlayerEntity player) {
        return isSideCropReady(world, player, false);
    }
}