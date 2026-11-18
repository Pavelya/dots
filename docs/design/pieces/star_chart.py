"""Draws the Star chart pictures and checks its contrast, for docs/design/theme-star-chart.md.

Run from the repository root (needs Python 3 and rsvg-convert, from librsvg):

    python3 docs/design/pieces/star_chart.py            # draw every picture
    python3 docs/design/pieces/star_chart.py contrast   # print the contrast tables

It writes:

- docs/design/theme-star-chart.svg: a board sample in the light and the dark version;
- star-chart-pieces-1x.svg and star-chart-pieces-4x.svg: every piece and mark, light and dark;
- star-chart-board-<version>-<pieces>-<view>.png: the colour-blindness check, a 15x15 board at
  4 mm per point with the usual and with high-contrast pieces, as seen, in greyscale and with
  protanopia, deuteranopia and tritanopia.

All values come from docs/design/theme-star-chart.md. The board position and the simulations
are the ones draw.py uses for Notebook, so the two themes are checked on the same board.
"""

import math
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from draw import SIMULATIONS, board_position, f  # noqa: E402

# ---- Colours (theme-star-chart.md, "Colours") ----

THEMES = {
    "light": dict(
        paper="#F3F5FA", card="#FFFFFF", grid="#D2DAEA", point="#8F9CBF",
        gold="#9A5B00", ice="#0E5C85",
        gold_haze="#F1E7D9", ice_haze="#DCE7F0", gold_pale="#E2CDAD", ice_pale="#B5CDE0",
        nova="#B8326E", nova_faint="#E6B3C9",
        text="#1A2240", dust="#525C78",
    ),
    "dark": dict(
        paper="#0F1733", card="#1A2448", grid="#26315C", point="#4A5A8E",
        gold="#F6CB6E", ice="#6DBAE6",
        gold_haze="#2C2A3F", ice_haze="#152A4C", gold_pale="#6B5B45", ice_pale="#2D5678",
        nova="#FF8DB5", nova_faint="#6A3559",
        text="#E6EAF7", dust="#9CA8CA",
    ),
}

# The Notebook inks, for the pairs this page compares with Notebook's.
NOTEBOOK = {
    "light": dict(blue="#1F4E9C", blue_pale="#BCCBEA", red="#C0392B", red_pale="#ECBEB7"),
    "dark": dict(blue="#6B90E8", blue_pale="#2F3F66", red="#FF9384", red_pale="#5E3834"),
}


def rgb(hex_colour):
    h = hex_colour.lstrip("#")
    return [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]


def linear(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def luminance(hex_colour, matrix=None):
    lin = [linear(c) for c in rgb(hex_colour)]
    if matrix is not None:
        lin = [max(0.0, min(1.0, sum(m * v for m, v in zip(row, lin)))) for row in matrix]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]


def ratio(a, b, matrix=None):
    la, lb = luminance(a, matrix), luminance(b, matrix)
    hi, lo = max(la, lb), min(la, lb)
    return math.floor((hi + 0.05) / (lo + 0.05) * 100) / 100  # cut, not rounded, as WebAIM shows


# ---- Sizes, in dp (theme-star-chart.md, "The board", and "The pieces") ----

def dot_size(spacing, contrast=False):
    if contrast:
        return min(24.0, max(9.0, spacing * 0.6))
    return min(20.0, max(9.0, spacing / 2))


def ring_width(spacing):
    return min(3.0, max(1.5, spacing / 12))


STAR_ACROSS = 1.4      # the circle through the star's five points, in dot sizes
STAR_INNER = 0.55      # the inner corners' radius, as a share of the points' radius
HEAD_ACROSS = 0.75     # the comet's round head, in dot sizes
TAIL_LEAVES = 70       # the tail leaves the head this many degrees either side of its line
MIDDLE_STREAK = 1.4    # the middle streak's tip, from the head's centre, in dot sizes
SIDE_STREAK = 1.05     # the side streaks' tips, along the tail
STREAK_SPREAD = 0.3    # the side streaks' tips, to either side of the tail's line
NOTCH = 0.65           # the notches between the streaks, along the tail
NOTCH_SPREAD = 0.11    # the notches, to either side of the tail's line
PINHOLE = 0.3
CUT_WIDTH = 1 / 6
CUT_LENGTH = 1.5
POINT_ACROSS = 2.0     # a chart point at each crossing, dp
HALO = 1.5             # high contrast: band round each piece, dp


