#!/usr/bin/env python3
"""Convertit un sous-ensemble de SVG en VectorDrawable Android.

Supporté : <path>, <circle>, <ellipse>, <g transform="translate(x y) rotate(a cx cy) scale(s)">,
fill/stroke (couleurs hex, url(#gradient), none), opacity, fill-opacity, stroke-*,
<linearGradient>/<radialGradient> en gradientUnits="userSpaceOnUse".
"""
import re
import sys
import xml.etree.ElementTree as ET

SVG = "{http://www.w3.org/2000/svg}"


def color(c, opacity=1.0):
    c = c.strip()
    if c.startswith("#"):
        h = c[1:]
        if len(h) == 3:
            h = "".join(ch * 2 for ch in h)
        a = round(max(0.0, min(1.0, opacity)) * 255)
        return "#%02X%s" % (a, h.upper())
    raise ValueError("couleur non supportée : " + c)


def num(v, default=0.0):
    return float(v) if v is not None else default


def fmt(x):
    s = ("%.3f" % x).rstrip("0").rstrip(".")
    return s if s not in ("-0", "") else "0"


def ellipse_path(cx, cy, rx, ry):
    return "M%s,%s a%s,%s 0 1,0 %s,0 a%s,%s 0 1,0 %s,0 Z" % (
        fmt(cx - rx), fmt(cy), fmt(rx), fmt(ry), fmt(2 * rx), fmt(rx), fmt(ry), fmt(-2 * rx))


def style_attrs(el):
    attrs = dict(el.attrib)
    if "style" in attrs:
        for part in attrs.pop("style").split(";"):
            if ":" in part:
                k, v = part.split(":", 1)
                attrs[k.strip()] = v.strip()
    return attrs


def gradient_xml(g, opacity, indent):
    pad = " " * indent
    stops = []
    for s in g.findall(SVG + "stop"):
        a = style_attrs(s)
        off = a.get("offset", "0")
        off = float(off[:-1]) / 100 if off.endswith("%") else float(off)
        stops.append((off, color(a.get("stop-color", "#000"), float(a.get("stop-opacity", 1)) * opacity)))
    if g.tag == SVG + "linearGradient":
        head = '%s<gradient android:type="linear" android:startX="%s" android:startY="%s" android:endX="%s" android:endY="%s">' % (
            pad, fmt(num(g.get("x1"))), fmt(num(g.get("y1"))), fmt(num(g.get("x2"))), fmt(num(g.get("y2"))))
    else:
        head = '%s<gradient android:type="radial" android:centerX="%s" android:centerY="%s" android:gradientRadius="%s">' % (
            pad, fmt(num(g.get("cx"))), fmt(num(g.get("cy"))), fmt(num(g.get("r"))))
    items = "\n".join('%s    <item android:offset="%s" android:color="%s" />' % (pad, fmt(o), c) for o, c in stops)
    return head + "\n" + items + "\n" + pad + "</gradient>"


