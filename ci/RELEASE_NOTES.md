# Devouring Storms 1.9.152 — calm sky fix (true night blue + strip palettes)

## Calm skyboxes match your gradient strips
Night and midnight are **deep navy blue** again — not the phase-5.4 purple/pink vault.

Root cause: the sky shader treated dark night fog as “storm”, then defaulted the
whole vault to magenta. That gate is gone for calm weather. Purple/pink remains
**only** for real storm phases ~5.4–5.9 (McsmPhaseSky + phase-tinted fog).

Palettes sampled from your strips:
- **Midnight** — deep navy zenith → bright blue horizon glow
- **Night** — soft periwinkle / lavender-blue vault
- **Day** — sky blue → pale lavender → muted lilac horizon
- **Sunset** — teal vault → orange belly → crimson rim

## Looking straight up
Soft volumetric cloud decks (paintDecks + overhead sticker mass) instead of the
blocky vanilla Minecraft cloud sticker. Vanilla cloud plane feathered to soft white.

## Also
- StoryModeSkyTint fog/sky colours retuned to the same strip palettes (deep blue night)
- Iris pack `gbuffers_skybasic` + storylook `position.fsh` both fixed
- OG CEM pack from 1.9.151 still DEFAULT ON

Install: drop the jar in `mods/`. Keep EMF + ETF. Resource packs `storylook` +
`ogs-cem` stay DEFAULT ENABLED.
