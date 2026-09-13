# Devouring Storms 7000.0.0-MCSM-CINEMATIC-FINAL -- procedural cinematic atmosphere, native Halo cleanup, and phase-7 Vortex

This candidate includes the 1.9.315 active-sky ownership and transparent
cloud changes, plus the same exact phase maps in the built-in core shader and
the shader-pack-v5 fallback. It also removes the old artificial zenith rim and
darkening overlay that could appear as a black discontinuity at the top.

This remains a CI artifact until the active scene is confirmed in Minecraft.

# Devouring Storms 1.9.315 -- active sky ownership and transparent exact-map pass

- Suppress the opaque FabricSkyBoxes backdrop before the sky pass only while a
  phase-5+ storm is active; calm and ordinary summon scenes keep their skybox.
- Reduce the directional cloud alpha target to 0.58 (below the 0.80 cap), so
  vanilla clouds and background motion remain visible through the core.
- Apply the requested exact phase maps in Java and shader paths.
- Write both uStormPos and u_StormPos carrier names.
- Request blurred/clamped sampling for storm_white.png.

This remains a CI artifact until the active scene is confirmed in Minecraft.

# Devouring Storms 1.9.314 -- stability follow-up for the confirmed cloud

The 1.9.311 log confirms the new purple/pink atmospheric layer is visually
close to the reference, but the run still dies in the optional shadow path:
Intel UHD reaches 145,000 storm vertices plus more than 80,000 ground vertices
per shadow submission before the JVM fails to reserve 2.16 GB of G1 virtual
space.

- Keep the confirmed Atmospheric W's Cloud and its fixed far directional
  projection unchanged.
- Disable only the optional trailer/storm/terrain shadow passes that cause the
  native-memory failure on this graphics path.
- The phase sky, storm model, beams, eyes, and atmospheric cloud remain active.

The previous 1.9.313 artifact remains superseded by this CI-only stability
build until the game survives a ground-level phase 5.9 test.

# Devouring Storms 1.9.312 -- remove the 3D circle and unify active sky

The 1.9.311 in-game report identified two separate problems: the remaining
world-space glare/ring geometry still looked like a giant circle that could be
approached, and the normal sky colour remained peach/black when the custom sky
route was enabled.

- Removed the remaining structured glare, ground pool, vortex meshes, and
  orbiting ring submissions. Atmospheric W's Cloud is now the only replacement
  atmosphere geometry.
- Moved the Java cloud projection to a fixed camera-relative far shell instead
  of the storm's physical distance, so it cannot become a 3D object beside the
  storm.
- Corrected the angular mapping that was using tan(58..88 degrees), which
  spread the patch around the sphere as a giant circular shape.
- Applied the active phase 5/5.5/6/8 sky deck to the vanilla SkyRenderState and
  suppress the regular dynamic skybox during the active storm; regular summon
  skyboxes return outside the storm.
- Removed the expensive ring/vortex geometry that contributed to the Intel UHD
  native-memory crash.

This remains CI-only until the active sky is visually checked in Minecraft.

# Devouring Storms 1.9.311 -- remove legacy circular backdrop

The 1.9.310 screenshot exposed one remaining old render path: the purple
round world-anchored Vortex Backdrop card was still being submitted beside
the storm. It was not Atmospheric W's Cloud and made the result look like a
distant circle.

- Removed the legacy VORTEX_BACKDROP quad from the structured glare pass.
- Atmospheric W's Cloud remains only in the infinite directional sky paths.
- The phase-colored cloud and its semi-transparent horizon smog are no longer
  competing with that old circular card.

This build is CI-only until the circle is gone and the pink/purple cloud is
visibly wrapped behind the storm in Minecraft.

# Devouring Storms 1.9.310 -- crown placement for Atmospheric W's Cloud

This placement correction follows the visual clarification that the cloud is
not merely centered on the storm's midpoint. The dense cloud crown now sits
above the Wither Storm while its broad, flat lower shoulder wraps across the
horizon behind the storm.

- Added a shared `0.22` top lift to the core GLSL, managed shader-pack, and
  Java fallback paths.
- Kept the 4x horizontal spread and 0.5x vertical compression, so the cloud
  remains massive around the centered horizon rather than becoming a small
  spot or a detached upper blob.
- Preserved infinite directional projection, semi-transparent 0.80 density
  cap, phase-color bleed-through, and shredded 3-octave FBM edges.

This build is CI-only until the crown/back/horizon placement is checked in
Minecraft.

# Devouring Storms 1.9.309 -- Atmospheric W's Cloud

This pass replaces the old geometric glare/smudge interpretation with the
actual visual structure from the supplied Wither Storm screenshots: a wide,
flat, asymmetric atmospheric cloud wrapping across the horizon.

- Renamed the visual concept to `Atmospheric W's Cloud`; it remains an
  infinite directional skybox effect, not a finite world object.
