package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.world.phys.Vec3;

/**
 * 1.9.215 R2 -- FabricSkyBoxes compatibility layer for the storm sky.
 *
 * The base mod ships its own FabricSkyBoxes skyboxes (day / night / sunset,
 * customSkyboxes = true by default). When the FabricSkyBoxes mod is loaded
 * it draws those OPAQUE textures over the vanilla sky pass, which hides the
 * core-shader storm dome and the infinite skybox blob entirely -- the user's
 * exact report ("the Wither Storm skies are not rendering with fabric
 * skyboxes on").
 *
 * This layer draws the SAME sky the shader paints as world geometry, but
 * AFTER the skybox, so it wins:
 *
 *   * a camera-centred shell at 400 blocks (inside the far plane at any
 *     render distance) -- because it re-centres on the camera every frame
 *     you can never fly out of it, which is the infinite-skybox property;
 *   * the full phase dome (three-stop zenith/mid/horizon gradient) using the
 *     CORRECTED 2026-09-11 hex decks, opaque while a storm is near;
 *   * the smeared oval infinite blob projected on the same shell, pinned to
 *     the storm bearing (u_StormPos) with the same angular field math as
 *     mcsm_blob() in mcsm_visuals.glsl (1.55x0.90 ellipse, 0.18 rad tilt,
 *     dark core, smoothstep smudge, outer flare);
 *   * distance fade 700..1600 blocks: the shell alpha falls to zero and the
 *     regular FabricSkyBoxes sky slowly returns ("go extremely far away and
 *     the sky changes back to vanilla");
 *   * terrain closer than the shell occludes it through the depth test
 *     (translucent pipeline, depth compare >=, no depth write), exactly
 *     like the sky-behind-terrain read of the reference frames.
 *
 * It only runs when the FabricSkyBoxes mod is loaded AND the base mod's
 * customSkyboxes toggle is on -- otherwise the core shader already owns the
 * sky and this layer would double it.
 */
public final class McsmStormSkyLayer {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    /** shell radius in blocks; inside the far plane at any render distance */
    private static final double SHELL = 400.0;

