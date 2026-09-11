#!/usr/bin/env python3
"""1.9.208: Story Mode body pass v2 over the OG storm atlases.

User spec (from the Telltale frames): the body is NOT obsidian -- it is a
MATTE DARK CHARCOAL-INDIGO block texture whose edges catch CRISP, sharp
blue-white highlights (shiny without gloss).  Jagged obsidian detail is
restricted to the very bottom of the atlas (the tentacle tips).

So this pass:
  * maps the mid-body to charcoal-indigo (target avg ~ (30,31,52)),
  * keeps saturated accents (command-block orange, magenta),
  * adds hard, thresholded edge highlights -- only pixels that stand clearly
    above their neighbourhood get the blue-white kick (crisp, not fuzzy),
  * crushes the bottom 22% of the atlas darker/obsidian and keeps its local
    contrast jagged.
Emissive (_e) atlases are NOT touched.
"""
import os, sys, glob
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import read_png, write_png

ROOTS = ['jar-overrides/assets/dabywitherstormmod/textures/entity']
NAMES = ['phase_4_assets_og', 'phase_4_assets_og_p55', 'phase_4_assets_og_p6', 'phase_4_assets_og_p7',
         'phase_4_assets', 'phase_4_assets_p55', 'phase_4_assets_p6', 'phase_4_assets_p7',
         'devourer_assets_og', 'devourer_assets_og_p55', 'devourer_assets_og_p6', 'devourer_assets_og_p7',
         'devourer_assets', 'devourer_assets_p55', 'devourer_assets_p6', 'devourer_assets_p7',
         'wither_storm_og', 'wither_storm']
BODY_ONLY = {'wither_storm_og', 'wither_storm'}  # face atlases: no tentacle-tip zone

def lum(p):
    return (p[0] * 299 + p[1] * 587 + p[2] * 114) // 1000

def process(path, name):
    w, h, px = read_png(path)
    lums = [lum(p) for p in px]
    out = []
    for i, (r, g, b, a) in enumerate(px):
        if a == 0:
            out.append((r, g, b, a))
            continue
        y = i // w
        frac_y = y / max(1, h - 1)
        mx, mn = max(r, g, b), min(r, g, b)
        sat = (mx - mn) / max(1, mx)
        if sat > 0.45 and mx > 90:
            out.append((r, g, b, a))          # keep saturated accents
            continue
        x = i % w
        n = []
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = x + dx, y + dy
            if 0 <= xx < w and 0 <= yy < h and px[yy * w + xx][3] > 0:
                n.append(lums[yy * w + xx])
        edge = max(0, lums[i] - (sum(n) / len(n))) if n else 0
        if frac_y > 0.78 and name not in BODY_ONLY:
            # tentacle tips: jagged obsidian, local contrast preserved
            nr = int(r * 0.22)
            ng = int(g * 0.22)
            nb = int(b * 0.24 + 4)
            if edge > 26:
                boost = min(110, edge * 2)
                nr, ng, nb = min(255, nr + boost), min(255, ng + boost), min(255, nb + boost)
            out.append((min(255, nr), min(255, ng), nb, a))
            continue
        # charcoal-indigo body: desaturate toward indigo, keep luminance shape
        nr = int(r * 0.30 + 8)
        ng = int(g * 0.38 + 10)
        nb = int(b * 0.78 + 22)
        if edge > 18:
            # crisp edge highlight: hard threshold, blue-white, no soft bloom
            boost = min(120, (edge - 18) * 2)
            nr = min(255, nr + boost)
            ng = min(255, ng + boost)
            nb = min(255, nb + boost)
        out.append((min(255, nr), min(255, ng), min(255, nb), a))
    write_png(path, w, h, out)

def main():
    n = 0
    for root in ROOTS:
        for name in NAMES:
            p = os.path.join(root, name + '.png')
            if os.path.isfile(p):
                process(p, name)
                n += 1
    print('charcoal-indigo body pass v2 applied to %d atlases' % n)

if __name__ == '__main__':
    main()
