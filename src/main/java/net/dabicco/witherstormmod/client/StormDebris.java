package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class StormDebris {
   private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dabywitherstormmod", "textures/entity/tractor_beam.png");
   private static final int SWARM = 1600;
   private static final int RING = 900;
   private static final int DEV_EXTRA = 900;
   private static final int HALO = 1100;
   private static final int HALO2 = 620;
   private static final int HEAD_RING = 420;
   private static final float HALO_RIM = 128.0F;
   private static final float HEAD_RING_RADIUS = 33.0F;
   private static final float HEAD_RING_SPREAD = 6.0F;
   private static final float HEAD_RING_HEIGHT = 24.0F;
   private static final float HEAD_RING_THICKNESS = 5.0F;
   private static final float HEAD_RING_SPEED = 0.078F;
   private static final int EARLY = 340;
   private static final int DEV_START = 2500;
   private static final int HALO_START = 3400;
   private static final int HALO2_START = 4500;
   private static final int HEAD_RING_START = 5120;
   private static final int EARLY_START = 5540;
   private static final int COUNT = 5880;
   private static final float EARLY_RADIUS = 4.6F;
   private static final float EARLY_SPREAD = 3.8F;
   private static final float EARLY_HEIGHT = 2.4F;
   private static final float EARLY_THICKNESS = 4.6F;
   private static final float EARLY_TILT = 0.3F;
   private static final float EARLY_GROWTH = 0.55F;
   private static final float EARLY_CUBE = 0.115F;
   private static final float EARLY_SPEED = 0.055F;
   private static final float EARLY_FROM = 0.5F;
   private static final float EARLY_TO = 3.8F;
   private static final float EARLY_EMERGE_SPAN = 0.1F;
   private static final float[] EARLY_DUE = new float[340];
   private static final float[] UX = new float[5880];
   private static final float[] UY = new float[5880];
   private static final float[] UZ = new float[5880];
   private static final float[] VX = new float[5880];
   private static final float[] VY = new float[5880];
   private static final float[] VZ = new float[5880];
   private static final float[] RADIUS = new float[5880];
   private static final float[] SPEED = new float[5880];
   private static final float[] PHASE = new float[5880];
   private static final float[] SIZE = new float[5880];
   private static final float[] CY = new float[5880];
   private static final float[] SHADE = new float[5880];
   private static final float[] TR = new float[5880];
   private static final float[] TG = new float[5880];
   private static final float[] TB = new float[5880];
   private static final float[] SPINX = new float[5880];
   private static final float[] SPINY = new float[5880];
   private static final float[] SPINZ = new float[5880];
   private static final float[] SPINPX = new float[5880];
   private static final float[] SPINPY = new float[5880];
   private static final float[] SPINPZ = new float[5880];
   private static final float[] RTARGET = new float[5880];
   private static final float[] RDELAY = new float[5880];
   private static final float RING_GROW_SEC = 4.5F;
   private static final float RETRACT_TICKS = 90.0F;
   private static final int SWARM_CULL_PERCENT = 88;
   private static final float SURVIVOR_SIZE = 0.45F;
   private static final HashMap<Integer, float[]> RING_HOME;
   private static final int GLOW_EVERY = 7;
   private static final float GLOW_GAIN = 0.18F;
   private static final float RING_HOME_SEC = 3.0F;
   private static final HashMap<Integer, float[]> EARLY_PHASE;
   private static final float EARLY_PHASE_RATE = 1.6F;
   private static float p00;
   private static float p01;
   private static float p02;
   private static float p10;
   private static float p11;
   private static float p12;
   private static float p20;
   private static float p21;
   private static float p22;
   private static float p30;
   private static float p31;
   private static float p32;
   private static float nrmX;
   private static float nrmY;
   private static float nrmZ;

   private StormDebris() {
   }

   private static float ringExtension(int stormId, boolean active) {
      long now = Util.getMillis();
      float[] slot = (float[])RING_HOME.get(stormId);
      if (slot == null) {
         slot = new float[]{active ? 1.0F : 0.0F, (float)now};
         RING_HOME.put(stormId, slot);
         if (RING_HOME.size() > 32) {
            RING_HOME.clear();
         }

         return slot[0];
      } else {
         float dt = Mth.clamp(((float)now - slot[1]) / 1000.0F, 0.0F, 0.5F);
         slot[1] = (float)now;
         float target = active ? 1.0F : 0.0F;
         float step = dt / 3.0F;
         slot[0] = slot[0] < target ? Math.min(target, slot[0] + step) : Math.max(target, slot[0] - step);
         return slot[0];
      }
   }

   public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase5Ticks, float phase58Ticks, int stormId, boolean devourer, float settle) {
      submit(poseStack, collector, timeTicks, light, phase5Ticks, phase58Ticks, stormId, devourer, settle, false);
   }

   public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase5Ticks, float phase58Ticks, int stormId, boolean devourer, float settle, boolean preview) {
      submit(poseStack, collector, timeTicks, light, phase5Ticks, phase58Ticks, stormId, settle, devourer ? 5880 : 2500, devourer, 2500, 1.0F, preview);
   }

   public static void submit(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase5Ticks, float phase58Ticks, int stormId, boolean devourer, float settle, boolean preview, float phase) {
      submit(poseStack, collector, timeTicks, light, phase5Ticks, phase58Ticks, stormId, settle, devourer ? 5880 : 2500, devourer, 2500, 1.0F, preview, phase);
   }

   public static void submitEarly(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase, int stormId) {
      if (!(phase < 0.5F)) {
         float ph = displayPhase(stormId, phase);
         collector.submitCustomGeometry(poseStack, FoglessRenderTypes.bodyCutout(TEXTURE), (pose, consumer) -> {
            Vector3f cam = camLocal(pose);
            beginBatch(pose);

            for(int i = 5540; i < 5880; ++i) {
               float due = EARLY_DUE[i - 5540];
               if (!(ph < due)) {
                  float t = Mth.clamp((ph - due) / 0.1F, 0.0F, 1.0F);
                  float ease = t * t * (3.0F - 2.0F * t);
                  float grow = 1.0F + 0.55F * Mth.clamp((ph - 0.5F) / 3.3F, 0.0F, 1.0F);
                  float radius = Mth.lerp(ease, 0.4F, RADIUS[i] * grow);
                  float size = 0.115F * ease * (float)DabyWSClientConfig.debrisSize;
                  if (!(size <= 1.0E-4F)) {
                     float a = PHASE[i] + timeTicks * SPEED[i];
                     float ca = Mth.cos((double)a) * radius;
                     float sa = Mth.sin((double)a) * radius;
                     float x = UX[i] * ca + VX[i] * sa;
                     float y = UY[i] * ca + VY[i] * sa + Mth.lerp(ease, 0.6F, CY[i] * grow);
                     float z = UZ[i] * ca + VZ[i] * sa;
                     int shade = (int)(SHADE[i] * 255.0F);
                     cube(pose, consumer, x, y, z, size, shade, shade, shade, light, i, timeTicks, cam.x, cam.y, cam.z);
                  }
               }
            }

         });
      }
   }

   private static float displayPhase(int stormId, float target) {
      long now = Util.getMillis();
      float[] slot = (float[])EARLY_PHASE.get(stormId);
      if (slot == null) {
         slot = new float[]{target, (float)now};
         if (EARLY_PHASE.size() > 32) {
            EARLY_PHASE.clear();
         }

         EARLY_PHASE.put(stormId, slot);
         return target;
      } else {
         float dt = Mth.clamp(((float)now - slot[1]) / 1000.0F, 0.0F, 0.5F);
         slot[1] = (float)now;
         if (Math.abs(target - slot[0]) > 0.6F) {
            slot[0] = target;
         } else {
            slot[0] += (target - slot[0]) * (1.0F - (float)Math.exp((double)(-dt * 1.6F)));
         }

         return slot[0];
      }
   }

   public static void submitSeveredCloud(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, int id, float cubeBoost) {
      submitSeveredCloud(poseStack, collector, timeTicks, light, id, cubeBoost, false);
   }

   public static void submitSeveredCloud(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, int id, float cubeBoost, boolean preview) {
      submit(poseStack, collector, timeTicks, light, 6000.0F, -1.0F, id, 0.0F, 2500, true, 0, cubeBoost, preview);
   }

   private static void submit(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase5Ticks, float phase58Ticks, int stormId, float settle, int drawCount, boolean violet, int glowFrom, float cubeBoost) {
      submit(poseStack, collector, timeTicks, light, phase5Ticks, phase58Ticks, stormId, settle, drawCount, violet, glowFrom, cubeBoost, false, 6.0F);
   }

   private static void submit(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase5Ticks, float phase58Ticks, int stormId, float settle, int drawCount, boolean violet, int glowFrom, float cubeBoost, boolean preview) {
      submit(poseStack, collector, timeTicks, light, phase5Ticks, phase58Ticks, stormId, settle, drawCount, violet, glowFrom, cubeBoost, preview, 6.0F);
   }

   private static void submit(PoseStack poseStack, SubmitNodeCollector collector, float timeTicks, int light, float phase5Ticks, float phase58Ticks, int stormId, float settle, int drawCount, boolean violet, int glowFrom, float cubeBoost, boolean preview, float phase) {
      boolean ringActive = phase5Ticks >= 0.0F;
      float homeAmount = ringExtension(stormId, ringActive);
      float elapsedSec = ringActive ? phase5Ticks / 20.0F : 0.0F;
      float retract = phase58Ticks < 0.0F ? 0.0F : Mth.clamp(phase58Ticks / 90.0F, 0.0F, 1.0F);
      float retractEase = retract * retract * (3.0F - 2.0F * retract);
      collector.submitCustomGeometry(poseStack, FoglessRenderTypes.bodyCutout(TEXTURE), (pose, consumer) -> {
         Vector3f cam = camLocal(pose);
         beginBatch(pose);
         float amount = (float)Mth.clamp(DabyWSClientConfig.debrisAmount, (double)0.0F, (double)2.0F);
         // Debris reaches its configured maximum only once the storm is at its
         // biggest: a thin veil at phase 4 that thickens into the full swarm
         // as the storm climbs toward phase 6+.
         float phaseRamp = Mth.clamp((phase - 3.6F) / 2.4F, 0.12F, 1.0F);
         phaseRamp = phaseRamp * phaseRamp * (3.0F - 2.0F * phaseRamp);
         int keepOf100 = (int)(amount * 50.0F * phaseRamp);

         for(int i = 0; i < drawCount; ++i) {
            if (keepOf100 >= 100 || i * 61 % 100 < keepOf100) {
               boolean isRing = RTARGET[i] >= 0.0F;
               if (!isRing || ringActive || !(homeAmount <= 0.002F)) {
                  boolean culled = isRing || i * 37 % 100 < 88;
                  if (!culled || !(retractEase >= 1.0F)) {
                     float radius = RADIUS[i];
                     boolean settling = settle > 0.0F && i * 41 % 100 < 70;
                     if (isRing) {
                        float t;
                        if (ringActive) {
                           t = Mth.clamp((elapsedSec - RDELAY[i]) / 4.5F, 0.0F, 1.0F);
                           t = t * t * (3.0F - 2.0F * t);
                        } else {
                           t = 1.0F;
                        }

                        radius = Mth.lerp(t * homeAmount, RADIUS[i], RTARGET[i]);
                     }

                     float size = SIZE[i] * cubeBoost * (float)DabyWSClientConfig.debrisSize;
                     if (culled && retractEase > 0.0F) {
                        radius = Mth.lerp(retractEase, radius, 0.0F);
                        size = Mth.lerp(retractEase, size, 0.0F);
                        if (size <= 0.002F) {
                           continue;
                        }
                     } else if (retractEase > 0.0F) {
                        size = Mth.lerp(retractEase, size, size * 0.45F);
                     }

                     float a = PHASE[i] + timeTicks * SPEED[i];
                     float ca = Mth.cos((double)a) * radius;
                     float sa = Mth.sin((double)a) * radius;
                     float x = UX[i] * ca + VX[i] * sa;
                     float y = UY[i] * ca + VY[i] * sa + CY[i];
                     float z = UZ[i] * ca + VZ[i] * sa;
                     float shade = SHADE[i] * 255.0F;
                     if (settling) {
                        Mth.lerp(settle, radius, RADIUS[i] * 0.12F);
                     }

                     int cr;
                     int cg;
                     int cb;
                     if (violet && TR[i] >= 0.0F) {
                        cr = (int)TR[i];
                        cg = (int)TG[i];
                        cb = (int)TB[i];
                     } else {
                        cr = cg = cb = (int)shade;
                     }

                     cube(pose, consumer, x, y, z, size, cr, cg, cb, light, i, timeTicks, cam.x, cam.y, cam.z);
                  }
               }
            }
         }

      });
      if (violet && DabyWSClientConfig.devourerDebrisGlow) {
         if (!preview) {
            boolean ourBloom = StormBloom.wantsEntityTarget();
            boolean packGlow = ShaderPackCompat.active();
            if (ourBloom || packGlow) {
               collector.order(1).submitCustomGeometry(poseStack, ourBloom ? GlowRenderTypes.bloomSource(TEXTURE) : GlowRenderTypes.emitterMark(TEXTURE), (pose, consumer) -> {
                  Vector3f cam = camLocal(pose);
                  beginBatch(pose);

                  for(int i = glowFrom; i < drawCount; i += 7) {
                     if (!(TR[i] < 0.0F) && !(retractEase >= 1.0F)) {
                        float a = PHASE[i] + timeTicks * SPEED[i];
                        float radius = RADIUS[i];
                        float ca = Mth.cos((double)a) * radius;
                        float sa = Mth.sin((double)a) * radius;
                        float x = UX[i] * ca + VX[i] * sa;
                        float y = UY[i] * ca + VY[i] * sa + CY[i];
                        float z = UZ[i] * ca + VZ[i] * sa;
                        cube(pose, consumer, x, y, z, SIZE[i], (int)(TR[i] * 0.18F), (int)(TG[i] * 0.18F), (int)(TB[i] * 0.18F), light, i, timeTicks, cam.x, cam.y, cam.z);
                     }
                  }

               });
            }
         }
      }
   }

   private static Vector3f camLocal(PoseStack.Pose pose) {
      return (new Matrix4f(pose.pose())).invert().transformPosition(new Vector3f(0.0F, 0.0F, 0.0F));
   }

   private static void beginBatch(PoseStack.Pose pose) {
      Matrix4f m = pose.pose();
      p00 = m.m00();
      p01 = m.m01();
      p02 = m.m02();
      p10 = m.m10();
      p11 = m.m11();
      p12 = m.m12();
      p20 = m.m20();
      p21 = m.m21();
      p22 = m.m22();
      p30 = m.m30();
      p31 = m.m31();
      p32 = m.m32();
      Vector3f n = pose.transformNormal(0.0F, 1.0F, 0.0F, new Vector3f());
      nrmX = n.x;
      nrmY = n.y;
      nrmZ = n.z;
   }

   private static void cube(PoseStack.Pose pose, VertexConsumer c, float x, float y, float z, float h, int r, int g, int b, int light, int i, float t, float camX, float camY, float camZ) {
      float ax = SPINX[i] * t + SPINPX[i];
      float ay = SPINY[i] * t + SPINPY[i];
      float az = SPINZ[i] * t + SPINPZ[i];
      float cx = Mth.cos((double)ax);
      float sx = Mth.sin((double)ax);
      float cy = Mth.cos((double)ay);
      float sy = Mth.sin((double)ay);
      float cz = Mth.cos((double)az);
      float sz = Mth.sin((double)az);
      float ux = cy * cz * h;
      float uy = cy * sz * h;
      float uz = -sy * h;
      float vx = (cz * sx * sy - cx * sz) * h;
      float vy = (cx * cz + sx * sy * sz) * h;
      float vz = cy * sx * h;
      float wx = (cx * cz * sy + sx * sz) * h;
      float wy = (cx * sy * sz - cz * sx) * h;
      float wz = cx * cy * h;
      float mmm_x = x - ux - vx - wx;
      float mmm_y = y - uy - vy - wy;
      float mmm_z = z - uz - vz - wz;
      float pmm_x = x + ux - vx - wx;
      float pmm_y = y + uy - vy - wy;
      float pmm_z = z + uz - vz - wz;
      float ppm_x = x + ux + vx - wx;
      float ppm_y = y + uy + vy - wy;
      float ppm_z = z + uz + vz - wz;
      float mpm_x = x - ux + vx - wx;
      float mpm_y = y - uy + vy - wy;
      float mpm_z = z - uz + vz - wz;
      float mmp_x = x - ux - vx + wx;
      float mmp_y = y - uy - vy + wy;
      float mmp_z = z - uz - vz + wz;
      float pmp_x = x + ux - vx + wx;
      float pmp_y = y + uy - vy + wy;
      float pmp_z = z + uz - vz + wz;
      float ppp_x = x + ux + vx + wx;
      float ppp_y = y + uy + vy + wy;
      float ppp_z = z + uz + vz + wz;
      float mpp_x = x - ux + vx + wx;
      float mpp_y = y - uy + vy + wy;
      float mpp_z = z - uz + vz + wz;
      float tx = camX - x;
      float ty = camY - y;
      float tz = camZ - z;
      if (ux * tx + uy * ty + uz * tz > 0.0F) {
         quad(pose, c, pmm_x, pmm_y, pmm_z, pmp_x, pmp_y, pmp_z, ppp_x, ppp_y, ppp_z, ppm_x, ppm_y, ppm_z, r + 3, g + 3, b + 3, light);
      } else {
         quad(pose, c, mmp_x, mmp_y, mmp_z, mmm_x, mmm_y, mmm_z, mpm_x, mpm_y, mpm_z, mpp_x, mpp_y, mpp_z, r + 3, g + 3, b + 3, light);
      }

      if (vx * tx + vy * ty + vz * tz > 0.0F) {
         quad(pose, c, mpm_x, mpm_y, mpm_z, ppm_x, ppm_y, ppm_z, ppp_x, ppp_y, ppp_z, mpp_x, mpp_y, mpp_z, r + 12, g + 12, b + 12, light);
      } else {
         quad(pose, c, mmp_x, mmp_y, mmp_z, pmp_x, pmp_y, pmp_z, pmm_x, pmm_y, pmm_z, mmm_x, mmm_y, mmm_z, r, g, b, light);
      }

      if (wx * tx + wy * ty + wz * tz > 0.0F) {
         quad(pose, c, pmp_x, pmp_y, pmp_z, mmp_x, mmp_y, mmp_z, mpp_x, mpp_y, mpp_z, ppp_x, ppp_y, ppp_z, r + 6, g + 6, b + 6, light);
      } else {
         quad(pose, c, mmm_x, mmm_y, mmm_z, pmm_x, pmm_y, pmm_z, ppm_x, ppm_y, ppm_z, mpm_x, mpm_y, mpm_z, r + 6, g + 6, b + 6, light);
      }

   }

   private static void quad(PoseStack.Pose pose, VertexConsumer c, float ax, float ay, float az, float bx, float by, float bz, float cx2, float cy2, float cz2, float dx, float dy, float dz, int r, int g, int b, int light) {
      vert(pose, c, ax, ay, az, 0.0F, 0.0F, r, g, b, light);
      vert(pose, c, bx, by, bz, 1.0F, 0.0F, r, g, b, light);
      vert(pose, c, cx2, cy2, cz2, 1.0F, 1.0F, r, g, b, light);
      vert(pose, c, dx, dy, dz, 0.0F, 1.0F, r, g, b, light);
   }

   private static void vert(PoseStack.Pose pose, VertexConsumer c, float x, float y, float z, float u, float v, int r, int g, int b, int light) {
      c.addVertex(p00 * x + p10 * y + p20 * z + p30, p01 * x + p11 * y + p21 * z + p31, p02 * x + p12 * y + p22 * z + p32).setColor(Math.min(255, r), Math.min(255, g), Math.min(255, b), 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nrmX, nrmY, nrmZ);
   }

   static {
      RandomSource r = RandomSource.create(1461142354L);

      for(int i = 0; i < 5880; ++i) {
         float nx = r.nextFloat() * 2.0F - 1.0F;
         float ny = r.nextFloat() * 2.0F - 1.0F;
         float nz = r.nextFloat() * 2.0F - 1.0F;
         float nl = Math.max(0.001F, Mth.sqrt(nx * nx + ny * ny + nz * nz));
         nx /= nl;
         ny /= nl;
         nz /= nl;
         float ux = ny * 0.0F - nz * 1.0F;
         float uy = nz * 0.0F - nx * 0.0F;
         float uz = nx * 1.0F - ny * 0.0F;
         float ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
         if (ul < 0.01F) {
            ux = 1.0F;
            uy = 0.0F;
            uz = 0.0F;
            ul = 1.0F;
         }

         ux /= ul;
         uy /= ul;
         uz /= ul;
         float vx = ny * uz - nz * uy;
         float vy = nz * ux - nx * uz;
         float vz = nx * uy - ny * ux;
         UX[i] = ux;
         UY[i] = uy;
         UZ[i] = uz;
         VX[i] = vx;
         VY[i] = vy;
         VZ[i] = vz;
         SPEED[i] = (0.03F + r.nextFloat() * 0.12F) * (float)(r.nextBoolean() ? 1 : -1);
         PHASE[i] = r.nextFloat() * ((float)Math.PI * 2F);
         SIZE[i] = 0.045F + r.nextFloat() * 0.155F;
         CY[i] = 6.0F + r.nextFloat() * 18.0F;
         SHADE[i] = 0.05F + r.nextFloat() * 0.1F;
         SPINX[i] = (0.004F + r.nextFloat() * 0.018F) * (float)(r.nextBoolean() ? 1 : -1);
         SPINY[i] = (0.004F + r.nextFloat() * 0.018F) * (float)(r.nextBoolean() ? 1 : -1);
         SPINZ[i] = (0.004F + r.nextFloat() * 0.018F) * (float)(r.nextBoolean() ? 1 : -1);
         SPINPX[i] = r.nextFloat() * ((float)Math.PI * 2F);
         SPINPY[i] = r.nextFloat() * ((float)Math.PI * 2F);
         SPINPZ[i] = r.nextFloat() * ((float)Math.PI * 2F);
         if (r.nextBoolean()) {
            float pink = r.nextFloat();
            TR[i] = Mth.lerp(pink, 168.0F, 255.0F);
            TG[i] = Mth.lerp(pink, 52.0F, 116.0F);
            TB[i] = Mth.lerp(pink, 246.0F, 200.0F);
         } else {
            TR[i] = -1.0F;
         }

         if (i >= 2500 && i < 3400) {
            boolean inner = r.nextFloat() < 0.65F;
            RADIUS[i] = inner ? 6.0F + r.nextFloat() * 12.0F : 14.0F + r.nextFloat() * 14.0F;
            RTARGET[i] = -1.0F;
         } else if (i >= 5540) {
            UX[i] = 1.0F;
            UY[i] = (r.nextFloat() - 0.5F) * 0.3F;
            UZ[i] = 0.0F;
            VX[i] = 0.0F;
            VY[i] = (r.nextFloat() - 0.5F) * 0.3F;
            VZ[i] = 1.0F;
            RADIUS[i] = 4.6F + (r.nextFloat() - 0.5F) * 3.8F;
            CY[i] = 2.4F + (r.nextFloat() - 0.5F) * 4.6F;
            SPEED[i] = 0.055F * (0.55F + r.nextFloat() * 0.95F) * (r.nextFloat() < 0.18F ? -1.0F : 1.0F);
            SIZE[i] = 0.115F;
            EARLY_DUE[i - 5540] = 0.5F + 3.3F * ((float)(i - 5540) / 340.0F);
            RTARGET[i] = -1.0F;
         } else if (i >= 5120) {
            UX[i] = 1.0F;
            UY[i] = 0.0F;
            UZ[i] = 0.0F;
            VX[i] = 0.0F;
            VY[i] = 0.0F;
            VZ[i] = 1.0F;
            RADIUS[i] = 33.0F + (r.nextFloat() - 0.5F) * 6.0F;
            CY[i] = 24.0F + (r.nextFloat() - 0.5F) * 5.0F;
            SPEED[i] = 0.078F * (0.94F + r.nextFloat() * 0.12F);
            SIZE[i] = 0.06F + r.nextFloat() * 0.15F;
            RTARGET[i] = -1.0F;
         } else if (i >= 4500) {
            UX[i] = 1.0F;
            UY[i] = 0.0F;
            UZ[i] = 0.0F;
            VX[i] = 0.0F;
            VY[i] = 0.0F;
            VZ[i] = 1.0F;
            float out = r.nextFloat();
            RADIUS[i] = 102.4F + 78.0F * out;
            CY[i] = 76.0F + 24.0F * out + (r.nextFloat() - 0.5F) * 12.0F;
            float[] var10000 = SPEED;
            var10000[i] *= 0.2F;
            SIZE[i] = 0.09F + r.nextFloat() * 0.2F;
            RTARGET[i] = -1.0F;
         } else if (i >= 3400) {
            UX[i] = 1.0F;
            UY[i] = 0.0F;
            UZ[i] = 0.0F;
            VX[i] = 0.0F;
            VY[i] = 0.0F;
            VZ[i] = 1.0F;
            float radial = Mth.sqrt(r.nextFloat());
            RADIUS[i] = 1.0F + 128.0F * radial;
            CY[i] = 44.0F + (1.0F - radial) * 26.0F + (r.nextFloat() - 0.5F) * 15.0F;
            RTARGET[i] = -1.0F;
            float[] var25 = SPEED;
            var25[i] *= 0.35F;
            SIZE[i] = 0.1F + r.nextFloat() * 0.24F;
         } else if (i < 1600) {
            boolean inner = r.nextFloat() < 0.65F;
            RADIUS[i] = inner ? 6.0F + r.nextFloat() * 12.0F : 14.0F + r.nextFloat() * 14.0F;
            RTARGET[i] = -1.0F;
         } else {
            int k = i - 1600;
            RADIUS[i] = 2.0F + r.nextFloat() * 4.0F;
            RTARGET[i] = 34.0F + r.nextFloat() * 14.0F;
            RDELAY[i] = (float)k * 0.085F + r.nextFloat() * 0.05F;
            CY[i] = 8.0F + r.nextFloat() * 14.0F;
         }
      }

      RING_HOME = new HashMap();
      EARLY_PHASE = new HashMap();
      nrmY = 1.0F;
   }
}
