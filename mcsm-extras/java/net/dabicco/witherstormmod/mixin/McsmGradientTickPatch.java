package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.dabicco.witherstormmod.client.StormSkins;
import net.dabicco.witherstormmod.client.StormSkyGradient;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.mcsm.extras.McsmDiag;
import net.mcsm.extras.McsmGate;
import net.mcsm.extras.client.McsmClientBlasts;
import net.mcsm.extras.client.McsmClientChat;
import net.mcsm.extras.client.McsmTeethPhaseTint;
import net.mcsm.extras.client.McsmBeamWeatherTint;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MCSM 1.9.74 -- the real reason the glare blob never appeared.
 *
 * StormSkyGradient.update(Vec3) is the ONLY writer of yawDeg, pitchDeg, phase
 * and active. A whole-jar bytecode scan for callers of that method returns
 * NOTHING -- it is dead code. Three classes read the results:
 *
 *     StormSkyGradientMixin   -> yaw(), color(), fogStampActive()
 *     McsmFogCarrierMixin     -> yaw(), pitch(), phase(), fogStampActive()
 *     McsmBlobCarrierPatch    -> yaw(), pitch(), phase(), fogStampActive()
 *
 * but nobody ever populates them. So "active" stays false for the entire
 * session, fogStampActive() returns false, and BOTH carriers bail at their
 * first guard. The direction never reaches the shader, mcsm_boss_dir() returns
 * w=0, and mcsm_blob() is never invoked.
 *
 * This sat underneath the aliasing bug fixed in 1.9.72: even with a perfectly
 * invertible encoding there was nothing to encode.
 *
 * Fix: drive update() once per frame from LevelRenderer.render, at HEAD so the
 * values are fresh before the fog carriers run later in the same frame.
 * CameraRenderState.pos is the camera position in world space, which is exactly
 * the argument update() expects (it walks ClientDistantStormManager.all() and
 * picks the strongest storm relative to that point).
 *
 * update() is self-contained and cheap -- one pass over the client-side storm
 * list, two atan2 calls -- so a per-frame call is fine.
 */
@Mixin(LevelRenderer.class)
public abstract class McsmGradientTickPatch {

    @Inject(
        method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"
               + "Lnet/minecraft/client/DeltaTracker;Z"
               + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"
               + "Lorg/joml/Matrix4fc;"
               + "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
               + "Lorg/joml/Vector4f;Z)V",
        at = @At("HEAD"),
        require = 1
    )
    private void mcsm$driveStormGradient(GraphicsResourceAllocator allocator,
                                         DeltaTracker deltaTracker,
                                         boolean renderBlockOutline,
                                         CameraRenderState cameraState,
                                         Matrix4fc frustumMatrix,
                                         GpuBufferSlice fogBuffer,
                                         Vector4f fogColor,
                                         boolean skipSky,
                                         CallbackInfo ci) {
        if (cameraState == null || cameraState.pos == null) {
            return;
        }
        try {
            McsmGate.openClient();
            McsmDiag.banner();
            // MCSM 1.9.110 -- speak the build number in chat once per world
            // load. Chat is the one place the player is guaranteed to look, so
            // "which jar is actually running?" stops needing a log hunt.
            McsmClientChat.announceBuildOnce();
            StormSkyGradient.update(cameraState.pos);
            McsmTeethPhaseTint.tick();
            McsmBeamWeatherTint.tick();
            // Report what update() produced. This is the value the glare blob
            // depends on -- if it never reports ACTIVE, the blob cannot draw
            // and the problem is upstream of the carrier, not in the shader.
            McsmDiag.gradient(StormSkyGradient.fogStampActive(),
                              StormSkyGradient.phase(),
                              StormSkyGradient.yaw(),
                              StormSkyGradient.pitch());
            // Phase 26: screenshots showed a fully rendered storm under a plain
            // vanilla sky and it was impossible to tell "phase below the 4.5
            // threshold" from "the patch is broken". StormSkyGradient.update()
            // only selects a storm at phase >= 4.5 and within 1400 blocks, so
            // below that there is NO storm sky BY DESIGN. Report the reason.
            McsmDiag.skyReason(StormSkyGradient.fogStampActive(),
                               StormSkyGradient.phase());
            // Phase 32: report the live feature flags. Every gate audits as OPEN
            // in bytecode, so if one of these prints TRUE and is still invisible
            // the fault is in drawing, not configuration -- and that is a very
            // different search.
            McsmDiag.features(DabyWSClientConfig.turquoiseTeeth,
                              DabyWSClientConfig.headEyeGlow,
                              DabyWSClientConfig.sunGlow,
                              DabyWSClientConfig.stormShadow,
                              DabyWSClientConfig.bloomStrength > 0.0,
                              StormSkins.og(),
                              DabyWSClientConfig.stormSkin);
            // MCSM 1.9.109 -- advance the expanding blasts. Driven from here
            // rather than from the storm's tick because the death blast has to
            // keep expanding for its full five seconds AFTER the storm entity
            // has been removed, and this hook runs for as long as the world is
            // being rendered. It steps at most once per game tick internally.
            McsmClientBlasts.tick();
        } catch (Throwable ignored) {
            // Never let a visual helper break the frame.
        }
    }
}
