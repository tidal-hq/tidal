package net.tidalhq.tidal.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.impl.PlayerListFooterSetEvent;
import net.tidalhq.tidal.event.impl.PlayerListHeaderSetEvent;
import net.tidalhq.tidal.state.TablistState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "setHeader", at = @At("TAIL"))
    private void onHeaderSet(Text header, CallbackInfo ci) {
        EventBus.getInstance().post(new PlayerListHeaderSetEvent(header.getString()));
    }

    @Inject(method = "setFooter", at = @At("TAIL"))
    private void onFooterSet(Text footer, CallbackInfo ci) {
        EventBus.getInstance().post(new PlayerListFooterSetEvent(footer.getString()));
    }
}
