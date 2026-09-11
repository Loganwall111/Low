# Devouring Storms 1.9.215 — canonical build (Telltale models + Infinite Skybox Blob)

This release is the **1.9.215** build you know, rebuilt with the
**Infinite Skybox Blob** sky overhaul folded in, plus **Sky Fix R2**
(2026-09-11): corrected hex decks, navy night, and FabricSkyBoxes
compatibility -- and **Fix R3**: the in-game version is always stamped
correctly (the mods screen previously kept showing the base jar's old
number) and the Wither Storm teeth + eyes now glow with the phase colours.
It supersedes the earlier `ds-1.9.215` uploads (original commit archived as
`archive-ds-1.9.215-2b054e1`, first blob port as
`archive-ds-1.9.215-r1-b48b7d2`, Sky Fix R2 as `archive-ds-1.9.215-r2-81f20ad`).

## 1. The glare is now Telltale's INFINITE SKYBOX BLOB
The Wither Storm glare is not a 3D volume, not a billboard and not a cloud
layer — it is a **separate infinite skybox projection tethered to the
storm** (the same trick as the infinite-hallway portals), painted inside the
sky pass (`gbuffers_sky`):

- pinned to the storm's position (`u_StormPos`), so it travels with it;
- a flat, smeared oval gradient (tilted ellipse, not a circle) with an
  **opaque dark-matter core** that masks the vanilla sky and outer borders
  that blend smoothly into a wide colour flare;
- **infinite fill** — fly up into it and it never ends; look away and the
  sky fades back to normal;
- painted with the exact hex decks, lerped by phase:
  - **Phase 5** — green emergence: `#161A1D` core, `#2D423F` mid,
    `#6A9A78` edge;
  - **Phase 5.5–5.9** — purple & pink void: `#0B0410` core,
    `#2D1442` mid, `#581C6E` upper bleed, `#87529C` edge;
  - **Phase 6** — four-colour apocalypse split: `#1A1226` zenith,
    `#462A52` upper-mid, `#966173` lower-mid, `#D89874` bottom glow;
- **distance fade**: inside 700 blocks the blob is full; from there out to
  1600 blocks it recedes, and beyond that the sky returns to regular
  vanilla ("go extremely far away and it slowly changes back");
- no raymarching, no volumetric fog, no transparent horizon-fog variables —
  pure angular dome-plane projection, fully procedural (zero pixel edges).

## 2. The white thing is GONE
The world-anchored white halo shell (the weird white circular/square mass
that floated in the distance behind the storm) no longer draws. The
infinite skybox blob owns the glare now. The purple vortex backdrop and
ground fog pool are untouched.

## 3. Sky revamp (R2 — corrected decks)
- **Day**: sampled from the skyday reference — lavender-blue vault falling
  to the pale-lilac horizon.
- **Night**: deep **navy blue** (midnight reference) — never purple.
- **Phase 5**: green dome re-keyed to `#161A1D/#2D423F/#6A9A78`.
- **Phase 5.5–5.9**: one purple & pink deck `#0B0410/#2D1442/#581C6E/#87529C`.
- **Phase 6**: the four-colour sunset split
  `#1A1226/#462A52/#966173/#D89874` replaces the old grey wash.
- The same corrected decks now drive ALL sky paths: the core shaders, the
  built-in Iris v5 pack, the Java sky/fog tints, and the skybox PNGs.
- Skybox PNGs (`mcsm_atmosphere/sky/*` and `glare/*`) regenerated from the
  corrected decks.

## 3c. Teeth & eye glow (Fix R3)
- The glow around the teeth and eyes now uses the **phase palette** instead
  of the old hard blue: **phase 5 = pure white with a white aura**,
  phase 5.5-5.9 = cyan-white, **phase 6 = greenish-blue (more blue)**,
  **phase 7+ = green-white**. The mod pushes these colours every tick and
  the glow shader now honours them.
- The eye glow no longer dims to black: the shader previously scaled the
  light by the bound texture's luminance, and the eye atlas tile is dark --
  that is exactly why the eyes read dark. Textures now only shape the light.
- Teeth mark textures retuned: phase 6 marks are bluer, phase 7 marks are
  green-white (classic + OG skins).

## 3d. Version number fix (Fix R3)
The old build stamped the jar's `fabric.mod.json` with a text replacement
that silently did nothing unless the old value was shaped exactly like
`1.9.200-26.2-beta` -- so the mods screen kept showing the base jar's old
number and the game looked like it had loaded a build from before this
release. The build now rewrites the version field as JSON (works for any
old format), stamps **`1.9.215-26.2-beta-ds`**, and the title screen,
config screens and `/ds` chat line all show build **1.9.215** -- the
number of THIS release.

## 3b. FabricSkyBoxes compatibility (NEW)
The mod ships its own FabricSkyBoxes skyboxes (day/night/sunset,
`customSkyboxes` ON by default), which previously covered the storm sky
entirely — the Wither Storm skies only appeared with FabricSkyBoxes
disabled. Now, when the FabricSkyBoxes mod is loaded, the storm dome + the
infinite oval blob are drawn as a far camera-centred layer over the
skybox (same hexes, same angular projection, same 700-1600 block distance
fade, terrain still occludes it), and the base mod's skybox textures are
replaced with the corrected reference decks — so the storm sky renders
with FabricSkyBoxes ENABLED. No vanilla distance fog touches the storm
sky in any path (fully procedural, opaque cinematic layer).

## 4. Everything from the original 1.9.215 carries
- **True 1:1 Telltale models**: Stage A (phase 2.0–4.49), Stage B
  (phase 5.0–5.49) and the severed Deadass mesh, voxelised from the
  extracted bbmodel meshes via `ci/bbmodel_convert.py`;
- the **volumetric GLSL storm deck** (`/lib/mcsm/stormVolume.glsl`,
  Iris uniform push, optional layer);
- every fix from 1.9.208–1.9.214 (structured glare era, cube rings,
  mouth/teeth work, phase textures).

## Assets
- `devouringstorms-1.9.215-26.2-beta-ds.jar` — the mod
- `devouringstorms-shaderpack-v5-1.9.215.zip` — Iris shader pack
- `devouringstorms-storylook-1.9.215.zip` — Story Look resource pack
- `devouringstorms-superduper-default-1.9.215.zip` — Super Duper pack
- `.sha256` checksums for verification