- Rewrote the active GLSL cloud field as a 4x horizontal / 0.5x vertical
  stretched weather layer using a non-circular box field and 3-octave FBM
  edge shredding. No `length(uv)` radial oval or doughnut bounds remain.
- Added semi-transparent dark matter density capped at `0.80`, with the
  required `pow(noise, 2.0)` falloff and `mix(phaseColor, vec3(0.02),
  densityAlpha)` phase-color bleed-through.
- Updated both the core sky shader and managed shader-pack path, including
  correct alpha compositing instead of the previous opaque occlusion formula.
- Renamed the control-panel labels while retaining old config keys for
  compatibility.

This build is CI-only until the new atmospheric-cloud result is checked in
Minecraft.

# Devouring Storms 1.9.308 -- enlarged storm-attached sky smear

This corrective build follows the 1.9.307 in-game check. The storm sky was
visible and correctly shaped, but its angular footprint still read as a small
dot floating behind the Wither Storm.

- Enlarged the directional smear substantially in every render path so its
  color mass sits directly behind and around the complete storm silhouette.
- Kept the 2.5x horizontal multiplier, phase 5/5.5/6 ramps, asymmetrical
  feeding tongues, and 2D FBM/torn contour.
- Broadened the alpha transition and reduced the side/outer coverage so the
  enlarged field still dissolves into the regular sky instead of becoming a
  solid dome.
- Retained the normalized `u_StormProximity` carrier and separate regular
  summon skybox textures.
- Bumped the installed build identity to `1.9.308-26.2-beta-ds`.

This build is intentionally CI-only until the enlarged result is checked in
Minecraft.

# Devouring Storms 1.9.307 -- alpha storm-sky patch, no legacy Fabric sky

This corrective build addresses the 1.9.306 screenshots. The previous build
painted a camera-centred full dome, which appeared as a giant green/purple
sphere and could cover the storm with the legacy FabricSkyBoxes backdrop.

- Removed the opaque full-sky dome from the no-shader/Fabric path. The storm
  backdrop is now only the large, alpha-feathered organic patch attached just
  behind the storm, so the reference sky can blend through its edges instead
  of ending at a hard sphere boundary.
- Temporarily suppresses the legacy FabricSkyBoxes day/night/sunset override
  only while an in-range phase-5+ storm is active. The phase storm patch then
  owns the backdrop; regular summon skyboxes return outside that storm window.
- Kept the asymmetric FBM silhouette, unequal side feeding, internal colour
  tongues, and close-to-storm shell placement; expanded the patch rather than
  expanding an opaque full-screen dome.
- Bumped the installed build identity to `1.9.307-26.2-beta-ds`.

Delete older Devouring Storms jars and old shader/resource-pack copies before
launching. Confirm the Mods screen shows `1.9.307-26.2-beta-ds`.

# Devouring Storms 1.9.306 -- reference-shaped infinite skybox smear

This build is based on the verified 1.9.305 line, not the older 1.9.200
shader-only jar. It targets the screenshots showing a clean circular colour
grade instead of the irregular, layered paint mass around the Wither Storm.

- Reworked the core GLSL, FabricSkyBoxes, and active shader-pack blob paths
  to use the same asymmetric FBM/domain-warped silhouette: unequal side
  tongues, a low shoulder, an upper notch, torn edges, and internal colour
  tongues. It is no longer a direct circle or a single radial grade.
- Pulled the angular footprint closer to the storm bearing (38..60 degrees
  before the size setting) while retaining the direction-only/infinite
  behaviour: flying toward the skybox cannot reach a back face.
- Kept the corrected phase palettes and blended them through the broken
  contour: phase 5 teal/moss, phase 5.5-5.9 violet/magenta, and phase 6
  dusty rose/amber vertical split.
- Bumped the player-facing build to 1.9.306 so the installed jar cannot be
  confused with the old 1.9.200 or the unmodified 1.9.305 release.

Install the 1.9.306 jar by deleting older `devouringstorms-*.jar` files from
`mods/` first. The Minecraft mods screen must show
`1.9.306-26.2-beta-ds`.

# Devouring Storms 1.9.305 (prior) -- the blob becomes a true infinite skybox smear

**Why 1.9.305:** the blob looked like a flat disc floating in the world.

**The problem:** on profiles where iris (or another shader mod) is merely
INSTALLED with its pack turned off, the mod routed the blob to
McsmBlobOval's four world-anchored oval quads -- 2D cards pinned at the
storm's 3D position. That is exactly the "giant purple disc with a dark
centre floating in mid air, extremely far away" you reported. A shader
mod being present is not the same as a shader pack being IN USE.

**The fix -- the blob is now a separate infinite skybox layer:**
* the pack gate now asks IrisApi whether a pack is actually rendering the
  sky, so vanilla pipelines (including iris with shaders disabled and
  FabricSkyBoxes) always get the true sky layer;
