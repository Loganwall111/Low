package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.util.Mth;

/**
 * Calm day/night/dusk fog + sky colour for Story Mode look.
 * 1.9.152: palettes matched to user storymode_sky_* strips.
 * Night/midnight is deep navy-blue — NEVER phase-5.4 purple/pink.
 * Purple vault lives only in McsmPhaseSky (phases 5.4–5.9 near the storm).
 */
public final class StoryModeSkyTint {
   // day strip: sky-blue zenith
   private static final float[] SKY_DAY = new float[]{0.353F, 0.627F, 0.863F};
   // midnight strip: deep navy (NOT purple)
   private static final float[] SKY_NIGHT = new float[]{0.031F, 0.051F, 0.310F};
   // sunset strip: teal vault / orange belly sampled as dusk average
   private static final float[] SKY_DUSK = new float[]{0.494F, 0.220F, 0.180F};
   private static final float[] SKY_DAWN = new float[]{0.620F, 0.420F, 0.380F};
   private static final float[] LIGHT_DAY = new float[]{1.0F, 0.98F, 1.0F};
   private static final float[] LIGHT_NIGHT = new float[]{0.42F, 0.52F, 0.95F};
   private static final float[] LIGHT_DUSK = new float[]{1.0F, 0.72F, 0.52F};
   private static final float[] LIGHT_DAWN = new float[]{1.0F, 0.86F, 0.82F};
   // day strip horizon: muted lilac
   private static final float[] HORIZON_DAY = new float[]{0.651F, 0.616F, 0.741F};
   // sunset strip horizon: crimson
   private static final float[] HORIZON_DUSK = new float[]{0.494F, 0.098F, 0.165F};
   // midnight strip horizon: bright blue glow
   private static final float[] HORIZON_NIGHT = new float[]{0.220F, 0.392F, 0.933F};
   private static final float[] HORIZON_DAWN = new float[]{0.860F, 0.560F, 0.480F};

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

   private static void byTime(long clockTime, float[] day, float[] dusk, float[] night, float[] dawn, float[] out) {
      float t = (float)(clockTime % 24000L);
      if (t < 1500.0F) {
         mix(out, dawn, day, ease(t / 1500.0F));
      } else if (t < 10500.0F) {
         out[0] = day[0];
         out[1] = day[1];
         out[2] = day[2];
      } else if (t < 12500.0F) {
         mix(out, day, dusk, ease((t - 10500.0F) / 2000.0F));
      } else if (t < 14000.0F) {
         mix(out, dusk, night, ease((t - 12500.0F) / 1500.0F));
      } else if (t < 22000.0F) {
         out[0] = night[0];
         out[1] = night[1];
         out[2] = night[2];
      } else {
         mix(out, night, dawn, ease((t - 22000.0F) / 2000.0F));
      }
   }

   public static void skyColor(long clockTime, float[] out) {
      byTime(clockTime, SKY_DAY, SKY_DUSK, SKY_NIGHT, SKY_DAWN, out);
   }

   public static void lightColor(long clockTime, float[] out) {
      byTime(clockTime, LIGHT_DAY, LIGHT_DUSK, LIGHT_NIGHT, LIGHT_DAWN, out);
   }

   public static void horizonColor(long clockTime, float[] out) {
      byTime(clockTime, HORIZON_DAY, HORIZON_DUSK, HORIZON_NIGHT, HORIZON_DAWN, out);
   }

   public static void blockLightColor(float[] out) {
      out[0] = 1.0F;
      out[1] = 0.826F;
      out[2] = 0.56F;
   }

   public static float fogStrength() {
      return DabyWSClientConfig.storyModeSky ? Mth.clamp((float)DabyWSClientConfig.storyModeFogStrength, 0.0F, 1.0F) : 0.0F;
   }

   public static float strength() {
      return DabyWSClientConfig.storyModeSky ? Mth.clamp((float)DabyWSClientConfig.storyModeSkyStrength, 0.0F, 1.0F) : 0.0F;
   }

   public static float lightStrength() {
      return DabyWSClientConfig.storyModeLighting ? Mth.clamp((float)DabyWSClientConfig.storyModeLightingStrength, 0.0F, 1.0F) : 0.0F;
   }
}
