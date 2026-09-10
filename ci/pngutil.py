#!/usr/bin/env python3
"""pngutil.py - dependency-free PNG read/write for the Devouring Storms asset
pipeline.

The sandbox has no Pillow, so every texture inspection/rewrite in this repo
goes through this module: a small chunk parser plus the five PNG row filters,
returning a flat list of (r, g, b, a) tuples exactly like
``make_branding.write_png`` consumes.
"""
import struct
import zlib

__all__ = ["read_png", "write_png", "size", "dominant", "crop", "scale"]


def _chunks(data):
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    off = 8
    while off + 8 <= len(data):
        (n,) = struct.unpack(">I", data[off:off + 4])
        tag = data[off + 4:off + 8]
        body = data[off + 8:off + 8 + n]
        yield tag, body
        off += 12 + n


def _paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def read_png(path):
    """Return (w, h, pixels) where pixels is a flat list of RGBA tuples."""
    data = open(path, "rb").read()
    ihdr = None
    idat = bytearray()
    plte = None
    trns = None
    for tag, body in _chunks(data):
        if tag == b"IHDR":
            ihdr = body
        elif tag == b"IDAT":
            idat += body
        elif tag == b"PLTE":
            plte = body
        elif tag == b"tRNS":
            trns = body
    if ihdr is None:
        raise ValueError("%s: no IHDR" % path)
    w, h, depth, ctype, comp, filt, inter = struct.unpack(">IIBBBBB", ihdr)
    if inter != 0:
        raise ValueError("%s: interlaced PNGs are not supported" % path)
    if depth != 8:
        raise ValueError("%s: bit depth %d is not supported" % (path, depth))
    raw = zlib.decompress(bytes(idat))
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    stride = w * nch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    pos = 0
    for y in range(h):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        if f == 1:
            for i in range(nch, stride):
                line[i] = (line[i] + line[i - nch]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                c = prev[i - nch] if i >= nch else 0
                line[i] = (line[i] + _paeth(a, prev[i], c)) & 0xFF
        elif f != 0:
            raise ValueError("%s: unknown filter %d" % (path, f))
        out[y * stride:(y + 1) * stride] = line
        prev = line
    px = []
    for i in range(w * h):
        o = i * nch
        if ctype == 6:
            px.append((out[o], out[o + 1], out[o + 2], out[o + 3]))
        elif ctype == 2:
            px.append((out[o], out[o + 1], out[o + 2], 255))
        elif ctype == 4:
            px.append((out[o], out[o], out[o], out[o + 1]))
        elif ctype == 0:
            px.append((out[o], out[o], out[o], 255))
        else:  # palette
            idx = out[o]
            r, g, b = plte[idx * 3], plte[idx * 3 + 1], plte[idx * 3 + 2]
            a = trns[idx] if trns is not None and idx < len(trns) else 255
            px.append((r, g, b, a))
    return w, h, px


def write_png(path, w, h, px):
    import os
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for r, g, b, a in px[y * w:(y + 1) * w]:
            raw += bytes((int(r) & 255, int(g) & 255, int(b) & 255, int(a) & 255))
    comp = zlib.compress(bytes(raw), 9)

    def chunk(tag, body):
        c = struct.pack(">I", len(body)) + tag + body
        return c + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", comp)
    png += chunk(b"IEND", b"")
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    open(path, "wb").write(png)


def size(path):
    data = open(path, "rb").read()
    w, h = struct.unpack(">II", data[16:24])
    return w, h


def crop(px, w, h, x0, y0, cw, ch):
    out = []
    for y in range(y0, y0 + ch):
        base = y * w
        out.extend(px[base + x0:base + x0 + cw])
    return out


def scale(px, w, h, nw, nh):
    out = []
    for y in range(nh):
        sy = min(h - 1, y * h // nh)
        base = sy * w
        for x in range(nw):
            out.append(px[base + min(w - 1, x * w // nw)])
    return out


def dominant(px, w, h, bins=6):
    """Top opaque colours as (rgb, count) — used to sample reference frames."""
    from collections import Counter
    c = Counter()
    for r, g, b, a in px:
        if a < 128:
            continue
        c[(r // bins * bins, g // bins * bins, b // bins * bins)] += 1
    total = sum(c.values()) or 1
    return [(rgb, round(100.0 * n / total, 2)) for rgb, n in c.most_common(12)]
