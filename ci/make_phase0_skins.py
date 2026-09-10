#!/usr/bin/env python3
"""make_phase0_skins.py — phase-0 command block + teeth/eye recolour pass.

1.9.201 feedback: the mini Wither Storm in phase 0 lost its command-block
belly because the *classic* root atlas (textures/entity/wither_storm.png) was
a 160x160, 99.98%-black placeholder while the phase-0 model layer is the
64x96 atlas whose rows 64-95 hold the original orange command block.  This
script copies the OG 64x96 atlas (heads + command-block belly intact) over the
classic one in BOTH src/main/resources and jar-overrides, so the original
command block is attached whichever skin mode is active.

It also recolours the emissive teeth/eye masks per the 1.9.201 correction:
  * phase 5  (p5/p51)   -> completely white, no blue cast;
  * phase 5.5 (p55)     -> bluish glow;
  * phase 6  (p6)       -> deeper blue/cyan;
keeping every alpha/mask pixel so the teeth stay where the model UVs put them.

Usage: python3 ci/make_phase0_skins.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from pngutil import read_png, write_png  # noqa: E402

SRC = "src/main/resources/assets/dabywitherstormmod/textures/entity"
OVR = "jar-overrides/assets/dabywitherstormmod/textures/entity"


def dist2(a, b):
    return (a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2


def recolor_two_tone(path, tone_a, tone_b):
    """Replace the two opaque tones with new ones, preserving alpha/mask."""
    w, h, px = read_png(path)
    opaque = [c for c in px if c[3] > 8]
    if not opaque:
        return False
    from collections import Counter
    common = Counter(opaque).most_common(2)
    old = [c[0] for c in common]
    if len(old) < 2:
        old.append(old[0])
    out = []
    for r, g, b, a in px:
        if a <= 8:
            out.append((r, g, b, a))
            continue
        new = tone_a if dist2((r, g, b), old[0]) <= dist2((r, g, b), old[1]) else tone_b
        out.append((new[0], new[1], new[2], a))
    write_png(path, w, h, out)
    return True


def main():
    # ---- 1. classic phase-0 atlas <- OG 64x96 (command block belly) --------
    for base in (SRC, OVR):
        og = os.path.join(base, "wither_storm_og.png")
        classic = os.path.join(base, "wither_storm.png")
        w, h, px = read_png(og)
        if (w, h) != (64, 96):
            print("!! %s is %dx%d, expected 64x96 — skipping classic fix" % (og, w, h))
            continue
        write_png(classic, w, h, px)
        print("[phase0] classic atlas rebuilt from OG 64x96 (command block attached): %s" % classic)

    # ---- 2. teeth/eye emissive recolour ------------------------------------
    white = (255, 255, 255)
    white2 = (240, 248, 255)
    blue55 = (96, 168, 255)
    blue55b = (64, 130, 250)
    blue6 = (48, 170, 255)
    blue6b = (56, 140, 240)
    jobs = [
        ("wither_storm_p5_e.png", white, white2),
        ("wither_storm_og_p5_e.png", white, white2),
        ("wither_storm_p51_e.png", white, white2),
        ("wither_storm_og_p51_e.png", white, white2),
        ("wither_storm_p55_e.png", blue55, blue55b),
        ("wither_storm_og_p55_e.png", blue55, blue55b),
        ("wither_storm_p6_e.png", blue6, blue6b),
        ("wither_storm_og_p6_e.png", blue6, blue6b),
    ]
    for base in (SRC, OVR):
        for name, ta, tb in jobs:
            p = os.path.join(base, name)
            if os.path.isfile(p) and recolor_two_tone(p, ta, tb):
                print("[teeth] recoloured %s" % p)
    return 0


if __name__ == "__main__":
    sys.exit(main())
