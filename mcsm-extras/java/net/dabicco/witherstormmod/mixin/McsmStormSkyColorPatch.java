package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.mcsm.extras.client.McsmNativeSkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Native SkyRenderer integration.
 *
 * SkyRenderState is the hand-off between LevelRenderer and Minecraft's own
 * spherical sky pass. This hook replaces the old texture-pack colour path:
 * no external JSON, texture, sky shell, or custom sky geometry is involved.
 */
@Mixin(value = SkyRenderer.class, priority = 1200)
public abstract class McsmStormSkyColorPatch {
    @Inject(
            method = "extractRenderState",
            at = @At("TAIL"),
            require = 0
    )
    private void mcsm$nativeAtmosphere(ClientLevel level, float partialTick,
            net.minecraft.world.phys.Vec3 cameraPosition, SkyRenderState state,
            CallbackInfo ci) {
        McsmNativeSkyRenderer.apply(level, partialTick, state);
    }

    // The camera-position parameter changed across the client transition. Keep
    // the fallback non-required so one descriptor can never crash startup.
    @Inject(
            method = "extractRenderState",
            at = @At("TAIL"),
            require = 0
    )
    private void mcsm$nativeAtmosphereCamera(ClientLevel level, float partialTick,
            Camera camera, SkyRenderState state, CallbackInfo ci) {
        McsmNativeSkyRenderer.apply(level, partialTick, state);
    }

    /** Full storm owns the celestial layer; the native gradient remains. */
    @Inject(
            method = "renderSunMoonAndStars",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void mcsm$suppressCelestials(PoseStack poseStack, float timeOfDay,
            int moonPhase, float sunAlpha, float starBrightness, CallbackInfo ci) {
        if (McsmNativeSkyRenderer.suppressCelestials()) {
            ci.cancel();
        }
    }

    /** Prevent the old directional sunrise fan from making a hard band. */
    @Inject(
            method = "renderSunriseAndSunset",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void mcsm$suppressSunrise(PoseStack poseStack, float angle, int color,
            CallbackInfo ci) {
        if (McsmNativeSkyRenderer.suppressCelestials()) {
            ci.cancel();
        }
    }
}
