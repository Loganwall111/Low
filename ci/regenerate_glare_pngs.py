#!/usr/bin/env python3
"""regenerate_skybox_pngs.py -- rebuild the retained glare PNGs.

Sky colour is owned by the native Java SkyRenderer state and is never
serialized into a texture-pack strip. The existing glare textures remain
because the glare system still consumes them.

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

# radial smudge discs, (radius 0..1, (r, g, b, a)), 256x256
GLARE_DISCS = {
    'glare/phase5.png': [
        (0.00, (22, 26, 29, 235)),
        (0.22, (45, 66, 63, 200)),
        (0.55, (106, 154, 120, 110)),
        (1.00, (106, 154, 120, 0))],
    'glare/phase54.png': [
        (0.00, (11, 4, 16, 230)),
        (0.30, (45, 20, 66, 180)),
        (0.70, (112, 56, 135, 90)),
        (1.00, (112, 56, 135, 0))],
    'glare/phase55.png': [
        (0.00, (11, 4, 16, 235)),
        (0.20, (45, 20, 66, 200)),
        (0.50, (88, 28, 110, 150)),
        (0.80, (135, 82, 156, 80)),
        (1.00, (135, 82, 156, 0))],
    'glare/phase6.png': [
        (0.00, (26, 18, 38, 235)),
        (0.25, (70, 42, 82, 200)),
        (0.55, (150, 97, 115, 140)),
        (0.80, (216, 152, 116, 80)),
        (1.00, (216, 152, 116, 0))],
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
        for name, anchors in GLARE_DISCS.items():
            path = os.path.join(root, name)
            write_png(path, 256, 256, disc_fn(anchors, 256, 256))
            print('wrote', path)

if __name__ == '__main__':
    main()
