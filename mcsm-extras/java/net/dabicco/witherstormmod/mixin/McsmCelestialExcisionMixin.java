package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.mcsm.extras.client.McsmCoreEngineController;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes only the native flat sun/moon/star draw call while the custom
 * physical 500-block slab is active. No sky renderer is used for the custom
 * atmosphere or replacement sun geometry.
 */
@Mixin(value = SkyRenderer.class, priority = 1100)
public abstract class McsmCelestialExcisionMixin {
    @Inject(
            method = "renderSunMoonAndStars",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void mcsm$removeNativeCelestials(PoseStack poseStack, float sunAngle,
            float moonAngle, float starAngle, MoonPhase moonPhase,
            float rainBrightness, float starBrightness, CallbackInfo ci) {
        if (McsmCoreEngineController.active()) {
            ci.cancel();
        }
    }
}
