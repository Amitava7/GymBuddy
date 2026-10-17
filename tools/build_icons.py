#!/usr/bin/env python3
"""Regenerate the icon resources in app/src/main/res.

The React Native app drew its icons with the Ionicons font from
@expo/vector-icons. Shipping that font would cost 390 KB, so this script pulls
the exact same font out of the pinned npm package, takes only the glyphs the
app uses and writes each one as a VectorDrawable. The shapes are identical to
what the old app showed; they just are not a font any more.

It also scales assets/icon.png down to the launcher foreground, the same way
Expo's prebuild did: the whole picture fills the adaptive icon layer, on the
same #0A0A0A background.

  pip install fonttools pillow
  python3 tools/build_icons.py

Needs network access the first time (the npm tarball is cached in tools/cache).
"""

import io
import json
import os
import tarfile
import urllib.request

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RES = os.path.join(ROOT, "app", "src", "main", "res")
CACHE = os.path.join(HERE, "cache")

PACKAGE = "@expo/vector-icons"
VERSION = "15.1.1"   # the version the React Native app shipped with
TARBALL = "https://registry.npmjs.org/%s/-/vector-icons-%s.tgz" % (PACKAGE, VERSION)
FONT = "package/build/vendor/react-native-vector-icons/Fonts/Ionicons.ttf"
GLYPHMAP = "package/build/vendor/react-native-vector-icons/glyphmaps/Ionicons.json"

# Every Ionicons name the old screens used.
ICONS = [
    "add", "add-circle", "add-circle-outline", "barbell", "barbell-outline",
    "business", "calendar-outline", "checkmark-circle", "checkmark-done",
    "chevron-forward", "close", "close-circle", "close-circle-outline",
    "create-outline", "document-text", "download-outline", "ellipse-outline",
    "fitness-outline", "layers-outline", "play-circle", "search",
    "share-outline", "swap-vertical-outline", "time", "time-outline",
    "trash-outline", "trophy",
]

# The native stack header's back arrow is the Material one, not an Ionicon.
BACK_ARROW = "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"


def fetch_package():
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, "vector-icons-%s.tgz" % VERSION)
    if not os.path.exists(path):
        print("downloading", TARBALL)
        with urllib.request.urlopen(TARBALL) as r, open(path, "wb") as fh:
            fh.write(r.read())
    with tarfile.open(path) as tf:
        font = tf.extractfile(FONT).read()
        glyphmap = json.load(tf.extractfile(GLYPHMAP))
    return font, glyphmap


def num(v):
    v = round(v, 2)
    return ("%d" % v) if v == int(v) else ("%g" % v)


def vector(path_data, viewport, comment):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- %s -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="24dp"\n'
        '    android:height="24dp"\n'
        '    android:viewportWidth="%d"\n'
        '    android:viewportHeight="%d">\n'
        '    <path\n'
        '        android:fillColor="#FFFFFFFF"\n'
        '        android:pathData="%s" />\n'
        "</vector>\n" % (comment, viewport, viewport, path_data)
    )


def write(rel, text):
    path = os.path.join(RES, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


def build_glyphs():
    font_bytes, glyphmap = fetch_package()
    font = TTFont(io.BytesIO(font_bytes))
    em = font["head"].unitsPerEm               # 512
    top = font["OS/2"].sTypoAscender           # 448: the glyphs fill y in [-64, 448]
    cmap = font.getBestCmap()
    glyphs = font.getGlyphSet()
    total = 0
    for name in ICONS:
        glyph = cmap[glyphmap[name]]
        pen = SVGPathPen(glyphs, ntos=num)
        # font units are y-up from the baseline; the drawable is y-down from the top
        glyphs[glyph].draw(TransformPen(pen, (1, 0, 0, -1, 0, top)))
        data = pen.getCommands()
        res = "drawable/ic_%s.xml" % name.replace("-", "_")
        write(res, vector(data, em, "Ionicons \"%s\" from %s@%s" % (name, PACKAGE, VERSION)))
        total += len(data)
    write("drawable/ic_back.xml", vector(BACK_ARROW, 24, "Material arrow_back, the native stack back button"))
    print("%d glyphs, %d bytes of path data" % (len(ICONS), total))


def build_launcher():
    from PIL import Image
    src = Image.open(os.path.join(ROOT, "assets", "icon.png")).convert("RGB")
    # 108 dp at xxxhdpi. Android scales the one bitmap for every density.
    fg = src.resize((432, 432), Image.LANCZOS)
    out = os.path.join(RES, "drawable-nodpi", "ic_launcher_foreground.webp")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    fg.save(out, "WEBP", quality=90, method=6)
    print("launcher foreground", os.path.getsize(out), "bytes")


if __name__ == "__main__":
    build_glyphs()
    build_launcher()
