#!/usr/bin/env python3
"""Convert clean Story Mode schematic/NBT exports into vanilla structure templates.

Inputs expected by the project:
  world_data_temp/MC105/   -> Sky City / Skylands clean schematic exports
  world_data_temp/MC201/   -> Beacontown clean schematic exports

Outputs:
  src/main/resources/assets/<modid>/structures/sky_city.nbt
  src/main/resources/assets/<modid>/structures/beacontown.nbt
  src/main/resources/data/<modid>/structure/sky_city.nbt
  src/main/resources/data/<modid>/structure/beacontown.nbt
  jar-overrides/assets/<modid>/structures/sky_city.nbt
  jar-overrides/data/<modid>/structure/sky_city.nbt

The assets/ path is written because the user requested that exact location.
The data/ path is also written because Minecraft's StructureTemplateManager
loads datapack structure templates from data/<namespace>/structure/*.nbt in this
26.2 workspace.

This script deliberately uses nbtlib/nbttag-style NBT automation instead of
manual inspection. It supports:
  * vanilla structure NBT files (palette/blocks/size)
  * Sponge .schem palette files
  * legacy MCEdit .schematic Blocks/Data files for a core vanilla block set

If a source folder is missing, the script exits successfully with a clear
warning so CI/workspace setup can proceed before the large structure uploads are
present. Re-run it after adding world_data_temp/MC105 and world_data_temp/MC201.
"""
from __future__ import annotations

import argparse
import gzip
import os
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Tuple

try:
    import nbtlib
    from nbtlib import Byte, Compound, Int, List as NbtList, String
except Exception as exc:  # pragma: no cover - actionable CLI error
    print("[convert] missing Python NBT library: install nbtlib or nbttag first", file=sys.stderr)
    print(f"[convert] import error: {exc}", file=sys.stderr)
    raise SystemExit(2)

AIR_NAMES = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void", "air"}
DATA_VERSION = 3955  # harmless modern template marker; Minecraft will datafix if needed

