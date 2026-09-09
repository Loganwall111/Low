# Devouring Storms 1.9.191 — corrected phase-6 texture atlases and crash-safe shader defaults

This build fixes the 1.9.190 texture miss: the body atlas was pushed too blue everywhere, and the head glow path was still not guaranteed to see the current phase before choosing the phase-6 atlas.

* **Reverted the over-blue body flood** and replaced it with a more faithful phase-6 style: mostly black/blue-black silhouette with sparse deep sapphire rectangular scratches/panels.
* **Added phase-specific phase-4/head/body atlases** (`phase_4_assets_p55/p6/p7` and OG variants) so phase 5.5, 6 and 7 can use different body/teeth palettes instead of one shared texture.
* **Added phase-specific devourer atlases** (`devourer_assets_p55/p6/p7` and OG variants) for the split storm body.
* **Rebuilt emissive teeth maps from only the bright teeth/mouth pixels**, instead of tinting the whole lower half cyan. This should stop the “whole thing blue” look and make the teeth colour stand out.
* **Forced storm/head renderers to publish the current phase before texture selection**, so detached heads and body parts choose the correct phase-6/phase-7 texture path.
* **Crash pressure reduced again**: bundled Super Duper bloom is now off by default, volumetric strength is zero by default, lens flare remains off, underwater caustics remain off, storm shadow heightmap is off, and debris particle/default pressure is capped lower.
* I also generated a reference texture concept with image generation, but kept the in-game atlas UV-safe instead of directly pasting the generated sheet over the model UVs.

# Devouring Storms 1.9.190 — Story Mode water, phase-dynamic teeth/beams, NPC spawn egg, and storm sway


This build starts the next larger pass from your reference frames.

* **Blue-black Wither Storm body skin pass**: normal/OG body atlases and phase/devourer atlases are pushed toward the dark bluish-black Story Mode look, while preserving the charcoal body shape.
* **Phase-specific teeth behavior**: phase 3 has no teeth glow, phase 4 glows only slightly, phase 5 stays flat white, phase 5.5 glows white, phase 6 glows blue/cyan, and phase 7+ shifts to green-blue glow.
* **New phase-7 emissive teeth textures** plus a transparent no-glow emissive texture for pre-glow phases.
* **Tractor beams now recolor with storm phase/day-night atmosphere** instead of staying one solid pink/purple all the time.
* **Standalone Story Mode Character Spawn Egg** registered as `dabywitherstormmod:story_character_spawn_egg`; it spawns named Story Mode cast NPCs without waiting for town population.
* **Dark opaque Story Mode water shader defaults**: water is deep blue, mostly opaque, non-reflective, with caustics/rays/reflection shine disabled by default.
* **Light rays/flaring reduced hard** in the bundled Super Duper shaderpack: lens flare off, volumetric strength near zero, underwater caustics off.
* **Storm sky/fog fades back out with distance** so going far from the storm returns toward the calm vanilla/Story Mode sky instead of keeping the storm palette forever.
* **Wither Storm body sway/summon animation pass**: early phases subtly tilt left/right; summon animation starts with a quick look-down/dipped pose then snaps upward.
* **Ground block-fragment particles** now lift off blocks near the storm, matching the little cubed debris feel from the references.
* **Death shockwave extended** from a few seconds to about 26 seconds so the purple/supernova pulse remains visible instead of disappearing immediately.
* Added an **experimental visual infinite back-growth option**, off by default: `/ds storm backgrowth true` and `/ds storm backgrowth_speed <0.01..12>`.

# Devouring Storms 1.9.189 — cyan storm teeth, slimmer HUD rail, and lower-reflection shader defaults


This build reacts to the 1.9.188 test: Story Look now loads, the dark storm body looks good, but the phase 5.5/6 teeth need the cyan Story Mode glow and the Intel/Iris/Sodium setup is still running out of native memory.

* **Brightened/cyan-shifted phase 5.5 and phase 6+ Wither Storm emissive teeth textures** for both normal and OG skin paths.
* **Actually ticks the phase teeth/eye tint driver every rendered frame**, so the configured turquoise teeth glow updates for the current storm phase instead of sitting unused.
* **Boosted the flat cyan mouth/teeth glow overlays** in the storm-face renderer for phase 5.5 and post-split phase 6+.
* **Shrank and whitened the left inventory rail** so it fits the left side better and is less black.
* **Removed heavy reflection defaults from the bundled Super Duper shaderpack**: previous-frame reflections, PBR/specular/environment material reflections, SSR, and rough reflections are disabled by default/profiles; shader bloom is reduced to a subtle value.
* **Disabled the mod's full-resolution HDR storm bloom by default** to stop the Iris/Sodium native-buffer memory failure seen in the crash log. Teeth/eyes still use emissive cyan render passes.

# Devouring Storms 1.9.188 — Sodium-safe Story Look and first-spawn story area fallback


The screenshots confirmed the pack is no longer red/broken in vanilla Minecraft metadata terms, but Sodium still marks Story Look incompatible because it contains vanilla `minecraft:core/*` shader overrides. This build makes the external pack Sodium-safe and moves that look back to the mod/shaderpack path.

