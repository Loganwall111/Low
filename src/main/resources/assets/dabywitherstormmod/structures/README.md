# Devouring Storms structure blueprints

`ci/convert_story_worlds.py` writes the converted Story Mode blueprints here:

- `sky_city.nbt`
- `beacontown.nbt`

The same files are mirrored to `src/main/resources/data/dabywitherstormmod/structure/`
because Minecraft's `StructureTemplateManager` loads runtime structure templates
from the data-pack structure path.

The current workspace did not contain `world_data_temp/MC105/` or
`world_data_temp/MC201/`, so the converter could not generate the final NBT
blueprints yet. Add those folders and rerun:

```bash
python3 ci/convert_story_worlds.py --require
```
