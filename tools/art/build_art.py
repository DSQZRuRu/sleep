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
def svg(body, defs="", w=256, h=256):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %s %s"><defs>%s</defs>%s</svg>' % (w, h, defs, body))


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


# ---------- Créatures (suite) ----------
def face(cx, ey, dx, ink=INK, closed=False, rx=10, ry=12, blush_c=BLUSH, mouth=True, shine=WHITE):
    out = eyes_closed(cx, ey, dx, ink=ink) if closed else eyes_open(cx, ey, dx, rx, ry, ink=ink, shine=shine)
    out += blush(cx, ey + 18, dx + 15, 9, 6, c=blush_c, op=0.45)
    if mouth:
        out += smile(cx, ey + 18, 6, ink=ink)
    return out


def base(glow_c, a=0.45, shadow_rx=52):
    gdef, g = glow("g", 128, 140, 114, glow_c, a)
    return gdef, g + ellipse(128, 222, shadow_rx, 8, "#000000", 0.17)


def frissonnette():
    gd, b = base("#6CC5CF")
    defs = gd + mist_gradient("m", "#F2FCFD", "#7FCFD9", 64, 214)
    b += path("M118 70 C116 54 128 46 136 52", stroke="#BDEFF3", width=5) + circle(137, 52, 6, "#FFFFFF")
    b += path("M64 150 C64 96 92 66 128 66 C164 66 192 96 192 150 L192 196 Q180 212 168 196 Q156 180 144 196 "
              "Q132 212 120 196 Q108 180 96 196 Q84 212 72 196 Q64 186 64 176 Z", "url(#m)")
    b += ellipse(100, 98, 16, 8, WHITE, 0.6, rot=-30)
    b += path("M80 168 Q100 158 120 168 T160 168", stroke=WHITE, width=4, op=0.5)
    b += face(128, 128, 24, ink="#1F4A52")
    b += sparkle(206, 88, 8, WHITE) + sparkle(48, 120, 6, "#BDEFF3")
    return svg(b, defs)


def veilleuse():
    gd, b = base("#FFC94D", 0.5)
    defs = gd + mist_gradient("m", "#FFF7DD", "#F5C65A", 70, 200)
    defs += rad("lamp", 128, 186, 44, [(0, "#FFFBE0", 1), (0.5, "#FFE48A", 1), (1, "#FFC94D", 1)])
    defs += lin("w", 0, -30, 0, 30, [(0, "#FFFFFF", 0.9), (1, "#FFF1C2", 0.35)])
    for sx in (-1, 1):
        b += '<g transform="translate(%s 104) rotate(%s)">%s</g>' % (128 + sx * 46, sx * 40, ellipse(0, -14, 22, 34, "url(#w)"))
    for sx in (-1, 1):
        b += path("M%s 88 C%s 64 %s 52 %s 48" % (128 + sx * 14, 128 + sx * 18, 128 + sx * 28, 128 + sx * 36), stroke="#5A4A2A", width=4)
        b += circle(128 + sx * 36, 48, 8, "#FFF4B8") + circle(128 + sx * 36, 48, 14, "#FFE48A", 0.35)
    b += circle(128, 186, 40, "#FFE48A", 0.35) + circle(128, 186, 32, "url(#lamp)")
    b += ellipse(128, 130, 50, 46, "url(#m)")
    b += ellipse(106, 106, 14, 7, WHITE, 0.6, rot=-30)
    b += face(128, 128, 20, ink="#4A3A1A", closed=True)
    return svg(b, defs)


def brumelot():
    gd, b = base("#9DB4CC", 0.4, 46)
    defs = gd + lin("m", 40, 120, 220, 160, [(0, "#C8D6E5", 1), (1, "#F4F7FB", 1)])
    b += path("M64 170 C34 176 18 150 34 134 C44 124 60 132 54 144", stroke="#C8D6E5", width=14)
    b += circle(96, 92, 16, "#DDE6F0") + circle(160, 92, 16, "#DDE6F0")
    b += path("M60 150 C60 104 92 84 132 84 C176 84 204 110 200 148 C196 186 164 200 128 200 C92 200 60 188 60 150 Z", "url(#m)")
    b += ellipse(150, 104, 18, 8, WHITE, 0.6, rot=-15)
    b += path("M84 182 Q112 192 140 184", stroke=WHITE, width=5, op=0.5)
    b += path("M106 136 Q116 130 126 136", stroke=INK, width=5) + path("M146 136 Q156 130 166 136", stroke=INK, width=5)
    b += ellipse(116, 140, 7, 5, INK) + ellipse(156, 140, 7, 5, INK)
    b += blush(136, 158, 34, 9, 6, op=0.4) + smile(136, 158, 6)
    return svg(b, defs)


def songelune():
    gd, b = base("#B38CFF", 0.5)
    defs = gd + mist_gradient("m", "#FAF6FF", "#B9A0FF", 80, 214)
    defs += lin("w", 0, -30, 0, 30, [(0, "#FFFFFF", 0.9), (1, "#D7C4FF", 0.4)])
    for sx in (-1, 1):
        b += '<g transform="translate(%s 140) rotate(%s)">%s</g>' % (128 + sx * 62, sx * 60, ellipse(0, -20, 18, 34, "url(#w)"))
    b += ellipse(128, 150, 62, 58, "url(#m)")
    b += path("M92 92 C96 52 160 52 164 92 C150 70 106 70 92 92 Z", GOLD)
    b += path("M150 46 C134 46 126 64 136 76 C126 74 120 60 130 48 C136 44 144 44 150 46 Z", GOLD_DEEP)
    b += ellipse(104, 120, 15, 8, WHITE, 0.55, rot=-30)
    b += face(128, 150, 24, closed=True)
    for (x, y, r) in [(50, 70, 9), (210, 60, 11), (220, 190, 7), (40, 196, 6)]:
        b += sparkle(x, y, r, GOLD if r > 8 else WHITE)
    return svg(b, defs)


