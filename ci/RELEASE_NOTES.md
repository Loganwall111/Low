# Devouring Storms 1.9.182 — lower-memory shaders, vanilla water, pack extraction

## Fixes

* **Disabled wavy/water shader features by default** in the managed Super Duper pack: water animation, water normal waves, water noise, foam, stylized absorption, and physics-ocean support are off so water behaves much closer to vanilla.
* **Lowered shader memory pressure** by default: disabled SSR, volumetric lighting, colored/filtered shadows, and reduced default cloud/AA load. This targets the Sodium native-buffer allocation crash reported with shaders enabled.
* **Calm night sky is now dark blue instead of pink/purple** in the managed Super Duper overworld settings. Pink/purple is reserved for Wither Storm phase 5.5+ atmosphere, not normal nighttime.
* **Installs Story Look and OGS CEM as real resource-pack zips** into the instance `resourcepacks/` folder and writes them into `options.txt`, because the screenshots showed they were not selected in the normal Resource Packs screen.
* **Shift+C now works from menus too**, not only when no screen is open, so testing it from the config screen should open the Devouring Storms panel.

---

# Devouring Storms 1.9.181 — built-in OGS pack auto-enabled and audited

## Conflict checks

* **Registers the built-in OGS CEM resource pack as default-enabled** alongside Story Look, instead of merely embedding it in the jar. This is the piece that can make the restored model/resource-pack assets visible when compatible model/resource-pack loaders are present.
* **Adds hard jar audits for the exact conflict symptoms:** required OGS texture/model paths must exist in the assembled jar, and the stale `MCSM extras 1.9.95` visible label must not survive assembly.
* Keeps the 1.9.180 live build-number button and the 1.9.179 restored OGS assets.

---

# Devouring Storms 1.9.180 — stale config label patched

## UI / install diagnostics

* **Patched the stale base config label** that still printed `MCSM extras 1.9.95` inside the original config screen even when the fresh jar was loaded. The build now rewrites that base class constant to the current Devouring Storms version during assembly.
* **The fixed bottom-left control-panel button now includes the live build number**, making it obvious which jar is loaded.
* Keeps the restored OGS Wither Storm assets from 1.9.179.

---

# Devouring Storms 1.9.179 — original OGS Wither Storm assets restored

## Models / textures

* **Restored the original OGS Wither Storm asset set** from `Loganwall111/ogs-stuff/witherstormmod`: phase CEM models, segment/torn/dismantled models, head/body textures, pulse/emissive overlays, tentacle texture, tractor-beam particle, and OGS `colors.json`.
* **Mirrored the assets into both namespaces**: `assets/witherstormmod/...` for the original OGS resource-pack layout and `assets/dabywitherstormmod/...` for this mod jar's runtime texture lookups.
* **Added root texture aliases used by the current renderer** (`textures/entity/wither_storm.png`, `wither_storm_og.png`, and emissive phase aliases) so the storm does not fall back to a black/missing-texture silhouette when the old root assets are absent.
* **Updated the built-in OG CEM resource pack** with the complete OGS CEM model list under both `witherstormmod` and `dabywitherstormmod`, while preserving the existing pack metadata.

Note: if the config screen still says `MCSM extras 1.9.95`, Minecraft is loading an older jar/cache. This release identifies as `1.9.179` in the Devouring Storms control panel and in the mod metadata.

---

# Devouring Storms 1.9.178 — first-spawn NBT world arrival

## Structures

* **First world arrival now uses the new NBT summon path.** On the first overworld tick with a player present, the mod attempts to summon the converted Story Mode blueprint world exactly where that player spawned/currently stands.
* **Removed the old first-spawn dependency on broken `.schematic` files.** `McsmEpisodeSpawnMixin` now calls `McsmTemplateSummoner` / `StructureTemplateManager`; it no longer queues the legacy EnderCon schematic.
* **Players are delivered into the summoned world once per session.** If the converted NBT files are present, first arrival gets the Episode One message and is teleported into the summoned spawn world.
* **Fails open when uploads are still missing.** If `sky_city.nbt` and `beacontown.nbt` have not been generated yet, normal spawning is left alone and the manual `/ds towns summon` command reports the missing blueprint.

