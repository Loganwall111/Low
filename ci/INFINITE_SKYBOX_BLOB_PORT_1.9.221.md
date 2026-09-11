# Infinite Skybox Blob — ported onto 1.9.220 → 1.9.221

The first port of the Infinite Skybox Blob landed on the stale 1.9.200-era
tree (session base). The mod has since moved through 1.9.214/1.9.215
(Telltale models, structured glare, cube rings) to 1.9.220. This commit
re-applies the whole feature set onto the CURRENT codebase (tag `ds-1.9.220`),
merging with the other agent's work instead of overwriting it.

## What was ported

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
  dusk/dawn values from the other agent are kept.
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

## Not touched (other agent's territory)
- `shaderpack-v5` / `shaderpack-v4` / `shaderpack-superduper` (their Iris
  infinite blob `McsmSkyBlob` + skyBlob.glsl and the optional volumetric
  layer stay as-is — under Iris their blob runs; without Iris the core
  blob from this port runs).
- `McsmSkyDome` (already a no-op), `McsmStormRings`, `McsmVortexMesh`,
  CEM models, teeth/emissive work, phase textures.
- 1.9.208 dusk/dawn and phase 8-9 ember decks (kept in the Java retunes).

## Validation
- `python3 glslcheck/shimcheck.py mcsm-core-shaders` → 56/56 pass.
- All new Java surfaces are already-verified APIs (same as 1.9.201 port).
- VERSION bumped to 1.9.221 (next build: `devouringstorms-1.9.221-26.2-beta-ds`).
