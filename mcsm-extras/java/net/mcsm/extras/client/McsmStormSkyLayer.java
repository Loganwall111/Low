package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.world.phys.Vec3;

/**
 * 1.9.215 R2 -- FabricSkyBoxes compatibility layer for the storm sky.
 * 1.9.303 -- extended to PURE VANILLA: this is now the storm-sky layer for
 * every configuration WITHOUT a shader pack (plain vanilla and
 * FabricSkyBoxes mode). The core-shader blob only works when a shader pack
 * binds the FogSkyEnd carrier uniforms, so vanilla alone never showed the
 * storm sky -- this layer fixes that.
 *
 * The base mod ships its own FabricSkyBoxes skyboxes (day / night / sunset),
 * but McsmSkyBlob suppresses that legacy backdrop before the sky pass while
 * an in-range phase-5+ storm is active. Otherwise those OPAQUE textures cover
 * the vanilla sky pass, hiding the directional storm smear entirely -- the
 * user's exact report ("the Wither Storm skies are not rendering with fabric
 * skyboxes on").
 *
 * This layer draws the SAME sky the shader paints as world geometry, but
 * AFTER the skybox, so it wins:
 *
 *   * only the organic storm SMEAR, pinned to the storm bearing: a separate
 *     infinite skybox layer attached to the vanilla sky (McsmBlobShape) --
 *     a noise-warped oval with side lobes, transparent feathered edges, and
 *     the corrected deck banding smeared over a noise-jittered radius;
 *   * distance fade 700..1600 blocks: the patch alpha falls to zero and the
 *     regular sky slowly returns ("go extremely far away and the sky changes
 *     back to vanilla");
 *   * terrain closer than the shell occludes it through the depth test
 *     (translucent pipeline, depth compare >=, no depth write), exactly
 *     like the sky-behind-terrain read of the reference frames.
 *
 * It runs whenever NO shader pack OWNS the sky -- plain vanilla,
 * FabricSkyBoxes mode, and iris/other shader mods with the pack turned
 * OFF (a shader mod merely being installed must not push the blob onto
 * world-anchored geometry; that was the "disc floating in mid air" the
 * 1.9.302-1.9.304 builds drew). With a shader pack ACTIVE, the pack's
 * sky pass plus McsmBlobOval (same McsmBlobShape smear, drawn at the
 * storm's distance) take over, so this layer steps aside and never
 * doubles the blob.
 */
public final class McsmStormSkyLayer {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    /** full presence inside this range; the layer is gone at MAX_RANGE */
    private static final double FULL_RANGE = 700.0;
    private static final double MAX_RANGE = 1600.0;

    // corrected 2026-09-11 hex decks (0..1)
    private static final float[] D5_Z = hex(0x16, 0x1A, 0x1D);
    private static final float[] D5_M = hex(0x2D, 0x42, 0x3F);
    private static final float[] D5_H = hex(0x6A, 0x9A, 0x78);
    private static final float[] D55_Z = hex(0x0B, 0x04, 0x10);
    private static final float[] D55_M = hex(0x2D, 0x14, 0x42);
    private static final float[] D55_H = hex(0x87, 0x52, 0x9C);
    private static final float[] D55_HIGH = hex(0x58, 0x1C, 0x6E);
    private static final float[] D6_Z = hex(0x1A, 0x12, 0x26);
    private static final float[] D6_UM = hex(0x46, 0x2A, 0x52);
    private static final float[] D6_LM = hex(0x96, 0x61, 0x73);
    private static final float[] D6_H = hex(0xD8, 0x98, 0x74);

    private McsmStormSkyLayer() {
    }

