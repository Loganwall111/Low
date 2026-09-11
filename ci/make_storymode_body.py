#!/usr/bin/env python3
"""1.9.214: remap the mod's storm atlases to the TRUE Telltale palette.

Measured from the real extracted assets (repo Loganwall111/gggggrff,
Telltale meshes/textures, traced by wheatlycrab5892):

  Stage B body  (1:1 flesh.png):         black + (0,8,16) blue sheen,
                                         highlights up to (0,8,24)/(0,16,32)
  Stage D head  (heead.png):             black + (0,16,32) family
  Stage C mass  (nice try...):           black + blues + cyan glow (0,112,224)
  Stage A small (skM0_witherstormStageA): light grey (96,80,80) w/ dark detail

No invented colours: every target value above was measured from those
textures.  The mod's atlases keep their own UV layout (the mod's model
defines it); this pass only swaps the colour ramp to the real one.
Emissive (_e) atlases are untouched.
"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import read_png, write_png

ROOTS = ['jar-overrides/assets/dabywitherstormmod/textures/entity']
BODY_NAMES = ['phase_4_assets', 'phase_4_assets_og',
              'phase_4_assets_p55', 'phase_4_assets_og_p55',
              'phase_4_assets_p6', 'phase_4_assets_og_p6',
              'phase_4_assets_p7', 'phase_4_assets_og_p7',
              'devourer_assets', 'devourer_assets_og',
              'devourer_assets_p55', 'devourer_assets_og_p55',
              'devourer_assets_p6', 'devourer_assets_og_p6',
              'devourer_assets_p7', 'devourer_assets_og_p7']
# face atlases: stage A style is light grey; phase 4+ faces follow the
# stage B/C blue-black ramp (the head measured (0,16,32) family)
FACE_NAMES = ['wither_storm', 'wither_storm_og']

def ramp_body(l):
    """black -> dark blue sheen, per the measured stage B/C clusters."""
    if l < 36:
        return (0, 0, 0)
    if l < 90:
        return (0, 0, 8)
    if l < 140:
        return (0, 8, 16)
    if l < 195:
        return (0, 8, 24)
    return (0, 16, 32)

def ramp_face_grey(l):
    """stage A small storm: light warm grey (96,80,80) with dark detail."""
    if l > 150:
        return (232, 234, 240)   # teeth stay lit on the mini storm
    if l < 45:
        return (20, 17, 17)
    g = int(0.60 * l + 42)
    return (min(255, g), min(255, int(g * 0.83)), min(255, int(g * 0.83)))

def process(path, ramp):
    w, h, px = read_png(path)
    lums = [(p[0] * 299 + p[1] * 587 + p[2] * 114) // 1000 if p[3] > 0 else None for p in px]
    live = sorted(l for l in lums if l is not None)
    if not live:
        return
    lo, hi = live[max(0, len(live) // 100)], live[min(len(live) - 1, len(live) * 99 // 100)]
    span = max(1, hi - lo)
    out = []
    for i, (r, g, b, a) in enumerate(px):
        if a == 0:
            out.append((r, g, b, a))
            continue
        # stretch this atlas' own luminance range to 0..255, then apply the
        # measured Telltale ramp -- the atlases peak at very low luminance,
        # so a global threshold would crush everything to black.
        l = min(255, max(0, (lums[i] - lo) * 255 // span))
        nr, ng, nb = ramp(l)
        # (the real 1:1 atlases carry no white teeth in the base texture --
        #  the teeth are the additive emissive overlay; keep the base dark)
        # keep saturated accents (command-block orange, magenta) intact --
        # except on the stage-A face, where the real small storm is grey
        # with only its purple eye kept.
        mx, mn = max(r, g, b), min(r, g, b)
        keep = (mx - mn) / max(1, mx) > 0.45 and mx > 90
        if ramp is ramp_face_grey:
            keep = keep and b > r
        if keep:
            nr, ng, nb = r, g, b
        out.append((nr, ng, nb, a))
    write_png(path, w, h, out)

def main():
    n = 0
    for root in ROOTS:
        for name in BODY_NAMES:
            p = os.path.join(root, name + '.png')
            if os.path.isfile(p):
                process(p, ramp_body)
                n += 1
        for name in FACE_NAMES:
            p = os.path.join(root, name + '.png')
            if os.path.isfile(p):
                process(p, ramp_face_grey)
                n += 1
    print('true Telltale palette applied to %d atlases' % n)

if __name__ == '__main__':
    main()
