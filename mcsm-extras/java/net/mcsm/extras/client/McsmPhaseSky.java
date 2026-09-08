package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Devouring Storms 1.9.153 — phase sky + soft glare glued to storm + sky.
 *
 * Calm night/day NEVER live here. Purple/pink/teal vaults are PHASE ONLY:
 *   phase 5.0–5.35  teal / turquoise sky  (user "phase 5 turquoise sky")
 *   phase 5.35–5.55 purple vault          (user "phase5sky purple")
 *   phase 5.55–5.95 pink-magenta-black    (user phase 5.5 — was SKIPPED)
 *   phase 5.95+     deep purple + rose    (user "phase6sky") — red/orange notes
 *
 * Backdrops (only these, NO face/heads/symbol):
 *   black soft blur core
 *   green/teal wash for phase 5
 *   purple wash for phase 5.4
 *   pink/magenta/black stack for phase 5.5+
 *
 * The glare is a SOFT FADE GRADIENT (not an opaque ball), multi-depth, glued
 * to the storm bearing and the sky so it moves with both.
 */
public final class McsmPhaseSky {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");
    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");

    /** Full-sky dome radius. Halo sits just behind the body shell. */
    private static final double DOME_R = 520.0;
    private static final double HALO_R = 240.0;
    /** Soft multi-depth slices — fade gradient, not a hard disc. */
    private static final double[] HALO_DEPTH = { -55.0, -28.0, -10.0, 0.0, 14.0, 28.0, 48.0 };
    private static final float[] HALO_SCALE = { 1.55F, 1.35F, 1.18F, 1.00F, 1.12F, 1.28F, 1.48F };
    private static final float[] HALO_ALPHA = { 0.22F, 0.38F, 0.55F, 0.72F, 0.50F, 0.32F, 0.18F };

    private static final int LON = 32;
    private static final int LAT = 16;

