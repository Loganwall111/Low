# 1.9.214 — THE REAL TELLTALE ASSETS DROP (repo Loganwall111/gggggrff)

The asset pack finally landed: original MCSM/Telltale meshes and textures extracted for Blockbench, with traced 1:1 "shaded" textures. Everything below uses those REAL assets -- no more invented textures.

- **TRUE body palette.** Measured from the 1:1 traced textures: Stage B body = black + (0,8,16) blue sheen with (0,8,24)/(0,16,32) highlights; Stage D head = black + (0,16,32); the massive-stage cyan glow is exactly (0,112,224). All 18 storm atlases are remapped to those exact measured values (per-atlas luminance stretch so the ramp lands correctly). The mini storm (Stage A) now uses the real light-grey (96,80,80) Stage A look with its purple eye kept.
- **THE HALO IS THE REAL ONE -- a volumetric thin shell.** Not a billboard, not a dome: an ellipsoid SHELL (wide oval) lathed around the storm in world space, textured with the game's own extracted halo radial falloff (fx_haloWitherstormStageB.png) -- bright core behind the body, black at the silhouette rim -- rendered additively in the phase tint as TWO nested layers (thin volumetric layer). World-anchored: it rides the storm's sway, never the camera; walk behind it and it is still there. Front-side quads keep only the rim band, so the glow wraps the storm's edges while the blackness stays inside.
- **REAL tractor beams.** The extracted beam textures (RECREATION A/B/C) are in the jar: the base beam is overridden with the real tapered blue shaft, and stage B/C colour variants ship for the later phases.
- **REAL vortex backdrop** (tile_witherstormVortexABackdrop) now darkens the sky band behind the storm's lower body.
- **REAL block textures** (obj_witherstormBlock Purple/DarkPurple/Black) now texture the phase 6/7/8-9 ring cubes.
- Also: Stage A body texture, block command textures and the rest of the extracted set are in the jar for future passes.

Carries everything from 1.9.208-1.9.213 (shader default + water fix, no dome, no purple night, teeth bloom, debris/glacier flakes/block tornado, ember 8-9 sky, characters).

# 1.9.213 — shader crash fixed, dome deleted, purple night gone, teeth glow restored

