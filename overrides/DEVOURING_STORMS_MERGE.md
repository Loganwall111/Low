# Devouring Storms + Winter Storm Overrides Merge

This `overrides/` folder is now merged with the current **Devouring Storms: The Point of No Return** work so the Winter Storm modpack can ship the Minecraft: Story Mode look as a single install.

## What was merged

- `overrides/` from the separate modpack project was imported into this branch.
- `overrides/resourcepacks/01_Devouring_Storms_Story_Look/` was added from the repo's `storylook/` pack.
- `overrides/global_packs/required_resources/01_Devouring_Storms_Story_Look/` was added with the same pack so Global Packs/required-resource loaders can force the look on worlds that support it.
- `overrides/shaderpacks/Devouring_Storms_Point_of_No_Return_Iris/` was added from `shaderpack-v5/` for Iris/OptiFine-style shader users.

## Recommended load order

Use this order in the Resource Packs screen, top to bottom:

1. `01_Devouring_Storms_Story_Look`
2. `06_TAW-OG-NO-DEBRIS-EDIT.zip`
3. `05_TAW MODELS NO DIBRIS.zip`
4. `04_CWSMPLUS RP 5.5 ESM EDIT.zip`
5. `03_3dtextures.zip`
6. `02_VanillaTweaks neww.zip`
7. Music/sound packs such as `08_ESM Music.zip` and `ESMSoundPack.zip`

The Devouring Storms Story Look pack should stay at the top because it owns the Minecraft core-shader sky, lightmap, clouds, sun/moon, and Story Mode colour grade.

## Shader pack choice

If Iris/Oculus is installed, select:

`Devouring_Storms_Point_of_No_Return_Iris`

That shader pack carries the stronger Point of No Return cinematic pass: saturated Story Mode colour, purple storm presence, bloom/vignette options, biome skies, horizon glow, aurora, and storm cloud-deck rendering.

If Iris/Oculus is not installed, the resource pack still provides the core Story Look through vanilla-compatible resource-pack shaders.

## Notes

Some imported override assets are stored in Git as LFS pointer files in the source branch. This repository does not currently have `git-lfs` available in the sandbox, so those pointers are preserved exactly as they existed upstream. The live Devouring Storms packs added from this branch are real files, not pointers.
