# Devouring Storms 1.9.153 — phase sky lock + fade glare + calm blue night

## Purple out of night / into the storm only
Calm night and midnight are **deep navy blue**. The purple/pink vault is
**phase-locked** and only paints when a real storm is nearby:

| Phase | Sky / glare |
|-------|-------------|
| 5.0–5.35 | teal / turquoise (user phase-5 strip) |
| 5.35–5.55 | purple (user phase-5.4 strip) |
| 5.55–5.95 | **pink / magenta / black** (5.5 was skipped — restored) |
| 5.95+ | deep purple + rose (phase 6+ strip; 5.5 has no red/orange) |

## Backdrops (face removed)
- Face-painted / three-head backdrop: **gone** (`storm_face.png` is transparent)
- Kept only: **black soft blur**, **green/teal (p5)**, **purple (p5.4)**, **pink-magenta-black (p5.5+)**
- Glare is a **soft fade gradient** (not an opaque ball), multi-depth, glued to storm + sky

## Calm day
Day vault is **lavender** (skyday strip), not orange. Sunset orange only at real dusk.

## Fog / shaders
`StormSkyDome` + `StormPalettes` only tint fog at phase 5+. Shader storm gates
ignore calm blue night so final.fsh / skybasic never purple-wash midnight.
