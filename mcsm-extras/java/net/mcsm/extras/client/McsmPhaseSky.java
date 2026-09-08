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
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.world.phys.Vec3;

/**
 * MCSM thick storm glare — body-glued soft volume (1.9.169).
 *
 * Ground truth from user stills:
 *   - Soft purple/teal/pink GLOW wrapped around the black mass (not a separate
 *     floating ball, not a wireframe sphere, not a dotted circle).
 *   - Player can go behind it — volume is world-space on the storm centre.
 *   - Long soft skirt fade (storm_glare.png radial), stacked depth slices so
 *     it reads thick in 3D without mesh grid lines.
 *   - Phase decks:
 *       4.x     icy light-blue shell
 *       5.0-5.2 teal / soft green-black
 *       5.2-5.45 purple
 *       5.5-5.9  dark purple-black core + soft pink/lavender rim
 *       6.0+     darker core, cooler blue-purple rim
 *
 * NEVER paints: face symbol, 3-head ring, debris cubes, orbiting dots, LON/LAT mesh.
 */
public final class McsmPhaseSky {

    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");
    private static final Identifier BLACK = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_black.png");

    /**
     * Depth slices along the view axis, in body-radius units.
     * Negative = behind storm (visible when player is behind / beside).
     * Positive = in front. Builds real volume without a polygon sphere.
     */
    private static final float[] DEPTH = {
            -1.55F, -1.10F, -0.70F, -0.35F, 0.00F, 0.30F, 0.65F, 1.05F, 1.45F
    };
    /** Radius scale per slice (outer slices larger = soft skirt). */
    private static final float[] SCALE = {
            2.85F, 2.45F, 2.10F, 1.75F, 1.45F, 1.70F, 2.05F, 2.40F, 2.80F
    };
    /** Alpha weight per slice (centre denser, outer skirt thin). */
    private static final float[] ALPHA = {
            0.07F, 0.11F, 0.16F, 0.22F, 0.28F, 0.20F, 0.14F, 0.09F, 0.05F
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
        if (phase < 4.0F || bodyR <= 0.0) {
            return Vec3.ZERO;
        }
        float amp = (float) (bodyR * (0.05 + 0.04 * Mth.clamp((phase - 4.0F) / 3.0F, 0.0F, 1.0F)));
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
        McsmExtrasConfig.load();
        double glareMul = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
        double smudge = Mth.clamp(McsmExtrasConfig.smudgeScale, 0.15, 2.5);

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();

        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            float phase = d.phase;
            // Glare starts when the storm has real mass (phase 4+)
            if (phase < 3.95F) {
                continue;
            }
            Vec3 stormPos = new Vec3(d.dispX, d.dispY, d.dispZ);
            double bodyR = bodyRadius(phase);
            Vec3 centre = stormPos.add(swayOffset(phase, nowSec, bodyR));

            Vec3 toStorm = centre.subtract(cam);
            double dist = toStorm.length();
            if (dist < 1.0E-3) {
                continue;
            }
            Vec3 view = toStorm.scale(1.0 / dist);
            float distFade = 1.0F - Mth.clamp((float) ((dist - 1800.0) / 1200.0), 0.0F, 1.0F);
            if (distFade <= 0.01F) {
                continue;
            }

            // Phase colour weights — match stills
            float wBlue = ramp(phase, 3.95F, 4.25F) * (1.0F - ramp(phase, 4.75F, 5.05F));
            float wTeal = ramp(phase, 4.90F, 5.10F) * (1.0F - ramp(phase, 5.28F, 5.42F));
            float wPurp = ramp(phase, 5.25F, 5.40F) * (1.0F - ramp(phase, 5.50F, 5.62F));
            float wPink = ramp(phase, 5.48F, 5.62F) * (1.0F - ramp(phase, 5.95F, 6.12F)); // 5.5-5.9
            float wSix  = ramp(phase, 5.95F, 6.20F);
            float presence = Mth.clamp(wBlue + wTeal + wPurp + wPink + wSix, 0.0F, 1.0F);
            if (presence <= 0.02F) {
                // still draw a faint dark shell so mass reads at phase 4 edge
                presence = ramp(phase, 3.95F, 4.15F) * 0.55F;
            }
            if (presence <= 0.01F) {
                continue;
            }

            // CORE / MID / RIM colours (linear 0-1), sampled toward MCSM stills
            float[] core = mix5(wBlue, wTeal, wPurp, wPink, wSix,
                    new float[]{0.04F, 0.08F, 0.16F},   // 4 icy dark blue
                    new float[]{0.02F, 0.07F, 0.07F},   // 5 teal black-green
                    new float[]{0.05F, 0.01F, 0.10F},   // 5.4 purple black
                    new float[]{0.04F, 0.01F, 0.08F},   // 5.5 dark purple-black
                    new float[]{0.02F, 0.02F, 0.06F});  // 6 near-black
            float[] mid = mix5(wBlue, wTeal, wPurp, wPink, wSix,
                    new float[]{0.22F, 0.42F, 0.85F},   // 4 light blue
                    new float[]{0.08F, 0.32F, 0.30F},   // 5 teal
                    new float[]{0.36F, 0.10F, 0.58F},   // 5.4 purple
                    new float[]{0.42F, 0.10F, 0.55F},   // 5.5 purple-pink mid
                    new float[]{0.14F, 0.08F, 0.28F});  // 6 cooler
            float[] rim = mix5(wBlue, wTeal, wPurp, wPink, wSix,
                    new float[]{0.45F, 0.65F, 1.00F},   // 4 icy rim
                    new float[]{0.16F, 0.48F, 0.44F},   // 5 teal rim
                    new float[]{0.55F, 0.22F, 0.75F},   // 5.4 purple rim
                    new float[]{0.78F, 0.38F, 0.72F},   // 5.5 pink-lavender rim (stills)
                    new float[]{0.30F, 0.20F, 0.52F});  // 6 blue-purple rim

            float breathe = 1.0F + 0.025F * Mth.sin(nowSec * 0.05F);
            double baseR = bodyR * (1.35 + 0.95 * glareMul) * smudge * breathe;
            // grow a bit through late phases so the mass fills the frame like stills
            if (phase > 5.3F) {
                baseR *= 1.0 + (phase - 5.3F) * 0.14;
            }
            float amp = presence * distFade;

            // ---- dark core wrap (silhouette mass, still #2 black body in purple) ----
            float coreA = amp * (0.35F + 0.45F * (wPink + wSix + wPurp * 0.6F));
            if (coreA > 0.02F) {
                // slightly smaller than mid glow so colour rim peeks around the body
                quad(poseStack, collector, GlowRenderTypes.glow(BLACK),
                        centre, view, baseR * 1.05,
                        (int) (core[0] * 255), (int) (core[1] * 255), (int) (core[2] * 255),
                        (int) (coreA * 90.0F));
                quad(poseStack, collector, GlowRenderTypes.glow(BLACK),
                        centre.add(view.scale(-bodyR * 0.15)), view, baseR * 0.82,
                        6, 4, 12, (int) (coreA * 110.0F));
            }

            // ---- thick soft colour volume (stacked billboards = 3D, no mesh) ----
            for (int s = 0; s < DEPTH.length; s++) {
                float t = s / (float) (DEPTH.length - 1);
                float[] col;
                if (t < 0.30F) {
                    col = lerp3(core, mid, t / 0.30F);
                } else if (t < 0.65F) {
                    col = lerp3(mid, rim, (t - 0.30F) / 0.35F);
                } else {
                    // outer skirt leans rim, slightly desaturated so it melts into sky
                    col = new float[]{
                            rim[0] * 0.85F,
                            rim[1] * 0.85F,
                            rim[2] * 0.90F
                    };
                }
                double radius = baseR * SCALE[s];
                float a = amp * ALPHA[s];
                // 5.5+ pink rim a touch stronger (stills show bright lavender edge)
                if (wPink > 0.25F && t > 0.55F) {
                    a *= 1.15F;
                    col = new float[]{
                            Mth.clamp(col[0] * 1.08F + 0.06F, 0.0F, 1.0F),
                            Mth.clamp(col[1] * 0.95F + 0.02F, 0.0F, 1.0F),
                            Mth.clamp(col[2] * 1.02F, 0.0F, 1.0F)
                    };
                }
                // phase 6 more restrained
                if (wSix > 0.3F) {
                    a *= 0.78F;
                    radius *= 0.94;
                }
                // micro breathe per slice so the mass feels alive, not a sticker
                radius *= 1.0 + 0.012 * Math.sin(nowSec * 0.31 + s * 0.4);

                Vec3 at = centre.add(view.scale(bodyR * DEPTH[s]));
                int ar = Mth.clamp((int) (col[0] * 255.0F), 0, 255);
                int ag = Mth.clamp((int) (col[1] * 255.0F), 0, 255);
                int ab = Mth.clamp((int) (col[2] * 255.0F), 0, 255);
                int aa = Mth.clamp((int) (a * 255.0F), 0, 255);
                if (aa <= 3) {
                    continue;
                }
                quad(poseStack, collector, GlowRenderTypes.glow(GLARE),
                        at, view, radius, ar, ag, ab, aa);
            }

            // ---- flank fill: two side discs so volume reads when viewed from the side ----
            // (stills show glow wrapping the flanks, not a flat card)
            if (amp > 0.08F && baseR > 6.0) {
                Vec3 upHint = Math.abs(view.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 right = view.cross(upHint).normalize();
                Vec3 up = right.cross(view).normalize();
                float sideA = amp * 0.12F * (0.7F + 0.3F * (wPink + wPurp + wTeal));
                int sr = Mth.clamp((int) (mid[0] * 255), 0, 255);
                int sg = Mth.clamp((int) (mid[1] * 255), 0, 255);
                int sb = Mth.clamp((int) (mid[2] * 255), 0, 255);
                int sa = Mth.clamp((int) (sideA * 255), 0, 255);
                if (sa > 4) {
                    quad(poseStack, collector, GlowRenderTypes.glow(GLARE),
                            centre.add(right.scale(bodyR * 0.55)), view,
                            baseR * 1.55, sr, sg, sb, sa);
                    quad(poseStack, collector, GlowRenderTypes.glow(GLARE),
                            centre.add(right.scale(-bodyR * 0.55)), view,
                            baseR * 1.55, sr, sg, sb, sa);
                    quad(poseStack, collector, GlowRenderTypes.glow(GLARE),
                            centre.add(up.scale(bodyR * 0.35)), view,
                            baseR * 1.35, sr, sg, sb, (int) (sa * 0.75F));
                }
            }
        }
    }

