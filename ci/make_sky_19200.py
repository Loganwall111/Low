#!/usr/bin/env python3
"""
MCSM 1.9.200 -- regenerate the main-world + phase skybox/sky gradient assets
from the user's uploaded references (2026-09-11):

  day.png          <- "skyday day (1).png"           (main world day skybox)
  night.png        <- "sky 2 midnight (1).png"       (main world midnight skybox)
  sunset.png       <- "storymode_sky_sunset.png"     (main world sunset skybox)
  phase5_teal.png  <- "phase 5 turquoise sky.png"    (phase 5)
  phase55 purple   <- "phase5sky0purple sky.png"     (phase 5.5 - 5.9)
  phase6           <- "phase6sky 6 witherstorm.png"  (phase 6 endgame split)

These replace the previous Fabric-shipped skybox set in BOTH asset locations
(textures/sky and textures/mcsm_atmosphere/sky), in src and jar-overrides.

Anchor colours are sampled by eye from the uploads; a tiny dither keeps the
1024x1024 fills band-free like the originals.
"""
import os
import random
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

W = H = 1024


def clamp255(v):
    if v < 0:
        return 0
    if v > 255:
        return 255
    return int(round(v))


def lerp(a, b, t):
    return a + (b - a) * t


def row_color(stops, t):
    """stops: list of (t, (r,g,b)) sorted by t. Returns (r,g,b) floats."""
    if t <= stops[0][0]:
        return stops[0][1]
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i + 1]
        if t <= t1:
            f = (t - t0) / (t1 - t0) if t1 > t0 else 0.0
            # smooth the segment joins slightly so anchors stay organic
            f = f * f * (3.0 - 2.0 * f)
            return (lerp(c0[0], c1[0], f),
                    lerp(c0[1], c1[1], f),
                    lerp(c0[2], c1[2], f))
    return stops[-1][1]


def gradient(stops, seed):
    rnd = random.Random(seed)
    px = bytearray()
    for y in range(H):
        t = y / (H - 1)
        r, g, b = row_color(stops, t)
        for x in range(W):
            # dither +/- ~1.3/255: kills 8-bit banding on the smooth fills
            d = (rnd.random() - 0.5) * 2.6
            px.append(clamp255(r + d))
            px.append(clamp255(g + d))
            px.append(clamp255(b + d))
    return bytes(px)


def write_png(path, px):
    raw = bytearray()
    stride = W * 3
    for y in range(H):
        raw.append(0)  # filter: none
        raw += px[y * stride:(y + 1) * stride]

    def chunk(tag, data):
        out = struct.pack('>I', len(data)) + tag + data
        return out + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack('>IIBBBBB', W, H, 8, 2, 0, 0, 0)
    with open(path, 'wb') as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', ihdr))
        f.write(chunk(b'IDAT', zlib.compress(bytes(raw), 9)))
        f.write(chunk(b'IEND', b''))
    print('wrote %s (%d bytes)' % (os.path.relpath(path, ROOT), os.path.getsize(path)))


# ---------------------------------------------------------------------------
# Anchor stops read from the uploaded references (top -> bottom).
# ---------------------------------------------------------------------------
DAY = [  # "skyday day (1).png": soft periwinkle, paler + pinker at the base
    (0.00, (122, 122, 216)),
    (0.40, (145, 144, 228)),
    (0.75, (172, 170, 240)),
    (1.00, (200, 186, 242)),
]
NIGHT = [  # "sky 2 midnight (1).png": deep midnight vault
    (0.00, (10, 12, 44)),
    (0.35, (16, 22, 76)),
    (0.65, (24, 36, 116)),
    (0.88, (32, 50, 156)),
    (1.00, (38, 58, 178)),
]
SUNSET = [  # "storymode_sky_sunset.png": teal top -> slate -> orange -> red base
    (0.00, (24, 52, 64)),
    (0.18, (44, 76, 88)),
    (0.34, (88, 92, 90)),
    (0.44, (190, 74, 34)),
    (0.56, (178, 52, 28)),
    (0.72, (140, 34, 40)),
    (0.88, (112, 26, 40)),
    (1.00, (96, 22, 38)),
]
P5 = [  # "phase 5 turquoise sky.png": dark teal slate -> grey-green
    (0.00, (18, 29, 39)),
    (0.28, (28, 49, 55)),
    (0.52, (47, 75, 72)),
    (0.76, (88, 119, 108)),
    (1.00, (138, 164, 146)),
]
P55 = [  # "phase5sky0purple sky.png": near-black plum -> mid purple (5.5-5.9)
    (0.00, (24, 12, 38)),
    (0.30, (40, 20, 62)),
    (0.55, (66, 36, 96)),
    (0.78, (94, 56, 130)),
    (1.00, (126, 80, 160)),
]
P6 = [  # "phase6sky 6 witherstorm.png": grey-lavender -> dusty salmon split
    (0.00, (86, 74, 88)),
    (0.30, (118, 97, 113)),
    (0.55, (150, 122, 133)),
    (0.78, (176, 143, 148)),
    (1.00, (199, 160, 155)),
]


def main():
    build = {
        'day.png':            gradient(DAY, 11),
        'night.png':          gradient(NIGHT, 12),
        'sunset.png':         gradient(SUNSET, 13),
        'phase5_teal.png':    gradient(P5, 14),
        'phase55.png':        gradient(P55, 15),
        'phase6.png':         gradient(P6, 16),
    }

    # (directory, filename -> built-key)
    sky_dir = 'textures/sky'
    atm_dir = 'textures/mcsm_atmosphere/sky'
    bases = [
        os.path.join(ROOT, 'src/main/resources/assets/dabywitherstormmod'),
        os.path.join(ROOT, 'jar-overrides/assets/dabywitherstormmod'),
    ]
    for base in bases:
        sky = os.path.join(base, sky_dir)
        atm = os.path.join(base, atm_dir)
        targets = [
            (sky, 'day.png', 'day.png'),
            (sky, 'night.png', 'night.png'),
            (sky, 'sunset.png', 'sunset.png'),
            (sky, 'phase5_teal.png', 'phase5_teal.png'),
            (sky, 'phase55_pink.png', 'phase55.png'),
            (sky, 'sky_twilight.png', 'phase55.png'),  # shipped as a 55 copy
            (sky, 'phase6_brownpink.png', 'phase6.png'),
            (atm, 'day.png', 'day.png'),
            (atm, 'night.png', 'night.png'),
            (atm, 'sunset.png', 'sunset.png'),
            (atm, 'phase5_teal.png', 'phase5_teal.png'),
            (atm, 'phase55.png', 'phase55.png'),
            (atm, 'phase6.png', 'phase6.png'),
        ]
        for d, name, key in targets:
            write_png(os.path.join(d, name), build[key])


if __name__ == '__main__':
    main()
