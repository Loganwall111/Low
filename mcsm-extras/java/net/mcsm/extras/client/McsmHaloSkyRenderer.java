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
            // Phase 4 remains vanilla. The procedural overlay fades in at
            // 4.5 so the green atmosphere has a smooth, non-banded onset.
            float phaseFade = Mth.clamp((storm.phase - 4.45F) / 0.22F, 0.0F, 1.0F);
            float distanceFade = 1.0F - Mth.clamp(
                    (float) ((distance - 1500.0D) / 1300.0D), 0.0F, 1.0F);
            float visibility = phaseFade * distanceFade;
            if (visibility <= 0.004F) {
                return;
            }

            // Scale the world-attached oval from the storm distance rather than
            // clamping a camera billboard.  Close approaches therefore cannot
            // turn it into a screen-filling wall or a paper fan.
            float size = Mth.clamp((float) McsmExtrasConfig.glareSize, 0.35F, 3.05F);
            double outerAngle = Math.toRadians(storm.phase >= 6.0F ? 31.0D : 27.0D);
            double horizontal = Math.max(bodyRadius * 2.2D,
                    distance * Math.tan(outerAngle)) * (0.72D + 0.14D * size);
            // Widen the vertical centre mask enough to cover the zenith and
            // stop overworld-blue bleed above the storm. This is still the
            // existing curved world-attached mesh, not a replacement sky card.
            double vertical = horizontal * 1.35D;
            double depth = horizontal * 0.16D;
            // Keep the geometry bounded even if a config slider is set to its
            // maximum on a close camera; all colour still fades at the rim.
            horizontal = Math.min(horizontal, 720.0D);
            vertical = Math.min(vertical, 860.0D);
            depth = Math.min(depth, 116.0D);

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
            if (storm.phase < 4.45F) {
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

    /**
     * Exact cinematic phase tracks. The interpolation axis is the mesh's
     * vertical coordinate: 0 is the horizon and 1 is the zenith. Phase 4 is
     * intentionally absent because vanilla owns the atmosphere until 4.45.
     */
    private static int palette(float phase, float vertical, double radius) {
        float y = smoothstep(0.0F, 1.0F, vertical);
        int green = verticalGradient(rgb(0x6E, 0x8F, 0x73), rgb(0x17, 0x3B, 0x32), y);
        int slate = verticalGradient(rgb(0x6E, 0x78, 0x73), rgb(0x1D, 0x2B, 0x2B), y);
        int purple = verticalGradient(rgb(0x7F, 0x3A, 0xA6), rgb(0x1A, 0x0A, 0x2A), y);
        int plum = verticalGradient(rgb(0xA0, 0x75, 0x7E), rgb(0x42, 0x2E, 0x3B), y);

        int color;
        if (phase < 5.0F) {
            color = mixColor(green, slate, smoothstep(4.45F, 5.0F, phase));
        } else if (phase < 5.5F) {
            color = mixColor(slate, purple, smoothstep(5.0F, 5.5F, phase));
        } else if (phase < 6.0F) {
            color = mixColor(purple, plum, smoothstep(5.5F, 6.0F, phase));
        } else {
            color = plum;
        }

        // Keep the old soft core/rim ownership: the exact vertical track is
        // the overlay, while the center remains dark enough to hide sky bleed.
        float core = 1.0F - smoothstep(0.0F, 0.72F, (float) radius);
        if (phase >= 5.0F) {
            color = mixColor(color, rgb(0x08, 0x05, 0x10), core * 0.28F);
        }
        return color;
    }

    private static int verticalGradient(int horizon, int zenith, float y) {
        return mixColor(horizon, zenith, y);
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
