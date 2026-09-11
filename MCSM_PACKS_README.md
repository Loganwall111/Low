# Minecraft: Story Mode Official Visuals & Packs (26.2)

Complete visual recreation of **Minecraft: Story Mode** by Telltale Games and
Mojang Studios, engineered for modern Minecraft **26.2** (Fabric / Iris /
Sodium / OptiFine).

---

## 📦 r1 Deliverables

The **r1** release rebuilds the whole atmosphere stack. Downloads live in
[`docs/releases/r1/`](docs/releases/r1/) (repo-staged artifacts; GitHub
release-asset uploads are blocked from the build sandbox, so the CI-built jar
is published as an Actions artifact):

| Package | File | Install into | What it is |
| :--- | :--- | :--- | :--- |
| **Wither Storm Mod** | `dabywitherstormmod-1.9.61-26.2-beta-r1.jar` | `.minecraft/mods/` | Mod with bundled skyboxes, procedural cloud vsh, Vortex mesh, storm atmosphere post-chain, OG shaded textures + `_e` emissive pairs. Renamed per build (`-r{run}`). |
| **MCSM Resource Pack** | `MCSM_ResourcePack.zip` | `.minecraft/resourcepacks/` | Shaded OG Story Mode textures, Day/Sunset/Night/Midnight + thunder-storm skybox set, `rendertype_clouds.vsh` (2.5x extrusion), `emissive.properties` for the turquoise teeth aura. |
| **MCSM Shader Pack** | `MCSM_ShaderPack.zip` | `.minecraft/shaderpacks/` | **100% procedural GLSL clouds** (no PNG sheets), sun-cast shadows on ground & water that sweep with the day/night clock, dynamic sky, colored lighting, turquoise teeth bloom. |

Checksums: `docs/releases/r1/SHA256SUMS.txt`.

---

## 🔗 Single-package integration (zero-conflict pipeline)

The cloud/sky stack now ships **inside the mod JAR itself** under
`assets/dabywitherstormmod/` so it works as one standalone package:

* `assets/dabywitherstormmod/shaders/core/rendertype_clouds.{vsh,fsh}` — the
  vanilla-core cloud program (CloudFaces decode + 2.5x extrusion + procedural
  fbm noise over `worldPosCoord`). This renders with **no shader pack at all**.
* The Iris shader pack mirrors the same math in its own `gbuffers_clouds` /
  `rendertype_clouds` programs, so enabling the pack swaps the *implementation*,
  never the look — the shader itself is the cloud, and there are no PNG buffers
  for the GPU to choke on.
* The dark purple-and-black backdrop is hardcoded in-pack under
  `shaders/textures/environment/sky/` and blended by `gbuffers_skytextured` on
  shader initialization, independent of the resource pack.
* Phase timeline: crisp cubic halo rings (white under-halo 4+, cataclysm
  ring 5.8+, clockwise trio 6+, counter-rotating 7+, triple inferno systems
  8–9 — the old shield dome is OFF by default), pink/magenta post fog
  (Phases 5.1–5.9), Phase 6 orange backdrop swap, maximized purple flares
  on Phase 7, red/dark-orange inferno sky on Phase 8–9.

## ✨ What r1 actually changed

### Clouds are now real shaders — zero PNG cloud sheets
* **No more `cloudTex0..7` texture bindings.** The 8 PNG cloud sheets and all
  `customTexture.cloudTex*` entries were deleted from the shader pack.
* `gbuffers_clouds.fsh` / `rendertype_clouds.fsh` are **procedural**: fractal
  value-noise (`hash13`/`fbm`), world-anchored, drifting with the game clock.
* `gbuffers_clouds.vsh` / `rendertype_clouds.vsh` **unproject** vertices to
  world space and apply the Story Mode 2.5x vertical extrusion
  (`scaledVertex.y *= 2.5`, `CloudHeight = 2.5`) with per-face brightness
  (sunlit top, lavender ambient sides, shadowed bottom).
* **Live time-of-day colour**: day reads white/coral, sunset orange, night
  periwinkle — driven by the running `worldTime` clock.

