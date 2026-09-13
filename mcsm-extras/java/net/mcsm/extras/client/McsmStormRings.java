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
 * 1.9.208 -- REAL cubed rings around the storm.
 *
 * Not skybox layers: actual block-cube rings orbiting the creature in the
 * world.  Phase 6 raises one horizontal + one vertical + one diagonal ring
 * (all clockwise).  Phase 7 adds a second, counter-spinning set and widens
 * them.  Phase 8-9 goes full apocalypse: three gigantic major rings, each
 * built from 10 thin layered sub-rings stacked into a funnel that is thin at
 * the top -- the vortex -- with the outer set spinning clockwise and the
 * inner set counter-clockwise, dyed in the phase 8-9 ember palette.
 *
 * Every cube is a pair of billboarded quads (a square and its 45-degree
 * diamond twin) so it reads as a chunky block from any distance.  Fully
 * stateless: positions are functions of index + time.
 */
public final class McsmStormRings {

    private McsmStormRings() {
    }

    private static final Map<Integer, Vec3> SMOOTH = new HashMap<>();

    // Embedded materials used by the supplied Stage C debris and Stage D
    // split models, not hand-painted ring replacements.
    private static final Identifier STAGE_C_DEBRIS = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/mcsm_atmosphere/stage_c_debris.png");
    private static final Identifier STAGE_D_SPLIT = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/mcsm_atmosphere/stage_d_split.png");

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double bodyRadius(float phase) {
        if (phase < 6.0F) {
            return 18.0D + 22.0D * (phase - 5.0D);
        }
        return Math.min(340.0D, 62.0D + 46.0D * (phase - 6.0D));
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            // Config is loaded by the client tick; keep this render-only
            // pass free of filesystem work and model parsing.
            if (!McsmExtrasConfig.stormRings) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return;
            }
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            float gt = (float) (mc.level.getGameTime() % 240000L)
                    + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float nowSec = gt * 0.05F;
            PoseStack poseStack = ctx.poseStack();
            SubmitNodeCollector collector = ctx.submitNodeCollector();

            int idx = 0;
            for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
                int key = idx++;
                float phase = d.phase;
                if (phase < 5.95F) {
                    continue; // rings appear with the phase-6 split
                }
                Vec3 centre = new Vec3(d.dispX, d.dispY, d.dispZ);
                Vec3 prev = SMOOTH.get(key);
                if (prev != null) {
                    centre = prev.add(centre.subtract(prev).scale(0.25D));
                }
                SMOOTH.put(key, centre);
                Vec3 toCam = centre.subtract(cam);
                double dist = toCam.length();
                if (dist < 1.0E-4D || dist > 2800.0D) {
                    continue;
                }
                float distFade = 1.0F - Mth.clamp((float) ((dist - 1500.0D) / 1100.0D), 0.0F, 1.0F);
                float on = ramp(phase, 5.95F, 6.10F) * distFade;
                if (on <= 0.01F) {
                    continue;
                }
                final double bR = bodyRadius(phase);
                final float fade = on;
                final float tSec = nowSec;
                final Vec3 c = centre;
                // The supplied Vortex model belongs to the split/late ladder:
                // it must not appear during phase 6 or the pre-split Stage C
                // entries.  Phase 7 is the first frame where it is allowed.
                final boolean vortex = phase >= 7.0F;
                final boolean phase7 = phase >= 7.0F;
                // Phase 9 is the sky-filling finale: expand the normal 1,200
                // cube positions to the requested approximately 10,000. Keep
                // the lighter phase-7/8 pass intact so the density ramps in
                // instead of appearing as a single-frame wall.
                final boolean phase9 = phase >= 8.0F;
                // p6-7: dark indigo blocks w/ purple edge; p8-9: ember
                final float cr = vortex ? 62 : 34;
                final float cg = vortex ? 26 : 27;
                final float cb = vortex ? 18 : 52;

