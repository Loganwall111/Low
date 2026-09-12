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
 * Procedural, world-space Wither Storm overcast.
 *
 * The renderer invokes this component from WitherStormRenderer.submit while
 * the dispatcher pose is already anchored at the entity's interpolated
 * u_StormPos. Nothing here queries the camera, a level-wide storm registry, a
 * sky renderer, or an external model file. The only parent transform added by
 * this component is the storm's body rotation and the exact 45-block offset
 * behind its primary body.
 *
 * The mesh is an ellipsoid/saucer generated directly into the native vertex
 * consumer. A neutral white carrier is used only so the existing translucent,
 * cull-disabled entity pipeline can accept vertex colour; it never supplies a
 * palette or shape. All visible colour and alpha are calculated per vertex.
 */
public final class McsmAtmosphericMeshComponent {
    private static final Identifier NEUTRAL_VERTEX_CARRIER = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    private static final float RADIUS_X = 250.0F;
    private static final float RADIUS_Z = 250.0F;
    private static final float RADIUS_Y = 80.0F;
    private static final float BEHIND_OFFSET = 45.0F;
    private static final float MAX_ALPHA = 0.80F;

    // Deliberately low-poly while retaining a clean silhouette: every cell is
    // generated procedurally as two triangles, with no OBJ/JSON/JEM geometry.
    private static final int AZIMUTH_SEGMENTS = 24;
    private static final int POLAR_SEGMENTS = 8;
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0F;

    private McsmAtmosphericMeshComponent() {
    }

    /**
     * Called at WitherStormRenderer.submit HEAD, before chassis, heads,
     * tentacles, teeth, and local debris are submitted.
     */
    public static void submit(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (state == null || poseStack == null || collector == null
                || state.preview != null || state.phase < 5.0D) {
            return;
        }

        // This is the Java-side u_StormPhase tracker for the vertex registry.
        // u_StormPos is already the renderer's active pose-stack translation;
        // adding state.worldX/Y/Z here would double-translate the entity.
        float u_StormPhase = (float) state.phase;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        if (state.bodyRoll != 0.0F) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
        }
        // In Minecraft entity-model space +Z is the body's rear. Applying this
        // after the body rotation keeps the offset attached when the boss turns.
        poseStack.translate(0.0D, 0.0D, BEHIND_OFFSET);

