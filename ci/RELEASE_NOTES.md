# Devouring Storms 1.9.147 — mega-phase 10: the visual presets actually stick

## Root cause of "the new model preset doesn't work"
The mod ships five presets — Custom, **MCSM OG Visuals**, Legacy Java,
Cinematic, **Netflix** — but its own recogniser only ever scanned three:

```java
for (int preset = 1; preset <= 3; preset++)   // Netflix is 4
```

and `refreshPreset()` runs at the end of `load()`. So picking the newest
preset *did* apply its values, and then the very next launch read those
values back, failed to recognise them, and reset the selector to
"Custom". The look was applied but the mod had forgotten which preset it
was — no way back to it in the GUI, and any reset lost it.

The same oversight hit `isPresetKey()`, which asked only the MCSM map
whether a key belongs to a preset, so keys unique to Cinematic and
Netflix were never treated as preset-owned.

## Fixed
- The recogniser now scans **all four** preset maps, newest first, so
  Netflix and Cinematic survive a restart and stay selected.
- `isPresetKey()` answers for any preset, not just MCSM OG Visuals.
- The comparison reads the config's own public static fields by name and
  understands double, float, int and boolean storage; a key it cannot
  resolve is skipped rather than counted as a mismatch, so a partial
  match never silently drops you back to Custom.

Both hooks are `require = 0` and wrapped in a catch — if anything about
the surface is unexpected, the base implementation simply runs instead.
