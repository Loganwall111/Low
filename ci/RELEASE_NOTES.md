# Devouring Storms 1.9.146 — mega-phase 9: the towns are inhabited, and they talk

Until now the Story Mode sites were architecture and nothing else. Every
ground town from the mod's own layout now has residents, and every
resident has a dialogue tree.

## Population
- Each town has a cast list of its own: Beacon Town gets Radar, Stella,
  Lluna's keeper and Nurm; the EnderCon fair gets Jesse, Petra, Axel,
  Olivia and Lukas; the Order's temple gets Gabriel and Ivor; Ellegaard,
  Magnus and Soren stand in their own courtyards; Champion City gets
  Aiden, Maya and Gill; the Terminal Control Center gets Harper and a
  PAMA terminal, and so on across twenty-three sites.
- They spawn the moment a player comes within 128 blocks, so the chunks
  are always loaded and nobody appears inside a wall. Named, name-tags
  visible, persistent.
- The spawn is **idempotent**: if any named resident is already standing
  in the town, the site is skipped. Restarting the server never doubles
  the cast.

## Dialogue
- Right-clicking a story NPC opens **their line instead of a trade
  screen**. Each further click advances their tree, and it loops when it
  runs out — tracked per player, per character.
- Lines are written from the characters: Ivor insists it was always a
  command block, Magnus wants to blow it up, Soren admits he ran, Radar
  has scheduled the evacuation twice, Nurm says "Hrm."
- Cast members without a written tree fall back to townsfolk lines about
  the thing over the hills.
- The interaction is hooked through Fabric's `UseEntityCallback` **by
  reflection**: registered when the event class is present, silently
  skipped when it is not, so the hook can never break a build.

Everything here uses calls the base mod already makes on 26.2 — the
`StoryNpcSpawner` spawn sequence, `getEntitiesOfClass`, `isLoaded`,
`playSound`, `sendSystemMessage` — and the whole tick is wrapped in a
catch: a town without its cast is survivable, a crashed tick is not.

---

# Devouring Storms 1.9.145 — mega-phase 8+7b: phase sky, thick shell, whole structures

## Phase sky is 5.4–5.9 only (not night)
The big glowing purple sky is **not night**. Night and midnight are a true
deep blue again in both sky shaders. The purple vault and its fog are drawn
by the mod itself (`McsmPhaseSky`) only while the storm sits in **phase 5.4
through 5.9**. Past 5.9 the purple leaves and the vault turns **light pink**,
exactly where the reference frames end. The vault is lit from the storm's
bearing; phase fog is thickest at the horizon.

## Daytime and noon
Contrasted bluish ramp from the reference sheet: deep blue zenith
(0.106, 0.286, 0.694) → azure mid → pale blue-white horizon.

## The far three-headed halo is gone
`StormPresenceFX.blackGlare` + `cataclysmHalos` painted `halo_ring.png` at
bodyR×1.3..1.9 — the "three heads / symbol far behind the wither". Both knobs
are forced OFF every frame (`McsmPresenceFxPatch` + `McsmGate`). Atmosphere
pulse and ejecta debris still run.

## Thick welded glare shell + sky-glued halo
- **McsmStormBlob**: multi-depth thick 3D shell of 7 plates offset along the
  camera→storm ray (behind / through / in front). World-space near the body,
  angular blend at range — never detaches. Body palette fringe (dark blue +
  dark purple) replaces the old face overlay. Chunky zigzag / dotted U-arc
  teeth sit on each mouth emitter as body detail, not glare.
- **McsmPhaseSky**: pure-colour multi-depth halo (no heads) at the storm's
  bearing, plus the 5.5–5.9 silhouette stack — dark-blue over-storm light,
  dark purple wrap, purple + moon-blue pair, black atmospheric cap that erases
  the top of the sky around the storm (not the body).

## Structures whole + Sky City altitude
1.9.143 failed because `@ModifyReturnValue` is not on the shipped Mixin jar.
`McsmWorldgenPatch` hooks `enqueue` with a ThreadLocal re-entry guard:
floating sites (y∈(200,1000)) are re-enqueued at y+3904 (~y4200) and the
original call is cancelled. Tick still clears the static queue on level change
and raises the budget to 900k so schematics land whole.

Unchanged: 6b warp portals, 6a particle field, built-in shader pack DEFAULT ON,
mirrored cloud decks.

Install: drop the jar in `mods/`. Existing worlds pick the new Sky City
altitude on fresh structure placement; already-placed blocks stay put.
