# Devouring Storms 1.9.148 — mega-phase 11: OG look default + vivid shaded world

## OG / MCSM look is the default
`ogCemModels` defaults **ON**. Force MCSM Look keeps `stormSkin` at Obsidian
Gloss (shiny near-black flesh, purple sheen, command-block belly as
obsidian-purple tiles) so the body stops falling back to Classic orange.
The extras panel toggle still lets players opt out.

## Vivid world lighting + real shade
- Iris pack final grade: bloom 0.55, exposure 1.08, contrast 1.10, vibrance 1.28.
- Terrain coloured-light strength 0.70; warmer day key, deeper night blue.
- Terrain vertex now computes a Lambert sun term (`mcsmShade`) so tree
  canopies and block faces throw real shade under Iris — the “shadows vanished
  under the shader pack” bug, closed.
- Client gate floors `glowStrength` / `stormShadowStrength` / bloom + impact
  light so teeth, eyes and trailer shadows burn at full.
- World gate floors `townNpcPopulation` so story towns keep a full cast.

## Already live (1.9.145–1.9.147)
- Thick welded glare shell + sky-glued phase halo; purple vault 5.4–5.9 only;
  true deep-blue night; far three-head halo killed.
- Story towns inhabited with per-character dialogue trees (`McsmNpcs`).
- Visual presets stick (Netflix / Cinematic / MCSM OG / Legacy all recognised).
- Structures whole; Sky City ~y4200.

Install: drop the jar in `mods/`. Walk into a story town to meet the cast;
right-click them to talk. Pick a look preset — it will still be selected next
launch.
