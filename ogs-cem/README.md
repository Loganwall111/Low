# Totally Accurate / MCSM OG CEM pack

Source: https://github.com/Loganwall111/ogs-stuff

Ships inside the Devouring Storms jar as `resourcepacks/ogs-cem/` and
registers DEFAULT_ENABLED via `McsmBuiltinPack`.

Requires **Entity Model Features** (user already runs EMF 3.3.5) to replace
the entity mesh with the `.jem` models. Textures apply even without EMF via
the `dabywitherstormmod` / `witherstormmod` texture paths.

Phase ladder (`wither_storm.properties`) selects models by NBT `Phase`:
  0–4.49 → phase4, 4.5+ → phase4.5, 5+ → phase5, 5.5+ → phase5.5,
  6+ → phase6, 6.5+ → phase6.5, 7+ → phase7.
