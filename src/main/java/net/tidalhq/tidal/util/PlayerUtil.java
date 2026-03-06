package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.tidalhq.tidal.state.Location;

public class PlayerUtil {
    private static MinecraftClient client = MinecraftClient.getInstance();

    public static void warp(Location to) {
        String command = "warp " + to.getName();

        client.getNetworkHandler().sendChatCommand(command);
    }
}
