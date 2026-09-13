package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Random;
import net.dabicco.witherstormmod.ModSounds;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class StormPresenceFX {
   private static final Identifier SOFT = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/tractor_beam.png");
   private static final Identifier PIECE_TEX = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/misc/broken_piece.png");
   private static final String[] PIECES = new String[]{"broken_piece_a", "broken_piece_b", "broken_piece_c", "broken_piece_d"};
   private static final Identifier HALO = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/misc/halo_ring.png");
   private static final int FULL_BRIGHT = 15728880;
   private static final float[] PULSE_SIZES = new float[]{1.0F, 1.45F, 2.05F};
   private static final float[] PULSE_ALPHAS = new float[]{0.3F, 0.18F, 0.1F};
   private static final int SPARKS = 128;
   private static final float[] SX = new float[128];
   private static final float[] SY = new float[128];
   private static final float[] SZ = new float[128];
   private static final float[] SVX = new float[128];
   private static final float[] SVY = new float[128];
   private static final float[] SVZ = new float[128];
   private static final int[] SLIFE = new int[128];
   private static final int[] SMAX = new int[128];
   private static final float[] SROT = new float[128];
   private static final float[] SROTV = new float[128];
   private static final int[] SKIND = new int[128];
   private static final Random RANDOM = new Random(7303L);
   private static float emitAcc;
   private static long nextBeatGameTime = -1L;

   private StormPresenceFX() {
   }

   private static double bodyRadius(float phase) {
      if (phase < 4.0F) {
         return 4.0 + phase * 1.5;
      } else {
         return phase < 5.0F ? 10.0 + (phase - 4.0F) * 12.0 : 22.0 + Math.min(phase - 5.0F, 1.99F) * 9.0;
      }
   }

   private static double haloLift(float phase, double bodyR) {
      float progression = Mth.clamp((phase - 4.45F) / 1.55F, 0.0F, 1.0F);
      return bodyR * (0.80D + 0.20D * progression);
   }

   private static float pulseWave(int stormId, float timeSeconds) {
      float period = (float)Math.max(0.5, DabyWSClientConfig.pulsePeriod);
      float off = stormId % 977 * 0.618034F;
      float s = Mth.sin((timeSeconds / period + off) * (float) (Math.PI * 2));
      return s * s;
   }

   public static void submit(LevelRenderContext ctx) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && !net.dabicco.witherstormmod.client.ClientDistantStormManager.all().isEmpty()) {
         float gt = (float)(mc.level.getGameTime() % 240000L) + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         float nowSec = gt * 0.05F;
         Vec3 cam = ctx.levelState().cameraRenderState.pos;
         PoseStack poseStack = ctx.poseStack();
         SubmitNodeCollector collector = ctx.submitNodeCollector();
         float[] col = new float[3];

         for (net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData d : net.dabicco.witherstormmod.client.ClientDistantStormManager.all()) {
            float phase = d.phase;
            Vec3 centre = new Vec3(d.dispX, d.dispY, d.dispZ);
            double bodyR = bodyRadius(phase);
            // Keep the original halo pass aligned with the expanded halo: the
            // full halo belongs over the storm crown, not through its center.
            Vec3 haloCentre = centre.add(0.0, haloLift(phase, bodyR), 0.0);
            if (DabyWSClientConfig.blackGlare && phase >= 4.0F) {
               float strength = (float)DabyWSClientConfig.blackGlareStrength * Mth.clamp((phase - 3.6F) / 0.8F, 0.0F, 1.0F);
               if (strength > 0.004F) {
                  Vec3 view = haloCentre.subtract(cam).normalize();
                  quad(
                     poseStack,
                     collector,
                     net.dabicco.witherstormmod.client.GlowRenderTypes.translucent(HALO),
                     cam,
                     haloCentre,
                     view,
                     bodyR * 1.3,
                     4,
                     2,
                     7,
                     (int)(strength * 235.0F)
                  );
                  quad(
                     poseStack,
                     collector,
                     net.dabicco.witherstormmod.client.GlowRenderTypes.translucent(HALO),
                     cam,
                     haloCentre,
                     view,
                     bodyR * 1.75,
                     3,
                     2,
                     6,
                     (int)(strength * 120.0F)
                  );
               }
            }

            if (DabyWSClientConfig.atmospherePulse && phase >= 4.5F) {
               float ramp = Mth.clamp((phase - 4.5F) / 0.6F, 0.0F, 1.0F);
               float breathe = pulseWave(d.entityId, nowSec);
               float amount = (float)DabyWSClientConfig.pulseStrength * ramp * (0.25F + 0.75F * breathe);
               if (amount > 0.004F) {
                  net.dabicco.witherstormmod.client.StormPalettes.pulseColor(phase, col);
                  int[][] layerCols = new int[PULSE_SIZES.length][3];

                  for (int i = 0; i < PULSE_SIZES.length; i++) {
                     layerCols[i][0] = (int)(col[0] * 255.0F);
                     layerCols[i][1] = (int)(col[1] * 255.0F);
                     layerCols[i][2] = (int)(col[2] * 255.0F);
                  }

                  double wob = Math.sin(nowSec * 0.21F + d.entityId) * bodyR * 0.12;
                  double wob2 = Math.cos(nowSec * 0.17F + d.entityId * 2) * bodyR * 0.12;
                  Vec3 glowCentre = centre.add(wob, bodyR * 0.18 + Math.sin(nowSec * 0.13F + d.entityId * 3) * bodyR * 0.05, wob2);
                  Vec3 glowView = glowCentre.subtract(cam).normalize();
                  double radius = bodyR * 2.2 * (float)DabyWSClientConfig.pulseSize;
                  net.dabicco.witherstormmod.client.StormGlowRenderer.submitLight(
                     poseStack, collector, glowCentre, glowView, radius, PULSE_SIZES, PULSE_ALPHAS, layerCols, amount
                  );
               }
            }

            if (DabyWSClientConfig.cataclysmHalos && phase >= 5.8F) {
               float ramp = Mth.clamp((phase - 5.8F) / 0.35F, 0.0F, 1.0F);
               float amount = (float)DabyWSClientConfig.haloStrength * ramp;
               if (amount > 0.004F) {
                  Vec3 view = haloCentre.subtract(cam).normalize();
                  net.dabicco.witherstormmod.client.StormPalettes.haloRingColor(col);
                  int aOuter = (int)(Mth.clamp(amount * 0.85F * (0.6F + 0.4F * pulseWave(d.entityId, nowSec)), 0.0F, 1.0F) * 255.0F);
                  quad(
                     poseStack,
                     collector,
                     net.dabicco.witherstormmod.client.GlowRenderTypes.glow(HALO),
                     cam,
                     haloCentre,
                     view,
                     bodyR * 1.9,
                     (int)(col[0] * 255.0F),
                     (int)(col[1] * 255.0F),
                     (int)(col[2] * 255.0F),
                     aOuter
                  );
                  net.dabicco.witherstormmod.client.StormPalettes.haloUnderColor(col);
                  Vec3 underCentre = haloCentre.add(0.0, -bodyR * 0.55, 0.0);
                  Vec3 viewUnder = underCentre.subtract(cam).normalize();
                  int aUnder = (int)(Mth.clamp(amount * 0.95F, 0.0F, 1.0F) * 255.0F);
                  quad(
                     poseStack,
                     collector,
                     net.dabicco.witherstormmod.client.GlowRenderTypes.glow(HALO),
                     cam,
                     underCentre,
                     viewUnder,
                     bodyR * 1.1,
                     (int)(col[0] * 255.0F),
                     (int)(col[1] * 255.0F),
                     (int)(col[2] * 255.0F),
                     aUnder
                  );
               }
            }
         }

         if (DabyWSClientConfig.glareEjecta) {
            collector.submitCustomGeometry(
               poseStack,
               net.dabicco.witherstormmod.client.GlowRenderTypes.glow(SOFT),
               (pose, consumer) -> {
                  float[] c = new float[3];

                  for (int i = 0; i < 128; i++) {
                     if (SLIFE[i] > 0 && SKIND[i] != 3) {
                        float fade = (float)SLIFE[i] / Math.max(1, SMAX[i]);
                        float alpha = fade * fade * (float)DabyWSClientConfig.ejectaBrightness;
                        if (!(alpha <= 0.004F)) {
                           float[] base = SKIND[i] == 0
                              ? net.dabicco.witherstormmod.client.StormPalettes.EJECTA_TEAL
                              : (
                                 SKIND[i] == 1
                                    ? net.dabicco.witherstormmod.client.StormPalettes.EJECTA_GREEN
                                    : net.dabicco.witherstormmod.client.StormPalettes.EJECTA_PALE
                              );
                           c[0] = base[0];
                           c[1] = base[1];
                           c[2] = base[2];
                           int a = (int)(alpha * 255.0F);
                           int r = (int)(c[0] * 255.0F);
                           int g = (int)(c[1] * 255.0F);
                           int b = (int)(c[2] * 255.0F);
                           Vec3 at = new Vec3(SX[i], SY[i], SZ[i]);
                           Vec3 viewx = at.subtract(cam).normalize();
                           Vec3 upHint = Math.abs(viewx.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
                           Vec3 right = viewx.cross(upHint).normalize().scale(1.15F);
                           Vec3 up = right.cross(viewx).normalize().scale(1.15F);
                           vertex(pose, consumer, at.subtract(right).subtract(up), 0.0F, 0.0F, r, g, b, a);
                           vertex(pose, consumer, at.add(right).subtract(up), 1.0F, 0.0F, r, g, b, a);
                           vertex(pose, consumer, at.add(right).add(up), 1.0F, 1.0F, r, g, b, a);
                           vertex(pose, consumer, at.subtract(right).add(up), 0.0F, 1.0F, r, g, b, a);
                        }
                     }
                  }
               }
            );
            collector.submitCustomGeometry(
               poseStack,
               net.dabicco.witherstormmod.client.GlowRenderTypes.translucent(PIECE_TEX),
               (pose, consumer) -> {
                  for (int i = 0; i < 128; i++) {
                     if (SLIFE[i] > 0 && SKIND[i] == 3) {
                        float fade = (float)SLIFE[i] / Math.max(1, SMAX[i]);
                        net.dabicco.witherstormmod.client.BakedMesh.emit(
                           consumer,
                           pose,
                           net.dabicco.witherstormmod.client.BakedMesh.mesh(PIECES[i % 4]),
                           new Vec3(SX[i], SY[i], SZ[i]),
                           SROT[i],
                           SROT[i] * 0.71F,
                           1.0F + i % 3 * 0.55F,
                           148,
                           112,
                           190,
                           (int)(fade * 240.0F),
                           15728880
                        );
                     }
                  }
               }
            );
         }
      }
   }

   public static void tick(Minecraft mc) {
      if (mc.level != null && !mc.isPaused()) {
         for (int i = 0; i < 128; i++) {
            if (SLIFE[i] > 0) {
               SLIFE[i]--;
               SX[i] = SX[i] + SVX[i];
               SY[i] = SY[i] + SVY[i];
               SZ[i] = SZ[i] + SVZ[i];
               SVX[i] = SVX[i] * 0.97F;
               SVZ[i] = SVZ[i] * 0.97F;
               SVY[i] = SVY[i] * 0.97F - 0.055F;
               SROT[i] = SROT[i] + SROTV[i];
            }
         }

         if (DabyWSClientConfig.glareEjecta) {
            for (net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData d : net.dabicco.witherstormmod.client.ClientDistantStormManager.all()) {
               if (!(d.phase < 4.5F)) {
                  float rate = (float)DabyWSClientConfig.ejectaRate * Mth.clamp((d.phase - 4.0F) / 1.8F, 0.15F, 1.0F);
                  emitAcc += rate * 0.02F;

                  while (emitAcc >= 1.0F) {
                     emitAcc--;
                     spawnSpark(d);
                  }
               }
            }
         }

         if (DabyWSClientConfig.pulseHeartbeat) {
            net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData nearest = null;
            double best = Double.MAX_VALUE;
            Vec3 cam = mc.player != null ? mc.player.position() : null;
            if (cam != null) {
               for (net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData dx : net.dabicco.witherstormmod.client.ClientDistantStormManager.all()) {
                  if (!(dx.phase < 4.5F)) {
                     double dist = cam.distanceTo(new Vec3(dx.dispX, dx.dispY, dx.dispZ));
                     if (dist < best) {
                        best = dist;
                        nearest = dx;
                     }
                  }
               }
            }

            if (nearest != null && best < DabyWSClientConfig.pulseHeartbeatRange) {
               float period = (float)Math.max(0.5, DabyWSClientConfig.pulsePeriod);
               long gt = mc.level.getGameTime();
               if (nextBeatGameTime < 0L) {
                  nextBeatGameTime = gt;
               }

               if (gt >= nextBeatGameTime) {
                  nextBeatGameTime = gt + Math.max(10L, (long)(period * 20.0F));
                  float vol = (float)(1.0 - best / DabyWSClientConfig.pulseHeartbeatRange) * (float)DabyWSClientConfig.pulseHeartbeatVolume;
                  if (vol > 0.02F) {
                     mc.level.playLocalSound(cam.x, cam.y, cam.z, ModSounds.STORM_THUMP, SoundSource.HOSTILE, vol, 0.5F, false);
                  }
               }
            }
         }
      }
   }

   private static void spawnSpark(net.dabicco.witherstormmod.client.ClientDistantStormManager.StormData d) {
      for (int i = 0; i < 128; i++) {
         if (SLIFE[i] <= 0) {
            double bodyR = bodyRadius(d.phase);
            double theta = RANDOM.nextDouble() * Math.PI * 2.0;
            double ringBias = 0.55 + 0.45 * RANDOM.nextDouble();
            SX[i] = (float)(d.dispX + Math.cos(theta) * ringBias * bodyR);
            SY[i] = (float)(d.dispY + (RANDOM.nextDouble() - 0.35) * bodyR * 0.8);
            SZ[i] = (float)(d.dispZ + Math.sin(theta) * ringBias * bodyR);
            double out = 0.1 + RANDOM.nextDouble() * 0.3;
            SVX[i] = (float)(Math.cos(theta) * out);
            SVZ[i] = (float)(Math.sin(theta) * out);
            SVY[i] = (float)(0.06 + RANDOM.nextDouble() * 0.22);
            SMAX[i] = SLIFE[i] = 30 + RANDOM.nextInt(60);
            float pick = RANDOM.nextFloat();
            SKIND[i] = pick < 0.12F ? 3 : (pick < 0.6F ? 0 : (pick < 0.88F ? 1 : 2));
            SROT[i] = RANDOM.nextFloat() * 360.0F;
            SROTV[i] = -8.0F + RANDOM.nextFloat() * 16.0F;
            return;
         }
      }
   }

   private static void quad(
      PoseStack poseStack, SubmitNodeCollector collector, RenderType type, Vec3 cam, Vec3 centre, Vec3 view, double radius, int r, int g, int b, int alpha
   ) {
      if (alpha > 2) {
         Vec3 upHint = Math.abs(view.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
         Vec3 right = view.cross(upHint).normalize();
         Vec3 up = right.cross(view).normalize();
         Vec3 rx = right.scale(radius);
         Vec3 uy = up.scale(radius);
         collector.submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            vertex(pose, consumer, centre.subtract(rx).subtract(uy), 0.0F, 0.0F, r, g, b, alpha);
            vertex(pose, consumer, centre.add(rx).subtract(uy), 1.0F, 0.0F, r, g, b, alpha);
            vertex(pose, consumer, centre.add(rx).add(uy), 1.0F, 1.0F, r, g, b, alpha);
            vertex(pose, consumer, centre.subtract(rx).add(uy), 0.0F, 1.0F, r, g, b, alpha);
         });
      }
   }

   private static void vertex(Pose pose, VertexConsumer consumer, Vec3 at, float u, float v, int r, int g, int b, int a) {
      consumer.addVertex(pose, (float)at.x, (float)at.y, (float)at.z)
         .setColor(r, g, b, a)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   public static void clear() {
      for (int i = 0; i < 128; i++) {
         SLIFE[i] = 0;
      }

      emitAcc = 0.0F;
      nextBeatGameTime = -1L;
   }
}