                Identifier blockTex = vortex ? STAGE_D_SPLIT
                        : (phase7 ? STAGE_D_SPLIT : STAGE_C_DEBRIS);
                final RenderType ringType = GlowRenderTypes.translucent(blockTex);
                collector.submitCustomGeometry(poseStack, ringType,
                        (pose, consumer) -> {
                            // Stage C/D debris rings remain the base layer;
                            // the Vortex is an additional top-of-storm object,
                            // never a replacement for the existing animation.
                            drawPhase67(pose, consumer, c, cam, bR, tSec, fade, phase7, cr, cg, cb);
                            if (vortex) {
                                drawVortex(pose, consumer, c, cam, bR, tSec, fade, cr, cg, cb, phase9);
                            }
                        });
                // 1.9.220 -- the REAL Telltale vortex model (ported from
                // Vortex.bbmodel): the funnel backdrop strip, the alpha
                // swirl and the black cube ring, lathed around the storm.
                // It is explicitly phase-7+, with a short fade-in so loading
                // the first world never has to parse a Blockbench file.
                if (vortex) {
                    float vortexStrength = ramp(phase, 7.0F, 7.35F);
                    drawVortexMeshes(poseStack, collector, c, bR, tSec, fade,
                            cr, cg, cb, vortexStrength);
                }
            }
        } catch (Throwable ignored) {
            // a visual must never break a frame
        }
    }

    /** Phase 6-7: horizontal + vertical + diagonal rings; 7 adds a second,
     *  counter-spinning diagonal set and grows the radii. */
    private static void drawPhase67(Pose pose, VertexConsumer consumer, Vec3 c, Vec3 cam,
            double bR, float tSec, float fade, boolean phase7, float cr, float cg, float cb) {
        double grow = phase7 ? 1.60D : 1.0D;
        int n = phase7 ? 32 : 26;
        double cube = bR * (phase7 ? 0.045D : 0.040D);

        // ring 1: horizontal, clockwise (viewed from above)
        ring(pose, consumer, c, cam, bR * 1.55D * grow, 0.0D, 0.0D, n, cube,
                tSec * 0.055F, fade, cr, cg, cb, false);
        // ring 2: vertical (rolling through the storm), clockwise
        ring(pose, consumer, c, cam, bR * 1.40D * grow, 90.0D, 0.0D, n, cube,
                tSec * 0.045F, fade * 0.9F, cr, cg, cb, false);
        // ring 3: diagonal 35 degrees, clockwise
        ring(pose, consumer, c, cam, bR * 1.65D * grow, 35.0D, 24.0D, n, cube,
                tSec * 0.050F, fade * 0.85F, cr, cg, cb, false);
        if (phase7) {
            // 1.9.212: phase 7 rings grow to cover the whole sky -- a big
            // flat horizontal ring high above the storm, thin at the top.
            ring(pose, consumer, c.add(0.0D, bR * 0.95D, 0.0D), cam, bR * 2.6D, 6.0D, 30.0D, 40,
                    cube * 0.7D, tSec * 0.030F, fade * 0.75F, cr, cg, cb, false);
            // ring 4: counter-clockwise diagonal, smaller, inner
            ring(pose, consumer, c, cam, bR * 1.45D, -52.0D, 18.0D, 26, cube * 0.8D,
                    -tSec * 0.040F, fade * 0.8F, cr, cg, cb, true);
            // ring 5: counter-clockwise horizontal below the body
            ring(pose, consumer, c, cam, bR * 1.30D, 0.0D, 0.0D, 24, cube * 0.8D,
                    -tSec * 0.048F, fade * 0.75F, cr, cg, cb, true);
        }
    }

    /** Phase 8-9: three major rings, each 10 layered thin sub-rings, funneled
     *  (thin at the top), outer clockwise / inner counter-clockwise. */
    private static void drawVortex(Pose pose, VertexConsumer consumer, Vec3 c, Vec3 cam,
            double bR, float tSec, float fade, float cr, float cg, float cb, boolean phase9) {
        // Keep the procedural cube rings as the under-layer of the vortex. In
        // phase 9 the thirty ring/layer buckets receive an even quota of the
        // configured target, producing exactly 10,000 positions by default
        // (rather than the old 3 x 10 x 40 = 1,200).
        int target = phase9 ? Math.max(30, McsmExtrasConfig.phase9RingCubes) : 1200;
        int perBucket = target / 30;
        int remainder = target % 30;
        for (int ringIdx = 0; ringIdx < 3; ringIdx++) {
            double majorR = bR * (4.30D - 0.55D * ringIdx);
            boolean ccw = (ringIdx == 2);
            for (int layer = 0; layer < 10; layer++) {
                int bucket = ringIdx * 10 + layer;
                int n = phase9 ? perBucket + (bucket < remainder ? 1 : 0) : 40;
                double h = (layer - 4.5D) / 4.5D;
                double r = majorR * (1.0D - 0.16D * Math.max(0.0D, h));
                double yOff = bR * h * 0.55D * (1.0D + 0.25D * ringIdx);
                double cube = bR * (0.030D + 0.020D * layer / 9.0D);
                if (h > 0.0D) {
                    cube *= (1.0D - 0.55D * h);
                }
                double speed = (ccw ? -1.0D : 1.0D) * (0.030D + 0.010D * ringIdx);
                double tilt = (ringIdx == 1) ? 14.0D : 0.0D;
                double azim = ringIdx * 40.0D;
                float a = fade * (0.55F + 0.45F * (1.0F - (float) Math.abs(h)));
                ring(pose, consumer, c.add(0.0D, yOff, 0.0D), cam, r, tilt, azim, n, cube,
                        tSec * (float) speed, a, cr, cg, cb, ccw);
            }
        }
    }

    /** The REAL Telltale vortex (Vortex.bbmodel ported to static arrays). */
    private static void drawVortexMeshes(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 c, double bR, float tSec, float fade, float cr, float cg, float cb,
            float strength) {
        if (strength <= 0.01F) {
            return;
        }
        for (McsmVortexMesh.Group g : McsmVortexMesh.GROUPS) {
            boolean cubes = g.texture.contains("color_000");
            boolean backdrop = g.texture.contains("Backdrop") && !g.texture.contains("alp");
            double scale = bR * (backdrop ? 3.60D : 3.30D) * (cubes ? 1.0D : strength);
            if (!cubes) {
                scale *= 0.55D + 0.45D * strength;
            }
            double spin = tSec * (cubes ? 0.070D : (backdrop ? 0.045D : -0.055D));
            Identifier tex = cubes
                    ? Identifier.fromNamespaceAndPath("dabywitherstormmod",
                            "textures/mcsm_atmosphere/" + g.texture)
                    : Identifier.fromNamespaceAndPath("dabywitherstormmod",
                            "textures/mcsm_atmosphere/" + g.texture);
            float scaleF = (float) scale;
            float spinF = (float) spin;
            float fadeF = fade * strength;
            float r = cubes ? 1.0F : (backdrop ? cr / 255.0F : Math.min(1.0F, cr / 200.0F));
            float gg = cubes ? 1.0F : (backdrop ? cg / 255.0F : Math.min(1.0F, cg / 200.0F));
            float bb = cubes ? 1.0F : (backdrop ? cb / 255.0F : Math.min(1.0F, cb / 200.0F));
            int alpha = cubes ? (int)(fadeF * 210.0F) : (backdrop ? (int)(fadeF * 150.0F) : (int)(fadeF * 95.0F));
            if (alpha <= 2) {
                continue;
            }
            final Identifier ftex = tex;
            final int falpha = alpha;
            final float fr = r, fg = gg, fb = bb;
            collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(ftex),
                    (pose, consumer) -> {
                        int ir = Mth.clamp((int)(fr * 255.0F), 0, 255);
                        int ig = Mth.clamp((int)(fg * 255.0F), 0, 255);
                        int ib = Mth.clamp((int)(fb * 255.0F), 0, 255);
                        double cs = Math.cos(spinF);
                        double sn = Math.sin(spinF);
                        float[] pos = g.pos;
                        float[] uv = g.uv;
                        for (int i = 0; i < g.idx.length; i++) {
                            int vi = g.idx[i] * 3;
                            int ui = g.idx[i] * 2;
                            float x = pos[vi];
                            float y = pos[vi + 1];
                            float z = pos[vi + 2];
                            double wx = (x * cs - z * sn) * scaleF;
                            double wy = y * scaleF;
                            double wz = (x * sn + z * cs) * scaleF;
                            consumer.addVertex(pose, (float)(c.x + wx), (float)(c.y + wy), (float)(c.z + wz))
                                    .setColor(ir, ig, ib, falpha)
                                    .setUv(uv[ui], uv[ui + 1])
                                    .setOverlay(OverlayTexture.NO_OVERLAY)
                                    .setLight(15728880)
                                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
                        }
                    });
        }
    }

    /** One ring of cube-pairs; tilt rotates the ring plane away from the
     *  horizontal, azim spins the tilt direction, spin drives the orbit. */
    private static void ring(Pose pose, VertexConsumer consumer, Vec3 c, Vec3 cam,
            double r, double tiltDeg, double azimDeg, int n, double cube,
            float spin, float fade, float cr, float cg, float cb, boolean ccw) {
        double tilt = Math.toRadians(tiltDeg);
        double azim = Math.toRadians(azimDeg);
        // ring-plane basis: horizontal circle tilted about the azimuth axis
        for (int i = 0; i < n; i++) {
            double a = 2.0D * Math.PI * i / n + (ccw ? -spin : spin);
            double x0 = Math.cos(a) * r;
            double z0 = Math.sin(a) * r;
            // tilt about the (cos azim, 0, sin azim) axis
            double axisX = Math.cos(azim), axisZ = Math.sin(azim);
            double dot = x0 * axisX + z0 * axisZ;
            double vx = axisX * dot * (1.0D - Math.cos(tilt)) + x0 * Math.cos(tilt);
            double vz = axisZ * dot * (1.0D - Math.cos(tilt)) + z0 * Math.cos(tilt);
            double vy = (z0 * axisX - x0 * axisZ) * Math.sin(tilt);
            Vec3 p = c.add(vx, vy, vz);
            double d = Math.max(1.0D, p.subtract(cam).length());
            if (d > 3000.0D) {
                continue;
            }
            cubePair(pose, consumer, p, cam, cube, fade, cr, cg, cb);
        }
    }

    /** A square + its 45-degree diamond twin -> a chunky cube read. */
    private static void cubePair(Pose pose, VertexConsumer consumer, Vec3 p, Vec3 cam,
            double cube, float fade, float cr, float cg, float cb) {
        Vec3 view = p.subtract(cam).normalize();
        Vec3 upHint = Math.abs(view.y) > 0.98D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = view.cross(upHint).normalize();
        Vec3 up = right.cross(view).normalize();
        Vec3 rx = right.scale(cube);
        Vec3 uy = up.scale(cube);
        int a = Mth.clamp((int) (fade * 245.0F), 0, 255);
        int ir = Mth.clamp((int) (cr * 255.0F), 0, 255);
        int ig = Mth.clamp((int) (cg * 255.0F), 0, 255);
        int ib = Mth.clamp((int) (cb * 255.0F), 0, 255);
        // axis-aligned square: full real block texture
        v(pose, consumer, p.subtract(rx).subtract(uy), 0.0F, 1.0F, ir, ig, ib, a);
        v(pose, consumer, p.add(rx).subtract(uy), 1.0F, 1.0F, ir, ig, ib, a);
        v(pose, consumer, p.add(rx).add(uy), 1.0F, 0.0F, ir, ig, ib, a);
        v(pose, consumer, p.subtract(rx).add(uy), 0.0F, 0.0F, ir, ig, ib, a);
        // 45-degree diamond twin (slightly brighter = block facet)
        Vec3 dx = rx.add(uy).scale(0.7071D);
        Vec3 dy = rx.scale(-1.0D).add(uy).scale(0.7071D);
        int ar = Mth.clamp(ir + 26, 0, 255);
        int ag = Mth.clamp(ig + 26, 0, 255);
        int ab = Mth.clamp(ib + 34, 0, 255);
        v(pose, consumer, p.subtract(dx), 0.35F, 0.65F, ar, ag, ab, a);
        v(pose, consumer, p.add(dy), 0.65F, 0.65F, ar, ag, ab, a);
        v(pose, consumer, p.add(dx), 0.65F, 0.35F, ar, ag, ab, a);
        v(pose, consumer, p.subtract(dy), 0.35F, 0.35F, ar, ag, ab, a);
    }

    private static void v(Pose pose, VertexConsumer consumer, Vec3 at,
            float u, float vv, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(r, g, b, a)
                .setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
