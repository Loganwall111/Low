package net.mcsm.extras.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.client.StormBloom;
import net.dabicco.witherstormmod.entity.state.WitherStormRenderState;
import net.dabicco.witherstormmod.mixin.RenderPipelinesAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Single state boundary for the MCSM visual systems.
 *
 * This controller does not replace the authoritative CEM/BB-model renderer.
 * It supplies the shared phase and world-space storm anchor to the procedural
 * atmosphere, the screen-space ambient pass, and the physical sun slab. The
 * existing WitherStormRenderer remains responsible for the chassis, heads,
 * tentacles, animations, teeth, and Vortex ordering.
 *
 * The post pass deliberately uses a translucent overlay instead of sampling
 * and writing the same framebuffer in one pass. That avoids an undefined GPU
 * feedback loop on drivers where the scene target is also the main target.
 * The world/entity core shaders continue to perform the actual material and
 * fog shading; this pass only supplies the final cinematic phase wash.
 */
public final class McsmCoreEngineController {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");
    private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "pipeline/mcsm_core_ambient");
    private static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "post/mcsm_core");

    private static final float MIN_PHASE = 5.0F;
    private static final float SUN_DISTANCE = 500.0F;
    private static final float SUN_HALF_WIDTH = 28.0F;
    private static final float SUN_HALF_HEIGHT = 28.0F;
    private static final float SUN_HALF_DEPTH = 0.35F;

    private static volatile float u_StormPhase;
    private static volatile float u_StormX;
    private static volatile float u_StormY;
    private static volatile float u_StormZ;
    private static volatile float stormDistanceFade;
    private static volatile float timeOfDay;
    private static volatile boolean frameHasStorm;

    private static RenderPipeline ambientPipeline;
    private static ByteBuffer uniformStaging;
    private static boolean ambientFailed;

    private McsmCoreEngineController() {
    }

    /** Reset and seed the frame's shared u_StormPhase/u_StormPos carriers. */
    public static void beginFrame(Vec3 cameraPosition) {
        u_StormPhase = 0.0F;
        u_StormX = 0.0F;
        u_StormY = 0.0F;
        u_StormZ = 0.0F;
        stormDistanceFade = 0.0F;
        frameHasStorm = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || cameraPosition == null) {
            timeOfDay = 0.0F;
            return;
        }
        timeOfDay = (float) Math.floorMod(mc.level.getOverworldClockTime(), 24000L) / 24000.0F;

        double bestDistance = Double.MAX_VALUE;
        net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData best = null;
        for (net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData storm
                : net.dabicco.witherstormmod.client.ClientDistantStormManager.all()) {
            if (storm.phase < MIN_PHASE) {
                continue;
            }
            double distance = cameraPosition.distanceToSqr(storm.dispX, storm.dispY, storm.dispZ);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = storm;
            }
        }

        if (best != null) {
            setStorm(best.phase, best.dispX, best.dispY, best.dispZ,
                    Math.sqrt(bestDistance));
        }
    }

    /** Exact renderer-state update; called when the actual boss is submitted. */
    public static void update(WitherStormRenderState state) {
        if (state == null || state.preview != null || state.phase < MIN_PHASE) {
            return;
        }
        double dx = u_StormX - state.worldX;
        double dy = u_StormY - state.worldY;
        double dz = u_StormZ - state.worldZ;
        double distance = frameHasStorm ? Math.sqrt(dx * dx + dy * dy + dz * dz) : 0.0D;
        setStorm((float) state.phase, state.worldX, state.worldY, state.worldZ, distance);
    }

    private static void setStorm(float phase, double x, double y, double z,
            double distance) {
        u_StormPhase = phase;
        u_StormX = (float) x;
        u_StormY = (float) y;
        u_StormZ = (float) z;
        stormDistanceFade = Mth.clamp(1.0F - (float) ((distance - 900.0D) / 800.0D),
                0.0F, 1.0F);
        frameHasStorm = true;
    }

    public static boolean active() {
        return frameHasStorm && u_StormPhase >= MIN_PHASE;
    }

    public static float phase() {
        return u_StormPhase;
    }

    /**
     * Retained compatibility hook for the old screen-space cinematic pass.
     * The pass is intentionally inert: it was an unbounded hidden colour
     * filter whose early-phase deck turned entities and the Wither Storm green.
     */
    public static void renderAmbientPass(CameraRenderState camera) {
        // Native entity fog, phase atlases, teeth/eye emitters, particles, and
        // the attached atmosphere remain active without a screen overlay.
    }

    /**
     * Retained registration hook for the former physical sun replacement.
     * The native celestial pass remains authoritative now; this is intentionally
     * inert so no camera-relative orange band can cover the sky.
     */
    public static void submitSunSlab(LevelRenderContext context) {
        // The replacement slab was the orange, camera-relative band visible at
        // the top of the supplied frames. Keep the native celestial/sky pass;
        // the storm's atmosphere is supplied by its attached mesh and halos.
        return;
        /*
        if (context == null || !active() || u_StormPhase < MIN_PHASE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }

        CameraRenderState camera = context.levelState().cameraRenderState;
        Vec3 lightDirection = orbitalLightDirection(mc);
        if (camera == null || camera.pos == null || lightDirection == null) {
            return;
        }

        Vec3 centre = camera.pos.add(lightDirection.scale(SUN_DISTANCE));
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        float[] colour = sunColour(u_StormPhase);
        int r = Mth.clamp((int) (colour[0] * 255.0F), 0, 255);
        int g = Mth.clamp((int) (colour[1] * 255.0F), 0, 255);
        int b = Mth.clamp((int) (colour[2] * 255.0F), 0, 255);

        poseStack.pushPose();
        poseStack.translate(centre.x, centre.y, centre.z);
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(poseStack, GlowRenderTypes.translucent(WHITE),
                (pose, consumer) -> emitSunSlab(pose, consumer, r, g, b));
        poseStack.popPose();
    }
    */
    }

    private static Vec3 orbitalLightDirection(Minecraft mc) {
        // Do not turn the replacement sun into a second, warm moon layer at
        // night.  The old opposite-vector fallback was the source of the
        // gigantic yellow band at the top of nighttime frames.  At night the
        // native continuous sky remains unoccluded; the custom daytime slab
        // simply has no submission to make.
        return net.dabicco.witherstormmod.client.StormShadow.sunDirection(mc);
    }

    private static void emitSunSlab(PoseStack.Pose pose, VertexConsumer consumer,
            int r, int g, int b) {
        float x = SUN_HALF_WIDTH;
        float y = SUN_HALF_HEIGHT;
        float z = SUN_HALF_DEPTH;
        cubeFace(pose, consumer, -x, -y, -z, x, y, -z, r, g, b);
        cubeFace(pose, consumer, x, -y, z, -x, y, z, r, g, b);
        cubeFace(pose, consumer, -x, y, -z, x, y, z, r, g, b);
        cubeFace(pose, consumer, -x, -y, z, x, -y, -z, r, g, b);
        cubeFace(pose, consumer, -x, -y, -z, -x, y, z, r, g, b);
        cubeFace(pose, consumer, x, -y, z, x, y, -z, r, g, b);
    }

    private static void cubeFace(PoseStack.Pose pose, VertexConsumer consumer,
            float x0, float y0, float z0, float x1, float y1, float z1,
            int r, int g, int b) {
        Vec3 a = new Vec3(x0, y0, z0);
        Vec3 bb = new Vec3(x1, y0, z1);
        Vec3 c = new Vec3(x1, y1, z1);
        Vec3 d = new Vec3(x0, y1, z0);
        putSunVertex(pose, consumer, a, r, g, b);
        putSunVertex(pose, consumer, bb, r, g, b);
        putSunVertex(pose, consumer, c, r, g, b);
        putSunVertex(pose, consumer, d, r, g, b);
    }

    private static void putSunVertex(PoseStack.Pose pose, VertexConsumer consumer,
            Vec3 point, int r, int g, int b) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(r, g, b, 255)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    private static float[] sunColour(float phase) {
        if (phase < 5.5F) {
            return new float[]{0x84 / 255.0F, 0x93 / 255.0F, 1.0F};
        }
        float t = Mth.clamp((phase - 5.5F) / 0.5F, 0.0F, 1.0F);
        return new float[]{Mth.lerp(t, 0x84 / 255.0F, 0xC4 / 255.0F),
                Mth.lerp(t, 0x93 / 255.0F, 0x7A / 255.0F),
                Mth.lerp(t, 1.0F, 0x5A / 255.0F)};
    }

    private static RenderPipeline ambientPipeline() {
        if (ambientPipeline == null) {
            ambientPipeline = RenderPipeline.builder(new RenderPipeline.Snippet[]{
                    RenderPipelinesAccessor.dabyws$postProcessingSnippet()})
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                    .withFragmentShader(SHADER_ID)
                    .withBindGroupLayout(BindGroupLayout.builder()
                            .withUniform("McsmCoreConfig", UniformType.UNIFORM_BUFFER)
                            .build())
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .build();
        }
        return ambientPipeline;
    }

    private static ByteBuffer staging(int bytes) {
        if (uniformStaging == null || uniformStaging.capacity() < bytes) {
            uniformStaging = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        }
        uniformStaging.clear();
        return uniformStaging;
    }
}
