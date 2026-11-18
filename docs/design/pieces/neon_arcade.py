"""Draws the Neon arcade pictures and checks its contrast, for docs/design/theme-neon-arcade.md.

Run from the repository root (needs Python 3 and rsvg-convert, from librsvg):

    python3 docs/design/pieces/neon_arcade.py            # draw every picture
    python3 docs/design/pieces/neon_arcade.py contrast   # print the contrast tables

It writes:

- docs/design/theme-neon-arcade.svg: a board sample in the light and the dark version;
- neon-arcade-pieces-1x.svg and neon-arcade-pieces-4x.svg: every piece and mark, light and dark;
- neon-arcade-board-<version>-<pieces>-<view>.png: the colour-blindness check, a 15x15 board at
  4 mm per point with the usual and with high-contrast pieces, as seen, in greyscale and with
  protanopia, deuteranopia and tritanopia;
- neon-arcade-faces.png: four of the computer's pictures from the app, recoloured with the face
  inks below, light above dark, on Paper, at 2 px per dp.

All values come from docs/design/theme-neon-arcade.md. The board position and the simulations
are the ones draw.py uses for Notebook, so every theme is checked on the same board.
"""

import math
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from draw import SIMULATIONS, board_position, f  # noqa: E402

# ---- Colours (theme-neon-arcade.md, "Colours") ----

THEMES = {
    "light": dict(
        paper="#F6F4FB", card="#FFFFFF", grid="#DCD6EB",
        magenta="#C8106E", cyan="#00778E",
        magenta_wash="#FADDEB", cyan_wash="#D8F1F5", magenta_glow="#F5B9D7", cyan_glow="#A3DDE7",
        magenta_core="#F77DB7", cyan_core="#5FC9DB",
        volt="#9C5E00", volt_faint="#EBCB8E",
        text="#1B1730", dim="#5A5574",
    ),
    "dark": dict(
        paper="#0A0A12", card="#1A1730", grid="#2A2546",
        magenta="#FF3D8B", cyan="#26E5FF",
        magenta_wash="#3A1630", cyan_wash="#0D3440", magenta_glow="#6A1D4A", cyan_glow="#0A4450",
        magenta_core="#FFC2DA", cyan_core="#C4F8FF",
        volt="#F5F23A", volt_faint="#5C5A1E",
        text="#F3F3FF", dim="#A8A4CA",
    ),
}

# The faces (theme-neon-arcade.md, "The faces"): the ink each Notebook ink of a picture becomes,
# in the order red ink, blue ink, red pale, blue pale. White stays white. In the dark version the
# body takes the deeper cyan, so the white eyes stand out on it, and the pale lines take the bright
# cyan.
FACE_INKS = {
    "light": [THEMES["light"][k] for k in ("magenta", "cyan", "magenta_glow", "cyan_glow")],
    "dark": [THEMES["dark"]["magenta"], THEMES["light"]["cyan"], THEMES["dark"]["magenta_glow"],
             THEMES["dark"]["cyan"]],
}
FACE_SAMPLES = ["opponent_moods_happy", "opponent_moods_upset", "opponent_strengths_sharp",
                "boss_world_1_smug"]

# The Notebook inks, for the pairs this page compares with Notebook's.
NOTEBOOK = {
    "light": dict(blue="#1F4E9C", blue_pale="#BCCBEA", red="#C0392B", red_pale="#ECBEB7", paper="#FBF8F1"),
    "dark": dict(blue="#6B90E8", blue_pale="#2F3F66", red="#FF9384", red_pale="#5E3834", paper="#1C1B19"),
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


# ---- Sizes, in dp (theme-neon-arcade.md, "The board", and "The pieces") ----

def dot_size(spacing, contrast=False):
    if contrast:
        return min(24.0, max(9.0, spacing * 0.6))
    return min(20.0, max(9.0, spacing / 2))


def ring_width(spacing):
    return min(3.0, max(1.5, spacing / 12))


TRIANGLE_REACH = 0.66  # the triangle's corners, from its centre, in dot sizes
PINHOLE = 0.3
CUT_WIDTH = 1 / 6
CUT_LENGTH = 1.5
GLOW = 1.5             # the glow round each live piece and each side of a ring's tube, dp
CORE = 1 / 3           # a ring's core, as a share of the ring's width
HALO = 1.5             # high contrast: band round each piece, dp
HATCH_GAP = 6


def triangle_points(cx, cy, d):
    r = d * TRIANGLE_REACH
    return [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
            for a in (-90, 30, 150)]


def piece_path(side, cx, cy, d):
    if side == "magenta":
        r = d / 2
        return (f"M{f(cx - r)},{f(cy)} A{f(r)},{f(r)} 0 1 0 {f(cx + r)},{f(cy)} "
                f"A{f(r)},{f(r)} 0 1 0 {f(cx - r)},{f(cy)} Z")
    return "M" + " L".join(f"{f(x)},{f(y)}" for x, y in triangle_points(cx, cy, d)) + " Z"


def band(side, cx, cy, d, colour, width):
    """A band of `width` dp round the piece's outline, with round corners."""
    p = piece_path(side, cx, cy, d)
    return (f'<path d="{p}" fill="{colour}" stroke="{colour}" stroke-width="{f(2 * width)}" '
            f'stroke-linejoin="round"/>')


def piece(side, cx, cy, d, fill):
    return f'<path d="{piece_path(side, cx, cy, d)}" fill="{fill}"/>'


def pinhole(cx, cy, d, t):
    return f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(d * PINHOLE / 2)}" fill="{t["paper"]}"/>'


