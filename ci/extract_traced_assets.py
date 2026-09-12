#!/usr/bin/env python3
"""Extract the embedded textures used by the shipped Telltale CEM models.

The source repository is intentionally not a runtime dependency. Run this tool
from a checkout of Loganwall111/gggggrff::

  python3 ci/extract_traced_assets.py \
    "/tmp/gggggrff/ALL wither storm assets from MCSM for blockbench you'll ever need V0.9 4.zip"

Only the image sources referenced by model faces are copied.  The seven small
CEM atlases are resized from the most-used embedded material so the existing
voxelised JEM geometry can consume them without parsing a .bbmodel at runtime.
The original, full-resolution material files are also shipped under `traced/`
for models/tools that need their native baked UVs. Embedded PNG/WebP sources are
converted offline to normal PNG files. No animation data is read.
"""
from __future__ import annotations

import base64
import json
import os
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from pngutil import read_png, scale, write_png  # noqa: E402

PACK_ROOT = Path("ogs-cem/assets")
# CEM's JEM files live in the minecraft namespace.  Keeping the selected atlas
# there avoids depending on a namespace/path quirk in OptiFine or EMF.
MINECRAFT_TEX = PACK_ROOT / "minecraft/textures/entity/cem"
DABY_TEX = PACK_ROOT / "dabywitherstormmod/textures/entity/cem/traced"

MODELS = {
    "stage_a": "Stage_A/witherstormStageA.bbmodel",
    "stage_a_debris": "Stage_A/witherstormStageA_with_debris.bbmodel",
    "stage_b": "Traced_shading_Textures/witherstormStageB (with traced shading textures).bbmodel",
    "stage_c_small": "Traced_shading_Textures/witherstormStageC_Small (with traced shading textures).bbmodel",
    "stage_c_big": "Traced_shading_Textures/witherstormStageC_Big (with traced shading textures).bbmodel",
    "stage_c_massive": "Traced_shading_Textures/witherstormStageC_Massive (with traced shading textures).bbmodel",
    "stage_d_small": "Traced_shading_Textures/witherstormStageD_Center_Small (with traced shading textures).bbmodel",
    "stage_d_massive": "Traced_shading_Textures/witherstormStageD_Center_Massive.bbmodel",
    "stage_d_big_mass": "Traced_shading_Textures/witherstormStageD_Center_big mass motherfucker (with in game accurate textures).bbmodel",
    # These are material sources for the animated debris/split pass.  They are
    # not selected as CEM body stages, so the game keeps its existing animation
    # implementation rather than importing Blockbench tracks.
    "stage_c_debris": "Stage_C/witherstormStageC_Massive_With_Debris_INACC.bbmodel",
    "stage_d_split": "Traced_shading_Textures/witherstormStageD_Center_No_mass (with traced shading textures).bbmodel",
}

JEM_ATLAS = {
    "wither_storm1.jem": "stage_a",
    "wither_storm2.jem": "stage_a_debris",
    "wither_storm3.jem": "stage_b",
    "wither_storm4.jem": "stage_c_small",
    "wither_storm5.jem": "stage_c_big",
    "wither_storm6.jem": "stage_c_massive",
    "wither_storm7.jem": "stage_d_massive",
}


def safe_name(name: str) -> str:
    name = re.sub(r"[^A-Za-z0-9_.-]+", "_", name)
    return name.strip("._") or "material.png"


def embedded_png(texture: dict) -> bytes | None:
    source = texture.get("source", "")
    if not isinstance(source, str) or not source.startswith("data:image/"):
        return None
    try:
        raw = base64.b64decode(source.split(",", 1)[1])
    except (ValueError, IndexError):
        return None
    if raw.startswith(b"\x89PNG\r\n\x1a\n"):
        return raw
    # A few of the supplied traced files contain WebP bytes with a .png name.
    # Decode those offline with the same ImageMagick tool already used by the
    # repo's asset pipeline; nothing performs this work during world loading.
    try:
        result = subprocess.run(["convert", "-", "png:-"], input=raw,
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                check=True)
        return result.stdout if result.stdout.startswith(b"\x89PNG\r\n\x1a\n") else None
    except (OSError, subprocess.CalledProcessError):
        return None


def used_indices(model: dict) -> list[int]:
    used: set[int] = set()
    for element in model.get("elements", []):
        for face in element.get("faces", {}).values():
            index = face.get("texture")
            if isinstance(index, int) and index >= 0:
                used.add(index)
    return sorted(used)


