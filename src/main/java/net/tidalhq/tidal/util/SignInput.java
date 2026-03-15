package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.tidalhq.tidal.mixin.SignEditScreenAccessor;

public class SignInput {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void setAndConfirm(String text) {
        if (!(client.currentScreen instanceof AbstractSignEditScreen screen)) {
            return;
        }
        if (client.player == null) {
            return;
        }

        SignEditScreenAccessor accessor = (SignEditScreenAccessor) screen;
        var pos   = accessor.getBlockEntity().getPos();
        var front = accessor.isFront();

        client.player.networkHandler.sendPacket(new UpdateSignC2SPacket(pos, front, text, "", "", ""));
        client.setScreen(null);
    }
}