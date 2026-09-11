# Devouring Storms 1.9.215 — canonical build (Telltale models + Infinite Skybox Blob)

This release is the **1.9.215** build you know, rebuilt with the
**Infinite Skybox Blob** sky overhaul folded in. It supersedes the earlier
`ds-1.9.215` upload (original commit archived as `archive-ds-1.9.215-2b054e1`).

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
  - **Phase 5** — cyan/green emergence: `#1A2223` core, `#2E4544` mid,
    `#7C9885` edge flare, `#8493FF` beam accent;
  - **Phase 5.5–5.9** — purple/pink corruption: `#0F0814` core,
    `#3A1B54` mid, `#5E2775` upper bleed, `#7D4B91` edge;
  - **Phase 6** — four-colour apocalypse split: `#171021` zenith,
    `#44284D` upper-mid, `#A36B73` lower-mid, `#D69776` bottom glow;
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

## 3. Sky revamp
- **Day**: clear vivid mid-blue vault falling to the pale-lilac horizon
  (no more washed lavender zenith).
- **Night**: **purple**, not blue — deep indigo-violet vault with a soft
  lavender-purple horizon glow.
- **Phase 5**: turquoise dome re-keyed to the `#1A2223/#2E4544/#7C9885`
  deck (brightness calibration preserved).
- **Phase 6**: the four-colour sunset split replaces the old grey wash.
- Skybox PNGs (`mcsm_atmosphere/sky/*` and `glare/*`) regenerated from the
  same decks.

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