def cut(side, cx, cy, d, t):
    """A band in the captor's wash along the captor's hatching: a captured triangle lies in a
    magenta area (hatched rising, /), a captured disc in a cyan area (hatched falling, \\)."""
    h = d * CUT_LENGTH / 2 / math.sqrt(2)
    if side == "cyan":
        x1, y1, x2, y2, colour = cx - h, cy + h, cx + h, cy - h, t["magenta_wash"]
    else:
        x1, y1, x2, y2, colour = cx - h, cy - h, cx + h, cy + h, t["cyan_wash"]
    return (f'<line x1="{f(x1)}" y1="{f(y1)}" x2="{f(x2)}" y2="{f(y2)}" stroke="{colour}" '
            f'stroke-width="{f(d * CUT_WIDTH)}"/>')


def preview(side, cx, cy, spacing, t, contrast=False):
    d = dot_size(spacing, contrast)
    out = piece(side, cx, cy, d, t[f"{side}_glow"])
    if contrast:
        out += (f'<path d="{piece_path(side, cx, cy, d)}" fill="none" stroke="{t[side]}" '
                f'stroke-width="1.5" stroke-linejoin="round"/>')
    return out + (f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(spacing * 0.45)}" fill="none" '
                  f'stroke="{t[side]}" stroke-width="{f(ring_width(spacing))}"/>')


# ---- Captured areas: wash, hatching and a tube ----

def straight_path(points):
    return "M" + " L".join(f"{f(x)},{f(y)}" for x, y in points) + " Z"


def area(uid, points, captor, spacing, t, width, height):
    """Returns (defs, fill, glow, tube, core): the tube's three layers are drawn in turn for every
    ring, so no ring's glow lies over another ring's tube."""
    d = straight_path(points)
    wash, glow = t[f"{captor}_wash"], t[f"{captor}_glow"]
    angle = -45 if captor == "magenta" else 45  # magenta rises to the right (/), cyan falls (\)
    gap = HATCH_GAP
    defs = (f'<clipPath id="c{uid}"><path d="{d}"/></clipPath>'
            f'<pattern id="h{uid}" width="{gap}" height="{gap}" patternUnits="userSpaceOnUse" '
            f'patternTransform="rotate({angle})"><rect width="{gap}" height="{gap}" fill="{wash}"/>'
            f'<line x1="0" y1="0" x2="{gap}" y2="0" stroke="{glow}" stroke-width="2"/></pattern>')
    fill = f'<rect width="{f(width)}" height="{f(height)}" fill="url(#h{uid})" clip-path="url(#c{uid})"/>'
    w = ring_width(spacing)

    def stroke(colour, width):
        return (f'<path d="{d}" fill="none" stroke="{colour}" stroke-width="{f(width)}" '
                f'stroke-linejoin="round" stroke-linecap="round"/>')

    return (defs, fill, stroke(glow, w + 2 * GLOW), stroke(t[captor], w),
            stroke(t[f"{captor}_core"], w * CORE))


# ---- A board, in dp; the caller scales it ----