        // GlowRenderTypes.translucent is the mod's translucent entity render
        // type with BlendFunction.TRANSLUCENT and culling disabled. That is the
        // native equivalent of entityTranslucentCull with the requested
        // double-sided pass, so the saucer is visible from both sides.
        collector.submitCustomGeometry(poseStack,
                GlowRenderTypes.translucent(NEUTRAL_VERTEX_CARRIER),
                (pose, consumer) -> emitEllipsoid(pose, consumer, u_StormPhase));
        poseStack.popPose();
    }

    /**
     * Mathematical ellipsoid loop from the memory card:
     *
     *   X = cos(u) * sin(v) * Radius_X
     *   Z = sin(u) * sin(v) * Radius_Z
     *   Y = cos(v) * Radius_Y
     *
     * v walks pole-to-pole and u walks the complete azimuth. Each grid cell
     * becomes two triangles sent straight to VertexConsumer.
     */
    private static void emitEllipsoid(Pose pose, VertexConsumer consumer,
            float u_StormPhase) {
        for (int polar = 0; polar < POLAR_SEGMENTS; polar++) {
            float v0 = PI * polar / POLAR_SEGMENTS;
            float v1 = PI * (polar + 1) / POLAR_SEGMENTS;

            for (int azimuth = 0; azimuth < AZIMUTH_SEGMENTS; azimuth++) {
                float u0 = TWO_PI * azimuth / AZIMUTH_SEGMENTS;
                float u1 = TWO_PI * (azimuth + 1) / AZIMUTH_SEGMENTS;

                Vec3 p00 = ellipsoidPoint(u0, v0);
                Vec3 p10 = ellipsoidPoint(u1, v0);
                Vec3 p11 = ellipsoidPoint(u1, v1);
                Vec3 p01 = ellipsoidPoint(u0, v1);

                emitTriangle(pose, consumer, p00, p10, p11, u_StormPhase);
                emitTriangle(pose, consumer, p00, p11, p01, u_StormPhase);
            }
        }
    }

    private static Vec3 ellipsoidPoint(float u, float v) {
        float sinV = Mth.sin(v);
        return new Vec3(
                Mth.cos(u) * sinV * RADIUS_X,
                Mth.cos(v) * RADIUS_Y,
                Mth.sin(u) * sinV * RADIUS_Z);
    }

    private static void emitTriangle(Pose pose, VertexConsumer consumer,
            Vec3 a, Vec3 b, Vec3 c, float u_StormPhase) {
        vertex(pose, consumer, a, u_StormPhase);
        vertex(pose, consumer, b, u_StormPhase);
        vertex(pose, consumer, c, u_StormPhase);
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Vec3 position,
            float u_StormPhase) {
        float normalizedX = (float) (position.x / RADIUS_X);
        float normalizedY = (float) (position.y / RADIUS_Y);
        float normalizedZ = (float) (position.z / RADIUS_Z);
        float radial = Mth.clamp(
                Mth.sqrt(normalizedX * normalizedX + normalizedZ * normalizedZ),
                0.0F, 1.0F);
        float vertical = Mth.clamp((normalizedY + 1.0F) * 0.5F, 0.0F, 1.0F);
        int colour = phaseColour(u_StormPhase, radial, vertical);
        Vec3 normal = ellipsoidNormal(normalizedX, normalizedY, normalizedZ);

        consumer.addVertex(pose, (float) position.x, (float) position.y,
                (float) position.z)
                .setColor((colour >> 16) & 0xFF, (colour >> 8) & 0xFF,
                        colour & 0xFF, (colour >>> 24) & 0xFF)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, (float) normal.x, (float) normal.y,
                        (float) normal.z);
    }

    private static Vec3 ellipsoidNormal(float x, float y, float z) {
        // The normalized ellipsoid coordinates make this a stable outward
        // normal even at the low-poly pole vertices.
        Vec3 normal = new Vec3(x, y, z);
        return normal.lengthSqr() < 1.0E-8D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : normal.normalize();
    }

    /**
     * Non-linear quadratic smooth-step alpha. Horizontal radial perimeter
     * vertices are exactly zero; the projected center remains at 0.80.
     */
    private static int phaseColour(float u_StormPhase, float radial,
            float vertical) {
        int p5 = phase5(radial);
        int p55 = phase55(radial, vertical);
        int p6 = phase6(vertical);

        int rgb;
        if (u_StormPhase < 5.5F) {
            rgb = mix(p5, p55, smoothStep(u_StormPhase, 5.0F, 5.5F));
        } else {
            // Phase 5.5 through 5.9 stays on the purple deck; phase 6 and 7
            // transition into and retain the apocalyptic sunset deck.
            rgb = mix(p55, p6, smoothStep(u_StormPhase, 5.9F, 6.05F));
        }

        float edge = 1.0F - smoothStep(radial, 0.52F, 1.0F);
        float alpha = MAX_ALPHA * edge * edge;
        return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24)
                | (rgb & 0x00FFFFFF);
    }

    private static int phase5(float radial) {
        int core = rgb(0x0A, 0x11, 0x12);
        int mid = rgb(0x1D, 0x33, 0x35);
        int outer = rgb(0x55, 0x70, 0x61);
        return radial < 0.40F
                ? mix(core, mid, radial / 0.40F)
                : mix(mid, outer, (radial - 0.40F) / 0.60F);
    }

    private static int phase55(float radial, float vertical) {
        int core = rgb(0x05, 0x02, 0x08);
        int mid = rgb(0x2A, 0x12, 0x3D);
        int outer = rgb(0x4B, 0x1E, 0x5E);
        int horizon = rgb(0x7D, 0x4B, 0x91);
        int radialColour = radial < 0.34F
                ? mix(core, mid, radial / 0.34F)
                : radial < 0.70F
                        ? mix(mid, outer, (radial - 0.34F) / 0.36F)
                        : mix(outer, horizon, (radial - 0.70F) / 0.30F);
        return mix(radialColour, horizon, (1.0F - vertical) * 0.20F * radial);
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

    private static float smoothStep(float value, float low, float high) {
        float t = Mth.clamp((value - low) / (high - low), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int mix(int a, int b, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = Math.round(((a >> 16) & 0xFF)
                + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF)
                + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int blue = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return rgb(r, g, blue);
    }

    private static int rgb(int r, int g, int b) {
        return (Mth.clamp(r, 0, 255) << 16)
                | (Mth.clamp(g, 0, 255) << 8)
                | Mth.clamp(b, 0, 255);
    }
}
