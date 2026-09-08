# Devouring Storms 1.9.164 — permanent reset to the good base

## Why
1.9.160–163 stacked sky/glare/beam experiments that washed the world pink,
blew out day skies, broke stage-0, blackened teeth, and left flat billboards.
That was not the MCSM look. This build **rolls the visual stack back to
1.9.159** (the last solid build you liked: Totally Accurate models, gray-edge
4–5.9 skins, phase-dynamic teeth, 3D body-glued glare, calm navy night) and
keeps only safe additions.

## Restored from 1.9.159
- McsmPhaseSky / McsmStormBlob / StormSkyDome / StoryModeSkyTint / StormSkins
- Gate + teeth phase tint + debris kill
- Phase 4 gray-edge + phase 6 devourer skins + emissive teeth atlases
- Shader sky vectors from 159
- Totally Accurate OG CEM default path

## Safe keep from later
- OptiFine `world0/sky1–4` overrides: calm day soft lavender, **night/midnight
  deep navy** (sky4 is NOT purple — that was the calm-night purple culprit)
- Mild day sky dim so noon is not pure white

## Explicitly NOT brought back
- Pink full-sky flood / over-amped StormSkyDome
- Phase 6/7 orbital ring field experiments
- Inflated beam cube motes / BeamMoteSpawner overrides
- Beam weather tint forcing pink wash
- 2D billboard kill that also stripped working body detail
- Broken stage-0 atlas rewrite

## Going forward
New MCSM accuracy work is added **on top of this 159 base only**, one system
at a time, with your frames — no more cascade rewrites.