def board(t, uid, n, spacing, pos, contrast=False):
    """Every layer in the theme's drawing order. Returns (defs, body, size in dp)."""
    s = spacing
    size = n * s

    def c(p):
        return s / 2 + p[0] * s, s / 2 + p[1] * s

    d = dot_size(spacing, contrast)
    defs, fills, glows, tubes, cores = [], [], [], [], []
    inside = {}
    for i, (captor, pts, captured) in enumerate(pos["areas"]):
        a, b, g, r, k = area(f"{uid}{i}", [c(p) for p in pts], captor, spacing, t, size, size)
        defs.append(a), fills.append(b), glows.append(g), tubes.append(r), cores.append(k)
        for p in captured:
            inside[p] = captor
    grid = []
    for i in range(n):
        w = 1.5 if i in (0, n - 1) else 1
        x = s / 2 + i * s
        grid.append(f'<line x1="{f(x)}" y1="{f(s / 2)}" x2="{f(x)}" y2="{f(size - s / 2)}" stroke="{t["grid"]}" stroke-width="{w}"/>')
        grid.append(f'<line x1="{f(s / 2)}" y1="{f(x)}" x2="{f(size - s / 2)}" y2="{f(x)}" stroke="{t["grid"]}" stroke-width="{w}"/>')
    threat_r = 0.42 * spacing
    marks = []
    for p in pos["threat_far"]:
        x, y = c(p)
        marks.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(threat_r)}" fill="none" stroke="{t["volt_faint"]}" stroke-width="1.5"/>')
    for p in pos["threat_near"]:
        x, y = c(p)
        marks.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(threat_r)}" fill="none" stroke="{t["volt"]}" stroke-width="2"/>')
    pieces, bands = [], []
    for side in ("magenta", "cyan"):
        for p in sorted(pos[side]):
            x, y = c(p)
            if contrast:
                surface = t[f'{inside[p]}_wash'] if p in inside else t["paper"]
                bands.append(band(side, x, y, d, surface, HALO))
            elif p not in pos["captured"]:
                bands.append(band(side, x, y, d, t[f"{side}_glow"], GLOW))
            pieces.append(piece(side, x, y, d, t[side]))
            if p in pos["captured"]:
                pieces.append(cut(side, x, y, d, t))
            if p == pos["last"]:
                pieces.append(pinhole(x, y, d, t))
    for p in pos["closing"]:
        x, y = c(p)
        pieces.append(f'<circle cx="{f(x)}" cy="{f(y)}" r="{f(spacing / 6)}" fill="none" stroke="{t["magenta"]}" stroke-width="1.5"/>')
    x, y = c(pos["preview"])
    pieces.append(preview(pos["to_move"], x, y, spacing, t, contrast))
    body = (f'<rect width="{f(size)}" height="{f(size)}" fill="{t["paper"]}"/>' + "".join(fills) +
            "".join(grid) + "".join(bands) + "".join(glows) + "".join(tubes) + "".join(cores) +
            "".join(marks) + "".join(pieces))
    return "".join(defs), body, size


