# Devouring Storms 1.9.150 — crash fix + OG glossy + embedded vivid

## CRASH FIX (must update)
`McsmWorldgen.tick(ServerLevel)` returns `int`. The 1.9.149 inject used plain
`CallbackInfo` → `InvalidInjectionException` the moment worldgen classloaded,
killing the integrated server tick. Fixed to `CallbackInfoReturnable<Integer>`.
**Replace 1.9.149 with this jar.**

## OG glossy body (your 2026-08-24 frame)
The MCSM body is pure black mass with a **blue sheen under the black** (and
black stripe over blue) — the reverse-shading glossy read. Painted as stacked
depth plates on the welded shell: blue underlay → black stripe mass → thin
cyan-blue rim. No three-head symbol. (We cannot force upstream `reverseShading`
ON on 26.2 — it selects the broken `bodyCutout` path; the gloss lives in the
blob instead.)

## Vanilla-embedded vivid style (no Iris required)
Story Look pack already ships inside the jar and auto-enables. 1.9.150 pushes:
- lightmap: hotter torch colour, deeper cool lavender shade, contrast + vibrance
  lift so the world reads vivid without a shader pack
- Iris pack (optional): contrast 1.18, vibrance 1.40, bloom 0.65, coloured light 0.85
- volumetric fog density floor 0.92 + coloured-lighting gates

Iris is still optional eye-candy on top. Vanilla + the embedded pack is the
baseline look.

## Layered clouds + void gaps
Already painted by the sky pass (9 decks, mirrored under the horizon for the
Sky City fall-through). 1.9.150 spreads deck altitudes and hardens the void
gap mask so the gigantic holes between layers read clearer.

## OG / Totally Accurate models
`ogs-stuff` is not reachable from this sandbox (404 / private). The base jar
already carries `assets/dabywitherstormmod/cem/`. Force MCSM Look +
`ogCemModels=true` (default) keeps stormSkin on Obsidian Gloss. **When you
embed the Totally Accurate CEM/jem pack into the repo**, drop it under
`jar-overrides/assets/dabywitherstormmod/cem/` (or EMF `emf/entity/`) and the
next build will ship it as default — animations (tentacle grab/slap) stay on
the base entity animation system as long as bone names match.

## Already live (1.9.145–1.9.149)
Teeth U-arcs, Formidi CB face, thick beams, phase sky palettes, NPCs, presets,
Sky City ~y4200, far three-head halo killed.

Install: drop the jar in `mods/`, remove 1.9.149. Story Look auto-enables.
