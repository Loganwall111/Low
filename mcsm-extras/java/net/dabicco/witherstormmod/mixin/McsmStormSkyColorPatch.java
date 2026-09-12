package net.dabicco.witherstormmod.mixin;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.mcsm.extras.client.McsmGlarePalettes;
import net.mcsm.extras.client.McsmStormAtmosphere;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.9.315 -- one active storm sky colour for every vanilla/FabricSkyBoxes
 * route. The old setup let the regular peach/black sky state remain visible
 * while the custom skybox was enabled, so the player saw mismatched sky decks
 * instead of one pink-purple atmosphere.
 *
 * This changes the SkyRenderer colour state, not the cloud geometry. Regular
 * summon skyboxes remain available when there is no active phase-5+ storm;
 * during the storm, the same phase deck drives the zenith and horizon.
 */
@Mixin(value = SkyRenderer.class, priority = 1200)
public abstract class McsmStormSkyColorPatch {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;"
                    + "FLnet/minecraft/client/Camera;"
                    + "Lnet/minecraft/client/renderer/state/level/SkyRenderState;)V",
            at = @At("TAIL"),
            require = 0)
    private void mcsm$unifyStormSky(ClientLevel level, float partialTick, Camera camera,
            SkyRenderState state, CallbackInfo ci) {
        try {
            float phase = McsmStormAtmosphere.nearestPhase();
            if (phase < 4.90F || state == null) {
                return;
            }

            // Exact active-scene sky endpoints; do not allow the old peach or
            // black FabricSkyBoxes deck to remain in the active render state.
            float[] top;
            float[] horizon;
            if (phase < 5.42F) {
                top = hex(0x55, 0x70, 0x61);       // #557061
                horizon = top;
            } else if (phase < 5.92F) {
                top = hex(0x7D, 0x4B, 0x91);       // #7D4B91
                horizon = top;
            } else {
                top = hex(0x10, 0x0A, 0x1A);       // #100A1A
                horizon = hex(0xC4, 0x7A, 0x5A);   // #C47A5A
            }
            state.skyColor = rgb(top, state.skyColor);
            state.sunriseAndSunsetColor = rgb(horizon, state.sunriseAndSunsetColor);
            net.mcsm.extras.client.McsmStormSkyLayer.suppressLegacySkybox();
        } catch (Throwable ignored) {
            // A sky tint must never prevent the world from rendering.
        }
    }

    private static float[] hex(int r, int g, int b) {
        return new float[]{r / 255.0F, g / 255.0F, b / 255.0F};
    }

    private static int rgb(float[] c, int old) {
        int a = old & 0xFF000000;
        int r = Math.max(0, Math.min(255, Math.round(c[0] * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(c[1] * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(c[2] * 255.0F)));
        return a | (r << 16) | (g << 8) | b;
    }
}
