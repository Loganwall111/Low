package net.dabicco.witherstormmod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dabicco.witherstormmod.client.ClientDistantStormManager;
import net.dabicco.witherstormmod.client.GlowRenderTypes;
import net.dabicco.witherstormmod.client.StormPresenceFX;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores the world-anchored presence effects that were temporarily disabled
 * while the attached oval was being repaired.
 *
 * The base pass supplies the sparks, pulse and original Catalyst halo. This
 * hook deliberately does not cancel it: it adds the phase-colored, enlarged
 * oval layers from the later 1.9.212/1.9.215 asset set. They are positioned at
 * the storm's real world coordinates rather than on a camera-wide sky card.
 */
@Mixin(StormPresenceFX.class)
public abstract class McsmPresenceFxPatch {
    private static final Identifier TURQUOISE = id("textures/misc/backdrop_turquoise.png");
    private static final Identifier PURPLE = id("textures/misc/backdrop_purple.png");
    private static final Identifier PURPLE_PINK = id("textures/misc/backdrop_purple_pink.png");
    private static final Identifier HALO_RING = id("textures/misc/halo_ring.png");
    private static final Identifier HALO_WHITE = id("textures/mcsm_atmosphere/halo.png");

    @Inject(method = "submit", at = @At("HEAD"), remap = false, require = 0)
    private static void dabyws$restorePresenceFx(LevelRenderContext ctx, CallbackInfo ci) {
        try {
            // The user explicitly wants these layers back. Keep the old pass
            // enabled as well so its debris and pulse colors are not lost.
            DabyWSClientConfig.cataclysmHalos = true;
            DabyWSClientConfig.blackGlare = true;
            DabyWSClientConfig.atmospherePulse = true;
            DabyWSClientConfig.glareEjecta = true;
            submitExpandedHalos(ctx);
        } catch (Throwable ignored) {
            // A missing optional render surface must never break the client.
        }
    }

