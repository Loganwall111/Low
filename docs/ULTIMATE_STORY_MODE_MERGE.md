# Ultimate Minecraft: Story Mode Merge

Goal: merge the Winter Storm modpack overrides with **Devouring Storms: The Point of No Return** so one project can deliver the full Minecraft: Story Mode presentation.

## Merge result

The modpack override tree now contains the Devouring Storms visual packs directly:

| Path | Purpose |
| --- | --- |
| `overrides/resourcepacks/01_Devouring_Storms_Story_Look/` | Vanilla/resource-pack Story Mode look. Pastel sky gradients, stacked cloud decks, cleaner sun/moon, pink-lavender dawn/sunset, readable lavender shadows, and MCSM terrain colour grading. |
| `overrides/global_packs/required_resources/01_Devouring_Storms_Story_Look/` | Same pack installed where Global Packs-style loaders can force required resources. |
| `overrides/shaderpacks/Devouring_Storms_Point_of_No_Return_Iris/` | Iris shaderpack version for the bigger cinematic look: storm purple grading, bloom/vignette, horizon glow, aurora, biome skies, and the stronger Point of No Return atmosphere. |
| `overrides/DEVOURING_STORMS_MERGE.md` | Player/modpack load-order notes. |

## How the pieces fit

- **Winter Storm overrides** provide the modpack configs, datapacks, KubeJS scripts, resource pack stack, music/sound packs, FancyMenu assets, and gameplay compatibility settings.
- **Devouring Storms: Story Look** becomes the first/top visual resource layer so the whole pack gets the Story Mode sky/light/colour identity.
- **Point of No Return Iris pack** becomes the optional high-end shader layer for players with Iris/Oculus.
- Existing CWSM/TAW/ESM packs remain available underneath for models, sounds, music, and legacy Wither Storm presentation.

## Recommended player setup

1. Install the modpack with the merged `overrides/` directory.
2. Enable `01_Devouring_Storms_Story_Look` at the top of Resource Packs.
3. Keep model/music packs underneath it.
4. If using Iris/Oculus, select `Devouring_Storms_Point_of_No_Return_Iris` in Shader Packs.
5. Start a fresh world for best structure/config results.

## Next continuation targets

- Replace upstream LFS pointer-only override files with real assets on a machine that has `git-lfs` installed.
- Add a checked modpack manifest if this project needs CurseForge/Modrinth export automation.
- Wire the preferred resource-pack order into `options.txt` or the pack's resource-loader config once the real upstream override file contents are available.
- Continue the story sequence work in the mod code: spawn setup, escalating storm phases, Formidibomb/rocket progression, and Bowels finale polish.
