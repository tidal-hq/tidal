package net.tidalhq.tidal.mixin;


import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.tidalhq.tidal.state.TablistState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void onPlayerListPacket(PlayerListS2CPacket packet, CallbackInfo ci) {
        TablistState.getInstance().onPlayerListUpdate();
    }
}
