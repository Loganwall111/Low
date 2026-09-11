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
import net.minecraft.client.renderer.rendertype.RenderTypes;
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

    // mega-phase 5c: the reference frames exposed how the original game
    // builds the glare - a plain soft gradient quad BEHIND the silhouette,
    // plus flat emissive squares for the mouth details. The old hard ring
    // glare is gone.
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");
    // 1.9.201: the extracted multi-colour glare discs (rebuilt by
    // ci/make_glare_from_sky.py from the OG sky strips).  The flat one-colour
    // wash is replaced by these textured domes.
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
            return phase < 6.0F ? 18.0F + 22.0F * (phase - 5.0F)
                    : Math.min(320.0F, 55.0F + 42.0F * (phase - 6.0F));
        }
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            // 1.9.201: the extracted OG sky gradients render every frame
            // (calm decks + storm decks) before the storm glare volume.
            McsmSkyDome.submit(ctx);
            // 1.9.208: structured glare (no sphere) + real orbiting cube rings
            submitStructuredGlare(ctx);
            McsmStormRings.submit(ctx);
        } catch (Throwable ignored) {
            // an unexpected base-jar surface degrades to no blob, never a crash
        }
    }

    /**
     * 1.9.208 -- GLARE COMPLETE REVAMP.
     *
     * The old construction (gaussian dome patches + soft circular billboards)
     * read as a fuzzy sphere sitting in the world, so it is GONE.  The new
     * glare is rigid and structured, built from the same 16-stop phase decks
     * the sky uses:
     *
     *   1. a hard-edged three-band gradient slab locked behind the creature
     *      (horizon / mid / zenith colours, crisp top and bottom cutoffs);
     *   2. sharp angular rays radiating from the storm core, alternating
     *      long/short, slowly rotating -- the "glare" itself;
     *   3. a small saturated core plus a faint purple fog glow cast onto the
     *      landscape beneath the tractor beams.
     *
     * Everything is additive glow geometry, so it renders identically with
     * and without the MCSM Visual Shader.  At phase 8-9 the palette swaps to
     * the ember deck and the halo layers disappear (only the glare remains).
     */
    private static void submitStructuredGlare(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientDistantStormManager.all().isEmpty()) return;
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        ClientDistantStormManager.StormData best = null;
        double bestD = Double.MAX_VALUE;
        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            if (d.phase < 3.9F) continue;
            double dx = d.dispX - cam.x, dy = d.dispY - cam.y, dz = d.dispZ - cam.z;
            double dd = dx * dx + dy * dy + dz * dz;
            if (dd < bestD) { bestD = dd; best = d; }
        }
        if (best == null) return;
        float phase = best.phase;
        double dist = Math.sqrt(bestD);
        if (dist < 1.0D || dist > 2800.0D) return;
        float gt = (float)(mc.level.getGameTime() % 240000L)
                + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float nowSec = gt * 0.05F;
        Vec3 centre = new Vec3(best.dispX, best.dispY, best.dispZ)
                .add(sway(phase, nowSec, bodyRadius(phase)));
        Vec3 view = centre.subtract(cam).normalize();
        if (view.lengthSqr() < 1.0E-4D) return;
        float amp = ramp(phase, 3.95F, 4.25F)
                * (1.0F - Mth.clamp((float)((dist - 1500.0D) / 1200.0D), 0.0F, 1.0F));
        if (amp <= 0.01F) return;
        final float aa = Math.min(1.0F, amp * 1.45F);

        // phase deck -> structured glare palette
        float[][] deck = McsmGlarePalettes.P4;
        if (phase >= 8.0F)      deck = McsmGlarePalettes.P89;
        else if (phase >= 5.9F) deck = McsmGlarePalettes.P6;
        else if (phase >= 5.48F) deck = McsmGlarePalettes.P55;
        else if (phase >= 5.25F) deck = McsmGlarePalettes.P5_PURPLE;
        else if (phase >= 4.9F) deck = McsmGlarePalettes.P5_TEAL;
        final float[] hor = { deck[15][0], deck[15][1], deck[15][2] };
        final float[] mid = { deck[8][0],  deck[8][1],  deck[8][2]  };
        final float[] top = { deck[1][0],  deck[1][1],  deck[1][2]  };
        // glare core: brighten the mid/horizon stop for the rays
        final float[] core = {
                Math.min(1.0F, hor[0] * 1.6F + 0.10F),
                Math.min(1.0F, hor[1] * 1.6F + 0.10F),
                Math.min(1.0F, hor[2] * 1.4F + 0.15F) };

        McsmExtrasConfig.load();
        final double gs = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
        final double wHalf = Math.min(70.0D, 30.0D + 22.0D * gs);   // slab half-width (deg)
        final double slabTop = Math.min(68.0D, 30.0D + 24.0D * gs); // slab top elevation
        final double slabBot = Math.min(34.0D, 14.0D + 12.0D * gs); // slab bottom depth

        final Vec3 dir = view;
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        PoseStack poseStack = ctx.poseStack();

        // ---- 1) structured three-band slab: hard edges, no falloff --------
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                (pose, consumer) -> {
                    angQuad(pose, consumer, cam, dir, 512.0D, -wHalf, wHalf, -slabBot, slabTop,
                            hor[0], hor[1], hor[2], aa * 78.0F);
                    angQuad(pose, consumer, cam, dir, 509.0D, -wHalf * 0.94D, wHalf * 0.94D,
                            slabTop * 0.18D, slabTop, top[0], top[1], top[2], aa * 88.0F);
                    angQuad(pose, consumer, cam, dir, 506.0D, -wHalf * 0.86D, wHalf * 0.86D,
                            -slabBot, slabTop * 0.30D, mid[0], mid[1], mid[2], aa * 96.0F);
                });

        // ---- 2) rigid rays: alternating long/short spokes, rotating --------
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                (pose, consumer) -> {
                    final int N = 15;
                    final double rot = nowSec * 0.016D; // slow, dignified
                    for (int i = 0; i < N; i++) {
                        double th = (2.0D * Math.PI * i / N) + rot;
                        boolean longRay = (i % 2) == 0;
                        double rIn = longRay ? slabTop * 0.30D : slabTop * 0.22D;
                        double rOut = longRay ? slabTop * 1.30D : slabTop * 0.95D;
                        double thk = longRay ? 2.4D : 1.6D;
                        // ray as a thin angular quad along (cos,sin)
                        double c0x = Math.cos(th) * rIn,  c0y = Math.sin(th) * rIn * 0.92D;
                        double c1x = Math.cos(th) * rOut, c1y = Math.sin(th) * rOut * 0.92D;
                        double px = -Math.sin(th) * thk,   py = Math.cos(th) * thk;
                        angQuadRaw(pose, consumer, cam, dir, 504.0D,
                                c0x + px, c0y + py, c0x - px, c0y - py,
                                c1x - px, c1y - py, c1x + px, c1y + py,
                                core[0], core[1], core[2], aa * 62.0F * (longRay ? 1.0F : 0.72F));
                    }
                });

        // ---- 3) saturated core + foggy glow on the landscape below ---------
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                (pose, consumer) -> {
                    double coreHalf = Math.min(16.0D, 5.0D + 6.0D * gs);
                    angQuad(pose, consumer, cam, dir, 502.0D, -coreHalf, coreHalf,
                            -coreHalf * 0.9D, coreHalf * 0.9D,
                            core[0], core[1], core[2], aa * 150.0F);
                });
        // ground fog pool under the beams (a horizontal quad at the storm's
        // feet; reads as geometric fog from the beam cones)
        double groundY = best.dispY - bodyRadius(phase) * 1.15D;
        Vec3 gAt = new Vec3(best.dispX, Math.max(groundY, best.dispY - 260.0D), best.dispZ);
        double gr = bodyRadius(phase) * 2.4D;
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                (pose, consumer) -> {
                    quadVerts(pose, consumer, gAt, new Vec3(0.0D, 1.0D, 0.0D), gr,
                            155, 90, 235, (int)(aa * 46.0F));
                });
    }

    private static Vec3 sway(float phase, float timeSec, double bodyR) {
        if (phase < 4.0F || bodyR <= 0.0D) return Vec3.ZERO;
        float amp = (float)(bodyR * (0.025D + 0.020D * Mth.clamp((phase - 4.0F) / 3.0F, 0.0F, 1.0F)));
        return new Vec3(Mth.sin(timeSec * 0.20F) * amp,
                Mth.sin(timeSec * 0.11F) * amp * 0.16F,
                Mth.sin(timeSec * 0.16F + 1.3F) * amp * 0.45F);
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
            double baseR = skyDist * angular * 1.08;
            if (phase > 5.5F) {
                baseR *= 1.0F + (phase - 5.5F) * 0.12F;
            }
            float breathe = 1.0F + 0.03F * Mth.sin(nowSec * 0.045F);
            baseR *= breathe;
            float a = master * distFade;

            float wBlue = ramp(phase, 3.95F, 4.2F) * (1.0F - ramp(phase, 4.6F, 5.0F));
            float wTurq = ramp(phase, 4.45F, 4.9F) * (1.0F - ramp(phase, 5.2F, 5.5F));
            float wViolet = ramp(phase, 5.2F, 5.5F) * (1.0F - ramp(phase, 6.0F, 6.35F));
            float wPurp = ramp(phase, 6.0F, 6.35F);
            float wPink = ramp(phase, 6.3F, 7.0F);
            float wCore = ramp(phase, 4.0F, 4.3F) * 0.42F;
            // 1.9.197: no fake storm-face or fake three-mouth overlay in the
            // sky glare. Those cards looked like duplicated heads stamped on a
            // giant texture. Teeth now come from the real storm model/tint path;
            // this pass is only a soft atmospheric halo.
            float wFace = 0.0F;
            // 1.9.208: phase 8-9 the halo disappears entirely; only the
            // structured glare and the gigantic rings remain.
            float wGlare = ramp(phase, 3.95F, 4.3F) * (1.0F - ramp(phase, 7.5F, 8.0F));
            float wMouth = 0.0F;
            float mouthBoost = phase >= 7.0F ? 1.85F : (phase >= 6.0F ? 1.70F : (phase >= 5.5F ? 1.45F : 0.82F));
            float mouthAlphaScale = phase >= 7.0F ? 1.18F : (phase >= 6.0F ? 1.12F : (phase >= 5.5F ? 1.0F : (phase >= 5.0F ? 0.36F : 0.48F)));
            int mouthR = phase >= 7.0F ? 132 : (phase >= 6.0F ? 88 : (phase >= 5.5F ? 245 : 225));
            int mouthG = phase >= 7.0F ? 255 : (phase >= 6.0F ? 210 : 255);
            int mouthB = phase >= 7.0F ? 224 : (phase >= 6.0F ? 255 : 245);

            // 1.9.208: the soft circular billboard glare and every blurry
            // backdrop wash are deleted (they read as fuzzy mist spheres).
            // submitStructuredGlare draws the rigid slab + rays instead.
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

    /** Crisp angular quad: corners as angular offsets (degrees) from the
     *  view axis; constant alpha -- no gaussian, no soft rim. */
    private static void angQuad(Pose pose, VertexConsumer consumer, Vec3 cam, Vec3 dir,
            double shell, double x0, double x1, double y0, double y1,
            float r, float g, float b, float a) {
        angQuadRaw(pose, consumer, cam, dir, shell, x0, y0, x1, y0, x1, y1, x0, y1, r, g, b, a);
    }

    private static void angQuadRaw(Pose pose, VertexConsumer consumer, Vec3 cam, Vec3 dir,
            double shell, double ax0, double ay0, double ax1, double ay1,
            double ax2, double ay2, double ax3, double ay3,
            float r, float g, float b, float a) {
        if (a <= 2) return;
        Vec3 upHint = Math.abs(dir.y) > 0.96D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = dir.cross(upHint).normalize();
        Vec3 up = right.cross(dir).normalize();
        double d0x = Math.tan(Math.toRadians(ax0)), d0y = Math.tan(Math.toRadians(ay0));
        double d1x = Math.tan(Math.toRadians(ax1)), d1y = Math.tan(Math.toRadians(ay1));
        double d2x = Math.tan(Math.toRadians(ax2)), d2y = Math.tan(Math.toRadians(ay2));
        double d3x = Math.tan(Math.toRadians(ax3)), d3y = Math.tan(Math.toRadians(ay3));
        Vec3 p0 = cam.add(dir.add(right.scale(d0x)).add(up.scale(d0y)).normalize().scale(shell));
        Vec3 p1 = cam.add(dir.add(right.scale(d1x)).add(up.scale(d1y)).normalize().scale(shell));
        Vec3 p2 = cam.add(dir.add(right.scale(d2x)).add(up.scale(d2y)).normalize().scale(shell));
        Vec3 p3 = cam.add(dir.add(right.scale(d3x)).add(up.scale(d3y)).normalize().scale(shell));
        int ir = Mth.clamp((int)(r * 255.0F), 0, 255);
        int ig = Mth.clamp((int)(g * 255.0F), 0, 255);
        int ib = Mth.clamp((int)(b * 255.0F), 0, 255);
        int ia = Mth.clamp((int)a, 0, 255);
        vertex(pose, consumer, p0, 0.0F, 1.0F, ir, ig, ib, ia);
        vertex(pose, consumer, p1, 1.0F, 1.0F, ir, ig, ib, ia);
        vertex(pose, consumer, p2, 1.0F, 0.0F, ir, ig, ib, ia);
        vertex(pose, consumer, p3, 0.0F, 0.0F, ir, ig, ib, ia);
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
