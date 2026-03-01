package net.tidalhq.tidal.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import net.tidalhq.tidal.state.TablistState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "setHeader", at = @At("TAIL"))
    private void onHeaderSet(Text header, CallbackInfo ci) {
        TablistState.getInstance().onTablistUpdate();
    }

    @Inject(method = "setFooter", at = @At("TAIL"))
    private void onFooterSet(Text footer, CallbackInfo ci) {
        TablistState.getInstance().onTablistUpdate();
    }
}
