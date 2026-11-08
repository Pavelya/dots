"""Draws the piece sheets and the colour-blindness check images for docs/design/pieces.md.

Run from the repository root (needs Python 3 and rsvg-convert, from librsvg):

    python3 docs/design/pieces/draw.py

It writes pieces-1x.svg and pieces-4x.svg (every piece, light and dark) and draws a 15x15 board
at 4 mm per point, in the light and the dark version, as five PNGs each: as seen, in greyscale,
and as seen with protanopia, deuteranopia and tritanopia. All values come from
docs/design/theme-notebook.md and docs/design/pieces.md.
"""

import math
import os
import random
import subprocess

HERE = os.path.dirname(os.path.abspath(__file__))

THEMES = {
    "light": dict(paper="#FBF8F1", grid="#D3DFEE", red="#C0392B", blue="#1F4E9C",
                  red_wash="#F6DCD7", blue_wash="#DCE4F4", red_pale="#ECBEB7", blue_pale="#BCCBEA",
                  hi="#A86200", hi_faint="#E9C58F"),
    "dark": dict(paper="#1C1B19", grid="#34383E", red="#FF9384", blue="#6B90E8",
                 red_wash="#3A2421", blue_wash="#1F2940", red_pale="#5E3834", blue_pale="#2F3F66",
                 hi="#F0B04A", hi_faint="#6E5428"),
}

# Colour-vision simulations, applied in linear RGB (the default for SVG filters).
# Machado, Oliveira and Fernandes (2009), full severity; greyscale is relative luminance.
SIMULATIONS = {
    "seen": None,
    "greyscale": [[0.2126, 0.7152, 0.0722]] * 3,
    "protanopia": [[0.152286, 1.052583, -0.204868],
                   [0.114503, 0.786281, 0.099216],
                   [-0.003882, -0.048116, 1.051998]],
    "deuteranopia": [[0.367322, 0.860646, -0.227968],
                     [0.280085, 0.672501, 0.047413],
                     [-0.011820, 0.042940, 0.968881]],
    "tritanopia": [[1.255528, -0.076749, -0.178779],
                   [-0.078411, 0.930809, 0.147602],
                   [0.004733, 0.691367, 0.303900]],
}


def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


# ---- Sizes, in dp, from the spacing (docs/design/pieces.md) ----

def dot_size(spacing):
    return min(20.0, max(9.0, spacing / 2))


def ring_width(spacing):
    return min(3.0, max(1.5, spacing / 12))


# ---- Pieces ----

def round_piece(cx, cy, d, fill):
    return f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(d / 2)}" fill="{fill}"/>'


def diamond_points(cx, cy, d):
    """The blue piece: a square turned 45 degrees, corners on the grid lines.
    Corner to corner it is 7/6 of the dot size (14 dp beside a 12 dp dot): at that size the
    darker blue diamond and the round red dot look equally heavy."""
    h = d * 7 / 6 / 2
    return [(cx, cy - h), (cx + h, cy), (cx, cy + h), (cx - h, cy)]


def diamond_piece(cx, cy, d, fill):
    pts = " ".join(f"{f(x)},{f(y)}" for x, y in diamond_points(cx, cy, d))
    return f'<polygon points="{pts}" fill="{fill}"/>'


def piece(side, cx, cy, d, fill):
    return round_piece(cx, cy, d, fill) if side == "red" else diamond_piece(cx, cy, d, fill)


def last_move_mark(cx, cy, d, t):
    """The pinhole: a Paper-coloured dot in the middle of the last piece placed."""
    return f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(d * 0.15)}" fill="{t["paper"]}"/>'