def abyssin():
    gd, b = base("#4F7BD0", 0.5, 56)
    defs = gd + lin("m", 128, 84, 128, 200, [(0, "#6F8FD0", 1), (1, "#2A4580", 1)])
    b += path("M200 140 C222 112 240 112 238 126 C236 138 226 142 214 142 C226 146 236 152 236 162 C236 176 218 170 200 146 Z", "#4E6FB4")
    b += path("M118 92 C126 66 150 64 158 88 Z", "#5C7EC4")
    b += ellipse(128, 142, 80, 58, "url(#m)")
    b += path("M60 160 C90 196 166 196 196 160 C170 176 90 176 60 160 Z", "#A9C2F2", op=0.85)
    b += ellipse(96, 110, 18, 8, WHITE, 0.4, rot=-20)
    b += face(116, 134, 26, ink="#0F1D3D", shine="#DDE8FF")
    for (x, y, r) in [(54, 92, 7), (40, 70, 5), (60, 52, 4), (204, 76, 6)]:
        b += circle(x, y, r, "#BFD4FF", 0.25) + '<circle cx="%s" cy="%s" r="%s" fill="none" stroke="#DDE8FF" stroke-width="2" opacity="0.7"/>' % (x, y, r)
    return svg(b, defs)


def mousselin():
    gd, b = base("#4E8F7A", 0.5)
    defs = gd + mist_gradient("m", "#BFE6D6", "#3E7A66", 78, 214)
    b += circle(84, 102, 18, "#5FA38C") + circle(172, 102, 18, "#5FA38C") + circle(84, 102, 8, "#A8DCC9") + circle(172, 102, 8, "#A8DCC9")
    b += ring(128, 146, 64, 58, 16, 14, "url(#m)")
    b += circle(128, 146, 64, "url(#m)")
    for (x, y, rot) in [(112, 82, -30), (132, 78, 10), (150, 86, 40)]:
        b += ellipse(x, y, 8, 16, "#7FD08A", rot=rot)
    b += path("M150 82 C156 70 168 70 170 80 C164 78 158 80 150 82 Z", "#F2A6C8")
    b += circle(92, 176, 7, "#E8F5EF", 0.7) + circle(168, 168, 5, "#E8F5EF", 0.7) + circle(150, 196, 6, "#E8F5EF", 0.6)
    b += ellipse(104, 116, 16, 8, WHITE, 0.45, rot=-30)
    b += face(128, 142, 24, ink="#1E3A30", closed=True)
    return svg(b, defs)


def lentorne():
    gd, b = base("#7B5BD0", 0.5)
    defs = gd + mist_gradient("m", "#CBB6FA", "#4A2F8C", 70, 214)
    b += path("M172 182 C206 196 222 170 212 150", stroke="#5B3F9E", width=12) + circle(212, 148, 10, "#C0A8F5")
    for sx in (-1, 1):
        x = 128 + sx * 30
        b += path("M%s 92 C%s 60 %s 40 %s 44 C%s 56 %s 64 %s 70" % (x, x + sx * 6, x + sx * 30, x + sx * 40, x + sx * 30, x + sx * 26, x + sx * 34),
                  stroke="#E9DDB8", width=9)
        b += ellipse(128 + sx * 58, 112, 16, 9, "#8C6CD6", rot=sx * 30)
    b += ellipse(128, 146, 64, 60, "url(#m)")
    b += circle(92, 168, 7, "#E6DBFF", 0.6) + circle(166, 160, 6, "#E6DBFF", 0.6) + circle(150, 190, 5, "#E6DBFF", 0.5)
    b += ellipse(104, 118, 15, 7, WHITE, 0.45, rot=-30)
    b += face(128, 140, 24, ink="#1E1340", closed=True)
    return svg(b, defs)


def onirion():
    gd, b = base("#7B8CFF", 0.6, 64)
    defs = gd + mist_gradient("m", "#4B5FB8", "#141E4D", 60, 222)
    defs += lin("w", 128, 50, 128, 190, [(0, "#FFF4D6", 0.95), (1, "#7B8CFF", 0.3)])
    for sx in (-1, 1):
        x0 = 128 + sx * 50
        b += path("M{a} 130 C{b} 70 {c} 46 {d} 56 C{e} 80 {f} 110 {a} 160 Z".format(
            a=x0, b=x0 + sx * 30, c=x0 + sx * 64, d=x0 + sx * 72, e=x0 + sx * 66, f=x0 + sx * 44), "url(#w)")
    b += path("M170 194 C214 206 236 176 222 146", stroke="#2A3A80", width=14) + sparkle(222, 142, 10, GOLD)
    b += ellipse(128, 142, 74, 70, "url(#m)")
    for (x, y, r) in [(96, 160, 2.5), (164, 118, 3), (150, 182, 2), (104, 116, 2), (176, 158, 2.5), (120, 194, 2), (140, 100, 2)]:
        b += circle(x, y, r, "#FFF4D6", 0.85)
    b += path("M96 76 L106 52 L118 70 L128 44 L138 70 L150 52 L160 76 Z", GOLD)
    b += circle(128, 62, 5, "#8FE3D4")
    b += ellipse(98, 108, 18, 8, WHITE, 0.3, rot=-28)
    b += face(128, 140, 28, ink="#0A0F2A", shine="#FFE6A8", blush_c="#B79CFF")
    b += path("M118 146 C122 150 134 150 138 146", stroke=GOLD, width=3, op=0.6)
    for (x, y, r) in [(34, 40, 10), (226, 34, 12), (30, 200, 7), (232, 210, 8)]:
        b += sparkle(x, y, r, "#FFF4D6")
    return svg(b, defs)


