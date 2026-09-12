package net.mcsm.extras.client;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcsm.extras.McsmExtrasConfig;

/**
 * MCSM 1.9.316 -- the Atmospheric W's Cloud for the SHADER-PACK path.
 *
 * 1.9.221-1.9.304 drew the blob here as a stack of four camera-facing oval
 * quads pinned at the storm's 3D position. That made the blob a flat 2D
 * card floating in the world -- the user's exact report ("a giant disc in
 * mid air, extremely far away, behind the storm"). The blob is NOT a world
 * object: it is a separate infinite skybox layer attached to the vanilla
 * sky. So this class now draws the SAME organic smear McsmStormSkyLayer
 * paints in vanilla/FBS mode (McsmBlobShape): a dense angular patch on a
 * fixed far sky sphere centred on the camera -- every vertex sits at true
 * skybox depth, the silhouette is
 * the noise-warped messy smear with feathered alpha edges and a stronger
 * interior, and the palette is the exact corrected 2026-09-11 hex decks.
 * The patch touches the storm exactly at the bearing centre, so it is
 * tethered to the monster and glides with it, and the depth test keeps
 * the storm body and terrain in front.
 *
 * Which path paints the blob depends on what OWNS the sky:
 *   - no shader pack in use (plain vanilla, FabricSkyBoxes, iris with its
 *     pack turned off): McsmStormSkyLayer draws the same smear on its
 *     400-block shell, and this class steps aside;
 *   - a shader pack actually rendering the sky (IrisApi says so): the
 *     pack's sky pass draws the dome, and THIS class draws the smear, so
 *     the blob exists on every render path without ever becoming world
 *     geometry.
 *
 * Blending is ALPHA (BlendFunction.TRANSLUCENT via GlowRenderTypes): the
 * dark smear body physically darkens the sky behind it, while the
 * feathered edge lets the vanilla sky bleed back in around the rim. The
 * render type is the same fogless translucent path the storm sky layer
 * uses, so NO vanilla distance fog ever touches the smear.
 */
public final class McsmBlobOval {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    private static final double FULL_RANGE = 700.0;
    private static final double MAX_RANGE = 100000.0;
    // Never place the cloud at the storm's world distance. It is a directional
    // sky projection, so flying toward it must not reveal a 3D object.
    private static final double SKY_RADIUS = 2048.0;

    private McsmBlobOval() {
    }

    private static float[] hex(int r, int g, int b) {
        return new float[]{r / 255.0F, g / 255.0F, b / 255.0F};
    }

    // corrected 2026-09-11 hex decks
    private static final float[] P5_CORE = hex(0x0A, 0x11, 0x12); // #0A1112
    private static final float[] P5_MID = hex(0x1D, 0x33, 0x35);  // #1D3335
    private static final float[] P5_EDGE = hex(0x55, 0x70, 0x61); // #557061
    private static final float[] P5_HIGH = hex(0x84, 0x93, 0xFF); // #8493FF
    private static final float[] P55_CORE = hex(0x05, 0x02, 0x08); // #050208
    private static final float[] P55_MID = hex(0x2A, 0x12, 0x3D);  // #2A123D
    private static final float[] P55_HIGH = hex(0x7D, 0x4B, 0x91); // #7D4B91
    private static final float[] P55_EDGE = hex(0x4B, 0x1E, 0x5E); // #4B1E5E
    private static final float[] P6_TOP = hex(0x10, 0x0A, 0x1A); // #100A1A
    private static final float[] P6_UMID = hex(0x33, 0x1C, 0x3D); // #331C3D
    private static final float[] P6_LMID = hex(0x8A, 0x53, 0x61); // #8A5361
    private static final float[] P6_BOT = hex(0xC4, 0x7A, 0x5A); // #C47A5A