    private static void submitExpandedHalos(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || ctx == null) {
            return;
        }
        Vec3 camera = ctx.levelState().cameraRenderState.pos;
        PoseStack poseStack = ctx.poseStack();
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        for (ClientDistantStormManager.StormData storm : ClientDistantStormManager.all()) {
            float phase = storm.phase;
            if (phase < 4.45F) {
                continue;
            }
            Vec3 centre = new Vec3(storm.dispX, storm.dispY, storm.dispZ);
            Vec3 toStorm = centre.subtract(camera);
            double distance = toStorm.length();
            if (distance < 1.0D || distance > 2700.0D) {
                continue;
            }
            float distanceFade = 1.0F - smoothstep((float) distance, 1700.0F, 2700.0F);
            if (distanceFade <= 0.004F) {
                continue;
            }
            double bodyRadius = bodyRadius(phase);
            // Lift every phase's halo as a unit so its center sits over the
            // storm's crown instead of cutting across the middle of the body.
            // The lift grows with the phase because the later storm silhouette
            // is taller and wider.
            Vec3 haloCentre = centre.add(0.0D, haloLift(phase, bodyRadius), 0.0D);
            Vec3 view = haloCentre.subtract(camera).normalize();

            // These are the actual colored backdrop assets, not placeholder
            // geometry. Cross-fade them through teal -> purple -> pink while
            // keeping the phase-5 deck green/teal as in the reference.
            float teal = smoothstep(phase, 4.45F, 5.05F)
                    * (1.0F - smoothstep(phase, 5.02F, 5.28F));
            float purple = smoothstep(phase, 5.00F, 5.30F)
                    * (1.0F - smoothstep(phase, 5.42F, 5.72F));
            float pink = smoothstep(phase, 5.34F, 5.68F)
                    * (1.0F - smoothstep(phase, 5.92F, 6.12F));
            float phaseSix = smoothstep(phase, 5.86F, 6.12F);

            layer(poseStack, collector, TURQUOISE, haloCentre, view,
                    bodyRadius * 3.35D, bodyRadius * 2.25D,
                    teal * distanceFade * 0.72F);
            layer(poseStack, collector, PURPLE, haloCentre, view,
                    bodyRadius * 3.55D, bodyRadius * 2.35D,
                    purple * distanceFade * 0.68F);
            layer(poseStack, collector, PURPLE_PINK, haloCentre, view,
                    bodyRadius * 3.85D, bodyRadius * 2.55D,
                    pink * distanceFade * 0.74F);
            layer(poseStack, collector, PURPLE_PINK, haloCentre, view,
                    bodyRadius * 4.05D, bodyRadius * 2.65D,
                    phaseSix * distanceFade * 0.48F);

            // The purple/pink oval ring is the older Catalyst Halo that was
            // present in the newer builds. Its width is intentionally larger
            // than the body's top silhouette, matching the supplied reference.
            float ring = smoothstep(phase, 5.18F, 5.48F);
            // Phase 5.5 is the broad rear-circle shot: enlarge the ring in
            // both axes until it clears and visually swallows the top of the
            // storm instead of reading as a small belt behind it.
            float phase55Circle = smoothstep(phase, 5.22F, 5.50F)
                    * (1.0F - smoothstep(phase, 5.70F, 5.96F));
            double ringWidth = bodyRadius * (4.35D + 3.05D * phase55Circle);
            double ringHeight = bodyRadius * (2.82D + 2.45D * phase55Circle);
            layer(poseStack, collector, HALO_RING, haloCentre, view,
                    ringWidth, ringHeight,
                    ring * distanceFade * 0.88F);
            if (phase >= 5.82F) {
                layer(poseStack, collector, HALO_RING, haloCentre, view,
                        bodyRadius * 3.05D, bodyRadius * 1.98D,
                        smoothstep(phase, 5.82F, 6.12F) * distanceFade * 0.46F);
                // This is the retained white under-halo from the newer asset
                // set. The texture is black outside its luminous shape, so it
                // is submitted through the additive glow pipeline rather than
                // as a translucent dark card.
                Vec3 under = haloCentre.add(0.0D, -bodyRadius * 0.38D, 0.0D);
                layer(poseStack, collector, HALO_WHITE, under, view,
                        bodyRadius * 3.25D, bodyRadius * 2.80D,
                        smoothstep(phase, 5.82F, 6.18F) * distanceFade * 0.34F);
            }
        }
    }

    private static double haloLift(float phase, double bodyRadius) {
        float progression = Mth.clamp((phase - 4.45F) / 1.55F, 0.0F, 1.0F);
        return bodyRadius * (0.80D + 0.20D * progression);
    }

    private static double bodyRadius(float phase) {
        if (phase < 4.0F) {
            return 4.0D + phase * 1.5D;
        }
        if (phase < 5.0F) {
            return 10.0D + (phase - 4.0D) * 12.0D;
        }
        return 22.0D + Math.min(phase - 5.0F, 1.99F) * 9.0D;
    }

    private static void layer(PoseStack poseStack, SubmitNodeCollector collector,
            Identifier texture, Vec3 centre, Vec3 view,
            double horizontalRadius, double verticalRadius, float alpha) {
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        if (a <= 2) {
            return;
        }
        Vec3 upHint = Math.abs(view.y) > 0.98D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = view.cross(upHint).normalize().scale(horizontalRadius);
        Vec3 up = right.normalize().cross(view).normalize().scale(verticalRadius);
        // The backdrop PNGs carry the requested teal/purple/pink color, so
        // they use the normal translucent textured pipeline. The historical
        // halo assets use the emissive pipeline only for the white under-halo;
        // the purple ring stays textured so its violet/pink RGB is preserved.
        RenderType type = texture.equals(HALO_WHITE)
                ? GlowRenderTypes.glow(texture)
                : GlowRenderTypes.translucent(texture);
        collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            vertex(pose, consumer, centre.subtract(right).subtract(up), 0.0F, 0.0F, a);
            vertex(pose, consumer, centre.add(right).subtract(up), 1.0F, 0.0F, a);
            vertex(pose, consumer, centre.add(right).add(up), 1.0F, 1.0F, a);
            vertex(pose, consumer, centre.subtract(right).add(up), 0.0F, 1.0F, a);
        });
    }

    private static void vertex(Pose pose, VertexConsumer consumer, Vec3 at,
            float u, float v, int alpha) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static float smoothstep(float value, float low, float high) {
        float t = Mth.clamp((value - low) / (high - low), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
    }
}
