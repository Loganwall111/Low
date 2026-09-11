#!/usr/bin/env python3
"""
Integrated release validator — run by .github/workflows/mcsm-release.yml before
assets are pushed to the v1.9.60 GitHub Release, but usable standalone.

Checks (all hard failures):
  * Zips are FLAT: no single nested parent folder wrapping the pack contents.
  * MCSM_ResourcePack.zip contains the custom time-of-day skyboxes under
    assets/minecraft/optifine/sky/world0/ (sky_day/sunset/night/midnight/storm
    png+properties, no Fabric skyboxes), split-range
    pack.mcmeta, and leak-free lang files. It must NOT ship any shaders/core
    cloud overrides (hidden the moment an Iris shaderpack loads — the shader
    pack owns the clouds) and must NOT ship a PNG clouds.png sheet.
  * MCSM_ShaderPack.zip is the modern-engine aligned PROCEDURAL pack:
      - uniform long worldTime in every sky program (Iris uniform type check)
      - NO reserved-keyword `uniform sampler2D texture;` samplers
      - gbuffers_clouds.fsh / rendertype_clouds.fsh are fully procedural (hash13/fbm)
      - the handwritten CloudFaces-decode vertex math (isamplerBuffer CloudFaces,
        CloudHeight 2.5x extrusion) in the cloud vertex shaders
      - sun shadow map (shadow.vsh/fsh) + shadowtex0 sampling in terrain/water
      - ZERO PNG cloud sheets and ZERO cloudTex bindings (hard failure otherwise)
      - no GLSL120 files under shaders/core (invalid for the #version 150 pipeline)
  * The mod JAR is a FRESH compile of the latest branch, i.e. it carries the storm
    atmosphere backdrop class (StormAtmospherePost), the sky/cloud mixins, the post
    processing filters, the post_effect jsons, AND the bundled skybox assets.
  * fabric.mod.json inside the JAR declares the expected version.

Also prints size + sha256 per artifact so the log can be matched against the
GitHub release asset `digest` field afterwards.
"""

import argparse
import hashlib
import json
import os
import sys
import zipfile

FAILURES = []


def fail(msg: str) -> None:
    FAILURES.append(msg)
    print(f"  FAIL  {msg}")


def ok(msg: str) -> None:
    print(f"  ok    {msg}")


def names(z: zipfile.ZipFile):
    return set(z.namelist())


def read(z: zipfile.ZipFile, name: str) -> bytes:
    return z.read(name)


def check_no_nested_parent(z: zipfile.ZipFile, label: str) -> None:
    tops = {n.split("/", 1)[0] for n in names(z) if n and not n.endswith("/")}
    # A "nested parent folder" zip wraps everything in exactly one directory
    # (e.g. MCSM_ResourcePack/pack.mcmeta). Flat packs open pack.mcmeta or
    # shaders.properties at the archive root.
    if len(tops) == 1 and next(iter(tops)) not in ("assets", "shaders", "lang", "pack.mcmeta"):
        fail(f"{label}: single top-level entry '{next(iter(tops))}/' — nested parent folder detected")
    else:
        ok(f"{label}: flat root structure (top-level: {', '.join(sorted(tops))[:120]})")


def check_leak_free(z: zipfile.ZipFile, label: str, lang_names) -> None:
    bad_tokens = ["http://", "https://", "| ---", "|:--", "```", "## ", "[1](", "](http"]
    for ln in lang_names:
        if ln not in names(z):
            continue
        text = read(z, ln).decode("utf-8", "replace")
        hits = [tok for tok in bad_tokens if tok in text]
        if hits:
            fail(f"{label}: metadata leak markers {hits} found in {ln}")
        else:
            ok(f"{label}: {ln} clean")


