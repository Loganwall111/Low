# Devouring Storms 1.9.162 — emergency repair (broken look)

**Not normal.** 1.9.160/161 left several systems broken. This build repairs them.

## Kill ALL 2D body billboards
McsmStormBlob no longer paints face plates, silhouette quads, glossy stripe
plates, or any flat backdrop behind the storm. Those read as broken 2D cards.

## Glare rebuilt from scratch (3D only)
McsmPhaseSky owns the glare exclusively as nested world-space spheres:
- **Phase 5** — green/teal volume (was missing)
- **Phase 5.4** — purple volume
- **Phase 5.5** — purple→pink twilight strip (user sky_twilight.png)
- **Phase 6+** — black-blue core + orbital rings (from 161)
Amp raised; shell layers denser so the volume actually reads.

## Day sky no longer blown white
StoryModeSkyTint + sky shaders + OptiFine sky1 dimmed. Muted lavender day,
not empty white.

## Phase fog restored
StormSkyDome teal/purple/pink decks stronger so phase 5 green and 5.5 purple
actually wash the air around the storm.

## Teeth fixed (were black)
- All phase emissive atlases regenerated with real bright teeth/eyes
- TeethPhaseTint always drives colour (even early phases → light blue)
- Gate floors turquoiseTeethIntensity + eyeColor

## Stage 0 Formidi fixed
64×96 rebuilt from MCSM maxresdefault: 3 black heads with white eyes +
command-block body with coloured button grid (not the glitched gray mass).

## Still on HUD
If HUD says 1.9.160 you are still on the old jar — install **1.9.162**.

## Kept
OptiFine navy night, weather beams, phase6/7 mega rings, debris ring,
StormDebris kill, gray-edge 4–5.9 skins.
