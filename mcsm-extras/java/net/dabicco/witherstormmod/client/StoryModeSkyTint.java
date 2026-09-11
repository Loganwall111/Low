package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.mcsm.extras.client.McsmStormAtmosphere;

/**
 * Calm day/night/dusk fog + sky from mcsm_atmosphere calm strips.
 * Purple NEVER on calm night — only McsmStormAtmosphere when storm phase >= 5.
 */
public final class StoryModeSkyTint {
   // 1.9.221 (port) -- user 2026-09-11: revamp the day sky (clear vivid
   // mid-blue) and make the NIGHT sky PURPLE, not blue (matches the
   // midnight reference PNG). Purple only on the night strip; day stays
   // blue-lavender and storm purple still comes from McsmStormAtmosphere.
   private static final float[] SKY_DAY = new float[]{0.26F, 0.42F, 0.80F};
   // user midnight strip -- deep indigo-violet now (was navy 0.005/0.012/0.140)
   private static final float[] SKY_NIGHT = new float[]{0.030F, 0.020F, 0.100F};
   private static final float[] SKY_DUSK = new float[]{0.090F, 0.100F, 0.340F};
   private static final float[] SKY_DAWN = new float[]{0.160F, 0.200F, 0.460F};
   private static final float[] LIGHT_DAY = new float[]{0.92F, 0.90F, 0.85F};
   // 1.9.221 (port): night light leans indigo-violet to match the purple sky
   private static final float[] LIGHT_NIGHT = new float[]{0.28F, 0.20F, 0.62F};
   private static final float[] LIGHT_DUSK = new float[]{0.55F, 0.62F, 0.92F};
   private static final float[] LIGHT_DAWN = new float[]{0.72F, 0.76F, 0.95F};
   private static final float[] HORIZON_DAY = new float[]{0.55F, 0.60F, 0.80F};
   private static final float[] HORIZON_DUSK = new float[]{0.100F, 0.120F, 0.400F};
   private static final float[] HORIZON_NIGHT = new float[]{0.10F, 0.07F, 0.36F};
   private static final float[] HORIZON_DAWN = new float[]{0.200F, 0.240F, 0.520F};

   private static final float[] TMP = new float[3];

   private StoryModeSkyTint() {
   }

   private static void mix(float[] out, float[] a, float[] b, float t) {
      out[0] = a[0] + (b[0] - a[0]) * t;
      out[1] = a[1] + (b[1] - a[1]) * t;
      out[2] = a[2] + (b[2] - a[2]) * t;
   }

   private static float ease(float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static float skyCityBlend() {
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null || mc.player == null) return 0.0F;
         // Sky City atmosphere starts high but ramps gently, with support for
         // the user's million-block cloud-stack concept without showing it
         // from normal ground play.
         return ease((float)((mc.player.getY() - 220.0D) / 420.0D));
      } catch (Throwable ignored) {
         return 0.0F;
      }
   }

   private static void mixTowardSkyCity(float[] out, float strength) {
      if (strength <= 0.001F) return;
      float[] blue = {0.42F, 0.70F, 1.0F};
      out[0] = out[0] + (blue[0] - out[0]) * strength;
      out[1] = out[1] + (blue[1] - out[1]) * strength;
      out[2] = out[2] + (blue[2] - out[2]) * strength;
   }

   private static void byTime(long clockTime, float[] day, float[] dusk, float[] night, float[] dawn, float[] out) {
      float t = (float)(clockTime % 24000L);
      if (t < 1500.0F) {
         mix(out, dawn, day, ease(t / 1500.0F));
      } else if (t < 10500.0F) {
         out[0] = day[0]; out[1] = day[1]; out[2] = day[2];
      } else if (t < 12500.0F) {
         mix(out, day, dusk, ease((t - 10500.0F) / 2000.0F));
      } else if (t < 14000.0F) {
         mix(out, dusk, night, ease((t - 12500.0F) / 1500.0F));
      } else if (t < 22000.0F) {
         out[0] = night[0]; out[1] = night[1]; out[2] = night[2];
      } else {
         mix(out, night, dawn, ease((t - 22000.0F) / 2000.0F));
      }
   }

   public static void skyColor(long clockTime, float[] out) {
      byTime(clockTime, SKY_DAY, SKY_DUSK, SKY_NIGHT, SKY_DAWN, out);
      // storm atmosphere owns purple/pink/teal — calm never does
      try {
         float b = McsmStormAtmosphere.skyBlend(TMP);
         if (b > 0.01F) {
            out[0] = out[0] + (TMP[0] - out[0]) * b;
            out[1] = out[1] + (TMP[1] - out[1]) * b;
            out[2] = out[2] + (TMP[2] - out[2]) * b;
         }
      } catch (Throwable ignored) {
      }
      mixTowardSkyCity(out, skyCityBlend() * 0.42F);
   }

   public static void lightColor(long clockTime, float[] out) {
      byTime(clockTime, LIGHT_DAY, LIGHT_DUSK, LIGHT_NIGHT, LIGHT_DAWN, out);
   }

   public static void horizonColor(long clockTime, float[] out) {
      byTime(clockTime, HORIZON_DAY, HORIZON_DUSK, HORIZON_NIGHT, HORIZON_DAWN, out);
      try {
         float b = McsmStormAtmosphere.skyBlend(TMP);
         if (b > 0.01F) {
            // horizon picks up more pink on 5.5
            out[0] = out[0] + (Math.min(1.0F, TMP[0] * 1.25F) - out[0]) * b;
            out[1] = out[1] + (TMP[1] * 0.9F - out[1]) * b;
            out[2] = out[2] + (TMP[2] - out[2]) * b;
         }
      } catch (Throwable ignored) {
      }
      mixTowardSkyCity(out, skyCityBlend() * 0.55F);
   }

   public static void blockLightColor(float[] out) {
      out[0] = 1.0F; out[1] = 0.826F; out[2] = 0.56F;
   }

   public static float fogStrength() {
      float base = DabyWSClientConfig.storyModeSky
            ? Mth.clamp((float)DabyWSClientConfig.storyModeFogStrength * 0.42F, 0.0F, 0.42F) : 0.0F;
      try {
         float b = McsmStormAtmosphere.skyBlend(TMP);
         float skyCity = skyCityBlend();
         // storm fog denser; high-altitude Sky City adds clean blue haze.
         return Mth.clamp(base + b * 0.35F + skyCity * 0.28F, 0.0F, 0.85F);
      } catch (Throwable t) {
         return base;
      }
   }

   public static float strength() {
      return DabyWSClientConfig.storyModeSky
            ? Mth.clamp((float)DabyWSClientConfig.storyModeSkyStrength, 0.0F, 1.0F) : 0.0F;
   }

   public static float lightStrength() {
      return DabyWSClientConfig.storyModeLighting
            ? Mth.clamp((float)DabyWSClientConfig.storyModeLightingStrength, 0.0F, 1.0F) : 0.0F;
   }
}
