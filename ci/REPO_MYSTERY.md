# REPO MYSTERY — SOLVED (2026-09-11)

Where every feature actually lives, why some never appeared in released
builds, and exactly how to see each one in the current release
(`ds-1.9.301`, build 1.9.301).

## How the release pipeline works (the root of every confusion)

1. A release is ONLY created when a push to an `arena/**` branch builds a
   `VERSION` whose `ds-<VERSION>` tag does not already exist.
2. Every jar is assembled from the SAME frozen base jar (1.9.100) plus the
   overlay files in the branch (`mcsm-extras/java`, `jar-overrides`,
   `src/main/resources`, `storylook`, `shaderpack-*`, `ogs-cem`).
3. Therefore: work committed on a NON-arena branch, or on a branch whose
   version number was already taken, or work kept in folders the build
   never packs (e.g. `MCSM_ResourcePack/`, `dabby-patch/`, root-level
   `net/`), NEVER reaches any jar. That is the whole "mystery".

## The feature map (verified file-by-file)

### IN the current release (ds-1.9.301) — needs the right trigger, not a new jar
- **Structures / towns**: the base jar itself contains the town system
  (`net.dabicco.witherstormmod.structures.McsmWorldgen` static queue,
  `McsmSchematic`, site layout with ABSOLUTE coordinates, MC105
  schematics in `assets/dabywitherstormmod/schematics/`). Our patches
  (`McsmWorldgenPatch`, `McsmTownCommandPatch`) raise Sky City, fix the
  queue budget and add `/ds towns ...`.
  **To see them in game: run `/ds towns start`** (builds the Episode-1
  opening cluster: Wilderness Treehouse, The Wilderness, EnderCon Town
  Fair), then `/ds towns list` and `/ds towns tp <site>`. `/ds towns build
  all` builds everything. The sites do NOT build themselves on world join —
  that is why "no structures" appeared despite everything shipping.
- **NPCs**: `McsmNpcs` is in the jar and ticks every 2s from the worldgen
  patch. It populates a town with its Story Mode cast the moment a player
  is near a BUILT town site. No built town = no NPCs. Build with
  `/ds towns start` (or `build all`), teleport there, and the cast appears.
  Spawn eggs also exist (`/give` the Story Character egg).
- **Tentacle grabbing**: `McsmStormGrabPatch` is in the jar — the base
  mod's grab machinery (forceTentacleSlam -> GrabTentacleEntity ->
  registerGrabHit: pull / shake / eat / throw players) now self-triggers
  from the storm's tick during survival phases instead of only via
  command. Watch for grab tentacles during an active storm.
- **Holographic terminal**: `McsmHudTerminal` is in the jar — the MCSM
  hotbar HUD with the holographic terminal panel (build stamp, storm
  state, position, world time) plus cutscene letterbox bars. It is ON by
  default (config `hudTerminal`).
- **Command block texture (in-game block)**: the storm's command block
  face lives in the shipped entity atlases (`wither_storm*.png` region)
  and the base jar's `textures/block/wither_storm_eye_block.png`.
- **Config surface**: the shipped config is `McsmExtrasScreen` (Story Mode
  Controls, MCSM Control Panel) + the base mod's `WitherStormConfigScreen`
  with ~40 toggles/sliders (`customSkyboxes`, `headEyeGlow`,
  `turquoiseTeeth`, etc.).
- **Pack names**: the "renamed" packs come from this line's release
  assets: `devouringstorms-superduper-default-*.zip` (the "Super Duper /
  super-vanilla look" pack) and `devouringstorms-storylook-*.zip` (the
  "Story Mode" look). Those names exist only on this line — no orphan
  branch owns them.

### TRUE orphans — implemented somewhere, never shipped
- **`arena/01a07250-lowuuuuuu` (VERSION 1.9.143, Sep 6)** — release
  `ds-1.9.143` NEVER existed (tag list skips 1.9.101..1.9.200 for this
  numbering). Its features (grab patch, town patch, HUD terminal, structure
  NBTs) DID flow into later released branches — verified present in
  1.9.301. Root-level `net/dabicco/...` sources there are stale copies.
- **`arena/01a07d16-lowuuuuuu` (VERSION 1.9.171, Sep 9)** — release
  `ds-1.9.171` never existed. Carries `McsmExtrasConfigScreen.java`: an
  UNFINISHED config-screen skeleton (placeholder comments, even a Forge
  import — it could never compile). The "complete config revamp" was
  abandoned; the shipped `McsmExtrasScreen` is the real successor.
- **`hres` (Sep 2)** — `dabby-patch/`: a patch overlay for DABICCO'S REAL
  MOD SOURCE (368 MB src, gradle build — a different project). Its 180+
  Story Mode schematics (MC101: Redstonia, Order Temple, Nether arrival,
  statues...) and structure Java target that separate rebuild, not our
  jar pipeline. "Structures implemented on a different branch" = this.
- **`Loganwall111-patch-1` / `wwwwwww` (Aug 25-30)** — `MCSM_ResourcePack/`
  experiment: the command block TOOL textures (axe/book/hoe/pickaxe/
  shovel/sword + particles, namespaces `devouringstorms`/`cmdblockascension`),
  a grapple item, withered block set. Never wired into the jar, so the
  command block item textures never shipped.
- **`v2.0.0-devouring` / `v2.2.0-mcsm-ultimate` tags** — the "secret
  numbers": tags on old Aug-Sep commits (`v2.2.0` = arena/01a05c7b). No
  v2.x release page ever existed; they were experiments, not publications.

## Why "secret versions" kept feeling newer

- Release pages list by publish time, not by content. Deleted releases
  (`ds-1.9.221`), republished tags (`ds-1.9.215`) and the other line's
  hourly 1.9.216-1.9.220 releases all interleaved with this line's work.
- The base jar is frozen at 1.9.100, so the mods screen could only ever
  show whatever `fabric.mod.json` got stamped — and the old build script
  silently skipped stamping when the format did not match (fixed: JSON
  rewrite + CI notice, build 1.9.300+).
- Features built in one branch but never merged forward (config skeleton,
  command block pack, dabby-patch) were lost at the next release because
  nothing ever copied them in.

## The fix that ended it

`ds-1.9.301` merged the other line's 32 feature commits (models, NPCs,
vortex, teeth/bloom, pack auto-select) with this line's sky work. The only
features still outside ANY jar are the three true orphans above, which
were never finished or belong to a different project — not secret releases,
just abandoned experiment branches.
