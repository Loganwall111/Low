package net.dabicco.witherstormmod.client;

import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.minecraft.util.Mth;

public final class StoryModeSkyTint {
   // mega-phase 7b: day/noon bluish gradient per frames; night is deep indigo
   // (the previous SKY_NIGHT read as a wrong purple midnight)
   private static final float[] SKY_DAY = new float[]{0.318F, 0.545F, 0.867F};
   private static final float[] SKY_NIGHT = new float[]{0.007F, 0.013F, 0.058F}
   private static final float[] SKY_DUSK = new float[]{0.69F, 0.4F, 0.4F};
   private static final float[] SKY_DAWN = new float[]{0.76F, 0.56F, 0.66F};
   private static final float[] LIGHT_DAY = new float[]{1.0F, 0.98F, 1.0F};
   private static final float[] LIGHT_NIGHT = new float[]{0.50F, 0.58F, 0.95F};
   private static final float[] LIGHT_DUSK = new float[]{1.0F, 0.78F, 0.64F};
   private static final float[] LIGHT_DAWN = new float[]{1.0F, 0.86F, 0.9F};
   private static final float[] HORIZON_DAY = new float[]{0.678F, 0.835F, 0.949F};
   private static final float[] HORIZON_DUSK = new float[]{0.945F, 0.573F, 0.404F};
   private static final float[] HORIZON_NIGHT = new float[]{0.030F, 0.062F, 0.178F};
   private static final float[] HORIZON_DAWN = new float[]{0.96F, 0.66F, 0.76F};

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
