package net.tidalhq.tidal.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.pathfinder.RotationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;
        if (Tidal.getMacroManager().isEnabled()) ci.cancel();
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;
        if (Tidal.getMacroManager().isEnabled()) ci.cancel();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double x, double y, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) return;
        if (Tidal.getMacroManager().isEnabled()) ci.cancel();
    }

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void injectSyntheticRotation(CallbackInfo ci) {
        RotationController rc = Tidal.getMacroManager().getActiveRotation();
        if (rc == null || !rc.hasTarget()) return;

        MouseAccessor self = (MouseAccessor) this;
        self.setCursorDeltaX(rc.consumeDeltaX());
        self.setCursorDeltaY(rc.consumeDeltaY());
    }
}