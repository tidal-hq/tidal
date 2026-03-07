package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class InputUtil {
    @SuppressWarnings("resource")
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final KeyBinding[] MOVEMENT_KEYS = {
            client.options.attackKey,
            client.options.useKey,
            client.options.backKey,
            client.options.forwardKey,
            client.options.leftKey,
            client.options.rightKey,
            client.options.jumpKey,
            client.options.sneakKey,
            client.options.sprintKey
    };

    public static void press(KeyBinding... keys) {
        for (KeyBinding key : keys) {
            if (key == null) return;
            if (!key.isPressed()) {
                KeyBinding.onKeyPressed(key.getDefaultKey());
                key.setPressed(true);
            }
        }
    }

    public static void release(KeyBinding... keys) {
        for (KeyBinding key : keys) {
            if (key == null) return;
            if (key.isPressed()) {
                key.setPressed(false);
            }
        }
    }

    public static void reset() {
        release(MOVEMENT_KEYS);
    }

    public static void sneak()   { press(client.options.sneakKey); }
    public static void unSneak() { release(client.options.sneakKey); }
}