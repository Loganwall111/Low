package net.mcsm.extras.client;

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
import net.minecraft.world.phys.Vec3;
import net.mcsm.extras.McsmExtrasConfig;

/**
 * Native render-only Halo sky pass.
 *
 * The old Halo payload is a single atmospheric dome around the live storm,
 * not a full-screen plate and not an independent black quad at the zenith.
 * This pass is submitted alongside the storm's native render graph node.  Its
 * centre is the same world-space position used for the u_StormPos carrier, so
 * the storm remains in the middle of the oval while the upper part of the
 * same mesh carries the dark core.
 *
 * There is deliberately no FabricSkyBoxes API, JSON skybox, vanilla sky
 * texture override, block, entity, persistent GPU buffer, or physical dome.
 * The gradient is vertex material colour with alpha interpolation; the native
 * translucent render pipeline provides the equivalent of linear filtering
 * between colour bands without sampling an attached sky texture.
 */
public final class McsmHaloSkyRenderer {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    private static final int SEGMENTS = 28;
    private static final int RINGS = 6;
    private static final float MAX_ALPHA = 0.80F;
    private static final double MAX_DISTANCE = 2800.0D;

    private McsmHaloSkyRenderer() {
    }

    /** Submit one nearest phase-5+ Halo mesh, fail-soft on every frame. */
    public static void submit(LevelRenderContext ctx) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || ctx == null) {
                return;
            }

            Vec3 camera = ctx.levelState().cameraRenderState.pos;
            ClientDistantStormManager.StormData storm = nearestStorm(camera);
            if (storm == null) {
                return;
            }

            Vec3 stormPos = new Vec3(storm.dispX, storm.dispY, storm.dispZ);
            Vec3 toCamera = camera.subtract(stormPos);
            double distance = toCamera.length();
            if (distance < 1.0D || distance > MAX_DISTANCE) {
                return;
            }

            Vec3 bearing = toCamera.scale(1.0D / distance);
            Vec3 upHint = Math.abs(bearing.y) > 0.985D
                    ? new Vec3(1.0D, 0.0D, 0.0D)
                    : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 right = bearing.cross(upHint).normalize();
            Vec3 up = right.cross(bearing).normalize();
            if (right.lengthSqr() < 1.0E-5D || up.lengthSqr() < 1.0E-5D) {
                return;
            }

            // Config is loaded by the client tick before render submission;
            // never perform config/file work in this geometry path.
            double bodyRadius = bodyRadius(storm.phase);
            float phaseFade = Mth.clamp((storm.phase - 4.90F) / 0.18F, 0.0F, 1.0F);
            float distanceFade = 1.0F - Mth.clamp(
                    (float) ((distance - 1500.0D) / 1300.0D), 0.0F, 1.0F);
            float visibility = phaseFade * distanceFade;
            if (visibility <= 0.004F) {
                return;
            }

            // The Halo is a world-tethered atmosphere around the storm, not a
            // distant camera plate.  Size it from the live body first so the
            // inner bands engulf the model even on a close approach; retain a
            // smaller perspective term so it remains legible at range.
            float size = Mth.clamp((float) McsmExtrasConfig.glareSize, 0.35F, 3.05F);
            double outerAngle = Math.toRadians(storm.phase >= 6.0F ? 31.0D : 27.0D);
            double horizontal = Math.max(bodyRadius * 4.8D,
                    distance * Math.tan(outerAngle) * 0.58D) * (0.92D + 0.10D * size);
            double vertical = horizontal * 0.72D;
            double depth = horizontal * 0.20D;
            // Keep the geometry bounded even if a config slider is set to its
            // maximum on a close camera. The outer ring still fades to zero
            // alpha, so enlarging the tethered Halo does not make a hard card.
            horizontal = Math.min(horizontal, 1100.0D);
            vertical = Math.min(vertical, 790.0D);
            depth = Math.min(depth, 220.0D);

            Vec3 centre = stormPos; // exact u_StormPos tether; no camera offset
            RenderType material = GlowRenderTypes.translucent(WHITE);
            PoseStack poseStack = ctx.poseStack();
            SubmitNodeCollector collector = ctx.submitNodeCollector();
            final Vec3 c = centre;
            final Vec3 r = right;
            final Vec3 u = up;
            final Vec3 b = bearing;
            final double h = horizontal;
            final double v = vertical;
            final double d = depth;
            final float phase = storm.phase;
            final float alpha = visibility * MAX_ALPHA;

            collector.submitCustomGeometry(poseStack, material,
                    (pose, consumer) -> emitOval(pose, consumer, c, r, u, b, h, v, d, phase, alpha));
        } catch (Throwable ignored) {
            // A visual pass must disappear rather than crash the render thread.
        }
    }

    private static ClientDistantStormManager.StormData nearestStorm(Vec3 camera) {
        ClientDistantStormManager.StormData best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ClientDistantStormManager.StormData storm : ClientDistantStormManager.all()) {
            if (storm.phase < 4.90F) {
                continue;
            }
            double dx = storm.dispX - camera.x;
            double dy = storm.dispY - camera.y;
            double dz = storm.dispZ - camera.z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = storm;
            }
        }
        return best;
    }

    /** Render concentric oval bands as one curved, jagged dome mesh. */
    private static void emitOval(Pose pose, VertexConsumer consumer, Vec3 centre,
            Vec3 right, Vec3 up, Vec3 bearing, double horizontal, double vertical,
            double depth, float phase, float maxAlpha) {
        double[] rings = {0.0D, 0.16D, 0.34D, 0.56D, 0.78D, 1.0D};
        for (int ring = 0; ring < RINGS - 1; ring++) {
            double inner = rings[ring];
            double outer = rings[ring + 1];
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a0 = Math.PI * 2.0D * segment / SEGMENTS;
                double a1 = Math.PI * 2.0D * (segment + 1) / SEGMENTS;
                Vec3 p00 = point(centre, right, up, bearing, horizontal, vertical, depth,
                        inner, a0, phase, segment);
                Vec3 p10 = point(centre, right, up, bearing, horizontal, vertical, depth,
                        outer, a0, phase, segment);
                Vec3 p11 = point(centre, right, up, bearing, horizontal, vertical, depth,
                        outer, a1, phase, segment + 1);
                Vec3 p01 = point(centre, right, up, bearing, horizontal, vertical, depth,
                        inner, a1, phase, segment + 1);

                put(pose, consumer, p00, phase, inner, a0, maxAlpha);
                put(pose, consumer, p10, phase, outer, a0, maxAlpha);
                put(pose, consumer, p11, phase, outer, a1, maxAlpha);
                put(pose, consumer, p01, phase, inner, a1, maxAlpha);
            }
        }
    }

    /**
     * Curved oval point with a quantized angular perturbation.  The quantized
     * perturbation is the recovered blocky/jagged border, while the six radial
     * bands keep the interior smoothly interpolated instead of fan-triangulated.
     */
    private static Vec3 point(Vec3 centre, Vec3 right, Vec3 up, Vec3 bearing,
            double horizontal, double vertical, double depth, double radius,
            double angle, float phase, int segment) {
        double jagged = 1.0D;
        if (radius > 0.70D) {
            double noise = Math.sin(segment * 17.371D + phase * 3.17D) * 0.5D + 0.5D;
            jagged = Math.floor((0.91D + noise * 0.18D) * 6.0D) / 6.0D;
        }
        double x = Math.cos(angle) * horizontal * radius * jagged;
        double y = Math.sin(angle) * vertical * radius * jagged;
        double curve = depth * (1.0D - radius * radius);
        return centre.add(right.scale(x)).add(up.scale(y)).add(bearing.scale(curve));
    }

    /** Native material colour + alpha interpolation replaces a texture filter. */
    private static void put(Pose pose, VertexConsumer consumer, Vec3 position,
            float phase, double radius, double angle, float maxAlpha) {
        float vertical = Mth.clamp((float) (Math.sin(angle) * 0.5D + 0.5D), 0.0F, 1.0F);
        int color = palette(phase, vertical, radius);
        float edge = 1.0F - smoothstep(0.48F, 1.0F, (float) radius);
        float alpha = maxAlpha * edge;
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF,
                        Mth.clamp((int) (alpha * 255.0F), 0, 255))
                .setUv(0.5F, vertical)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** Exact phase colour decks supplied for the recovered Halo payload. */
    private static int palette(float phase, float vertical, double radius) {
        int phase5 = gradient(
                rgb(0x55, 0x70, 0x61), rgb(0x1D, 0x33, 0x35), rgb(0x0A, 0x11, 0x12),
                vertical, 0.18F, 0.64F, 0.84F);
        int phase55 = gradient(
                rgb(0x4B, 0x1E, 0x5E), rgb(0x2A, 0x12, 0x3D), rgb(0x05, 0x02, 0x08),
                vertical, 0.18F, 0.64F, 0.84F);
        int phase6 = gradient6(vertical);

        float w55 = smoothstep(5.22F, 5.52F, phase);
        float w6 = smoothstep(5.88F, 6.08F, phase);
        int color = mixColor(phase5, phase55, w55);
        color = mixColor(color, phase6, w6);

        // The beam focus is a restrained native highlight near the lower
        // centre; it never becomes a separate plate or detached band.
        float focus = (float) Math.max(0.0D, 0.42D - radius) * (1.0F - vertical) * 0.32F;
        if (focus > 0.0F) {
            color = mixColor(color, rgb(0x84, 0x93, 0xFF), focus);
        }
        return color;
    }

    private static int gradient6(float vertical) {
        int bottom = rgb(0xC4, 0x7A, 0x5A);
        int lower = rgb(0x8A, 0x53, 0x61);
        int upper = rgb(0x33, 0x1C, 0x3D);
        int top = rgb(0x10, 0x0A, 0x1A);
        int color = mixColor(bottom, lower, smoothstep(0.04F, 0.30F, vertical));
        color = mixColor(color, upper, smoothstep(0.28F, 0.58F, vertical));
        return mixColor(color, top, smoothstep(0.58F, 0.94F, vertical));
    }

    private static int gradient(int lower, int middle, int top, float vertical,
            float lowerStop, float middleStop, float topStop) {
        int color = mixColor(lower, middle, smoothstep(lowerStop, middleStop, vertical));
        return mixColor(color, top, smoothstep(middleStop, topStop, vertical));
    }

    private static int mixColor(int a, int b, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int blue = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return rgb(r, g, blue);
    }

    private static int rgb(int r, int g, int b) {
        return (Mth.clamp(r, 0, 255) << 16)
                | (Mth.clamp(g, 0, 255) << 8)
                | Mth.clamp(b, 0, 255);
    }

    private static float smoothstep(float lo, float hi, float value) {
        if (hi <= lo) {
            return value >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((value - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double bodyRadius(float phase) {
        if (phase < 5.0F) {
            return 12.0D;
        }
        return phase < 6.0F
                ? 18.0D + 22.0D * (phase - 5.0F)
                : Math.min(340.0D, 40.0D + 30.0D * (phase - 6.0F));
    }
}
