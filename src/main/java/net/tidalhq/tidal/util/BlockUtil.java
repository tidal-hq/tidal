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
import net.tidalhq.tidal.Tidal;

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

//    public static float getUnitX(float yaw) {
//        float yaw360 = (yaw % 360 + 360) % 360;
//        if (yaw360 < 45) return 0;
//        else if (yaw360 < 135) return -1f;
//        else if (yaw360 < 225) return 0;
//        else if (yaw360 < 315) return 1f;
//        else return 0;
//    }
//
//    public static float getUnitZ(float yaw) {
//        float yaw360 = (yaw % 360 + 360) % 360;
//        if (yaw360 < 45) return 1f;
//        else if (yaw360 < 135) return 0;
//        else if (yaw360 < 225) return -1f;
//        else if (yaw360 < 315) return 0;
//        else return 1f;
//    }

//    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
//        assert client.player != null;
//
//        return BlockPos.ofFloored(
//                client.player.getX() + getUnitX(yaw) * z + getUnitZ(yaw) * -1 * x,
//                (client.player.getY() % 1 > 0.7 ? Math.ceil(client.player.getY()) : client.player.getY()) + y,
//                client.player.getZ() + getUnitZ(yaw) * z + getUnitX(yaw) * x
//        );
//    }

    public static BlockPos getPlayerFeetPos() {
        assert client.player != null;
        BlockPos pos = client.player.getBlockPos();

        for (int i = 0; i < 3; i++) {
            Block block = getBlock(pos.down(i));
            if (block != Blocks.AIR && block != Blocks.WATER) {
                return pos.down(i).up();
            }
        }
        return pos; // fallback
    }

    public static BlockPos getRelativeBlockPos(float x, float y, float z, float yaw) {
        assert client.player != null;

        Direction facing = Direction.fromHorizontalDegrees(yaw);
        Direction right = facing.rotateYClockwise();

        BlockPos feetPos = getPlayerFeetPos();

        return BlockPos.ofFloored(
                client.player.getX() + facing.getOffsetX() * z + right.getOffsetX() * x,
                feetPos.getY() + y,
                client.player.getZ() + facing.getOffsetZ() * z + right.getOffsetZ() * x
        );
    }

    public static Block getBlock(BlockPos pos) {
        assert client.world != null;
        return client.world.getBlockState(pos).getBlock();
    }

    public static boolean canWalkThrough(BlockPos blockPos, Direction direction) {
        assert client.world != null;
        assert client.player != null;
        World world = client.world;
        PlayerEntity player = client.player;

        var state = world.getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof SignBlock || block instanceof WallSignBlock) {
            return true;
        }
        return canWalkThroughBottom(blockPos, direction) &&
                canWalkThroughAbove(blockPos.up(), direction);
    }

    private static boolean canWalkThroughBottom(BlockPos blockPos, Direction direction) {
        assert client.world != null;
        assert client.player != null;
        World world = client.world;
        PlayerEntity player = client.player;

        var state = world.getBlockState(blockPos);
        Block block = state.getBlock();

        if (player.getY() % 1 >= 0.5 && player.getY() % 1 <= 0.75)
            return true;

        if (initialWalkables.contains(block))
            return true;

        if (block instanceof DoorBlock && direction != null) {
            return canWalkThroughDoor(blockPos, direction);
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

        return state.isAir() || !state.isSolid();
    }

    private static boolean canWalkThroughAbove(BlockPos blockPos, Direction direction) {
        assert client.world != null;
        assert client.player != null;
        World world = client.world;
        PlayerEntity player = client.player;

        var state = world.getBlockState(blockPos);
        Block block = state.getBlock();

        if (block instanceof CarpetBlock)
            return false;

        if (block instanceof DoorBlock && direction != null) {
            return canWalkThroughDoor(blockPos.down(), direction);
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

    public static boolean canWalkThroughDoor(BlockPos blockPos, Direction direction) {
        assert client.world != null;
        assert client.player != null;
        World world = client.world;
        PlayerEntity player = client.player;

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

    public static boolean canWalkForward() {
        assert client.player != null;
        BlockPos forwardPos = getRelativeBlockPos(0, 0, 1, client.player.getYaw());
        return canWalkThrough(forwardPos, Direction.NORTH);
    }

    public static boolean canWalkBackward() {
        assert client.player != null;
        BlockPos backwardPos = getRelativeBlockPos(0, 0, -1, client.player.getYaw());
        return canWalkThrough(backwardPos, Direction.SOUTH);
    }

    public static boolean canWalkLeft() {
        assert client.player != null;
        BlockPos leftPos = getRelativeBlockPos(-1, 0, 0, client.player.getYaw());
        return canWalkThrough(leftPos, Direction.WEST);
    }

    public static boolean canWalkRight() {
        assert client.player != null;
        BlockPos rightPos = getRelativeBlockPos(1, 0, 0, client.player.getYaw());
        return canWalkThrough(rightPos, Direction.EAST);
    }

    public static boolean isAtFarmStart() {
        assert client.world != null;
        assert client.player != null;
        World world = client.world;
        PlayerEntity player = client.player;

        BlockPos behindPos = getRelativeBlockPos(0, -1, -1, player.getYaw());
        Block behindBlock = world.getBlockState(behindPos).getBlock();

        boolean hasFarmlandBehind = isFarmland(behindBlock);

        BlockPos frontGroundPos = getRelativeBlockPos(0, -1, 1, player.getYaw());
        BlockPos frontCropPos = getRelativeBlockPos(0, 0, 1, player.getYaw());
        BlockPos frontAbovePos = getRelativeBlockPos(0, 1, 1, player.getYaw());

        Block frontGroundBlock = world.getBlockState(frontGroundPos).getBlock();
        Block frontCropBlock = world.getBlockState(frontCropPos).getBlock();
        Block frontAboveBlock = world.getBlockState(frontAbovePos).getBlock();

        boolean hasFarmlandAhead = isFarmland(frontGroundBlock);

        boolean hasCropAhead = isCrop(frontCropBlock) ||
                isCrop(frontAboveBlock) ||
                frontCropBlock == Blocks.AIR;

        boolean leftReady = isLeftCropReady();
        boolean rightReady = isRightCropReady();
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
            BlockPos checkPos = getRelativeBlockPos(0, -1, i, player.getYaw());
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
                block == Blocks.MYCELIUM ||
                block == Blocks.PODZOL;
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

    public static boolean isCropReady(BlockPos pos) {
        assert client.world != null;
        World world = client.world;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.MELON || block == Blocks.PUMPKIN) {
            return true;
        }

        if (block == Blocks.MELON_STEM || block == Blocks.PUMPKIN_STEM) {
            return hasFruitNearby(pos, block == Blocks.MELON_STEM);
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
            return isTopOfSugarCane(pos);
        }

        if (block == Blocks.CACTUS) {
            return isTopOfCactus(pos);
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

    private static boolean isTopOfSugarCane(BlockPos pos) {
        assert client.world != null;
        World world = client.world;

        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        if (belowState.isOf(Blocks.SUGAR_CANE) && world.getBlockState(pos.up()).isAir()) {
            BlockPos belowBelowPos = belowPos.down();
            return world.getBlockState(belowBelowPos).isOf(Blocks.SUGAR_CANE);
        }
        return false;
    }

    private static boolean isTopOfCactus(BlockPos pos) {
        assert client.world != null;
        World world = client.world;

        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        if (belowState.isOf(Blocks.CACTUS) && world.getBlockState(pos.up()).isAir()) {
            BlockPos belowBelowPos = belowPos.down();
            return world.getBlockState(belowBelowPos).isOf(Blocks.CACTUS);
        }
        return false;
    }

    private static boolean hasFruitNearby(BlockPos stemPos, boolean isMelon) {
        assert client.world != null;
        World world = client.world;

        Block fruitType = isMelon ? Blocks.MELON : Blocks.PUMPKIN;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos checkPos = stemPos.offset(dir);
            if (world.getBlockState(checkPos).getBlock() == fruitType) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSideCropReady(boolean checkLeft) {
        assert client.player != null;
        PlayerEntity player = client.player;

        int checkDistance = 3;
        float xOffset = checkLeft ? -1 : 1;

        for (int i = 1; i <= checkDistance; i++) {
            BlockPos checkPos = getRelativeBlockPos(xOffset, 0, i, player.getYaw());

            if (isCropReady(checkPos)) {
                return true;
            }

            BlockPos abovePos = checkPos.up();
            if (isCropReady(abovePos)) {
                return true;
            }

            BlockPos aboveAbovePos = checkPos.up(2);
            if (isCropReady(aboveAbovePos)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isLeftCropReady() {
        return isSideCropReady(true);
    }

    public static boolean isRightCropReady() {
        return isSideCropReady(false);
    }
}