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
 * The old custom celestial slab is retired. The native sun, moon, and stars
 * remain available; only the separate sunrise fan is suppressed by the
 * continuous-sky hook.
 */
@Mixin(value = SkyRenderer.class, priority = 1100)
public abstract class McsmCelestialExcisionMixin {
    @Inject(
            method = "renderSunMoonAndStars",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void mcsm$removeNativeCelestials(PoseStack poseStack, float sunAngle,
            float moonAngle, float starAngle, MoonPhase moonPhase,
            float rainBrightness, float starBrightness, CallbackInfo ci) {
        // Native celestial geometry is authoritative. The former replacement
        // slab is inert, so cancelling this call would remove the regular sky
        // instead of merely removing the unwanted orange band.
    }
}
