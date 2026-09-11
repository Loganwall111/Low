# Infinite Skybox Blob — pinned to 1.9.215 → 1.9.215.1

The user designated **1.9.215** as the correct version of the mod
(`ds-1.9.215` release). This branch is now based exactly on tag
`ds-1.9.215` (commit `2b054e1`), and the full Infinite Skybox Blob + sky
revamp port is re-applied on top of it.

The earlier port onto 1.9.220 is preserved as tag
`port-1.9.221-2d2450c` if the 220 line is ever wanted back.

## Version naming

- Base: `ds-1.9.215` (the user's designated version, release exists).
- This port: `1.9.215.1` — the patch dot keeps the artifact distinct
  (the next CI build produces `devouringstorms-1.9.215.1-26.2-beta-ds`
  and the tag `ds-1.9.215.1`) instead of re-releasing over the existing
  `ds-1.9.215`.
- The 1.9.216-1.9.220 line (other agent: teeth fixes, vortex mesh,
  phase textures, their Iris-side blob) is intentionally NOT in this
  branch. Everything this port touches is byte-identical between 215 and
  220 (verified: core shaders, the six Java files, the sky/glare PNGs),
  so the port can be re-applied onto any newer tag with the same patches.

## What the port contains (same feature set as before)

### 1. The Infinite Skybox Blob — core (no-Iris) sky pass
`mcsm-core-shaders/include/mcsm_visuals.glsl` + `core/sky.fsh`:
- `mcsm_blob()` is the infinite skybox projection tethered to
  `u_StormPos` (`witherstorm_BossPos` / the aim carrier): smeared tilted
  oval (1.90×0.95, 0.31 rad), opaque dark-matter core that masks the
  vanilla sky, mid bleed, outer flare blending back to the regular sky;
  flying into it never ends, looking away fades to normal.
- Exact 2026-09-11 hex decks:
  - P5 `#1A2223` / `#2E4544` / `#7C9885` + `#8493FF` beam accent
  - P5.5-5.9 `#0F0814` / `#3A1B54` / `#5E2775` / `#7D4B91`
  - P6 four-color split `#171021` / `#44284D` / `#A36B73` / `#D69776`
    keyed to ray elevation
- Window widened to 5.00–6.95 (cloud-deck occlusion matches).
- `mcsm_blob_color()` (terrain rim) retuned to the same decks.
- The 1.9.175 Sky City strata twin in `position.fsh` is preserved.
- GLSL gate: 56/56 pass.

### 2. The white thing — DELETED
The world-anchored **volumetric halo shell** in
`McsmStormBlob.submitStructuredGlare()` (two nested ellipsoid layers,
additive `glow(HALO_TEX)`, tinted pale cyan-white at phase 4-5) was the
weird white circular/square mass in the distance. Its draw calls are gone;
`emitHaloShell` stays in the source as dormant code. The vortex backdrop
and the ground fog pool (both purple-tinted, matching the reference frames)
are untouched.

### 3. Sky revamp (user: "fix the daytime sky", "night sky is purple not blue")
- `sky.fsh` `SKY_DAY`: clear vivid mid-blue zenith → pale lilac horizon.
- `sky.fsh` `SKY_NIGHT`: **purple** indigo-violet vault (was blue).
- `sky.fsh` phase-5 dome stop re-keyed to the turquoise hex deck; phase 6
  is now the four-color sunset split instead of the old grey wash.
- `StoryModeSkyTint`: day/night/horizon strips retuned; the 1.9.208
  dusk/dawn values are kept.
- `McsmStormAtmosphere`: phase decks re-keyed to the hexes; the 1.9.208
  phase 8-9 ember block is kept.
- `StormSkyDome`: decks aligned.

### 4. Java driver — `McsmInfiniteSkyboxBlob` (new)
Wired into `McsmBlobCarrierPatch` (skipped during the death cinematic):
tracks the nearest storm, smooths the aim 25 %/frame, distance-fades the
blob between 700 and 1600 blocks ("go extremely far away and the sky
slowly changes back to regular vanilla").

### 5. Skybox PNGs
`ci/regenerate_skybox_pngs.py` rebuilt `mcsm_atmosphere/sky/*` and
`glare/*` (both `src/main/resources` and `jar-overrides`) from the new
decks — day, purple night, phase-5 turquoise, 5.4, 5.5, phase-6 split and
matching radial smudges. The glare PNGs are not referenced by any current
Java renderer (the structured glare used `halo.png`), so replacing them is
safe; drop the user's original uploads over these filenames to use them
verbatim.

## Not touched (other agent's territory, all present in the 215 base)
- The 1.9.215 Telltale model ladder (Stage A / Stage B / Deadass severed
  via bbmodel→jem voxeliser), `ci/bbmodel_convert.py`.
- The 1.9.215 volumetric GLSL storm deck (`stormVolume.glsl`) and the
  Iris uniform push (`McsmSkyBlob`).
- `McsmSkyDome` (already a no-op), `McsmStormRings`, CEM models, teeth /
  emissive work, phase textures.
- 1.9.208 dusk/dawn and phase 8-9 ember decks (kept in the Java retunes).

## Validation
- `python3 glslcheck/shimcheck.py mcsm-core-shaders` → 56/56 pass.
- All new Java surfaces are already-verified APIs (same as before).
- VERSION bumped to 1.9.215.1 (next build:
  `devouringstorms-1.9.215.1-26.2-beta-ds`).
