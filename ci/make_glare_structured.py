#!/usr/bin/env python3
"""1.9.212: rebuild the glare assets as the ORIGINAL OVAL halo, revamped.

The user's spec: an OVAL (not a circle), not foggy, bands that BLEND:
outer blackness -> dark purple -> purple middle -> blackness core, with a
little alpha overall.  This bakes that design into:

  * mcsm_atmosphere/glare/phase{4,5,54,55,6,89}.png -- the coloured oval
    billboards the mod's own glare pass draws (white-tinted, so the bands
    are visible), per-phase palettes;
  * textures/misc/halo_ring.png -- the SAME oval in ring form for the base
    mod's Catalyst Halo pass (that pass multiplies by a near-black tint, so
    here the shape/alpha does the work).

Wide horizontal oval (w:h = 1 : 0.72), smooth band blending, max alpha
~205 (a little transparency, never glassy).
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import write_png

SIZE = 256
CY = 0.72  # vertical squash -> horizontal oval

# per-phase band palettes: (rim, dark, mid, core)
PAL = {
    'phase4':  ((8, 6, 24),  (30, 26, 90),  (95, 105, 220), (6, 6, 20)),
    'phase5':  ((4, 16, 12), (10, 52, 46),  (48, 168, 150), (3, 12, 10)),
    'phase54': ((16, 6, 26), (58, 22, 86),  (128, 48, 172), (12, 5, 20)),
    'phase55': ((22, 8, 26), (80, 28, 84),  (190, 78, 130), (16, 6, 20)),
    'phase6':  ((26, 12, 18), (86, 44, 62), (178, 96, 120), (20, 9, 14)),
    'phase89': ((34, 8, 2),  (110, 34, 10), (216, 96, 34),  (28, 8, 3)),
}

def band(q, rim, dark, mid, core):
    """q = 0..1 normalized ellipse radius -> (r,g,b,a)."""
    if q > 1.0:
        return (0, 0, 0, 0)
    # outer rim: core->dark, dark->mid, mid->black core; all smoothstepped
    def s(a, b, t):
        t = max(0.0, min(1.0, (t - a) / (b - a)))
        return t * t * (3.0 - 2.0 * t)
    if q < 0.30:
        c = mixc(mid, core, s(0.30, 0.16, q))
        a = 150.0 + 55.0 * s(0.30, 0.16, q)
    elif q < 0.58:
        c = mixc(dark, mid, s(0.30, 0.58, q))
        a = 205.0
    elif q < 0.88:
        c = mixc(rim, dark, s(0.58, 0.88, q))
        a = 205.0 - 30.0 * s(0.58, 0.88, q)
    else:
        c = rim
        a = 175.0 * (1.0 - s(0.88, 1.0, q))
    return (c[0], c[1], c[2], int(min(255, a)))

def mixc(a, b, t):
    return (int(a[0] + (b[0] - a[0]) * t), int(a[1] + (b[1] - a[1]) * t), int(a[2] + (b[2] - a[2]) * t))

def make_oval(pal, ring=False):
    rim, dark, mid, core = pal
    px = [(0, 0, 0, 0)] * (SIZE * SIZE)
    for y in range(SIZE):
        ny = (y - SIZE / 2) / (SIZE / 2)
        for x in range(SIZE):
            nx = (x - SIZE / 2) / (SIZE / 2)
            q = math.sqrt(nx * nx + (ny / CY) * (ny / CY))
            if ring:
                # hollow centre so the storm silhouette owns the middle
                if q < 0.16:
                    continue
                r, g, b, a = band(q, rim, dark, mid, core)
                a = int(a * (0.35 + 0.65 * max(0.0, min(1.0, (q - 0.16) / 0.4))))
            else:
                r, g, b, a = band(q, rim, dark, mid, core)
            px[y * SIZE + x] = (r, g, b, a)
    return px

def main():
    base = 'jar-overrides/assets/dabywitherstormmod/textures/mcsm_atmosphere/glare'
    os.makedirs(base, exist_ok=True)
    for name, pal in PAL.items():
        write_png(os.path.join(base, name + '.png'), SIZE, SIZE, make_oval(pal))
        print(name)
    misc = 'jar-overrides/assets/dabywitherstormmod/textures/misc'
    os.makedirs(misc, exist_ok=True)
    # halo ring override: the base pass tints it near-black, so only the
    # oval shape + alpha matter; use the phase-5.5 purple as a base.
    write_png(os.path.join(misc, 'halo_ring.png'), SIZE, SIZE, make_oval(PAL['phase55'], ring=True))
    print('halo_ring')

if __name__ == '__main__':
    main()
