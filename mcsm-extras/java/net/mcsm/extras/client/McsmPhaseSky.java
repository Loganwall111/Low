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
 * Devouring Storms 1.9.155 — phase sky + BODY-GLUED soft glare.
 *
 * Calm night/day NEVER live here. Purple/pink/teal vaults are PHASE ONLY and
 * only while a real storm is nearby:
 *   5.0–5.35  teal
 *   5.35–5.55 purple
 *   5.55–5.95 pink-magenta + dark blue-black silhouette (5.5)
 *   5.95+     deep purple / rose
 *
 * NO face, NO three-head halo, NO floating mid-air disc, NO giant opaque sphere.
 * Glare is multi-depth soft-fade plates ATTACHED to the storm centre (not a
 * fixed sky radius), so it moves with the body. Dome alpha fades hard at the
 * rim so gradients blend into the calm sky instead of reading as a hard ball.
 */
public final class McsmPhaseSky {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");
    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");

    /** Soft sky wash radius — large but LOW alpha so it never reads as a ball. */
    private static final double DOME_R = 480.0;
    private static final int LON = 36;
    private static final int LAT = 18;

    /** Depth slices along the storm ray, in body-radius units (glued to body). */
    private static final float[] SHELL_DEPTH = {
            -1.10F, -0.55F, -0.15F, 0.10F, 0.40F, 0.75F, 1.15F
    };
    private static final float[] SHELL_SCALE = {
            1.70F, 1.40F, 1.18F, 1.00F, 1.15F, 1.35F, 1.60F
    };
    private static final float[] SHELL_ALPHA = {
            0.18F, 0.30F, 0.45F, 0.55F, 0.40F, 0.25F, 0.14F
    };

