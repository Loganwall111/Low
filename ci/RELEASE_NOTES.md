# Devouring Storms 1.9.155 — kill face halo, body-glued glare, calm blue night, toned body

## Floating face / three-head halo — GONE
`StormPresenceFX` is fully cancelled every frame (not just knobs off). That kills:
- the three-head / face halo texture (`halo_ring.png`)
- floating atmosphere-pulse spheres
- black-glare symbol far behind the storm

`storm_face.png` stays fully transparent. Gate forces `cataclysmHalos`,
`blackGlare`, and `atmospherePulse` OFF permanently.

## Glare attached to the storm (not floating mid-air)
Phase glare is multi-depth soft-fade plates placed **on the storm centre**
(body radius), not a fixed far sky disc. It moves with the body. Soft long
falloff so edges fade into the sky — **no giant opaque sphere**.

Phase vault alpha only brightens toward the storm bearing and dissolves at
the rim so gradients blend instead of reading as a hard ball.

## Calm night is deep navy (not phase-5.5 purple)
Purple/pink vault is **phase-locked** (5.0 teal → 5.4 purple → 5.5 pink → 6+).
Calm midnight/night uses the deep navy strip. Fog + shader storm gates ignore
calm blue night so residual phase fog cannot purple-wash the vault.

## Body textures toned
OG wither_storm skins pushed toward reference: blacker mass, cooler blue sheen,
less brown midtone. Early Formidi command-block path still uses base
`WitherCommandBlock` + CEM ladder (phase 0–4.5 → jem model 1).

## Also
- Phase 5.5 dark blue-black upper-body silhouette (from 1.9.154) kept, body-glued
- Shell alpha softened further