def captured_mark(side, cx, cy, d, t):
    """The cut: a stroke in the captor's wash colour across the captured piece, along the captor's
    hatching. Red pieces are captured by blue (hatched falling, \\), blue by red (rising, /)."""
    h = d * 0.75  # reaches past the piece's edge, so the cut goes right through it
    w = d / 6
    if side == "red":  # captor blue: falling to the right
        x1, y1, x2, y2 = cx - h / math.sqrt(2), cy - h / math.sqrt(2), cx + h / math.sqrt(2), cy + h / math.sqrt(2)
        colour = t["blue_wash"]
    else:  # captor red: rising to the right
        x1, y1, x2, y2 = cx - h / math.sqrt(2), cy + h / math.sqrt(2), cx + h / math.sqrt(2), cy - h / math.sqrt(2)
        colour = t["red_wash"]
    return (f'<line x1="{f(x1)}" y1="{f(y1)}" x2="{f(x2)}" y2="{f(y2)}" stroke="{colour}" '
            f'stroke-width="{f(w)}" stroke-linecap="butt"/>')


def preview(side, cx, cy, spacing, t):
    d = dot_size(spacing)
    pale = t["red_pale"] if side == "red" else t["blue_pale"]
    return (piece(side, cx, cy, d, pale) +
            f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(spacing * 0.45)}" fill="none" '
            f'stroke="{t[side]}" stroke-width="{f(ring_width(spacing))}"/>')


# ---- Areas and rings ----

def bowed_path(points, seed, bow):
    rnd = random.Random(seed)
    out = [f"M{f(points[0][0])},{f(points[0][1])}"]
    n = len(points)
    for i in range(n):
        (x1, y1), (x2, y2) = points[i], points[(i + 1) % n]
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        length = math.hypot(x2 - x1, y2 - y1)
        nx, ny = -(y2 - y1) / length, (x2 - x1) / length
        k = rnd.uniform(-bow, bow) * 2  # a quadratic's middle moves half as far as its control point
        out.append(f"Q{f(mx + nx * k)},{f(my + ny * k)} {f(x2)},{f(y2)}")
    return " ".join(out) + " Z"


def area(uid, points, captor, spacing, scale, t, width, height):
    """Wash, hatching and ring of one captured area, in px; points are already in px."""
    d = bowed_path(points, seed=f"{points[0]}", bow=0.4 * scale)
    wash = t["red_wash"] if captor == "red" else t["blue_wash"]
    pale = t["red_pale"] if captor == "red" else t["blue_pale"]
    angle = -45 if captor == "red" else 45  # red rises to the right (/), blue falls (\)
    gap = 5 * scale
    defs = (f'<clipPath id="c{uid}"><path d="{d}"/></clipPath>'
            f'<pattern id="h{uid}" width="{f(gap)}" height="{f(gap)}" patternUnits="userSpaceOnUse" '
            f'patternTransform="rotate({angle})"><rect width="{f(gap)}" height="{f(gap)}" fill="{wash}"/>'
            f'<line x1="0" y1="0" x2="{f(gap)}" y2="0" stroke="{pale}" stroke-width="{f(2 * scale)}"/></pattern>')
    fill = f'<rect width="{f(width)}" height="{f(height)}" fill="url(#h{uid})" clip-path="url(#c{uid})"/>'
    ring = (f'<path d="{d}" fill="none" stroke="{t[captor]}" stroke-width="{f(ring_width(spacing) * scale)}" '
            f'stroke-linejoin="round" stroke-linecap="round"/>')
    return defs, fill, ring


# ---- The check board: 15x15, mid game ----