def main(root_arg: str) -> int:
    root = Path(root_arg)
    if not root.is_dir():
        raise SystemExit(f"source model directory does not exist: {root}")
    MINECRAFT_TEX.mkdir(parents=True, exist_ok=True)
    DABY_TEX.mkdir(parents=True, exist_ok=True)
    manifest: dict[str, object] = {
        "source": "Loganwall111/gggggrff",
        "pack": "Traced_shading_Textures plus Stage_A/Stage_C debris material sources",
        "animations": "not imported; existing in-game animation code remains authoritative",
        "models": {},
    }

    for key, relative in MODELS.items():
        path = root / relative
        if not path.is_file():
            print(f"warning: missing source model: {path}", file=sys.stderr)
            continue
        model = json.loads(path.read_text(encoding="utf-8", errors="replace"))
        textures = model.get("textures", [])
        indices = used_indices(model)
        materials: list[dict[str, object]] = []
        decoded: list[tuple[int, str, bytes]] = []
        for index in indices:
            if index >= len(textures):
                continue
            texture = textures[index]
            raw = embedded_png(texture)
            if raw is None:
                continue
            name = safe_name(str(texture.get("name") or f"material_{index}.png"))
            # Keep each model's embedded material namespace separate: same
            # Blockbench filename can represent different phase palettes.
            out = DABY_TEX / key / f"{index:02d}_{name}"
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_bytes(raw)
            decoded.append((index, name, raw))
            materials.append({"index": index, "name": name, "path": str(out)})

        # The dominant face material is the useful atlas for the existing JEM
        # voxel pass.  Preserve the embedded image's colours and alpha; this is
        # a scale operation only, not a hand-authored replacement palette.
        dominant = None
        counts = {}
        for element in model.get("elements", []):
            for face in element.get("faces", {}).values():
                index = face.get("texture")
                if isinstance(index, int):
                    counts[index] = counts.get(index, 0) + 1
        for index, name, raw in decoded:
            if dominant is None or counts.get(index, 0) > counts.get(dominant[0], 0):
                dominant = (index, name, raw)
        if dominant is not None:
            index, name, raw = dominant
            tmp = Path("/tmp") / f"mcsm-traced-{key}-{index}.png"
            tmp.write_bytes(raw)
            try:
                width, height, pixels = read_png(str(tmp))
                atlas = MINECRAFT_TEX / f"wither_storm_{key}.png"
                write_png(str(atlas), 160, 160, scale(pixels, width, height, 160, 160))
                # Keep a generated emissive companion so EMF/OptiFine's
                # suffix.emissive rule has an explicit phase asset. It starts
                # transparent; the native head renderer supplies the eyes and
                # teeth emission separately, avoiding a painted glow mask.
                write_png(str(MINECRAFT_TEX / f"wither_storm_{key}_e.png"), 160, 160,
                          [(0, 0, 0, 0)] * (160 * 160))
                atlas_info = {"source_material_index": index, "source_material": name,
                              "source_size": [width, height], "atlas": str(atlas)}
            except ValueError as exc:
                print(f"warning: cannot decode {key}/{name}: {exc}", file=sys.stderr)
                atlas_info = None
        else:
            atlas_info = None

        manifest["models"][key] = {"model": relative, "materials": materials, "atlas": atlas_info}
        print(f"{key}: {len(materials)} embedded materials -> {atlas_info or 'no atlas'}")

    (PACK_ROOT / "traced_asset_manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    # Point the already-generated phase geometry at the extracted atlas.  This
    # is intentionally a small textual edit, preserving the generated JEM
    # geometry and keeping model loading offline/constant-time at runtime.
    jem_dir = Path("ogs-cem/assets/minecraft/optifine/cem/dabywitherstormmod")
    for jem_name, stage in JEM_ATLAS.items():
        path = jem_dir / jem_name
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        if '"texture":' not in text.split('"models"', 1)[0]:
            marker = '"credit": "Telltale 1:1 meshes (extracted by wheatlycrab5892, voxelised by Devouring Storms)",\n'
            replacement = marker + f'\t"texture": "textures/entity/cem/wither_storm_{stage}.png",\n'
            if marker not in text:
                raise RuntimeError(f"unexpected JEM header: {path}")
            text = text.replace(marker, replacement, 1)
            path.write_text(text, encoding="utf-8")
            print(f"linked {jem_name} -> wither_storm_{stage}.png")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: extract_traced_assets.py <gggggrff model directory>")
    raise SystemExit(main(sys.argv[1]))
