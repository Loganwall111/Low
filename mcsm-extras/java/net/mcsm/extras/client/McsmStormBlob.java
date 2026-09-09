package net.mcsm.extras.client;

import java.util.HashMap;
import java.util.Map;

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
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.world.phys.Vec3;

/**
 * Devouring Storms: mega-phase 2+5 - the storm blob, corrected and welded.
 *
 *  - phases 5.5-5.9 get the PINKISH-VIOLET blob; the reddish cast is gone;
 *  - the dark storm heart sits dead-centre in every phase from 4 up;
 *  - the centre direction is temporally smoothed (25% per frame) so blob
 *    and storm glide as one sky element;
 *  - the GLARE is the original game's construction, exposed by the reference
 *    frames: one soft gradient billboard hung BEHIND the silhouette (wide
 *    purple aura 5.5+, blue at 4-5), terrain occluding it for free - the old
 *    hard ring glare is deleted;
 *  - the MOUTHS are flat emissive squares over the body: cyan-white inner
 *    mouth, a zigzagged U-arc of tiny white dashed teeth, one magenta cube
 *    per emitter - exactly what the close-up frames show;
 *  - new for 5.5+: a PURPLE OVERLAY over the storm's face - an additive
 *    fringe hugging the silhouette plus a faint violet wash across the
 *    whole face, like a second silhouette layered on the creature.
 *
 * Every call is copied verbatim from the base mod's own compiled
 * StormBackdrop (verified 26.2 surface).
 */
public final class McsmStormBlob {

    private static final Identifier BLUE4 = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_phase4_blue.png");
    private static final Identifier BLACK = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_black.png");
    private static final Identifier TURQUOISE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_turquoise.png");
    private static final Identifier PURPLE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_purple.png");
    private static final Identifier PURPLE_PINK = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_purple_pink.png");
    private static final Identifier EMBER = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/backdrop_ember.png");
    private static final Identifier STORM_FACE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_face.png");
    // mega-phase 5c: the reference frames exposed how the original game
    // builds the glare - a plain soft gradient quad BEHIND the silhouette,
    // plus flat emissive squares for the mouth details. The old hard ring
    // glare is gone.
    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    /** The three beam mouths, in billboard units of baseR (x right, y up). */
    private static final float[] MOUTH_X = { -0.30F, 0.00F, 0.30F };
    private static final float[] MOUTH_Y = { -0.04F, -0.14F, -0.02F };

    private static final Map<Integer, Vec3> SMOOTH = new HashMap<>();

