package net.mcsm.extras.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.HashMap;
import java.util.Map;

import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.mixin.RenderPipelinesAccessor;
import net.dabicco.witherstormmod.mixin.RenderTypeInvoker;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcsm.extras.McsmExtrasConfig;

/**
 * MCSM 1.9.221 -- the infinite skybox blob as a JAVA rendering layer, so it
 * exists on every render path.
 *
 * Which path paints the blob depends on what is loaded:
 *   - vanilla (no Iris, no skybox mod): the core GLSL blob in the sky pass
 *     (mcsm_blob() in mcsm_visuals.glsl / sky.fsh) owns it;
 *   - FabricSkyBoxes active: McsmStormSkyLayer paints the infinite dome +
 *     blob shell over the skybox;
 *   - Iris shader pack active (the built-in v5 pack, the default): NEITHER
 *     of those runs -- that is the path the user plays on, and until now it
 *     showed only the flat dome with no blob at all. THIS class fixes that.
 *
 * The blob here is a stack of four camera-facing OVAL quads (bleed / edge /
 * mid / core) pinned at the storm position, so it sits directly behind the
 * Wither Storm, tracks it, and scales with distance like an infinite skybox
 * layer. Each quad is tinted with the exact corrected 2026-09-11 hexes and
 * shaped by core/mcsm_blob_oval.fsh, which turns the quad into a perfectly
 * soft radial smudge with smoothstep falloff -- fully procedural, no texture
 * pixels, so there is nothing to filter (GL_LINEAR-quality by construction).
 *
 * Blending is ALPHA (BlendFunction.TRANSLUCENT): the dark core layer is
 * near-opaque and physically darkens the sky behind it (opaque cinematic
 * mass), while the outer layers are translucent and bleed the smudge into
 * the dome. The pipeline is the fogless entity-emissive path, so NO vanilla
 * distance fog (linear or exponential) ever touches the blob. The depth
 * test keeps terrain and the storm body in front of the backdrop.
 */
public final class McsmBlobOval {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    /** layer radii as fractions of the outer angular radius (mirrors the GLSL
     *  stops: core 0.42 / mid 0.95 / edge 1.32 / bleed 1.85) */
    private static final double CORE_R = 0.42;
    private static final double MID_R = 0.95;
    private static final double EDGE_R = 1.32;
    private static final double BLEED_R = 1.85;

    /** layer alphas (pre-distance-fade): opaque core -> translucent bleed */
    private static final float CORE_A = 0.96F;
    private static final float MID_A = 0.80F;
    private static final float EDGE_A = 0.55F;
    private static final float BLEED_A = 0.28F;

    /** the oval: 1.55 wide x 0.90 tall, tilted 0.18 rad (same as the GLSL blob) */
    private static final double OVAL_X = 1.55;
    private static final double OVAL_Y = 0.90;
    private static final double TILT = 0.18;

    private static final double FULL_RANGE = 700.0;
    private static final double MAX_RANGE = 1600.0;

    private static final Map<Identifier, RenderType> TYPES = new HashMap<>();

