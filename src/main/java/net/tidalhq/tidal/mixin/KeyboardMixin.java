package net.tidalhq.tidal.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.tidalhq.tidal.Tidal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.currentScreen instanceof ChatScreen) {
            return;
        }

        if (Tidal.getMacroManager().isEnabled()) {
            if (client.options.chatKey.matchesKey(input) ||
                    client.options.commandKey.matchesKey(input) ||
                    client.options.screenshotKey.matchesKey(input) ||
                    client.options.playerListKey.matchesKey(input) ||
                    input.isEscape() ||
                    input.isEnter() ||
                    Arrays.stream(client.options.debugKeys).anyMatch(debugKey -> debugKey.matchesKey(input))) {
                return;
            }

            ci.cancel();
        }
    }
}
