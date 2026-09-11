#!/usr/bin/env python3
"""
build_mcsm_packs.py — Package the MCSM resource pack and shader pack.

The source of truth is the committed directories (MCSM_ResourcePack/,
MCSM_ShaderPack/) — the packs are built from the working tree, never
regenerated from scratch. Clouds are 100% procedural GLSL (fractal noise,
2.5x vertex extrusion); the shader pack must contain zero PNG cloud sheets.

Layout: flat zips (files at the zip root), matching what Iris / OptiFine and
the Minecraft resource-pack loader expect.

Run from the repository root (the CI workflows and humans both call
`python3 tools/build_mcsm_packs.py` with no arguments).
"""

import hashlib
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RP_DIR = os.path.join(ROOT, "MCSM_ResourcePack")
SP_DIR = os.path.join(ROOT, "MCSM_ShaderPack")
RP_ZIP = os.path.join(ROOT, "MCSM_ResourcePack.zip")
SP_ZIP = os.path.join(ROOT, "MCSM_ShaderPack.zip")


def zip_dir(src_dir: str, out_zip: str) -> list[str]:
    if not os.path.isdir(src_dir):
        print(f"missing pack directory: {src_dir}")
        sys.exit(1)
    if os.path.exists(out_zip):
        os.remove(out_zip)
    names: list[str] = []
    for dirpath, _dirnames, filenames in os.walk(src_dir):
        for name in sorted(filenames):
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, src_dir).replace(os.sep, "/")
            names.append(rel)
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as z:
        for rel in names:
            z.write(os.path.join(src_dir, rel.replace("/", os.sep)), rel)
    return names


def validate_shader_pack(names: list[str]) -> None:
    """Hard fail if PNG cloud sheets or cloudTex bindings sneak back in."""
    cloud_pngs = [n for n in names if "cloud" in n.lower() and n.lower().endswith(".png")]
    if cloud_pngs:
        print(f"FATAL: {len(cloud_pngs)} PNG cloud sheet(s) found in shader pack: {cloud_pngs[:3]} ...")
        sys.exit(1)
    props = [n for n in names if n.endswith("shaders.properties")]
    for p in props:
        with open(os.path.join(ROOT, "MCSM_ShaderPack", p.replace("/", os.sep)), "r") as fh:
            content = fh.read()
        if "cloudTex" in content:
            print(f"FATAL: shader pack {p} still binds cloudTex samplers")
            sys.exit(1)


REQUIRED_SKY_PNGS = [
    "sky_day.png",
    "sky_sunset.png",
    "sky_night.png",
    "sky_midnight.png",
    "sky_storm.png",
]
REQUIRED_SKY_PROPS = [
    "sky_day.properties",
    "sky_sunset_dusk.properties",
    "sky_sunset_dawn.properties",
    "sky_night.properties",
    "sky_midnight.properties",
    "sky_storm.properties",
]


def validate_resource_pack(names: list[str]) -> None:
    """Hard fail unless the MCSM skybox set is complete and exclusive.

    world0 must hold exactly the Day / Sunset / Night / Midnight / Storm
    sheets: no Fabric skyboxes, no discarded shield sprites, no stale skyN
    layers. Phase 5 / 5.8 / 6 / 7 fog tinting rides on the thunder sky via
    the mod's live phase palettes, so no extra sheets are expected.
    """
    sky = [n for n in names if "/optifine/sky/world0/" in n]
    if [n for n in names if "fabricskyboxes" in n]:
        print("FATAL: resource pack still contains fabricskyboxes")
        sys.exit(1)
    banned = [n for n in sky if "blue_shield" in n or "/sky1." in n or "/sky2." in n or "/sky3." in n or "/sky4." in n]
    if banned:
        print(f"FATAL: stale sky file(s) in resource pack: {banned[:3]} ...")
        sys.exit(1)
    short = {n.rsplit("/", 1)[-1] for n in sky}
    missing = [f for f in REQUIRED_SKY_PNGS + REQUIRED_SKY_PROPS if f not in short]
    if missing:
        print(f"FATAL: resource pack sky set incomplete, missing: {missing}")
        sys.exit(1)


def main() -> int:
    rp_names = zip_dir(RP_DIR, RP_ZIP)
    sp_names = zip_dir(SP_DIR, SP_ZIP)
    validate_shader_pack(sp_names)
    validate_resource_pack(rp_names)

    def sha(p: str) -> str:
        with open(p, "rb") as fh:
            return hashlib.sha256(fh.read()).hexdigest()

    print(f"wrote {RP_ZIP}  ({len(rp_names)} files)  sha256 {sha(RP_ZIP)}")
    print(f"wrote {SP_ZIP}  ({len(sp_names)} files)  sha256 {sha(SP_ZIP)}")
    print("shader pack is PNG-free (procedural GLSL clouds only)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