    private McsmBlobOval() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
    }

    private static float[] hex(int r, int g, int b) {
        return new float[]{r / 255.0F, g / 255.0F, b / 255.0F};
    }

    // corrected 2026-09-11 hex decks
    private static final float[] P5_CORE = hex(0x16, 0x1A, 0x1D);
    private static final float[] P5_MID = hex(0x2D, 0x42, 0x3F);
    private static final float[] P5_EDGE = hex(0x6A, 0x9A, 0x78);
    private static final float[] P55_CORE = hex(0x0B, 0x04, 0x10);
    private static final float[] P55_MID = hex(0x2D, 0x14, 0x42);
    private static final float[] P55_HIGH = hex(0x58, 0x1C, 0x6E);
    private static final float[] P55_EDGE = hex(0x87, 0x52, 0x9C);
    private static final float[] P6_TOP = hex(0x1A, 0x12, 0x26);
    private static final float[] P6_UMID = hex(0x46, 0x2A, 0x52);
    private static final float[] P6_LMID = hex(0x96, 0x61, 0x73);
    private static final float[] P6_BOT = hex(0xD8, 0x98, 0x74);

    private static float ramp(float v, float lo, float hi) {
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float ss(float lo, float hi, float v) {
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float[] blend(float[] a, float[] b, float[] c, float wa, float wb, float wc, float tot) {
        return new float[]{
                (a[0] * wa + b[0] * wb + c[0] * wc) / tot,
                (a[1] * wa + b[1] * wb + c[1] * wc) / tot,
                (a[2] * wa + b[2] * wb + c[2] * wc) / tot};
    }

    private static boolean modLoaded(String id) {
        try {
            Class<?> loaderCls = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderCls.getMethod("getInstance").invoke(null);
            Object ans = loaderCls.getMethod("isModLoaded", String.class).invoke(loader, id);
            return Boolean.TRUE.equals(ans);
        } catch (Throwable t) {
            return false;
        }
    }

    /** the alpha-blended, fogless, no-cull, depth-tested oval pipeline */
    private static RenderType blobType() {
        return TYPES.computeIfAbsent(WHITE, t -> {
            RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelinesAccessor.dabyws$entityEmissiveSnippet()})
                    .withLocation(id("pipeline/mcsm_blob_oval"))
                    .withVertexShader(id("core/fogless_entity"))
                    .withFragmentShader(id("core/mcsm_blob_oval"))
                    .withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withCull(false)
                    .build();
            return RenderTypeInvoker.dabyws$create("dabywitherstormmod:mcsm_blob_oval:" + t,
                    RenderSetup.builder(p).withTexture("Sampler0", t).createRenderSetup());
        });
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            // Only where no full-sky blob exists yet: the vanilla path is the
            // core GLSL blob, the FBS path is McsmStormSkyLayer, so this runs
            // on the Iris shader-pack path (the built-in pack).
            if (fabricSkyboxesOwnSky()) {
                return;
            }
            if (!modLoaded("iris") && !modLoaded("optifine") && !modLoaded("optifabric") && !modLoaded("canvas")) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null || mc.player == null) {
                return;
            }
            StormData best = null;
            double bestD = Double.MAX_VALUE;
            Vec3 cam = ctx.levelState().cameraRenderState.pos;
            for (StormData d : ClientDistantStormManager.all()) {
                if (d.phase < 5.0F || d.phase > 6.95F) {
                    continue;
                }
                double dx = d.dispX - cam.x, dy = d.dispY - cam.y, dz = d.dispZ - cam.z;
                double dd = dx * dx + dy * dy + dz * dz;
                if (dd < bestD) {
                    bestD = dd;
                    best = d;
                }
            }
            if (best == null || bestD > MAX_RANGE * MAX_RANGE) {
                return;
            }
            double dist = Math.sqrt(bestD);
            float presence = 1.0F - ss((float) FULL_RANGE, (float) MAX_RANGE, (float) dist);
            if (presence <= 0.005F) {
                return;
            }
            float phase = best.phase;

            // phase windows (same as the GLSL blob): 5 / 5.5-5.9 / 6
            float w5 = ramp(phase, 5.00F, 5.10F) * (1.0F - ramp(phase, 5.42F, 5.52F));
            float w55 = ramp(phase, 5.42F, 5.52F) * (1.0F - ramp(phase, 5.92F, 6.08F));
            float w6 = ramp(phase, 5.92F, 6.08F) * (1.0F - ramp(phase, 6.95F, 7.05F));
            float tot = w5 + w55 + w6;
            if (tot < 0.02F) {
                return;
            }
            // per-layer palettes, lerped across the phase windows; the phase-6
            // four-colour split is approximated vertically (dark zenith cap on
            // the core, dusty rose mid, amber bottom on the bleed).
            final float[] core = blend(P5_CORE, P55_CORE, P6_TOP, w5, w55, w6, tot);
            final float[] mid = blend(P5_MID, P55_MID, P6_UMID, w5, w55, w6, tot);
            final float[] edge = blend(P5_EDGE, P55_EDGE, mix(P6_LMID, P6_BOT, 0.4F), w5, w55, w6, tot);
            final float[] bleed = blend(P5_EDGE, P55_EDGE, P6_BOT, w5, w55, w6, tot);
            // 5.5-5.9: the high-altitude royal magenta pushes into the edge
            for (int i = 0; i < 3; i++) {
                edge[i] += (P55_HIGH[i] - edge[i]) * (w55 / tot) * 0.35F;
            }

            McsmExtrasConfig.load();
            double gs = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
            double outer = (52.0 + 24.0 * ramp(phase, 5.0F, 6.0F))
                    * (0.80 + (gs - 0.25) * 0.196);

            final Vec3 anchor = new Vec3(best.dispX, best.dispY, best.dispZ);
            Vec3 rawView = anchor.subtract(cam);
            if (rawView.lengthSqr() < 1.0E-4D) {
                return;
            }
            final Vec3 view = rawView.normalize();
            // screen-space frame, oval squash + tilt
            Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 right = view.cross(upHint).normalize();
            Vec3 up = right.cross(view).normalize();
            double ct = Math.cos(TILT), st = Math.sin(TILT);
            final Vec3 rx = right.scale(ct).add(up.scale(st)).scale(OVAL_X);
            final Vec3 uy = up.scale(ct).subtract(right.scale(st)).scale(OVAL_Y);

            // world half-extent of the outer radius at the storm's distance
            final double half = Math.tan(Math.toRadians(outer) * 0.5) * dist;

            final int aCore = (int) (255.0F * CORE_A * presence);
            final int aMid = (int) (255.0F * MID_A * presence);
            final int aEdge = (int) (255.0F * EDGE_A * presence);
            final int aBleed = (int) (255.0F * BLEED_A * presence);

            SubmitNodeCollector collector = ctx.submitNodeCollector();
            collector.submitCustomGeometry(ctx.poseStack(), blobType(), (pose, consumer) -> {
                // largest first, core on top: the alpha stack builds the smudge
                oval(pose, consumer, anchor, rx, uy, half * BLEED_R, bleed, aBleed);
                oval(pose, consumer, anchor, rx, uy, half * EDGE_R, edge, aEdge);
                oval(pose, consumer, anchor, rx, uy, half * MID_R, mid, aMid);
                oval(pose, consumer, anchor, rx, uy, half * CORE_R, core, aCore);
            });
        } catch (Throwable ignored) {
            // the blob must never break a frame
        }
    }

    private static boolean fabricSkyboxesOwnSky() {
        return modLoaded("fabricskyboxes") && DabyWSClientConfig.customSkyboxes;
    }

    private static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    /** one tilted oval quad: anchor +/- rx*hw +/- uy*hh, full UV disc */
    private static void oval(Pose pose, VertexConsumer consumer, Vec3 at,
                             Vec3 rx, Vec3 uy, double halfR,
                             float[] rgb, int alpha) {
        if (alpha <= 2 || halfR <= 0.0) {
            return;
        }
        int r = (int) (rgb[0] * 255.0F), g = (int) (rgb[1] * 255.0F), b = (int) (rgb[2] * 255.0F);
        Vec3 xw = rx.scale(halfR);
        Vec3 yh = uy.scale(halfR);
        vtx(pose, consumer, at.subtract(xw).subtract(yh), 0.0F, 0.0F, r, g, b, alpha);
        vtx(pose, consumer, at.add(xw).subtract(yh), 1.0F, 0.0F, r, g, b, alpha);
        vtx(pose, consumer, at.add(xw).add(yh), 1.0F, 1.0F, r, g, b, alpha);
        vtx(pose, consumer, at.subtract(xw).add(yh), 0.0F, 1.0F, r, g, b, alpha);
    }

    private static void vtx(Pose pose, VertexConsumer consumer, Vec3 at,
                            float u, float v, int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