---

# Devouring Storms 1.9.177 — broken schematics purged, NBT world summon path

## Structures

* **Old broken `.schematic` assets are purged from the assembled mod jar** during CI packaging. The legacy MCEdit schematic folder from the base jar is removed before release so the mod cannot silently keep using bad structures.
* **Added `ci/convert_story_worlds.py`**, an NBT automation converter using Python `nbtlib`/nbttag-style parsing. It targets `world_data_temp/MC105/` and `world_data_temp/MC201/`, crops the standalone structural bounds, and writes `sky_city.nbt` plus `beacontown.nbt`.
* **Writes both requested and runtime locations**: `src/main/resources/assets/dabywitherstormmod/structures/` for the requested asset path and `src/main/resources/data/dabywitherstormmod/structure/` for Minecraft `StructureTemplateManager` runtime loading.
* **Added Java StructureTemplateManager summoning code**: `McsmTemplateSummoner` places the converted templates, and `/ds towns summon [world|beacontown|sky_city|all]` summons them at the player/spawn location.
* **Added an item implementation template**: `McsmStructureSummonerItemTemplate` shows the clean custom Item hook for a future registered story-world summoner item.

Note: this workspace did not currently contain `world_data_temp/MC105/` or `world_data_temp/MC201/`, so the converter was executed and reported those folders missing. Add/upload those folders and rerun `python3 ci/convert_story_worlds.py --require` to generate the final `.nbt` blueprints.

---

# Devouring Storms 1.9.176 — Super Duper shaderpack embedded as managed default

## Merge

* **Imported the user-provided `here-here` shaderpack source** into `shaderpack-superduper/` and mirrored it into `overrides/shaderpacks/Super_Duper_Devouring_Storms_Default/`.
* **The mod jar now embeds this pack as the managed Iris/Oculus default**. On launch, the built-in shaderpack installer writes `DevouringStorms-SuperDuperDefault.zip` into the instance `shaderpacks/` folder and selects it when Iris is available and no player-chosen pack is already selected.
* **The original Devouring Storms shaderpack remains available** as `shaderpack-v5` and as a release asset, but the managed default inside the mod now comes from the Super Duper pack source you linked.
* **No-Iris fallback still stays merged into the mod** through `mcsm-core-shaders/` plus the built-in Story Look resource pack.

---

# Devouring Storms 1.9.175 — million-block stacked cloud strata

## Fixes / Visuals

* **1024 logical stacked cloud layers** now run through the Story Mode sky system from y=192 up to y=1,000,000. They are mathematically sampled instead of brute-forcing one thousand draw calls.
* **Gigantic void gaps** separate the stack clusters: every 64-layer band only has a short visible deck cluster, followed by a long empty gap, so the high clouds do not become a solid wall.
* **Ground view is protected**: high-altitude strata are camera-height gated and horizon-hidden, so normal gameplay keeps the usual nearby Story Mode clouds instead of seeing the million-block layers from the surface.
* **No-Iris and Iris paths both updated**: the built-in/default core shader, Story Look resource pack, and Point of No Return Iris shaderpack all share the high-strata treatment.

---

# Devouring Storms 1.9.174 — permanent Story Mode clouds

## Fixes

* **Shader clouds no longer disappear**: the Iris/Oculus `gbuffers_clouds` pass now owns a persistent Story Mode cloud layer with a safe alpha floor, white/lavender colour, storm tint, and procedural softness. Turning shaders on should not wipe the Story Mode cloud read anymore.
* **Regular non-Iris clouds protected too**: the vanilla/core resource-pack cloud vertex shader now clamps the fade math so real cloud faces cannot fade to full transparent just because of camera height or shader pipeline differences.
* **Merged default look stays in the mod**: Story Look remains embedded/built into the jar for no-shader play, and the override copies were refreshed from the same sources.

---

