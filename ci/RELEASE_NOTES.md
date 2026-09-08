# Devouring Storms 1.9.166 — sky deep-clean + vivid light bake

Continues 1.9.165 on the permanent 1.9.164 (159 visual) base.

## Sky folders — no more fighting
| Path | Role |
|------|------|
| `minecraft/optifine/sky/world0` | Calm day soft blue, sunset, **navy night/midnight** |
| `dabywitherstormmod/textures/sky` | Calm + **phase-only** refs (teal/purple/pink/twilight) |
| `fabricskyboxes/textures/sky` | **Calm only** (day/night/sunset). Phase PNGs **deleted** so FSB cannot paint purple ambient |

Day avg ~soft MCSM blue (not white/purple). Soft wisps only — **no cube noise**.

## Clouds — no cube vault
Core + Iris + Story Look cloud passes: softer alpha, translucent mass,
feathered edges so looking straight up is decks, not MC block stickers.

## Vivid light / contrast / shadows (baked, always-on)
User: colourful light never showed up. Now forced harder in every path:
- Core `mcsm_visuals` sat 1.38 / contrast 1.22 + deep cloud shadows
- Core terrain hard block-face key + exaggerated ground cloud occlusion
- Core + Story Look lightmap: warm day key, deep blue night, deep shade floor
- Story Look terrain grade sat 1.36 / contrast 1.20
- Iris pack: CONTRAST 1.32, VIBRANCE 1.35, shade harder on block faces
- Gate floors storyModeLighting/Sky strength, bloom, storm shadows to 1.0

Works with **or without Iris** (core shaders always bake the look).

## Kept
159 PhaseSky/Blob/teeth/gray-edge/Formidi path. Debris kill. OptiFine navy night.