# Minimal legacy numeric-id mapping. Sponge/vanilla NBT exports should carry a
# palette and do not need this table. Legacy .schematic inputs use it as a safe
# fallback for common world blocks.
LEGACY_ID = {
    0: "minecraft:air", 1: "minecraft:stone", 2: "minecraft:grass_block", 3: "minecraft:dirt",
    4: "minecraft:cobblestone", 5: "minecraft:oak_planks", 6: "minecraft:oak_sapling", 7: "minecraft:bedrock",
    8: "minecraft:water", 9: "minecraft:water", 10: "minecraft:lava", 11: "minecraft:lava",
    12: "minecraft:sand", 13: "minecraft:gravel", 14: "minecraft:gold_ore", 15: "minecraft:iron_ore",
    16: "minecraft:coal_ore", 17: "minecraft:oak_log", 18: "minecraft:oak_leaves", 19: "minecraft:sponge",
    20: "minecraft:glass", 21: "minecraft:lapis_ore", 22: "minecraft:lapis_block", 24: "minecraft:sandstone",
    25: "minecraft:note_block", 26: "minecraft:white_bed", 27: "minecraft:powered_rail", 28: "minecraft:detector_rail",
    29: "minecraft:sticky_piston", 30: "minecraft:cobweb", 31: "minecraft:grass", 32: "minecraft:dead_bush",
    33: "minecraft:piston", 35: "minecraft:white_wool", 37: "minecraft:dandelion", 38: "minecraft:poppy",
    39: "minecraft:brown_mushroom", 40: "minecraft:red_mushroom", 41: "minecraft:gold_block", 42: "minecraft:iron_block",
    43: "minecraft:smooth_stone_slab", 44: "minecraft:smooth_stone_slab", 45: "minecraft:bricks",
    46: "minecraft:tnt", 47: "minecraft:bookshelf", 48: "minecraft:mossy_cobblestone", 49: "minecraft:obsidian",
    50: "minecraft:torch", 51: "minecraft:fire", 52: "minecraft:spawner", 53: "minecraft:oak_stairs",
    54: "minecraft:chest", 55: "minecraft:redstone_wire", 56: "minecraft:diamond_ore", 57: "minecraft:diamond_block",
    58: "minecraft:crafting_table", 59: "minecraft:wheat", 60: "minecraft:farmland", 61: "minecraft:furnace",
    62: "minecraft:furnace", 63: "minecraft:oak_sign", 64: "minecraft:oak_door", 65: "minecraft:ladder",
    66: "minecraft:rail", 67: "minecraft:cobblestone_stairs", 68: "minecraft:oak_wall_sign", 69: "minecraft:lever",
    70: "minecraft:stone_pressure_plate", 71: "minecraft:iron_door", 72: "minecraft:oak_pressure_plate",
    73: "minecraft:redstone_ore", 76: "minecraft:redstone_torch", 77: "minecraft:stone_button",
    78: "minecraft:snow", 79: "minecraft:ice", 80: "minecraft:snow_block", 81: "minecraft:cactus",
    82: "minecraft:clay", 83: "minecraft:sugar_cane", 84: "minecraft:jukebox", 85: "minecraft:oak_fence",
    86: "minecraft:pumpkin", 87: "minecraft:netherrack", 88: "minecraft:soul_sand", 89: "minecraft:glowstone",
    90: "minecraft:nether_portal", 91: "minecraft:jack_o_lantern", 95: "minecraft:white_stained_glass",
    96: "minecraft:oak_trapdoor", 98: "minecraft:stone_bricks", 101: "minecraft:iron_bars",
    102: "minecraft:glass_pane", 103: "minecraft:melon", 106: "minecraft:vine", 107: "minecraft:oak_fence_gate",
    108: "minecraft:brick_stairs", 109: "minecraft:stone_brick_stairs", 110: "minecraft:mycelium",
    111: "minecraft:lily_pad", 112: "minecraft:nether_bricks", 113: "minecraft:nether_brick_fence",
    114: "minecraft:nether_brick_stairs", 121: "minecraft:end_stone", 123: "minecraft:redstone_lamp",
    126: "minecraft:oak_slab", 128: "minecraft:sandstone_stairs", 129: "minecraft:emerald_ore",
    131: "minecraft:tripwire_hook", 133: "minecraft:emerald_block", 134: "minecraft:spruce_stairs",
    135: "minecraft:birch_stairs", 136: "minecraft:jungle_stairs", 139: "minecraft:cobblestone_wall",
    145: "minecraft:anvil", 152: "minecraft:redstone_block", 155: "minecraft:quartz_block",
    156: "minecraft:quartz_stairs", 159: "minecraft:white_terracotta", 160: "minecraft:white_stained_glass_pane",
    161: "minecraft:acacia_leaves", 162: "minecraft:acacia_log", 163: "minecraft:acacia_stairs",
    164: "minecraft:dark_oak_stairs", 168: "minecraft:prismarine", 169: "minecraft:sea_lantern",
    170: "minecraft:hay_block", 171: "minecraft:white_carpet", 172: "minecraft:terracotta",
    173: "minecraft:coal_block", 174: "minecraft:packed_ice", 175: "minecraft:sunflower",
}

@dataclass
class TemplateData:
    name: str
    size: Tuple[int, int, int]
    palette: List[Compound]
    blocks: List[Compound]
    entities: List[Compound]


def as_int(v) -> int:
    return int(v.value if hasattr(v, "value") else v)


def load_nbt(path: Path):
    try:
        return nbtlib.load(path, gzipped=None)
    except Exception:
        with gzip.open(path, "rb") as fh:
            return nbtlib.File.parse(fh)


def block_name_from_palette_entry(entry) -> str:
    if isinstance(entry, str):
        return entry
    if hasattr(entry, "value") and isinstance(entry.value, str):
        return entry.value
    if isinstance(entry, Compound):
        if "Name" in entry:
            return str(entry["Name"])
    return str(entry)


def normalize_palette_entry(name: str) -> Compound:
    name = name.strip('"')
    if ":" not in name:
        name = "minecraft:" + name
    return Compound({"Name": String(name)})


