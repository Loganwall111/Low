package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * World-attached Wither Storm overcast.
 *
 * This is deliberately an entity render component, not a sky pass. The
 * renderer calls it while the Wither Storm's own pose stack is still active,
 * so the backdrop inherits the entity translation, rotation, interpolation,
 * and scale exactly once. No camera position, level render event, SkyRenderer,
 * skybox resource, or screen-facing billboard is involved.
 *
 * The mesh is the original curved, low-poly 3D wall behind the body. Its
 * footprint is phase-sized: close to the storm at Phase 5.5, then larger at
 * Phases 6 and 7. It is a render-only translucent object with no collision, so
 * the camera and clouds can pass through it instead of revealing a distant
 * back face. The outer margin reaches zero alpha, allowing the ordinary world
 * and clouds to show through its soft edge.
 */
public final class McsmAtmosphericMeshComponent {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    private static final int COLUMNS = 28;
    private static final int ROWS = 12;
    /** Close attached oval; it grows with the storm rather than becoming a far sky card. */
    private static final float BEHIND_OFFSET = 36.0F;
    private static final float MAX_ALPHA = 0.72F;
    private static final double START_PHASE = 4.45D;

    private McsmAtmosphericMeshComponent() {
    }

    /**
     * Called from WitherStormRenderer.submit at HEAD, before the chassis,
     * heads, tentacles, and ordinary local debris are submitted.
     */
    public static void submit(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (state == null || poseStack == null || collector == null
                || state.preview != null || state.phase < START_PHASE) {
            return;
        }

        // Keep the phase input explicit: this is the Java-side u_StormPhase
        // carrier for the dynamic vertex palette, not a camera or level query.
        float u_StormPhase = (float) state.phase;
        float phaseFade = smoothstep(u_StormPhase, 4.45F, 4.68F);
        float phase = u_StormPhase;
        if (phaseFade <= 0.004F) {
            return;
        }

        // Match the renderer's body orientation. The pose stack already owns
        // the entity's world-space translation; this local rotation keeps the
        // atmosphere parented to the same yaw as the boss rather than to the
        // camera bearing.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        if (state.bodyRoll != 0.0F) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
        }

