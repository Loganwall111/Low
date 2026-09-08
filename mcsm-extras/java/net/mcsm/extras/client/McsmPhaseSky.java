package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Giant 3D gradient volume glued to the storm in WORLD space.
 *
 * Not a 2D billboard. Nested translucent spheres sit on the storm's world
 * position so:
 *   - the volume moves with the storm (not with the camera)
 *   - the player can walk through it and still see it from behind
 *   - the storm body is nested inside the volume
 *
 * NO particles, NO orbiting dots, NO debris cubes, NO face/three-head.
 * Soft fade edges. Phase colour decks from user strips.
 * Calm night/day never live here.
 */
public final class McsmPhaseSky {

    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");

    /** Sphere mesh resolution. */
    private static final int LON = 24;
    private static final int LAT = 14;

    /**
     * Nested shell radii as multiples of body radius. Inner = black smudge core,
     * outer = blue/purple fade into the world sky. Drawn back-to-front for
     * correct translucency when the camera is inside.
     */
    private static final float[] SHELL_R = {
            0.55F, 0.85F, 1.15F, 1.55F, 2.10F, 2.80F, 3.60F, 4.60F
    };
    private static final float[] SHELL_A = {
            0.72F, 0.58F, 0.48F, 0.36F, 0.26F, 0.18F, 0.11F, 0.06F
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

    /** Shared sway so shell + blob stay locked (radians-scale world offset). */
    public static Vec3 swayOffset(float phase, float timeSec, double bodyR) {
        if (phase < 4.0F) {
            return Vec3.ZERO;
        }
        // slow left-right sway; amplitude grows with phase
        float amp = (float) (bodyR * (0.08 + 0.06 * Mth.clamp((phase - 4.0F) / 3.0F, 0.0F, 1.0F)));
        float x = Mth.sin(timeSec * 0.22F) * amp;
        float z = Mth.sin(timeSec * 0.17F + 1.3F) * amp * 0.55F;
        float y = Mth.sin(timeSec * 0.13F) * amp * 0.18F;
        return new Vec3(x, y, z);
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
        float gt = (float) (mc.level.getGameTime() % 240000L)
                + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float nowSec = gt * 0.05F;
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

        float wTeal = ramp(phase, 4.95F, 5.10F) * (1.0F - ramp(phase, 5.30F, 5.42F));
        float wPurp = ramp(phase, 5.30F, 5.42F) * (1.0F - ramp(phase, 5.52F, 5.65F));
        float wPink = ramp(phase, 5.52F, 5.65F) * (1.0F - ramp(phase, 5.92F, 6.10F));
        float wSix  = ramp(phase, 5.92F, 6.15F);
        float sky = Mth.clamp(wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
        float near = 1.0F - Mth.clamp((float) ((best - 900.0) / 1100.0), 0.0F, 1.0F);
        sky *= near;
        if (sky <= 0.012F) {
            return;
        }

        double bodyR = bodyRadius(phase);
        Vec3 centre = stormPos.add(swayOffset(phase, nowSec, bodyR));

        // phase core / mid / outer colours (user strips)
        float[] core = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.02F, 0.08F, 0.08F},   // teal black-green
                new float[]{0.06F, 0.02F, 0.12F},   // purple black
                new float[]{0.12F, 0.02F, 0.10F},   // pink black
                new float[]{0.10F, 0.02F, 0.08F});  // six black
        float[] mid = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.08F, 0.36F, 0.34F},
                new float[]{0.42F, 0.10F, 0.55F},
                new float[]{0.72F, 0.14F, 0.55F},
                new float[]{0.55F, 0.12F, 0.42F});
        float[] out = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.18F, 0.48F, 0.46F},
                new float[]{0.55F, 0.22F, 0.72F},
                new float[]{0.90F, 0.35F, 0.70F},
                new float[]{0.70F, 0.28F, 0.48F});
        // outer rim: blue + purple smudge around the outside
        float[] rim = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.12F, 0.28F, 0.42F},
                new float[]{0.22F, 0.12F, 0.55F},
                new float[]{0.28F, 0.10F, 0.48F},
                new float[]{0.30F, 0.10F, 0.40F});

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        float amp = sky;

        // draw outer shells first (far), then inner (near) so translucency stacks
        for (int s = SHELL_R.length - 1; s >= 0; s--) {
            float t = s / (float) (SHELL_R.length - 1);
            // colour: core (black smudge) → mid → out → rim (blue/purple outside)
            float[] col;
            if (t < 0.25F) {
                col = lerp3(core, mid, t / 0.25F);
            } else if (t < 0.60F) {
                col = lerp3(mid, out, (t - 0.25F) / 0.35F);
            } else {
                col = lerp3(out, rim, (t - 0.60F) / 0.40F);
            }
            double radius = bodyR * SHELL_R[s];
            float alpha = amp * SHELL_A[s];
            // soft breathe so it doesn't feel static/flat
            radius *= 1.0 + 0.02 * Math.sin(nowSec * 0.31 + s * 0.4);
            boolean glow = s >= 3; // outer shells additive, inner translucent black
            sphere(poseStack, collector, centre, cam, radius, col[0], col[1], col[2], alpha, glow);
        }

        // extra outer blue-black smudge ring (the "outside" dark wrap)
        float sil = (wPink + wSix * 0.85F + wPurp * 0.35F) * near;
        if (sil > 0.01F) {
            sphere(poseStack, collector, centre, cam, bodyR * 5.2,
                    0.02F, 0.04F, 0.12F, amp * sil * 0.10F, false);
            sphere(poseStack, collector, centre, cam, bodyR * 4.0,
                    0.04F, 0.06F, 0.18F, amp * sil * 0.14F, false);
        }
    }

    /** Full UV sphere in WORLD space, centred on storm. Two-sided via full mesh. */
    private static void sphere(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 centre, Vec3 cam, double radius, float cr, float cg, float cb, float alpha, boolean glow) {
        if (alpha <= 0.008F || radius < 1.0) {
            return;
        }
        int ar = (int) (cr * 255.0F);
        int ag = (int) (cg * 255.0F);
        int ab = (int) (cb * 255.0F);
        int aa = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        var type = glow ? GlowRenderTypes.glow(GLARE) : GlowRenderTypes.translucent(GLARE);

        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            for (int j = 0; j < LAT; j++) {
                double v0 = Math.PI * j / LAT;
                double v1 = Math.PI * (j + 1) / LAT;
                double y0 = Math.cos(v0);
                double y1 = Math.cos(v1);
                double r0 = Math.sin(v0);
                double r1 = Math.sin(v1);
                // fade alpha toward poles/equator edges for soft blend
                float band0 = (float) (0.55 + 0.45 * Math.sin(v0));
                float band1 = (float) (0.55 + 0.45 * Math.sin(v1));
                for (int i = 0; i < LON; i++) {
                    double u0 = 2.0 * Math.PI * i / LON;
                    double u1 = 2.0 * Math.PI * (i + 1) / LON;
                    Vec3 p00 = centre.add(r0 * Math.cos(u0) * radius, y0 * radius, r0 * Math.sin(u0) * radius);
                    Vec3 p10 = centre.add(r0 * Math.cos(u1) * radius, y0 * radius, r0 * Math.sin(u1) * radius);
                    Vec3 p11 = centre.add(r1 * Math.cos(u1) * radius, y1 * radius, r1 * Math.sin(u1) * radius);
                    Vec3 p01 = centre.add(r1 * Math.cos(u0) * radius, y1 * radius, r1 * Math.sin(u0) * radius);
                    int a0 = Mth.clamp((int) (aa * band0), 0, 255);
                    int a1 = Mth.clamp((int) (aa * band1), 0, 255);
                    // front face
                    vertex(pose, consumer, p00, 0, 0, ar, ag, ab, a0);
                    vertex(pose, consumer, p10, 1, 0, ar, ag, ab, a0);
                    vertex(pose, consumer, p11, 1, 1, ar, ag, ab, a1);
                    vertex(pose, consumer, p01, 0, 1, ar, ag, ab, a1);
                    // back face (visible from inside / behind)
                    vertex(pose, consumer, p00, 0, 0, ar, ag, ab, a0);
                    vertex(pose, consumer, p01, 0, 1, ar, ag, ab, a1);
                    vertex(pose, consumer, p11, 1, 1, ar, ag, ab, a1);
                    vertex(pose, consumer, p10, 1, 0, ar, ag, ab, a0);
                }
            }
        });
    }

    private static float[] mixPhase(float wT, float wP, float wK, float wS,
            float[] teal, float[] purp, float[] pink, float[] six) {
        float tot = Math.max(wT + wP + wK + wS, 1.0E-4F);
        return new float[] {
                (teal[0] * wT + purp[0] * wP + pink[0] * wK + six[0] * wS) / tot,
                (teal[1] * wT + purp[1] * wP + pink[1] * wK + six[1] * wS) / tot,
                (teal[2] * wT + purp[2] * wP + pink[2] * wK + six[2] * wS) / tot
        };
    }

    private static float[] lerp3(float[] a, float[] b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return new float[] {
                a[0] + (b[0] - a[0]) * t,
                a[1] + (b[1] - a[1]) * t,
                a[2] + (b[2] - a[2]) * t
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
