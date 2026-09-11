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
import net.minecraft.client.renderer.texture.OverlayTexture;
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
        return Math.min(320.0D, 55.0D + 42.0D * (phase - 6.0D));
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            McsmExtrasConfig.load();
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
                final boolean vortex = phase >= 7.95F;
                final boolean phase7 = phase >= 7.0F;
                // p6-7: dark indigo blocks w/ purple edge; p8-9: ember
                final float cr = vortex ? 62 : 34;
                final float cg = vortex ? 26 : 27;
                final float cb = vortex ? 18 : 52;

                collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                        "dabywitherstormmod", "textures/misc/storm_white.png")),
                        (pose, consumer) -> {
                            if (vortex) {
                                drawVortex(pose, consumer, c, cam, bR, tSec, fade, cr, cg, cb);
                            } else {
                                drawPhase67(pose, consumer, c, cam, bR, tSec, fade, phase7, cr, cg, cb);
                            }
                        });
            }
        } catch (Throwable ignored) {
            // a visual must never break a frame
        }
    }

    /** Phase 6-7: horizontal + vertical + diagonal rings; 7 adds a second,
     *  counter-spinning diagonal set and grows the radii. */
    private static void drawPhase67(Pose pose, VertexConsumer consumer, Vec3 c, Vec3 cam,
            double bR, float tSec, float fade, boolean phase7, float cr, float cg, float cb) {
        double grow = phase7 ? 1.18D : 1.0D;
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
            // ring 4: counter-clockwise diagonal, smaller, inner
            ring(pose, consumer, c, cam, bR * 1.25D, -52.0D, 18.0D, 26, cube * 0.8D,
                    -tSec * 0.040F, fade * 0.8F, cr, cg, cb, true);
            // ring 5: counter-clockwise horizontal below the body
            ring(pose, consumer, c, cam, bR * 1.15D, 0.0D, 0.0D, 24, cube * 0.8D,
                    -tSec * 0.048F, fade * 0.75F, cr, cg, cb, true);
        }
    }

    /** Phase 8-9: three major rings, each 10 layered thin sub-rings, funneled
     *  (thin at the top), outer clockwise / inner counter-clockwise. */
    private static void drawVortex(Pose pose, VertexConsumer consumer, Vec3 c, Vec3 cam,
            double bR, float tSec, float fade, float cr, float cg, float cb) {
        int n = 40;
        // major ring radii and heights (top rings thinner + smaller = vortex)
        for (int ringIdx = 0; ringIdx < 3; ringIdx++) {
            double majorR = bR * (3.10D - 0.45D * ringIdx);
            boolean ccw = (ringIdx == 2);
            for (int layer = 0; layer < 10; layer++) {
                // funnel: higher layers pull inward and thin out
                double h = (layer - 4.5D) / 4.5D;             // -1 (low) .. +1 (high)
                double r = majorR * (1.0D - 0.16D * Math.max(0.0D, h));
                double yOff = bR * h * 0.55D * (1.0D + 0.25D * ringIdx);
                double cube = bR * (0.030D + 0.020D * layer / 9.0D);
                if (h > 0.0D) {
                    cube *= (1.0D - 0.55D * h);               // thin at the top
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
        int a = Mth.clamp((int) (fade * 235.0F), 0, 255);
        int ir = Mth.clamp((int) (cr * 255.0F), 0, 255);
        int ig = Mth.clamp((int) (cg * 255.0F), 0, 255);
        int ib = Mth.clamp((int) (cb * 255.0F), 0, 255);
        // axis-aligned square
        v(pose, consumer, p.subtract(rx).subtract(uy), ir, ig, ib, a);
        v(pose, consumer, p.add(rx).subtract(uy), ir, ig, ib, a);
        v(pose, consumer, p.add(rx).add(uy), ir, ig, ib, a);
        v(pose, consumer, p.subtract(rx).add(uy), ir, ig, ib, a);
        // 45-degree diamond twin (slightly brighter = block facet)
        Vec3 dx = rx.add(uy).scale(0.7071D);
        Vec3 dy = rx.scale(-1.0D).add(uy).scale(0.7071D);
        int ar = Mth.clamp(ir + 26, 0, 255);
        int ag = Mth.clamp(ig + 26, 0, 255);
        int ab = Mth.clamp(ib + 34, 0, 255);
        v(pose, consumer, p.subtract(dx), ar, ag, ab, a);
        v(pose, consumer, p.add(dy), ar, ag, ab, a);
        v(pose, consumer, p.add(dx), ar, ag, ab, a);
        v(pose, consumer, p.subtract(dy), ar, ag, ab, a);
    }

    private static void v(Pose pose, VertexConsumer consumer, Vec3 at, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(r, g, b, a)
                .setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
