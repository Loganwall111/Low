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
 * Devouring Storms mega-phase 7b — welded thick-shell storm aura.
 *
 * The glare is a thick 3D layer glued to the storm body (not a far 2D
 * skybox billboard). Multiple depth slices sit in front of, through, and
 * behind the silhouette so the player can walk behind the storm and still
 * see the shell, and the back always tracks the storm as it moves.
 *
 * Phase colour decks (sampled from the Story Mode frames):
 *   4.x   moon-blue
 *   5.0   soft violet wash begins
 *   5.4   big purple glow ramps hard
 *   5.5–5.9 violet zenith / magenta mid / salmon-pink rim (body is purple)
 *   6.x   deep purple + ember
 *
 * Silhouette / nightglow stack (5.5+): dark-blue base + dark-purple wrap
 * over the top + moon-blue fringe + black atmospheric top that eats the
 * roof of the sky around the storm (not the body itself).
 *
 * Teeth are body detail on the mouth emitters (chunky white zigzag U-arcs),
 * never part of the glare shell. The far three-headed HALO ring from
 * StormPresenceFX is killed by McsmPresenceFxPatch.
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
    private static final Identifier GLARE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_glare.png");
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    /** Three beam mouths, in body-radius units (x right, y up). */
    private static final float[] MOUTH_X = { -0.28F, 0.00F, 0.28F };
    private static final float[] MOUTH_Y = { -0.02F, -0.12F, 0.00F };

    /**
     * Depth slices of the thick shell, in body-radius units along the view
     * axis. Negative = behind the storm (visible when the player is behind
     * or off to the side), positive = in front. The zero slice is the
     * welded face plate.
     */
    private static final float[] SHELL_DEPTH = {
            -1.35F, -0.85F, -0.40F, 0.00F, 0.35F, 0.75F, 1.15F
    };
    private static final float[] SHELL_SCALE = {
            1.55F, 1.38F, 1.22F, 1.08F, 1.18F, 1.32F, 1.48F
    };
    private static final float[] SHELL_ALPHA = {
            0.55F, 0.70F, 0.85F, 1.00F, 0.80F, 0.60F, 0.40F
    };

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
            // unexpected base-jar surface degrades to no blob, never a crash
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
        McsmExtrasConfig.load();
        double glareMul = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
        double smudge = Mth.clamp(McsmExtrasConfig.smudgeScale, 0.15, 2.5);

        // nearest storm owns the face overlay + mouth detail
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
            float distFade = 1.0F - Mth.clamp((float) ((dist - 1600.0) / 1100.0), 0.0F, 1.0F);
            if (distFade <= 0.004F) {
                continue;
            }

            double bodyR = bodyRadius(phase);
            // world-space shell radius — welded to the body, not a far sky disc
            double shellR = bodyR * (1.55 + 1.10 * glareMul) * smudge;
            if (phase > 5.4F) {
                shellR *= 1.0F + (phase - 5.4F) * 0.18F;
            }
            float breathe = 1.0F + 0.028F * Mth.sin(nowSec * 0.045F);
            shellR *= breathe;

            // far storms still need a skybox-scale disc so the silhouette
            // reads at range; blend world-space radius up toward an angular
            // sky radius past ~350 blocks without ever detaching the shell
            double skyDist = Math.min(Math.max(dist * 0.92, bodyR * 2.5), 280.0);
            double angular = Mth.clamp(bodyR / Math.max(dist, 1.0), 0.010, 0.90);
            double skyR = skyDist * angular * (1.65 + 1.4 * glareMul) * smudge;
            float nearW = 1.0F - Mth.clamp((float) ((dist - 180.0) / 420.0), 0.0F, 1.0F);
            double baseR = shellR * nearW + skyR * (1.0F - nearW);
            // when far, the shell still sits ON the storm ray (at the storm
            // itself when near, sliding toward skyDist only as distance grows)
            double placeDist = dist * nearW + skyDist * (1.0F - nearW);
            // never push past the real storm centre when we are in front of it
            placeDist = Math.min(placeDist, dist * 0.98);
            Vec3 at = cam.add(view.scale(Math.max(placeDist, bodyR * 1.2)));

            float a = distFade;

            // phase colour weights — match the frame decks
            float wBlue = ramp(phase, 3.95F, 4.2F) * (1.0F - ramp(phase, 4.7F, 5.15F));
            float wTurq = ramp(phase, 4.5F, 4.95F) * (1.0F - ramp(phase, 5.25F, 5.45F));
            // 5.0 soft violet → 5.4 hard purple → 5.5–5.9 pinkish-violet peak
            float wViolet = ramp(phase, 5.0F, 5.35F) * (1.0F - ramp(phase, 5.95F, 6.25F));
            float wHard = ramp(phase, 5.35F, 5.55F) * (1.0F - ramp(phase, 5.95F, 6.25F)); // 5.4–5.9 punch
            float wPurp = ramp(phase, 5.95F, 6.3F);
            float wPink = ramp(phase, 6.25F, 6.9F); // post-cataclysm light pink
            float wCore = ramp(phase, 4.0F, 4.3F);
            // body fringe 5.5+ (plain palette, no face — the wide halo +
            // silhouette stack live sky-glued in McsmPhaseSky)
            float wFace = ramp(phase, 5.45F, 5.75F);
            float wShell = ramp(phase, 3.95F, 4.25F);
            float wMouth = ramp(phase, 3.9F, 4.3F);

            // ---------- THICK 3D SHELL (glued, multi-depth) ----------------
            // Each slice is a soft gradient plate offset along the view axis
            // so the aura has real thickness. Behind-slices (negative depth)
            // stay lit when the camera is behind or beside the storm.
            if (wShell > 0.004F) {
                // phase-tinted shell colour (exact frame decks)
                float wr = 0.28F * wBlue + 0.30F * wTurq + 0.55F * wViolet
                        + 0.62F * wHard + 0.50F * wPurp + 0.78F * wPink;
                float wg = 0.48F * wBlue + 0.82F * wTurq + 0.22F * wViolet
                        + 0.18F * wHard + 0.16F * wPurp + 0.38F * wPink;
                float wb = 0.95F * wBlue + 0.80F * wTurq + 0.78F * wViolet
                        + 0.88F * wHard + 0.82F * wPurp + 0.58F * wPink;
                float wsum = wBlue + wTurq + wViolet + wHard + wPurp + wPink;
                if (wsum > 0.004F) {
                    wr /= wsum;
                    wg /= wsum;
                    wb /= wsum;
                } else {
                    wr = 0.50F;
                    wg = 0.22F;
                    wb = 0.82F;
                }
                int cr = (int) (wr * 255.0F);
                int cg = (int) (wg * 255.0F);
                int cb = (int) (wb * 255.0F);

                for (int s = 0; s < SHELL_DEPTH.length; s++) {
                    double depth = bodyR * SHELL_DEPTH[s];
                    // offset along the camera→storm ray: negative depth sits
                    // BEHIND the silhouette (farther from cam when looking
                    // at the front face, closer to cam when looking from behind)
                    Vec3 slice = at.add(view.scale(depth));
                    double r = baseR * SHELL_SCALE[s];
                    int alpha = (int) (a * wShell * SHELL_ALPHA[s] * 125.0F);
                    // outer slices use the soft glare gradient; mid slices
                    // use the phase backdrop so the body colour bleeds through
                    Identifier tex = (s == 0 || s == SHELL_DEPTH.length - 1) ? GLARE
                            : (wHard > 0.4F || wViolet > 0.4F) ? PURPLE_PINK
                            : (wBlue > 0.4F) ? BLUE4
                            : (wTurq > 0.4F) ? TURQUOISE
                            : PURPLE;
                    RenderType rt = (s <= 2) ? GlowRenderTypes.glow(tex)
                            : GlowRenderTypes.translucent(tex);
                    quad(poseStack, collector, rt, slice, view, r, cr, cg, cb, alpha);
                }

                // extra outer wash — the wide soft aura the frames show
                // wrapping the flanks without floating off as a ring symbol
                quad(poseStack, collector, GlowRenderTypes.glow(GLARE),
                        at.add(view.scale(-bodyR * 0.15)), view,
                        baseR * (1.85 + 0.55 * glareMul),
                        cr, cg, cb, (int) (a * wShell * 70.0F));
            }

            // body colour plates (welded, slightly smaller than the shell)
            if (wPink > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(PURPLE_PINK), at, view,
                        baseR * 1.05, 255, 210, 230, (int) (a * wPink * 230.0F));
            }
            if (wPurp > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(PURPLE), at, view,
                        baseR * 0.98, 230, 190, 255, (int) (a * wPurp * 240.0F));
            }
            if (wHard > 0.004F || wViolet > 0.004F) {
                float wv = Math.max(wHard, wViolet);
                // 5.4–5.9 pinkish-violet body — the purple comes from HERE,
                // not the sky (sky carries the salmon-pink horizon)
                quad(poseStack, collector, GlowRenderTypes.translucent(PURPLE_PINK), at, view,
                        baseR * 1.02, 245, 200, 235, (int) (a * wv * 235.0F));
                quad(poseStack, collector, GlowRenderTypes.glow(PURPLE), at, view,
                        baseR * 0.88, 200, 90, 230, (int) (a * wv * 110.0F));
            }
            if (wTurq > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(TURQUOISE), at, view,
                        baseR * 0.95, 255, 255, 255, (int) (a * wTurq * 245.0F));
            }
            if (phase >= 6.4F) {
                quad(poseStack, collector, GlowRenderTypes.translucent(EMBER), at, view,
                        baseR * 1.10, 255, 255, 255, (int) (a * 55.0F));
            }
            if (wCore > 0.004F) {
                // dark storm heart, dead-centre
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK), at, view,
                        baseR * 0.72, 255, 255, 255, (int) (a * wCore * 240.0F));
            }

            // body palette fringe 5.5+ — dark-blue then dark-purple over it,
            // plain soft gradient, NO face/heads. The wide sky-glued halo and
            // the full silhouette stack (purple + moon-blue + black roof) are
            // drawn by McsmPhaseSky behind this shell.
            if (key == mainKey && wFace > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.glow(GLARE), at, view,
                        baseR * 1.14, 26, 52, 124, (int) (a * wFace * 92.0F));
                quad(poseStack, collector, GlowRenderTypes.glow(GLARE), at, view,
                        baseR * 1.02, 78, 28, 118, (int) (a * wFace * 76.0F));
            }
            if (wBlue > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.glow(BLUE4), at, view,
                        baseR * 0.90, 190, 215, 255, (int) (a * wBlue * 230.0F));
            }

            // ---------- PARTICLES (cubes / beams / motes / mist) -----------
            if (key == mainKey && wShell > 0.004F && baseR > 8.0) {
                final float bR = (float) baseR;
                final float tt = nowSec;
                final float aa = a;
                final float wg = wShell;
                final Vec3 atF = at;
                final Vec3 viewF = view;
                collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                        (pose, consumer) -> {
                    // black cubes peeling off the silhouette edge
                    for (int i = 0; i < 32; i++) {
                        float sd = i * 0.618034F;
                        float cyc = fract(tt * 0.05F + sd);
                        float ang = fract(sd) * 6.28318F;
                        float rr = (0.70F + 0.80F * cyc) * bR;
                        float x = (float) Math.cos(ang) * rr * 0.95F;
                        float y = (float) Math.sin(ang) * rr * 0.70F - cyc * 0.40F * bR;
                        Vec3 pq = billboardOffset(atF, viewF, x, y);
                        float sz = bR * (0.018F + 0.022F * fract(sd * 7.3F));
                        quadVerts(pose, consumer, pq, viewF, sz, 8, 6, 12,
                                (int) (aa * wg * 210.0F * (1.0F - cyc * 0.7F)));
                    }
                    // mist puffs at the storm's base
                    for (int i = 0; i < 8; i++) {
                        float sd = i * 0.31F + 0.17F;
                        float x = (fract(sd * 3.7F) * 2.4F - 1.2F) * bR;
                        float y = -1.05F * bR + fract(sd * 9.1F) * 0.3F * bR;
                        Vec3 pq = billboardOffset(atF, viewF, x, y);
                        quadVerts(pose, consumer, pq, viewF,
                                bR * (0.35F + 0.2F * fract(sd * 5.3F)), 150, 130, 170,
                                (int) (aa * wg * 28.0F));
                    }
                });
                collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                        (pose, consumer) -> {
                    // sparkle dots riding down inside each beam cone
                    for (int m = 0; m < 3; m++) {
                        for (int j = 0; j < 10; j++) {
                            float sd = m * 0.37F + j * 0.111F;
                            float tp = fract(tt * 0.22F + sd);
                            float gx = MOUTH_X[m] * 2.6F;
                            float gy = -1.5F;
                            float x = MOUTH_X[m] + (gx - MOUTH_X[m]) * tp
                                    + (fract(sd * 13.7F) - 0.5F) * 0.5F * tp;
                            float y = MOUTH_Y[m] + (gy - MOUTH_Y[m]) * tp
                                    + (fract(sd * 17.3F) - 0.5F) * 0.35F * tp;
                            Vec3 pq = billboardOffset(atF, viewF, bR * x, bR * y);
                            quadVerts(pose, consumer, pq, viewF, bR * 0.012F, 235, 225, 255,
                                    (int) (aa * wg * 190.0F * (1.0F - tp)));
                        }
                    }
                    // faint motes orbiting the whole storm
                    for (int i = 0; i < 36; i++) {
                        float sd = i * 0.4717F;
                        float ang = fract(sd) * 6.28318F + tt * 0.04F;
                        float rr = (0.35F + 1.25F * fract(sd * 5.1F)) * bR;
                        Vec3 pq = billboardOffset(atF, viewF,
                                (float) Math.cos(ang) * rr, (float) Math.sin(ang) * rr * 0.8F);
                        boolean purple = fract(sd * 3.3F) > 0.5F;
                        quadVerts(pose, consumer, pq, viewF, bR * 0.010F,
                                purple ? 200 : 240, purple ? 160 : 240, purple ? 255 : 250,
                                (int) (aa * wg * 70.0F * (0.4F + 0.6F * fract(sd * 11.0F))));
                    }
                });
            }

            // ---------- TEETH (body detail, NOT glare) --------------------
            // Reference frames: chunky white zigzag / dotted U-arc of teeth
            // sitting on each mouth emitter. Drawn last so they sit over the
            // body plate. The cyan inner-mouth + magenta emitter cube stay.
            if (key == mainKey && wMouth > 0.004F && baseR > 10.0) {
                for (int m = 0; m < 3; m++) {
                    Vec3 mo = billboardOffset(at, view, baseR * MOUTH_X[m], baseR * MOUTH_Y[m]);
                    // inner mouth cavity
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), mo, view,
                            baseR * 0.11, 120, 230, 225, (int) (a * wMouth * 110.0F));
                    // chunky zigzag teeth: 9 dashed blocks on a downward U,
                    // alternating offset for the dotted/zigzag read
                    for (int i = 0; i < 9; i++) {
                        float t = i / 8.0F;
                        float ang = (float) (Math.PI * (1.08 + 0.84 * t));
                        float zig = ((i & 1) == 1) ? 0.022F : -0.006F;
                        float tx = MOUTH_X[m] + (float) Math.cos(ang) * 0.135F;
                        float ty = MOUTH_Y[m] + (float) Math.sin(ang) * 0.115F + zig;
                        Vec3 tp = billboardOffset(at, view, baseR * tx, baseR * ty);
                        // chunky: larger than the old 0.028 dashes
                        double toothR = baseR * (0.038 + (((i & 1) == 1) ? 0.012 : 0.0));
                        quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), tp, view,
                                toothR, 255, 255, 255, (int) (a * wMouth * 250.0F));
                    }
                    // secondary inner dotted arc (the "dotted U" read)
                    for (int i = 0; i < 5; i++) {
                        float t = i / 4.0F;
                        float ang = (float) (Math.PI * (1.20 + 0.60 * t));
                        float tx = MOUTH_X[m] + (float) Math.cos(ang) * 0.085F;
                        float ty = MOUTH_Y[m] + (float) Math.sin(ang) * 0.070F;
                        Vec3 tp = billboardOffset(at, view, baseR * tx, baseR * ty);
                        quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), tp, view,
                                baseR * 0.018, 240, 250, 255, (int) (a * wMouth * 200.0F));
                    }
                    // magenta emitter cube above the mouth
                    Vec3 cp = billboardOffset(at, view, baseR * MOUTH_X[m],
                            baseR * (MOUTH_Y[m] + 0.18F));
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), cp, view,
                            baseR * 0.048, 232, 40, 226, (int) (a * wMouth * 255.0F));
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