def validate_rp(path: str) -> None:
    print(f"[RP] {path}")
    with zipfile.ZipFile(path) as z:
        n = names(z)
        check_no_nested_parent(z, "RP")
        if "pack.mcmeta" not in n:
            fail("RP: pack.mcmeta missing at zip root")
        else:
            meta = json.loads(read(z, "pack.mcmeta").decode("utf-8"))
            pf = meta.get("pack", {}).get("pack_format")
            sf = meta.get("pack", {}).get("supported_formats", {})
            if pf != 46 or sf.get("min_format") != 42 or sf.get("max_format") != 50:
                fail(f"RP: pack.mcmeta split-range schema wrong: pack_format={pf} supported={sf}")
            else:
                ok("RP: pack.mcmeta modern split range (46 / 42..50)")
        for leaf in ("sky_day.png", "sky_sunset.png", "sky_night.png", "sky_midnight.png", "sky_storm.png",
                     "sky_day.properties", "sky_sunset_dusk.properties", "sky_sunset_dawn.properties",
                     "sky_night.properties", "sky_midnight.properties", "sky_storm.properties"):
            want = f"assets/minecraft/optifine/sky/world0/{leaf}"
            if want not in n:
                fail(f"RP: missing custom skybox file {want}")
        if all(f"assets/minecraft/optifine/sky/world0/{leaf}" in n for leaf in ("sky_day.png", "sky_sunset.png", "sky_night.png", "sky_midnight.png", "sky_storm.png")):
            ok("RP: custom time-of-day skyboxes present (day/sunset/night/midnight/storm + properties)")
        if [x for x in n if "fabricskyboxes" in x]:
            fail("RP: Fabric skyboxes must not ship - the world0 set is the only one")
        for key, must in {
            "assets/minecraft/optifine/sky/world0/sky_day.properties": "startFadeIn=6:00",
            "assets/minecraft/optifine/sky/world0/sky_sunset_dusk.properties": "startFadeIn=17:00",
            "assets/minecraft/optifine/sky/world0/sky_night.properties": "startFadeIn=19:30",
            "assets/minecraft/optifine/sky/world0/sky_storm.properties": "weather=thunder",
        }.items():
            if key in n and must not in read(z, key).decode("utf-8"):
                fail(f"RP: {key} missing fade spec '{must}'")
        want_core = "assets/minecraft/shaders/core/rendertype_clouds.vsh"
        core_overrides = [x for x in n if "/shaders/core/" in x and x.endswith((".vsh", ".fsh"))]
        if core_overrides:
            fail(f"RP: {len(core_overrides)} hidden core shader override(s) shipped — Iris replaces them with the shader pack, delete them ({core_overrides[:2]})")
        elif want_core in n:
            fail(f"RP: {want_core} still present — misplaced cloud core override (shader pack owns the clouds)")
        else:
            ok("RP: no shaders/core cloud overrides (shader pack owns the clouds)")
        if "assets/minecraft/textures/environment/clouds.png" in n:
            fail("RP: loose PNG clouds.png still shipped — clouds must be procedural GLSL")
        else:
            ok("RP: no PNG clouds.png sheet (procedural GLSL clouds only)")
        for cm in ("assets/minecraft/textures/colormap/grass.png",
                   "assets/minecraft/textures/colormap/foliage.png"):
            if cm not in n:
                fail(f"RP: expected texture {cm} missing")
        check_leak_free(z, "RP", ["lang/en_us.lang", "shaders/lang/en_us.lang"])


def _check_aligned_program(label: str, z: zipfile.ZipFile, name: str, *, need_long_time: bool,
                           forbid_texture_sampler: bool, extra_markers=()) -> None:
    n = names(z)
    if name not in n:
        fail(f"{label}: {name} missing")
        return
    text = read(z, name).decode("utf-8")
    if need_long_time and "worldTime" in text:
        if "uniform long worldTime" not in text:
            fail(f"{label}: {name} declares worldTime without `long` type — Iris will reject it")
    if "uniform int worldTime" in text:
        fail(f"{label}: {name} still declares `uniform int worldTime` (modern engines need long)")
    if forbid_texture_sampler and "uniform sampler2D texture;" in text:
        fail(f"{label}: {name} declares reserved-keyword sampler `texture` — must sample gtexture only")
    for marker in extra_markers:
        if marker not in text:
            fail(f"{label}: {name} missing alignment marker `{marker}`")
    ok(f"{label}: {name} aligned with modern engine spec")


