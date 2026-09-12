package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.mcsm.extras.McsmExtrasConfig;
import net.minecraft.world.phys.Vec3;

/**
 * The late-stage Vortex pass.
 *
 * This class intentionally submits only the offline port of the supplied
 * Telltale Vortex.bbmodel (see McsmVortexMesh).  The old procedural dot line,
 * cubed rings, billboard diamonds, and layered funnel have been removed: they
 * were generated geometry, not the debris asset.  Phase 7+ gating is retained
 * so the real debris model does not appear during the earlier phase ladder.
 */
public final class McsmStormRings {
    private static final double MAX_DISTANCE = 2800.0D;
    private static final Identifier VORTEX_ROOT = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/mcsm_atmosphere");

    private McsmStormRings() {
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            if (!McsmExtrasConfig.stormRings) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null || ctx == null) {
                return;
            }

            Vec3 camera = ctx.levelState().cameraRenderState.pos;
            float gameTime = (float) (mc.level.getGameTime() % 240000L)
                    + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float spin = gameTime * 0.05F;
            PoseStack poseStack = ctx.poseStack();
            SubmitNodeCollector collector = ctx.submitNodeCollector();

            for (ClientDistantStormManager.StormData storm : ClientDistantStormManager.all()) {
                if (storm.phase < 7.0F) {
                    continue;
                }
                Vec3 centre = new Vec3(storm.dispX, storm.dispY, storm.dispZ);
                double distance = centre.distanceTo(camera);
                if (distance < 1.0D || distance > MAX_DISTANCE) {
                    continue;
                }

                float distanceFade = 1.0F - Mth.clamp(
                        (float) ((distance - 1500.0D) / 1300.0D), 0.0F, 1.0F);
                float strength = smoothstep(storm.phase, 7.0F, 7.35F) * distanceFade;
                if (strength <= 0.01F) {
                    continue;
                }

                double bodyRadius = bodyRadius(storm.phase);
                drawVortexMeshes(poseStack, collector, centre, bodyRadius, spin, strength);
            }
        } catch (Throwable ignored) {
            // A visual pass must disappear rather than break the render thread.
        }
    }

    private static float smoothstep(float value, float low, float high) {
        float t = Mth.clamp((value - low) / (high - low), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double bodyRadius(float phase) {
        return Math.min(340.0D, 62.0D + 46.0D * (phase - 6.0D));
    }

    /** Submit the actual BB-model mesh, preserving its textured groups. */
    private static void drawVortexMeshes(PoseStack poseStack, SubmitNodeCollector collector,
            Vec3 centre, double bodyRadius, float spin, float strength) {
        for (McsmVortexMesh.Group group : McsmVortexMesh.GROUPS) {
            boolean cubes = group.texture.contains("color_000");
            boolean backdrop = group.texture.contains("Backdrop") && !group.texture.contains("alp");
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

            Identifier texture = Identifier.fromNamespaceAndPath(
                    VORTEX_ROOT.getNamespace(), VORTEX_ROOT.getPath() + "/" + group.texture);
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