def position():
    """Notebook's check board, with red as magenta and blue as cyan."""
    p = board_position()
    names = {"red": "magenta", "blue": "cyan"}
    return dict(magenta=p["red"], cyan=p["blue"], captured=p["captured"], last=p["last"],
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
    magenta, cyan, areas = set(), set(), []

    def ring(captor, pts, inside):
        (magenta if captor == "magenta" else cyan).update(pts)
        (cyan if captor == "magenta" else magenta).update(inside)
        areas.append((captor, pts, inside))

    ring("magenta", [(2, 1), (3, 2), (3, 3), (2, 4), (1, 3), (1, 2)], [(2, 2), (2, 3)])
    ring("cyan", [(6, 1), (7, 2), (6, 3), (5, 2)], [(6, 2)])
    magenta.update([(4, 4), (5, 5), (7, 5)])
    cyan.update([(4, 3), (6, 4), (5, 4), (4, 1)])
    captured = {p for _, _, inside in areas for p in inside}
    return dict(magenta=magenta, cyan=cyan, areas=areas, captured=captured, last=(7, 5),
                threat_near=[(6, 4)], threat_far=[(4, 1)], closing=[(6, 5)], to_move="cyan",
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
    magenta, cyan, magenta last move, cyan last move, captured magenta, captured cyan, magenta
    preview, cyan preview."""
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
            side = "magenta" if col % 2 == 0 else "cyan"
            if col in (4, 5):  # captured: in the captor's wash and hatching
                captor = "cyan" if side == "magenta" else "magenta"
                out.append(f'<rect x="{f(x0)}" y="{f(y0)}" width="{f(spacing)}" height="{f(spacing)}" fill="{t[captor + "_wash"]}"/>')
                for k in range(-5, 6):
                    o = k * HATCH_GAP * math.sqrt(2)
                    if captor == "magenta":
                        line = (cx + o - 20, cy + 20, cx + o + 20, cy - 20)
                    else:
                        line = (cx + o - 20, cy - 20, cx + o + 20, cy + 20)
                    out.append(f'<line x1="{f(line[0])}" y1="{f(line[1])}" x2="{f(line[2])}" y2="{f(line[3])}" '
                               f'stroke="{t[captor + "_glow"]}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            out.append(f'<line x1="{f(x0)}" y1="{f(cy)}" x2="{f(x0 + spacing)}" y2="{f(cy)}" stroke="{t["grid"]}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            out.append(f'<line x1="{f(cx)}" y1="{f(y0)}" x2="{f(cx)}" y2="{f(y0 + spacing)}" stroke="{t["grid"]}" stroke-width="1" clip-path="url(#cell{row}{col})"/>')
            g = []
            if col >= 6:
                g.append(preview(side, cx, cy, spacing, t))
            else:
                if col < 4:
                    g.append(band(side, cx, cy, d, t[side + "_glow"], GLOW))
                g.append(piece(side, cx, cy, d, t[side]))
                if col in (2, 3):
                    g.append(pinhole(cx, cy, d, t))
                if col in (4, 5):
                    g.append(cut(side, cx, cy, d, t))
            out.append(f'<g clip-path="url(#cell{row}{col})">{"".join(g)}</g>')
    w, h = cols * spacing, 2 * spacing
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{f(w * scale)}" height="{f(h * scale)}" '
            f'viewBox="0 0 {f(w)} {f(h)}">{"".join(out)}</svg>\n')


# ---- Contrast (theme-neon-arcade.md, "Contrast") ----

PAIRS = [
    # kind, label, foreground, background, target
    ("Text", "Main text on paper", "text", "paper", 4.5),
    ("Text", "Dim text on paper", "dim", "paper", 4.5),
    ("Text", "Main text on card", "text", "card", 4.5),
    ("Text", "Dim text on card", "dim", "card", 4.5),
    ("Text", "Magenta ink on paper", "magenta", "paper", 4.5),
    ("Text", "Cyan ink on paper", "cyan", "paper", 4.5),
    ("Text", "Magenta ink on card", "magenta", "card", 4.5),
    ("Text", "Cyan ink on card", "cyan", "card", 4.5),
    ("Text", "Paper on cyan ink (main button)", "paper", "cyan", 4.5),
    ("Text", "Paper on magenta ink (booster count, error)", "paper", "magenta", 4.5),
    ("Text", "Volt on paper (dots left, running out)", "volt", "paper", 4.5),
    ("Piece", "Magenta ink on paper", "magenta", "paper", 3),
    ("Piece", "Cyan ink on paper", "cyan", "paper", 3),
    ("Piece", "Magenta ink on grid", "magenta", "grid", 3),
    ("Piece", "Cyan ink on grid", "cyan", "grid", 3),
    ("Piece", "Magenta ink on magenta wash", "magenta", "magenta_wash", 3),
    ("Piece", "Cyan ink on magenta wash", "cyan", "magenta_wash", 3),
    ("Piece", "Magenta ink on cyan wash", "magenta", "cyan_wash", 3),
    ("Piece", "Cyan ink on cyan wash", "cyan", "cyan_wash", 3),
    ("Piece", "Magenta ink on magenta glow (its glow; hatch line)", "magenta", "magenta_glow", 3),
    ("Piece", "Cyan ink on magenta glow (hatch line)", "cyan", "magenta_glow", 3),
    ("Piece", "Magenta ink on cyan glow (hatch line)", "magenta", "cyan_glow", 3),
    ("Piece", "Cyan ink on cyan glow (its glow; hatch line)", "cyan", "cyan_glow", 3),
    ("Mark", "Volt on paper", "volt", "paper", 3),
    ("Mark", "Volt on magenta wash", "volt", "magenta_wash", 3),
    ("Mark", "Volt on cyan wash", "volt", "cyan_wash", 3),
    ("Mark", "Pinhole: paper on magenta ink", "paper", "magenta", 3),
    ("Mark", "Pinhole: paper on cyan ink", "paper", "cyan", 3),
    ("Mark", "Cut: cyan wash on magenta ink", "cyan_wash", "magenta", 3),
    ("Mark", "Cut: magenta wash on cyan ink", "magenta_wash", "cyan", 3),
    ("Quiet", "Grid on paper", "grid", "paper", None),
    ("Quiet", "Magenta wash on paper", "magenta_wash", "paper", None),
    ("Quiet", "Cyan wash on paper", "cyan_wash", "paper", None),
    ("Quiet", "Magenta glow on paper", "magenta_glow", "paper", None),
    ("Quiet", "Cyan glow on paper", "cyan_glow", "paper", None),
    ("Quiet", "Magenta glow on magenta wash", "magenta_glow", "magenta_wash", None),
    ("Quiet", "Cyan glow on cyan wash", "cyan_glow", "cyan_wash", None),
    ("Quiet", "Magenta core on magenta ink", "magenta_core", "magenta", None),
    ("Quiet", "Cyan core on cyan ink", "cyan_core", "cyan", None),
    ("Quiet", "Volt faint on paper", "volt_faint", "paper", None),
    ("Quiet", "Magenta ink on cyan ink", "magenta", "cyan", None),
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
            f"{ratio(THEMES[v]['magenta'], THEMES[v]['cyan'], m):.2f}" for v in ("light", "dark")) + " |")
    lines += ["", "| Faces | Light | Dark |", "| --- | --- | --- |"]
    for label, a, b, na, nb in (("White eyes on the body", "#FFFFFF", 1, "#FFFFFF", "blue"),
                                ("Lines on the body (eyebrows, mouth)", 3, 1, "blue_pale", "blue"),
                                ("The body on paper", 1, "paper", "blue", "paper")):
        cells = []
        for v in ("light", "dark"):
            ink = FACE_INKS[v]

            def colour(x):
                return ink[x] if isinstance(x, int) else THEMES[v].get(x, x)

            nbk = NOTEBOOK[v]
            cells.append(f"{ratio(colour(a), colour(b)):.2f} ({ratio(nbk.get(na, na), nbk.get(nb, nb)):.2f})")
        lines.append(f"| {label} | {cells[0]} | {cells[1]} |")
    return "\n".join(lines), failures


def faces_png(path):
    """The FACE_SAMPLES from the app, recoloured with FACE_INKS, light row above dark, on Paper."""
    sys.path.insert(0, os.path.join(HERE, "..", "illustrations"))
    import make_pictures as mp

    def hx(h):
        return tuple(int(h.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4))

    root = os.path.join(HERE, "..", "..", "..", "app", "src", "main", "res", "drawable-nodpi")
    rows = []
    for version in ("light", "dark"):
        swap = dict(zip(mp.LIGHT + [mp.WHITE], [hx(c) for c in FACE_INKS[version]] + [mp.WHITE]))
        paper = hx(THEMES[version]["paper"])
        pictures = []
        for name in FACE_SAMPLES:
            w, h, px = mp.read_png(os.path.join(root, f"{name}.png"))
            out = []
            for r, g, b, a in px:
                c = swap[(r, g, b)] if a else paper
                out.append(tuple(round(c[i] * a / 255 + paper[i] * (1 - a / 255)) for i in range(3)) + (255,))
            pictures.append((w, h, out))
        rows.append((pictures, paper))
    height = max(h for pictures, _ in rows for _, h, _ in pictures)
    width = sum(w for w, _, _ in rows[0][0])
    canvas = []
    for pictures, paper in rows:
        for y in range(height):
            for w, h, out in pictures:
                canvas.extend(out[y * w:(y + 1) * w] if y < h else [paper + (255,)] * w)
    # Half size (2 px per dp), each pixel the average of four, so the file stays small.
    w2, h2 = width // 2, height
    small = []
    for y in range(h2):
        for x in range(w2):
            block = [canvas[(2 * y + dy) * width + 2 * x + dx] for dy in (0, 1) for dx in (0, 1)]
            small.append(tuple(round(sum(p[i] for p in block) / 4) for i in range(3)) + (255,))
    mp.write_png(path, w2, h2, small)


def write(path, text):
    with open(path, "w") as fh:
        fh.write(text)


def main():
    if sys.argv[1:] == ["contrast"]:
        table, failures = contrast_tables()
        print(table)
        print("\n" + ("\n".join(failures) if failures else "Every pair with a target meets it."))
        return
    write(os.path.join(HERE, "..", "theme-neon-arcade.svg"), sample_svg())
    write(os.path.join(HERE, "neon-arcade-pieces-1x.svg"), sheet_svg(1))
    write(os.path.join(HERE, "neon-arcade-pieces-4x.svg"), sheet_svg(4))
    faces_png(os.path.join(HERE, "neon-arcade-faces.png"))
    for version in THEMES:
        for contrast in (False, True):
            for sim in SIMULATIONS:
                src = os.path.join(HERE, "_neon_arcade.svg")
                write(src, check_board_svg(version, contrast, None if sim == "seen" else sim))
                name = f"neon-arcade-board-{version}-{'contrast' if contrast else 'usual'}-{sim}.png"
                subprocess.run(["rsvg-convert", "-o", os.path.join(HERE, name), src], check=True)
                os.remove(src)


if __name__ == "__main__":
    main()
