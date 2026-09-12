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
# 1.9.215 R2: CORRECTED 2026-09-11 hex decks + reference day/midnight strips.
SKY_STRIPS = {
    'sky/day.png': [
        (122, 116, 224), (126, 125, 229), (133, 135, 236),
        (143, 147, 239), (163, 158, 241), (192, 170, 242)],
    'sky/night.png': [
        (8, 10, 52), (14, 17, 78), (27, 32, 120),
        (39, 49, 159), (51, 67, 204), (68, 92, 245)],
    'sky/phase5_teal.png': [
        (22, 26, 29), (30, 38, 39), (45, 66, 63),
        (75, 108, 90), (90, 130, 104), (106, 154, 120)],
    'sky/phase5_purple.png': [
        (11, 4, 16), (22, 9, 32), (45, 20, 66),
        (88, 28, 110), (112, 56, 135), (135, 82, 156)],
    'sky/phase54.png': [
        (26, 14, 38), (45, 20, 66), (88, 28, 110),
        (112, 56, 135), (135, 82, 156), (135, 82, 156)],
    'sky/phase55.png': [
        (11, 4, 16), (22, 9, 32), (45, 20, 66),
        (88, 28, 110), (112, 56, 135), (135, 82, 156)],
    'sky/phase6.png': [
        (26, 18, 38), (70, 42, 82), (150, 97, 115), (216, 152, 116)],
}

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

# FabricSkyBoxes skybox textures (the base mod's own skyboxes; jar-overrides
# replace them in the built jar so the FBS-on look matches the references).
FBS_STRIPS = {
    'assets/fabricskyboxes/textures/sky/day.png': SKY_STRIPS['sky/day.png'],
    'assets/fabricskyboxes/textures/sky/night.png': SKY_STRIPS['sky/night.png'],
    'assets/fabricskyboxes/textures/sky/sky_twilight.png': [
        (22, 70, 86), (66, 81, 87), (201, 77, 46), (184, 54, 38), (92, 20, 50)],
    'assets/dabywitherstormmod/textures/sky/day.png': SKY_STRIPS['sky/day.png'],
    'assets/dabywitherstormmod/textures/sky/night.png': SKY_STRIPS['sky/night.png'],
    'assets/dabywitherstormmod/textures/sky/sunset.png': [
        (22, 70, 86), (66, 81, 87), (201, 77, 46), (184, 54, 38), (92, 20, 50)],
    'assets/dabywitherstormmod/textures/environment/storymode_sky_day.png': SKY_STRIPS['sky/day.png'],
    'assets/dabywitherstormmod/textures/environment/storymode_sky_night.png': SKY_STRIPS['sky/night.png'],
    'assets/dabywitherstormmod/textures/environment/storymode_sky_sunset.png': [
        (22, 70, 86), (66, 81, 87), (201, 77, 46), (184, 54, 38), (92, 20, 50)],
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
    # jar-overrides: FabricSkyBoxes skybox textures + the mod's own sky
    # textures and Story Mode environment strips (all replaced in the jar).
    for name, anchors in FBS_STRIPS.items():
        path = os.path.join('jar-overrides', name)
        write_png(path, 1024, 1024, strip_fn(anchors, 1024))
        print('wrote', path)


if __name__ == '__main__':
    main()