def star_points(cx, cy, d):
    outer = d * STAR_ACROSS / 2
    inner = outer * STAR_INNER
    pts = []
    for k in range(10):
        a = math.radians(-90 + 36 * k)
        r = outer if k % 2 == 0 else inner
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def star_path(cx, cy, d):
    return "M" + " L".join(f"{f(x)},{f(y)}" for x, y in star_points(cx, cy, d)) + " Z"


def comet_path(cx, cy, d):
    """The round head on the point and a tail of three streaks running up and to the left, along
    the falling diagonal: a long one in the middle and a shorter one either side, with notches
    between them. Lengths are along the tail's line from the head's centre, widths across it."""
    h = d * HEAD_ACROSS / 2
    ux, uy = -1 / math.sqrt(2), -1 / math.sqrt(2)  # along the tail
    nx, ny = -uy, ux                               # across the tail

    def at(along, across):
        return cx + (ux * along + nx * across) * d, cy + (uy * along + ny * across) * d

    c, s = math.cos(math.radians(TAIL_LEAVES)), math.sin(math.radians(TAIL_LEAVES))
    a = (cx + (ux * c + nx * s) * h, cy + (uy * c + ny * s) * h)
    b = (cx + (ux * c - nx * s) * h, cy + (uy * c - ny * s) * h)
    pts = [a, at(SIDE_STREAK, STREAK_SPREAD), at(NOTCH, NOTCH_SPREAD), at(MIDDLE_STREAK, 0),
           at(NOTCH, -NOTCH_SPREAD), at(SIDE_STREAK, -STREAK_SPREAD), b]
    return ("M" + " L".join(f"{f(x)},{f(y)}" for x, y in pts) +
            f" A{f(h)},{f(h)} 0 1 0 {f(a[0])},{f(a[1])} Z")


def piece_path(side, cx, cy, d):
    return star_path(cx, cy, d) if side == "gold" else comet_path(cx, cy, d)


def piece(side, cx, cy, d, fill, halo=0.0, halo_colour=None):
    p = piece_path(side, cx, cy, d)
    out = ""
    if halo:
        out += (f'<path d="{p}" fill="{halo_colour}" stroke="{halo_colour}" stroke-width="{f(2 * halo)}" '
                f'stroke-linejoin="round"/>')
    return out + f'<path d="{p}" fill="{fill}"/>'


def pinhole(cx, cy, d, t):
    return f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(d * PINHOLE / 2)}" fill="{t["paper"]}"/>'


def cut(side, cx, cy, d, t):
    """A band in the captor's haze along the captor's hatching: a captured comet lies in a gold
    area (hatched rising, /), a captured star in an ice area (hatched falling, \\)."""
    h = d * CUT_LENGTH / 2 / math.sqrt(2)
    if side == "ice":
        x1, y1, x2, y2, colour = cx - h, cy + h, cx + h, cy - h, t["gold_haze"]
    else:
        x1, y1, x2, y2, colour = cx - h, cy - h, cx + h, cy + h, t["ice_haze"]
    return (f'<line x1="{f(x1)}" y1="{f(y1)}" x2="{f(x2)}" y2="{f(y2)}" stroke="{colour}" '
            f'stroke-width="{f(d * CUT_WIDTH)}"/>')


def preview(side, cx, cy, spacing, t, contrast=False):
    d = dot_size(spacing, contrast)
    pale = t[f"{side}_pale"]
    out = piece(side, cx, cy, d, pale)
    if contrast:
        out += (f'<path d="{piece_path(side, cx, cy, d)}" fill="none" stroke="{t[side]}" '
                f'stroke-width="1.5" stroke-linejoin="round"/>')
    return out + (f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(spacing * 0.45)}" fill="none" '
                  f'stroke="{t[side]}" stroke-width="{f(ring_width(spacing))}"/>')


# ---- Captured areas: haze, hatching and a straight constellation line ----

