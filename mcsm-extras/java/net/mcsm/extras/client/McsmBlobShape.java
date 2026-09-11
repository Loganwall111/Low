package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.9.305 -- THE ORGANIC STORM SMEAR, the one true shape of the infinite
 * skybox blob, shared by every Java render path.
 *
 * The blob is NOT a world object and NOT a flat disc: it is a separate,
 * infinite skybox layer attached to the vanilla sky and tethered to the
 * storm's bearing -- the same read as the sunrise colour band or an
 * aurora: the vanilla sky stays visible above and around it, it never
 * gets closer when you fly, and it glides with the storm.
 *
 * This class owns:
 *   * the mesh: a dense angular patch laid out on the sky sphere around
 *     the storm bearing (rectangular in the tilted blob frame), streamed
 *     as quads around the camera at any caller-chosen radius -- 400
 *     blocks for the vanilla/FBS sky layer, the storm's own distance for
 *     the shader-pack fallback, so it always behaves like sky, never
 *     like a sprite pinned in the world;
 *   * the silhouette: a noise-warped oval with two warped side lobes --
 *     a messy organic smear, not a clean ellipse. The edge is a feathered
 *     smoothstep of the warped distance field, so it melts into the sky
 *     with no rim; the body alpha is a flat plateau (uniform opacity);
 *   * the colour: the exact corrected 2026-09-11 hex decks, banded
 *     core/mid/edge on a noise-jittered radius (so the bands smear too),
 *     with the 5.5 royal-magenta overhead, the phase-6 four-colour split
 *     and domain-warped streak shading inside the body;
 *   * packInUse(): whether a shader pack is ACTUALLY rendering the sky
 *     (IrisApi.isShaderPackInUse() when iris is present) -- a shader mod
 *     merely being INSTALLED must not route the blob to world geometry,
 *     which was the 1.9.302-1.9.304 "disc floating in mid air" bug.
 *
 * Everything is deterministic value noise (no RNG, no textures), so the
 * smear is stable frame to frame and pixel-soft by construction.
 */
public final class McsmBlobShape {

    /** patch density: dense enough that the noise-warped edge reads smooth */
    public static final int NX = 84;
    public static final int NY = 60;

    /** patch half-extent in tangent-space units -- covers the old 1.85 bleed */
    public static final double SX_MAX = 1.55 * 1.85;
    public static final double SY_MAX = 0.90 * 1.85;

    /** the oval footprint + tilt shared with the GLSL blob */
    private static final double OVAL_X = 1.55;
    private static final double OVAL_Y = 0.90;
    private static final double TILT = 0.18;

    // phase-6 four-colour split (canonical corrected hex set B)
    private static final float[] D6_Z = hex(0x1A, 0x12, 0x26);
    private static final float[] D6_UM = hex(0x46, 0x2A, 0x52);
    private static final float[] D6_LM = hex(0x96, 0x61, 0x73);
    private static final float[] D6_H = hex(0xD8, 0x98, 0x74);

    private McsmBlobShape() {
    }

    // ------------------------------------------------------------- patches

    /** one precomputed smear patch: unit sky directions + per-vertex colours */
    public static final class Patch {
        public final Vec3[] dirs;
        public final int[] r;
        public final int[] g;
        public final int[] b;
        public final int[] a;

        Patch(int n) {
            dirs = new Vec3[n];
            r = new int[n];
            g = new int[n];
            b = new int[n];
            a = new int[n];
        }
    }

    private static long lastKey = Long.MIN_VALUE;
    private static Patch lastPatch;

    /**
     * Build (or fetch from the cache) the smear patch for the current
     * bearing/phase/presence state. The vertex colours bake in the organic
     * silhouette, the deck banding and the streak shading, so per-frame
     * rendering is pure vertex streaming.
     */
    public static Patch patchFor(Vec3 b, float phase, double outer, float presence,
            float[] core, float[] midc, float[] edge, float[] high,
            float w55, float w6) {
        long key = quantKey(b, phase, outer, presence);
        if (key == lastKey && lastPatch != null) {
            return lastPatch;
        }
        // bearing frame with the blob tilt, same convention as the GLSL blob
        Vec3 up = Math.abs(b.y) > 0.985 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 ex = up.cross(b).normalize();
        Vec3 ey = b.cross(ex);
        double ct = Math.cos(TILT);
        double st = Math.sin(TILT);
        Vec3 exT = ex.scale(ct).add(ey.scale(st));
        Vec3 eyT = ey.scale(ct).subtract(ex.scale(st));
        double t = Math.tan(Math.toRadians(outer));
        int n = (NX + 1) * (NY + 1);
        Patch p = new Patch(n);
        int idx = 0;
        for (int i = 0; i <= NY; i++) {
            double sy = -SY_MAX + (2.0 * SY_MAX * i) / NY;
            for (int j = 0; j <= NX; j++) {
                double sx = -SX_MAX + (2.0 * SX_MAX * j) / NX;
                Vec3 d = b.add(exT.scale(sx * t)).add(eyT.scale(sy * t)).normalize();
                float[] c = smearColor(sx, sy, d.y, core, midc, edge, high, w55, w6);
                float m = mask(sx, sy);
                int al = (int) (m * presence * 255.0F);
                p.dirs[idx] = d;
                p.r[idx] = (int) (c[0] * 255.0F);
                p.g[idx] = (int) (c[1] * 255.0F);
                p.b[idx] = (int) (c[2] * 255.0F);
                p.a[idx] = Math.max(0, Math.min(255, al));
                idx++;
            }
        }
        lastKey = key;
        lastPatch = p;
        return p;
    }