def grignoton():
    gd, b = base("#D99A6C", 0.4)
    defs = gd + mist_gradient("m", "#FBE3CE", "#C47E4B", 74, 214)
    for sx in (-1, 1):
        b += circle(128 + sx * 52, 86, 22, "#C9895A") + circle(128 + sx * 52, 86, 12, "#F7C9C2")
    b += ellipse(128, 148, 70, 64, "url(#m)")
    b += ellipse(128, 172, 40, 30, "#FFF4EA", 0.8)
    b += ellipse(92, 160, 18, 14, "#F6CBB0") + ellipse(164, 160, 18, 14, "#F6CBB0")
    b += circle(118, 120, 7, "#F2D7C0", 0.6) + circle(150, 110, 5, "#F2D7C0", 0.6)
    b += face(128, 136, 24, ink="#3A2414", mouth=False)
    b += ellipse(128, 150, 6, 4, "#8A4A3A")
    b += path("M122 156 Q128 162 134 156", stroke="#8A4A3A", width=3)
    b += '<g transform="rotate(-15 128 196)">%s</g>' % (ellipse(128, 196, 16, 12, "#8FE3D4") + sparkle(128, 196, 8, WHITE))
    b += ellipse(104, 194, 9, 7, "#E8B48F") + ellipse(152, 194, 9, 7, "#E8B48F")
    return svg(b, defs)


def cliquetis():
    gd, b = base("#E8C468", 0.45)
    defs = gd + mist_gradient("m", "#FFF4D2", "#D9A93E", 74, 214)
    b += path("M128 78 C124 58 140 48 150 56 C158 62 152 72 144 68", stroke="#C99A2E", width=6)
    for sx in (-1, 1):
        b += path("M%s 98 L%s 70 L%s 96 Z" % (128 + sx * 30, 128 + sx * 44, 128 + sx * 52), "#C99A2E")
    b += ellipse(128, 146, 66, 62, "url(#m)")
    b += circle(128, 176, 18, "#FFFFFF", 0.35) + '<circle cx="128" cy="176" r="18" fill="none" stroke="#C99A2E" stroke-width="3"/>'
    b += path("M128 176 L128 164 M128 176 L137 180", stroke="#8A6A1E", width=3)
    b += ellipse(104, 116, 15, 8, WHITE, 0.6, rot=-30)
    b += face(128, 136, 24, ink="#3A2C0A")
    for (x1, y1, x2, y2) in [(46, 150, 30, 146), (50, 170, 34, 176), (210, 150, 226, 146), (206, 170, 222, 176)]:
        b += path("M%s %s L%s %s" % (x1, y1, x2, y2), stroke="#FFE7A0", width=4, op=0.7)
    return svg(b, defs)


def orageon():
    gd, b = base("#7D8AA6", 0.45)
    defs = gd + mist_gradient("m", "#E3E8F2", "#6A7794", 60, 200)
    b += path("M150 180 L134 208 L148 208 L136 236 L168 198 L152 198 L164 180 Z", "#FFD24A")
    for (x, y, r) in [(80, 136, 34), (176, 136, 34), (104, 102, 34), (156, 100, 30), (128, 92, 30), (128, 150, 46),
                      (70, 164, 24), (186, 164, 24), (100, 172, 28), (156, 172, 28)]:
        b += circle(x, y, r, "url(#m)")
    b += ellipse(100, 92, 16, 8, WHITE, 0.6, rot=-25)
    b += path("M100 118 L118 126", stroke="#2A3048", width=5) + path("M156 118 L138 126", stroke="#2A3048", width=5)
    b += face(128, 138, 22, ink="#2A3048", mouth=False)
    b += path("M118 162 Q128 154 138 162", stroke="#2A3048", width=4)
    for (x, y) in [(70, 206), (96, 220), (186, 212)]:
        b += path("M%s %s C%s %s %s %s %s %s Z" % (x, y - 12, x - 7, y, x + 7, y, x, y - 12), "#9FD8FF")
        b += circle(x, y - 1, 6, "#9FD8FF")
    return svg(b, defs)


def flocondor():
    gd, b = base("#5E8FD0", 0.5, 64)
    defs = gd + mist_gradient("m", "#F2F8FF", "#7FA8DD", 80, 214)
    b += path("M60 178 C40 150 60 112 92 118 C70 128 66 156 84 170", stroke="#B9D4F2", width=18)
    b += circle(176, 102, 18, "#A9C6EC") + circle(176, 102, 8, "#E6F0FF")
    b += ellipse(132, 158, 76, 56, "url(#m)")
    b += path("M64 176 C90 206 176 210 204 174 C190 196 90 198 64 176 Z", "#E6F0FF")
    b += path("M90 112 C100 90 160 86 176 110 C150 98 112 100 90 112 Z", WHITE)
    b += circle(110, 160, 6, "#E6F0FF", 0.7) + circle(164, 150, 5, "#E6F0FF", 0.7)
    b += ellipse(104, 132, 16, 7, WHITE, 0.6, rot=-20)
    b += face(140, 150, 22, ink="#1F3A5C", closed=True, blush_c="#FFB8D2")
    for a in (0, 60, 120):
        b += '<g transform="rotate(%s 214 66)">%s</g>' % (a, path("M214 52 L214 80", stroke=WHITE, width=3))
    for a in (0, 60, 120):
        b += '<g transform="rotate(%s 42 74)">%s</g>' % (a, path("M42 64 L42 84", stroke="#CDEBFF", width=3))
    return svg(b, defs)


def lanternelle():
    gd, b = base("#FF9F3D", 0.6)
    defs = gd + rad("m", 128, 150, 70, [(0, "#FFF7DD", 1), (0.6, "#FFC46B", 1), (1, "#F08A2E", 1)])
    defs += lin("w", 0, -30, 0, 30, [(0, "#FFFFFF", 0.9), (1, "#FFE0A8", 0.3)])
    for sx in (-1, 1):
        b += '<g transform="translate(%s 128) rotate(%s)">%s</g>' % (128 + sx * 60, sx * 55, ellipse(0, -18, 20, 36, "url(#w)"))
    b += path("M104 82 L152 82 L148 94 L108 94 Z", "#B5541A")
    b += path("M100 206 L156 206 L150 216 L106 216 Z", "#B5541A")
    b += ellipse(128, 150, 62, 58, "url(#m)")
    for x in (98, 128, 158):
        b += path("M%s 96 Q%s 150 %s 204" % (x, x + (x - 128) * 0.35, x), stroke="#E07A20", width=3, op=0.5)
    b += path("M106 60 L116 44 L128 58 L140 44 L150 60 Z", GOLD) + sparkle(128, 36, 8, "#FFF4D6")
    b += face(128, 146, 22, ink="#5A2A08", shine="#FFFBE6")
    for (x, y, r) in [(40, 80, 9), (218, 70, 10), (44, 196, 6), (214, 200, 7)]:
        b += sparkle(x, y, r, "#FFE7A8")
    return svg(b, defs)


