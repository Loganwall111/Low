#!/usr/bin/env python3
"""1.9.153: face-painted backdrop KILLED. Emit a fully transparent 256x256 so
any leftover code path that still binds storm_face.png draws nothing."""
import sys
sys.path.insert(0, "ci")
from make_branding import write_png  # noqa: E402

W = H = 256
px = [(0, 0, 0, 0)] * (W * H)
write_png(sys.argv[1], W, H, px)
print("storm face KILLED (transparent):", sys.argv[1])