### Restored Story Mode skybox loop
* `SkyRendererMixin` re-tints the vanilla sky dome: **lavender zenith with a
  warm orange horizon** that follows the storm's phase — green at phase 4.5,
  turquoise at phase 5, purple/magenta/black through the cataclysm.
* The OptiFine custom skyboxes (`sky_day`, `sky_sunset`, `sky_night`,
  `sky_midnight` + the thunder-keyed `sky_storm` for phases 4–7) ship in both
  the resource pack and the mod jar; the world clock keeps running (never
  frozen at tick 0). Fabric skyboxes are gone — this set is the only one.

### Storm atmosphere as a true post-effect
* `post_effect/storm_atmosphere.json` + `shaders/post/storm_atmosphere.fsh`
  run a **full-screen post pass** (`StormAtmosphere`, hooked into
  `LevelRendererBloomMixin`) — purple → dark-magenta atmospheric fog gradient.
* The old solid 3D spherical shell (`WitherShieldSphere`) and its halo
  billboard were **deleted**. No solid shells, no texture walls: every storm
  atmosphere element is translucent/glow geometry or a screen-space pass.

### Phase FX (all GLSL/shader-style, no 3D assets)
* Thin white cubic under-halo beneath the storm from phase 4 (blue-purple
  cataclysm ring joins at 5.8); both die out as phase 8 burns in.
* Giant colour-shifting centre blob (phase 5.1 → 5.9): dark purple → magenta →
  pink/blue/black-purple nested soft shells.
* Heavy magenta/purple/pink/black rear fog layer attached to the storm's back
  (phase 5.1+), moving with it.
* Phase 6+: bright pulse **directly above the storm every 2 minutes**
  (2400-tick window, quick rise / slow fade).
* Phase 4+: the **abduction vortex** — a 560-cube tornado helix plus real
  block-crack particles ripped off the ground under the storm, thickening
  into a full tornado by phase 6.

### Fog / sky per phase
* Palette anchors: purple gloom → **green (4.5)** → turquoise (5+) →
  purple/pink drained cataclysm sky (5.45+). Driven by `StormPalettes`
  (4-anchor blend) and read by fog, sky tint, cloud tint, and the post pass.

### Shadows, lighting, held items
* New `shadow.vsh/fsh` sun shadow-map pass; `gbuffers_terrain.fsh` and
  `gbuffers_water.fsh` sample `shadowtex0` — **cast shadows sweep the ground
  and water** with the day/night cycle.
* `gbuffers_entities.fsh` keeps the **turquoise emissive teeth glow** (+
  magenta accents).
* Held-item alpha transparency preserved (`gbuffers_hand*` discard).

---

## 🎮 Recommended In-Game Settings

1. **Video Settings -> Quality -> Custom Sky**: `ON`
2. **Video Settings -> Quality -> Sky / Sun & Moon**: `ON`
3. **Video Settings -> Shader Packs -> MCSM_ShaderPack -> Shader Options**:
   - **Cloud Rendering**: `fast` (procedural GLSL)
   - **Custom Skies**: `ON`
   - Story Mode colored lighting, the turquoise teeth glow and the matte
     charcoal skin grade are compiled into the pack — always ON.

---

## 🏗️ Building & release

* `.github/workflows/build.yml` compiles the mod jar (Java 25 + Fabric Loom),
  renames it per build, and uploads it as a CI artifact.
* `.github/workflows/mcsm-release.yml` (on `main`) additionally rebuilds the
  two packs through `tools/build_mcsm_packs.py` and force-uploads the three
  artifacts over the release tag.
* `tools/build_mcsm_packs.py` packages the committed pack directories as flat
  zips and **fails hard if any PNG cloud sheet or `cloudTex` binding sneaks
  back in, or if the Day/Sunset/Night/Midnight/Storm skybox set is incomplete**.
* `tools/merge_release_jar.py` produces the repo-staged r1 jar (original
  classes + merged resources, no `geo/` Blockbench sources, no `ffmpeg`).