def citrouillon():
    gd, b = base("#E0701A", 0.5)
    defs = gd + mist_gradient("m", "#FFC88A", "#E06A12", 80, 214)
    b += path("M128 86 C126 70 132 60 140 56", stroke="#5E7A3A", width=8)
    b += path("M138 64 C150 52 168 56 170 66 C158 70 146 70 138 64 Z", "#7FBF5A")
    for (x, rx) in [(84, 34), (172, 34), (106, 36), (150, 36), (128, 40)]:
        b += ellipse(x, 150, rx, 60, "url(#m)")
    for x in (106, 150):
        b += path("M%s 96 Q%s 150 %s 206" % (x, x + (x - 128) * 0.3, x), stroke="#C9580E", width=3, op=0.6)
    b += ellipse(106, 116, 14, 7, WHITE, 0.5, rot=-30)
    b += face(128, 142, 26, ink="#3A1A06", mouth=False)
    b += path("M110 164 Q119 172 128 164 Q137 172 146 164", stroke="#3A1A06", width=4)
    b += sparkle(212, 92, 8, GOLD) + sparkle(46, 104, 6, GOLD)
    return svg(b, defs)


def brumoire():
    gd, b = base("#7E5FC4", 0.6)
    defs = gd + mist_gradient("m", "#8E78C8", "#2E2450", 70, 214)
    defs += lin("w", 128, 60, 128, 180, [(0, "#B7A4E0", 0.8), (1, "#4A3B6B", 0.2)])
    for sx in (-1, 1):
        x0 = 128 + sx * 48
        b += path("M{a} 120 L{b} 70 L{c} 100 L{d} 86 L{e} 122 L{f} 116 L{a} 150 Z".format(
            a=x0, b=x0 + sx * 50, c=x0 + sx * 56, d=x0 + sx * 76, e=x0 + sx * 66, f=x0 + sx * 76), "url(#w)")
        b += path("M%s 90 C%s 70 %s 58 %s 56" % (128 + sx * 24, 128 + sx * 26, 128 + sx * 36, 128 + sx * 44), stroke="#B7A4E0", width=9)
    b += path("M66 150 C66 102 94 80 128 80 C162 80 190 102 190 150 L190 200 Q178 186 166 200 Q154 214 142 200 "
              "Q128 186 114 200 Q102 214 90 200 Q78 186 66 200 Z", "url(#m)")
    b += ellipse(104, 110, 14, 7, WHITE, 0.3, rot=-30)
    b += face(128, 140, 24, ink="#E9E0FF", shine="#2E2450", blush_c="#B38CFF")
    b += sparkle(46, 60, 7, "#B7A4E0") + sparkle(212, 196, 6, "#B7A4E0")
    return svg(b, defs)


def bourgeonnet():
    gd, b = base("#6DBF57", 0.45)
    defs = gd + mist_gradient("m", "#EFFBE8", "#7CC765", 82, 214)
    b += path("M128 92 C128 76 128 66 128 58", stroke="#4E9A3E", width=6)
    b += path("M128 72 C108 56 90 62 88 74 C104 80 118 80 128 72 Z", "#7FD08A")
    b += path("M128 64 C146 46 166 50 170 62 C154 70 140 70 128 64 Z", "#6DBF57")
    b += ellipse(128, 52, 9, 12, "#F7B9D3")
    b += ellipse(128, 150, 62, 58, "url(#m)")
    b += ellipse(92, 200, 13, 9, "#A8DC8F") + ellipse(164, 200, 13, 9, "#A8DC8F")
    b += ellipse(104, 120, 15, 8, WHITE, 0.6, rot=-30)
    b += face(128, 146, 22, ink="#1E3A14")
    b += sparkle(206, 80, 8, "#FFF4B8") + sparkle(50, 116, 6, WHITE)
    return svg(b, defs)


def petalune():
    gd, b = base("#DB78A8", 0.45)
    defs = gd + mist_gradient("m", "#FFF0F7", "#E59AC0", 92, 206)
    defs += lin("p", 0, -60, 0, 0, [(0, "#FFE3F0", 1), (1, "#F2A6C8", 1)])
    for a in range(0, 360, 45):
        b += '<g transform="translate(128 150) rotate(%s)">%s</g>' % (a, ellipse(0, -52, 24, 34, "url(#p)"))
    b += circle(128, 150, 52, "url(#m)")
    b += path("M108 98 L116 82 L128 94 L140 82 L148 98 Z", GOLD)
    b += ellipse(110, 128, 13, 7, WHITE, 0.6, rot=-30)
    b += face(128, 148, 20, ink="#4A1A30", closed=True, blush_c="#FF7FAF")
    return svg(b, defs)


def solsticelle():
    gd, b = base("#FFC02E", 0.55)
    defs = gd + mist_gradient("m", "#FFFBE6", "#FFC94A", 84, 210)
    defs += lin("w", 0, -30, 0, 30, [(0, "#FFFFFF", 0.9), (1, "#FFE9A0", 0.35)])
    for a in range(0, 360, 30):
        b += '<g transform="rotate(%s 128 148)">%s</g>' % (a, path("M120 74 L128 58 L136 74 Z", "#FFD24A"))
    for sx in (-1, 1):
        b += '<g transform="translate(%s 134) rotate(%s)">%s</g>' % (128 + sx * 66, sx * 60, ellipse(0, -16, 16, 30, "url(#w)"))
    b += circle(128, 148, 66, "#FFE07A", 0.5) + circle(128, 148, 58, "url(#m)")
    b += path("M118 82 C114 70 124 62 132 66", stroke="#F2B84B", width=5)
    b += ellipse(106, 122, 14, 7, WHITE, 0.65, rot=-30)
    b += face(128, 144, 22, ink="#5A3A06")
    return svg(b, defs)


