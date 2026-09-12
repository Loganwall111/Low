package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcsm.extras.McsmExtrasConfig;

import java.util.Locale;

/**
 * The attached late-stage Vortex pass.
 *
 * This is called from WitherStormRenderer after the entity's ordinary model
 * and local debris have been submitted. It contains only the offline port of
 * the supplied Telltale Vortex.bbmodel. The former level-wide dotted line,
 * cubed rings, billboard diamonds, and generated funnel are gone.
 */
public final class McsmAttachedVortex {
    private static final Identifier VORTEX_ROOT = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/mcsm_atmosphere");

    private McsmAttachedVortex() {
    }

    /** Phase-7+ Vortex, parented to the same entity pose as the storm. */
    public static void submit(WitherStormRenderState state, PoseStack poseStack,
            SubmitNodeCollector collector) {
        if (state == null || poseStack == null || collector == null
                || !McsmExtrasConfig.stormRings || state.preview != null || state.phase < 7.0D) {
            return;
        }

        double bodyRadius = Math.min(340.0D, 62.0D + 46.0D * (state.phase - 6.0D));
        // The base submit has restored its entry pose by TAIL. Reapply the
        // exact body yaw used by the ordinary renderer so the supplied Vortex
        // travels and rotates with the same world-space parent.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        if (state.bodyRoll != 0.0F) {
            poseStack.mulPose(Axis.ZN.rotationDegrees(state.bodyRoll));
        }
        drawVortexMeshes(poseStack, collector, new Vec3(0.0D, 0.0D, 0.0D),
                bodyRadius, state.idleTimeTicks * 0.05F, 1.0F);
        poseStack.popPose();
    }

    /** Submit the real BB-model mesh while preserving its textured groups. */
    private static void drawVortexMeshes(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 centre, double bodyRadius, float spin, float strength) {
        for (McsmVortexMesh.Group group : McsmVortexMesh.GROUPS) {
            String textureName = group.texture.toLowerCase(Locale.ROOT);
            boolean cubes = textureName.contains("color_000");
            boolean backdrop = textureName.contains("backdrop") && !textureName.contains("alp");
            double scale = bodyRadius * (backdrop ? 3.60D : 3.30D);
            if (!cubes) {
                scale *= 0.55D + 0.45D * strength;
            }
            double angularSpeed = cubes ? 0.070D : (backdrop ? 0.045D : -0.055D);
            float spinF = (float) (spin * angularSpeed / 0.05D);
            float scaleF = (float) scale;
            float alpha = strength * (cubes ? 0.82F : (backdrop ? 0.58F : 0.36F));
            if (alpha <= 0.01F) {
                continue;
            }

            Identifier texture = safeTextureIdentifier(textureName);
            if (texture == null) {
                // Do not let a stale/generated material name take down phase 7.
                continue;
            }
            final McsmVortexMesh.Group mesh = group;
            final Identifier finalTexture = texture;
            final float finalScale = scaleF;
            final float finalSpin = spinF;
            final int finalAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
            collector.submitCustomGeometry(poseStack, GlowRenderTypes.glow(finalTexture),
                    (pose, consumer) -> emit(mesh, centre, pose, consumer,
                            finalScale, finalSpin, finalAlpha));
        }
    }

    /**
     * Identifier paths are stricter than ordinary filenames. Keep this guard
     * beside the real mesh submission so stale/generated uppercase material
     * labels fail soft instead of crashing the phase-7 render thread.
     */
    private static Identifier safeTextureIdentifier(String textureName) {
        if (textureName == null || textureName.isEmpty()) {
            return null;
        }
        String path = VORTEX_ROOT.getPath() + "/" + textureName;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '/' || c == '.' || c == '_' || c == '-')) {
                System.out.println("[dabywitherstormmod] skipped invalid Vortex texture path: " + path);
                return null;
            }
        }
        return Identifier.fromNamespaceAndPath(VORTEX_ROOT.getNamespace(), path);
    }

    private static void emit(McsmVortexMesh.Group mesh, Vec3 centre,
            PoseStack.Pose pose, VertexConsumer consumer, float scale, float spin, int alpha) {
        double cos = Math.cos(spin);
        double sin = Math.sin(spin);
        float[] positions = mesh.pos;
        float[] uv = mesh.uv;
        for (int i = 0; i < mesh.idx.length; i++) {
            int positionIndex = mesh.idx[i] * 3;
            int uvIndex = mesh.idx[i] * 2;
            float x = positions[positionIndex];
            float y = positions[positionIndex + 1];
            float z = positions[positionIndex + 2];
            double worldX = (x * cos - z * sin) * scale;
            double worldY = y * scale;
            double worldZ = (x * sin + z * cos) * scale;
            consumer.addVertex(pose, (float) (centre.x + worldX),
                    (float) (centre.y + worldY), (float) (centre.z + worldZ))
                    .setColor(255, 255, 255, alpha)
                    .setUv(uv[uvIndex], uv[uvIndex + 1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(15728880)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }
}
