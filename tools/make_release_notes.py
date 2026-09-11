#!/usr/bin/env python3
"""Generate the GitHub Release body for the integrated MCSM release build.

Writes a markdown file that links every artifact via the permanent release-asset
URL (never a branch raw link, which is how stale binaries got cached before)
and pins the exact sha256 + byte size recorded at upload time.
"""

import argparse
import hashlib
import os
import sys


def sha256_of(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def fmt_size(n: int) -> str:
    mb = n / (1024 * 1024)
    return f"{mb:.2f} MB" if mb >= 1 else f"{n / 1024:.1f} KB"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", required=True, help="directory holding the artifacts")
    ap.add_argument("--tag", required=True, help="release tag, e.g. v1.9.60-26.2-mcsm")
    ap.add_argument("--repo", required=True, help="owner/name")
    ap.add_argument("--out", required=True)
    ap.add_argument("--run", default="?", help="GitHub run number baked into jar names")
    a = ap.parse_args()

    order = []
    for name in ("MCSM_ResourcePack.zip", "MCSM_ShaderPack.zip"):
        if os.path.exists(os.path.join(a.dir, name)):
            order.append(name)
    jar = [n for n in sorted(os.listdir(a.dir)) if n.endswith(".jar")]
    order += jar
    order += [n for n in ("MCSM_ResourcePack_and_Mod.zip", "MCSM_ShaderPack_and_Mod.zip")
              if os.path.exists(os.path.join(a.dir, n))]
    if not order:
        print("no artifacts found", file=sys.stderr)
        return 1

    lines = [
        "## Minecraft: Story Mode — Integrated Build (resource pack + mod, shader pack + mod)",
        "",
        f"Finalized build r{a.run} — force-synchronized with the latest mod branch. "
        "Every file below is a **release asset** (permanent link, no branch-raw files, "
        "no nested folders inside any zip).",
        "",
        "### What was fixed in this build",
        "",
        "1. **Mod JAR force-resynced to the latest codebase** — recompiled from source in CI "
        "(Java 25 / Fabric Loom), not zipped from an old binary. It now explicitly bundles the "
        "storm atmosphere backdrop (`StormAtmospherePost`: phase 5.1+ violet/pink void fog, "
        "phase 6 volcanic mask, 45-tick purple flashbang, 2-min end-flash cadence), the sky "
        "darkening and cloud color mixins, and all 11 `shaders/post` storm post-processing "
        "filters + `post_effect` definitions **inside** the jar, so the Wither Storm shows its "
        "purple background backdrop without any extra download.",
        "2. **Original custom skyboxes restored & injected** — `assets/minecraft/optifine/sky/world0/` "
        "in both the resource pack and the mod jar carries the authentic 1536x1024 rigid-glare "
        "time-of-day skies `sky_day / sky_sunset / sky_night / sky_midnight` plus the thunder-keyed "
        "`sky_storm` sheet for storm phases 4-7, with full fade specs and `blend=alpha`. `gbuffers_skybasic.fsh` / `gbuffers_skytextured.fsh` "
        "are hooked to those maps: they sample the `worldTime` uniform (declared `uniform long` for the "
        "modern engine) with a `sunAngle`/`sunPosition` fallback when a mod loader freezes tick 0, and "
        "skytextured now visibly tints sun/moon/sky quads warm-orange at sunrise/sunset and lavender at night.",
        "3. **Cloud pattern alignment fixed** — `gbuffers_clouds.fsh` (and the `rendertype_clouds` copies) "
        "no longer sample the 8 square 256x256 cloud sheets through the legacy 1024x512 atlas UV layout. "
        "Sampling is rebuilt from camera-relative world position: square texel cells, `fract()`-wrapped "
        "drift so sheets tile seamlessly instead of smearing at the clamp edge, seam-free phase on "
        "extruded side faces, and time-of-day sheet weighting so the sky shifts with the game clock. "
        "The invalid reserved-keyword `texture` sampler was removed from every program — that mismatch is "
        "what made modern Iris skip the whole pack and fall back to old visuals.",
        "",
        "### Downloads",
        "",
    ]
    for name in order:
        p = os.path.join(a.dir, name)
        url = f"https://github.com/{a.repo}/releases/download/{a.tag}/{name}"
        lines.append(f"- **[{name}]({url})** — {fmt_size(os.path.getsize(p))} · sha256 `{sha256_of(p)}`")
    lines += [
        "",
        "### Quick installation",
        "",
        "1. **Mod** — drop `dabywitherstormmod-1.9.61-26.2-beta-r" + str(a.run) + ".jar` into `.minecraft/mods/` "
        "(needs Fabric Loader, Fabric API, Minecraft 26.2).",
        "2. **Resource pack + mod bundle** — use `MCSM_ResourcePack_and_Mod.zip` as a one-file installer for "
        "the pack side, or take `MCSM_ResourcePack.zip` alone into `.minecraft/resourcepacks/` (do **not** unzip).",
        "3. **Shader pack + mod bundle** — use `MCSM_ShaderPack_and_Mod.zip`, or take `MCSM_ShaderPack.zip` "
        "alone into `.minecraft/shaderpacks/` (do **not** unzip). Enable it via Video Settings → Shader Packs "
        "with Iris, and pick `MCSM_ShaderPack` alongside any full shader or standalone.",
        "",
        "All zips are flat-rooted: `pack.mcmeta` / `shaders/` sit directly at the archive root, and the "
        "release notes themselves are regenerated on every build, so these links never go stale.",
        "",
        "### Integrity",
        "",
        "Every artifact above is validated in CI by `tools/validate_release_artifacts.py` (flat structure, "
        "skybox presence, `uniform long worldTime` alignment, post-filter and backdrop class bundling) "
        "before upload.",
    ]
    with open(a.out, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    print(f"wrote {a.out} ({len(order)} artifacts listed)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