    private static float ramp(float v, float lo, float hi) {
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float ss(float lo, float hi, float v) {
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] blend(float[] a, float[] b, float[] c, float wa, float wb, float wc, float tot) {
        return new float[]{
                (a[0] * wa + b[0] * wb + c[0] * wc) / tot,
                (a[1] * wa + b[1] * wb + c[1] * wc) / tot,
                (a[2] * wa + b[2] * wb + c[2] * wc) / tot};
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            // Only where no full-sky blob exists yet: the vanilla/FBS path
            // is McsmStormSkyLayer, so this runs when a shader pack is
            // ACTUALLY in use (iris with the pack off renders the vanilla
            // pipeline and belongs to the sky layer, not to this class).
            if (fabricSkyboxesOwnSky()) {
                return;
            }
            if (!McsmBlobShape.packInUse()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return;
            }
            StormData best = null;
            double bestD = Double.MAX_VALUE;
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            for (StormData d : ClientDistantStormManager.all()) {
                if (d.phase < 5.0F || d.phase > 6.95F) {
                    continue;
                }
                double dx = d.dispX - cam.x, dy = d.dispY - cam.y, dz = d.dispZ - cam.z;
                double dd = dx * dx + dy * dy + dz * dz;
                if (dd < bestD) {
                    bestD = dd;
                    best = d;
                }
            }
            if (best == null || bestD > MAX_RANGE * MAX_RANGE) {
                return;
            }
            double dist = Math.sqrt(bestD);
            // Infinite projection: do not fade the cloud because the player
            // flies away from the storm. Only its angular bearing is used.
            float presence = 1.0F;
            float phase = best.phase;

            // phase windows (same as the GLSL blob): 5 / 5.5-5.9 / 6
            float w5 = ramp(phase, 5.00F, 5.10F) * (1.0F - ramp(phase, 5.42F, 5.52F));
            float w55 = ramp(phase, 5.42F, 5.52F) * (1.0F - ramp(phase, 5.92F, 6.08F));
            float w6 = ramp(phase, 5.92F, 6.08F) * (1.0F - ramp(phase, 6.95F, 7.05F));
            float tot = w5 + w55 + w6;
            if (tot < 0.02F) {
                return;
            }
            // per-layer palettes, lerped across the phase windows; the
            // phase-6 four-colour split is approximated vertically (dark
            // zenith cap on the core, dusty rose mid, amber bottom).
            final float[] core = blend(P5_CORE, P55_CORE, P6_TOP, w5, w55, w6, tot);
            final float[] mid = blend(P5_MID, P55_MID, P6_UMID, w5, w55, w6, tot);
            final float[] edge = blend(P5_EDGE, P55_EDGE, mix(P6_LMID, P6_BOT, 0.4F), w5, w55, w6, tot);
            // 5.5-5.9: the high-altitude royal magenta pushes into the edge
            for (int i = 0; i < 3; i++) {
                edge[i] += (P55_HIGH[i] - edge[i]) * (w55 / tot) * 0.35F;
            }

            McsmExtrasConfig.load();
            double gs = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
            // 1.9.316: make the alpha patch large enough to sit behind the
            // whole storm silhouette while remaining an irregular field.
            double outer = (58.0 + 30.0 * ramp(phase, 5.0F, 6.0F))
                    * (0.78 + (gs - 0.25) * 0.139);

            Vec3 rawView = new Vec3(best.dispX, best.dispY, best.dispZ).subtract(cam);
            if (rawView.lengthSqr() < 1.0E-4D) {
                return;
            }
            final Vec3 bearing = rawView.normalize();

            // 1.9.316: the smear patch is drawn on a fixed camera-centred
            // far sky sphere, never at the storm's world distance. Its bearing
            // follows the storm, but its depth cannot be approached in-world.
            final float[] high = blend(P5_HIGH, P55_HIGH, P6_UMID, w5, w55, w6, tot);
            final McsmBlobShape.Patch patch = McsmBlobShape.patchFor(bearing, phase,
                    outer, presence, core, mid, edge, high, w55 / tot, w6 / tot);
            // Keep the shell just behind the storm, rather than at a fixed
            // card distance or exactly coplanar with the body.
            final double radius = SKY_RADIUS;
            final Vec3 camPos = cam;

            SubmitNodeCollector collector = ctx.submitNodeCollector();
            collector.submitCustomGeometry(ctx.poseStack(), GlowRenderTypes.translucent(WHITE),
                    (pose, consumer) -> McsmBlobShape.stream(pose, consumer, patch, camPos, radius));
        } catch (Throwable ignored) {
            // the blob must never break a frame
        }
    }

    private static boolean fabricSkyboxesOwnSky() {
        return McsmBlobShape.modLoaded("fabricskyboxes") && DabyWSClientConfig.customSkyboxes;
    }
}