def meduzephyr():
    gd, b = base("#45B5C4", 0.5)
    defs = gd + lin("m", 128, 70, 128, 160, [(0, "#E8FBFD", 0.95), (1, "#7FD6E0", 0.85)])
    for i, x in enumerate((92, 112, 132, 152, 168)):
        w = -10 if i % 2 else 10
        b += path("M%s 150 C%s 176 %s 188 %s 214" % (x, x + w, x - w, x + w * 0.6), stroke="#9FE3EA", width=6, op=0.8)
    b += path("M60 150 C60 98 92 70 128 70 C164 70 196 98 196 150 Q180 162 162 152 Q146 164 128 152 Q110 164 94 152 Q76 162 60 150 Z", "url(#m)")
    b += path("M76 130 C92 112 164 112 180 130", stroke=WHITE, width=4, op=0.5)
    b += ellipse(100, 96, 16, 8, WHITE, 0.7, rot=-30)
    b += face(128, 120, 22, ink="#0F3A42", blush_c="#FFB3C7")
    for (x, y, r) in [(46, 70, 6), (212, 90, 7), (204, 200, 5)]:
        b += '<circle cx="%s" cy="%s" r="%s" fill="none" stroke="#C8F2F6" stroke-width="2" opacity="0.8"/>' % (x, y, r)
    return svg(b, defs)


# ---------- Décor : la chambre (360 × 240) ----------
def room():
    defs = lin("wall", 180, 0, 180, 200, [(0, "#141A45", 1), (1, "#2B3070", 1)])
    defs += lin("sky", 265, 30, 265, 130, [(0, "#070B22", 1), (1, "#28307A", 1)])
    defs += lin("floor", 180, 190, 180, 240, [(0, "#1A1F4D", 1), (1, "#0E1230", 1)])
    defs += lin("wood", 145, 104, 145, 200, [(0, "#6E5BC0", 1), (1, "#3D3285", 1)])
    defs += lin("quilt", 145, 160, 145, 224, [(0, "#9AA6F7", 1), (1, "#5A66CF", 1)])
    defs += lin("curtain", 0, 20, 0, 150, [(0, "#B9A8FF", 1), (1, "#7B68D8", 1)])
    defs += rad("lampglow", 52, 92, 60, [(0, "#FFD98A", 0.55), (0.5, "#FFD98A", 0.15), (1, "#FFD98A", 0)])
    defs += rad("moonglow", 286, 60, 40, [(0, "#FFF4D6", 0.5), (1, "#FFF4D6", 0)])
    b = path("M0 0 H360 V240 H0 Z", "url(#wall)")
    for (x, y) in [(20, 30), (70, 18), (120, 40), (160, 14), (30, 150), (190, 60), (180, 150), (340, 160), (110, 90), (345, 20)]:
        b += sparkle(x, y, 3, "#3E4690")
    b += path("M0 186 H360 V240 H0 Z", "url(#floor)")
    b += ellipse(170, 226, 150, 12, "#3A3F8C", 0.55)
    # fenêtre
    b += path("M208 134 V76 A57 57 0 0 1 322 76 V134 Z", "#3A3F7A")
    b += path("M216 128 V78 A49 49 0 0 1 314 78 V128 Z", "url(#sky)")
    b += circle(286, 60, 40, "url(#moonglow)")
    b += path("M294 44 C278 42 266 56 268 72 C270 86 284 94 298 90 C286 86 278 76 280 64 C282 54 288 48 294 44 Z", "#FFE6A8")
    for (x, y, r) in [(236, 66, 3.5), (252, 96, 2.5), (300, 112, 3), (232, 110, 2), (262, 50, 2)]:
        b += sparkle(x, y, r * 1.6, WHITE)
    b += path("M265 26 V128 M216 92 H314", stroke="#3A3F7A", width=5)
    b += path("M204 136 H326", stroke="#4A4F90", width=8)
    # rideaux
    b += path("M196 14 C206 60 200 110 214 150 L190 150 C184 104 184 50 186 14 Z", "url(#curtain)")
    b += path("M334 14 C324 60 330 110 316 150 L340 150 C346 104 346 50 344 14 Z", "url(#curtain)")
    b += path("M180 12 H350", stroke="#C9B9FF", width=5)
    # étagère, plante et veilleuse
    b += circle(52, 92, 60, "url(#lampglow)")
    b += path("M18 112 H108", stroke="#4A3C90", width=6)
    b += path("M40 112 L44 94 H60 L64 112 Z", "#FFD98A") + path("M50 94 V84", stroke="#E7A94A", width=3)
    b += path("M80 112 L84 98 H100 L104 112 Z", "#5C6BC4")
    for (x, y, rot) in [(86, 92, -30), (92, 88, 0), (98, 92, 30)]:
        b += ellipse(x, y, 4, 9, "#7FD08A", rot=rot)
    # lit
    b += path("M40 204 V128 Q40 108 60 108 H230 Q250 108 250 128 V204 Z", "url(#wood)")
    b += path("M145 120 C130 120 122 134 128 146 C116 142 112 126 124 118 C132 112 140 114 145 120 Z", "#FFD98A", op=0.8)
    b += path("M52 196 V130 Q52 120 62 120 H228 Q238 120 238 130 V196 Z", "#000000", op=0.12)
    b += ellipse(92, 160, 44, 18, "#EEF0FF") + ellipse(84, 154, 22, 6, WHITE, 0.7)
    b += path("M28 198 Q28 168 58 166 H232 Q262 168 262 198 V214 Q262 224 252 224 H38 Q28 224 28 214 Z", "url(#quilt)")
    b += path("M28 198 Q28 168 58 166 H130 Q100 196 112 224 H38 Q28 224 28 214 Z", "#B4BEFF", op=0.35)
    for (x, y) in [(150, 186), (190, 200), (230, 184), (176, 214), (70, 206)]:
        b += path("M%s %s C%s %s %s %s %s %s C%s %s %s %s %s %s Z" % (x + 4, y - 5, x - 4, y - 6, x - 6, y + 4, x + 2, y + 6,
                                                                       x - 2, y + 2, x - 2, y - 2, x + 4, y - 5), "#FFE6A8", op=0.7)
    b += path("M28 200 Q145 190 262 200", stroke=WHITE, width=2, op=0.3)
    b += path("M44 224 V236 M246 224 V236", stroke="#2E2770", width=8)
    return svg(b, defs, 360, 240)


