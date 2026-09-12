package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.mcsm.extras.McsmExtrasConfig;

import java.lang.reflect.Method;

/**
 * Opt-in client presentation of the small Telltale-style studio stage.
 *
 * This renderer is intentionally a visual stage, not a world replacement:
 * enabling it never deletes blocks, edits a save, asks a server to stop
 * generating chunks, or installs a fake block/entity for the dome.  Natural
 * terrain is visually occluded at the stage perimeter and the custom shell
 * supplies the flat underside/black outside view.  That is the only safe
 * client-side interpretation of "no chunks outside the pad" on multiplayer.
 *
 * The dome is submitted through the 26.2 level render graph.  Its render type
 * has culling disabled, so the same low-poly shell is visible from inside and
 * outside; this is the render-graph equivalent of disabling GL culling and
 * avoids direct global GL state changes that can corrupt later entity passes.
 */
public final class McsmExperimentalStoryStage {
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(
            "dabywitherstormmod", "textures/misc/storm_white.png");

    /** A 16 by 16 chunk presentation boundary centered on the world spawn. */
    public static final int STAGE_CHUNK_RADIUS = 8;
    public static final double PAD_HALF_SIZE = STAGE_CHUNK_RADIUS * 16.0D;
    public static final double PAD_FLOOR_Y = 63.0D;
    public static final double DOME_CENTER_Y = 64.0D;
    public static final double DOME_TOP_Y = 400.0D;
    public static final double DOME_RADIUS_Y = DOME_TOP_Y - DOME_CENTER_Y;
    public static final double DOME_RADIUS_XZ = 320.0D;
    // Deliberately low-poly: roughly 27 KiB of transient vertices, never a
    // persistent mesh or a world object.
    private static final int DOME_SEGMENTS = 24;
    private static final int DOME_RINGS = 10;
    private static final int[] PROP_X = {-1, 0, 1, 1, 1, 0, -1, -1};
    private static final int[] PROP_Z = {-1, -1, -1, 0, 1, 1, 1, 0};
    private static volatile ClientLevel cachedSpawnLevel;
    private static volatile BlockPos cachedSpawn;

    private McsmExperimentalStoryStage() {
    }

    public static boolean active() {
        // The client tick owns config loading. This predicate is called from
        // the render graph, so it must remain a pure in-memory gate.
        return McsmExtrasConfig.ENABLE_EXPERIMENTAL_STORY_MODE_STAGE;
    }

    /** The camera is in the black exterior once it crosses the ellipsoid. */
    public static boolean cameraOutside(ClientLevel level, Vec3 camera) {
        if (!active() || level == null || camera == null) {
            return false;
        }
        Vec3 c = stageCenter(level);
        double x = (camera.x - c.x) / DOME_RADIUS_XZ;
        double y = (camera.y - DOME_CENTER_Y) / DOME_RADIUS_Y;
        double z = (camera.z - c.z) / DOME_RADIUS_XZ;
        return x * x + y * y + z * z > 1.0D;
    }

    /**
     * Client-side chunk policy used by render hooks and diagnostics.  It does
     * not return a fake ChunkAccess and therefore cannot poison block/entity
     * lookups or destabilize multiplayer.
     */
    public static boolean shouldRenderChunk(int chunkX, int chunkZ, ClientLevel level) {
        if (!active() || level == null) {
            return true;
        }
        BlockPos spawn = stageSpawn(level);
        int sx = Math.floorDiv(spawn.getX(), 16);
        int sz = Math.floorDiv(spawn.getZ(), 16);
        return chunkX >= sx - STAGE_CHUNK_RADIUS && chunkX < sx + STAGE_CHUNK_RADIUS
                && chunkZ >= sz - STAGE_CHUNK_RADIUS && chunkZ < sz + STAGE_CHUNK_RADIUS;
    }

