package net.tidalhq.tidal.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.impl.ParticleSpawnedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V", at = @At("HEAD"))
    private void onAddParticle(ParticleEffect parameters, boolean force, boolean important, double x, double y, double z, double vx, double vy, double vz, CallbackInfo ci) {
        EventBus.getInstance().post(new ParticleSpawnedEvent(
                parameters, force, important, x, y, z, vx, vy, vz
        ));
    }
}