# ---------- Chambre modulaire (calques 360 × 240 superposés dans l'app) ----------
ROOM_DEFS = (lin("wall", 180, 0, 180, 200, [(0, "#141A45", 1), (1, "#2B3070", 1)])
             + lin("sky", 265, 30, 265, 130, [(0, "#070B22", 1), (1, "#28307A", 1)])
             + lin("floor", 180, 190, 180, 240, [(0, "#1A1F4D", 1), (1, "#0E1230", 1)])
             + lin("wood", 145, 104, 145, 200, [(0, "#6E5BC0", 1), (1, "#3D3285", 1)]))
QUILT_SHAPE = "M28 198 Q28 168 58 166 H232 Q262 168 262 198 V214 Q262 224 252 224 H38 Q28 224 28 214 Z"
QUILT_FOLD = "M28 198 Q28 168 58 166 H130 Q100 196 112 224 H38 Q28 224 28 214 Z"


def room_back():
    b = path("M0 0 H360 V240 H0 Z", "url(#wall)")
    for (x, y) in [(20, 30), (70, 18), (120, 40), (160, 14), (30, 150), (190, 60), (180, 150), (340, 160), (110, 90), (345, 20)]:
        b += sparkle(x, y, 3, "#3E4690")
    b += path("M216 128 V78 A49 49 0 0 1 314 78 V128 Z", "url(#sky)")
    for (x, y, r) in [(236, 66, 3.5), (252, 96, 2.5), (300, 112, 3), (232, 110, 2), (262, 50, 2)]:
        b += sparkle(x, y, r * 1.6, WHITE)
    return svg(b, ROOM_DEFS, 360, 240)


def room_window():
    b = path("M208 134 V76 A57 57 0 0 1 322 76 V134 Z M216 128 V78 A49 49 0 0 1 314 78 V128 Z", "#3A3F7A")
    b = b.replace('<path ', '<path fill-rule="evenodd" ', 1)
    b += path("M265 30 V128 M216 92 H314", stroke="#3A3F7A", width=5)
    b += path("M204 136 H326", stroke="#4A4F90", width=8)
    b += path("M180 12 H350", stroke="#C9B9FF", width=5)
    b += path("M0 186 H360 V240 H0 Z", "url(#floor)")
    b += path("M18 112 H108", stroke="#4A3C90", width=6)
    return svg(b, ROOM_DEFS, 360, 240)


def room_bed():
    b = ellipse(150, 230, 120, 8, "#000000", 0.25)
    b += path("M40 204 V128 Q40 108 60 108 H230 Q250 108 250 128 V204 Z", "url(#wood)")
    b += path("M145 120 C130 120 122 134 128 146 C116 142 112 126 124 118 C132 112 140 114 145 120 Z", "#FFD98A", op=0.8)
    b += path("M52 196 V130 Q52 120 62 120 H228 Q238 120 238 130 V196 Z", "#000000", op=0.12)
    b += ellipse(92, 160, 44, 18, "#EEF0FF") + ellipse(84, 154, 22, 6, WHITE, 0.7)
    b += path("M44 222 V236 M246 222 V236", stroke="#2E2770", width=8)
    return svg(b, ROOM_DEFS, 360, 240)


def layer(body, defs=""):
    return svg(body, defs, 360, 240)


def quilt(top, bottom, fold, motif):
    defs = lin("q", 145, 160, 145, 224, [(0, top, 1), (1, bottom, 1)])
    b = path(QUILT_SHAPE, "url(#q)") + path(QUILT_FOLD, fold, op=0.35) + motif
    b += path("M28 200 Q145 190 262 200", stroke=WHITE, width=2, op=0.3)
    return layer(b, defs)


def moon_motif(x, y, c="#FFE6A8", op=0.7):
    return path("M%s %s C%s %s %s %s %s %s C%s %s %s %s %s %s Z" % (x + 4, y - 5, x - 4, y - 6, x - 6, y + 4, x + 2, y + 6,
                                                                 x - 2, y + 2, x - 2, y - 2, x + 4, y - 5), c, op=op)


