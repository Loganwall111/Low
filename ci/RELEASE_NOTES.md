# Devouring Storms 1.9.160 — OptiFine purple sky kill, beams, debris ring, stage0, NPCs

## Purple sky folder — FIXED
OptiFine `assets/minecraft/optifine/sky/world0/sky1–4.png` now ships as jar
overrides. **sky4 was the purple/magenta calm-night culprit** from extracted
1.9.158 jars. All four skies rewritten:
- sky1 day lavender-blue
- sky2 sunset
- sky3 / sky4 **deep navy midnight** (never purple)

Phase purple stays on the storm halo / McsmPhaseSky only (≥5.4 / 5.5).
FabricSkyBoxes + dabywitherstormmod night textures re-stamped navy.

## Tractor beams — weather + density rewrite
- **Clear** → blue beam; **rain/thunder** → pink/magenta (hot pink at 5.5+)
- `McsmBeamWeatherTint` drives `beamColor*` every frame
- Motes denser (14/tick), **larger**, **synchronized upward climb** (shared SYNC_CLIMB)
- Preview motes denser/larger; colours follow live beam config
- Ground crumbs boosted: denser `BLOCK` particles matching actual floor blocks
  (phase 4+ pulled dirt/stone/grass — not glacier cards)

## Debris ring — new (StormDebris cube swarm stays KILLED)
Orbiting dark mass chunks around the lower body from phase 4+, gray-edge on
pre-6, blue sheen edge on 6+. Not the StormDebris cube halo.

## Halo / phase volume
Shell radii + alphas tightened so the 3D gradient stays a body-glued volume,
not a sky flood. 5.5 pink-magenta rim retuned from user sky refs.

## Stage 0 Formidi atlas
64×96 rebuilt: three black heads with white eye slots + command-block body
with coloured button grid (MCSM Formidi CB wither look).

## Characters (first pass)
- Distinct entity types per cast member (not every face a plain villager)
- Speak anim: look-at player + hop + villager yes/ambient
- Dialogue trees unchanged

## Kept from 1.9.159
Phase 4–5.9 gray-edge black skins, phase-6 black+blue, phase-dynamic teeth
ladder, baked Story Look (Iris optional), StormDebris kill.
