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
 * 1.9.312 -- one active storm sky colour for every vanilla/FabricSkyBoxes
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

            float[][] deck;
            if (phase < 5.42F) {
                deck = McsmGlarePalettes.P5_TEAL;
            } else if (phase < 5.92F) {
                deck = McsmGlarePalettes.P55;
            } else if (phase < 7.95F) {
                deck = McsmGlarePalettes.P6;
            } else {
                deck = McsmGlarePalettes.P89;
            }

            // SkyRenderState stores the zenith and sunrise/horizon colours as
            // packed ARGB values. Use the same endpoints as the cloud deck so
            // the vanilla sky, custom skybox and cloud do not disagree.
            state.skyColor = rgb(deck[0], state.skyColor);
            state.sunriseAndSunsetColor = rgb(deck[deck.length - 1],
                    state.sunriseAndSunsetColor);

            // The base mod's dynamic skybox toggle is retained outside a
            // storm, but cannot cover the active phase deck while it is live.
            DabyWSClientConfig.customSkyboxes = false;
        } catch (Throwable ignored) {
            // A sky tint must never prevent the world from rendering.
        }
    }

    private static int rgb(float[] c, int old) {
        int a = old & 0xFF000000;
        int r = Math.max(0, Math.min(255, Math.round(c[0] * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(c[1] * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(c[2] * 255.0F)));
        return a | (r << 16) | (g << 8) | b;
    }
}