    private static float[] mix5(float wB, float wT, float wP, float wK, float wS,
            float[] blue, float[] teal, float[] purp, float[] pink, float[] six) {
        float tot = Math.max(wB + wT + wP + wK + wS, 1.0E-4F);
        return new float[] {
                (blue[0] * wB + teal[0] * wT + purp[0] * wP + pink[0] * wK + six[0] * wS) / tot,
                (blue[1] * wB + teal[1] * wT + purp[1] * wP + pink[1] * wK + six[1] * wS) / tot,
                (blue[2] * wB + teal[2] * wT + purp[2] * wP + pink[2] * wK + six[2] * wS) / tot
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

    private static void quad(PoseStack poseStack, SubmitNodeCollector collector,
            net.minecraft.client.renderer.rendertype.RenderType type,
            Vec3 at, Vec3 view, double radius, int r, int g, int b, int alpha) {
        if (alpha <= 2 || radius < 0.5) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            quadVerts(pose, consumer, at, view, radius, r, g, b, alpha);
        });
    }

    private static void quadVerts(Pose pose, VertexConsumer consumer, Vec3 at, Vec3 view,
            double radius, int r, int g, int b, int a) {
        Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = view.cross(upHint).normalize();
        Vec3 up = right.cross(view).normalize();
        // slight vertical squash so the mass reads like MCSM oval volume, not a perfect disc
        Vec3 rx = right.scale(radius * 1.12);
        Vec3 uy = up.scale(radius * 0.92);
        int fa = Math.min(Math.max(a, 0), 255);
        vertex(pose, consumer, at.subtract(rx).subtract(uy), 0.0F, 1.0F, r, g, b, fa);
        vertex(pose, consumer, at.add(rx).subtract(uy), 1.0F, 1.0F, r, g, b, fa);
        vertex(pose, consumer, at.add(rx).add(uy), 1.0F, 0.0F, r, g, b, fa);
        vertex(pose, consumer, at.subtract(rx).add(uy), 0.0F, 0.0F, r, g, b, fa);
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