def decor_layers():
    pts = [(150, 186), (190, 200), (230, 184), (176, 214), (70, 206)]
    out = {}
    out["quilt_moons"] = quilt("#9AA6F7", "#5A66CF", "#B4BEFF", "".join(moon_motif(x, y) for x, y in pts))
    out["quilt_dawn"] = quilt("#FFC2D9", "#B88AE0", "#FFE3EE",
                              "".join(circle(x, y, 4, WHITE, 0.6) + circle(x + 5, y + 1, 3, WHITE, 0.6) for x, y in pts))
    out["quilt_forest"] = quilt("#8FD6A0", "#3E8A5C", "#C8F0CF",
                                "".join(ellipse(x, y, 4, 8, "#E6FBE9", 0.65, rot=35) for x, y in pts))
    out["quilt_stars"] = quilt("#3B4AA8", "#1B2160", "#5B6BD0", "".join(sparkle(x, y, 6, GOLD) for x, y in pts + [(120, 200), (210, 212)]))

    def curtains(top, bottom, tie=None):
        defs = lin("c", 0, 20, 0, 150, [(0, top, 1), (1, bottom, 1)])
        b = path("M196 14 C206 60 200 110 214 150 L190 150 C184 104 184 50 186 14 Z", "url(#c)")
        b += path("M334 14 C324 60 330 110 316 150 L340 150 C346 104 346 50 344 14 Z", "url(#c)")
        b += path("M192 30 C196 70 194 110 200 146", stroke=WHITE, width=2, op=0.25)
        if tie:
            b += path("M190 100 Q200 106 210 100", stroke=tie, width=4) + path("M320 100 Q330 106 340 100", stroke=tie, width=4)
        return layer(b, defs)

    out["curtains_lilac"] = curtains("#B9A8FF", "#7B68D8")
    out["curtains_velvet"] = curtains("#3A4AA8", "#1A2060", GOLD)
    out["curtains_peach"] = curtains("#FFD3B8", "#F2A08A", "#D9785E")

    def glowdef(c):
        return rad("lg", 52, 92, 60, [(0, c, 0.55), (0.5, c, 0.15), (1, c, 0)])

    out["lamp_classic"] = layer(circle(52, 92, 60, "url(#lg)") + path("M40 112 L44 94 H60 L64 112 Z", "#FFD98A")
                                + path("M50 94 V84", stroke="#E7A94A", width=3), glowdef("#FFD98A"))
    out["lamp_paper"] = layer(circle(52, 92, 64, "url(#lg)") + path("M52 66 V74", stroke="#8A6A3A", width=2)
                              + ellipse(52, 92, 14, 18, "#FFB86B") + path("M40 92 H64 M42 84 H62 M42 100 H62", stroke="#E0883A", width=2, op=0.6)
                              + path("M46 74 H58 M46 110 H58", stroke="#8A4A1A", width=4), glowdef("#FFB86B"))
    out["lamp_fireflies"] = layer(circle(52, 92, 64, "url(#lg)") + path("M38 112 V86 Q38 80 44 80 H60 Q66 80 66 86 V112 Z", "#BFE9FF", op=0.35)
                                  + path("M40 78 H64", stroke="#8A6A3A", width=5)
                                  + "".join(circle(x, y, 2.5, "#F6FF8A") + circle(x, y, 6, "#F6FF8A", 0.3) for x, y in [(46, 100), (56, 92), (52, 106), (60, 102), (48, 90)]),
                                  glowdef("#DFFF7A"))
    pot = path("M80 112 L84 98 H100 L104 112 Z", "#5C6BC4")
    out["plant_sprout"] = layer(pot + "".join(ellipse(x, y, 4, 9, "#7FD08A", rot=r) for x, y, r in [(86, 92, -30), (92, 88, 0), (98, 92, 30)]))
    out["plant_fern"] = layer(pot + "".join('<g transform="rotate(%s 92 98)">%s</g>' % (r, ellipse(92, 78, 5, 20, "#5FBF77"))
                                            for r in (-50, -25, 0, 25, 50)))
    out["plant_mushrooms"] = layer(pot + circle(92, 92, 22, "#8FE3D4", 0.25)
                                   + path("M86 98 V90", stroke="#E6FFF8", width=3) + path("M78 90 Q86 78 94 90 Z", "#8FE3D4")
                                   + path("M98 98 V86", stroke="#E6FFF8", width=3) + path("M90 86 Q98 72 106 86 Z", "#B8F0E8")
                                   + circle(84, 86, 1.5, WHITE) + circle(99, 80, 1.5, WHITE))
    out["rug_moon"] = layer(ellipse(306, 214, 46, 13, "#8C7BE0") + ellipse(306, 214, 36, 9, "#A99CF0")
                            + moon_motif(306, 214, "#FFE6A8", 0.9))
    out["rug_cloud"] = layer("".join(ellipse(x, 214, 18, 10, "#DCE4FF") for x in (278, 298, 318, 336)) + ellipse(306, 218, 48, 8, "#C9D3FF"))
    out["mobile_stars"] = layer(path("M120 0 V40 M150 0 V58 M180 0 V34", stroke="#C9B9FF", width=1.5, op=0.8)
                                + path("M114 6 H186", stroke="#8C7BE0", width=3)
                                + sparkle(120, 46, 9, GOLD) + sparkle(150, 64, 10, GOLD) + moon_motif(180, 40, GOLD, 1))
    out["mobile_dreamcatcher"] = layer(path("M150 0 V20", stroke="#C9B9FF", width=1.5)
                                       + '<circle cx="150" cy="40" r="20" fill="none" stroke="#E7C38A" stroke-width="3"/>'
                                       + path("M134 30 L166 50 M166 30 L134 50 M150 20 V60 M130 40 H170", stroke="#E6E1FF", width=1, op=0.7)
                                       + "".join(path("M%s 60 V76" % x, stroke="#C9B9FF", width=1) + ellipse(x, 82, 4, 9, "#B9A8FF") for x in (138, 150, 162)))
    bulbs = ""
    for i in range(11):
        x = 6 + i * 17
        y = 14 + 16 * math.sin(math.pi * i / 10)
        c = ["#FFD98A", "#FF9EC4", "#8FE3D4", "#B9A8FF"][i % 4]
        bulbs += circle(round(x, 1), round(y + 4, 1), 7, c, 0.3) + circle(round(x, 1), round(y + 4, 1), 3.5, c)
    out["wall_garland"] = layer(path("M0 12 Q90 46 180 12", stroke="#4A3C90", width=2) + bulbs)
    stars = [(28, 30), (52, 22), (74, 40), (96, 30), (40, 70), (70, 76), (100, 64), (140, 26), (160, 48)]
    out["wall_constellations"] = layer(
        path("M28 30 L52 22 L74 40 L96 30 M40 70 L70 76 L100 64 M140 26 L160 48", stroke="#8C9BFF", width=1.2, op=0.6)
        + "".join(sparkle(x, y, 4, "#DDE2FF") for x, y in stars))
    return out