- **THE SHADER IS FIXED.** 1.9.212's water-stripe edit referenced a time uniform this pack never declares, which broke the water program and degraded the whole pack under Iris -- that is why the shader "was failing". The stripes are now a static pattern (safe uniforms only); the pack loads clean again.
- **The weird dome is DELETED.** The sky shell no longer renders at all -- the MCSM Visual Shader (or the base mod's Story Mode sky tint) owns the sky. No more circle/edge in the sky, ever.
- **Purple night gone.** The frame fog-colour override (which blended storm purple into the calm night horizon) is removed. Calm night = midnight navy; purple/pink/teal/ember only appear when a storm actually owns the sky.
- **Teeth emissive restored.** The mod's bloom pass had been zeroed for memory safety -- that is why the teeth looked flat instead of glowing. Bloom is now floored at 0.35 (raise-only), so the teeth/eyes get their emissive halo from the reference frames.
- **Halo closer to the reference.** Less black cover (0.45) so the purple middle of the oval reads, the oval is more elliptical, and slightly stronger alpha.

Everything else from 1.9.208-1.9.212 carries (world-anchored oval glare, Catalyst Halo, debris/rings/glacier flakes, block tornado, ember 8-9 sky, settings fix, character cast).

# 1.9.212 — the original oval glare is back (revamped), twilight pink gone, teeth brighter, block tornado

- **THE ORIGINAL GLARE IS BACK, revamped.** The old structured slab/rays experiment is deleted. The glare is now the original style: a gigantic world-anchored OVAL billboard sitting at the storm's centre -- it follows the storm and its slow atmospheric sway, but does NOT move with the player, so you can walk around and behind it. The textures carry the spec exactly: black rim blending to dark purple, purple middle, blackness core, a little alpha overall. Per-phase palettes (blue / teal-green / purple / pink-purple / brown-pink / ember 8-9).
- **Catalyst Halo re-enabled.** The base mod's own halo pass (black oval + breathing atmosphere pulses + cataclysm rings) was being cancelled by our patch; it now stays ON with a new oval halo_ring texture, layered inside the coloured oval for the blackness behind the silhouette. The fake sun-glow card is OFF (the shader draws the sun).
- **Twilight pink deleted everywhere.** The dusk/dawn fog decks are now deep blues, and the frame fog colour is re-matched to the calm sky deck every frame -- the purple/pink night wash below the horizon cannot come back. Night = the midnight navy deck.
- **Teeth glow much brighter** -- intensities raised to (p4 3.4 / p5 3.8 / p5.5-6 4.0 / p7 3.8) and the halo layers behind them are now at full strength.
- **Water got its vanilla stripes back** -- slow diagonal flow bands over the opaque story-mode blue (shader), so the surface reads as moving water, still zero reflections.
- **Block-matching tornado.** Phase 4 now summons a real tornado: particles emitted from the ACTUAL world blocks around the storm (sampled live, not random dust), pulled up and spiralling. From phase 6 the pull also comes off the ground under the storm, and crossing phase 4 fires an outward burst.
- **Phase 6 body bigger** -- backdrop radius 55->62 base + 46/phase, model scale 1.60->1.90 base at 6 (cap 2.8x).
- **Phase 7 rings cover the sky** -- a new giant flat ring high above the storm plus wider counter-spin sets; the phase 8-9 vortex rings grew to engulf the sky.

Everything from 1.9.208-1.9.211 carries (shader default, settings fix, debris, glacier flakes, ember sky).

# 1.9.211 — classic skin atlases finally recoloured (the phase 4-5.9 "original" look is gone)

- **Why phases 4-5.9 still looked like the ORIGINAL storm:** the skin pass only ever recoloured the `_og` atlas set. The texture selector can resolve the CLASSIC atlases (phase_4_assets, devourer_assets, wither_storm), which were never touched -- so some phases kept the vanilla grey body. The charcoal-indigo v2 pass now covers ALL 18 atlases, so every phase reads as the matte dark-charcoal-indigo Telltale body with crisp edge highlights, obsidian only at the tentacle tips.
- Everything from 1.9.208-1.9.210 carries unchanged.

**IMPORTANT -- you are on 1.9.202.** Almost everything you asked for has been fixed since then (sphere deleted, structured glare, debris/rings/glacier flakes, characters, shader default, settings fix, model sizes). Delete EVERY devouringstorms-*.jar in mods/ and install ONLY this one.

# 1.9.210 — phase-crossing voice lines, structured glare baked into assets, brighter story water

- **The cast now has phase-crossing VOICE LINES.** When the storm crosses phase 4, 5, 6, 7 or 8, a nearby character shouts in-character (with talk animation + villager voice sound) and the line lands in chat attributed to them: "It split its heads! THREE of them!", "The sky is BURNING...", "Stay OUT of the light!" etc. — three variants per phase, chosen at random.
- **Structured glare baked into the assets.** The five phase glare textures (plus a new phase 8-9 ember glare) are no longer soft radial mist discs — they now carry the rigid design directly: hard-edged three-band slab, 15 crisp alternating rays, saturated core, transparent background. Whatever code path samples them (including the shader) gets the rigid Telltale glare, never a fuzzy sphere.
- **Dead soft-glare code removed.** The old body-glued soft-volume pass (McsmPhaseSky) and every unused backdrop-wash texture reference in the blob are gone from the jar — the fuzzy sphere path cannot come back.
- **Water brightened again** toward story-mode mid blue (shader), still opaque, still zero reflections.

Everything from 1.9.208/1.9.209 carries: shader embedded + auto-selected by default, vanilla look permanently disabled, per-phase model growth + Storm Model Scale slider, settings now stick across sessions, opaque dome with no circle, cube rings 6/7 + the 8-9 layered vortex, glacier tornado flakes, max debris, ember phase 8-9 sky.

# 1.9.209 — settings actually stick, storm models grow per phase

- **"Settings not activating" fixed.** The MCSM gate was silently re-forcing its look over any value you had saved in the base mod's own config screen at every session start, so a clicked toggle looked dead. The gate now reads `config/dabywitherstormmod-client.json` once and NEVER touches a key you have explicitly persisted to a different value. Fresh installs still get the full Story Mode default; every click you make now sticks across sessions.
- **Bigger models per phase.** The whole storm MODEL now scales up each phase on top of its native growth (1.10x at 4, 1.30x at 5, up to 2.4x by 6+), not just the backdrop — heads, body and tentacles swell together. New "Storm Model Scale" slider (0.50-2.50x) in the Control Panel multiplies it globally.
- Also in this build: the sky texture folder from 1.9.208 (day/night/sunset/midnight + phase 5 / 5.5-5.9 / 6 / 7 fog + phase 8-9 ember strips), shader-as-default, the structured glare, cube rings, glacier flakes and max debris remain exactly as shipped in 1.9.208.

# 1.9.208 — shader is the default, glare rebuilt, rings, glacier flakes, ember apocalypse

- **The MCSM Visual Shader is now the DEFAULT.** `shaderPackGate` + `auto_select_shader_pack` are ON out of the box (existing installs flip on migration): the pack self-installs into `shaderpacks/` and Iris auto-selects it — no clicks, no resource packs, ever. The "Force MCSM Look / World" kill-switches are gone from the Control Panel (the vanilla look is permanently disabled); the in-game briefing now says the shader is the default.
- **Glare complete revamp.** The old gaussian "sphere" dome patches and every soft circular billboard wash are DELETED. The new glare is rigid and structured, built from the same phase palettes as the sky: a hard-edged three-band gradient slab locked behind the creature, 15 crisp rotating rays, a saturated core, and a faint purple fog pool cast on the ground under the tractor beams. Rendered as additive geometry, so it draws identically under the shader.
- **Debris overhaul.** Debris density is pinned at MAXIMUM by default (`debrisAlwaysMax`, toggleable); dark block fragments + purple dust vortex now wrap the storm at full count from phase 2 up. Real orbiting CUBE rings (horizontal / vertical / diagonal, pure tangential velocity) circle the body.
- **Glacier flakes (phase 4+).** Icy-white ice/snow block fragments rip off the body blocks and spiral UPWARD around the storm like a tornado, exactly the Telltale read.
- **Cubed storm rings (phase 6+, toggleable).** Not skybox layers — real rings of chunky cubes in the world: clockwise horizontal + vertical + diagonal at phase 6, a second counter-spinning set at phase 7. Phase 8-9 goes full vortex: three gigantic major rings, each 10 thin layered sub-rings funneled thin-at-the-top, outer clockwise / inner counter-clockwise.
- **Phase 8-9 apocalypse.** The whole sky burns dark-orange/ember (new P89 palette drives sky dome + fog), the halo disappears, and only the glare and the giant rings remain.
- **Sky texture set rebuilt.** `textures/sky/` now carries exactly the user set — day, night, sunset, midnight, phase 5 fog, 5.5-5.9 fog, phase 6 fog, phase 7 fog, phase 8-9 ember — as clean zenith→horizon strips generated from the runtime palettes.
- **Water is Story Mode now.** The shader's water albedo was near-black; it is a flat bright opaque story-mode blue (no reflections, no mirror).
- **Body skin v2 — matte charcoal-indigo, NOT obsidian.** Atlases remapped to dark indigo with crisp thresholded blue-white edge highlights (shiny without gloss); jagged obsidian restricted to the tentacle tips (bottom of the atlas). Saturated command-block orange/magenta preserved.
- **Storm is bigger at phase 6+** (55+42/phase, cap 320 blocks), tentacle girth 2.2→2.8.
- **Beams are volumetric day/night.** Tractor-beam opacity/colour now modulate live with the sun: hardest near noon, dim purple shafts at midnight.
- **Sky dome is fully opaque** (no more visible "circle" edge) and starts flush at the horizon.
- Story cast: more in-character dialogue lines for Jesse, Petra, Axel, Olivia, Lukas, Radar, Ivor, the Order, Harper, Stella.

# 1.9.207 — teeth colours per the corrected spec

- **Pure white ONLY at phase 5.** Phase 4, 5.5 and 6 are cyan-white (0.72/1.0/1.0); phase 7+ is GREEN (0.45/1.0/0.62). The 5.5/6/7 emissive masks were repainted to the same colours (recolour only, pixels never moved) so the atlas no longer fights the tint.

# 1.9.206 — Story Mode body skin, merged textures ACTUALLY ship, simulated tentacles off

- **Critical packaging fix.** `src/main/resources/` was never overlaid into the jar by the build, so the merged Story Look sun/moon, the villager cast skins and 1.9.205's 22 story_character skins silently missed every release (that is why "the old skin texture is appearing" and packs still seemed needed). The build now overlays every asset from it that the jar does not already contain.
- **Story Mode body pass** (`ci/make_storymode_body.py`) on all 9 OG storm atlases: luminance crushed to a near-black silhouette (avg 24,25,35 → 8,11,36), remaining light pushed blue, and block edges get a dark-blue underglow — the Telltale look from your comparison frames. Command-block orange and magenta accents preserved; emissive masks untouched.
- **Teeth vivid white.** Phase 4–5 pure white at intensity 2.6–3.0; 5.5+ white-blue (0.8/0.92/1.0 @3.4). Beams stay neon purple.
- **Simulate Tentacles OFF by default** (Force MCSM Look now sets `tentaclePhysics=false`); the 2.2× girth still applies.
- Shader reminder: "MCSM Visual Shader" is embedded in the jar and self-installs into `shaderpacks/` on launch; select it in Iris.

# 1.9.205 — the Story Mode cast are real characters now

- **New entity `dabywitherstormmod:story_character`.** Player-shaped humanoid (vanilla PLAYER model layer, 64×64 skin), 40 HP, walks with the normal walk/arm-swing cycle, opens doors, wanders, looks at players and at each other. Named cast in towns (Jesse, Petra, Axel, Olivia, Lukas, Ivor, Radar, Gabriel, Ellegaard, Magnus, Soren, Harper, Stella, Nurm, Jack, Binta, Otto, Hadrian, Maya, Stampy, Aiden) now spawn as this entity instead of re-skinned villagers; Reuben stays a pig.
- **Acting.** Two synced channels the renderer animates: *talking* (head nods, raised gesturing right arm, body sway, white speech motes) and *laughing* (fast head-bob, shoulders shaking, arms drawn in, celebrate sound). Talking to a character triggers them from the dialogue line (lines with "ha!"/"haha" laugh); idle cast members also chatter to each other every 15–45 s and sometimes laugh.
- **22 cast skins** generated in `ci/make_cast_skins.py` (canonical outfit colours: Jesse's white tee + red suspenders, Petra's bandana + gold armour trim, Axel's green hoodie, Olivia's goggles, Lukas's Ocelot jacket, Ivor's purple robe, …) at `textures/entity/story/<name>.png`. Unknown names fall back to Jesse.
- Not included (honestly): Telltale facial animation, lip-sync and voice acting.

# 1.9.204 — Story Mode storm density: debris vortex, colossal tentacles, neon tractor beams

- **Debris vortex.** Every tracked storm (phase 2+) is now wrapped in a swirling cloud of dark obsidian/blackstone/crying-obsidian block fragments, purple dust and black smoke plumes — up to ~110 fragments per frame at phase 6+, spiralling tangentially with an inward pull. The silhouette reads dense and massive like the Story Mode frames instead of a clean cut-out. Client-side only; fades with distance; never touches gameplay.
- **Colossal tentacles.** New `tentacle_girth` (default 2.2) fattens every tentacle chain on its cross axes after the physics pose, ramping from ~1.4× at phase 1.5 to full girth at phase 4, so the limbs read as heavy block limbs rather than thin ropes. Set it to 1.0 in the MCSM config to restore the original.
- **Neon-purple tractor beams.** Force MCSM Look now drives the beam colour to solid violet (0.55/0.15/1.0) and opacity 1.35, so the eyes radiate a powerful purple light.
- CI now also dumps the 26.2 entity/renderer/Fabric-object-builder API (`ci/api/entity.txt`) for the upcoming Story Mode character entity.

# 1.9.203 — MCSM Visual Shader, user palettes, no more sphere, readable blue teeth

- **Shader is now "MCSM Visual Shader".** It installs into `shaderpacks/MCSM Visual Shader.zip` on every launch by default (the old `DevouringStorms-SuperDuperDefault.zip` is removed), so it is visible in the Iris shader list; auto-selection stays opt-in in the MCSM panel for GL safety. Tuned toward the Story Mode concept frames: soft bloom on torches/lava/glowstone, soft coloured shadows, warmer orange block light (255/214/150 @1.25), saturation 1.20, emissive intensity 8, gentler sun, lighter ground fog. Reflections, god rays and see-through water stay hard-killed.
- **Skies use the palettes you supplied.** Midnight (navy 16,16,72 → 70,95,230 horizon band), phase-5 purple, phase-5.5 dusk (glarephase6plus) and phase-6 mauve decks are now taken from the palette PNGs instead of my extracted ramps, so night is no longer purple.
- **The giant sphere is gone.** The glare dome patches used a polynomial falloff that stayed almost opaque out to the rim, so they read as a hard-edged ball floating in the world. Falloff is now gaussian, the main glare patch is 28% smaller and half as bright, the skirt 20% smaller, and the black upper cap is 30×17° (was 42×24°).
- **Teeth read as blue.** Phase 5.5+ is now 0.62/0.86/1.0 at intensity 3.2, phase 6 0.50/0.82/1.0 at 3.4 — the old values were so dark the overlay looked unlit.
- If new features seem missing: make sure `mods/` contains ONLY `devouringstorms-1.9.203-*.jar` (older copies load first) and the Mods list shows 1.9.203.

# 1.9.202 — teal vault wins over the sunset, small-phase glitch squashed, opaque Story-Mode water

- **The teal vault actually wins now.** The 1.9.201 dome capped the storm blend at 0.85 and only reached alpha 210, so vanilla's pink/orange sunset bled straight through — that is the wash in the 09-10 screenshot vs the accurate 09-06 frame. The storm decks now commit to ~96% blend with alpha 175–235, and the calm day/sunset/night decks ride the `customSkyboxes` gate too, so a stale config can no longer leave them dark.
- **Small phase de-glitched.** The deep black upper cap drew at full alpha from phase ~4 on, hanging over the mini storm as a floating black blob (the 09-10 "completely glitched" frames). It now fades in only between phase 4.55 and 5.25, as the storm grows into its full silhouette. Phase 0–1.99 keeps the native 3-head + command-block model on the rebuilt OG 64×96 atlas.
- **Glowing teeth are visible again.** 1.9.201 set phase 5 to glow=off, which hid the teeth overlay entirely ("no glowing teeth"). Phase 5 now renders completely white teeth with the overlay on (intensity 1.6), and the phase-5.5+ bluish glow is brighter (0.45/0.72/1.0 at 2.2) so it reads on screen. Phase 6/7 unchanged.
- **Story-Mode water + hard-killed reflections/god rays in Super Duper.** The pack already contained a `MCSM_DARK_OPAQUE_WATER` opaque deep-blue water path — it was just never enabled; now it is, so water is opaque, dark and mirror-free. `SSR`, `SSGI`, `ROUGH_REFLECTIONS`, `PREVIOUS_FRAME`, `SPECULAR_HIGHLIGHTS`, `ENVIRONMENT_PBR`, `VOLUMETRIC_LIGHTING` and `LENS_FLARE` are force-undefined, so even stale per-pack option sidecars cannot turn reflections or god rays back on. Meanwhile `SHADOW_COLOR` (colourful shadow light) is enabled and ambient lighting lifts 0.05 → 0.12 for vivid Story-Mode lighting; shadows, entity/block-entity shadows stay on. Mirrored into the managed `overrides/shaderpacks/` copy.
- **Two more textures live inside the mod jar.** `textures/environment/sun.png` and `moon_phases.png` — the last Story Look assets not already merged — are now in the mod jar itself (and jar-overrides), so the skies use them without any resource pack. The villager cast skins were already merged in earlier builds; the only remaining external pack is the 162 MB OGS CEM model pack, which cannot fit inside the jar.

# 1.9.201 — extracted skies render, OG command block at phase 0, multi-colour glare, cast skins

- **The skies render now.** The OG 1024×1024 gradient strips in `mcsm_atmosphere/sky/` were dead assets — nothing in any jar ever drew them, which is exactly the "skies are not rendering" report. New `McsmSkyDome` paints them as 16-stop zenith→horizon ramps (extracted by `ci/make_glare_from_sky.py` into `McsmGlarePalettes`) onto a translucent sky shell: calm day/sunset/night decks by clock, blending to the phase 4/5/5.4/5.5/6 decks near a storm, with terrain and clouds still reading through.
- **Glare is multi-colour and much bigger.** The old wash was one flat colour because the shipped glare PNGs were 100% transparent and the dome patches used a single tint. The glare discs are rebuilt from the extracted sky strips (bright horizon core → saturated middle → dark zenith, hot-pink ring on 5.5/6), drawn as textured dome patches at up to ~2× the old angular size, riding the Glare Size slider whose default rises 0.58 → 1.35 (old configs migrate). The black upper cap and saturated core remain so the silhouette reads.
- **Phase-0 command block reattached.** The classic root atlas was a 160×160 near-black placeholder with the wrong size for the 64×96 phase-0 model layer, so the mini storm could never show the belly. It is rebuilt from the OG 64×96 atlas (3 heads + orange command-block grid at texOffs(0,64)) in both skin modes, and the OGS CEM ladder now maps only phase 2.0–4.499 onto the grown CEM model — phase 0–1.99 keeps the native command-block model.
- **Teeth/eyes follow the corrected frames:** phase 5 = completely white with no glow bloom; phase 5.5+ = the bluish glow; phase 6 = deeper blue/cyan; 7 green-blue. Emissive masks recoloured without moving any teeth/eye pixels.
- **Built-in packs install again.** `McsmBuiltinPack` hard-required the `ResourcePackActivationPredicate` class that Fabric API 0.160+ removed, so registration threw and Story Look / OGS CEM never registered — the "OG texture packs did not install with it" cause. The activation predicate is now derived from the live `registerBuiltinResourcePack` signature.
- **Cast skins are deterministic.** Named characters always wear their Story Look outfit (villager-type skin per character) instead of hash-rolled variants. Note: the cast still spawns as named, persistent villager-based entities; a brand-new registered entity type needs a runtime testbed this sandbox does not have, so that remains the follow-up.

# 1.9.200 — Sky City haze included in verified build

- Includes the high-altitude Sky City blue haze blend in the published jar, ramping in above tall build heights without changing normal ground-level play.
- Carries forward 1.9.199's thick storm-top oval halo/backdrop, command-block controls, OGS texture fallback, shader reflection/god-ray trim, and skybox audit.

# 1.9.199 — thick MCSM storm halo, command-block controls, Story Mode shader trim

- Reworked the storm wash into a thick storm-bearing oval/top-cap halo: a black upper shell plus saturated phase-colored core now wraps the Wither Storm sides/top instead of reading as thin horizon fog.
- Added uploaded-reference-style day, midnight, sunset, phase 5, phase 5.5/5.9, and phase 6 sky/glare gradients into both mod assets and jar override assets.
- Copied the OGS Wither Storm texture set directly into the mod namespace as a fallback in addition to the default-enabled OGS CEM resource pack, so the OG look is no longer dependent only on pack ordering.
- Added command-block interactions: crouch/use opens the Devouring Storms control panel; normal use snaps nearby non-player entities out with DISCARDED removal instead of damage/death.
- Shrunk and re-anchored the left inventory rail closer to the screen edge.
- Added high-altitude Sky City blue haze blending and kept storm phase sky/fog distance-limited.
- Stripped the packaged Super Duper default further toward Story Mode defaults: reflections/god-rays stay disabled while shadows, lighting, and dynamic sky remain.

# Devouring Storms 1.9.198 — storm-anchored sky-volume glare, OGS default, shader safety

This build follows the 1.9.197 test and the new MCSM references. The important correction is that the storm glare should not be a texture billboard/card: it should behave like a storm-attached skybox/fog volume behind the storm.

* **Replaced the flat glare billboard with a curved sky-volume wash**: layered dome patches are projected onto a far sky shell around the camera in the storm's direction, creating a foggy MCSM backdrop/blob instead of a visible 2D card.
* **Removed edge sparkle/dot fields from the glare** so the backdrop reads as atmospheric darkness/colour, not particles stuck to a rectangle.
* **Uses vanilla translucent-emissive rendering for the sky volume** to avoid Iris custom pipeline warnings like missing `dabywitherstormmod:pipeline/storm_glow` / `storm_translucent` when external shaderpacks are active.
* **Stops forcing `ShaderPackCompat.active()` false by default** when Iris/external shaders are on. No-shader play keeps the mod visuals, but external shaders no longer get our custom storm pipelines forced into their override lists.
* **Adds `pack.mcmeta` to the bundled Super Duper shaderpack zips** and includes it in the embedded/release shaderpack packaging, improving Iris pack recognition.
* **Restores the OGS CEM preset/model pack as a default-enabled built-in pack** and refreshes the default OGS body/tentacle/tractor textures from `Loganwall111/ogs-stuff/witherstormmod`.
* **Recenters and fits the left HUD sidebar** so it sits in the vertical center area instead of being too low/cut off.
* Heavy shaders are still optional rather than forced on, because the latest log shows native virtual-memory/pagefile failure when Super Duper shaders are enabled.

# Devouring Storms 1.9.197 — soft MCSM glare, calm blue night, restored opening structures

This build targets the first successful no-shader gameplay test after 1.9.196: the game runs, but the sky glare/duplicate-head cards, purple night wash, missing opening structures, and weak cyan teeth needed an immediate correction pass.

* **Rebuilt storm glare textures as clean soft radial haze**: no embedded storm-face silhouettes, no hard square/card edges, and no big line-like texture artifacts.
* **Disabled the duplicate phase-sky glare card path** so only one camera-space storm halo renders around the nearest storm instead of stacked billboards that looked like extra heads.
* **Removed fake sky teeth/head overlays from the glare pass**; teeth should now come from the real storm model/emissive tint instead of a giant stamped sky texture.
* **Reduced purple/pink storm sky strength** and made phase 6 more grey-black with only slight purple, so normal night is no longer globally purple. Existing old configs with max night opacity are migrated down.
* **Restored first-spawn Episode One structure queue** to the treehouse, wilderness, and EnderCon opening cluster while keeping the safe 4096 blocks/tick placement budget.
* **Boosted phase-dynamic cyan teeth/emissives**: phase 5 reads white-cyan, phase 5.5 glows white-cyan, phase 6 is stronger blue/cyan, and phase 7 is green-blue.
* **Added only subtle purple underside tinting** to later body/devourer atlases while preserving the dark black/blue-black MCSM body.
* Shaders are still shipped as release assets and installed as available shaderpacks, but are **not auto-enabled** after the user's NVIDIA/OpenGL `GL_OUT_OF_MEMORY` crash path.

# Devouring Storms 1.9.196 — no-shader stability and readable dark storm body

This build reacts to the no-shader test: the game can play without shaders, but the storm body became too black/silhouette-like and the remaining crash path is native/GL memory rather than Java code.

* **OGS CEM is no longer default-enabled.** It is still extracted and available in Resource Packs, but the stable default keeps only Story Look auto-enabled. This avoids EMF/CEM loading the heaviest storm model pack automatically on Sodium/Iris/Nuit.
* **Removes already-selected built-in OGS CEM from `options.txt` once**, so old profiles do not keep the heavy model pack selected after updating.
* **Lifted the phase/body atlas brightness back up** to readable MCSM black/blue-black. It should no longer be a pure black silhouette, while still avoiding the rejected full-purple/full-blue body.
* Keeps shaders default-off and the config preview default-off from 1.9.195.

# Devouring Storms 1.9.195 — emergency GL/native-memory safe mode

This build targets the new `GL_OUT_OF_MEMORY` / `Native memory allocation ... AllocateHeap` screenshots. The crash is coming from OpenGL/native buffers, so lowering Java heap alone is not enough.

* **Managed Iris/Super Duper shaderpack is no longer auto-enabled by default.** The mod still ships it, but the stable default is now the resource-pack/core look; players can turn the shader back on from Shift+C after confirming the world is stable.
* **Migrates older Devouring Storms configs to disable `embedded_shader_pack` once**, because old config files kept forcing the heavy shader back on even after installing a safer jar.
* **If Iris is currently using `DevouringStorms-SuperDuperDefault.zip`, the installer switches Iris back to internal/no shader** when safe mode is active.
* **The config screen’s live Wither Storm model preview now starts OFF** to avoid a huge dynamic-buffer spike just from opening the settings menu. The `Model` button can still turn it back on manually.
* Keeps 1.9.194’s first-spawn/structure pacing and debris reductions.

# Devouring Storms 1.9.194 — native-memory crash guard for first-spawn/chunk loading

This build targets the new HotSpot fatal error: `Native memory allocation (malloc) failed ... Chunk::new`. That is not a normal Java exception; it means the JVM/native renderer ran out of native memory while chunks/models were being built.

* **Stops first-spawn from queueing four large Story Mode schematic areas at once.** The first world now starts with the Wilderness Treehouse only; larger towns can still be built/summoned after the world settles.
* **Spreads schematic placement across ticks** instead of forcing a near-whole structure in one tick, reducing Sodium/Iris chunk-mesh allocation spikes.
* **Disables the heaviest storm debris/ejecta defaults** while keeping the body, teeth/eye glow, sky, water, HUD, and core Story Mode look active.
* **Clears old per-pack shader option sidecars when the managed shaderpack updates**, so an older heavy Custom profile does not keep overriding the new safe defaults.
* Keeps 1.9.193’s darker storm skin and visible phase-5 teeth.

# Devouring Storms 1.9.193 — darker MCSM storm skin and visible phase-5 teeth

This build corrects the screenshot where the phase-5/phase-6 preview still looked like a purple body with no readable teeth.

* **Reworked phase-4/5, phase-5.5, phase-6 and phase-7 body/devourer atlases** so the storm is mostly black/blue-black like Minecraft: Story Mode, with only sparse purple/blue/teal panel scratches instead of a full purple or blue flood.
* **Made phase-5 teeth visibly white** by keeping the teeth overlay enabled at a very low intensity and increasing the phase-5 white teeth mask opacity. It should read as white teeth without the huge bloom cloud reserved for phase 5.5+.
* **Rebranded the base config title** from `Dabicco's Wither Storm` to `Devouring Storms` during jar assembly so the fresh build no longer looks like the old base screen.
* **Preserved phase behavior:** phase 5.5 glows white, phase 6 shifts blue/cyan, and phase 7 shifts green-blue.
* Keeps the 1.9.192 Fabric API 0.160 built-in resource-pack activation fix.

# Devouring Storms 1.9.192 — Fabric 0.160 resource-pack activation fix

This build follows up on the successful 1.9.191 launch log. The game is now loading the correct jar, but Fabric API 0.160 changed/renamed the built-in resource-pack activation API that the mod was reflecting.

* **Fixed built-in Story Look / OGS CEM auto-registration on Fabric API 0.160+** by reflecting the actual `registerBuiltinResourcePack` activation parameter type instead of hard-coding the removed `ResourcePackActivationPredicate` class name.
* **Keeps 1.9.191’s phase-6 texture correction and shader crash-safe defaults.**
* The normal extracted `DevouringStorms-StoryLook.zip` / `DevouringStorms-OGS-CEM.zip` files still work manually, but the built-in default-enabled path should no longer print `ClassNotFoundException: ResourcePackActivationPredicate`.

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
* **Magical Sparkles**: Shimmering white, pink and purple particles around storm bodies and magical anchors.

### ⚡ Enhanced Wither Storm AI
* **Menacing Threat Tracking**: Prioritizes players holding beacons, formidibombs, or nether stars.
* **Combat Aggression**: Predictive tentacle slams, roar cues, and aggressive pursuit.

### 🎛 Control Panel & Settings
* **MCSM Control Panel**: Complete scrollable in-game menu covering all visual, atmospheric, gameplay, and AI parameters.
* **Quick Access**: Accessible via Wither Storm settings or Shift+C shortcut.