def straight_path(points):
    return "M" + " L".join(f"{f(x)},{f(y)}" for x, y in points) + " Z"


def area(uid, points, captor, spacing, t, width, height):
    d = straight_path(points)
    haze, pale = t[f"{captor}_haze"], t[f"{captor}_pale"]
    angle = -45 if captor == "gold" else 45  # gold rises to the right (/), ice falls (\)
    gap = 6
    defs = (f'<clipPath id="c{uid}"><path d="{d}"/></clipPath>'
            f'<pattern id="h{uid}" width="{gap}" height="{gap}" patternUnits="userSpaceOnUse" '
            f'patternTransform="rotate({angle})"><rect width="{gap}" height="{gap}" fill="{haze}"/>'
            f'<line x1="0" y1="0" x2="{gap}" y2="0" stroke="{pale}" stroke-width="2"/></pattern>')
    fill = f'<rect width="{f(width)}" height="{f(height)}" fill="url(#h{uid})" clip-path="url(#c{uid})"/>'
    ring = (f'<path d="{d}" fill="none" stroke="{t[captor]}" stroke-width="{f(ring_width(spacing))}" '
            f'stroke-linejoin="round" stroke-linecap="round"/>')
    return defs, fill, ring


# ---- A board, in dp; the caller scales it ----

def board(t, uid, n, spacing, pos, contrast=False):
    """Every layer in the theme's drawing order. Returns (defs, body, size in dp)."""
    s = spacing
    size = n * s

    def c(p):
        return s / 2 + p[0] * s, s / 2 + p[1] * s

    d = dot_size(spacing, contrast)
    defs, fills, rings = [], [], []
    inside = {}
    for i, (captor, pts, captured) in enumerate(pos["areas"]):
        a, b, r = area(f"{uid}{i}", [c(p) for p in pts], captor, spacing, t, size, size)
        defs.append(a), fills.append(b), rings.append(r)
        for p in captured:
            inside[p] = captor
    grid = []
    for i in range(n):
        w = 1.5 if i in (0, n - 1) else 1
        x = s / 2 + i * s
        grid.append(f'<line x1="{f(x)}" y1="{f(s / 2)}" x2="{f(x)}" y2="{f(size - s / 2)}" stroke="{t["grid"]}" stroke-width="{w}"/>')
        grid.append(f'<line x1="{f(s / 2)}" y1="{f(x)}" x2="{f(size - s / 2)}" y2="{f(x)}" stroke="{t["grid"]}" stroke-width="{w}"/>')
    for i in range(n):  # a chart point at every crossing; the pieces cover their own
        for j in range(n):
            x, y = c((i, j))
            grid.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(POINT_ACROSS / 2)}" fill="{t["point"]}"/>')
    threat_r = 0.42 * spacing
    marks = []
    for p in pos["threat_far"]:
        x, y = c(p)
        marks.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(threat_r)}" fill="none" stroke="{t["nova_faint"]}" stroke-width="1.5"/>')
    for p in pos["threat_near"]:
        x, y = c(p)
        marks.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(threat_r)}" fill="none" stroke="{t["nova"]}" stroke-width="2"/>')
    pieces, halos = [], []
    for side in ("gold", "ice"):
        for p in sorted(pos[side]):
            x, y = c(p)
            if contrast:
                surface = t[f'{inside[p]}_haze'] if p in inside else t["paper"]
                halos.append(piece(side, x, y, d, surface, HALO, surface))
            pieces.append(piece(side, x, y, d, t[side]))
            if p in pos["captured"]:
                pieces.append(cut(side, x, y, d, t))
            if p == pos["last"]:
                pieces.append(pinhole(x, y, d, t))
    for p in pos["closing"]:
        x, y = c(p)
        pieces.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(spacing / 6)}" fill="none" stroke="{t["gold"]}" stroke-width="1.5"/>')
    x, y = c(pos["preview"])
    pieces.append(preview(pos["to_move"], x, y, spacing, t, contrast))
    body = (f'<rect width="{f(size)}" height="{f(size)}" fill="{t["paper"]}"/>' + "".join(fills) +
            "".join(grid) + "".join(halos) + "".join(rings) + "".join(marks) + "".join(pieces))
    return "".join(defs), body, size


