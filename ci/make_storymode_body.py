#!/usr/bin/env python3
"""1.9.206: Story Mode body pass over the OG storm atlases.

Telltale's storm is a near-black silhouette whose edges carry a faint
dark-blue underglow.  The shipped OG atlases average (24,25,35) with a purple
cast that reads grey-violet against a sunset.  This pass:
  * crushes luminance (x0.42) so the body is black in daylight,
  * tints the surviving light toward blue (b keeps 0.85, r 0.35, g 0.45),
  * adds a blue edge underglow: pixels that are lighter than their neighbours
    (block edges/highlights) get +blue.
Only dark/desaturated pixels are touched; saturated pixels (command block
orange, magenta accents) are preserved.  Emissive (_e) atlases are NOT touched.
"""
import os, sys, glob
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import read_png, write_png

ROOTS = ['jar-overrides/assets/dabywitherstormmod/textures/entity']
NAMES = ['phase_4_assets_og', 'phase_4_assets_og_p55', 'phase_4_assets_og_p6', 'phase_4_assets_og_p7',
         'devourer_assets_og', 'devourer_assets_og_p55', 'devourer_assets_og_p6', 'devourer_assets_og_p7',
         'wither_storm_og']

def process(path):
    w, h, px = read_png(path)
    lum = [ (p[0]*299 + p[1]*587 + p[2]*114)//1000 for p in px ]
    out = []
    for i, (r, g, b, a) in enumerate(px):
        if a == 0:
            out.append((r, g, b, a)); continue
        mx, mn = max(r, g, b), min(r, g, b)
        sat = (mx - mn) / max(1, mx)
        if sat > 0.45 and mx > 90:
            out.append((r, g, b, a)); continue          # keep saturated accents
        x, y = i % w, i // w
        n = []
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            xx, yy = x+dx, y+dy
            if 0 <= xx < w and 0 <= yy < h and px[yy*w+xx][3] > 0:
                n.append(lum[yy*w+xx])
        edge = 0
        if n:
            edge = max(0, lum[i] - sum(n)/len(n))       # brighter than neighbours = edge
        nr = int(r * 0.35)
        ng = int(g * 0.45)
        nb = int(b * 0.85 + 6)
        glow = min(60, int(edge * 2.2))
        nb = min(255, nb + glow)
        ng = min(255, ng + glow // 3)
        out.append((min(255,nr), min(255,ng), nb, a))
    write_png(path, w, h, out)

def main():
    n = 0
    for root in ROOTS:
        for name in NAMES:
            p = os.path.join(root, name + '.png')
            if os.path.isfile(p):
                process(p); n += 1
    print('storymode body pass applied to %d atlases' % n)

if __name__ == '__main__':
    main()