# Devouring Storms 1.9.173 — version sync + Shift+C quick menu fix

## Fixes

* **Version metadata synced**: source `fabric.mod.json`, `gradle.properties`, runtime `BUILD_VERSION`, and `VERSION` now all identify the build as Devouring Storms instead of the old Dabicco 1.9.60 metadata. The mod id stays `dabywitherstormmod` for save/config compatibility.
* **Shift+C quick access implemented**: added a client tick mixin that opens the Devouring Storms / MCSM Control Panel in-game with Shift+C. The 1.9.172 notes mentioned this shortcut, but the polling mixin was missing, so players could install a newer jar and still see old behavior.
* **Winter Storm overrides refreshed**: the merged override copies of Story Look and the Point of No Return Iris shaderpack are refreshed from the latest 1.9.172 assets.

---

# Devouring Storms 1.9.172 — The Point of No Return

## Atmospheric VFX, Enhanced AI, Speaking Cast & Shaders Overhaul

### 🌟 Atmosphere & Multi-Color Glare
* **Multi-Color Radial Glare**: Replaced solid single-hue glare discs with rich multi-color radial gradients:
  - Phase 4: Electric icy-cyan core with deep indigo falloff
  - Phase 5: Vibrant blue center blending into cosmic purple with soft magenta highlights
  - Phase 5.4: Smooth indigo-to-purple transition
  - Phase 5.5: Dark outer perimeter, rich blue interior, dark purple halo matching ground truth frames
  - Phase 6: Deep cosmic purple to dark violet cataclysmic aura
* **Non-Euclidean Storm Glare**: Glare disc can now be world-anchored in the storm's local frame (`glareNonEuclidean`), allowing players to traverse behind the storm with true 3D spatial depth.
* **Smooth Glare & Body Animations**: Config-driven pulsation, orbital sway, and phase-1 eye/jaw rhythmic throbbing.

### 👥 Story Mode Inhabited Towns & Speaking Cast
* **Town Populations**: Ground structures and towns now spawn the canonical Story Mode cast (Jesse, Petra, Axel, Olivia, Lukas, Gabriel, Ivor, Soren, Ellegaard, Magnus, Radar, Stella, Harper, etc.).
* **Dialogue Progression**: Right-clicking NPCs advances story dialogue trees per player with ambient voice tones.
* **Animations**: Natural walking, wandering, head tracking towards players, and speaking particle bursts.

### 🌌 Sky & Dimension Overhauls
* **Aurora Borealis Ribbons**: 4-color shimmering curtains (Blue, Pink, Purple, Orange) rippling across night skies.
* **Snow Biome Celestial Band**: Gigantic icy-blue atmospheric arch encircling the sky dome in cold/snow biomes.
* **Twinkling Multi-Colored Stars**: Dynamic twinkling star field with varied cosmic hues.
* **Night Shooting Comets**: Periodic luminous shooting star streaks across the night dome.
* **End Sky Cosmic Vortex**: Deep void black sky with a gigantic swirling purple vortex and dimensional reality rips along the horizon.

### 🔮 Lighting & Particle VFX
* **Portal Illumination**: Nether portals emit purple atmospheric glow and swirl motes; End portals emit dark void particles.
* **Beacon Corona**: Radiant cyan light halo around active beacons.
* **Nether Atmosphere**: Deep crimson fog and rising sparks/embers over lava lakes.
* **Underwater Ambience**: Subtle crepuscular god rays and deep blue haze.
* **Magical Sparkles**: Shimmering white, pink, and purple particles around storm bodies and magical anchors.

### ⚡ Enhanced Wither Storm AI
* **Menacing Threat Tracking**: Prioritizes players holding beacons, formidibombs, or nether stars.
* **Combat Aggression**: Predictive tentacle slams, roar cues, and aggressive pursuit.

### 🎛 Control Panel & Settings
* **MCSM Control Panel**: Complete scrollable in-game menu covering all visual, atmospheric, gameplay, and AI parameters.
* **Quick Access**: Accessible via Wither Storm settings or Shift+C shortcut.