    private McsmPhaseSky() {
    }

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double bodyRadius(float phase) {
        if (phase < 4.0F) {
            return 4.0 + 1.5 * phase;
        } else if (phase < 5.0F) {
            return 10.0 + 8.0 * (phase - 4.0F);
        } else {
            return phase < 6.0F ? 18.0 + 22.0 * (phase - 5.0F) : 40.0 + 30.0 * (phase - 6.0F);
        }
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            submitInner(ctx);
        } catch (Throwable ignored) {
        }
    }

    private static void submitInner(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientDistantStormManager.all().isEmpty()) {
            return;
        }
        Vec3 cam = ctx.levelState().cameraRenderState.pos;

        float phase = 0.0F;
        Vec3 stormPos = null;
        double best = Double.MAX_VALUE;
        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            if (d.phase < 4.95F) {
                continue;
            }
            Vec3 c = new Vec3(d.dispX, d.dispY, d.dispZ);
            double dd = c.subtract(cam).length();
            if (dd < best) {
                best = dd;
                phase = d.phase;
                stormPos = c;
            }
        }
        if (stormPos == null) {
            return;
        }

        Vec3 toStorm = stormPos.subtract(cam);
        double dist = toStorm.length();
        if (dist < 1.0E-4) {
            return;
        }
        Vec3 bearing = toStorm.scale(1.0 / dist);

        float wTeal = ramp(phase, 4.95F, 5.10F) * (1.0F - ramp(phase, 5.30F, 5.42F));
        float wPurp = ramp(phase, 5.30F, 5.42F) * (1.0F - ramp(phase, 5.52F, 5.65F));
        float wPink = ramp(phase, 5.52F, 5.65F) * (1.0F - ramp(phase, 5.92F, 6.10F));
        float wSix  = ramp(phase, 5.92F, 6.15F);
        float sky = Mth.clamp(wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
        // tighter near-falloff so far storms don't repaint the whole vault
        float near = 1.0F - Mth.clamp((float) ((best - 700.0) / 900.0), 0.0F, 1.0F);
        sky *= near;
        if (sky <= 0.010F) {
            return;
        }

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        double bodyR = bodyRadius(phase);

        // soft fade vault (NOT a hard sphere) — low alpha, rim dissolves
        dome(poseStack, collector, cam, bearing, wTeal * near, wPurp * near,
                wPink * near, wSix * near, sky);

        // body-glued soft glare — attached to storm centre, moves with it
        glareOnBody(poseStack, collector, cam, stormPos, bearing, bodyR, dist,
                phase, near, wTeal, wPurp, wPink, wSix);
    }

    /* ---- soft fade vault -------------------------------------------------- */

    private static void dome(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cam,
            Vec3 bearing, float wTeal, float wPurp, float wPink, float wSix, float sky) {
        final float[] tZ = { 0.020F, 0.140F, 0.145F };
        final float[] tM = { 0.060F, 0.320F, 0.300F };
        final float[] tH = { 0.280F, 0.520F, 0.460F };
        final float[] pZ = { 0.090F, 0.035F, 0.180F };
        final float[] pM = { 0.320F, 0.090F, 0.420F };
        final float[] pH = { 0.620F, 0.280F, 0.480F };
        final float[] kZ = { 0.180F, 0.040F, 0.220F };
        final float[] kM = { 0.620F, 0.120F, 0.520F };
        final float[] kH = { 0.920F, 0.380F, 0.620F };
        final float[] sZ = { 0.120F, 0.030F, 0.200F };
        final float[] sM = { 0.480F, 0.100F, 0.400F };
        final float[] sH = { 0.780F, 0.320F, 0.380F };

        final float tw = wTeal, pw = wPurp, kw = wPink, sw = wSix;
        final float tot = Math.max(tw + pw + kw + sw, 1.0E-4F);
        final float[] zen = mix4(tZ, pZ, kZ, sZ, tw / tot, pw / tot, kw / tot, sw / tot);
        final float[] mid = mix4(tM, pM, kM, sM, tw / tot, pw / tot, kw / tot, sw / tot);
        final float[] hor = mix4(tH, pH, kH, sH, tw / tot, pw / tot, kw / tot, sw / tot);
        final Vec3 bear = bearing;
        final float amp = sky;

        collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                (pose, consumer) -> {
            for (int j = 0; j < LAT; j++) {
                double y0 = latY(j);
                double y1 = latY(j + 1);
                for (int i = 0; i < LON; i++) {
                    double a0 = i * 2.0 * Math.PI / LON;
                    double a1 = (i + 1) * 2.0 * Math.PI / LON;
                    domeVertex(pose, consumer, cam, onDome(a0, y0), bear, zen, mid, hor, amp, 0.0F, 1.0F);
                    domeVertex(pose, consumer, cam, onDome(a1, y0), bear, zen, mid, hor, amp, 1.0F, 1.0F);
                    domeVertex(pose, consumer, cam, onDome(a1, y1), bear, zen, mid, hor, amp, 1.0F, 0.0F);
                    domeVertex(pose, consumer, cam, onDome(a0, y1), bear, zen, mid, hor, amp, 0.0F, 0.0F);
                }
            }
        });
    }

    private static double latY(int j) {
        return -0.30 + 1.30 * ((double) j / LAT);
    }

    private static Vec3 onDome(double az, double y) {
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        return new Vec3(Math.cos(az) * r, y, Math.sin(az) * r);
    }

    private static void domeVertex(Pose pose, VertexConsumer consumer, Vec3 cam, Vec3 dir,
            Vec3 bearing, float[] zen, float[] mid, float[] hor, float amp, float u, float v) {
        float y = (float) dir.y;
        float t = (float) Math.pow(1.0 - Mth.clamp(y, 0.0F, 1.0F), 1.25);
        float[] c = lerp3(zen, mid, smooth(t, 0.05F, 0.55F));
        c = lerp3(c, hor, smooth(t, 0.55F, 0.96F));

        // only brighten toward the storm — rest of vault stays soft
        float toward = (float) (dir.x * bearing.x + dir.y * bearing.y + dir.z * bearing.z);
        float glow = Mth.clamp((toward - 0.15F) / 0.85F, 0.0F, 1.0F);
        glow = glow * glow;
        c = new float[] {
                c[0] * (1.0F + glow * 0.55F),
                c[1] * (1.0F + glow * 0.35F),
                c[2] * (1.0F + glow * 0.50F)
        };

        // FADE: strong only near the storm bearing, dissolves everywhere else.
        // This is what kills the "giant opaque sphere" read.
        float stormCone = Mth.clamp((toward + 0.05F) / 1.05F, 0.0F, 1.0F);
        stormCone = stormCone * stormCone * (3.0F - 2.0F * stormCone);
        float fog = 0.22F + 0.45F * smooth(t, 0.20F, 1.00F);
        float alpha = amp * fog * stormCone * 165.0F;
        alpha *= Mth.clamp((y + 0.28F) / 0.28F, 0.0F, 1.0F);
        // dissolve the roof so it blends into calm sky
        alpha *= 1.0F - 0.55F * Mth.clamp((y - 0.40F) / 0.55F, 0.0F, 1.0F);
        // soft rim falloff at the cone edge
        alpha *= 0.35F + 0.65F * stormCone;

        vertex(pose, consumer, cam.add(dir.scale(DOME_R)), u, v,
                (int) Mth.clamp(c[0] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(c[1] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(c[2] * 255.0F, 0.0F, 255.0F),
                (int) Mth.clamp(alpha, 0.0F, 255.0F));
    }

    /* ---- body-glued soft glare (moves with the storm) --------------------- */

    private static void glareOnBody(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 cam, Vec3 stormPos, Vec3 bearing, double bodyR, double dist,
            float phase, float near,
            float wTeal, float wPurp, float wPink, float wSix) {
        float a = near * Mth.clamp(wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
        if (a <= 0.010F) {
            return;
        }

        // place the shell ON the storm body (not a far sky disc)
        double placeDist = Math.min(dist * 0.96, dist - bodyR * 0.15);
        placeDist = Math.max(placeDist, bodyR * 1.5);
        Vec3 at = cam.add(bearing.scale(placeDist));

        // shell radius scales with body — glued, not a fixed sky radius
        double baseR = bodyR * 2.4;
        // when far, grow a little so it still reads, but stay on the storm ray
        float far = Mth.clamp((float) ((dist - 120.0) / 600.0), 0.0F, 1.0F);
        baseR = baseR * (1.0F - far) + Math.min(dist * 0.18, bodyR * 4.5) * far;

        float wsum = Math.max(wTeal + wPurp + wPink + wSix, 1.0E-4F);
        float hr = (0.18F * wTeal + 0.55F * wPurp + 0.78F * wPink + 0.62F * wSix) / wsum;
        float hg = (0.55F * wTeal + 0.18F * wPurp + 0.22F * wPink + 0.14F * wSix) / wsum;
        float hb = (0.62F * wTeal + 0.82F * wPurp + 0.72F * wPink + 0.70F * wSix) / wsum;
        int r = (int) (hr * 255.0F);
        int g = (int) (hg * 255.0F);
        int b = (int) (hb * 255.0F);

        // multi-depth soft fade plates along the storm ray (real thickness)
        for (int s = 0; s < SHELL_DEPTH.length; s++) {
            Vec3 slice = at.add(bearing.scale(bodyR * SHELL_DEPTH[s]));
            float sc = SHELL_SCALE[s];
            float aa = SHELL_ALPHA[s];
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    baseR * 1.55 * sc, r, g, b, (int) (a * aa * 32.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    baseR * 1.05 * sc, r, g, b, (int) (a * aa * 48.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), slice, bearing, 0.0, 0.0,
                    baseR * 0.60 * sc, r, g, b, (int) (a * aa * 60.0F));
        }

        // black soft blur core — ON the body
        disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 0.0,
                baseR * 0.72, 4, 3, 8, (int) (a * 150.0F));
        disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, 0.0,
                baseR * 0.42, 2, 1, 4, (int) (a * 190.0F));

        if (wTeal > 0.01F) {
            float tw = wTeal * near;
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    baseR * 1.35, 40, 160, 150, (int) (tw * 65.0F));
        }
        if (wPurp > 0.01F) {
            float pw = wPurp * near;
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    baseR * 1.40, 140, 40, 210, (int) (pw * 70.0F));
        }

        // phase 5.5+ silhouette stack glued to body
        float sil = (wPink + wSix * 0.85F) * near;
        if (sil > 0.01F) {
            // purple/magenta under
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    baseR * 0.78, 220, 60, 180, (int) (sil * 65.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, baseR * 0.08,
                    baseR * 1.05, 90, 28, 130, (int) (sil * 85.0F));
            // blue silhouette
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, 0.0,
                    baseR * 1.15, 28, 54, 128, (int) (sil * 75.0F));
            disc(poseStack, collector, GlowRenderTypes.glow(GLARE), at, bearing, 0.0, baseR * 0.18,
                    baseR * 0.88, 34, 46, 120, (int) (sil * 70.0F));

            // extremely dark blue-black upper-body silhouette OVER purple+blue
            final int dbR = 2, dbG = 4, dbB = 14;
            final int nbR = 1, nbG = 2, nbB = 8;
            final int mbR = 4, mbG = 8, mbB = 22;
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, -baseR * 0.35,
                    baseR * 1.30, dbR, dbG, dbB, (int) (sil * 200.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, -baseR * 0.55, baseR * 0.05,
                    baseR * 1.05, dbR, dbG, dbB, (int) (sil * 190.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, baseR * 0.55, baseR * 0.05,
                    baseR * 1.05, dbR, dbG, dbB, (int) (sil * 190.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, baseR * 0.30,
                    baseR * 1.20, dbR, dbG, dbB, (int) (sil * 220.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, baseR * 0.50,
                    baseR * 1.00, nbR, nbG, nbB, (int) (sil * 240.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, baseR * 0.70,
                    baseR * 0.85, nbR, nbG, nbB, (int) (sil * 230.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, baseR * 0.95,
                    baseR * 1.55, 1, 1, 5, (int) (sil * 170.0F));
            disc(poseStack, collector, GlowRenderTypes.translucent(GLARE), at, bearing, 0.0, baseR * 0.12,
                    baseR * 1.45, mbR, mbG, mbB, (int) (sil * 90.0F));
        }
    }

    private static void disc(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
            Vec3 at, Vec3 view, double dx, double dy, double radius, int r, int g, int b, int alpha) {
        if (alpha <= 2 || radius <= 0.5) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 right = view.cross(upHint).normalize();
            Vec3 up = right.cross(view).normalize();
            Vec3 c = at.add(right.scale(dx)).add(up.scale(dy));
            Vec3 rx = right.scale(radius * 1.10);
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