    private McsmStormBlob() {
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
            return 4.0F + 1.5F * phase;
        } else if (phase < 5.0F) {
            return 10.0F + 8.0F * (phase - 4.0F);
        } else {
            return phase < 6.0F ? 18.0F + 22.0F * (phase - 5.0F) : 40.0F + 30.0F * (phase - 6.0F);
        }
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            submitInner(ctx);
        } catch (Throwable ignored) {
            // an unexpected base-jar surface degrades to no blob, never a crash
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
        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        float master = 1.0F; // config-free: the corrected blob always runs

        // pass 1: the NEAREST storm is the main one - halo and face overlay
        // attach to it and nothing else
        int mainKey = -1;
        double mainDist = Double.MAX_VALUE;
        int probe = 0;
        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            int key = probe++;
            if (d.phase < 3.9F) {
                continue;
            }
            Vec3 c = new Vec3(d.dispX, d.dispY, d.dispZ);
            double dd = c.subtract(cam).length();
            if (dd < mainDist) {
                mainDist = dd;
                mainKey = key;
            }
        }

        int idx = 0;
        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            int key = idx++;
            float phase = d.phase;
            if (phase < 3.9F) {
                continue;
            }
            Vec3 centre = new Vec3(d.dispX, d.dispY, d.dispZ);
            Vec3 prev = SMOOTH.get(key);
            if (prev != null) {
                centre = prev.add(centre.subtract(prev).scale(0.25D));
            }
            SMOOTH.put(key, centre);
            Vec3 toStorm = centre.subtract(cam);
            double dist = toStorm.length();
            if (dist < 1.0E-4) {
                continue;
            }
            Vec3 view = toStorm.scale(1.0D / dist);
            float distFade = 1.0F - Mth.clamp((float) ((dist - 1200.0) / 900.0), 0.0F, 1.0F);
            if (distFade <= 0.004F) {
                continue;
            }
            double skyDist = 220.0D;
            Vec3 at = cam.add(view.scale(skyDist));
            double angular = Mth.clamp(bodyRadius(phase) / Math.max(dist, 1.0), 0.012, 0.85);
            double baseR = skyDist * angular * 1.5;
            if (phase > 5.5F) {
                baseR *= 1.0F + (phase - 5.5F) * 0.26F;
            }
            float breathe = 1.0F + 0.03F * Mth.sin(nowSec * 0.045F);
            baseR *= breathe;
            float a = master * distFade;

            float wBlue = ramp(phase, 3.95F, 4.2F) * (1.0F - ramp(phase, 4.6F, 5.0F));
            float wTurq = ramp(phase, 4.45F, 4.9F) * (1.0F - ramp(phase, 5.2F, 5.5F));
            float wViolet = ramp(phase, 5.2F, 5.5F) * (1.0F - ramp(phase, 6.0F, 6.35F));
            float wPurp = ramp(phase, 6.0F, 6.35F);
            float wPink = ramp(phase, 6.3F, 7.0F);
            float wCore = ramp(phase, 4.0F, 4.3F);
            float wFace = ramp(phase, 5.5F, 5.8F);
            float wGlare = ramp(phase, 3.95F, 4.3F);
            float wMouth = phase < 4.0F ? 0.0F : ramp(phase, 4.0F, 4.25F);
            float mouthBoost = phase >= 7.0F ? 1.85F : (phase >= 6.0F ? 1.70F : (phase >= 5.5F ? 1.45F : 0.82F));
            float mouthAlphaScale = phase >= 7.0F ? 1.18F : (phase >= 6.0F ? 1.12F : (phase >= 5.5F ? 1.0F : (phase >= 5.0F ? 0.36F : 0.48F)));
            int mouthR = phase >= 7.0F ? 132 : (phase >= 6.0F ? 88 : (phase >= 5.5F ? 245 : 225));
            int mouthG = phase >= 7.0F ? 255 : (phase >= 6.0F ? 210 : 255);
            int mouthB = phase >= 7.0F ? 224 : (phase >= 6.0F ? 255 : 245);

            // THE GLARE, FIRST: one soft gradient billboard hung behind the
            // silhouette, exactly as the original frames expose it - wide
            // purple aura at 5.5+, blue at phase 4-5, teal in the green
            // phase. Terrain draws later, so trees and buildings occlude it
            // for free. Scale rides the Glare Size slider (default 0.58).
            if (key == mainKey && wGlare > 0.004F) {
                McsmExtrasConfig.load();
                double gs = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
                float gr = (float) (baseR * (1.7D + 2.2D * gs));
                float wr = 0.30F * wBlue + 0.35F * wTurq + 0.48F * wViolet
                        + 0.55F * wPurp + 0.72F * wPink;
                float wg = 0.45F * wBlue + 0.85F * wTurq + 0.28F * wViolet
                        + 0.22F * wPurp + 0.32F * wPink;
                float wb = 0.92F * wBlue + 0.85F * wTurq + 0.80F * wViolet
                        + 0.78F * wPurp + 0.62F * wPink;
                float wsum = wBlue + wTurq + wViolet + wPurp + wPink;
                if (wsum > 0.004F) {
                    wr /= wsum; wg /= wsum; wb /= wsum;
                } else {
                    wr = 0.48F; wg = 0.28F; wb = 0.80F;
                }
                quad(poseStack, collector, GlowRenderTypes.glow(GLARE), at, view,
                        gr, (int) (wr * 255.0F), (int) (wg * 255.0F), (int) (wb * 255.0F),
                        (int) (a * wGlare * 110.0F));
            }
            if (wPink > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(PURPLE_PINK), at, view,
                        baseR * 1.55, 255, 205, 225, (int) (a * wPink * 245.0F));
            }
            if (wPurp > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(PURPLE), at, view,
                        baseR * 1.18, 236, 200, 255, (int) (a * wPurp * 250.0F));
            }
            if (wViolet > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(PURPLE_PINK), at, view,
                        baseR * 1.35, 255, 214, 236, (int) (a * wViolet * 240.0F));
            }
            if (wTurq > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(TURQUOISE), at, view,
                        baseR * 1.1, 255, 255, 255, (int) (a * wTurq * 250.0F));
            }
            if (phase >= 6.5F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(EMBER), at, view,
                        baseR * 1.34, 255, 255, 255, (int) (a * 60.0F));
            }
            if (wCore > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK), at, view,
                        baseR * 0.85, 255, 255, 255, (int) (a * wCore * 235.0F));
            }
            // the purple overlay: additive fringe on the silhouette plus a
            // faint violet wash across the whole face, 5.5 and up
            if (key == mainKey && wFace > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.glow(STORM_FACE), at, view,
                        baseR * 1.06, 255, 255, 255, (int) (a * wFace * 140.0F));
                quad(poseStack, collector, GlowRenderTypes.translucent(STORM_FACE), at, view,
                        baseR * 0.92, 255, 255, 255, (int) (a * wFace * 55.0F));
            }
            if (wBlue > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.glow(BLUE4), at, view,
                        baseR * 0.95, 190, 215, 255, (int) (a * wBlue * 235.0F));
            }
            // PHASE-6 PARTICLE FIELD (batched into two draws): black cubes
            // peeling off the body edge, sparkle dots travelling down inside
            // the beam cones, faint motes orbiting the whole storm and mist
            // puffs clinging to its base - exactly the four particle reads
            // the reference frames show. Stateless: every position is a hash
            // of its index plus time, so nothing is stored or synced.
            if (key == mainKey && wGlare > 0.004F && baseR > 10.0) {
                final float bR = (float) baseR;
                final float tt = nowSec;
                final float aa = a;
                final float wg = wGlare;
                collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                        (pose, consumer) -> {
                    // black cubes: cycle outward off the silhouette edge
                    for (int i = 0; i < 26; i++) {
                        float sd = i * 0.618034F;
                        float cyc = fract(tt * 0.05F + sd);
                        float ang = fract(sd) * 6.28318F;
                        float rr = (0.75F + 0.65F * cyc) * bR;
                        float x = (float) Math.cos(ang) * rr * 0.95F;
                        float y = (float) Math.sin(ang) * rr * 0.70F - cyc * 0.35F * bR;
                        Vec3 pq = billboardOffset(at, view, x, y);
                        float sz = bR * (0.020F + 0.020F * fract(sd * 7.3F));
                        quadVerts(pose, consumer, pq, view, sz, 8, 6, 12,
                                (int) (aa * wg * 210.0F * (1.0F - cyc * 0.7F)));
                    }
                    // mist puffs at the storm's base
                    for (int i = 0; i < 6; i++) {
                        float sd = i * 0.31F + 0.17F;
                        float x = (fract(sd * 3.7F) * 2.4F - 1.2F) * bR;
                        float y = -1.05F * bR + fract(sd * 9.1F) * 0.3F * bR;
                        Vec3 pq = billboardOffset(at, view, x, y);
                        quadVerts(pose, consumer, pq, view,
                                bR * (0.35F + 0.2F * fract(sd * 5.3F)), 150, 130, 170,
                                (int) (aa * wg * 26.0F));
                    }
                });
                collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                        (pose, consumer) -> {
                    // sparkle dots riding down inside each beam cone
                    for (int m = 0; m < 3; m++) {
                        for (int j = 0; j < 9; j++) {
                            float sd = m * 0.37F + j * 0.111F;
                            float tp = fract(tt * 0.22F + sd);
                            float gx = MOUTH_X[m] * 2.6F;
                            float gy = -1.5F;
                            float x = MOUTH_X[m] + (gx - MOUTH_X[m]) * tp
                                    + (fract(sd * 13.7F) - 0.5F) * 0.5F * tp;
                            float y = MOUTH_Y[m] + (gy - MOUTH_Y[m]) * tp
                                    + (fract(sd * 17.3F) - 0.5F) * 0.35F * tp;
                            Vec3 pq = billboardOffset(at, view, bR * x, bR * y);
                            quadVerts(pose, consumer, pq, view, bR * 0.012F, 235, 225, 255,
                                    (int) (aa * wg * 190.0F * (1.0F - tp)));
                        }
                    }
                    // faint motes orbiting the whole storm - the "subtle
                    // particles everywhere" read
                    for (int i = 0; i < 30; i++) {
                        float sd = i * 0.4717F;
                        float ang = fract(sd) * 6.28318F + tt * 0.04F;
                        float rr = (0.35F + 1.25F * fract(sd * 5.1F)) * bR;
                        Vec3 pq = billboardOffset(at, view,
                                (float) Math.cos(ang) * rr, (float) Math.sin(ang) * rr * 0.8F);
                        boolean purple = fract(sd * 3.3F) > 0.5F;
                        quadVerts(pose, consumer, pq, view, bR * 0.010F,
                                purple ? 200 : 240, purple ? 160 : 240, purple ? 255 : 250,
                                (int) (aa * wg * 70.0F * (0.4F + 0.6F * fract(sd * 11.0F))));
                    }
                });
            }

            // Optional experimental infinite rear/back growth: OFF by default.
            // This is a light visual-only first pass so it cannot engulf saves
            // or destroy worlds until the player explicitly enables it.
            if (key == mainKey && phase >= 4.0F) {
                McsmExtrasConfig.load();
                if (McsmExtrasConfig.infiniteBackGrowth) {
                    float speed = (float)Mth.clamp(McsmExtrasConfig.infiniteBackGrowthSpeed, 0.01, 12.0);
                    float grow = Mth.clamp((nowSec * speed) / 900.0F, 0.0F, 1.0F);
                    final float bR2 = (float)baseR;
                    collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE), (pose, consumer) -> {
                        for (int i = 0; i < 44; i++) {
                            float q = i / 43.0F;
                            float width = (0.08F + q * 0.34F + grow * 0.22F) * bR2;
                            float yy = (0.35F + q * (2.8F + grow * 9.0F)) * bR2;
                            float xx = (fract(i * 0.618F) - 0.5F) * width;
                            Vec3 bp = billboardOffset(at, view, xx, yy);
                            float sz = (0.032F + 0.038F * fract(i * 0.371F)) * bR2;
                            quadVerts(pose, consumer, bp, view, sz, 7, 8, 18, (int)(a * (1.0F - q * 0.35F) * 185.0F));
                        }
                    });
                }
            }

            // MOUTH DETAILS, LAST (over the body): the original frames show
            // each emitter as a cyan-white inner-mouth square, a U-arc of
            // tiny white dashed teeth (zigzagged), and one small magenta
            // cube floating above. Flat emissive squares - their softness
            // comes from distance alone.
            if (key == mainKey && wMouth > 0.004F && baseR > 12.0) {
                for (int m = 0; m < 3; m++) {
                    Vec3 mo = billboardOffset(at, view, baseR * MOUTH_X[m], baseR * MOUTH_Y[m]);
                    // inner mouth: cyan-white emissive square
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), mo, view,
                            baseR * 0.135 * mouthBoost, mouthR, mouthG, mouthB, (int) (a * wMouth * 210.0F * mouthAlphaScale));
                    // dashed teeth: 7 tiny squares on a downward U-arc
                    for (int i = 0; i < 7; i++) {
                        float ang = (float) (Math.PI * (1.12 + 0.76 * i / 6.0));
                        float tx = MOUTH_X[m] + (float) Math.cos(ang) * 0.115F;
                        float ty = MOUTH_Y[m] + (float) Math.sin(ang) * 0.10F
                                + ((i & 1) == 1 ? 0.014F : 0.0F);
                        Vec3 tp = billboardOffset(at, view, baseR * tx, baseR * ty);
                        quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), tp, view,
                                baseR * 0.036 * mouthBoost, mouthR, mouthG, mouthB, (int) (a * wMouth * 255.0F * mouthAlphaScale));
                    }
                    // the magenta emitter cube above the mouth
                    Vec3 cp = billboardOffset(at, view, baseR * MOUTH_X[m],
                            baseR * (MOUTH_Y[m] + 0.17F));
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), cp, view,
                            baseR * 0.052 * mouthBoost, 232, 40, 255, (int) (a * wMouth * 255.0F));
                }
            }
        }
    }

    private static float fract(float x) {
        return x - (float) Math.floor(x);
    }

    private static void quadVerts(Pose pose, VertexConsumer consumer, Vec3 at, Vec3 view,
            double radius, int r, int g, int b, int a) {
        Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = view.cross(upHint).normalize();
        Vec3 up = right.cross(view).normalize();
        Vec3 rx = right.scale(radius * 1.15);
        Vec3 uy = up.scale(radius);
        int fa = Math.min(Math.max(a, 0), 255);
        vertex(pose, consumer, at.subtract(rx).subtract(uy), 0.0F, 1.0F, r, g, b, fa);
        vertex(pose, consumer, at.add(rx).subtract(uy), 1.0F, 1.0F, r, g, b, fa);
        vertex(pose, consumer, at.add(rx).add(uy), 1.0F, 0.0F, r, g, b, fa);
        vertex(pose, consumer, at.subtract(rx).add(uy), 0.0F, 0.0F, r, g, b, fa);
    }

    private static Vec3 billboardOffset(Vec3 at, Vec3 view, double x, double y) {
        Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = view.cross(upHint).normalize();
        Vec3 up = right.cross(view).normalize();
        return at.add(right.scale(x * 1.15)).add(up.scale(y));
    }

    private static void quadAt(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
            Vec3 at, Vec3 view, double radius, int r, int g, int b, int alpha) {
        quad(poseStack, collector, type, at, view, radius, r, g, b, alpha);
    }

    private static void quad(PoseStack poseStack, SubmitNodeCollector collector, RenderType type,
            Vec3 at, Vec3 view, double radius, int r, int g, int b, int alpha) {
        if (alpha <= 2) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            quadVerts(pose, consumer, at, view, radius, r, g, b, alpha);
        });
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
