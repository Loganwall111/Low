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
 * behind, locked to the body (no billboard parallax). NO face/three-head symbols.
 *
 * Phase glare decks (user frames):
 *   5.0–5.2  teal / soft green-black
 *   5.2–5.45 purple
 *   5.5–5.9  dark purple-black CORE + soft pink/lavender RIM (not full-sky magenta)
 *   6.0+     darker black core + FOUR gigantic black orbital rings (side-flanking,
 *            horizontal drift, direction reversals)
 *   7.0+     sky-swallowing ring field — dozens of rings, top of vault gone
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
            0.40F, 0.65F, 0.90F, 1.20F, 1.55F, 2.00F, 2.55F, 3.20F
    };
    private static final float[] SHELL_A = {
            0.85F, 0.68F, 0.50F, 0.34F, 0.20F, 0.11F, 0.05F, 0.018F
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
        } else if (phase < 6.0F) {
            return 18.0 + 22.0 * (phase - 5.0F);
        } else if (phase < 7.0F) {
            // phase 6: gigantic body that carries the four mega black rings
            return 40.0 + 55.0 * (phase - 6.0F);
        } else {
            // phase 7+: sky-swallowing mass — rings fill past the top of the vault
            return 95.0 + 80.0 * Math.min(phase - 7.0F, 3.0F);
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
                new float[]{0.85F, 0.45F, 0.72F},   // 5.5 soft pink-magenta rim (user sky ref)
                new float[]{0.28F, 0.18F, 0.42F});  // phase6 cooler
        float[] rim = mixPhase(wTeal, wPurp, wPink, wSix,
                new float[]{0.10F, 0.26F, 0.38F},
                new float[]{0.30F, 0.14F, 0.50F},
                new float[]{0.70F, 0.40F, 0.68F},   // soft pink edge, not full-sky flood
                new float[]{0.18F, 0.14F, 0.35F});

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

        // ---------- PHASE 6 / 7 GIGANTIC BLACK ORBITAL RINGS --------------
        // User: phase 6 has ~4 huge black rings surrounding the sides of the
        // storm, drifting horizontally and reversing direction. Phase 7 has
        // so many rings you cannot see the top of the sky.
        drawOrbitalRings(poseStack, collector, centre, bodyR, phase, nowSec, amp * near);
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


    /**
     * Phase-6: four gigantic black rings flanking the storm, each on its own
     * horizontal plane, slowly drifting and reversing spin direction.
     * Phase-7+: a sky-filling stack of dozens of rings — top of vault vanishes.
     */
    private static void drawOrbitalRings(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 centre, double bodyR, float phase, float nowSec, float amp) {
        float w6 = ramp(phase, 5.95F, 6.25F);
        float w7 = ramp(phase, 6.85F, 7.15F);
        if (w6 < 0.01F && w7 < 0.01F) {
            return;
        }
        // --- phase 6: four mega rings ---
        if (w6 > 0.01F && w7 < 0.85F) {
            // four rings at staggered elevations around the body flanks
            final float[] elev = { -0.55F, -0.10F, 0.35F, 0.85F };
            final float[] radMul = { 2.6F, 3.4F, 4.2F, 5.1F };
            final float[] thick = { 0.22F, 0.28F, 0.34F, 0.40F };
            final float[] speed = { 0.11F, -0.09F, 0.13F, -0.07F }; // base direction
            final float[] tilt = { 0.18F, -0.12F, 0.22F, -0.16F };
            float fade = w6 * (1.0F - w7 * 0.55F);
            for (int r = 0; r < 4; r++) {
                // direction reverse: slow sine flips the spin every ~20-30s
                float flip = Mth.sin(nowSec * (0.045F + r * 0.011F) + r * 1.7F);
                float ang = nowSec * speed[r] * (0.55F + 0.45F * Math.signum(flip + 1.0E-4F))
                        + flip * 0.85F
                        + r * 1.1F;
                // horizontal drift of the ring centre (summer / side sway)
                double driftX = Math.sin(nowSec * (0.07 + r * 0.02) + r) * bodyR * 0.55;
                double driftZ = Math.cos(nowSec * (0.055 + r * 0.018) + r * 2.1) * bodyR * 0.45;
                double driftY = Math.sin(nowSec * 0.04 + r) * bodyR * 0.12;
                Vec3 ringC = centre.add(driftX, bodyR * elev[r] + driftY, driftZ);
                float a = amp * fade * (0.72F + 0.18F * (r % 2));
                blackRing(poseStack, collector, ringC, bodyR * radMul[r],
                        bodyR * thick[r], tilt[r], ang, a, 48);
            }
        }
        // --- phase 7: sky-swallowing ring field ---
        if (w7 > 0.01F) {
            int count = 22 + (int) (18.0F * Math.min(w7, 1.0F)); // up to ~40 rings
            float fade = w7;
            for (int r = 0; r < count; r++) {
                float t = r / (float) Math.max(count - 1, 1);
                // stack from below body to FAR above — past top of sky
                float elev = -1.2F + t * 5.5F;
                float radMul = 2.2F + t * 9.5F + 0.6F * Mth.sin(r * 1.3F);
                float thick = 0.18F + 0.35F * (0.4F + 0.6F * t);
                float baseSp = ((r & 1) == 0 ? 1.0F : -1.0F) * (0.05F + 0.04F * (r % 5) / 4.0F);
                float flip = Mth.sin(nowSec * (0.03F + (r % 7) * 0.007F) + r * 0.9F);
                float ang = nowSec * baseSp * (0.4F + 0.6F * Math.abs(flip))
                        + flip * 1.2F
                        + r * 0.55F;
                float tilt = 0.08F * Mth.sin(r * 0.7F + nowSec * 0.02F);
                double driftX = Math.sin(nowSec * 0.04 + r * 0.4) * bodyR * (0.3 + 0.5 * t);
                double driftZ = Math.cos(nowSec * 0.035 + r * 0.5) * bodyR * (0.25 + 0.45 * t);
                Vec3 ringC = centre.add(driftX, bodyR * elev, driftZ);
                // denser / more opaque higher so the vault disappears
                float a = amp * fade * (0.35F + 0.55F * t);
                int segs = t > 0.6F ? 56 : 40;
                blackRing(poseStack, collector, ringC, bodyR * radMul,
                        bodyR * thick, tilt, ang, a, segs);
            }
            // extra outer wash discs so the sky top is pure black mass
            for (int k = 0; k < 6; k++) {
                float elev = 2.5F + k * 0.85F;
                double rad = bodyR * (8.0 + k * 2.2);
                Vec3 c = centre.add(0.0, bodyR * elev, 0.0);
                sphere(poseStack, collector, c, rad * 0.35,
                        0.01F, 0.01F, 0.02F, amp * fade * (0.08F + 0.04F * k), false);
            }
        }
    }

    /**
     * Thick black torus approx: segs cross-section quads around a tilted plane.
     * Spin angle rotates the ring in its plane; tilt leans it for 3D read.
     */
    private static void blackRing(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 centre, double majorR, double tubeR, float tilt, float spin,
            float alpha, int segs) {
        if (alpha <= 0.01F || majorR < 2.0 || tubeR < 0.5) {
            return;
        }
        int aa = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        // near-black with slight blue edge so it reads against purple storm sky
        int cr = 4, cg = 5, cb = 12;
        var type = GlowRenderTypes.translucent(GLARE);
        final double cosT = Math.cos(tilt);
        final double sinT = Math.sin(tilt);
        final double cosS = Math.cos(spin);
        final double sinS = Math.sin(spin);
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            // two concentric bands (inner/outer tube) for thickness
            for (int band = 0; band < 3; band++) {
                double br = majorR + (band - 1) * tubeR * 0.55;
                double halfH = tubeR * (band == 1 ? 1.0 : 0.55);
                int ba = band == 1 ? aa : (aa * 2 / 3);
                for (int i = 0; i < segs; i++) {
                    double u0 = 2.0 * Math.PI * i / segs;
                    double u1 = 2.0 * Math.PI * (i + 1) / segs;
                    // ring local XY, then tilt around X, then spin around Y
                    Vec3 a0 = ringPoint(centre, br, -halfH, u0, cosT, sinT, cosS, sinS);
                    Vec3 a1 = ringPoint(centre, br, -halfH, u1, cosT, sinT, cosS, sinS);
                    Vec3 b0 = ringPoint(centre, br, halfH, u0, cosT, sinT, cosS, sinS);
                    Vec3 b1 = ringPoint(centre, br, halfH, u1, cosT, sinT, cosS, sinS);
                    // front
                    vertex(pose, consumer, a0, 0, 0, cr, cg, cb, ba);
                    vertex(pose, consumer, a1, 1, 0, cr, cg, cb, ba);
                    vertex(pose, consumer, b1, 1, 1, cr, cg, cb, ba);
                    vertex(pose, consumer, b0, 0, 1, cr, cg, cb, ba);
                    // back
                    vertex(pose, consumer, a0, 0, 0, cr, cg, cb, ba);
                    vertex(pose, consumer, b0, 0, 1, cr, cg, cb, ba);
                    vertex(pose, consumer, b1, 1, 1, cr, cg, cb, ba);
                    vertex(pose, consumer, a1, 1, 0, cr, cg, cb, ba);
                }
            }
        });
    }

    private static Vec3 ringPoint(Vec3 centre, double rad, double yLocal, double ang,
            double cosT, double sinT, double cosS, double sinS) {
        double lx = Math.cos(ang) * rad;
        double ly = yLocal;
        double lz = Math.sin(ang) * rad;
        // tilt around X
        double ty = ly * cosT - lz * sinT;
        double tz = ly * sinT + lz * cosT;
        double tx = lx;
        // spin around Y (horizontal orbit)
        double wx = tx * cosS + tz * sinS;
        double wy = ty;
        double wz = -tx * sinS + tz * cosS;
        return centre.add(wx, wy, wz);
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