* the world-anchored quads are GONE on every path. The blob is painted as
  an organic, noise-warped smear (an oval with two warped side lobes,
  feathered edges, uniform body alpha, internal streak shading) on a
  camera-centred sky sphere -- the sunrise-band / aurora construction you
  described: infinite, tethered to the storm's bearing, gliding with it,
  never approaching, with the vanilla sky visible above and around it;
* the corrected 2026-09-11 hex decks (dark core, phase-5 greenish rim,
  phase-5.5 royal magenta, phase-6 sunset split) are baked into the smear
  on a noise-jittered radius, so the bands read as smeared paint, not
  clean rings;
* with a real shader pack active, the same smear draws at the storm's
  distance (skybox depth), so every configuration now shows one identical
  sky-blob.

**After updating:** load any world during phases 5.0-6.95 and look at the
storm. The blob is a huge messy purple smear across the sky behind the
storm -- walk sideways and it stays glued to the storm; fly at it and it
never gets closer (that is the infinite-skybox tell). Beyond ~1600 blocks
from the storm it fades out and the regular sky returns.

## 1. Everything from the other line (1.9.201-1.9.220) is IN
- **Story Mode NPCs (McsmNpcs)**: the canonical cast spawns at towns and
  sites, with dialogue, talk/laugh acting and phase-crossing voice lines.
- **StoryCharacter humanoid entity** with 22 cast skins and its renderer.
- **Real Telltale models**: Stage A / Stage B / Stage C Small/Big/Massive /
  Stage D massive, the severed storm, the head, voxelised from the real
  meshes (bbmodel -> jem), plus the per-phase growth slider.
- **REAL Telltale Vortex model ported** (McsmVortexMesh) over the
  procedural swirls, debris vortex, glacier tornado flakes, cube rings.
- **OG look skins + charcoal-indigo atlas pass** on all 18 skin atlases.
- **Teeth/eye glow root fix**: bloomStrength hard-floor 2.5, glowStrength
  1.0, forced turquoiseTeeth/headEyeGlow, mini-storm teeth stay lit.
- **Built-in shader pack auto-select + install fix**: the new pack replaces
  the old one on update -- this is what previously left the OLD
  orange/yellow sky on screen even after a new jar was installed.
- shader default+auto-select, structured glare revamp (sphere deleted),
  story water, per-phase model growth, tentacle girth, debris maxing.

## 2. This line's sky work is IN (and stays the sky you approved)
- **Infinite Skybox Blob** with the CORRECTED hexes: phase 5
  `#161A1D/#2D423F/#6A9A78` (green), phase 5.5-5.9
  `#0B0410/#2D1442/#581C6E/#87529C` (purple & pink void), phase 6
  `#1A1226/#462A52/#966173/#D89874` (four-colour sunset split).
- **Opaque, alpha-blended**: the dark core is near-opaque and truly darkens
  the sky; the smudge rings bleed outward with smoothstep. NO vanilla
  distance fog on the storm sky, ever (opaque cinematic layer).
- Blob on EVERY path: core GLSL sky pass (vanilla), McsmStormSkyLayer
  (FabricSkyBoxes on), McsmBlobOval (Iris shader-pack path -- the built-in
  pack). Fully procedural, GL_LINEAR-equivalent, zero pixels.
- **Day sky**: lavender-blue per the reference. **Night sky**: deep NAVY,
  never purple. 700-1600 block distance fade back to vanilla sky.
- **Teeth/eyes**: phase 5 = pure white with white aura, 5.5-5.9 cyan-white,
  6 = greenish-blue (more blue), 7+ = green-white. Eye glow no longer dims
  to black.

## 3. Version
Mods screen shows **1.9.305-26.2-beta-ds**; title screen, config screens
and `/ds` show build **1.9.305**. Verified by CI annotation on the build.

## Assets
- `devouringstorms-1.9.305-26.2-beta-ds.jar` — the mod
- `devouringstorms-shaderpack-v5-1.9.305.zip` — Iris shader pack
- `devouringstorms-storylook-1.9.305.zip` — Story Look resource pack
- `devouringstorms-superduper-default-1.9.305.zip` — Super Duper pack
- `.sha256` checksums for verification

## Installing (so the old jar/pack can never win again)
1. Delete EVERY `devouringstorms-*.jar` from your `mods/` folder.
2. Download `devouringstorms-1.9.305-26.2-beta-ds.jar` from this release
   and put it in `mods/` alone.
3. Delete the old pack folders from `shaderpacks/` (any folder starting
   with `devouringstorms`), then toggle "Built-in Shader Pack" OFF and ON
   once in the MCSM Control Panel -- the new pack reinstalls.
4. In game: the mods screen must show `1.9.305-26.2-beta-ds`. If it shows
   ANY other number, that jar is not this build.