def position():
    """Notebook's check board, with red as gold and blue as ice."""
    p = board_position()
    names = {"red": "gold", "blue": "ice"}
    return dict(gold=p["red"], ice=p["blue"], captured=p["captured"], last=p["last"],
                areas=[(names[a], pts, inside) for a, pts, inside in p["areas"]],
                threat_near=p["threat_near"], threat_far=p["threat_far"], closing=p["closing"],
                to_move=names[p["to_move"]], preview=p["preview"])


def sim_filter(sim):
    m = SIMULATIONS[sim]
    values = " ".join(" ".join(str(v) for v in row) + " 0 0" for row in m) + " 0 0 0 1 0"
    return (f'<filter id="sim" x="0" y="0" width="1" height="1" color-interpolation-filters="linearRGB">'
            f'<feColorMatrix type="matrix" values="{values}"/></filter>')


def check_board_svg(version, contrast, sim=None, spacing=25.2, scale=420 / 160):
    """The 15x15 check board at 4 mm per point (25.2 dp on a 420 dpi phone)."""
    defs, body, size = board(THEMES[version], f"{version}{int(contrast)}", 15, spacing, position(), contrast)
    content = f'<g transform="scale({scale:.4f})">{body}</g>'
    filt = ""
    if sim is not None:
        filt = sim_filter(sim)
        content = f'<g filter="url(#sim)">{content}</g>'
    px = round(size * scale)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 {px} {px}">'
            f'<defs>{defs}{filt}</defs>{content}</svg>\n')


# ---- The sample on the theme page: a 9x7 board, light and dark side by side ----

def sample_position():
    gold, ice, areas = set(), set(), []

    def ring(captor, pts, inside):
        (gold if captor == "gold" else ice).update(pts)
        (ice if captor == "gold" else gold).update(inside)
        areas.append((captor, pts, inside))

    ring("gold", [(2, 1), (3, 2), (3, 3), (2, 4), (1, 3), (1, 2)], [(2, 2), (2, 3)])
    ring("ice", [(6, 1), (7, 2), (6, 3), (5, 2)], [(6, 2)])
    gold.update([(4, 4), (5, 5), (7, 5)])
    ice.update([(4, 3), (6, 4), (5, 4), (4, 1)])
    captured = {p for _, _, inside in areas for p in inside}
    return dict(gold=gold, ice=ice, areas=areas, captured=captured, last=(7, 5),
                threat_near=[(6, 4)], threat_far=[(4, 1)], closing=[(6, 5)], to_move="ice",
                preview=(1, 5))


def sample_svg(scale=2):
    parts, defs = [], []
    width = 0
    for k, version in enumerate(("light", "dark")):
        d, body, size = board(THEMES[version], f"s{version}", 9, 24, sample_position())
        # 9 columns, 7 rows: clip the square board to its top seven rows.
        h = 7 * 24
        defs.append(d + f'<clipPath id="cut{version}"><rect width="{size}" height="{h}"/></clipPath>')
        parts.append(f'<g transform="translate({k * (size + 12)},0)" clip-path="url(#cut{version})">{body}</g>')
        width = size * 2 + 12
    h = 7 * 24
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{f(width * scale)}" height="{f(h * scale)}" '
            f'viewBox="0 0 {f(width)} {h}"><defs>{"".join(defs)}</defs>{"".join(parts)}</svg>\n')


# ---- The piece sheet ----