    /** Submit the dome, flat stage underside, edge walls, and paper props. */
    public static void submit(LevelRenderContext ctx) {
        if (!active() || ctx == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc == null ? null : mc.level;
        if (level == null) {
            return;
        }

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        Vec3 center = stageCenter(level);
        Vec3 camera = ctx.levelState().cameraRenderState.pos;
        float phase = stagePhase();

        RenderType shell = GlowRenderTypes.translucent(WHITE);
        submitDome(poseStack, collector, shell, center, phase, cameraOutside(level, camera));
        submitPadAndEdge(poseStack, collector, shell, center, cameraOutside(level, camera));
        submitPaperMountains(poseStack, collector, shell, center, camera, phase);
    }

    private static float stagePhase() {
        float phase = McsmNativeSkyRenderer.stormPhase();
        if (!(phase >= 5.0F)) {
            return 5.0F;
        }
        return Mth.clamp(phase, 5.0F, 7.0F);
    }

    /** Horizon stop for native fog; kept at the same exact stage ramp. */
    public static void horizonColor(float phase, float[] out) {
        if (out == null || out.length < 3) {
            return;
        }
        int color = palette(Mth.clamp(phase, 5.0F, 7.0F), 0.0F, 255.0F);
        out[0] = ((color >> 16) & 0xFF) / 255.0F;
        out[1] = ((color >> 8) & 0xFF) / 255.0F;
        out[2] = (color & 0xFF) / 255.0F;
    }

    private static Vec3 stageCenter(ClientLevel level) {
        BlockPos p = stageSpawn(level);
        return new Vec3(p.getX() + 0.5D, DOME_CENTER_Y, p.getZ() + 0.5D);
    }

    /** Reflection keeps the extras overlay compatible with both 26.2 spawn APIs. */
    private static BlockPos stageSpawn(ClientLevel level) {
        BlockPos known = cachedSpawn;
        if (level == cachedSpawnLevel && known != null) {
            return known;
        }
        synchronized (McsmExperimentalStoryStage.class) {
            known = cachedSpawn;
            if (level == cachedSpawnLevel && known != null) {
                return known;
            }
            BlockPos resolved = new BlockPos(0, 64, 0);
            try {
                Object value = invokeNoArg(level, "getSharedSpawnPos");
                if (value instanceof BlockPos pos) {
                    resolved = pos;
                } else {
                    Object respawn = invokeNoArg(level, "getRespawnData");
                    if (respawn != null) {
                        Object pos = invokeNoArg(respawn, "pos");
                        if (pos instanceof BlockPos blockPos) {
                            resolved = blockPos;
                        }
                    }
                    Object data = invokeNoArg(level, "getLevelData");
                    if (data != null) {
                        Object x = invokeNoArg(data, "getXSpawn");
                        Object z = invokeNoArg(data, "getZSpawn");
                        if (x instanceof Number nx && z instanceof Number nz) {
                            resolved = new BlockPos(nx.intValue(), 64, nz.intValue());
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            cachedSpawnLevel = level;
            cachedSpawn = resolved;
            return resolved;
        }
    }

    private static Object invokeNoArg(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        for (Method method : owner.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                try {
                    return method.invoke(owner);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static void submitDome(PoseStack poseStack, SubmitNodeCollector collector,
            RenderType type, Vec3 center, float phase, boolean outside) {
        // Keep the stage atmosphere at the same 0.80 ceiling as the native
        // storm path; the black exterior supplies contrast, not an opaque
        // overlay.
        final float alpha = outside ? 204.0F : 196.0F;
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            for (int iy = 0; iy < DOME_RINGS; iy++) {
                double v0 = (double) iy / DOME_RINGS;
                double v1 = (double) (iy + 1) / DOME_RINGS;
                double p0 = -Math.PI * 0.5D + Math.PI * v0;
                double p1 = -Math.PI * 0.5D + Math.PI * v1;
                for (int ix = 0; ix < DOME_SEGMENTS; ix++) {
                    double u0 = 2.0D * Math.PI * ix / DOME_SEGMENTS;
                    double u1 = 2.0D * Math.PI * (ix + 1) / DOME_SEGMENTS;
                    Vec3 a = domePoint(center, p0, u0);
                    Vec3 b = domePoint(center, p0, u1);
                    Vec3 c = domePoint(center, p1, u1);
                    Vec3 d = domePoint(center, p1, u0);
                    domeQuad(pose, consumer, phase, a, b, c, d, alpha);
                }
            }
        });
    }

    private static Vec3 domePoint(Vec3 center, double pitch, double yaw) {
        double cp = Math.cos(pitch);
        return center.add(
                Math.cos(yaw) * cp * DOME_RADIUS_XZ,
                Math.sin(pitch) * DOME_RADIUS_Y,
                Math.sin(yaw) * cp * DOME_RADIUS_XZ);
    }

    private static void domeQuad(Pose pose, VertexConsumer consumer, float phase,
            Vec3 a, Vec3 b, Vec3 c, Vec3 d, float alpha) {
        float va = vertical(a.y);
        float vb = vertical(b.y);
        float vc = vertical(c.y);
        float vd = vertical(d.y);
        vertex(pose, consumer, a, 0.0F, 1.0F, palette(phase, va, alpha), normal(a));
        vertex(pose, consumer, b, 1.0F, 1.0F, palette(phase, vb, alpha), normal(b));
        vertex(pose, consumer, c, 1.0F, 0.0F, palette(phase, vc, alpha), normal(c));
        vertex(pose, consumer, d, 0.0F, 0.0F, palette(phase, vd, alpha), normal(d));
    }

    private static float vertical(double y) {
        return Mth.clamp((float) ((y - (DOME_CENTER_Y - DOME_RADIUS_Y))
                / (DOME_RADIUS_Y * 2.0D)), 0.0F, 1.0F);
    }

    /** Exact phase decks requested for the experimental stage shell. */
    private static int palette(float phase, float vertical, float alpha) {
        int p5Top = rgb(0x10, 0x16, 0x17);
        int p5Mid = rgb(0x1D, 0x33, 0x35);
        int p5Hor = rgb(0x55, 0x70, 0x61);
        int p55Top = rgb(0x05, 0x02, 0x08);
        int p55Mid = rgb(0x2A, 0x12, 0x3D);
        int p55Hor = rgb(0x4B, 0x1E, 0x5E);
        int p6Top = rgb(0x10, 0x0A, 0x1A);
        int p6Mid = rgb(0x33, 0x1C, 0x3D);
        int p6Hor = rgb(0xC4, 0x7A, 0x5A);

        int top;
        int mid;
        int hor;
        float phaseBlend = Mth.clamp((phase - 5.0F) / 0.5F, 0.0F, 1.0F);
        if (phase < 5.5F) {
            top = mixColor(p5Top, p55Top, phaseBlend);
            mid = mixColor(p5Mid, p55Mid, phaseBlend);
            hor = mixColor(p5Hor, p55Hor, phaseBlend);
        } else {
            float t = Mth.clamp((phase - 5.5F) / 0.5F, 0.0F, 1.0F);
            top = mixColor(p55Top, p6Top, t);
            mid = mixColor(p55Mid, p6Mid, t);
            hor = mixColor(p55Hor, p6Hor, t);
        }

        int color;
        if (vertical < 0.38F) {
            color = mixColor(hor, mid, vertical / 0.38F);
        } else {
            color = mixColor(mid, top, (vertical - 0.38F) / 0.62F);
        }
        return (color & 0x00FFFFFF) | (clamp((int) alpha, 0, 255) << 24);
    }

    private static void submitPadAndEdge(PoseStack poseStack, SubmitNodeCollector collector,
            RenderType type, Vec3 center, boolean outside) {
        final int alpha = outside ? 255 : 220;
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            double h = PAD_HALF_SIZE;
            double y = PAD_FLOOR_Y;
            quad(pose, consumer,
                    new Vec3(center.x - h, y, center.z - h),
                    new Vec3(center.x + h, y, center.z - h),
                    new Vec3(center.x + h, y, center.z + h),
                    new Vec3(center.x - h, y, center.z + h),
                    4, 5, 8, alpha);
            // Four flat cutoff faces hide the underside beyond the stage edge.
            wall(pose, consumer, center.x - h, center.z - h, center.x + h, center.z - h, alpha);
            wall(pose, consumer, center.x + h, center.z - h, center.x + h, center.z + h, alpha);
            wall(pose, consumer, center.x + h, center.z + h, center.x - h, center.z + h, alpha);
            wall(pose, consumer, center.x - h, center.z + h, center.x - h, center.z - h, alpha);
        });
    }

    private static void wall(Pose pose, VertexConsumer consumer, double x0, double z0,
            double x1, double z1, int alpha) {
        double bottom = -32.0D;
        double top = PAD_FLOOR_Y;
        quad(pose, consumer, new Vec3(x0, bottom, z0), new Vec3(x1, bottom, z1),
                new Vec3(x1, top, z1), new Vec3(x0, top, z0), 3, 4, 6, alpha);
    }

    private static void submitPaperMountains(PoseStack poseStack, SubmitNodeCollector collector,
            RenderType type, Vec3 center, Vec3 camera, float phase) {
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            double h = PAD_HALF_SIZE * 0.94D;
            for (int i = 0; i < PROP_X.length; i++) {
                double x = center.x + PROP_X[i] * h;
                double z = center.z + PROP_Z[i] * h;
                Vec3 at = new Vec3(x, 130.0D + (i % 3) * 8.0D, z);
                Vec3 view = camera.subtract(at).normalize();
                double width = 42.0D + (i % 4) * 13.0D;
                double height = 48.0D + (i % 5) * 18.0D;
                int tint = phase >= 6.0F ? pack(80, 47, 58, 226)
                        : pack(25, 30, 43, 230);
                paperMountain(pose, consumer, at, view, width, height, tint);
            }
        });
    }

    private static void paperMountain(Pose pose, VertexConsumer consumer, Vec3 at, Vec3 view,
            double width, double height, int tint) {
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = view.cross(up).normalize();
        Vec3 baseLeft = at.subtract(right.scale(width));
        Vec3 baseRight = at.add(right.scale(width));
        Vec3 peak = at.add(up.scale(height));
        // Two triangles form a paper-thin low-poly mountain.  The reverse
        // triangle is also submitted so culling cannot make the prop vanish.
        triangle(pose, consumer, baseLeft, baseRight, peak, tint);
        triangle(pose, consumer, baseRight, baseLeft, peak, tint);
    }

    private static void triangle(Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, int tint) {
        vertex(pose, consumer, a, 0.0F, 1.0F, tint, normal(c.subtract(a).cross(b.subtract(a))));
        vertex(pose, consumer, b, 1.0F, 1.0F, tint, normal(c.subtract(a).cross(b.subtract(a))));
        vertex(pose, consumer, c, 0.5F, 0.0F, tint, normal(c.subtract(a).cross(b.subtract(a))));
    }

    private static void quad(Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            int r, int g, int bColor, int alpha) {
        int color = pack(r, g, bColor, alpha);
        Vec3 n = normal(c.subtract(a).cross(b.subtract(a)));
        vertex(pose, consumer, a, 0.0F, 1.0F, color, n);
        vertex(pose, consumer, b, 1.0F, 1.0F, color, n);
        vertex(pose, consumer, c, 1.0F, 0.0F, color, n);
        vertex(pose, consumer, d, 0.0F, 0.0F, color, n);
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Vec3 at, float u, float v,
            int color, Vec3 normal) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static Vec3 normal(Vec3 value) {
        return value.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 1.0D, 0.0D) : value.normalize();
    }

    private static int mixColor(int a, int b, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int blue = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return rgb(r, g, blue);
    }

    private static int rgb(int r, int g, int b) {
        return (clamp(r, 0, 255) << 16) | (clamp(g, 0, 255) << 8) | clamp(b, 0, 255);
    }

    private static int pack(int r, int g, int b, int alpha) {
        return (clamp(alpha, 0, 255) << 24) | rgb(r, g, b);
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }
}
