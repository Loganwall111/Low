package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.mcsm.extras.client.McsmNativeSkyRenderer;
import net.mcsm.extras.client.McsmStoryModeSunSlab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
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
    private void mcsm$suppressCelestials(PoseStack poseStack, float sunAngle,
            float moonAngle, float sunAlpha, MoonPhase moonPhase,
            float starAngle, float starBrightness, CallbackInfo ci) {
        if (McsmNativeSkyRenderer.suppressCelestials()) {
            if (McsmStoryModeSunSlab.enabled()) {
                mcsm$renderStorySun(poseStack, sunAngle);
            }
            // The ordinary sun, moon, and stars are all in this method.  Do
            // this after the native gradient has been extracted: the slab is
            // therefore the only optional celestial geometry, while the
            // Wither Storm/entity pass that follows still draws in front.
            ci.cancel();
        }
    }

    /**
     * Render the optional Story Mode geometry through the native sun pass.
     * SkyRenderer's own sun buffer is a camera-relative geometric quad; the
     * transform below turns it into a tall slab without a texture-pack sky,
     * billboard entity, physical block, or persistent custom GPU allocation.
     *
     * The vanilla sun quad is centered at local Y=100.  Translate the local
     * orbital origin by exactly 500.0F first, then recenter that quad before
     * stretching it vertically.  Consequently its center remains exactly
     * 500.0F blocks from the camera along the rotated time-of-day vector.
     */
    private void mcsm$renderStorySun(PoseStack poseStack, float timeOfDay) {
        final float FIXED_ORBITAL_DISTANCE = 500.0F;
        final float NATIVE_SUN_CENTER = 100.0F;
        final float SLAB_VERTICAL_SCALE = 3.0F;

        poseStack.pushPose();
        // SkyRenderer supplies the standard sun angle in radians.  Rotating
        // the native orbital Y axis by it preserves the day/night cycle; the
        // ecliptic tilt makes the result a vertical Story Mode slab rather
        // than a flat, stationary screen overlay.
        poseStack.mulPose(Axis.YP.rotation(-timeOfDay));
        poseStack.mulPose(Axis.ZP.rotation((float) Math.toRadians(23.44D)));
        poseStack.translate(0.0F, FIXED_ORBITAL_DISTANCE, 0.0F);
        poseStack.scale(1.0F, SLAB_VERTICAL_SCALE, 1.0F);
        poseStack.translate(0.0F, -NATIVE_SUN_CENTER, 0.0F);
        mcsm$renderSun(1.0F, poseStack);
        poseStack.popPose();
    }

    /** Access the existing native sun quad without allocating another mesh. */
    @Invoker("renderSun")
    protected abstract void mcsm$renderSun(float alpha, PoseStack poseStack);

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