def board_position():
    red, blue = set(), set()
    areas = []  # (captor, ring points, captured points)

    def ring(captor, pts, inside):
        (red if captor == "red" else blue).update(pts)
        (blue if captor == "red" else red).update(inside)
        areas.append((captor, pts, inside))

    ring("red", [(3, 2), (4, 3), (4, 4), (3, 5), (2, 4), (2, 3)], [(3, 3), (3, 4)])
    ring("blue", [(9, 4), (10, 5), (9, 6), (8, 5)], [(9, 5)])
    ring("blue", [(5, 9), (6, 9), (7, 10), (7, 11), (6, 12), (5, 11), (4, 10)], [(5, 10), (6, 10), (6, 11)])
    ring("red", [(11, 10), (12, 11), (11, 12), (10, 11)], [(11, 11)])
    red.update([(5, 3), (6, 4), (6, 6), (7, 5), (10, 3), (11, 4), (12, 6), (8, 7), (9, 8), (8, 9),
                (3, 7), (2, 9), (4, 12), (9, 12), (12, 13), (13, 9), (12, 1), (13, 2), (1, 6), (7, 13)])
    blue.update([(5, 5), (6, 3), (7, 6), (8, 3), (11, 6), (12, 5), (10, 8), (8, 8), (4, 7), (3, 8),
                 (3, 10), (2, 11), (8, 11), (10, 13), (13, 12), (12, 8), (12, 2), (1, 2), (11, 2), (6, 7)])
    captured = {p for _, _, inside in areas for p in inside}
    return dict(red=red, blue=blue, areas=areas, captured=captured, last=(9, 8),
                threat_near=[(8, 8)], threat_far=[(12, 2)], closing=[(7, 8)], to_move="blue",
                preview=(2, 13))


def board_svg(theme, spacing=25.2, scale=420 / 160, sim=None):
    """spacing in dp (25.2 dp = 4 mm); scale in px per dp (420 dpi phone)."""
    t = THEMES[theme]
    n = 15
    s = spacing * scale
    size = n * s
    pos = board_position()

    def c(p):
        return s / 2 + p[0] * s, s / 2 + p[1] * s

    d = dot_size(spacing) * scale
    defs, fills, rings, body = [], [], [], []
    for i, (captor, pts, _) in enumerate(pos["areas"]):
        a, b, r = area(f"{theme}{i}", [c(p) for p in pts], captor, spacing, scale, t, size, size)
        defs.append(a), fills.append(b), rings.append(r)

    grid = []
    for i in range(n):
        w = (1.5 if i in (0, n - 1) else 1) * scale
        x = s / 2 + i * s
        grid.append(f'<line x1="{f(x)}" y1="{f(s / 2)}" x2="{f(x)}" y2="{f(size - s / 2)}" stroke="{t["grid"]}" stroke-width="{f(w)}"/>')
        grid.append(f'<line x1="{f(s / 2)}" y1="{f(x)}" x2="{f(size - s / 2)}" y2="{f(x)}" stroke="{t["grid"]}" stroke-width="{f(w)}"/>')

    threat_r = 0.42 * spacing * scale
    for p in pos["threat_far"]:
        x, y = c(p)
        body.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(threat_r)}" fill="none" stroke="{t["hi_faint"]}" stroke-width="{f(1.5 * scale)}"/>')
    for p in pos["threat_near"]:
        x, y = c(p)
        body.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(threat_r)}" fill="none" stroke="{t["hi"]}" stroke-width="{f(2 * scale)}"/>')
    for side in ("red", "blue"):
        for p in sorted(pos[side]):
            x, y = c(p)
            body.append(piece(side, x, y, d, t[side]))
            if p in pos["captured"]:
                body.append(captured_mark(side, x, y, d, t))
            if p == pos["last"]:
                body.append(last_move_mark(x, y, d, t))
    for p in pos["closing"]:
        x, y = c(p)
        body.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(spacing / 6 * scale)}" fill="none" stroke="{t["red"]}" stroke-width="{f(1.5 * scale)}"/>')
    x, y = c(pos["preview"])
    pv = preview(pos["to_move"], x / scale, y / scale, spacing, t)
    body.append(f'<g transform="scale({scale:.4f})">{pv}</g>')

    content = (f'<rect width="{f(size)}" height="{f(size)}" fill="{t["paper"]}"/>' + "".join(fills) +
               "".join(grid) + "".join(rings) + "".join(body))
    filt = ""
    if sim is not None:
        m = SIMULATIONS[sim]
        values = " ".join(" ".join(str(v) for v in row) + " 0 0" for row in m) + " 0 0 0 1 0"
        filt = (f'<filter id="sim" x="0" y="0" width="1" height="1" color-interpolation-filters="linearRGB">'
                f'<feColorMatrix type="matrix" values="{values}"/></filter>')
        content = f'<g filter="url(#sim)">{content}</g>'
    px = round(size)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 {f(size)} {f(size)}">'
            f'<defs>{"".join(defs)}{filt}</defs>{content}</svg>\n')


