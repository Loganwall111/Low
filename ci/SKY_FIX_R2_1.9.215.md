# Sky Fix R2 — corrected hex decks, navy night, FabricSkyBoxes compatibility

User report (2026-09-11, second pass): the storm glare still read as a flat
washed-out horizon fog; gradients and shapes were off vs the references; the
night sky was still purple; the day sky rendered the old look; and the
Wither Storm skies did not render at all with FabricSkyBoxes enabled.

## Root causes found

1. **Three sky systems, three different palettes.** The mod has three paths
   that paint the sky and they disagreed: the core shader (assets/minecraft/
   shaders, used when no shader pack and no skybox mod), the built-in Iris
   v5 pack (default ON), and the base mod's own FabricSkyBoxes skyboxes
   (`assets/fabricskyboxes/…`, `customSkyboxes = true` by default). Each
   carried different day/night/storm colours, so "the ones you made and the
   ones that are still in the thing" looked inconsistent.
2. **FabricSkyBoxes hides the storm sky.** The base mod ships opaque FBS
   skyboxes (day/night/sunset). With FBS loaded they draw over the vanilla
   sky pass, so the core-shader storm dome and the infinite blob were
   invisible — exactly "not rendering with fabric skyboxes on".
3. **Wrong hexes.** The first pass used older palette values. This pass
   applies the CORRECTED reference hexes everywhere.
4. **Night purple.** The earlier retune made the night violet; the midnight
   reference is deep navy. Reverted to navy.

## Fixes applied (all sky paths now share the same decks)

### CORRECTED hex decks (user spec)
- Phase 5 green: `#161A1D` core / `#2D423F` bleed / `#6A9A78` edge
- Phase 5.5-5.9 purple & pink void: `#0B0410` core / `#2D1442` smoke /
  `#581C6E` high-altitude / `#87529C` horizon border
- Phase 6 four-colour split: `#1A1226` zenith / `#462A52` upper-mid /
  `#966173` lower-mid / `#D89874` bottom glow

### Files changed
- `mcsm-core-shaders/include/mcsm_visuals.glsl` — blob decks, moderated
  smear shape (1.55x0.90, 0.18 rad tilt), beam accent removed.
- `mcsm-core-shaders/core/sky.fsh` (+ `position.fsh` twin) — day sampled
  from the skyday reference, night = deep navy (never purple), storm dome
  stops re-keyed to the corrected decks (5.5-5.9 holds one deck).
- `shaderpack-v5/shaders/gbuffers_skybasic.fsh` — calm day/night decks and
  `storyStormSky` re-keyed to the same decks (the pack is default ON, so
  this is the path most players actually see).
- Java decks: `StoryModeSkyTint` (navy night restored, day per reference),
  `StormSkyDome` + `McsmStormAtmosphere` (corrected hexes).
- **NEW `McsmStormSkyLayer`** — FabricSkyBoxes compatibility layer: when
  the FBS mod is loaded and `customSkyboxes` is on, it draws the full
  phase dome + the infinite oval blob as a far camera-centred shell AFTER
  the skybox (the same angular projection math as the GLSL blob, same
  hexes, same 700-1600 block distance fade, terrain occluding it through
  the depth test). Never runs otherwise, so the core shader stays the
  single owner when FBS is absent.
- `ci/regenerate_skybox_pngs.py` — rebuilt every sky/glare PNG from the
  corrected decks, plus NEW jar-overrides that replace the base mod's
  FabricSkyBoxes textures (day/night/twilight), its `textures/sky/*`
  panels and the Story Mode environment strips, so the FBS-on calm skies
  match the references too.

### Vanilla fog
The storm sky never uses vanilla distance fog in any path: the core-shader
storm branch replaces the sky outright, the v5 storm function derives only
colour keys from fog colour, and the new FBS layer is opaque alpha-blended
geometry. The blob is fully procedural (no texture sampling), so there are
no pixel edges and no sampler filtering to worry about.

## Validation
- `python3 glslcheck/shimcheck.py mcsm-core-shaders` — 56/56 pass.
- New Java surfaces are existing verified APIs (GlowRenderTypes.translucent,
  ClientDistantStormManager, DabyWSClientConfig.customSkyboxes, etc.).
- VERSION stays 1.9.215; the release is republished after CI verifies.
