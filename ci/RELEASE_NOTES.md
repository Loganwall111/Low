# Devouring Storms 1.9.165 — deep clean: sky, light, no cube vault

Built on the permanent 1.9.164 (159 visual) base. Accuracy pass only.

## Sky deep-clean (duplicate folders fixed)
Three calm-sky sources were fighting each other and day was bright purple-white:

| Folder | Role after clean |
|--------|------------------|
| `minecraft/optifine/sky/world0` | Day soft blue, sunset, **navy night/midnight** (sky4 NOT purple) |
| `dabywitherstormmod/textures/sky` | Same calm day/night/sunset + phase-only refs |
| `fabricskyboxes/textures/sky` | **Calm only** (day/night/sunset). Phase PNGs **removed** so FSB cannot load purple/pink as ambient world sky |

Day average ~ (121,137,185) soft blue — was (164,162,230) bright purple. Soft wisps only (no blocky cube noise).

## Calm shader skies dimmed
- Core `sky.fsh` + Iris `gbuffers_skybasic` + Story Look `position.fsh`
- Soft MCSM blue day, deep navy night (not lavender night)
- Overhead cloud sticker softened (less cube-like looking up)

## Vivid light / contrast / shadows (user: never showed up)
Baked stronger into always-on core shaders (works without Iris):
- Story grade sat 1.38 / contrast 1.22
- Harder block-face sun key + deeper shade side
- Stronger moving cloud shadows on ground
- Lightmap: warm day key, deep blue night fill, hotter torches, deeper shade floor
- Gate floors `storyModeLightingStrength` / coloured light to 1.0
- Iris pack final: more contrast/vibrance, less exposure blowout

## Clouds
Vanilla cloud pass more translucent soft mass (not MC cube stickers).

## Not touched (159 base kept)
PhaseSky / Blob / Formidi / gray-edge skins / teeth ladder / Debris kill.
