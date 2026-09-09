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