def opaline():
    gd, b = glow("g", 128, 140, 116, "#B8F0E8", 0.5)
    defs = gd + rad("pearl", 112, 118, 80, [(0, "#FFFFFF", 1), (0.55, "#E6DEFF", 1), (1, "#A9E6DD", 1)])
    defs += lin("halo", 128, 30, 128, 130, [(0, "#FFF4D6", 0.9), (1, "#FFD98A", 0.2)])
    b += ellipse(128, 222, 50, 8, "#000000", 0.16)
    b += path("M60 120 C60 70 96 40 128 40 C160 40 196 70 196 120 C186 82 160 60 128 60 C96 60 70 82 60 120 Z", "url(#halo)")
    b += circle(128, 146, 62, "url(#pearl)")
    for (x, y, r) in [(108, 92, 5), (128, 86, 6), (148, 92, 5)]:
        b += circle(x, y, r, "#FFFFFF") + circle(x - 1.5, y - 1.5, r * 0.4, "#E6DEFF")
    b += ellipse(104, 122, 16, 8, WHITE, 0.8, rot=-30)
    b += path("M92 170 Q128 190 164 170", stroke="#C8F2EC", width=3, op=0.6)
    b += eyes_closed(128, 146, 22, ink="#4A3F7A")
    b += blush(128, 164, 38, 9, 6, c="#FFB8D9", op=0.45) + smile(128, 164, 6, ink="#4A3F7A")
    for (x, y, r) in [(44, 70, 9), (214, 64, 11), (40, 190, 7), (218, 196, 8)]:
        b += sparkle(x, y, r, "#FFF4D6")
    return svg(b, defs)


# ---------- Ambiances de la journée (fond de chambre et lumière) ----------
def room_back_variant(wall_top, wall_bottom, sky_stops, celestial="", wall_dots="#3E4690", sky_stars=False):
    defs = lin("wallv", 180, 0, 180, 200, [(0, wall_top, 1), (1, wall_bottom, 1)])
    defs += lin("skyv", 265, 30, 265, 130, sky_stops)
    b = path("M0 0 H360 V240 H0 Z", "url(#wallv)")
    for (x, y) in [(20, 30), (70, 18), (120, 40), (160, 14), (30, 150), (190, 60), (180, 150), (340, 160), (110, 90), (345, 20)]:
        b += sparkle(x, y, 3, wall_dots)
    b += path("M216 128 V78 A49 49 0 0 1 314 78 V128 Z", "url(#skyv)")
    b += celestial
    if sky_stars:
        for (x, y, r) in [(236, 60, 2.5), (300, 52, 2), (252, 74, 1.8)]:
            b += sparkle(x, y, r * 1.6, WHITE, 0.7)
    return svg(b, defs, 360, 240)


def sun(x, y, r, core, glow_c, gid):
    return (rad(gid, x, y, r * 3, [(0, glow_c, 0.7), (0.4, glow_c, 0.25), (1, glow_c, 0)]),
            circle(x, y, r * 3, "url(#%s)" % gid) + circle(x, y, r, core))


def ambience_layers():
    out = {}
    gd, g = sun(288, 62, 15, "#FFF2B0", "#FFE48A", "sund")
    clouds = "".join(ellipse(x, y, rx, ry, WHITE, 0.85) for x, y, rx, ry in [(236, 96, 16, 6), (248, 92, 12, 6), (300, 108, 14, 5), (310, 104, 10, 5)])
    day = room_back_variant("#3F4C93", "#5A66A8", [(0, "#7CC6F5", 1), (1, "#D4EFFF", 1)], g + clouds, "#6B78C0")
    out["art_room_back_day"] = day.replace("<defs>", "<defs>" + gd, 1)
    gd, g = sun(280, 122, 17, "#FFD27A", "#FF9E6B", "sunr")
    dawn = room_back_variant("#33295E", "#574683", [(0, "#7B6BD6", 1), (0.55, "#F2A3B3", 1), (1, "#FFC48A", 1)], g, "#5A4A90", True)
    out["art_room_back_dawn"] = dawn.replace("<defs>", "<defs>" + gd, 1)
    gd, g = sun(246, 124, 16, "#FFB36B", "#FF7E5F", "suns")
    dusk = room_back_variant("#241F55", "#40316B", [(0, "#2E2A78", 1), (0.5, "#9A5FA8", 1), (1, "#FF8A6B", 1)], g, "#4A3E8A", True)
    out["art_room_back_dusk"] = dusk.replace("<defs>", "<defs>" + gd, 1)
    # Rayons de lumière à travers la fenêtre (jour : blanc chaud ; aube/crépuscule : orangé).
    for name, c, a in (("art_room_light_day", "#FFF4D6", 0.16), ("art_room_light_warm", "#FFB38A", 0.14)):
        defs = lin("beam", 265, 128, 200, 240, [(0, c, a * 1.6), (1, c, 0)])
        defs += rad("wash", 265, 80, 170, [(0, c, a), (1, c, 0)])
        b = circle(265, 80, 170, "url(#wash)")
        b += path("M216 128 L314 128 L270 240 L110 240 Z", "url(#beam)")
        out[name] = svg(b, defs, 360, 240)
    return out


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
    "art_frissonnette": frissonnette(),
    "art_veilleuse": veilleuse(),
    "art_brumelot": brumelot(),
    "art_songelune": songelune(),
    "art_abyssin": abyssin(),
    "art_mousselin": mousselin(),
    "art_lentorne": lentorne(),
    "art_onirion": onirion(),
    "art_grignoton": grignoton(),
    "art_cliquetis": cliquetis(),
    "art_orageon": orageon(),
    "art_flocondor": flocondor(),
    "art_lanternelle": lanternelle(),
    "art_citrouillon": citrouillon(),
    "art_brumoire": brumoire(),
    "art_bourgeonnet": bourgeonnet(),
    "art_petalune": petalune(),
    "art_solsticelle": solsticelle(),
    "art_meduzephyr": meduzephyr(),
    "art_room": room(),
    "art_opaline": opaline(),
    "art_room_back": room_back(),
    "art_room_window": room_window(),
    "art_room_bed": room_bed(),
}
ART.update({"art_decor_" + k: v for k, v in decor_layers().items()})
ART.update(ambience_layers())

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
        "border-radius:20px;padding:8px;text-align:center}img{width:200px;height:200px;display:block}"
        "img[src*=room]{width:420px;height:280px}</style>"
        + "".join(cards))
    print("ok", len(ART))