def validate_sp(path: str) -> None:
    print(f"[SP] {path}")
    with zipfile.ZipFile(path) as z:
        n = names(z)
        check_no_nested_parent(z, "SP")
        props_name = "shaders/shaders.properties"
        if props_name not in n:
            fail(f"SP: {props_name} missing")
        else:
            props = read(z, props_name).decode("utf-8")
            if "clouds=fast" not in props:
                fail("SP: shaders.properties missing clouds=fast routing")
            if "customTexture.cloudTex" in props:
                fail("SP: shaders.properties still binds PNG cloud sheets (cloudTex) — clouds must be procedural GLSL")
            else:
                ok("SP: clouds=fast routing present, no cloudTex PNG bindings (procedural)")
        cloud_pngs = [x for x in n if "cloud" in x.lower() and x.lower().endswith(".png")]
        if cloud_pngs:
            fail(f"SP: {len(cloud_pngs)} PNG cloud sheet(s) shipped — clouds must be procedural GLSL ({cloud_pngs[:2]})")
        else:
            ok("SP: zero PNG cloud sheets (100% procedural GLSL clouds)")
        # Cloud programs are core-dialect (#version 150, matching the
        # CloudFaces vertex ABI): GLSL 150 has no `long` uniforms, so the live
        # clock flows through frameTimeCounter/sunPosition here; the sky
        # programs carry the long worldTime uniform.
        _check_aligned_program("SP", z, "shaders/gbuffers_clouds.fsh",
                               need_long_time=False, forbid_texture_sampler=True,
                               extra_markers=("hash13", "fbm"))
        _check_aligned_program("SP", z, "shaders/rendertype_clouds.fsh",
                               need_long_time=False, forbid_texture_sampler=True,
                               extra_markers=("hash13", "fbm"))
        for f in ("shaders/gbuffers_clouds.vsh", "shaders/rendertype_clouds.vsh"):
            if f not in n:
                fail(f"SP: {f} missing")
                continue
            text = read(z, f).decode("utf-8")
            for marker in ("isamplerBuffer CloudFaces", "// Vertical block thickness", "vertices[(direction * 4)"):
                if marker not in text:
                    fail(f"SP: {f} missing the handwritten CloudFaces-decode marker `{marker}`")
            if (text.count("vec3(1,0,0),vec3(1,0,1)") != 1 or text.count("vec3(1,0,0),vec3(1,1,0),vec3(1,1,1),vec3(1,0,1)") != 1):
                fail(f"SP: {f} vertices[] coordinate array does not match the raw directive source")
            if (not text.startswith("#version 150") or "scaledVertex.y *= CloudHeight" not in text
                    or "CloudHeight      = 2.5" not in text):
                fail(f"SP: {f} not re-anchored to the raw #version 150 vertex math (CloudHeight 2.5x extrusion)")
            else:
                ok(f"SP: {f} raw CloudFaces vertex math + 2.5x CloudHeight extrusion present")
        # Sun shadow map on ground + water.
        for f in ("shaders/shadow.vsh", "shaders/shadow.fsh"):
            if f not in n:
                fail(f"SP: {f} missing (sun shadow map pass)")
            else:
                ok(f"SP: {f} present")
        for f in ("shaders/gbuffers_terrain.fsh", "shaders/gbuffers_water.fsh"):
            if f not in n:
                fail(f"SP: {f} missing (shadow-receiving surface)")
                continue
            text = read(z, f).decode("utf-8")
            if "shadowtex0" not in text:
                fail(f"SP: {f} does not sample shadowtex0 — ground/water shadows missing")
            else:
                ok(f"SP: {f} samples shadowtex0 (sun-cast shadows)")
        _check_aligned_program("SP", z, "shaders/gbuffers_skybasic.fsh",
                               need_long_time=True, forbid_texture_sampler=True,
                               extra_markers=("getSunsetSky",))
        _check_aligned_program("SP", z, "shaders/gbuffers_skybasic.vsh",
                               need_long_time=True, forbid_texture_sampler=True)
        _check_aligned_program("SP", z, "shaders/gbuffers_skytextured.fsh",
                               need_long_time=True, forbid_texture_sampler=True,
                               extra_markers=("warmTint", "lavenderNight"))
        _check_aligned_program("SP", z, "shaders/gbuffers_skytextured.vsh",
                               need_long_time=True, forbid_texture_sampler=True)
        for f in ("shaders/gbuffers_terrain.fsh", "shaders/gbuffers_entities.fsh",
                  "shaders/gbuffers_hand.fsh", "shaders/gbuffers_hand_water.fsh"):
            _check_aligned_program("SP", z, f, need_long_time=False, forbid_texture_sampler=True)
        core_bad = [x for x in n if x.startswith("shaders/core/") and x.endswith((".fsh", ".vsh"))]
        for x in core_bad:
            if "#version 150" not in read(z, x).decode("utf-8", "replace"):
                fail(f"SP: {x} is a non-#version-150 core override — breaks the vanilla core pipeline")
        if not core_bad:
            ok("SP: no stale GLSL120 core overrides shipped")
        else:
            ok("SP: core overrides are all #version 150")