# ---- The piece sheet ----

def sheet_svg(scale):
    """Eight cells per row, light row then dark row, each cell one 24 dp grid square.
    Columns: red, blue, red last move, blue last move, red captured, blue captured,
    red preview, blue preview."""
    spacing = 24.0
    d = dot_size(spacing)
    cols = 8
    out = []
    for row, theme in enumerate(("light", "dark")):
        t = THEMES[theme]
        y0 = row * spacing
        out.append(f'<rect x="0" y="{f(y0)}" width="{f(cols * spacing)}" height="{f(spacing)}" fill="{t["paper"]}"/>')
        for col in range(cols):
            x0 = col * spacing
            cx, cy = x0 + spacing / 2, y0 + spacing / 2
            out.append(f'<clipPath id="cell{row}{col}"><rect x="{f(x0)}" y="{f(y0)}" width="{f(spacing)}" height="{f(spacing)}"/></clipPath>')
            if col in (4, 5):  # captured: sit in the captor's wash and hatching
                captor = "blue" if col == 4 else "red"
                wash = t[f"{captor}_wash"]
                out.append(f'<rect x="{f(x0)}" y="{f(y0)}" width="{f(spacing)}" height="{f(spacing)}" fill="{wash}"/>')
                pale = t[f"{captor}_pale"]
                for k in range(-5, 6):
                    o = k * 5 * math.sqrt(2)
                    if captor == "red":
                        line = (cx + o - 20, cy + 20, cx + o + 20, cy - 20)
                    else:
                        line = (cx + o - 20, cy - 20, cx + o + 20, cy + 20)
                    out.append(f'<line x1="{f(line[0])}" y1="{f(line[1])}" x2="{f(line[2])}" y2="{f(line[3])}" '
                               f'stroke="{pale}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            out.append(f'<line x1="{f(x0)}" y1="{f(cy)}" x2="{f(x0 + spacing)}" y2="{f(cy)}" stroke="{t["grid"]}" stroke-width="1"/>')
            out.append(f'<line x1="{f(cx)}" y1="{f(y0)}" x2="{f(cx)}" y2="{f(y0 + spacing)}" stroke="{t["grid"]}" stroke-width="1"/>')
            side = "red" if col % 2 == 0 else "blue"
            if col >= 6:
                out.append(preview(side, cx, cy, spacing, t))
                continue
            out.append(piece(side, cx, cy, d, t[side]))
            if col in (2, 3):
                out.append(last_move_mark(cx, cy, d, t))
            if col in (4, 5):
                out.append(captured_mark(side, cx, cy, d, t))
    w, h = cols * spacing, 2 * spacing
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{f(w * scale)}" height="{f(h * scale)}" '
            f'viewBox="0 0 {f(w)} {f(h)}">{"".join(out)}</svg>\n')


def write(name, text):
    with open(os.path.join(HERE, name), "w") as fh:
        fh.write(text)


def main():
    write("pieces-1x.svg", sheet_svg(1))
    write("pieces-4x.svg", sheet_svg(4))
    for theme in THEMES:
        for sim in SIMULATIONS:
            src = os.path.join(HERE, "_sim.svg")
            with open(src, "w") as fh:
                fh.write(board_svg(theme, sim=None if sim == "seen" else sim))
            subprocess.run(["rsvg-convert", "-o", os.path.join(HERE, f"board-{theme}-{sim}.png"), src], check=True)
            os.remove(src)


if __name__ == "__main__":
    main()