def convert(svg_text):
    root = ET.fromstring(svg_text)
    vb = [float(x) for x in root.get("viewBox").split()]
    grads = {g.get("id"): g for g in root.iter() if g.tag in (SVG + "linearGradient", SVG + "radialGradient")}
    out = []

    def emit_path(d, attrs, indent, inherited_opacity):
        pad = " " * indent
        op = float(attrs.get("opacity", 1)) * inherited_opacity
        fill = attrs.get("fill", "#000")
        stroke = attrs.get("stroke", "none")
        lines = ['%s<path android:pathData="%s"' % (pad, " ".join(d.split()))]
        aapt = []
        if fill != "none":
            fo = float(attrs.get("fill-opacity", 1)) * op
            m = re.match(r"url\(#(.+)\)", fill)
            if m:
                aapt.append(("android:fillColor", gradient_xml(grads[m.group(1)], fo, indent + 8)))
            else:
                lines.append('%s      android:fillColor="%s"' % (pad, color(fill, fo)))
        if stroke != "none":
            so = float(attrs.get("stroke-opacity", 1)) * op
            m = re.match(r"url\(#(.+)\)", stroke)
            if m:
                aapt.append(("android:strokeColor", gradient_xml(grads[m.group(1)], so, indent + 8)))
            else:
                lines.append('%s      android:strokeColor="%s"' % (pad, color(stroke, so)))
            lines.append('%s      android:strokeWidth="%s"' % (pad, fmt(num(attrs.get("stroke-width"), 1))))
            cap = attrs.get("stroke-linecap")
            if cap in ("round", "square", "butt"):
                lines.append('%s      android:strokeLineCap="%s"' % (pad, cap))
            join = attrs.get("stroke-linejoin")
            if join in ("round", "bevel", "miter"):
                lines.append('%s      android:strokeLineJoin="%s"' % (pad, join))
        if attrs.get("fill-rule") == "evenodd":
            lines.append('%s      android:fillType="evenOdd"' % pad)
        if not aapt:
            out.append("\n".join(lines) + " />")
        else:
            out.append("\n".join(lines) + ">")
            for name, gx in aapt:
                out.append('%s    <aapt:attr name="%s">' % (pad, name))
                out.append(gx)
                out.append('%s    </aapt:attr>' % pad)
            out.append(pad + "</path>")

    def walk(el, indent, inherited, opacity):
        for child in el:
            tag = child.tag.replace(SVG, "")
            a = dict(inherited)
            a.update({k: v for k, v in style_attrs(child).items() if k not in ("opacity", "transform", "d", "id")})
            own_op = float(style_attrs(child).get("opacity", 1))
            if tag == "g":
                t = child.get("transform", "")
                group = []
                for fn, args in re.findall(r"(\w+)\(([^)]*)\)", t):
                    v = [float(x) for x in re.split(r"[ ,]+", args.strip())]
                    if fn == "translate":
                        group.append('android:translateX="%s" android:translateY="%s"' % (fmt(v[0]), fmt(v[1] if len(v) > 1 else 0)))
                    elif fn == "rotate":
                        r = 'android:rotation="%s"' % fmt(v[0])
                        if len(v) == 3:
                            r += ' android:pivotX="%s" android:pivotY="%s"' % (fmt(v[1]), fmt(v[2]))
                        group.append(r)
                    elif fn == "scale":
                        group.append('android:scaleX="%s" android:scaleY="%s"' % (fmt(v[0]), fmt(v[1] if len(v) > 1 else v[0])))
                if group:
                    out.append(" " * indent + "<group " + " ".join(group) + ">")
                    walk(child, indent + 4, a, opacity * own_op)
                    out.append(" " * indent + "</group>")
                else:
                    walk(child, indent, a, opacity * own_op)
            elif tag in ("path", "circle", "ellipse"):
                if tag == "path":
                    d = child.get("d")
                elif tag == "circle":
                    r = num(child.get("r"))
                    d = ellipse_path(num(child.get("cx")), num(child.get("cy")), r, r)
                else:
                    d = ellipse_path(num(child.get("cx")), num(child.get("cy")), num(child.get("rx")), num(child.get("ry")))
                a["opacity"] = own_op
                emit_path(d, a, indent, opacity)
            elif tag in ("defs", "title", "desc"):
                continue
            else:
                raise ValueError("élément non supporté : " + tag)

    walk(root, 4, {}, 1.0)
    head = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Généré par tools/art/svg2vd.py depuis art/svg : ne pas modifier à la main. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="256dp" android:height="256dp"\n'
            '    android:viewportWidth="%s" android:viewportHeight="%s">\n') % (fmt(vb[2]), fmt(vb[3]))
    return head + "\n".join(out) + "\n</vector>\n"


if __name__ == "__main__":
    sys.stdout.write(convert(open(sys.argv[1], encoding="utf-8").read()))
