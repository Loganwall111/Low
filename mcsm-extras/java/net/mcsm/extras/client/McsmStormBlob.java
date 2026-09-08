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
 * Body-local mouth / teeth / beam detail + glossy stripe.
 *
 * The giant 3D phase gradient volume lives in McsmPhaseSky (world-space
 * nested spheres glued to the storm). This class does NOT paint halo
 * billboards, orbiting dots, black debris cubes, or underside motes —
 * those were rejected. Teeth stay on the body; tractor beams stay as
 * soft cones without sparkle particle fields.
 *
 * Phase-only left/right sway is shared with McsmPhaseSky.swayOffset so
 * the body detail never slides off the volume.
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
            0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F, 0.00F
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


    /**
     * Phase-dynamic tooth colours (user frames):
     *   phase 4.x     very light blue / icy white-blue
     *   phase 5.0     pure white (no blue)
     *   phase 5.1-5.4 mostly white, slight blue
     *   phase 5.5-5.9 glowing white
     *   phase 6+      extremely dark blue
     */
    private static int[] teethRgb(float phase) {
        if (phase >= 6.0F) {
            return new int[] { 18, 40, 95 };          // extremely dark blue
        }
        if (phase >= 5.5F) {
            return new int[] { 250, 252, 255 };       // glowing white
        }
        if (phase >= 5.1F) {
            return new int[] { 235, 245, 255 };       // white + slight blue
        }
        if (phase >= 5.0F) {
            return new int[] { 255, 255, 255 };       // pure white
        }
        // phase 4.x — very light blue
        return new int[] { 195, 225, 255 };
    }

    private static int[] teethRimRgb(float phase) {
        if (phase >= 6.0F) {
            return new int[] { 10, 28, 70 };
        }
        if (phase >= 5.5F) {
            return new int[] { 220, 235, 255 };
        }
        if (phase >= 5.0F) {
            return new int[] { 210, 230, 255 };
        }
        return new int[] { 150, 195, 245 };
    }

    private static float teethAlphaMul(float phase) {
        if (phase >= 6.0F) {
            return 0.85F;   // dark blue still visible but not blast
        }
        if (phase >= 5.5F) {
            return 1.0F;    // glowing white
        }
        if (phase >= 5.0F) {
            return 0.95F;
        }
        return 0.90F;
    }

    private static double bodyRadius(float phase) {
        if (phase < 4.0F) {
            return 4.0F + 1.5F * phase;
        } else if (phase < 5.0F) {
            return 10.0F + 8.0F * (phase - 4.0F);
        } else if (phase < 6.0F) {
            return 18.0F + 22.0F * (phase - 5.0F);
        } else if (phase < 7.0F) {
            return 40.0F + 55.0F * (phase - 6.0F);
        } else {
            return 95.0F + 80.0F * Math.min(phase - 7.0F, 3.0F);
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
            // phase-only L/R sway — same offset as McsmPhaseSky so body stays inside volume
            double bodyREarly = bodyRadius(phase);
            centre = centre.add(McsmPhaseSky.swayOffset(phase, nowSec, bodyREarly));

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
            // keep shell ON the body — no far floating sky disc
            double skyDist = Math.min(Math.max(dist * 0.88, bodyR * 2.0), 200.0);
            double angular = Mth.clamp(bodyR / Math.max(dist, 1.0), 0.010, 0.55);
            double skyR = skyDist * angular * (1.25 + 0.9 * glareMul) * smudge;
            float nearW = 1.0F - Mth.clamp((float) ((dist - 100.0) / 500.0), 0.0F, 1.0F);
            // bias hard toward body-glued radius
            double baseR = shellR * (0.55 + 0.45 * nearW) + skyR * (0.45 * (1.0F - nearW));
            // when far, the shell still sits ON the storm ray (at the storm
            // itself when near, sliding toward skyDist only as distance grows)
            double placeDist = dist * nearW + skyDist * (1.0F - nearW);
            // never push past the real storm centre when we are in front of it
            placeDist = Math.min(placeDist, dist * 0.98);
            Vec3 at = cam.add(view.scale(Math.max(placeDist, bodyR * 1.2)));

            float a = distFade;

            // phase colour weights — user strips:
            //   5.0 teal/green, 5.4 purple, 5.5 pink-magenta (was SKIPPED),
            //   6+ deep purple. NO face backdrop. Black blur core always.
            float wBlue = ramp(phase, 3.95F, 4.2F) * (1.0F - ramp(phase, 4.7F, 5.05F));
            float wTurq = ramp(phase, 4.85F, 5.10F) * (1.0F - ramp(phase, 5.30F, 5.42F)); // phase 5 green
            float wViolet = ramp(phase, 5.30F, 5.42F) * (1.0F - ramp(phase, 5.52F, 5.65F)); // 5.4 purple
            float wHard = ramp(phase, 5.52F, 5.65F) * (1.0F - ramp(phase, 5.92F, 6.10F)); // 5.5 pink
            float wPurp = ramp(phase, 5.92F, 6.15F); // 6+ deep purple
            float wPink = ramp(phase, 6.20F, 6.9F); // post light pink
            float wCore = ramp(phase, 4.0F, 4.3F);
            // NO face overlay — user killed the face-painted backdrop
            float wFace = 0.0F;
            float wShell = ramp(phase, 3.95F, 4.25F);
            float wMouth = ramp(phase, 3.9F, 4.3F);

            // ---------- BODY COLOUR PLATES (tight, not the sky volume) ----
            // McsmPhaseSky owns the giant 3D gradient shell. These plates
            // only tint the body mass itself so the silhouette still reads.
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
                    int alpha = (int) (a * wShell * SHELL_ALPHA[s] * 62.0F);
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

            // body-local extremely dark blue-black silhouette for phase
            // 5.5+ — layered over purple/blue plates so the upper body reads as
            // a black silhouette next to the purple + blue stack (matches the
            // sky-glued wrap in McsmPhaseSky). Face backdrop stays removed.
            if (key == mainKey && wHard > 0.004F) {
                float ws = a * wHard;
                // outer dark-blue-black wrap (sides + bottom of body)
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK),
                        at.add(view.scale(bodyR * 0.05)), view,
                        baseR * 1.12, 6, 10, 24, (int) (ws * 195.0F));
                // dense upper-body black silhouette mass
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK),
                        at.add(view.scale(bodyR * 0.08)), view,
                        baseR * 0.95, 3, 5, 14, (int) (ws * 235.0F));
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK),
                        at.add(view.scale(bodyR * 0.12)), view,
                        baseR * 0.78, 2, 3, 10, (int) (ws * 245.0F));
                // cold blue-black fringe so it sits "next to" the blue silhouette
                quad(poseStack, collector, GlowRenderTypes.glow(BLUE4),
                        at.add(view.scale(bodyR * 0.02)), view,
                        baseR * 1.05, 8, 16, 40, (int) (ws * 55.0F));
            }
            if (wBlue > 0.004F) {
                quad(poseStack, collector, GlowRenderTypes.glow(BLUE4), at, view,
                        baseR * 0.90, 190, 215, 255, (int) (a * wBlue * 230.0F));
            }

            // ---------- OG GLOSSY STRIPE (user frame 2026-08-24) ------------
            // MCSM body is pure black mass with a blue sheen UNDER the black
            // (or black stripe over blue) — reverse-shading glossy read.
            // Painted as stacked depth plates: blue underlay, black stripe
            // bars, thin cyan-blue rim. No three-head symbol.
            if (key == mainKey && wShell > 0.004F && baseR > 6.0) {
                float wg = Math.max(wCore, Math.max(wBlue, Math.max(wHard, wViolet)));
                wg = Math.max(wg, 0.55F * wShell);
                // blue sheen under the black mass (the "blue under black")
                // blue sheen only as thin under-stripe (phase6 glossy), not gray outline
                quad(poseStack, collector, GlowRenderTypes.glow(BLUE4),
                        at.add(view.scale(-bodyR * 0.06)), view,
                        baseR * 0.90, 12, 30, 90, (int) (a * wg * 35.0F));
                // black stripe mass over the blue (the "black under/over blue")
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK), at, view,
                        baseR * 0.82, 255, 255, 255, (int) (a * wg * 210.0F));
                quad(poseStack, collector, GlowRenderTypes.translucent(BLACK),
                        at.add(view.scale(bodyR * 0.05)), view,
                        baseR * 0.70, 255, 255, 255, (int) (a * wg * 180.0F));
                // thin glossy cyan-blue rim catching light on the silhouette
                quad(poseStack, collector, GlowRenderTypes.glow(GLARE),
                        at.add(view.scale(bodyR * 0.02)), view,
                        baseR * 0.78, 70, 140, 255, (int) (a * wg * 55.0F));
            }

            // ---------- TRACTOR BEAMS (no debris cubes / no motes) --------
            // Soft purple/blue cones from each mouth. No orbiting black
            // cubes, no underside mist puffs, no sparkle particle field —
            // those read as "dots around the halo" and were rejected.
            if (key == mainKey && wShell > 0.004F && baseR > 8.0) {
                final float bR = (float) baseR;
                final float aa = a;
                final float wg = wShell;
                final Vec3 atF = at;
                final Vec3 viewF = view;
                collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(WHITE),
                        (pose, consumer) -> {
                    for (int m = 0; m < 3; m++) {
                        for (int k = 0; k < 8; k++) {
                            float tp = k / 7.0F;
                            float gx = MOUTH_X[m] * 2.8F;
                            float gy = -1.7F;
                            float x = MOUTH_X[m] + (gx - MOUTH_X[m]) * tp;
                            float y = MOUTH_Y[m] + (gy - MOUTH_Y[m]) * tp;
                            Vec3 pq = billboardOffset(atF, viewF, bR * x, bR * y);
                            float coneR = bR * (0.035F + 0.14F * tp);
                            quadVerts(pose, consumer, pq, viewF, coneR,
                                    160, 70, 255, (int) (aa * wg * 55.0F * (1.0F - tp * 0.45F)));
                            quadVerts(pose, consumer, pq, viewF, coneR * 0.55F,
                                    90, 140, 255, (int) (aa * wg * 80.0F * (1.0F - tp * 0.35F)));
                        }
                    }
                });
            }

            // ---------- TEETH (body detail, NOT glare) --------------------
            // Ground-truth frames (user batch 2026-09-07): each mouth is a
            // U-shaped arc of DISCRETE glowing cyan-white rectangular blocks
            // (dotted ring / dashed smile), with a hot-magenta cube above.
            // Not a solid bar, not a glare decal — chunky emissive cubes.
            if (key == mainKey && wMouth > 0.004F && baseR > 10.0) {
                int[] trgb = teethRgb(phase);
                int[] rrgb = teethRimRgb(phase);
                float tam = teethAlphaMul(phase);
                for (int m = 0; m < 3; m++) {
                    Vec3 mo = billboardOffset(at, view, baseR * MOUTH_X[m], baseR * MOUTH_Y[m]);
                    // dark cavity behind the teeth so the blocks pop
                    quadAt(poseStack, collector, GlowRenderTypes.translucent(BLACK), mo, view,
                            baseR * 0.16, 8, 6, 14, (int) (a * wMouth * 200.0F));
                    // mouth wash tinted toward the phase tooth colour
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), mo, view,
                            baseR * 0.12, trgb[0], trgb[1], trgb[2], (int) (a * wMouth * tam * 55.0F));
                    // outer U-arc: 11 chunky blocks
                    for (int i = 0; i < 11; i++) {
                        float tt = i / 10.0F;
                        float ang = (float) (Math.PI * (1.05 + 0.90 * tt));
                        float zig = ((i & 1) == 1) ? 0.028F : -0.010F;
                        float rad = 0.155F + (((i & 1) == 1) ? 0.012F : 0.0F);
                        float tx = MOUTH_X[m] + (float) Math.cos(ang) * rad;
                        float ty = MOUTH_Y[m] + (float) Math.sin(ang) * (rad * 0.88F) + zig;
                        Vec3 tp = billboardOffset(at, view, baseR * tx, baseR * ty);
                        double toothR = baseR * (0.048 + (((i & 1) == 1) ? 0.016 : 0.0));
                        quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), tp, view,
                                toothR, trgb[0], trgb[1], trgb[2], (int) (a * wMouth * tam * 220.0F));
                        quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), tp, view,
                                toothR * 1.35, rrgb[0], rrgb[1], rrgb[2], (int) (a * wMouth * tam * 45.0F));
                    }
                    // secondary inner dotted arc
                    for (int i = 0; i < 7; i++) {
                        float tt = i / 6.0F;
                        float ang = (float) (Math.PI * (1.18 + 0.64 * tt));
                        float tx = MOUTH_X[m] + (float) Math.cos(ang) * 0.095F;
                        float ty = MOUTH_Y[m] + (float) Math.sin(ang) * 0.078F;
                        Vec3 tp = billboardOffset(at, view, baseR * tx, baseR * ty);
                        quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), tp, view,
                                baseR * 0.026, trgb[0], trgb[1], trgb[2], (int) (a * wMouth * tam * 180.0F));
                    }
                    // hot-magenta emitter cube ABOVE the mouth
                    Vec3 cp = billboardOffset(at, view, baseR * MOUTH_X[m],
                            baseR * (MOUTH_Y[m] + 0.20F));
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), cp, view,
                            baseR * 0.062, 235, 30, 200, (int) (a * wMouth * 230.0F));
                    quadAt(poseStack, collector, GlowRenderTypes.glow(WHITE), cp, view,
                            baseR * 0.095, 230, 60, 210, (int) (a * wMouth * 70.0F));
                }
            }

            // ---------- DEBRIS RING (phase 4+, NOT StormDebris cube swarm) --
            // MCSM refs: loose dark mass chunks orbiting the lower body / waist,
            // pulled upward into the storm. Soft dark slabs + phase-tinted edges.
            // Distinct from the killed StormDebris particle cube field.
            if (key == mainKey && phase >= 3.9F && baseR > 12.0) {
                float ringA = a * ramp(phase, 3.9F, 4.4F);
                float spin = nowSec * 0.18F;
                int pieces = phase >= 5.5F ? 28 : (phase >= 5.0F ? 22 : 16);
                for (int i = 0; i < pieces; i++) {
                    float ang = spin + (float) (i * Math.PI * 2.0 / pieces);
                    float wobble = 0.08F * Mth.sin(nowSec * 0.55F + i * 1.7F);
                    float radMul = 0.95F + 0.35F * ((i % 5) / 4.0F) + wobble;
                    float elev = -0.55F + 0.18F * Mth.sin(ang * 2.0F + nowSec * 0.3F)
                            + 0.08F * ((i % 3) - 1);
                    // orbit in a plane roughly under the body, slightly tilted
                    float ox = (float) Math.cos(ang) * radMul;
                    float oz = (float) Math.sin(ang) * radMul * 0.55F; // depth via view
                    float oy = elev;
                    Vec3 piece = billboardOffset(at, view,
                            baseR * ox * 1.15F,
                            baseR * oy);
                    // push along view for depth variety
                    piece = piece.add(view.scale(baseR * oz * 0.35F));
                    double pr = baseR * (0.055 + 0.04 * ((i % 4) / 3.0));
                    // dark mass chunk
                    int da = (int) (ringA * (160.0F + 40.0F * ((i % 3) / 2.0F)));
                    quadAt(poseStack, collector, GlowRenderTypes.translucent(BLACK), piece, view,
                            pr, 8, 8, 12, da);
                    // gray edge lines on pre-6 chunks (match flesh gray edges)
                    if (phase < 6.0F) {
                        quadAt(poseStack, collector, GlowRenderTypes.translucent(WHITE), piece, view,
                                pr * 1.12, 55, 58, 65, (int) (ringA * 35.0F));
                    } else {
                        // phase 6+ blue sheen edge
                        quadAt(poseStack, collector, GlowRenderTypes.glow(BLUE4), piece, view,
                                pr * 1.08, 20, 50, 120, (int) (ringA * 40.0F));
                    }
                    // occasional pulled-up trail chunk
                    if ((i % 4) == 0) {
                        Vec3 trail = piece.add(view.scale(-baseR * 0.08F))
                                .add(0.0, baseR * 0.12F, 0.0);
                        quadAt(poseStack, collector, GlowRenderTypes.translucent(BLACK), trail, view,
                                pr * 0.55, 10, 10, 14, (int) (ringA * 90.0F));
                    }
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