def from_vanilla_structure(path: Path, root: Compound) -> TemplateData:
    size = tuple(as_int(x) for x in root["size"])
    palette = [p if isinstance(p, Compound) else normalize_palette_entry(str(p)) for p in root["palette"]]
    blocks = []
    for b in root.get("blocks", []):
        pos = [as_int(x) for x in b["pos"]]
        state = as_int(b["state"])
        blocks.append(Compound({"pos": NbtList[Int]([Int(pos[0]), Int(pos[1]), Int(pos[2])]), "state": Int(state)}))
    entities = [e for e in root.get("entities", [])]
    return crop_template(path.stem, size, palette, blocks, entities)


def from_sponge(path: Path, root: Compound) -> TemplateData:
    w = as_int(root.get("Width", root.get("width", 0)))
    h = as_int(root.get("Height", root.get("height", 0)))
    l = as_int(root.get("Length", root.get("length", 0)))
    palette_tag = root.get("Palette", root.get("palette", {}))
    id_to_name: Dict[int, str] = {}
    for k, v in palette_tag.items():
        id_to_name[as_int(v)] = str(k)
    raw = root.get("BlockData", root.get("blockData"))
    if raw is None:
        raise ValueError("Sponge schematic has no BlockData")
    # Sponge BlockData is varint-encoded palette indexes.
    vals = []
    value = 0
    shift = 0
    for byte in bytes(raw):
        value |= (byte & 0x7F) << shift
        if (byte & 0x80) == 0:
            vals.append(value)
            value = 0
            shift = 0
        else:
            shift += 7
    palette = [normalize_palette_entry(id_to_name[i]) for i in sorted(id_to_name)]
    index_remap = {old: new for new, old in enumerate(sorted(id_to_name))}
    blocks: List[Compound] = []
    for idx, old_state in enumerate(vals[: w * h * l]):
        x = idx % w
        z = (idx // w) % l
        y = idx // (w * l)
        name = id_to_name.get(old_state, "minecraft:air")
        if name in AIR_NAMES:
            continue
        blocks.append(Compound({"pos": NbtList[Int]([Int(x), Int(y), Int(z)]), "state": Int(index_remap[old_state])}))
    return crop_template(path.stem, (w, h, l), palette, blocks, [])


def from_legacy_schematic(path: Path, root: Compound) -> TemplateData:
    w = as_int(root["Width"])
    h = as_int(root["Height"])
    l = as_int(root["Length"])
    raw = bytes(root["Blocks"])
    palette_names: List[str] = []
    palette_index: Dict[str, int] = {}
    blocks: List[Compound] = []
    for idx, bid in enumerate(raw[: w * h * l]):
        name = LEGACY_ID.get(bid, "minecraft:stone")
        if name in AIR_NAMES:
            continue
        if name not in palette_index:
            palette_index[name] = len(palette_names)
            palette_names.append(name)
        x = idx % w
        z = (idx // w) % l
        y = idx // (w * l)
        blocks.append(Compound({"pos": NbtList[Int]([Int(x), Int(y), Int(z)]), "state": Int(palette_index[name])}))
    palette = [normalize_palette_entry(n) for n in palette_names]
    return crop_template(path.stem, (w, h, l), palette, blocks, [])


def crop_template(name: str, size: Tuple[int, int, int], palette: List[Compound], blocks: List[Compound], entities: List[Compound]) -> TemplateData:
    solid = []
    for b in blocks:
        state = as_int(b["state"])
        pname = block_name_from_palette_entry(palette[state]) if 0 <= state < len(palette) else "minecraft:air"
        if pname not in AIR_NAMES:
            solid.append(b)
    if not solid:
        return TemplateData(name, (1, 1, 1), [normalize_palette_entry("minecraft:air")], [], [])
    xs = [as_int(b["pos"][0]) for b in solid]
    ys = [as_int(b["pos"][1]) for b in solid]
    zs = [as_int(b["pos"][2]) for b in solid]
    minx, miny, minz = min(xs), min(ys), min(zs)
    maxx, maxy, maxz = max(xs), max(ys), max(zs)
    out_blocks = []
    for b in solid:
        x, y, z = (as_int(b["pos"][0]) - minx, as_int(b["pos"][1]) - miny, as_int(b["pos"][2]) - minz)
        nb = Compound({"pos": NbtList[Int]([Int(x), Int(y), Int(z)]), "state": Int(as_int(b["state"]))})
        if "nbt" in b:
            nb["nbt"] = b["nbt"]
        out_blocks.append(nb)
    return TemplateData(name, (maxx - minx + 1, maxy - miny + 1, maxz - minz + 1), palette, out_blocks, entities)


def read_template(path: Path) -> TemplateData:
    nbt = load_nbt(path)
    root = nbt.root if hasattr(nbt, "root") else nbt
    if "palette" in root and "blocks" in root and "size" in root:
        return from_vanilla_structure(path, root)
    if "Palette" in root and "BlockData" in root:
        return from_sponge(path, root)
    if "Blocks" in root and "Width" in root and "Height" in root and "Length" in root:
        return from_legacy_schematic(path, root)
    raise ValueError(f"unrecognized structure NBT format: {path}")


def write_template(t: TemplateData, out: Path) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    root = Compound({
        "DataVersion": Int(DATA_VERSION),
        "size": NbtList[Int]([Int(t.size[0]), Int(t.size[1]), Int(t.size[2])]),
        "palette": NbtList[Compound](t.palette),
        "blocks": NbtList[Compound](t.blocks),
        "entities": NbtList[Compound](t.entities),
    })
    nbtlib.File(root).save(out, gzipped=True)


def candidates(folder: Path, terms: Iterable[str]) -> List[Path]:
    files = [p for p in folder.rglob("*") if p.is_file() and p.suffix.lower() in {".nbt", ".schem", ".schematic"}]
    scored = []
    for p in files:
        low = p.name.lower()
        score = sum(1 for t in terms if t in low)
        if score:
            scored.append((score, len(p.parts), p))
    return [p for _score, _depth, p in sorted(scored, key=lambda x: (-x[0], x[1], str(x[2]).lower()))]


def convert_one(label: str, folder: Path, terms: List[str], modid: str, root: Path) -> bool:
    if not folder.exists():
        print(f"[convert] WARNING: {label} source folder missing: {folder}")
        return False
    picks = candidates(folder, terms)
    if not picks:
        print(f"[convert] WARNING: no {label} schematic/NBT candidate found in {folder}")
        return False
    src = picks[0]
    print(f"[convert] {label}: using {src}")
    t = read_template(src)
    stem = "sky_city" if label == "sky_city" else "beacontown"
    asset_out = root / "src/main/resources/assets" / modid / "structures" / f"{stem}.nbt"
    data_out = root / "src/main/resources/data" / modid / "structure" / f"{stem}.nbt"
    jar_asset_out = root / "jar-overrides/assets" / modid / "structures" / f"{stem}.nbt"
    jar_data_out = root / "jar-overrides/data" / modid / "structure" / f"{stem}.nbt"
    write_template(t, asset_out)
    for mirror in (data_out, jar_asset_out, jar_data_out):
        mirror.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(asset_out, mirror)
    print(f"[convert] {label}: {t.size} {len(t.blocks)} blocks -> {asset_out}")
    print(f"[convert] {label}: runtime copy -> {data_out}")
    print(f"[convert] {label}: jar override copies -> {jar_asset_out}, {jar_data_out}")
    return True


def main(argv=None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    ap.add_argument("--modid", default="dabywitherstormmod")
    ap.add_argument("--require", action="store_true", help="fail when folders/files are absent")
    args = ap.parse_args(argv)
    root = Path(args.root).resolve()
    ok1 = convert_one("sky_city", root / "world_data_temp/MC105", ["skylandtown", "sky_city", "skycity", "skyland", "sky"], args.modid, root)
    ok2 = convert_one("beacontown", root / "world_data_temp/MC201", ["beacontown", "beacon_town", "adv_beacontown"], args.modid, root)
    if args.require and not (ok1 and ok2):
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
