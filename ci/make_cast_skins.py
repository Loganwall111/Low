#!/usr/bin/env python3
"""Generate 64x64 player-layout skins for the Story Mode cast.

Each skin is a flat-shaded blocky outfit in the character's canonical
Story Mode colours (hair, skin, top, trousers, shoes, accent) written into
the standard player UV layout, so the humanoid StoryCharacter entity wears
them with the vanilla PlayerModel layout (head/hat/body/arms/legs + overlay).

Usage: python3 ci/make_cast_skins.py  (writes into
       src/main/resources/assets/dabywitherstormmod/textures/entity/story/)
"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from pngutil import write_png

W = H = 64
OUT = os.path.join(os.path.dirname(__file__), '..',
                   'src/main/resources/assets/dabywitherstormmod/textures/entity/story')

# name: (skin, hair, eyes, top, top_accent, pants, shoes, extra)
CAST = {
    'jesse':     ((232, 180, 140), (70, 40, 20),  (60, 120, 60),  (245, 245, 245), (210, 40, 40),  (60, 80, 160),  (70, 45, 25),  'suspenders'),
    'petra':     ((235, 190, 150), (200, 90, 30), (80, 140, 90),  (215, 170, 60),  (170, 30, 30),  (80, 70, 70),   (50, 40, 40),  'bandana'),
    'axel':      ((225, 170, 130), (40, 30, 25),  (70, 60, 50),   (55, 130, 60),   (40, 40, 40),   (60, 60, 65),   (40, 40, 40),  'hoodie'),
    'olivia':    ((150, 100, 70),  (30, 25, 20),  (60, 50, 40),   (200, 60, 60),   (245, 245, 245),(60, 60, 60),   (40, 40, 40),  'goggles'),
    'lukas':     ((240, 200, 165), (230, 200, 90),(80, 130, 180), (60, 60, 60),    (180, 160, 60), (60, 90, 150),  (50, 40, 30),  'jacket'),
    'ivor':      ((225, 180, 150), (30, 30, 30),  (60, 60, 60),   (60, 40, 90),    (140, 90, 200), (50, 40, 60),   (40, 30, 30),  'robe'),
    'radar':     ((140, 90, 60),   (40, 30, 25),  (60, 50, 40),   (240, 240, 240), (80, 80, 200),  (70, 70, 90),   (40, 40, 40),  'glasses'),
    'gabriel':   ((150, 95, 65),   (35, 30, 25),  (60, 50, 40),   (80, 80, 90),    (200, 200, 220),(70, 70, 80),   (40, 40, 40),  'armor'),
    'ellegaard': ((240, 200, 170), (200, 60, 40), (80, 120, 90),  (200, 50, 50),   (120, 120, 130),(60, 60, 60),   (40, 40, 40),  'redstone'),
    'magnus':    ((225, 175, 140), (40, 30, 25),  (60, 50, 40),   (60, 120, 60),   (40, 40, 40),   (50, 50, 55),   (40, 40, 40),  'griefer'),
    'soren':     ((240, 205, 175), (240, 240, 240),(90, 120, 150),(230, 230, 235), (150, 160, 180),(80, 90, 110),  (50, 50, 60),  'robe'),
    'harper':    ((150, 100, 70),  (240, 240, 240),(60, 50, 40),  (60, 140, 150),  (240, 240, 240),(60, 60, 70),   (40, 40, 40),  'jacket'),
    'stella':    ((240, 205, 175), (230, 200, 90),(80, 130, 180), (250, 230, 240), (230, 170, 50), (200, 100, 150),(80, 60, 60),  'crown'),
    'nurm':      ((190, 150, 110), (60, 40, 30),  (60, 50, 40),   (120, 90, 60),   (80, 60, 40),   (100, 80, 60),  (50, 40, 30),  'villager'),
    'jack':      ((225, 175, 140), (60, 45, 30),  (80, 120, 90),  (110, 80, 50),   (200, 160, 100),(70, 60, 50),   (40, 30, 25),  'explorer'),
    'binta':     ((140, 90, 60),   (30, 25, 20),  (60, 50, 40),   (220, 90, 60),   (240, 200, 60), (70, 60, 60),   (40, 40, 40),  'jacket'),
    'otto':      ((230, 190, 160), (200, 200, 200),(80, 80, 90),  (240, 240, 240), (200, 170, 60), (90, 90, 100),  (50, 50, 60),  'robe'),
    'hadrian':   ((225, 180, 150), (60, 50, 50),  (70, 60, 60),   (250, 250, 250), (40, 40, 40),   (240, 240, 240),(40, 40, 40),  'suit'),
    'maya':      ((150, 100, 70),  (30, 25, 20),  (60, 50, 40),   (60, 60, 60),    (180, 60, 200), (50, 50, 60),   (40, 40, 40),  'jacket'),
    'stampy':    ((230, 190, 160), (230, 150, 60),(80, 120, 60),  (250, 120, 40),  (250, 250, 250),(250, 120, 40), (60, 60, 60),  'cat'),
    'aiden':     ((235, 190, 150), (60, 45, 30),  (60, 100, 60),  (50, 50, 60),    (200, 200, 200),(60, 60, 70),   (40, 40, 40),  'jacket'),
    'reuben':    ((240, 170, 170), (240, 170, 170),(30, 30, 30),  (240, 170, 170), (230, 130, 140),(240, 170, 170),(200, 120, 130),'pig'),
}

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)

def fill(px, x0, y0, w, h, c, a=255):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            px[y * W + x] = (c[0], c[1], c[2], a)

def box(px, x, y, sx, sy, sz, top, bottom, front, back, left, right):
    """Standard MC box UV: (x,y) is the top-left of the box net."""
    fill(px, x + sz,      y,      sx, sz, top)            # top
    fill(px, x + sz + sx, y,      sx, sz, bottom)         # bottom
    fill(px, x,           y + sz, sz, sy, right)          # right
    fill(px, x + sz,      y + sz, sx, sy, front)          # front
    fill(px, x + sz + sx, y + sz, sz, sy, left)           # left
    fill(px, x + 2*sz+sx, y + sz, sx, sy, back)           # back

def make(name, spec):
    skin, hair, eyes, top, acc, pants, shoes, extra = spec
    px = [(0, 0, 0, 0)] * (W * H)
    d = lambda c: shade(c, 0.82)
    # HEAD 8x8x8 at (0,0)
    box(px, 0, 0, 8, 8, 8, hair, skin, skin, hair, skin, skin)
    # hair on front top rows / sides
    fill(px, 8, 8, 8, 2, hair); fill(px, 0, 8, 8, 3, hair); fill(px, 16, 8, 8, 3, hair)
    # eyes: whites + iris at front (8..16, 8..16)
    fill(px, 10, 12, 1, 1, (255, 255, 255)); fill(px, 11, 12, 1, 1, eyes)
    fill(px, 13, 12, 1, 1, eyes); fill(px, 14, 12, 1, 1, (255, 255, 255))
    fill(px, 12, 14, 1, 1, shade(skin, 0.7))                          # nose
    fill(px, 11, 15, 3, 1, (150, 70, 70))                             # mouth
    # BODY 8x12x4 at (16,16)
    box(px, 16, 16, 8, 12, 4, top, d(pants), top, d(top), d(top), d(top))
    # RIGHT ARM 4x12x4 at (40,16), LEFT ARM at (32,48)
    box(px, 40, 16, 4, 12, 4, top, skin, top, d(top), d(top), d(top))
    box(px, 32, 48, 4, 12, 4, top, skin, top, d(top), d(top), d(top))
    # hands
    fill(px, 44, 26, 4, 2, skin); fill(px, 36, 58, 4, 2, skin)
    # RIGHT LEG 4x12x4 at (0,16), LEFT LEG at (16,48)
    box(px, 0, 16, 4, 12, 4, pants, shoes, pants, d(pants), d(pants), d(pants))
    box(px, 16, 48, 4, 12, 4, pants, shoes, pants, d(pants), d(pants), d(pants))
    fill(px, 4, 26, 4, 2, shoes); fill(px, 20, 58, 4, 2, shoes)
    # outfit accents on body front (20..28, 20..32)
    if extra == 'suspenders':
        fill(px, 21, 20, 1, 12, acc); fill(px, 26, 20, 1, 12, acc); fill(px, 20, 30, 8, 2, d(acc))
    elif extra == 'bandana':
        fill(px, 8, 8, 8, 2, acc); fill(px, 0, 8, 8, 2, acc); fill(px, 16, 8, 8, 2, acc); fill(px, 24, 8, 8, 2, acc)
        fill(px, 20, 20, 8, 3, acc)                                    # armour trim
    elif extra == 'hoodie':
        fill(px, 20, 20, 8, 2, acc); fill(px, 23, 22, 2, 10, acc)
    elif extra == 'goggles':
        fill(px, 8, 8, 8, 2, acc); fill(px, 9, 9, 2, 1, (60, 60, 60)); fill(px, 13, 9, 2, 1, (60, 60, 60))
        fill(px, 20, 20, 8, 12, acc); fill(px, 21, 21, 6, 10, top)
    elif extra == 'jacket':
        fill(px, 23, 20, 2, 12, acc)
    elif extra == 'robe':
        fill(px, 20, 20, 8, 1, acc); fill(px, 23, 21, 2, 11, acc)
        fill(px, 16, 48, 16, 16, top); fill(px, 0, 16, 16, 16, top)   # robe over legs
        box(px, 0, 16, 4, 12, 4, top, shoes, top, d(top), d(top), d(top))
        box(px, 16, 48, 4, 12, 4, top, shoes, top, d(top), d(top), d(top))
    elif extra == 'glasses':
        fill(px, 9, 12, 3, 1, (40, 40, 40)); fill(px, 12, 12, 3, 1, (40, 40, 40)); fill(px, 12, 12, 1, 1, (40, 40, 40))
        fill(px, 10, 12, 1, 1, (255, 255, 255)); fill(px, 14, 12, 1, 1, (255, 255, 255))
        fill(px, 20, 20, 8, 1, acc)
    elif extra == 'armor':
        fill(px, 20, 20, 8, 12, acc); fill(px, 22, 22, 4, 8, top)
        fill(px, 8, 8, 8, 1, acc)
    elif extra == 'redstone':
        fill(px, 20, 20, 8, 2, acc); fill(px, 23, 22, 2, 2, (200, 20, 20)); fill(px, 8, 8, 8, 2, acc)
    elif extra == 'griefer':
        fill(px, 20, 20, 8, 1, acc); fill(px, 21, 26, 6, 2, acc); fill(px, 8, 8, 8, 3, (40, 40, 40))
    elif extra == 'crown':
        fill(px, 8, 8, 8, 1, acc); fill(px, 0, 8, 8, 1, acc); fill(px, 16, 8, 8, 1, acc); fill(px, 24, 8, 8, 1, acc)
        fill(px, 20, 22, 8, 1, acc)
    elif extra == 'villager':
        fill(px, 20, 20, 8, 1, acc); fill(px, 11, 13, 3, 3, shade(skin, 0.85))
    elif extra == 'explorer':
        fill(px, 20, 20, 8, 2, acc); fill(px, 8, 8, 8, 2, (120, 90, 50)); fill(px, 11, 15, 3, 2, hair)
    elif extra == 'suit':
        fill(px, 22, 20, 4, 12, acc); fill(px, 23, 21, 2, 10, top); fill(px, 8, 8, 8, 2, hair)
    elif extra == 'cat':
        fill(px, 20, 20, 8, 12, acc); fill(px, 20, 22, 8, 1, top); fill(px, 20, 26, 8, 1, top)
        fill(px, 8, 8, 1, 1, top); fill(px, 15, 8, 1, 1, top)
    elif extra == 'pig':
        fill(px, 10, 12, 1, 1, eyes); fill(px, 14, 12, 1, 1, eyes); fill(px, 11, 13, 3, 2, acc)
        fill(px, 8, 8, 8, 2, skin)
    # HAT layer (32,0): transparent except hair volume for a bit of depth
    for x in range(40, 48):
        for y in range(8, 10):
            px[y * W + x] = (hair[0], hair[1], hair[2], 255)
    return px

def main():
    os.makedirs(OUT, exist_ok=True)
    for name, spec in CAST.items():
        write_png(os.path.join(OUT, name + '.png'), W, H, make(name, spec))
    print('wrote %d cast skins to %s' % (len(CAST), OUT))

if __name__ == '__main__':
    main()
