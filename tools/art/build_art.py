#!/usr/bin/env python3
"""Illustrations originales de Somnia (Brumi et créatures du rêve).

Génère art/svg/*.svg (source), les VectorDrawable Android dans app/src/main/res/drawable/art_*.xml,
et un aperçu art/preview.html. Usage : python3 tools/art/build_art.py
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from svg2vd import convert  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
INK = "#23264A"
WHITE = "#FFFFFF"
BLUSH = "#FF9EC4"
GOLD = "#FFD98A"
GOLD_DEEP = "#F2B84B"


# ---------- primitives ----------
def svg(body, defs=""):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><defs>%s</defs>%s</svg>' % (defs, body))


def lin(gid, x1, y1, x2, y2, stops):
    s = "".join('<stop offset="%s" stop-color="%s" stop-opacity="%s"/>' % (o, c, a) for o, c, a in stops)
    return '<linearGradient id="%s" gradientUnits="userSpaceOnUse" x1="%s" y1="%s" x2="%s" y2="%s">%s</linearGradient>' % (gid, x1, y1, x2, y2, s)


def rad(gid, cx, cy, r, stops):
    s = "".join('<stop offset="%s" stop-color="%s" stop-opacity="%s"/>' % (o, c, a) for o, c, a in stops)
    return '<radialGradient id="%s" gradientUnits="userSpaceOnUse" cx="%s" cy="%s" r="%s">%s</radialGradient>' % (gid, cx, cy, r, s)


def circle(cx, cy, r, fill, op=1):
    return '<circle cx="%s" cy="%s" r="%s" fill="%s" opacity="%s"/>' % (cx, cy, r, fill, op)


def ellipse(cx, cy, rx, ry, fill, op=1, rot=None):
    e = '<ellipse cx="%s" cy="%s" rx="%s" ry="%s" fill="%s" opacity="%s"/>' % (cx, cy, rx, ry, fill, op)
    return '<g transform="rotate(%s %s %s)">%s</g>' % (rot, cx, cy, e) if rot else e


def path(d, fill="none", stroke=None, width=None, op=1):
    s = '<path d="%s" fill="%s" opacity="%s"' % (d, fill, op)
    if stroke:
        s += ' stroke="%s" stroke-width="%s" stroke-linecap="round" stroke-linejoin="round"' % (stroke, width)
    return s + "/>"


def glow(gid, cx, cy, r, c, a=0.45):
    return rad(gid, cx, cy, r, [(0, c, a), (0.6, c, a * 0.35), (1, c, 0)]), circle(cx, cy, r, "url(#%s)" % gid)


def sparkle(cx, cy, r, fill=GOLD, op=1):
    k = r * 0.18
    d = ("M{x},{t} Q{a},{b} {r_},{y} Q{a},{c} {x},{bt} Q{d},{c} {l},{y} Q{d},{b} {x},{t} Z"
         .format(x=cx, y=cy, t=cy - r, bt=cy + r, r_=cx + r, l=cx - r, a=cx + k, d=cx - k, b=cy - k, c=cy + k))
    return path(d, fill, op=op)


def eyes_open(cx, cy, dx, rx=10, ry=12, ink=INK, shine=WHITE):
    out = ""
    for x in (cx - dx, cx + dx):
        out += ellipse(x, cy, rx, ry, ink)
        out += circle(x + rx * 0.35, cy - ry * 0.4, rx * 0.42, shine)
        out += circle(x - rx * 0.35, cy + ry * 0.38, rx * 0.2, shine, 0.8)
    return out


def eyes_closed(cx, cy, dx, w=11, ink=INK, width=5):
    out = ""
    for x in (cx - dx, cx + dx):
        out += path("M%s %s Q%s %s %s %s" % (x - w, cy - 2, x, cy + w * 0.8, x + w, cy - 2), stroke=ink, width=width)
    return out


def blush(cx, cy, dx, rx=11, ry=7, c=BLUSH, op=0.5):
    return ellipse(cx - dx, cy, rx, ry, c, op) + ellipse(cx + dx, cy, rx, ry, c, op)


def smile(cx, cy, w=8, ink=INK, width=4):
    return path("M%s %s Q%s %s %s %s" % (cx - w, cy, cx, cy + w * 0.9, cx + w, cy), stroke=ink, width=width)


def ring(cx, cy, rx, ry, n, r, fill, a0=0, a1=360):
    out = ""
    for i in range(n):
        a = math.radians(a0 + (a1 - a0) * i / (n - 1 if a1 - a0 < 360 else n))
        out += circle(round(cx + rx * math.cos(a), 1), round(cy + ry * math.sin(a), 1), r, fill)
    return out


def mist_gradient(gid, top, bottom, y1, y2, mid=None):
    stops = [(0, top, 1)] + ([(0.5, mid, 1)] if mid else []) + [(1, bottom, 1)]
    return lin(gid, 128, y1, 128, y2, stops)


# ---------- Brumi ----------
def brumi_egg():
    gdef, g = glow("g", 128, 140, 116, "#A9B4FF")
    defs = gdef + mist_gradient("shell", "#F7F8FF", "#AEB8FF", 44, 224, "#DCE1FF")
    egg = "M128 40 C172 40 198 106 198 154 C198 200 168 226 128 226 C88 226 58 200 58 154 C58 106 84 40 128 40 Z"
    body = g + ellipse(128, 228, 56, 9, "#000000", 0.18)
    body += path(egg, "url(#shell)")
    body += ellipse(100, 92, 16, 28, WHITE, 0.45, rot=24)
    body += circle(102, 128, 8, GOLD, 0.9) + circle(156, 108, 6, "#8FE3D4", 0.9)
    body += circle(162, 176, 9, GOLD, 0.8) + circle(106, 190, 6, "#C7A8FF", 0.9) + circle(138, 150, 4, "#8FE3D4", 0.8)
    body += path("M60 160 C92 186 150 128 196 146", stroke=WHITE, width=7, op=0.55)
    body += path("M66 182 C100 204 150 168 186 184", stroke=WHITE, width=5, op=0.35)
    body += sparkle(196, 74, 12) + sparkle(62, 96, 8, WHITE, 0.8)
    return svg(body, defs)


def brumi_body(cx, cy, rx, ry, top, bottom, gid="mist", y_pad=0):
    return mist_gradient(gid, top, bottom, cy - ry, cy + ry + 24 + y_pad)


def brumi_sprout(closed=False):
    gdef, g = glow("g", 128, 150, 104, "#A9B4FF")
    defs = gdef + mist_gradient("mist", "#F6F7FF", "#A9B4FF", 96, 214)
    body = g + ellipse(128, 222, 46, 7, "#000000", 0.16)
    body += ring(128, 196, 40, 8, 5, 16, "url(#mist)", 0, 180)
    body += path("M128 102 C124 82 138 70 150 76 C160 82 154 94 145 90", stroke="#E7EAFF", width=7)
    body += sparkle(150, 74, 9)
    body += ellipse(128, 152, 58, 52, "url(#mist)")
    body += ellipse(106, 122, 14, 8, WHITE, 0.55, rot=-28)
    body += ellipse(128, 172, 28, 18, WHITE, 0.3)
    body += (eyes_closed(128, 150, 22, w=10) if closed else eyes_open(128, 148, 22, rx=11, ry=13))
    body += blush(128, 168, 36, 10, 6)
    body += smile(128, 168, 6)
    return svg(body, defs)


def brumi_young(closed=False):
    gdef, g = glow("g", 128, 140, 112, "#A9B4FF")
    defs = gdef + mist_gradient("mist", "#F4F5FF", "#A2AEFA", 70, 214)
    body = g + ellipse(128, 224, 58, 8, "#000000", 0.16)
    body += path("M168 186 C200 196 222 180 214 152 C210 170 196 178 178 172 Z", "#B3BDFF")
    body += circle(86, 88, 22, "url(#mist)") + circle(170, 88, 22, "url(#mist)")
    body += circle(86, 88, 10, "#DDE2FF") + circle(170, 88, 10, "#DDE2FF")
    body += ring(128, 196, 52, 8, 6, 18, "url(#mist)", 0, 180)
    body += path("M128 80 C124 58 142 48 154 56 C164 64 156 76 146 72", stroke="#E9ECFF", width=8)
    body += ellipse(128, 140, 72, 64, "url(#mist)")
    body += ellipse(98, 104, 18, 9, WHITE, 0.55, rot=-28)
    body += ellipse(128, 166, 36, 24, WHITE, 0.28)
    body += (eyes_closed(128, 138, 27) if closed else eyes_open(128, 136, 27))
    body += blush(128, 158, 44)
    body += smile(128, 158, 7)
    return svg(body, defs)


def brumi_grown(closed=False):
    gdef, g = glow("g", 128, 136, 120, "#8FA4FF")
    defs = gdef + mist_gradient("mist", "#EEF0FF", "#8E9BF2", 60, 220, "#C5CCFF")
    defs += lin("mane", 128, 40, 128, 160, [(0, "#FFFFFF", 1), (1, "#C9D0FF", 1)])
    body = g + ellipse(128, 228, 66, 9, "#000000", 0.16)
    body += path("M170 190 C214 204 240 172 226 136 C220 164 204 176 182 172 Z", "#A7B2FA")
    body += circle(226, 136, 9, "#D6DBFF")
    body += ring(128, 120, 78, 66, 9, 24, "url(#mane)", 190, 350)
    body += ring(128, 202, 60, 8, 7, 19, "url(#mist)", 0, 180)
    body += ellipse(128, 138, 80, 72, "url(#mist)")
    body += ellipse(96, 98, 20, 10, WHITE, 0.5, rot=-28)
    body += ellipse(128, 168, 42, 28, WHITE, 0.25)
    body += path("M128 74 C118 78 116 94 128 98 C122 92 122 82 128 74 Z", GOLD)
    body += (eyes_closed(128, 134, 30) if closed else eyes_open(128, 132, 30, rx=11, ry=13))
    body += blush(128, 156, 48)
    body += smile(128, 156, 8)
    body += ellipse(102, 206, 15, 10, "#E6E9FF") + ellipse(154, 206, 15, 10, "#E6E9FF")
    return svg(body, defs)


def brumi_astral(closed=False):
    gdef, g = glow("g", 128, 132, 124, "#B79CFF", 0.55)
    defs = gdef + mist_gradient("mist", "#C9BCFF", "#4E3FB8", 60, 222, "#8A78F0")
    defs += lin("wing", 128, 60, 128, 200, [(0, "#FFF4D6", 0.95), (1, "#FFD98A", 0.25)])
    body = g + ellipse(128, 228, 66, 9, "#000000", 0.2)
    for sx in (-1, 1):
        x0 = 128 + sx * 52
        body += path("M{a} 120 C{b} 74 {c} 62 {d} 80 C{e} 100 {f} 124 {a} 150 Z".format(
            a=x0, b=x0 + sx * 26, c=x0 + sx * 60, d=x0 + sx * 66, e=x0 + sx * 60, f=x0 + sx * 40), "url(#wing)")
        body += path("M{a} 150 C{b} 142 {c} 152 {d} 168 C{e} 176 {f} 172 {a} 166 Z".format(
            a=x0, b=x0 + sx * 30, c=x0 + sx * 50, d=x0 + sx * 54, e=x0 + sx * 40, f=x0 + sx * 20), "url(#wing)")
    body += path("M168 190 C210 204 238 172 224 136 C218 164 202 176 180 172 Z", "#6E5FD6")
    body += ring(128, 202, 60, 8, 7, 19, "url(#mist)", 0, 180)
    body += ellipse(128, 138, 80, 72, "url(#mist)")
    for (x, y, r) in [(92, 160, 2.5), (170, 120, 3), (150, 178, 2), (100, 112, 2), (178, 160, 2.5), (120, 186, 2)]:
        body += circle(x, y, r, WHITE, 0.8)
    body += ellipse(96, 98, 20, 10, WHITE, 0.4, rot=-28)
    body += path("M104 58 C112 40 144 40 152 58 C140 50 116 50 104 58 Z", GOLD)
    body += path("M110 62 L118 46 L128 60 L138 46 L146 62 Z", GOLD_DEEP)
    body += sparkle(128, 40, 9, "#FFF4D6")
    body += (eyes_closed(128, 134, 30, ink="#1A1440") if closed else eyes_open(128, 132, 30, rx=11, ry=13, ink="#1A1440", shine="#FFF4D6"))
    body += blush(128, 156, 48, c="#FFB3D9", op=0.45)
    body += smile(128, 156, 8, ink="#1A1440")
    for (x, y, r) in [(34, 70, 10), (222, 60, 12), (30, 190, 8), (230, 200, 9), (60, 30, 6)]:
        body += sparkle(x, y, r, "#FFF4D6", 0.9)
    return svg(body, defs)


# ---------- Créatures ----------
def plumeline():
    gdef, g = glow("g", 128, 140, 112, "#8FA4FF")
    defs = gdef + mist_gradient("fluff", "#F2F5FF", "#AEBEFF", 70, 210)
    defs += lin("feather", 0, -40, 0, 40, [(0, "#FFFFFF", 1), (1, "#BCCBFF", 1)])
    body = g + ellipse(128, 222, 54, 8, "#000000", 0.16)
    for sx in (-1, 1):
        for (dx, dy, rot) in ((50, 124, 55), (56, 144, 75), (52, 164, 100)):
            body += '<g transform="translate(%s %s) rotate(%s)">%s</g>' % (
                128 + sx * dx, dy, sx * rot, ellipse(0, -16, 12, 30, "url(#feather)"))
    for k, ang in enumerate((-25, 0, 25)):
        body += '<g transform="translate(%s %s) rotate(%s)">%s</g>' % (128 + ang * 0.8, 74 - (0 if ang else 6), ang, ellipse(0, -14, 7, 20, "url(#feather)"))
    body += ring(128, 140, 58, 54, 14, 17, "url(#fluff)")
    body += circle(128, 140, 60, "url(#fluff)")
    body += ellipse(104, 108, 16, 8, WHITE, 0.6, rot=-30)
    body += ellipse(128, 166, 30, 20, WHITE, 0.35)
    body += eyes_open(128, 134, 24, rx=10, ry=12)
    body += path("M120 150 L128 160 L136 150 Q128 146 120 150 Z", GOLD_DEEP)
    body += blush(128, 154, 40, 9, 6)
    body += sparkle(206, 84, 10, WHITE, 0.9) + sparkle(52, 196, 7, GOLD)
    return svg(body, defs)


def dormouflon():
    gdef, g = glow("g", 128, 140, 116, "#6C7BD9", 0.5)
    defs = gdef + mist_gradient("wool", "#E4E8FF", "#8E9AF0", 60, 220)
    defs += lin("face", 128, 110, 128, 190, [(0, "#4B5196", 1), (1, "#2C2F66", 1)])
    defs += lin("horn", 0, 0, 0, 40, [(0, "#FFE3A8", 1), (1, "#E7A94A", 1)])
    body = g + ellipse(128, 226, 64, 9, "#000000", 0.2)
    body += ellipse(98, 212, 10, 14, "#2C2F66") + ellipse(158, 212, 10, 14, "#2C2F66")
    for (x, y, r) in [(70, 150, 30), (186, 150, 30), (84, 110, 30), (172, 110, 30), (128, 92, 34), (96, 186, 30),
                      (160, 186, 30), (128, 196, 30), (64, 182, 22), (192, 182, 22), (106, 82, 22), (150, 82, 22)]:
        body += circle(x, y, r, "url(#wool)")
    body += circle(128, 146, 56, "url(#wool)")
    body += ellipse(104, 92, 16, 8, WHITE, 0.55, rot=-25)
    for sx in (-1, 1):
        x = 128 + sx * 44
        body += path("M{a} 128 C{b} 112 {c} 140 {d} 148 C{e} 156 {f} 140 {g} 138".format(
            a=x, b=x + sx * 34, c=x + sx * 40, d=x + sx * 22, e=x + sx * 8, f=x + sx * 6, g=x + sx * 16), stroke="#F2C97A", width=13)
    body += ellipse(128, 150, 42, 38, "url(#face)")
    body += circle(116, 112, 13, "url(#wool)") + circle(140, 112, 13, "url(#wool)") + circle(128, 106, 14, "url(#wool)")
    body += eyes_closed(128, 148, 17, w=9, ink="#E8EAFF", width=4)
    body += blush(128, 162, 24, 7, 4, op=0.55)
    body += path("M123 170 Q128 174 133 170", stroke="#E8EAFF", width=3)
    body += path("M188 58 L204 58 L188 74 L204 74", stroke="#E8EAFF", width=4, op=0.8)
    body += path("M210 34 L220 34 L210 44 L220 44", stroke="#E8EAFF", width=3, op=0.6)
    return svg(body, defs)


def tourbillot():
    gdef, g = glow("g", 128, 140, 112, "#F2A07B", 0.4)
    defs = gdef + mist_gradient("fur", "#FFE2D2", "#E5814F", 70, 210)
    body = g + ellipse(128, 222, 54, 8, "#000000", 0.18)
    body += path("M176 176 C214 180 228 140 206 122 C190 110 170 124 182 138 C190 146 200 138 196 132",
                 stroke="#E8875A", width=16)
    body += path("M118 74 C114 56 128 44 140 50 C150 56 144 68 136 64", stroke="#F6B08E", width=7)
    body += circle(136, 64, 7, "#FFD3BE")
    body += '<g transform="rotate(-8 128 142)">%s</g>' % ellipse(128, 142, 64, 62, "url(#fur)")
    body += ellipse(102, 110, 16, 8, WHITE, 0.55, rot=-35)
    body += path("M128 186 C122 186 121 179 127 178 C133 177 135 184 131 189 C125 196 116 191 117 183",
                 stroke="#FFF1E8", width=4, op=0.85)
    body += eyes_open(128, 132, 26, rx=10, ry=12, ink="#3A1E14")
    body += path("M102 114 L116 120", stroke="#3A1E14", width=4) + path("M154 114 L140 120", stroke="#3A1E14", width=4)
    body += path("M112 150 Q128 168 144 150 Q128 156 112 150 Z", "#3A1E14")
    body += ellipse(128, 157, 6, 3, "#FF8FA8")
    body += blush(128, 150, 42, 9, 6, op=0.45)
    body += path("M44 120 Q36 140 46 160", stroke=WHITE, width=4, op=0.5)
    body += path("M30 132 Q26 142 32 152", stroke=WHITE, width=3, op=0.35)
    body += path("M210 72 Q222 84 218 100", stroke=WHITE, width=4, op=0.5)
    return svg(body, defs)


def givrelin():
    gdef, g = glow("g", 128, 140, 116, "#9FD8FF", 0.5)
    defs = gdef + mist_gradient("ice", "#FFFFFF", "#8CCBF5", 70, 212, "#D8F1FF")
    defs += lin("crystal", 0, -34, 0, 0, [(0, "#FFFFFF", 1), (1, "#9FD8FF", 1)])
    body = g + ellipse(128, 222, 52, 8, "#000000", 0.16)
    for ang, l in ((-50, 24), (-25, 30), (0, 38), (25, 30), (50, 24)):
        body += '<g transform="translate(128 96) rotate(%s)">%s</g>' % (
            ang, path("M0 %s L8 -6 L0 4 L-8 -6 Z" % (-l - 8), "url(#crystal)"))
    body += ellipse(92, 186, 14, 10, "#CDEBFF") + ellipse(164, 186, 14, 10, "#CDEBFF")
    body += path("M128 84 C178 84 196 124 192 158 C188 194 160 208 128 208 C96 208 68 194 64 158 C60 124 78 84 128 84 Z", "url(#ice)")
    body += ellipse(100, 112, 16, 8, WHITE, 0.7, rot=-30)
    for a in (0, 60, 120):
        body += '<g transform="rotate(%s 128 176)">%s</g>' % (a, path("M128 162 L128 190 M122 166 L128 171 L134 166 M122 186 L128 181 L134 186", stroke=WHITE, width=3))
    body += eyes_open(128, 140, 24, rx=10, ry=12, ink="#1F3A5C")
    body += blush(128, 158, 40, 9, 6, c="#FFB8D2", op=0.5)
    body += smile(128, 156, 6, ink="#1F3A5C")
    body += sparkle(52, 90, 10, WHITE) + sparkle(206, 120, 8, WHITE) + sparkle(196, 196, 6, "#CDEBFF")
    body += circle(60, 160, 3, WHITE, 0.9) + circle(214, 70, 3, WHITE, 0.9) + circle(40, 200, 2, WHITE, 0.8)
    return svg(body, defs)


ART = {
    "art_brumi_egg": brumi_egg(),
    "art_brumi_sprout": brumi_sprout(),
    "art_brumi_sprout_sleep": brumi_sprout(closed=True),
    "art_brumi_young": brumi_young(),
    "art_brumi_young_sleep": brumi_young(closed=True),
    "art_brumi_grown": brumi_grown(),
    "art_brumi_grown_sleep": brumi_grown(closed=True),
    "art_brumi_astral": brumi_astral(),
    "art_brumi_astral_sleep": brumi_astral(closed=True),
    "art_plumeline": plumeline(),
    "art_dormouflon": dormouflon(),
    "art_tourbillot": tourbillot(),
    "art_givrelin": givrelin(),
}

if __name__ == "__main__":
    svg_dir = os.path.join(ROOT, "art", "svg")
    res_dir = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
    os.makedirs(svg_dir, exist_ok=True)
    cards = []
    for name, s in ART.items():
        open(os.path.join(svg_dir, name + ".svg"), "w", encoding="utf-8").write(s)
        open(os.path.join(res_dir, name + ".xml"), "w", encoding="utf-8").write(convert(s))
        cards.append('<figure><img src="svg/%s.svg"><figcaption>%s</figcaption></figure>' % (name, name[4:]))
    open(os.path.join(ROOT, "art", "preview.html"), "w", encoding="utf-8").write(
        "<!doctype html><meta charset=utf-8><style>body{background:#0B1026;color:#AEB4D6;font:13px sans-serif;"
        "display:flex;flex-wrap:wrap;gap:12px;padding:16px;margin:0}figure{margin:0;background:#141A36;"
        "border-radius:20px;padding:8px;text-align:center}img{width:200px;height:200px;display:block}</style>"
        + "".join(cards))
    print("ok", len(ART))
