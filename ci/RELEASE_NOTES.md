# Devouring Storms 1.9.149 — MCSM ground-truth retarget

User supplied the full MCSM reference frame set (phase skies, glare,
teeth close-ups, Formidi-bomb core, multi-head storm, tractor beams,
env stills). This build retargets the overlay against those frames.

## Teeth = model body detail (not glare)
- Phase-4 atlas UV `(8,510)` painted bright cyan-white; eye UV hot magenta.
- Emissive overlays (`phase_4_assets_e`, `wither_storm_*_e`) so
  `turquoiseTeeth` burns.
- Distant-blob mouths redrawn as **11 chunky white blocks on a U-arc**
  + inner dotted arc + magenta emitter cube above (match close-ups).
- Gate floors `turquoiseTeethIntensity` 2.4 and eye tint toward cyan-white.

## Formidi-bomb / early OG core
- `formidibomb.png` retargeted to brown command-block face + RGB button
  grid (MCSM summon frames). Emissive lights the coloured buttons only.
- `wither_storm_og.png` + emissives shipped so `stormSkin=OG` resolves
  (near-black flesh, warm CB belly, bright teeth).
- `phase_4_assets_og.png` + `devourer_assets_og.png` paths filled.

## Tractor beams
- Distant blob draws thick purple/blue conical shafts with sparkle motes.
- Gate floors `beamOpacity` 0.92 and MCSM purple beam colour bias.

## Phase skies (sampled from gradient strips)
- Day/noon: deeper pure-blue zenith → soft lavender horizon.
- 5.4–5.9: near-black indigo zenith → magenta mid → **salmon-pink** rim.
- Turquoise phase-5 strip punched greener.
- `McsmPhaseSky` dome palette matched to the same decks.

## Already live (1.9.145–1.9.148)
- Thick welded glare shell; far three-head halo killed.
- OG CEM default ON; vivid shade/lighting under Iris.
- Story town NPCs + dialogue; presets stick; Sky City ~y4200.

Install: drop the jar in `mods/`. Force MCSM Look stays ON for the OG
skin + teeth path. Walk a story town and right-click the cast.