def sheet_svg(scale):
    """Eight cells per row, light row then dark row, each cell one 24 dp grid square. Columns:
    gold, ice, gold last move, ice last move, captured gold, captured ice, gold preview, ice preview."""
    spacing = 24.0
    d = dot_size(spacing)
    cols = 8
    out = []
    for row, version in enumerate(("light", "dark")):
        t = THEMES[version]
        y0 = row * spacing
        out.append(f'<rect x="0" y="{f(y0)}" width="{f(cols * spacing)}" height="{f(spacing)}" fill="{t["paper"]}"/>')
        for col in range(cols):
            x0 = col * spacing
            cx, cy = x0 + spacing / 2, y0 + spacing / 2
            out.append(f'<clipPath id="cell{row}{col}"><rect x="{f(x0)}" y="{f(y0)}" width="{f(spacing)}" height="{f(spacing)}"/></clipPath>')
            side = "gold" if col % 2 == 0 else "ice"
            if col in (4, 5):  # captured: in the captor's haze and hatching
                captor = "ice" if side == "gold" else "gold"
                out.append(f'<rect x="{f(x0)}" y="{f(y0)}" width="{f(spacing)}" height="{f(spacing)}" fill="{t[captor + "_haze"]}"/>')
                for k in range(-5, 6):
                    o = k * 6 * math.sqrt(2)
                    if captor == "gold":
                        line = (cx + o - 20, cy + 20, cx + o + 20, cy - 20)
                    else:
                        line = (cx + o - 20, cy - 20, cx + o + 20, cy + 20)
                    out.append(f'<line x1="{f(line[0])}" y1="{f(line[1])}" x2="{f(line[2])}" y2="{f(line[3])}" '
                               f'stroke="{t[captor + "_pale"]}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            out.append(f'<line x1="{f(x0)}" y1="{f(cy)}" x2="{f(x0 + spacing)}" y2="{f(cy)}" stroke="{t["grid"]}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            out.append(f'<line x1="{f(cx)}" y1="{f(y0)}" x2="{f(cx)}" y2="{f(y0 + spacing)}" stroke="{t["grid"]}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            g = []
            if col >= 6:
                g.append(preview(side, cx, cy, spacing, t))
            else:
                g.append(piece(side, cx, cy, d, t[side]))
                if col in (2, 3):
                    g.append(pinhole(cx, cy, d, t))
                if col in (4, 5):
                    g.append(cut(side, cx, cy, d, t))
            out.append(f'<g clip-path="url(#cell{row}{col})">{"".join(g)}</g>')
    w, h = cols * spacing, 2 * spacing
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{f(w * scale)}" height="{f(h * scale)}" '
            f'viewBox="0 0 {f(w)} {f(h)}">{"".join(out)}</svg>\n')


# ---- Contrast (theme-star-chart.md, "Contrast") ----

PAIRS = [
    # kind, label, foreground, background, target
    ("Text", "Starlight on paper", "text", "paper", 4.5),
    ("Text", "Dust on paper", "dust", "paper", 4.5),
    ("Text", "Starlight on card", "text", "card", 4.5),
    ("Text", "Dust on card", "dust", "card", 4.5),
    ("Text", "Gold ink on paper", "gold", "paper", 4.5),
    ("Text", "Ice ink on paper", "ice", "paper", 4.5),
    ("Text", "Gold ink on card", "gold", "card", 4.5),
    ("Text", "Ice ink on card", "ice", "card", 4.5),
    ("Text", "Paper on ice ink (main button)", "paper", "ice", 4.5),
    ("Text", "Paper on gold ink (booster count)", "paper", "gold", 4.5),
    ("Text", "Nova on paper (error)", "nova", "paper", 4.5),
    ("Text", "Nova on card (error)", "nova", "card", 4.5),
    ("Text", "Paper on nova", "paper", "nova", 4.5),
    ("Piece", "Gold ink on paper", "gold", "paper", 3),
    ("Piece", "Ice ink on paper", "ice", "paper", 3),
    ("Piece", "Gold ink on grid", "gold", "grid", 3),
    ("Piece", "Ice ink on grid", "ice", "grid", 3),
    ("Piece", "Gold ink on gold haze", "gold", "gold_haze", 3),
    ("Piece", "Ice ink on gold haze", "ice", "gold_haze", 3),
    ("Piece", "Gold ink on ice haze", "gold", "ice_haze", 3),
    ("Piece", "Ice ink on ice haze", "ice", "ice_haze", 3),
    ("Piece", "Gold ink on gold pale (hatch line)", "gold", "gold_pale", 3),
    ("Piece", "Ice ink on gold pale (hatch line)", "ice", "gold_pale", 3),
    ("Piece", "Gold ink on ice pale (hatch line)", "gold", "ice_pale", 3),
    ("Piece", "Ice ink on ice pale (hatch line)", "ice", "ice_pale", 3),
    ("Mark", "Nova on paper", "nova", "paper", 3),
    ("Mark", "Nova on gold haze", "nova", "gold_haze", 3),
    ("Mark", "Nova on ice haze", "nova", "ice_haze", 3),
    ("Mark", "Pinhole: paper on gold ink", "paper", "gold", 3),
    ("Mark", "Pinhole: paper on ice ink", "paper", "ice", 3),
    ("Mark", "Cut: ice haze on gold ink", "ice_haze", "gold", 3),
    ("Mark", "Cut: gold haze on ice ink", "gold_haze", "ice", 3),
    ("Quiet", "Grid on paper", "grid", "paper", None),
    ("Quiet", "Chart point on paper", "point", "paper", None),
    ("Quiet", "Gold haze on paper", "gold_haze", "paper", None),
    ("Quiet", "Ice haze on paper", "ice_haze", "paper", None),
    ("Quiet", "Gold pale on gold haze", "gold_pale", "gold_haze", None),
    ("Quiet", "Ice pale on ice haze", "ice_pale", "ice_haze", None),
    ("Quiet", "Nova faint on paper", "nova_faint", "paper", None),
    ("Quiet", "Gold ink on ice ink", "gold", "ice", None),
]


