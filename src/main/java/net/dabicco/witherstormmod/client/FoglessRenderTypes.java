package net.dabicco.witherstormmod.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import java.util.HashMap;
import java.util.Map;
import net.dabicco.witherstormmod.config.DabyWSClientConfig;
import net.dabicco.witherstormmod.mixin.RenderPipelinesAccessor;
import net.dabicco.witherstormmod.mixin.RenderTypeInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class FoglessRenderTypes {
   private static boolean active = false;
   private static final Map<String, RenderPipeline> CUTOUT_PIPES = new HashMap();
   private static final Map<Float, RenderPipeline> EMISSIVE_PIPES = new HashMap();
   private static final Map<Float, RenderPipeline> EYES_PIPES = new HashMap();
   private static final Map<String, RenderType> TYPES = new HashMap();
   private static final int SUN_STEPS = 64;

   private FoglessRenderTypes() {
   }

   public static void setActive(boolean a) {
      active = a;
   }

   public static boolean fogless() {
      return active && !DabyWSClientConfig.legacyDistantRenderer && !ShaderPackCompat.active();
   }

   private static float fogMix() {
      return DabyWSClientConfig.distantFog ? 0.5F : 0.0F;
   }

   public static boolean modelShading() {
      return DabyWSClientConfig.stormModelShading && !ShaderPackCompat.active();
   }

   private static int sunStep() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return 0;
      } else {
         long t = level.getOverworldClockTime() % 24000L;
         return Math.floorMod((int)(t * 64L / 24000L), 64);
      }
   }

   private static float[] sunVector(int step) {
      double angle = ((double)step + (double)0.5F) / (double)64.0F * Math.PI * (double)2.0F;
      float x = (float)Math.cos(angle);
      float y = (float)Math.sin(angle);
      return new float[]{x, y, 0.0F};
   }

   private static RenderPipeline cutoutPipeline(float fog, boolean reverse) {
      return cutoutPipeline(fog, reverse, modelShading(), modelShading() ? sunStep() : 0);
   }

   private static RenderPipeline cutoutPipeline(float fog, boolean reverse, boolean shade, int sun) {
      return cutoutPipeline(fog, reverse, shade, sun, false);
   }

   private static RenderPipeline cutoutPipeline(float fog, boolean reverse, boolean shade, int sun, boolean cull) {
      return (RenderPipeline)CUTOUT_PIPES.computeIfAbsent(fog + "|" + reverse + "|" + shade + "|" + sun + "|" + cull, (k) -> {
         RenderPipeline.Builder var10000 = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelinesAccessor.dabyws$entitySnippet()});
         String var10001 = tag(fog);
         RenderPipeline.Builder b = var10000.withLocation(id("pipeline/fogless_entity_cutout_" + var10001 + (reverse ? "_rev" : "") + (shade ? "_lit" + sun : ""))).withVertexShader(id("core/fogless_entity")).withFragmentShader(id("core/fogless_entity")).withShaderDefine("ALPHA_CUTOUT", 0.1F).withShaderDefine("FOG_MIX", fog).withShaderDefine("NO_OVERLAY").withShaderDefine("STORM_RIM").withShaderDefine("CHARCOAL_SKIN").withCull(cull);
         if (reverse) {
            b.withShaderDefine("REVERSE_SHADING");
         }

         if (shade) {
            float[] v = sunVector(sun);
            b.withShaderDefine("STORM_SHADING").withShaderDefine("SUN_X", v[0]).withShaderDefine("SUN_Y", v[1]).withShaderDefine("SUN_Z", v[2]);
         }

         return b.build();
      });
   }

   public static boolean reverseShading() {
      return DabyWSClientConfig.reverseShading && !ShaderPackCompat.active();
   }

   private static RenderPipeline emissivePipeline(float fog) {
      return (RenderPipeline)EMISSIVE_PIPES.computeIfAbsent(fog, (f) -> RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelinesAccessor.dabyws$entityEmissiveSnippet()}).withLocation(id("pipeline/fogless_entity_translucent_emissive_" + tag(f))).withVertexShader(id("core/fogless_entity")).withFragmentShader(id("core/fogless_entity")).withShaderDefine("FOG_MIX", f).withShaderDefine("NO_OVERLAY").withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build());
   }

   private static RenderPipeline eyesPipeline(float fog) {
      return (RenderPipeline)EYES_PIPES.computeIfAbsent(fog, (f) -> RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelinesAccessor.dabyws$entityEmissiveSnippet()}).withLocation(id("pipeline/fogless_eyes_" + tag(f))).withVertexShader(id("core/fogless_entity")).withFragmentShader(id("core/fogless_entity")).withShaderDefine("FOG_MIX", f).withShaderDefine("NO_OVERLAY").withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)).withCull(false).build());
   }

   private static RenderType cached(String kind, Identifier tex, float fog, RenderPipeline pipeline, boolean lightmap) {
      return (RenderType)TYPES.computeIfAbsent(kind + "|" + String.valueOf(tex) + "|" + fog + "|" + String.valueOf(pipeline.getLocation()), (k) -> {
         RenderSetup.RenderSetupBuilder b = RenderSetup.builder(pipeline).withTexture("Sampler0", tex);
         if (lightmap) {
            b.useLightmap();
         }

         return RenderTypeInvoker.dabyws$create("dabywitherstormmod:" + kind + ":" + String.valueOf(tex) + ":" + tag(fog), b.createRenderSetup());
      });
   }

   public static RenderType bodyCutout(Identifier texture) {
      return bodyCutout(texture, false);
   }

   public static RenderType bodyCutout(Identifier texture, boolean cull) {
      boolean rev = reverseShading();
      if (cull) {
         float fc = fogless() ? fogMix() : 1.0F;
         return cached("cull_cutout", texture, fc, cutoutPipeline(fc, rev, modelShading(), modelShading() ? sunStep() : 0, true), true);
      } else if (!fogless()) {
         return !rev && !modelShading() ? RenderTypes.entityCutout(texture) : cached(rev ? "rev_cutout" : "lit_cutout", texture, 1.0F, cutoutPipeline(1.0F, rev), true);
      } else {
         float f = fogMix();
         return cached(rev ? "fogless_cutout_rev" : "fogless_cutout", texture, f, cutoutPipeline(f, rev), true);
      }
   }

   public static RenderType entityTranslucentEmissive(Identifier texture) {
      if (!fogless()) {
         return RenderTypes.entityTranslucentEmissive(texture);
      } else {
         float f = fogMix();
         return cached("fogless_emissive", texture, f, emissivePipeline(f), false);
      }
   }

   public static RenderType eyes(Identifier texture) {
      if (!fogless()) {
         return RenderTypes.eyes(texture);
      } else {
         float f = fogMix();
         return cached("fogless_eyes", texture, f, eyesPipeline(f), false);
      }
   }

   private static String tag(float fog) {
      return fog <= 0.0F ? "crisp" : "halffog";
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("dabywitherstormmod", path);
   }
}
