# MINECRAFT: STORY MODE — Official Atmosphere Shaderpack (1.21.2 / 26.2)
Standalone atmosphere shaderpack for **Iris** (Fabric) and **OptiFine** (Java Edition).

## Namespace unification
Every custom sky/environment asset resolves from ONE synchronized directory so
Sodium and Iris can never flash between mismatched namespaces:
- time-of-day skyboxes: `assets/minecraft/optifine/sky/world0/` (`sky_day`, `sky_sunset`, `sky_night`, `sky_midnight`, thunder-keyed `sky_storm`);
- the dark backdrop sheets live under the mod's own namespace
  (`assets/dabywitherstormmod/textures/environment/sky/`) and are bound into
  the pack via `customTexture.darkBackdrop`;
- the old protective shield sphere is retired (the mod gates it behind the
  OFF-by-default `shieldDome` toggle; the dead `blueHalo` texture binding is
  removed). MCSM halos are thin hard rings, drawn as cube geometry by the mod.
`gbuffers_skybasic` + `gbuffers_skytextured` sample the LIVE `worldTime`
uniform (with `sunAngle`/`sunPosition` fallbacks) so the clock never locks at
tick 0. The pack ships a single lowercase `shaders/lang/en_us.lang` and a
single `shaders/block.properties` — root-level duplicates are removed.

## How the packs fit together (single, zero-conflict pipeline)
The cloud and sky look is delivered by **two coordinated layers**:

1. **The mod JAR itself** (`assets/dabywitherstormmod/shaders/core/rendertype_clouds.{vsh,fsh}`)
   runs the vanilla-core cloud pipeline: the vertex stage decodes the vanilla
   `CloudFaces` buffer with the 2.5x extrusion and passes `worldPosCoord` to a
   100% mathematical fragment stage. This is the layer that always renders,
   with or without a shader pack. (The resource pack deliberately ships NO
   `shaders/core/` copies and NO `clouds.png` — those overrides are hidden the
   moment an Iris shaderpack loads and only cause namespace collisions.)
2. **This shader pack** (`gbuffers_clouds.{vsh,fsh}` + `rendertype_clouds.{vsh,fsh}`)
   replaces the cloud program *inside Iris* with the same handwritten
   CloudFaces-decode vertex math and procedural noise in the fragment stage.
   No PNG cloud sheets, no `cloudTex0-7` samplers — the shader itself is the
   cloud, so turning the pack on can never blank the sky. `shaders.properties`
   keeps `clouds=fast` routing.

## Features
- **Iris Shader Options unlocked**: root `shaders.properties` + `shaders/shaders.properties`
  route the pipeline (`clouds=fast`, `customSkies=true`, `shadowMapResolution=2048`) and
  bind the custom materials (`witherFlesh`, `tornFlesh`, `darkBackdrop`).
- **100% procedural clouds**: blocky fbm noise mapped over `worldPosCoord`, live
  `uniform long worldTime` day/night palettes, distance haze via `vertexDistance`.
- **Dark backdrop hardcoded in-pack**: `gbuffers_skytextured` blends the bound
  dark purple-and-black atmosphere sheet (`shaders/textures/environment/sky/`) into
  the lower sky dome on shader initialization — no resource pack required.
- **Matte materials**: near-black `witherFlesh` / `tornWitheredFlesh` voxel
  sheets and entity skins grade to charcoal-indigo and catch one restrained
  indigo edge light — no specular lobe, no glint band. MCSM flesh is flat
  dark matter with crisp rims, never glossy metal.
- **Story Mode Colored Lighting** (always ON): warm golden sunlight,
  lavender ambient shadows and amber torchlight across terrain, water AND
  entities (`MCSM_LIGHTING`), with the emissive turquoise teeth aura
  bypassing the lightmap so the teeth burn at night.
- **Bright Story Mode water**: lifted ambient/sun terms and softer shadows.
- **Hand item alpha masking**: `gbuffers_hand` discards transparent texels so
  held tools never render as solid black voids.

## Installation
1. Install **Iris + Sodium** (recommended) or OptiFine.
2. Copy `MCSM_ShaderPack.zip` into `.minecraft/shaderpacks/` (DO NOT unzip).
3. In Minecraft: Video Settings -> Shader Packs -> select **MCSM_ShaderPack**.
4. For the full Story Mode look keep the MCSM resource pack enabled too — the
   mod's own assets (skyboxes, skins, presets) now live inside the mod JAR.
