#!/usr/bin/env python3
"""1.9.210: rebuild mcsm_atmosphere/glare/*.png as RIGID STRUCTURED glares.

The old textures were soft radial discs (fuzzy mist spheres -- exactly what
the user keeps rejecting).  These bake the new structured design into the
asset itself so ANY code path (legacy disc sampler, shader, preview screens)
gets the rigid Telltale read:

  * hard-edged vertical slab: three crisp bands (horizon / mid / zenith)
    from the phase palette, sharp top and bottom cutoffs;
  * 15 crisp rays alternating long/short radiating from the core;
  * small saturated core, everything else transparent (additive-ready).

Rendered at 512x512 with a transparent background.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import write_png

SIZE = 512

# palette stops matching McsmGlarePalettes: (zenith, mid, horizon)
PAL = {
    'phase4':   ((0.50,0.53,0.92), (0.55,0.58,0.95), (0.42,0.78,0.92)),
    'phase5':   ((0.02,0.28,0.25), (0.06,0.42,0.38), (0.63,0.82,0.68)),
    'phase54':  ((0.26,0.10,0.36), (0.42,0.16,0.52), (0.70,0.45,0.80)),
    'phase55':  ((0.22,0.09,0.30), (0.52,0.20,0.48), (0.80,0.53,0.55)),
    'phase6':   ((0.35,0.20,0.31), (0.50,0.35,0.46), (0.73,0.54,0.60)),
    'phase89':  ((0.118,0.016,0.008), (0.443,0.153,0.059), (0.729,0.529,0.337)),
}

def make(name, zen, mid, hor):
    px = [(0, 0, 0, 0)] * (SIZE * SIZE)
    cx, cy = SIZE // 2, SIZE // 2
    # crisp three-band slab: sharp edges, no falloff
    band_h = SIZE // 3
    for y in range(SIZE):
        band = y // band_h
        c = (hor, mid, zen)[band] if band < 3 else zen
        for x in range(SIZE):
            px[y * SIZE + x] = (int(c[0]*255), int(c[1]*255), int(c[2]*255), 235)
    # rays: alternating long/short spokes from the centre
    core = (min(1.0, hor[0]*1.6+0.10), min(1.0, hor[1]*1.6+0.10), min(1.0, hor[2]*1.4+0.15))
    for i in range(15):
        th = 2.0 * math.pi * i / 15
        long_ray = (i % 2) == 0
        r_out = SIZE * (0.62 if long_ray else 0.46)
        r_in = SIZE * (0.16 if long_ray else 0.11)
        thick = 3.0 if long_ray else 2.0
        for r in range(int(r_in), int(r_out)):
            x = int(cx + math.cos(th) * r)
            y = int(cy + math.sin(th) * r * 0.92)
            for dx in range(-int(thick), int(thick) + 1):
                for dy in range(-int(thick), int(thick) + 1):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < SIZE and 0 <= yy < SIZE:
                        a = 200 if (abs(dx) + abs(dy)) <= 1 else 110
                        px[yy * SIZE + xx] = (int(core[0]*255), int(core[1]*255), int(core[2]*255), a)
    # saturated core disc
    for y in range(cy - 14, cy + 14):
        for x in range(cx - 14, cx + 14):
            if (x - cx) ** 2 + (y - cy) ** 2 <= 14 * 14:
                px[y * SIZE + x] = (int(core[0]*255), int(core[1]*255), int(core[2]*255), 255)
    return px

def main():
    base = 'jar-overrides/assets/dabywitherstormmod/textures/mcsm_atmosphere/glare'
    os.makedirs(base, exist_ok=True)
    for name, (zen, mid, hor) in PAL.items():
        write_png(os.path.join(base, name + '.png'), SIZE, SIZE, make(name, zen, mid, hor))
        print(name)

if __name__ == '__main__':
    main()
