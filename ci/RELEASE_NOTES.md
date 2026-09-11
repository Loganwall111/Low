# Devouring Storms 1.9.304 -- crash fix: towns auto-start once (no more server overload)

**Why 1.9.304:** hotfix for the 1.9.302 crash.

**The crash:** the town auto-start guard compared the last ticked dimension
against the current one, but the world tick runs for EVERY loaded dimension
(overworld, nether, end and the mod's bowels dimension). The guard therefore
tripped on every call: the structure queue was wiped every tick and the
three Episode-1 schematics were re-enqueued endlessly -- the server overloaded
("Can't keep up! ... ticks behind"), chunks timed out and the game hung.

**The fix:** the worldgen patch now manages the OVERWORLD only, keyed by the
world's own identity, exactly once per world load, and it probes each site
first so an already-built town is never rebuilt. Bonus: this same flaw was
why `/ds towns build` appeared to do nothing -- the queue was being cleared
before it could place. Towns now actually build, and they build once.

**After updating:** load a NEW world (or delete the half-built one from
1.9.302). The log should print the towns message exactly ONCE, then the
Episode-1 cluster (Wilderness Treehouse, The Wilderness, EnderCon Town
Fair) builds over a few seconds and the cast appears when you walk in.

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
Mods screen shows **1.9.304-26.2-beta-ds**; title screen, config screens
and `/ds` show build **1.9.304**. Verified by CI annotation on the build.

## Assets
- `devouringstorms-1.9.304-26.2-beta-ds.jar` — the mod
- `devouringstorms-shaderpack-v5-1.9.304.zip` — Iris shader pack
- `devouringstorms-storylook-1.9.304.zip` — Story Look resource pack
- `devouringstorms-superduper-default-1.9.304.zip` — Super Duper pack
- `.sha256` checksums for verification

## Installing (so the old jar/pack can never win again)
1. Delete EVERY `devouringstorms-*.jar` from your `mods/` folder.
2. Download `devouringstorms-1.9.304-26.2-beta-ds.jar` from this release
   and put it in `mods/` alone.
3. Delete the old pack folders from `shaderpacks/` (any folder starting
   with `devouringstorms`), then toggle "Built-in Shader Pack" OFF and ON
   once in the MCSM Control Panel -- the new pack reinstalls.
4. In game: the mods screen must show `1.9.304-26.2-beta-ds`. If it shows
   ANY other number, that jar is not this build.
