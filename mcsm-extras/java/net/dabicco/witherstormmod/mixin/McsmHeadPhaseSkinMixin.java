package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.dabicco.witherstormmod.entity.WitherStormHeadEntity;
import net.dabicco.witherstormmod.entity.state.WitherStormHeadRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps detached head rendering on the same phase-specific atlas as the body. */
@Mixin(net.dabicco.witherstormmod.entity.renderer.WitherStormHeadRenderer.class)
public abstract class McsmHeadPhaseSkinMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false, require = 0)
    private void mcsm$phaseHintForHead(WitherStormHeadEntity entity, WitherStormHeadRenderState state,
                                       float partialTick, CallbackInfo ci) {
        try {
            if (entity.level().getEntity(entity.getStormId()) instanceof WitherStormEntity storm) {
                StormSkins.setPhaseHint(storm.getPhase());
            }
        } catch (Throwable ignored) {
        }
    }
}