* **Removed vanilla core shader overrides from the external Story Look resource pack** so Sodium should stop flagging `DevouringStorms-StoryLook.zip` as incompatible.
* **Kept Story Look as a normal visual pack** for sky/environment/NPC textures while the jar and Iris shaderpack continue handling the shader look.
* **Restored legacy schematic fallback assets in the jar** until the clean MC105/MC201 NBT blueprints are supplied, so `/ds towns build`, `/ds towns start`, and first-spawn fallback can actually place Story Mode areas again.
* **First-spawn now falls back to the Episode One schematic cluster** if converted NBT blueprints are not present, placing/teleporting the player to the Wilderness Treehouse opening area instead of leaving them at an empty vanilla spawn tower.
* **Lightened the Story Mode HUD rail** so the hotbar is less black and closer to the white/clear UI in the reference image.
* **Capped full-res storm bloom strength** to reduce Iris/Sodium native-memory pressure while keeping the glow visible.

# Devouring Storms 1.9.187 — 26.2 pack metadata + Story Look shader reload fix


The latest log showed the exact problem: Minecraft 26.2 rejects packs above format 64 unless `min_format` and `max_format` are present. It also showed Story Look was overriding `minecraft:core/block` with an older fragment shader that did not match the 26.2 block vertex shader.

* **Fixed generated Story Look and OGS CEM `pack.mcmeta` again**: they now include `pack_format: 88`, `min_format: [88, 0]`, and `max_format: [88, 0]`.
* **Synced Story Look's block/terrain core shaders with the jar's 26.2 shader pair** so selecting the resource pack should no longer break `minecraft:pipeline/solid_block`, `cutout_block`, or `translucent_block` during reload.
* Keeps the 1.9.186 HUD/camera/NPC changes.

# Devouring Storms 1.9.186 — Story Mode HUD, camera, and town NPC pass


This build starts the in-game interface and NPC cleanup requested from the reference screenshots while keeping the 1.9.185 resource-pack format fix.

* **Changed the gameplay HUD toward the Minecraft: Story Mode layout**: the hotbar is now a large vertical left-side rail with a cream selection outline, selected-item label beside the rail, top-center effect/status callouts, lower-right gold action bars, and a small cyan prompt icon.
* **Improved third-person framing** by pulling the detached camera farther back so the player has a better full-body/bridge view like the screenshots.
* **Reduced Story Mode NPC population**: ambient cast now spawns only around actual story towns, with smaller town rosters and automatic cleanup for older overpopulated custom cast members found outside those towns.
* **Started human-like NPC variants**: named story cast members now use humanized villager bodies/professions where available, with procedurally generated skin/type texture variants bundled directly in the jar and Story Look pack.
* **Added simple interaction animation hooks**: talking NPCs look at the player, hop slightly, sparkle, and attempt a vanilla hand wave/swing when spoken to.

# Devouring Storms 1.9.185 — Minecraft 26.2 resource-pack format fix


## Resource packs

* **Changed Story Look and OGS CEM resource packs to `pack_format: 88`**, the Minecraft 26.2 resource-pack format, so the Resource Packs screen should stop showing the broken/unknown-version confirmation prompt.
* **Removed the broad `supported_formats` range** from those generated packs. The pack screen now receives one exact 26.2 format number instead of ambiguous compatibility metadata.
* Because the build version changed, the mod will regenerate fresh `DevouringStorms-StoryLook.zip` and `DevouringStorms-OGS-CEM.zip` copies in your instance `resourcepacks/` folder.

---

# Devouring Storms 1.9.184 — resource-pack recovery and visible storm texture fallback

## Fixes

* **Resource-pack recovery:** the jar still installs fixed `DevouringStorms-StoryLook.zip` and `DevouringStorms-OGS-CEM.zip`, but it no longer forces them selected. On launch it removes stale selected entries from `options.txt` so a previously failed reload does not keep breaking the pack screen. Enable the packs manually after launch to test them.
* **Broader pack compatibility metadata:** Story Look and OGS CEM packs now include `pack_format` plus broad `supported_formats` so Minecraft 26.2 should not classify them as broken just because of pack-format metadata.
* **Direct Wither Storm texture fallback:** brightened the main phase/devourer atlas textures and mirrored them into OG aliases so the in-world storm is not a completely black silhouette even without the resource pack enabled. This is a temporary direct-main fallback before final model/texture remapping.

---

# Devouring Storms 1.9.183 — resource-pack failure fix and cleaner Story Mode UI

## Fixes

* **Fixed the built-in Story Look / OGS CEM resource-pack metadata** by restoring the required `pack_format` field. The previous `min_format`/`max_format`-only metadata could show as failed/incompatible in the Resource Packs screen.
* **Kept forced extraction/selection of the visual packs** so the next version writes fresh fixed copies of `DevouringStorms-StoryLook.zip` and `DevouringStorms-OGS-CEM.zip` into the instance `resourcepacks/` folder.
* **Cleaned the old config screen bridge**: removed the fixed overlay button that covered base buttons and replaced the duplicate Devouring Storms rows with one clean “Open Devouring Storms” row.
* **Cleaned the Devouring Storms control panel layout** with Story Mode side borders, left-aligned/diagonal controls, and no giant duplicate top banner.
* **Main menu no longer draws a second Devouring Storms logo** over the existing title art; it keeps only the cinematic frame and bottom build strip.

Note: most `overrides/resourcepacks/*.zip` files in this checkout are still Git LFS pointer text, not real zips, so they cannot be converted/merged until hydrated/uploaded as real archives.

---

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