    private McsmPhaseSky() {
    }

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            submitInner(ctx);
        } catch (Throwable ignored) {
            // never take the level render down with us
        }
    }

    private static void submitInner(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientDistantStormManager.all().isEmpty()) {
            return;
        }
        Vec3 cam = ctx.levelState().cameraRenderState.pos;

        float phase = 0.0F;
        Vec3 bearing = null;
        double best = Double.MAX_VALUE;
        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            // phase sky starts at 5.0 (teal). Below that: no purple dome.
            if (d.phase < 4.95F) {
                continue;
            }
            Vec3 c = new Vec3(d.dispX, d.dispY, d.dispZ);
            double dd = c.subtract(cam).length();
            if (dd < best) {
                best = dd;
                phase = d.phase;
                bearing = dd > 1.0E-4 ? c.subtract(cam).scale(1.0D / dd) : null;
            }
        }
        if (bearing == null) {
            return;
        }

        // ---- phase weights (user strip order) -----------------------------
        // 1 = phase 5 teal, 2 = 5.4 purple, 3 = 5.5 pink-magenta, 4 = 6+ deep
        float wTeal = ramp(phase, 4.95F, 5.10F) * (1.0F - ramp(phase, 5.30F, 5.42F));
        float wPurp = ramp(phase, 5.30F, 5.42F) * (1.0F - ramp(phase, 5.52F, 5.65F));
        float wPink = ramp(phase, 5.52F, 5.65F) * (1.0F - ramp(phase, 5.92F, 6.10F));
        float wSix  = ramp(phase, 5.92F, 6.15F);
        float sky = Mth.clamp(wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
        // fall off with distance so far storms don't repaint the whole night
        float near = 1.0F - Mth.clamp((float) ((best - 900.0) / 1100.0), 0.0F, 1.0F);
        sky *= near;
        if (sky <= 0.008F) {
            return;
        }

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();

        dome(poseStack, collector, cam, bearing, wTeal * near, wPurp * near, wPink * near, wSix * near, sky);
        glare(poseStack, collector, cam, bearing, phase, near, wTeal, wPurp, wPink, wSix);
    }

    /* ---- the phase vault (soft gradient dome, NOT an opaque ball) -------- */

    /**
     * Palettes sampled from user strips:
     *   phase 5 turquoise: deep teal zenith → mint mid → pale aqua horizon
     *   phase 5.4 purple:  deep indigo zenith → violet mid → mauve horizon
     *   phase 5.5 pink:    deep magenta zenith → hot pink mid → light pink hor
     *   phase 6+:          deep purple zenith → magenta mid → rose/dust hor
     *                      (6+ may carry a faint red/orange belly; 5.5 does NOT)
     */
    private static void dome(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam,
            Vec3 bearing, float wTeal, float wPurp, float wPink, float wSix, float sky) {
        // phase 5 — turquoise / teal (glare pase 5 + phase 5 turquoise sky)
        final float[] tZ = { 0.020F, 0.140F, 0.145F };
        final float[] tM = { 0.060F, 0.320F, 0.300F };
        final float[] tH = { 0.280F, 0.520F, 0.460F };
        // phase 5.4 — purple (phase5sky purple)
        final float[] pZ = { 0.090F, 0.035F, 0.180F };
        final float[] pM = { 0.320F, 0.090F, 0.420F };
        final float[] pH = { 0.620F, 0.280F, 0.480F };
        // phase 5.5 — pink / magenta / black stack (NO red/orange)
        final float[] kZ = { 0.180F, 0.040F, 0.220F };
        final float[] kM = { 0.620F, 0.120F, 0.520F };
        final float[] kH = { 0.920F, 0.380F, 0.620F };
        // phase 6+ — deep purple with rose + faint ember belly
        final float[] sZ = { 0.120F, 0.030F, 0.200F };
        final float[] sM = { 0.480F, 0.100F, 0.400F };
        final float[] sH = { 0.780F, 0.320F, 0.380F }; // slight warm/rose, not pure pink

        final float tw = wTeal, pw = wPurp, kw = wPink, sw = wSix;
        final float tot = Math.max(tw + pw + kw + sw, 1.0E-4F);
        final float[] zen = mix4(tZ, pZ, kZ, sZ, tw / tot, pw / tot, kw / tot, sw / tot);
        final float[] mid = mix4(tM, pM, kM, sM, tw / tot, pw / tot, kw / tot, sw / tot);
        final float[] hor = mix4(tH, pH, kH, sH, tw / tot, pw / tot, kw / tot, sw / tot);
        final Vec3 bear = bearing;
        final float amp = sky;

        // translucent soft dome — alpha falls off so it FADE-gradients into
        // the calm sky instead of reading as an opaque ball
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                (pose, consumer) -> {
            for (int j = 0; j < LAT; j++) {
                double y0 = latY(j);
                double y1 = latY(j + 1);
                for (int i = 0; i < LON; i++) {
                    double a0 = i * 2.0 * Math.PI / LON;
                    double a1 = (i + 1) * 2.0 * Math.PI / LON;
                    Vec3 d00 = onDome(a0, y0);
                    Vec3 d10 = onDome(a1, y0);
                    Vec3 d11 = onDome(a1, y1);
                    Vec3 d01 = onDome(a0, y1);
                    domeVertex(pose, consumer, cam, d00, bear, zen, mid, hor, amp, 0.0F, 1.0F);
                    domeVertex(pose, consumer, cam, d10, bear, zen, mid, hor, amp, 1.0F, 1.0F);
                    domeVertex(pose, consumer, cam, d11, bear, zen, mid, hor, amp, 1.0F, 0.0F);
                    domeVertex(pose, consumer, cam, d01, bear, zen, mid, hor, amp, 0.0F, 0.0F);
                }
            }
        });
    }

    private static double latY(int j) {
        double t = (double) j / LAT;
        return -0.35 + 1.35 * t;
    }

    private static Vec3 onDome(double az, double y) {
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        return new Vec3(Math.cos(az) * r, y, Math.sin(az) * r);
    }

    private static void domeVertex(Pose pose, VertexConsumer consumer, Vec3 cam, Vec3 dir,
            Vec3 bearing, float[] zen, float[] mid, float[] hor, float amp, float u, float v) {
        float y = (float) dir.y;
        float t = (float) Math.pow(1.0 - Mth.clamp(y, 0.0F, 1.0F), 1.30);
        float[] c = lerp3(zen, mid, smooth(t, 0.05F, 0.55F));
        c = lerp3(c, hor, smooth(t, 0.55F, 0.96F));

        // lit from the storm bearing — soft, not a hard disc
        float toward = (float) (dir.x * bearing.x + dir.y * bearing.y + dir.z * bearing.z);
        float glow = Mth.clamp((toward - 0.05F) / 0.95F, 0.0F, 1.0F);
        glow = glow * glow * 0.45F;
        c = new float[] { c[0] * (1.0F + glow * 0.85F), c[1] * (1.0F + glow * 0.50F),
                c[2] * (1.0F + glow * 0.80F) };

        // FADE gradient alpha: thickest near horizon + toward storm, soft at
        // zenith edges so it never reads as an opaque ball
        float fog = 0.38F + 0.50F * smooth(t, 0.15F, 1.00F);
        float edge = Mth.clamp((toward + 0.35F) / 1.20F, 0.15F, 1.0F);
        float alpha = amp * fog * edge * 210.0F;
        alpha *= Mth.clamp((y + 0.34F) / 0.30F, 0.0F, 1.0F);
        // soften the roof so the dome dissolves into calm sky
        alpha *= 1.0F - 0.35F * Mth.clamp((y - 0.55F) / 0.45F, 0.0F, 1.0F);

        vertex(pose, consumer, cam.add(dir.scale(DOME_R)), u, v,
                (int) Mth.clamp(c[0] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(c[1] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(c[2] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(alpha, 0.0F, 255.0F));
    }

    /* ---- soft glare + backdrops glued to storm + sky --------------------- */

    /**
     * Soft fade-gradient glare (user glare pase 5 / glarephase6plus).
     * NO face, NO three-heads, NO symbol. Only:
     *   black soft blur core
     *   teal wash (phase 5)
     *   purple wash (phase 5.4)
     *   pink/magenta/black stack (phase 5.5+)
     */
    private static void glare(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam,
            Vec3 bearing, float phase, float near,
            float wTeal, float wPurp, float wPink, float wSix) {
        Vec3 at = cam.add(bearing.scale(HALO_R));
        float a = near * Mth.clamp(wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
        if (a <= 0.008F) {
            return;
        }

        // outer soft aura colour from the active phase
        float hr, hg, hb;
        float wsum = Math.max(wTeal + wPurp + wPink + wSix, 1.0E-4F);
        // teal glow, purple glow, pink glow, six glow — from user glare strips
        hr = (0.18F * wTeal + 0.55F * wPurp + 0.78F * wPink + 0.62F * wSix) / wsum;
        hg = (0.55F * wTeal + 0.18F * wPurp + 0.22F * wPink + 0.14F * wSix) / wsum;
        hb = (0.62F * wTeal + 0.82F * wPurp + 0.72F * wPink + 0.70F * wSix) / wsum;
        int r = (int) (hr * 255.0F);
        int g = (int) (hg * 255.0F);
        int b = (int) (hb * 255.0F);

        // multi-depth SOFT gradient discs — long falloff, never opaque ball
        for (int s = 0; s < HALO_DEPTH.length; s++) {
            Vec3 slice = cam.add(bearing.scale(HALO_R + HALO_DEPTH[s]));
            float sc = HALO_SCALE[s];
            float aa = HALO_ALPHA[s];
            // very wide soft skirt
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    210.0 * sc, r, g, b, (int) (a * aa * 28.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    140.0 * sc, r, g, b, (int) (a * aa * 42.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    78.0 * sc, r, g, b, (int) (a * aa * 55.0F));
        }

        // ---- black soft blur core (always, once phase sky is on) ----------
        disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 0.0,
                95.0, 4, 3, 8, (int) (a * 160.0F));
        disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 0.0,
                55.0, 2, 1, 4, (int) (a * 200.0F));

        // ---- phase 5 teal/green wash --------------------------------------
        if (wTeal > 0.01F) {
            float tw = wTeal * near;
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    175.0, 40, 160, 150, (int) (tw * 70.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    110.0, 30, 200, 180, (int) (tw * 90.0F));
        }

        // ---- phase 5.4 purple wash ----------------------------------------
        if (wPurp > 0.01F) {
            float pw = wPurp * near;
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    185.0, 140, 40, 210, (int) (pw * 75.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    120.0, 100, 30, 190, (int) (pw * 95.0F));
        }

        // ---- phase 5.5 pink / magenta / black stack -----------------------
        // (also carries into 6 as the silhouette stack)
        float sil = (wPink + wSix * 0.85F) * near;
        if (sil > 0.01F) {
            // dark-blue over storm
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    150.0, 28, 54, 128, (int) (sil * 80.0F));
            // dark purple wrap
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 12.0,
                    135.0, 90, 28, 130, (int) (sil * 95.0F));
            // hot pink / magenta mid
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    100.0, 220, 60, 180, (int) (sil * 70.0F));
            // moon-blue fringe
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 30.0,
                    115.0, 34, 46, 120, (int) (sil * 75.0F));
            // black atmospheric top (soft fade, not a hard disc)
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 70.0,
                    200.0, 3, 2, 7, (int) (sil * 140.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 120.0,
                    260.0, 2, 1, 5, (int) (sil * 100.0F));
        }
    }

    /* ---- primitives ------------------------------------------------------- */

    private static void disc(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
            Vec3 at, Vec3 view, double dx, double dy, double radius, int r, int g, int b, int alpha) {
        if (alpha <= 2) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 right = view.cross(upHint).normalize();
            Vec3 up = right.cross(view).normalize();
            Vec3 c = at.add(right.scale(dx)).add(up.scale(dy));
            Vec3 rx = right.scale(radius * 1.12);
            Vec3 uy = up.scale(radius);
            int fa = Mth.clamp(alpha, 0, 255);
            vertex(pose, consumer, c.subtract(rx).subtract(uy), 0.0F, 1.0F, r, g, b, fa);
            vertex(pose, consumer, c.add(rx).subtract(uy), 1.0F, 1.0F, r, g, b, fa);
            vertex(pose, consumer, c.add(rx).add(uy), 1.0F, 0.0F, r, g, b, fa);
            vertex(pose, consumer, c.subtract(rx).add(uy), 0.0F, 0.0F, r, g, b, fa);
        });
    }

    private static float smooth(float x, float lo, float hi) {
        float t = Mth.clamp((x - lo) / Math.max(hi - lo, 1.0E-4F), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] lerp3(float[] a, float[] b, float t) {
        return new float[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t };
    }

    private static float[] mix4(float[] a, float[] b, float[] c, float[] d,
            float wa, float wb, float wc, float wd) {
        return new float[] {
                a[0] * wa + b[0] * wb + c[0] * wc + d[0] * wd,
                a[1] * wa + b[1] * wb + c[1] * wc + d[1] * wd,
                a[2] * wa + b[2] * wb + c[2] * wc + d[2] * wd
        };
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Vec3 at,
            float u, float v, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
