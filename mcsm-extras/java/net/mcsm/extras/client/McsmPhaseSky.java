package net.mcsm.extras.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
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
 * MCSM storm glare — body-glued SOFT volume from mcsm_atmosphere/glare/*.
 *
 * One (plus thin flank) soft radial disc per storm. NO mesh, NO stacked
 * concentric rings, NO dots, NO lines. Texture alpha carries the fade.
 * Purple/pink glare is storm-phase only (5.4+); calm night never uses it.
 */
public final class McsmPhaseSky {

    private static final Identifier G4 = id("textures/mcsm_atmosphere/glare/phase4.png");
    private static final Identifier G5 = id("textures/mcsm_atmosphere/glare/phase5.png");
    private static final Identifier G54 = id("textures/mcsm_atmosphere/glare/phase54.png");
    private static final Identifier G55 = id("textures/mcsm_atmosphere/glare/phase55.png");
    private static final Identifier G6 = id("textures/mcsm_atmosphere/glare/phase6.png");
    private static final Identifier BLACK = id("textures/misc/backdrop_black.png");

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
    }

    private McsmPhaseSky() {
    }

    private static float ramp(float v, float lo, float hi) {
        if (hi <= lo) {
            return v >= hi ? 1.0F : 0.0F;
        }
        float t = Mth.clamp((v - lo) / (hi - lo), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double bodyRadius(float phase) {
        if (phase < 4.0F) {
            return 4.0 + 1.5 * phase;
        } else if (phase < 5.0F) {
            return 10.0 + 8.0 * (phase - 4.0F);
        } else {
            return phase < 6.0F ? 18.0 + 22.0 * (phase - 5.0F) : 40.0 + 30.0 * (phase - 6.0F);
        }
    }

    public static Vec3 swayOffset(float phase, float timeSec, double bodyR) {
        if (phase < 4.0F || bodyR <= 0.0) {
            return Vec3.ZERO;
        }
        float amp = (float) (bodyR * (0.04 + 0.03 * Mth.clamp((phase - 4.0F) / 3.0F, 0.0F, 1.0F)));
        return new Vec3(
                Mth.sin(timeSec * 0.22F) * amp,
                Mth.sin(timeSec * 0.13F) * amp * 0.18F,
                Mth.sin(timeSec * 0.17F + 1.3F) * amp * 0.55F);
    }

    /** Pick glare texture for phase — matches user atmosphere pack. */
    private static Identifier glareTex(float phase) {
        if (phase >= 6.0F) {
            return G6;
        }
        if (phase >= 5.48F) {
            return G55; // 5.5-5.9 pink-lavender soft mass
        }
        if (phase >= 5.25F) {
            return G54; // 5.4 purple
        }
        if (phase >= 4.9F) {
            return G5; // 5.0 teal
        }
        return G4; // 4.x icy blue
    }

    public static void submit(LevelRenderContext ctx) {
        try {
            submitInner(ctx);
        } catch (Throwable ignored) {
        }
    }

    private static void submitInner(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ClientDistantStormManager.all().isEmpty()) {
            return;
        }
        float gt = (float) (mc.level.getGameTime() % 240000L)
                + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float nowSec = gt * 0.05F;
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        McsmExtrasConfig.load();
        double glareMul = Mth.clamp(McsmExtrasConfig.glareSize, 0.25, 3.05);
        double smudge = Mth.clamp(McsmExtrasConfig.smudgeScale, 0.15, 2.5);

        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();

        for (ClientDistantStormManager.StormData d : ClientDistantStormManager.all()) {
            float phase = d.phase;
            // Glare only when storm has real mass — never paints calm night purple
            if (phase < 3.95F) {
                continue;
            }
            Vec3 stormPos = new Vec3(d.dispX, d.dispY, d.dispZ);
            double bodyR = bodyRadius(phase);
            Vec3 centre = stormPos.add(swayOffset(phase, nowSec, bodyR));

            Vec3 toStorm = centre.subtract(cam);
            double dist = toStorm.length();
            if (dist < 1.0E-3) {
                continue;
            }
            Vec3 view = toStorm.scale(1.0 / dist);
            float distFade = 1.0F - Mth.clamp((float) ((dist - 1800.0) / 1200.0), 0.0F, 1.0F);
            if (distFade <= 0.01F) {
                continue;
            }

            float presence = ramp(phase, 3.95F, 4.25F);
            if (presence <= 0.01F) {
                continue;
            }

            float breathe = 1.0F + 0.02F * Mth.sin(nowSec * 0.05F);
            double baseR = bodyR * (1.55 + 1.05 * glareMul) * smudge * breathe;
            if (phase > 5.3F) {
                baseR *= 1.0 + (phase - 5.3F) * 0.16;
            }
            // 5.5+ glare is BIG soft mass like stills (wraps body, not tiny ball)
            if (phase >= 5.48F && phase < 6.0F) {
                baseR *= 1.35;
            }
            float amp = presence * distFade;
            int aa = Mth.clamp((int) (amp * 210.0F), 0, 255);
            if (aa <= 4) {
                continue;
            }

            Identifier tex = glareTex(phase);

            // Dark core plate behind glare so body stays silhouette (no grey wash)
            if (phase >= 4.5F) {
                int ca = Mth.clamp((int) (amp * 70.0F), 0, 255);
                quad(poseStack, collector, GlowRenderTypes.glow(BLACK),
                        centre, view, baseR * 0.72, 4, 3, 6, ca);
            }

            // MAIN glare: single soft disc (texture alpha = fade). No stack.
            // Tint white so texture RGB carries phase colour.
            quad(poseStack, collector, GlowRenderTypes.glow(tex),
                    centre, view, baseR, 255, 255, 255, aa);
            // one slightly larger outer skirt for thickness without ring artifacts
            int aa2 = Mth.clamp((int) (aa * 0.45F), 0, 255);
            if (aa2 > 4) {
                quad(poseStack, collector, GlowRenderTypes.glow(tex),
                        centre.add(view.scale(-bodyR * 0.08)), view,
                        baseR * 1.28, 255, 255, 255, aa2);
            }

            // thin flank fill so volume reads from the side (still no dots)
            if (amp > 0.12F && baseR > 8.0) {
                Vec3 upHint = Math.abs(view.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 right = view.cross(upHint).normalize();
                int sa = Mth.clamp((int) (aa * 0.28F), 0, 255);
                if (sa > 4) {
                    quad(poseStack, collector, GlowRenderTypes.glow(tex),
                            centre.add(right.scale(bodyR * 0.4)), view,
                            baseR * 0.95, 255, 255, 255, sa);
                    quad(poseStack, collector, GlowRenderTypes.glow(tex),
                            centre.add(right.scale(-bodyR * 0.4)), view,
                            baseR * 0.95, 255, 255, 255, sa);
                }
            }
        }
    }

    private static void quad(PoseStack poseStack, SubmitNodeCollector collector,
            net.minecraft.client.renderer.rendertype.RenderType type,
            Vec3 at, Vec3 view, double radius, int r, int g, int b, int alpha) {
        if (alpha <= 2 || radius < 0.5) {
            return;
        }
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 right = view.cross(upHint).normalize();
            Vec3 up = right.cross(view).normalize();
            // slight oval like MCSM mass (not perfect circle sticker)
            Vec3 rx = right.scale(radius * 1.10);
            Vec3 uy = up.scale(radius * 0.95);
            int fa = Math.min(Math.max(alpha, 0), 255);
            vtx(pose, consumer, at.subtract(rx).subtract(uy), 0.0F, 1.0F, r, g, b, fa);
            vtx(pose, consumer, at.add(rx).subtract(uy), 1.0F, 1.0F, r, g, b, fa);
            vtx(pose, consumer, at.add(rx).add(uy), 1.0F, 0.0F, r, g, b, fa);
            vtx(pose, consumer, at.subtract(rx).add(uy), 0.0F, 0.0F, r, g, b, fa);
        });
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
