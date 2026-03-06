package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
public class InputUtil {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final KeyBinding[] MOVEMENT_KEYS = {
            client.options.forwardKey,
            client.options.backKey,
            client.options.leftKey,
            client.options.rightKey,
            client.options.jumpKey,
            client.options.sneakKey,
            client.options.attackKey,
            client.options.useKey
    };

    public static void reset() {
        for (KeyBinding key : MOVEMENT_KEYS) {
            key.setPressed(false);
        }
    }

    public static void press(KeyBinding... keys) {
        for (KeyBinding key : keys) {
            key.setPressed(true);
        }
    }

    public static void release(KeyBinding... keys) {
        for (KeyBinding key : keys) {
            key.setPressed(false);
        }
    }

    public static void sneak()   { client.options.sneakKey.setPressed(true); }
    public static void unSneak() { client.options.sneakKey.setPressed(false); }
}