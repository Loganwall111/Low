# Devouring Storms: Story Look (26.1 – 26.2)

Resource pack recreating the Minecraft Story Mode look from the reference
screenshots, measured colour-by-colour:

- **position.fsh/vsh** (the 26.x sky dome): exact three-stop gradients
  sampled from the shots — dawn periwinkle→pink, midday cyan, night deep
  blue — plus nine stacked cloud decks (adjacent pairs, void gaps, clouds
  nested in clouds, front-to-back occlusion, hard ceiling at the top deck)
  and sharpened stars. The vanilla pale horizon wash is gone.
- **position_color.fsh**: sunrise/sunset tint remapped from orange to the
  reference pink-lavender (never orange); everything else passes through.
- **block.fsh**: story grading — lavender-lifted shadows that never crush
  to black, gentle saturation + S-curve contrast, and distance fog blended
  to the exact horizon haze of the current time of day.
- **lightmap.fsh**: soft ambient floor on sky-lit shade (readable outdoor
  shadows like the shots; caves keep vanilla darkness) and a cool tint in
  sky shadow.
- **rendertype_clouds.fsh**: near cloud deck pushed to pure white.
- **sun.png**: small bright disc inside a wide soft halo, as in the dawn
  shot.

Every shader is a strict modification of the real 26.2 vanilla shader
(fog/dynamictransforms UBOs copied verbatim) and is validated offline
against the vanilla includes by `ci/expand_storylook.py` + the committed
glslang on every CI build.

## Honest limits

Core shaders cannot cast projected shadow maps — the soft-shadow look is a
lighting curve in lightmap.fsh. The stacked decks live on the sky dome, so
they sit behind world geometry. True volumetrics would need Iris/OptiFine,
which this pack deliberately does not require.

## Install

Drop the zip (or this folder) into `.minecraft/resourcepacks/` and enable
"Devouring Storms: Story Look". Minecraft 26.1 and newer (min_format 84, no upper cap - the pack
never trips the "broken or incompatible" flag on future updates).