def contrast_tables():
    lines = ["| Kind | Pair | Target | Light | Dark |", "| --- | --- | --- | --- | --- |"]
    failures = []
    for kind, label, fg, bg, target in PAIRS:
        values = []
        for version in ("light", "dark"):
            t = THEMES[version]
            r = ratio(t[fg], t[bg])
            values.append(f"{r:.2f}")
            if target is not None and r < target:
                failures.append(f"{label}, {version}: {r:.2f} < {target}")
        lines.append(f"| {kind} | {label} | {target if target is not None else '–'} | {values[0]} | {values[1]} |")
    lines += ["", "| View | Light | Dark |", "| --- | --- | --- |"]
    for sim, m in SIMULATIONS.items():
        name = {"seen": "As seen"}.get(sim, sim.capitalize())
        lines.append(f"| {name} | " + " | ".join(
            f"{ratio(THEMES[v]['gold'], THEMES[v]['ice'], m):.2f}" for v in ("light", "dark")) + " |")
    lines += ["", "| Picture lines | Light | Dark |", "| --- | --- | --- |"]
    for label, a, b, nb in (("Ice pale on ice ink (Notebook: Blue pale on blue ink)", "ice_pale", "ice", ("blue_pale", "blue")),
                            ("Gold pale on gold ink (Notebook: Red pale on red ink)", "gold_pale", "gold", ("red_pale", "red"))):
        cells = []
        for v in ("light", "dark"):
            cells.append(f"{ratio(THEMES[v][a], THEMES[v][b]):.2f} ({ratio(NOTEBOOK[v][nb[0]], NOTEBOOK[v][nb[1]]):.2f})")
        lines.append(f"| {label} | {cells[0]} | {cells[1]} |")
    return "\n".join(lines), failures


def write(path, text):
    with open(path, "w") as fh:
        fh.write(text)


def main():
    if sys.argv[1:] == ["contrast"]:
        table, failures = contrast_tables()
        print(table)
        print("\n" + ("\n".join(failures) if failures else "Every pair with a target meets it."))
        return
    write(os.path.join(HERE, "..", "theme-star-chart.svg"), sample_svg())
    write(os.path.join(HERE, "star-chart-pieces-1x.svg"), sheet_svg(1))
    write(os.path.join(HERE, "star-chart-pieces-4x.svg"), sheet_svg(4))
    for version in THEMES:
        for contrast in (False, True):
            for sim in SIMULATIONS:
                src = os.path.join(HERE, "_star_chart.svg")
                write(src, check_board_svg(version, contrast, None if sim == "seen" else sim))
                name = f"star-chart-board-{version}-{'contrast' if contrast else 'usual'}-{sim}.png"
                subprocess.run(["rsvg-convert", "-o", os.path.join(HERE, name), src], check=True)
                os.remove(src)


if __name__ == "__main__":
    main()
