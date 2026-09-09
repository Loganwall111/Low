package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.entity.WitherStormEntity;
import net.dabicco.witherstormmod.entity.renderer.WitherStormRenderer;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.util.Mth;
import net.mcsm.extras.McsmExtrasConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the subtle MCSM creature sway: early phase bodies lean/tilt left and
 * right, and the summon animation starts with the heads/body dipped downward
 * before rising quickly.
 */
@Mixin(WitherStormRenderer.class)
public abstract class McsmStormSwayMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"), remap = false, require = 0)
    private void mcsm$swayAndSummonLookDown(WitherStormEntity entity, WitherStormRenderState state,
                                            float partialTick, CallbackInfo ci) {
        try {
            McsmExtrasConfig.load();
            if (!McsmExtrasConfig.stormBodySway) {
                return;
            }
            float now = (float)(entity.level().getGameTime() % 100000L) + partialTick;
            float phase = (float)state.phase;
            float early = 1.0F - Mth.clamp((phase - 1.0F) / 3.4F, 0.0F, 1.0F);
            float big = Mth.clamp((phase - 3.8F) / 2.5F, 0.0F, 1.0F);
            float sway = Mth.sin(now * 0.045F + entity.getId() * 0.7F);
            float sway2 = Mth.sin(now * 0.029F + entity.getId() * 1.3F);
            state.bodyRoll += sway * (3.8F * early + 1.4F * big);
            state.xRot += sway2 * (2.4F * early + 0.65F * big);
            state.bodyRot += Mth.sin(now * 0.018F) * (1.8F * early + 0.35F * big);
            if (state.playingSpawnAnimation) {
                float t = Mth.clamp(state.spawnElapsedTicks / 10.0F, 0.0F, 1.0F);
                float snapUp = 1.0F - t * t * (3.0F - 2.0F * t);
                state.xRot += 24.0F * snapUp;
                state.bodyRoll += Mth.sin(now * 0.22F) * 4.0F * snapUp;
                for (int i = 0; i < state.headXRot.length; i++) {
                    state.headXRot[i] += 18.0F * snapUp;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
