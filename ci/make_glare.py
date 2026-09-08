#!/usr/bin/env python3
"""make_glare.py — soft fade-gradient glare (1.9.153).

User order: the backdrop has a FADE gradient, not an opaque ball.
  storm_glare.png - 512x512 white radial gradient, long soft skirt to zero
  storm_white.png - 8x8 flat white emissive square
"""
import math
import sys

sys.path.insert(0, 'ci')
from make_branding import write_png  # noqa: E402


def glare():
    n = 512
    px = []
    for y in range(n):
        for x in range(n):
            dx = (x + 0.5) / n * 2.0 - 1.0
            dy = (y + 0.5) / n * 2.0 - 1.0
            r = math.sqrt(dx * dx + dy * dy)
            t = min(r, 1.0)
            # longer soft skirt: bright tiny core, long fade, zero edge
            # power 2.8 + smoothstep gives the fade-gradient the frames show
            a = max(0.0, 1.0 - t)
            a = a ** 2.8
            a = a * a * (3.0 - 2.0 * a)
            # crush the hard core so it never reads as a solid disc
            core = max(0.0, 1.0 - t / 0.22)
            a = a * (0.55 + 0.45 * (1.0 - core * 0.5))
            v = int(round(255.0 * min(a, 1.0)))
            px.append((255, 255, 255, v))
    return n, px


def white():
    n = 8
    px = [(255, 255, 255, 255)] * (n * n)
    return n, px


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    n, px = glare()
    write_png(sys.argv[1], n, n, px)
    n, px = white()
    write_png(sys.argv[2], n, n, px)
    print('[glare] wrote soft fade %s and %s' % (sys.argv[1], sys.argv[2]))
    return 0


if __name__ == '__main__':
    sys.exit(main())