        final float finalPhase = phase;
        final float finalFade = phaseFade;
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                (pose, consumer) -> emitBackdrop(pose, consumer, finalPhase, finalFade));
        poseStack.popPose();
    }

    private static void emitBackdrop(Pose pose, VertexConsumer consumer,
            float phase, float phaseFade) {
        // Keep the original curved-wall construction, but size it from the
        // actual phase. At 5.5 the top is about 10 blocks above the body;
        // Phases 6 and 7 deliberately grow the oval instead of moving it away.
        double radius = bodyRadius(phase);
        double widthFactor = phase < 5.0F ? 1.65D
                : (phase < 5.5F ? 1.80D
                : (phase < 6.0F ? 2.00D
                : (phase < 7.0F ? 2.20D : 2.35D)));
        double halfWidth = radius * widthFactor;
        double extraTop = phase < 5.5F ? 8.0D : 10.0D + Math.max(0.0D, phase - 5.5D) * 20.0D;
        double halfHeight = radius + extraTop;
        double depth = Math.min(48.0D, halfWidth * 0.34D);
        double behind = Math.min(BEHIND_OFFSET, Math.max(18.0D, radius * 0.85D));

        for (int row = 0; row < ROWS; row++) {
            double y0 = -1.0D + 2.0D * row / ROWS;
            double y1 = -1.0D + 2.0D * (row + 1) / ROWS;
            for (int column = 0; column < COLUMNS; column++) {
                double x0 = -1.0D + 2.0D * column / COLUMNS;
                double x1 = -1.0D + 2.0D * (column + 1) / COLUMNS;
                Vec3 a = point(x0, y0, halfWidth, halfHeight, behind, depth);
                Vec3 b = point(x1, y0, halfWidth, halfHeight, behind, depth);
                Vec3 c = point(x1, y1, halfWidth, halfHeight, behind, depth);
                Vec3 d = point(x0, y1, halfWidth, halfHeight, behind, depth);

                putQuad(pose, consumer, phase, phaseFade, a, b, c, d, x0, x1, y0, y1);
            }
        }
    }

    /** A convex-in-depth, horizontally stretched atmospheric wall. */
    private static Vec3 point(double x, double y, double halfWidth, double halfHeight,
            double behind, double depth) {
        double horizontalCurve = 1.0D - x * x;
        double verticalCurve = 0.78D + 0.22D * (1.0D - y * y);
        double z = behind + depth * horizontalCurve * verticalCurve;
        return new Vec3(x * halfWidth, y * halfHeight, z);
    }

    private static void putQuad(Pose pose, VertexConsumer consumer, float phase,
            float phaseFade, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            double x0, double x1, double y0, double y1) {
        int ca = colour(phase, x0, y0, phaseFade);
        int cb = colour(phase, x1, y0, phaseFade);
        int cc = colour(phase, x1, y1, phaseFade);
        int cd = colour(phase, x0, y1, phaseFade);
        Vec3 normal = normal(c.subtract(a).cross(b.subtract(a)));

        vertex(pose, consumer, a, 0.0F, 1.0F, ca, normal);
        vertex(pose, consumer, b, 1.0F, 1.0F, cb, normal);
        vertex(pose, consumer, c, 1.0F, 0.0F, cc, normal);
        vertex(pose, consumer, d, 0.0F, 0.0F, cd, normal);
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Vec3 at,
            float u, float v, int colour, Vec3 normal) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor((colour >> 16) & 0xFF, (colour >> 8) & 0xFF,
                        colour & 0xFF, (colour >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    /** Exact phase decks: p5, p5.5-5.9, and p6+. */
    private static int colour(float phase, double x, double y, float phaseFade) {
        float radius = Mth.clamp((float) Math.sqrt(x * x + y * y), 0.0F, 1.0F);
        float vertical = Mth.clamp((float) ((y + 1.0D) * 0.5D), 0.0F, 1.0F);
        int p45 = phase45(radius);
        int p5 = phase5(radius);
        int p55 = phase55(radius, vertical);
        int p6 = phase6(vertical);

        int rgb;
        if (phase < 5.0F) {
            // Bring back the green backdrop only as a local atmosphere around
            // the storm. The body texture itself is never tinted green.
            rgb = mix(p45, p5, smoothstep(phase, 4.82F, 5.0F));
        } else if (phase < 5.5F) {
            rgb = mix(p5, p55, smoothstep(phase, 5.0F, 5.5F));
        } else {
            rgb = mix(p55, p6, smoothstep(phase, 5.9F, 6.05F));
        }

        float outerFade = 1.0F - smoothstep(radius, 0.68F, 1.0F);
        // Phase 5's black core is intentionally dark but not opaque: the
        // cloud layer and the storm silhouette remain visible through it.
        float corePass = phase >= 5.0F
                ? 0.22F + 0.78F * smoothstep(radius, 0.0F, 0.42F)
                : 1.0F;
        float alpha = MAX_ALPHA * phaseFade * outerFade * corePass;
        return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int phase45(float radius) {
        int core = rgb(0x06, 0x0E, 0x12);
        int mid = rgb(0x12, 0x2A, 0x2B);
        int fringe = rgb(0x35, 0x5E, 0x4B);
        return radius < 0.40F
                ? mix(core, mid, radius / 0.40F)
                : mix(mid, fringe, (radius - 0.40F) / 0.60F);
    }

    private static int phase5(float radius) {
        // Phase 5 is purple with a dark moon-blue outer halo and an almost
        // black centre; the green phase-4.5 backdrop ends before this deck.
        int core = rgb(0x03, 0x02, 0x0D);
        int mid = rgb(0x11, 0x0E, 0x2A);
        int fringe = rgb(0x1D, 0x2D, 0x5A);
        return radius < 0.40F
                ? mix(core, mid, radius / 0.40F)
                : mix(mid, fringe, (radius - 0.40F) / 0.60F);
    }

    private static int phase55(float radius, float vertical) {
        int core = rgb(0x05, 0x02, 0x08);
        int mid = rgb(0x2A, 0x12, 0x3D);
        int fringe = rgb(0x4B, 0x1E, 0x5E);
        int horizon = rgb(0x7D, 0x4B, 0x91);
        int radial = radius < 0.34F
                ? mix(core, mid, radius / 0.34F)
                : radius < 0.70F
                        ? mix(mid, fringe, (radius - 0.34F) / 0.36F)
                        : mix(fringe, horizon, (radius - 0.70F) / 0.30F);
        // The horizon bleed is strongest below the center without replacing
        // the radial dark core.
        return mix(radial, horizon, (1.0F - vertical) * 0.20F * radius);
    }

    private static int phase6(float vertical) {
        int zenith = rgb(0x10, 0x0A, 0x1A);
        int upper = rgb(0x33, 0x1C, 0x3D);
        int lower = rgb(0x8A, 0x53, 0x61);
        int horizon = rgb(0xC4, 0x7A, 0x5A);
        if (vertical > 0.68F) {
            return mix(upper, zenith, (vertical - 0.68F) / 0.32F);
        }
        if (vertical > 0.30F) {
            return mix(lower, upper, (vertical - 0.30F) / 0.38F);
        }
        return mix(horizon, lower, vertical / 0.30F);
    }

    private static double bodyRadius(float phase) {
        if (phase < 5.0F) {
            return 25.0D + 20.0D * Math.max(0.0D, phase - 4.45D);
        }
        if (phase < 6.0F) {
            return 36.0D + 26.0D * (phase - 5.0D);
        }
        return Math.min(340.0D, 40.0D + 30.0D * (phase - 6.0D));
    }

    private static float smoothstep(float value, float low, float high) {
        float t = Mth.clamp((value - low) / (high - low), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int mix(int a, int b, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bBlue = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return rgb(r, g, bBlue);
    }

    private static int rgb(int r, int g, int b) {
        return (Mth.clamp(r, 0, 255) << 16)
                | (Mth.clamp(g, 0, 255) << 8)
                | Mth.clamp(b, 0, 255);
    }

    private static Vec3 normal(Vec3 value) {
        return value.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : value.normalize();
    }
}