    private static float[] hex(int r, int g, int b) {
        return new float[]{r / 255.0F, g / 255.0F, b / 255.0F};
    }

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float ss(float lo, float hi, float v) {
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static boolean fabricSkyboxesLoaded() {
        return modLoaded("fabricskyboxes");
    }

    /** reflective isModLoaded so the layer works with or without Fabric API's loader wiring */
    private static boolean modLoaded(String id) {
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object ans = loaderCls.getMethod("isModLoaded", String.class).invoke(loader, id);
            return Boolean.TRUE.equals(ans);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean shaderPackLoaded() {
        return McsmBlobShape.packInUse();
    }

    private static ClientDistantStormManager.StormData nearestStorm(double maxDist) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return null;
            }
            Vec3 pos = mc.player.position();
            ClientDistantStormManager.StormData best = null;
            double bestD = Double.MAX_VALUE;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                if (d.phase < 4.95F) {
                    continue;
                }
                double dx = d.dispX - pos.x;
                double dy = d.dispY - pos.y;
                double dz = d.dispZ - pos.z;
                double dd = dx * dx + dy * dy + dz * dz;
                if (dd < bestD) {
                    bestD = dd;
                    best = d;
                }
            }
            if (best == null || bestD > maxDist * maxDist) {
                return null;
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            // 1.9.303 -- the blob must exist in PURE VANILLA too. It never
            // did before: the core-shader blob reads the storm phase from
            // shader uniforms (FogSkyEnd etc.) that only shader packs bind,
            // so in vanilla the sky pass always saw "no storm". This layer
            // therefore runs whenever no shader pack owns the sky -- plain
            // vanilla AND FabricSkyBoxes mode. 1.9.308: "no pack owns the
            // sky" now means the pack is INACTIVE (IrisApi), not that iris
            // is merely installed; a shader mod with its pack turned off
            // renders the vanilla pipeline and this layer draws for it too.
            boolean fbsSky = fabricSkyboxesLoaded() && DabyWSClientConfig.customSkyboxes;
            if (shaderPackLoaded() && !fbsSky) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return;
            }
            ClientDistantStormManager.StormData storm = nearestStorm(MAX_RANGE);
            if (storm == null) {
                return;
            }
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            double dx = storm.dispX - cam.x;
            double dy = storm.dispY - cam.y;
            double dz = storm.dispZ - cam.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float presence = 1.0F - ss((float) FULL_RANGE, (float) MAX_RANGE, (float) dist);
            if (presence <= 0.005F) {
                return;
            }
            float phase = storm.phase;
            // Same lifetime as the core-shader blob: 5.00 - 6.95.
            float w5 = ramp(phase, 5.00F, 5.10F) * (1.0F - ramp(phase, 5.42F, 5.52F));
            float w55 = ramp(phase, 5.42F, 5.52F) * (1.0F - ramp(phase, 5.92F, 6.08F));
            float w6 = ramp(phase, 5.92F, 6.08F) * (1.0F - ramp(phase, 6.95F, 7.05F));
            float tot = w5 + w55 + w6;
            if (tot < 0.02F) {
                return;
            }
            final Vec3 bearing = new Vec3(dx, dy, dz).normalize();
            McsmExtrasConfig.load();
            double gs = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
            // 1.9.308: the alpha patch itself is large enough to sit behind
            // the whole storm silhouette. This is a broken angular field,
            // not the old opaque full-sky dome.
            final double outer = (58.0 + 30.0 * ramp(phase, 5.0F, 6.0F))
                    * (0.78 + (gs - 0.25) * 0.139);
            final float w55f = w55 / tot;
            final float w6f = w6 / tot;

            SubmitNodeCollector collector = ctx.submitNodeCollector();
            // Do not paint an opaque camera-centred dome here. That was the
            // giant green/purple sphere in the 1.9.308 screenshots and it also
            // hid the active Fabric sky. The storm sky is the alpha-feathered
            // organic patch below; the untouched sky remains visible through
            // its broken edge, exactly like the reference glare frames.
            // 1.9.308 -- the organic smear: a SEPARATE infinite skybox layer
            // tethered to the storm bearing. Put the camera-centred angular
            // patch just behind the storm instead of at a fixed 399 blocks;
            // this keeps the paint visually close to the monster while it
            // remains direction-only and impossible to physically reach.
            final float[] core = blend(D5_Z, D55_Z, D6_Z, 1.0F - w55 - w6, w55, w6, 1.0F);
            final float[] midc = blend(D5_M, D55_M, D6_UM, 1.0F - w55 - w6, w55, w6, 1.0F);
            final float[] edge = blend(D5_H, D55_H, D6_LM, 1.0F - w55 - w6, w55, w6, 1.0F);
            final McsmBlobShape.Patch patch = McsmBlobShape.patchFor(bearing, phase,
                    outer, presence, core, midc, edge, D55_HIGH, w55f, w6f);
            final double blobRadius = Math.max(2.0, dist - 8.0);
            collector.submitCustomGeometry(ctx.poseStack(), GlowRenderTypes.translucent(WHITE),
                    (pose, consumer) -> McsmBlobShape.stream(pose, consumer, patch, cam, blobRadius));

        } catch (Throwable ignored) {
            // a missing surface must never break a frame
        }
    }

    /** weighted 3-way blend of the three phase decks */
    private static float[] blend(float[] a, float[] b, float[] c, float wa, float wb, float wc, float tot) {
        return new float[]{
                (a[0] * wa + b[0] * wb + c[0] * wc) / tot,
                (a[1] * wa + b[1] * wb + c[1] * wc) / tot,
                (a[2] * wa + b[2] * wb + c[2] * wc) / tot};
    }


}
