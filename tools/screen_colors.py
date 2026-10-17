#!/usr/bin/env python3
"""Say how much of a screen is each of the app's colours.

CI cannot look at pictures, so tools/smoke_test.sh asks this instead: a home
screen that is mostly #0A0A0A with some #3DC2EC in it is the GymBuddy theme; a
white or blank screen is not.

  adb exec-out screencap > screen.raw
  python3 tools/screen_colors.py screen.raw
"""

import struct
import sys

# Ui.java. Translucent tints are listed blended over what they sit on.
PALETTE = [
    ("background", (0x0A, 0x0A, 0x0A)),
    ("surface", (0x16, 0x16, 0x16)),
    ("border", (0x25, 0x25, 0x25)),
    ("primary", (0x3D, 0xC2, 0xEC)),
    ("primary", (0x1A, 0x26, 0x2B)),   # primary at 8% over surface: icon boxes, pills
    ("text", (0xF5, 0xF5, 0xF5)),
    ("secondary", (0x8A, 0x8A, 0x8A)),
    ("light", (0x4A, 0x4A, 0x4A)),
    ("danger", (0xEF, 0x44, 0x44)),
    ("warning", (0xFB, 0xBF, 0x24)),
]


def read_screencap(path):
    """screencap writes width, height, format, [colourspace], then RGBA."""
    with open(path, "rb") as fh:
        data = fh.read()
    w, h, _fmt = struct.unpack("<III", data[:12])
    for header in (12, 16):
        if len(data) - header == w * h * 4:
            return w, h, header, data
    sys.exit("not a screencap dump: %dx%d does not match %d bytes" % (w, h, len(data)))


def main():
    w, h, off, data = read_screencap(sys.argv[1])
    counts = {}
    step = 4
    total = 0
    for y in range(0, h, step):
        for x in range(0, w, step):
            i = off + (y * w + x) * 4
            r, g, b = data[i], data[i + 1], data[i + 2]
            best, best_d = "other", 900
            for name, (pr, pg, pb) in PALETTE:
                d = (r - pr) ** 2 + (g - pg) ** 2 + (b - pb) ** 2
                if d < best_d:
                    best, best_d = name, d
            counts[best] = counts.get(best, 0) + 1
            total += 1
    parts = sorted(counts.items(), key=lambda kv: -kv[1])
    print("screen %dx%d: " % (w, h) + " ".join("%s=%.1f%%" % (k, 100.0 * v / total) for k, v in parts))
    for k, v in parts:
        print("PCT_%s %.1f" % (k.upper(), 100.0 * v / total))


if __name__ == "__main__":
    main()