def validate_jar(path: str, expect_version: str) -> None:
    print(f"[JAR] {path}")
    with zipfile.ZipFile(path) as z:
        n = names(z)
        for cls in (
            "net/dabicco/witherstormmod/client/StormAtmospherePost.class",
            "net/dabicco/witherstormmod/client/StormCloudDeck.class",
            "net/dabicco/witherstormmod/client/StormPresenceFX.class",
            "net/dabicco/witherstormmod/mixin/SkyRendererMixin.class",
            "net/dabicco/witherstormmod/mixin/CloudColorMixin.class",
        ):
            if cls not in n:
                fail(f"JAR: missing class {cls} — JAR was not compiled from the latest master sources")
        ok("JAR: storm atmosphere backdrop + cloud deck + sky/cloud mixin classes present")
        post_fsh = [x for x in n if x.startswith("assets/dabywitherstormmod/shaders/post/") and x.endswith(".fsh")]
        if len(post_fsh) < 11:
            fail(f"JAR: only {len(post_fsh)} post-processing filter files bundled (expected >= 11)")
        else:
            ok(f"JAR: {len(post_fsh)} post-processing filters bundled")
        pe = [x for x in n if x.endswith(".json") and "/post_effect/" in x]
        if not pe:
            fail("JAR: no post_effect json definitions bundled")
        else:
            ok(f"JAR: {len(pe)} post_effect definitions bundled")
        jar_sky_fail = 0
        for leaf in ("sky_day.png", "sky_sunset.png", "sky_night.png", "sky_midnight.png", "sky_storm.png",
                     "sky_day.properties", "sky_sunset_dusk.properties", "sky_sunset_dawn.properties",
                     "sky_night.properties", "sky_midnight.properties", "sky_storm.properties"):
            want = f"assets/minecraft/optifine/sky/world0/{leaf}"
            if want not in n:
                jar_sky_fail += 1
                fail(f"JAR: MCSM skybox asset {want} not bundled into the mod")
        if jar_sky_fail == 0:
            ok("JAR: custom skyboxes bundled under assets/minecraft/optifine/sky/world0/")
        if [x for x in n if "fabricskyboxes" in x]:
            fail("JAR: Fabric skyboxes must not ship - the world0 set is the only one")
        if "pack.mcmeta" not in n:
            fail("JAR: pack.mcmeta missing (mod assets will not merge as a resource tree)")
        fmj = json.loads(read(z, "fabric.mod.json").decode("utf-8"))
        if fmj.get("version") != expect_version:
            fail(f"JAR: fabric.mod.json version {fmj.get('version')!r} != expected {expect_version!r}")
        else:
            ok(f"JAR: fabric.mod.json version == {expect_version}")
        if "${version}" in read(z, "fabric.mod.json").decode("utf-8"):
            fail("JAR: fabric.mod.json still contains an unexpanded ${version} template token")


def digest(path: str) -> None:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    print(f"  size={os.path.getsize(path)} bytes  sha256={h.hexdigest()}  {os.path.basename(path)}")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--rp", required=True)
    ap.add_argument("--sp", required=True)
    ap.add_argument("--jar", required=True)
    ap.add_argument("--expect-version", required=True)
    a = ap.parse_args()
    for p in (a.rp, a.sp, a.jar):
        if not os.path.exists(p):
            fail(f"artifact missing: {p}")
    if FAILURES:
        print("\n".join(FAILURES))
        return 1
    validate_rp(a.rp)
    validate_sp(a.sp)
    validate_jar(a.jar, a.expect_version)
    print("\n[digests]")
    for p in (a.rp, a.sp, a.jar):
        digest(p)
    if FAILURES:
        print(f"\nVALIDATION FAILED ({len(FAILURES)} problem(s))")
        return 1
    print("\nVALIDATION PASSED — integrated build is release-ready")
    return 0


if __name__ == "__main__":
    sys.exit(main())
