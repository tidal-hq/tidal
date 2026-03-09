package net.tidalhq.tidal.world;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class MinecraftWorldAccessor {

    private final MinecraftClient client;

    public MinecraftWorldAccessor(MinecraftClient client) {
        this.client = client;
    }

    public float getPlayerYaw() {
        return client.player.getYaw();
    }

    public boolean isPlayerOnGround() {
        return client.player.isOnGround();
    }

    public double getPlayerX() { return client.player.getX(); }

    public double getPlayerY() { return client.player.getY(); }

    public double getPlayerZ() { return client.player.getZ(); }

    public Block getBlock(BlockPos pos) {
        return client.world.getBlockState(pos).getBlock();
    }
}