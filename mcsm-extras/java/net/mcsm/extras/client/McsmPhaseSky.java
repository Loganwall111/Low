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
 * Nested translucent spheres on the storm centre — traversable, visible from
 * behind, locked to the body (no billboard parallax). NO particles, cubes,
 * debris rings, or face/three-head symbols.
 *
 * Phase glare decks (user frames):
 *   5.0–5.2  teal / soft green-black
 *   5.2–5.45 purple
 *   5.5–5.9  dark purple-black CORE + soft pink/lavender RIM (not full-sky magenta)
 *   6.0+     darker black core, cooler blue-purple rim (phase-6 different)
 *
 * Calm night/day never live here — StoryModeSkyTint + sky shaders own those.
 */
public final class McsmPhaseSky {

    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");

    private static final int LON = 28;
    private static final int LAT = 16;

    /** Nested shell radii as multiples of body radius. Outer = soft fade. */
    private static final float[] SHELL_R = {
            0.45F, 0.70F, 0.95F, 1.25F, 1.65F, 2.15F, 2.80F, 3.60F
    };
    private static final float[] SHELL_A = {
            0.82F, 0.64F, 0.48F, 0.32F, 0.20F, 0.12F, 0.06F, 0.025F
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

    public static Vec3 swayOffset(float phase, float timeSec, double bodyR) {
        if (phase < 4.0F) {
            return Vec3.ZERO;
        }
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

        // phase weights — match user glare frames
        float wTeal = ramp(phase, 4.95F, 5.10F) * (1.0F - ramp(phase, 5.25F, 5.38F));
        float wPurp = ramp(phase, 5.20F, 5.38F) * (1.0F - ramp(phase, 5.48F, 5.58F));
        float wPink = ramp(phase, 5.48F, 5.60F) * (1.0F - ramp(phase, 5.95F, 6.12F)); // 5.5-5.9
        float wSix  = ramp(phase, 5.95F, 6.20F);
        float sky = Mth.clamp(wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
        float near = 1.0F - Mth.clamp((float) ((best - 1100.0) / 1200.0), 0.0F, 1.0F);
        sky *= near;
        if (sky <= 0.012F) {
            return;
        }

        double bodyR = bodyRadius(phase);
        Vec3 centre = stormPos.add(swayOffset(phase, nowSec, bodyR));

        // CORE = dark centre of the halo (black / deep purple-black)
        // User: "the colour of the Halo in the night time" for 5.5+ is the
        // deep purple-black core — NOT flooding the whole calm night sky.
        float[] core = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.02F, 0.06F, 0.06F},   // teal black-green
                new float[]{0.05F, 0.01F, 0.10F},   // purple black
                new float[]{0.04F, 0.01F, 0.08F},   // 5.5 dark purple-black core
                new float[]{0.02F, 0.02F, 0.05F});  // phase6 near-black
        // MID = coloured body of the glare ball
        float[] mid = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.06F, 0.28F, 0.26F},
                new float[]{0.32F, 0.08F, 0.48F},
                new float[]{0.28F, 0.06F, 0.42F},   // purple mid for 5.5 halo
                new float[]{0.12F, 0.06F, 0.22F});  // phase6 cooler
        // OUT / RIM = soft fade into the world (pink-lavender for 5.5, not solid magenta sky)
        float[] out = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.14F, 0.40F, 0.38F},
                new float[]{0.48F, 0.18F, 0.62F},
                new float[]{0.72F, 0.42F, 0.78F},   // soft pink-lavender rim (5.5 frame)
                new float[]{0.35F, 0.22F, 0.48F});  // phase6 blue-purple rim
        float[] rim = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.10F, 0.26F, 0.38F},
                new float[]{0.30F, 0.14F, 0.50F},
                new float[]{0.55F, 0.35F, 0.70F},   // soft lavender edge
                new float[]{0.22F, 0.18F, 0.40F});

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        float amp = sky;

        // outer shells first
        for (int s = SHELL_R.length - 1; s >= 0; s--) {
            float t = s / (float) (SHELL_R.length - 1);
            float[] col;
            if (t < 0.22F) {
                col = lerp3(core, mid, t / 0.22F);
            } else if (t < 0.55F) {
                col = lerp3(mid, out, (t - 0.22F) / 0.33F);
            } else {
                col = lerp3(out, rim, (t - 0.55F) / 0.45F);
            }
            double radius = bodyR * SHELL_R[s];
            float alpha = amp * SHELL_A[s];
            // 5.5+ rim is softer so it doesn't paint the whole sky magenta
            if (wPink > 0.3F && t > 0.55F) {
                alpha *= 0.70F;
            }
            // phase 6 glare is more restrained
            if (wSix > 0.3F) {
                alpha *= 0.75F;
                radius *= 0.92;
            }
            radius *= 1.0 + 0.015 * Math.sin(nowSec * 0.28 + s * 0.35);
            boolean glow = s >= 4 && t < 0.85F;
            sphere(poseStack, collector, centre, radius, col[0], col[1], col[2], alpha, glow);
        }

        // 5.5+ extremely dark blue-black wrap on the back / upper body side
        float sil = (wPink + wSix * 0.7F + wPurp * 0.25F) * near;
        if (sil > 0.01F) {
            sphere(poseStack, collector, centre, bodyR * 4.8,
                    0.015F, 0.03F, 0.10F, amp * sil * 0.09F, false);
            sphere(poseStack, collector, centre, bodyR * 3.6,
                    0.03F, 0.04F, 0.12F, amp * sil * 0.12F, false);
        }
    }

    private static void sphere(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 centre, double radius, float cr, float cg, float cb, float alpha, boolean glow) {
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
                // soft equator bias so poles fade cleaner into sky
                float band0 = (float) (0.50 + 0.50 * Math.sin(v0));
                float band1 = (float) (0.50 + 0.50 * Math.sin(v1));
                for (int i = 0; i < LON; i++) {
                    double u0 = 2.0 * Math.PI * i / LON;
                    double u1 = 2.0 * Math.PI * (i + 1) / LON;
                    Vec3 p00 = centre.add(r0 * Math.cos(u0) * radius, y0 * radius, r0 * Math.sin(u0) * radius);
                    Vec3 p10 = centre.add(r0 * Math.cos(u1) * radius, y0 * radius, r0 * Math.sin(u1) * radius);
                    Vec3 p11 = centre.add(r1 * Math.cos(u1) * radius, y1 * radius, r1 * Math.sin(u1) * radius);
                    Vec3 p01 = centre.add(r1 * Math.cos(u0) * radius, y1 * radius, r1 * Math.sin(u0) * radius);
                    int a0 = Mth.clamp((int) (aa * band0), 0, 255);
                    int a1 = Mth.clamp((int) (aa * band1), 0, 255);
                    vertex(pose, consumer, p00, 0, 0, ar, ag, ab, a0);
                    vertex(pose, consumer, p10, 1, 0, ar, ag, ab, a0);
                    vertex(pose, consumer, p11, 1, 1, ar, ag, ab, a1);
                    vertex(pose, consumer, p01, 0, 1, ar, ag, ab, a1);
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
