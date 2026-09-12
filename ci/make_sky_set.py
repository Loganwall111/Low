#!/usr/bin/env python3
"""1.9.208: regenerate textures/sky/*.png as clean zenith->horizon strips
from the runtime palettes (day / sunset / night / midnight / phase fog decks
5, 5.5-5.9, 6, 7 and the phase 8-9 ember sky). Same 1024x1024 strip format
the base skybox system expects; top row = zenith, bottom row = horizon."""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import write_png

SIZE = 1024
PAL = {
    'day':            ((0.478,0.455,0.878),(0.769,0.682,0.965)),
    'sunset':         ((0.086,0.275,0.337),(0.700,0.400,0.260)),
    'night':          ((0.063,0.063,0.282),(0.157,0.176,0.471)),
    'midnight':       ((0.030,0.030,0.160),(0.137,0.208,0.720)),
    'sky_twilight':   ((0.060,0.050,0.190),(0.500,0.260,0.380)),
    'phase5_fog':     ((0.020,0.280,0.250),(0.630,0.820,0.680)),   # turquoise fog
    'phase5_teal':    ((0.020,0.280,0.250),(0.630,0.820,0.680)),
    'phase54_purple': ((0.260,0.100,0.360),(0.630,0.300,0.700)),
    'phase55_fog':    ((0.260,0.100,0.360),(0.630,0.300,0.700)),   # 5.5-5.9 purple-pink fog
    'phase55_pink':   ((0.260,0.100,0.360),(0.630,0.300,0.700)),
    'phase6_fog':     ((0.320,0.160,0.260),(0.700,0.400,0.420)),   # brown-pink fog
    'phase6_brownpink':((0.320,0.160,0.260),(0.700,0.400,0.420)),
    'phase7_fog':     ((0.200,0.180,0.240),(0.550,0.480,0.520)),
    'phase89_ember':  ((0.118,0.016,0.008),(0.729,0.529,0.337)),
}

def ramp(a, b, t):
    return (a[0]+(b[0]-a[0])*t, a[1]+(b[1]-a[1])*t, a[2]+(b[2]-a[2])*t)

def main():
    base = 'jar-overrides/assets/dabywitherstormmod/textures/sky'
    os.makedirs(base, exist_ok=True)
    for name, (top, bot) in PAL.items():
        px = []
        for y in range(SIZE):
            t = y / (SIZE - 1)
            c = ramp(top, bot, t)
            row = [(int(c[0]*255), int(c[1]*255), int(c[2]*255), 255)] * SIZE
            px.extend(row)
        write_png(os.path.join(base, name + '.png'), SIZE, SIZE, px)
        print(name)

if __name__ == '__main__':
    main()
