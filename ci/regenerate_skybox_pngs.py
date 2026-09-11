#!/usr/bin/env python3
"""regenerate_skybox_pngs.py -- rebuild the mcsm_atmosphere sky/glare PNGs.

MCSM 1.9.201 -- the user's skybox replacement PNGs could not be read in this
sandbox, so the shipped PNGs are rebuilt from the 2026-09-11 palette decks
instead (the same hexes the GLSL sky pass and the Java fog decks now use).
Drop the original uploaded PNGs over these filenames whenever they are
available and they will take over verbatim:

  sky/day.png            vivid mid-blue vault -> pale lilac horizon
  sky/night.png          PURPLE night (was blue)
  sky/phase5_teal.png    #1A2223 -> #2E4544 -> #7C9885 -> pale mint
  sky/phase5_purple.png  #0F0814 -> #3A1B54 -> #5E2775 -> #7D4B91 -> pink
  sky/phase54.png        5.4 transition deck
  sky/phase55.png        5.5-5.9 corruption deck (near-black overhead)
  sky/phase6.png         four-color split #171021/#44284D/#A36B73/#D69776
  glare/phase5.png       radial smudge: #8493FF beam -> #2E4544 -> #7C9885
  glare/phase54.png      radial smudge: purple transition
  glare/phase55.png      radial smudge: #0F0814 core -> #3A1B54 -> #7D4B91
  glare/phase6.png       radial smudge: #171021 -> #A36B73 -> #D69776

Dependency-free RGBA8 PNG writer (filter 0 rows + zlib, no Pillow needed).
Usage: python3 ci/regenerate_skybox_pngs.py
"""
import os
import struct
import zlib

ROOTS = [
    'src/main/resources/assets/dabywitherstormmod/textures/mcsm_atmosphere',
    'jar-overrides/assets/dabywitherstormmod/textures/mcsm_atmosphere',
]

# vertical strips, top (zenith) -> bottom (horizon), 1024x1024
SKY_STRIPS = {
    'sky/day.png': [
        (76, 120, 240), (97, 135, 246), (120, 150, 251),
        (143, 164, 254), (166, 178, 255), (194, 194, 255)],
    'sky/night.png': [
        (11, 7, 27), (15, 10, 38), (22, 15, 56),
        (31, 23, 79), (43, 34, 107), (61, 51, 143)],
    'sky/phase5_teal.png': [
        (26, 34, 35), (40, 52, 52), (66, 88, 84),
        (104, 130, 120), (140, 164, 150), (170, 200, 170)],
    'sky/phase5_purple.png': [
        (17, 6, 34), (28, 9, 50), (58, 27, 84),
        (94, 39, 117), (149, 76, 169), (230, 140, 165)],
    'sky/phase54.png': [
        (30, 25, 45), (55, 30, 70), (90, 40, 100),
        (130, 60, 120), (180, 110, 140), (220, 150, 160)],
    'sky/phase55.png': [
        (10, 4, 28), (26, 9, 50), (58, 27, 84),
        (94, 39, 117), (125, 75, 145), (230, 140, 165)],
    'sky/phase6.png': [
        (23, 16, 33), (68, 40, 77), (163, 107, 115), (214, 151, 118)],
}

# radial smudge discs, (radius 0..1, (r, g, b, a)), 256x256
GLARE_DISCS = {
    'glare/phase5.png': [
        (0.00, (132, 147, 255, 235)),
        (0.22, (46, 69, 68, 190)),
        (0.55, (124, 152, 133, 110)),
        (1.00, (124, 152, 133, 0))],
    'glare/phase54.png': [
        (0.00, (30, 20, 50, 230)),
        (0.30, (70, 40, 100, 180)),
        (0.70, (130, 70, 130, 90)),
        (1.00, (130, 70, 130, 0))],
    'glare/phase55.png': [
        (0.00, (15, 8, 20, 235)),
        (0.20, (58, 27, 84, 200)),
        (0.50, (94, 39, 117, 150)),
        (0.80, (125, 75, 145, 80)),
        (1.00, (125, 75, 145, 0))],
    'glare/phase6.png': [
        (0.00, (23, 16, 33, 235)),
        (0.25, (68, 40, 77, 200)),
        (0.55, (163, 107, 115, 140)),
        (0.80, (214, 151, 118, 80)),
        (1.00, (214, 151, 118, 0))],
}


def clamp8(v):
    v = int(round(v))
    return max(0, min(255, v))


def write_png(path, w, h, pixel_fn):
    raw = bytearray()
    for y in range(h):
        raw.append(0)  # filter type 0 (None)
        for x in range(w):
            r, g, b, a = pixel_fn(x, y)
            raw += bytes((clamp8(r), clamp8(g), clamp8(b), clamp8(a)))

    def chunk(tag, data):
        return (struct.pack('>I', len(data)) + tag + data
                + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', ihdr)
           + chunk(b'IDAT', zlib.compress(bytes(raw), 9))
           + chunk(b'IEND', b''))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(png)


def strip_fn(anchors, h):
    n = len(anchors)
    seg = (h - 1) / max(n - 1, 1)

    def fn(x, y):
        t = y / seg
        i = min(int(t), n - 2)
        f = t - i
        a, b = anchors[i], anchors[i + 1]
        return (a[0] + (b[0] - a[0]) * f,
                a[1] + (b[1] - a[1]) * f,
                a[2] + (b[2] - a[2]) * f, 255)
    return fn


def disc_fn(anchors, w, h):
    n = len(anchors)
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    maxr = max(cx, cy)

    def fn(x, y):
        r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5 / maxr
        for k in range(n - 1):
            r0, c0 = anchors[k]
            r1, c1 = anchors[k + 1]
            if r <= r1 or k == n - 2:
                f = 0.0 if r1 == r0 else (r - r0) / (r1 - r0)
                f = max(0.0, min(1.0, f))
                return (c0[0] + (c1[0] - c0[0]) * f,
                        c0[1] + (c1[1] - c0[1]) * f,
                        c0[2] + (c1[2] - c0[2]) * f,
                        c0[3] + (c1[3] - c0[3]) * f)
        return (0, 0, 0, 0)
    return fn


def main():
    for root in ROOTS:
        for name, anchors in SKY_STRIPS.items():
            path = os.path.join(root, name)
            write_png(path, 1024, 1024, strip_fn(anchors, 1024))
            print('wrote', path)
        for name, anchors in GLARE_DISCS.items():
            path = os.path.join(root, name)
            write_png(path, 256, 256, disc_fn(anchors, 256, 256))
            print('wrote', path)


if __name__ == '__main__':
    main()