    /** stream the patch as quads on the sphere of the given radius around cam */
    public static void stream(Pose pose, VertexConsumer consumer, Patch p,
            Vec3 cam, double radius) {
        for (int i = 0; i < NY; i++) {
            int r0 = i * (NX + 1);
            int r1 = (i + 1) * (NX + 1);
            for (int j = 0; j < NX; j++) {
                vtx(pose, consumer, p, cam, radius, r1 + j);
                vtx(pose, consumer, p, cam, radius, r0 + j);
                vtx(pose, consumer, p, cam, radius, r0 + j + 1);
                vtx(pose, consumer, p, cam, radius, r1 + j + 1);
            }
        }
    }

    private static void vtx(Pose pose, VertexConsumer consumer, Patch p,
            Vec3 cam, double radius, int i) {
        Vec3 d = p.dirs[i];
        consumer.addVertex(pose,
                        (float) (cam.x + d.x * radius),
                        (float) (cam.y + d.y * radius),
                        (float) (cam.z + d.z * radius))
                .setColor(p.r[i], p.g[i], p.b[i], p.a[i])
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    // --------------------------------------------------------- the shape

    /** feathered organic silhouette: 1 inside the smear, 0 outside, no rim */
    private static float mask(double sx, double sy) {
        return (float) smoothstep(1.52, 1.02, shapeField(sx, sy));
    }

    /** warped-oval-plus-lobes distance field: a messy smear, never a disc */
    private static double shapeField(double sx, double sy) {
        double r = Math.sqrt((sx / OVAL_X) * (sx / OVAL_X)
                + (sy / OVAL_Y) * (sy / OVAL_Y));
        double n1 = fbm(sx * 1.9 + 3.7, sy * 1.9 + 9.1, 2);
        double n2 = fbm(sx * 4.1 + 11.3, sy * 4.1 + 2.9, 2);
        double wr = r * (1.0 + 0.34 * (n1 - 0.5)) + 0.14 * (n2 - 0.5);
        double l1x = (sx + 1.05) / 1.05;
        double l1y = (sy + 0.16) / 0.78;
        double lo1 = Math.sqrt(l1x * l1x + l1y * l1y)
                * (1.0 + 0.30 * (fbm(sx * 2.3 + 17.7, sy * 2.3 + 4.4, 2) - 0.5));
        double l2x = (sx - 1.05) / 1.05;
        double l2y = (sy + 0.10) / 0.80;
        double lo2 = Math.sqrt(l2x * l2x + l2y * l2y)
                * (1.0 + 0.30 * (fbm(sx * 2.3 + 31.1, sy * 2.3 + 8.8, 2) - 0.5));
        return Math.min(wr, Math.min(lo1, lo2));
    }

    /** full per-vertex colour: deck banding on a noise-jittered radius,
     *  royal-magenta overhead, phase-6 split and warp-streak shading */
    private static float[] smearColor(double sx, double sy, double ty,
            float[] core, float[] midc, float[] edge, float[] high,
            float w55, float w6) {
        double r = Math.sqrt((sx / OVAL_X) * (sx / OVAL_X)
                + (sy / OVAL_Y) * (sy / OVAL_Y));
        double rr = r * (1.0 + 0.18 * (fbm(sx * 3.1 + 5.5, sy * 3.1 + 1.1, 2) - 0.5));
        float wCore = (float) (1.0 - smoothstep(0.10, 0.42, rr));
        float wMid = (float) (smoothstep(0.16, 0.46, rr)
                * (1.0 - smoothstep(0.58, 0.95, rr)));
        float wEdge = (float) (smoothstep(0.50, 0.88, rr)
                * (1.0 - smoothstep(0.95, 1.32, rr)));
        float rest = Math.max(0.0F, 1.0F - wCore - wMid - wEdge);
        float[] c = new float[3];
        for (int i = 0; i < 3; i++) {
            c[i] = core[i] * wCore + midc[i] * wMid + edge[i] * (wEdge + rest * 0.55F);
        }
        // 5.5-5.9: richer royal magenta overhead
        float upness = Mth.clamp((float) (sy / OVAL_Y) * 0.5F + 0.5F, 0.0F, 1.0F);
        for (int i = 0; i < 3; i++) {
            c[i] += (midc[i] + (high[i] - midc[i]) * 0.6F) * wMid * upness * w55 * 0.5F;
        }
        // phase 6: the four-colour sunset split owns the gradient
        if (w6 > 0.0F) {
            float[] split = p6Split(ty);
            float heart = 0.62F + 0.38F * wCore;
            for (int i = 0; i < 3; i++) {
                c[i] = c[i] + (split[i] * heart - c[i]) * w6;
            }
        }
        // internal streak shading: domain-warped so it reads as smeared paint
        double wx = sx + 1.1 * (fbm(sx * 1.3 + 2.1, sy * 1.3 + 6.7, 2) - 0.5);
        double wy = sy + 1.1 * (fbm(sx * 1.3 + 8.9, sy * 1.3 + 3.3, 2) - 0.5);
        double sh = 0.86 + 0.28 * fbm(wx * 2.6, wy * 2.6, 3);
        for (int i = 0; i < 3; i++) {
            c[i] = (float) Math.max(0.0, Math.min(1.0, c[i] * sh));
        }
        return c;
    }

    /** phase 6: #1A1226 / #462A52 / #966173 / #D89874 keyed to elevation */
    private static float[] p6Split(double ty) {
        float u = Mth.clamp((float) ty * 0.5F + 0.5F, 0.0F, 1.0F);
        float[] c = mix3(D6_H, D6_LM, ss(0.02F, 0.30F, u));
        c = mix3(c, D6_UM, ss(0.26F, 0.55F, u));
        c = mix3(c, D6_Z, ss(0.52F, 0.95F, u));
        return c;
    }

    // ---------------------------------------------------------- utilities

    /** is a shader pack ACTUALLY rendering the sky right now? */
    public static boolean packInUse() {
        try {
            Class<?> api = Class.forName("net.iris.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            Object inUse = api.getMethod("isShaderPackInUse").invoke(inst);
            if (Boolean.TRUE.equals(inUse)) {
                return true;
            }
            // iris present but no pack active: the vanilla pipeline owns the
            // sky, so the Java sky layer must draw it. This is exactly the
            // 1.9.302-1.9.304 bug: iris INSTALLED is not a pack IN USE.
        } catch (Throwable ignored) {
            // no iris at all, or an API mismatch: fall through
        }
        // OptiFine-style shader mods replace the pipeline whenever loaded.
        return modLoaded("optifine") || modLoaded("optifabric") || modLoaded("canvas");
    }

    public static boolean modLoaded(String id) {
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object ans = loaderCls.getMethod("isModLoaded", String.class).invoke(loader, id);
            return Boolean.TRUE.equals(ans);
        } catch (Throwable t) {
            return false;
        }
    }

    private static long quantKey(Vec3 b, float phase, double outer, float presence) {
        double az = Math.atan2(b.z, b.x); // -pi..pi
        int azQ = ((int) Math.floor((az + Math.PI) * (720.0 / (2.0 * Math.PI)))) & 719;
        double el = Math.asin(Mth.clamp(b.y, -1.0, 1.0));
        int elQ = ((int) Math.floor((el + Math.PI * 0.5) * (360.0 / Math.PI))) & 359;
        int phQ = (int) (Mth.clamp(phase, 0.0F, 8.0F) * 50.0F);
        int prQ = (int) (Mth.clamp(presence, 0.0F, 1.0F) * 24.0F);
        int ouQ = (int) (Mth.clamp(outer, 0.0, 120.0) * 4.0);
        return ((long) azQ << 31) | ((long) elQ << 22) | ((long) phQ << 13)
                | ((long) prQ << 8) | (long) ouQ;
    }

    private static float[] hex(int r, int g, int b) {
        return new float[]{r / 255.0F, g / 255.0F, b / 255.0F};
    }

    private static float ss(float lo, float hi, float v) {
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double smoothstep(double lo, double hi, double v) {
        double t = (v - lo) / (hi - lo);
        if (t < 0.0) {
            return 0.0;
        }
        if (t > 1.0) {
            return 1.0;
        }
        return t * t * (3.0 - 2.0 * t);
    }

    private static float[] mix3(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t};
    }

    // deterministic 2D value noise -- stable across frames, zero textures
    private static double hash2(int x, int y) {
        long h = (long) x * 374761393L + (long) y * 668265263L + 1442695040888963407L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h = h ^ (h >>> 16);
        return (h & 0xFFFFFFL) / 16777215.0;
    }

    private static double vnoise(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double xf = x - xi;
        double yf = y - yi;
        double u = xf * xf * (3.0 - 2.0 * xf);
        double v = yf * yf * (3.0 - 2.0 * yf);
        double a = hash2(xi, yi);
        double b = hash2(xi + 1, yi);
        double c = hash2(xi, yi + 1);
        double d = hash2(xi + 1, yi + 1);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    private static double fbm(double x, double y, int oct) {
        double s = 0.0;
        double amp = 0.5;
        double f = 1.0;
        for (int i = 0; i < oct; i++) {
            s += amp * vnoise(x * f, y * f);
            amp *= 0.5;
            f *= 2.0;
        }
        return s;
    }
}