    private static final int SECTORS = 24;   // azimuth segments (smooth, no facets)
    private static final int BANDS = 10;     // elevation rings: -12deg .. +88deg

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
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object ans = loaderCls.getMethod("isModLoaded", String.class)
                    .invoke(loader, "fabricskyboxes");
            return Boolean.TRUE.equals(ans);
        } catch (Throwable t) {
            return false;
        }
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

    private static Vec3 dir(double elevDeg, double azim) {
        double ce = Math.cos(Math.toRadians(elevDeg));
        return new Vec3(Math.cos(azim) * ce, Math.sin(Math.toRadians(elevDeg)), Math.sin(azim) * ce);
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            // Only while FabricSkyBoxes would otherwise cover the storm sky.
            if (!fabricSkyboxesLoaded() || !DabyWSClientConfig.customSkyboxes) {
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
            // full-sky phase dome, three stops weighted by the phase windows
            final float[] zen = blend(D5_Z, D55_Z, D6_Z, w5, w55, w6, tot);
            final float[] mid = blend(D5_M, D55_M, D6_UM, w5, w55, w6, tot);
            final float[] hor = blend(D5_H, D55_H, D6_H, w5, w55, w6, tot);

            final Vec3 bearing = new Vec3(dx, dy, dz).normalize();
            McsmExtrasConfig.load();
            double gs = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
            final double outer = (52.0 + 24.0 * ramp(phase, 5.0F, 6.0F))
                    * (0.80 + (gs - 0.25) * 0.196);
            final int alpha = (int) (presence * 255.0F);
            final float w55f = w55 / tot;
            final float w6f = w6 / tot;

            SubmitNodeCollector collector = ctx.submitNodeCollector();
            collector.submitCustomGeometry(ctx.poseStack(), GlowRenderTypes.translucent(WHITE),
                    (pose, consumer) -> {
                        for (int i = 0; i < BANDS; i++) {
                            double e0 = -12.0 + i * (100.0 / BANDS);
                            double e1 = -12.0 + (i + 1) * (100.0 / BANDS);
                            for (int j = 0; j < SECTORS; j++) {
                                double a0 = (2.0 * Math.PI * j) / SECTORS;
                                double a1 = (2.0 * Math.PI * (j + 1)) / SECTORS;
                                Vec3 p00 = dir(e0, a0).scale(SHELL).add(cam);
                                Vec3 p01 = dir(e0, a1).scale(SHELL).add(cam);
                                Vec3 p10 = dir(e1, a0).scale(SHELL).add(cam);
                                Vec3 p11 = dir(e1, a1).scale(SHELL).add(cam);
                                float[] c00 = color(dir(e0, a0), bearing, outer, zen, mid, hor, w55f, w6f);
                                float[] c01 = color(dir(e0, a1), bearing, outer, zen, mid, hor, w55f, w6f);
                                float[] c10 = color(dir(e1, a0), bearing, outer, zen, mid, hor, w55f, w6f);
                                float[] c11 = color(dir(e1, a1), bearing, outer, zen, mid, hor, w55f, w6f);
                                vtx(pose, consumer, p10, c10, alpha);
                                vtx(pose, consumer, p00, c00, alpha);
                                vtx(pose, consumer, p01, c01, alpha);
                                vtx(pose, consumer, p11, c11, alpha);
                            }
                        }
                    });

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

    /**
     * Full colour for a shell direction: the phase dome gradient plus the
     * infinite oval blob (same angular math as the GLSL blob).
     */
    private static float[] color(Vec3 d, Vec3 b, double outer, float[] zen, float[] mid,
                                 float[] hor, float w55, float w6) {
        float ty = (float) Mth.clamp(d.y, -1.0, 1.0);
        float t = (float) Math.pow(1.0 - Mth.clamp(ty, 0.0F, 1.0F), 1.35);
        float[] base = mix3(zen, mid, ss(0.04F, 0.45F, t));
        base = mix3(base, hor, ss(0.45F, 0.95F, t));
        // below the horizon, keep darkening instead of holding one colour
        float dark = (float) Mth.clamp(ty * 4.0 + 1.0, 0.0, 1.0);
        base = scale(base, 0.62F + 0.38F * dark);

        // ---- the infinite blob, projected behind the storm bearing ----
        double cd = Math.max(d.dot(b), 0.02);
        Vec3 up = Math.abs(b.y) > 0.985 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 ex = up.cross(b).normalize();
        Vec3 ey = b.cross(ex);
        double ct = Math.cos(0.18), st = Math.sin(0.18);
        Vec3 exT = ex.scale(ct).add(ey.scale(st));
        Vec3 eyT = ey.scale(ct).subtract(ex.scale(st));
        double k = cd * Math.tan(Math.toRadians(outer));
        double sx = d.dot(exT) / k;
        double sy = d.dot(eyT) / k;
        float u = (float) Math.sqrt((sx / 1.55) * (sx / 1.55) + (sy / 0.90) * (sy / 0.90));
        if (u >= 1.85F) {
            return base; // beyond the bleed: plain dome (vanilla sky at the rim)
        }

        float coreW = 1.0F - ss(0.10F, 0.42F, u);
        float midW = ss(0.16F, 0.46F, u) * (1.0F - ss(0.58F, 0.95F, u));
        float edgeW = ss(0.50F, 0.88F, u) * (1.0F - ss(0.95F, 1.32F, u));
        float bleedW = ss(0.85F, 1.32F, u) * (1.0F - ss(1.30F, 1.85F, u));

        float[] core = blend(D5_Z, D55_Z, D6_Z, 1.0F - w55 - w6, w55, w6, 1.0F);
        float[] midc = blend(D5_M, D55_M, D6_UM, 1.0F - w55 - w6, w55, w6, 1.0F);
        float[] edge = blend(D5_H, D55_H, D6_LM, 1.0F - w55 - w6, w55, w6, 1.0F);

        float[] blob = new float[3];
        for (int i = 0; i < 3; i++) {
            blob[i] = core[i] * coreW + midc[i] * midW + edge[i] * edgeW
                    + edge[i] * bleedW * 0.55F;
        }
        // 5.5-5.9: richer royal magenta overhead
        float upness = (float) Mth.clamp((sy / 0.90) * 0.5 + 0.5, 0.0, 1.0);
        for (int i = 0; i < 3; i++) {
            blob[i] += (midc[i] + (D55_HIGH[i] - midc[i]) * 0.6F) * midW * upness * w55 * 0.5F;
        }
        // phase 6: the four-colour sunset split owns the gradient
        if (w6 > 0.0F) {
            float[] split = p6Split(ty);
            float heart = 0.62F + 0.38F * coreW;
            for (int i = 0; i < 3; i++) {
                blob[i] = blob[i] + (split[i] * heart - blob[i]) * w6;
            }
        }

        float blendW = 1.0F - ss(1.32F, 1.85F, u);
        return mix3(base, blob, blendW);
    }

    /** phase 6: #1A1226 / #462A52 / #966173 / #D89874 keyed to elevation */
    private static float[] p6Split(float ty) {
        float u = Mth.clamp(ty * 0.5F + 0.5F, 0.0F, 1.0F);
        float[] c = mix3(D6_H, D6_LM, ss(0.02F, 0.30F, u));
        c = mix3(c, D6_UM, ss(0.26F, 0.55F, u));
        c = mix3(c, D6_Z, ss(0.52F, 0.95F, u));
        return c;
    }

    private static float[] mix3(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static float[] scale(float[] a, float s) {
        return new float[]{a[0] * s, a[1] * s, a[2] * s};
    }

    private static void vtx(Pose pose, VertexConsumer consumer, Vec3 at, float[] rgb, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor((int) (rgb[0] * 255.0F), (int) (rgb[1] * 255.0F), (int) (rgb[2] * 255.0F), a)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
