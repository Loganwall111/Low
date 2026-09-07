# Devouring Storms 1.9.151 — Totally Accurate OG models default ON

## Totally Accurate / MCSM OG CEM (from ogs-stuff)
The pack at https://github.com/Loganwall111/ogs-stuff is now **embedded** in
the mod jar as built-in resource pack `ogs-cem`, **DEFAULT ENABLED**.

- Full phase ladder of `.jem` models (phase4 → 4.5 → 5 → 5.5 → 6 → 6.5 → 7,
  plus torn / dismantled / segment / head / grab tentacle aliases)
- OG textures (160×160 wither_storm body, head, tentacle, tractor beam)
- Both `dabywitherstormmod` and legacy `witherstormmod` texture namespaces
- Phase switching via entity NBT `Phase` (not ConsumedEntities)

**Requires Entity Model Features** (you already run EMF 3.3.5) for the mesh
swap. Without EMF you still get the OG textures on the base models.

## Also in this line
- 1.9.150 crash fix (`McsmWorldgen.tick` → `CallbackInfoReturnable`)
- OG glossy blue-under-black body plates
- Vanilla-embedded vivid lightmap (no Iris required)
- Harder cloud void gaps; volumetric + coloured-light floors

Install: drop the jar in `mods/`. Keep EMF + ETF. Open resource packs once
if the new `ogs-cem` pack did not auto-enable, and turn it ON (it should be
default). Scrap/disable any old external Totally Accurate pack to avoid
double-loading.
