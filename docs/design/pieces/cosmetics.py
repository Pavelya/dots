"""Draws the pictures for docs/design/cosmetics.md: the two piece styles and the two ring effects.

Run from the repository root (needs Python 3, rsvg-convert from librsvg, and ffmpeg):

    python3 docs/design/pieces/cosmetics.py            # draw every picture
    python3 docs/design/pieces/cosmetics.py sheets     # only the piece and ring sheets
    python3 docs/design/pieces/cosmetics.py measure    # print each shape's size, area and reach

It writes, in docs/design/pieces/:

- cosmetics-pieces-<style>.svg: the piece style's pieces and marks in every theme, light and dark;
- cosmetics-rings.svg: a captured area with each theme's own ring and with each ring effect;
- cosmetics-strip-<effect>.png: key frames of a capture with the ring effect, Notebook light and dark;
- cosmetics-board-<style>-<theme>-<version>-<pieces>-<view>.png: the colour-blindness check, the
  check board of pieces.md (15x15 at 4 mm per point) with the piece style and its ring effect
  (Hearts and clubs with Lasso, Flame and frost with Rubber stamp), usual and high-contrast pieces, as
  seen, in greyscale and with protanopia, deuteranopia and tritanopia.

The shapes, rings and theme colours come from docs/design/canvas/draw.py, which draws them from
docs/design/cosmetics.md and the theme pages, so the canvas frames and these pictures agree.
"""

import importlib.util
import math
import os
import subprocess
import sys

sys.dont_write_bytecode = True  # keep the design folders free of __pycache__
HERE = os.path.dirname(os.path.abspath(__file__))


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


notebook = load("pieces_draw", os.path.join(HERE, "draw.py"))  # the check board and the simulations
canvas = load("canvas_draw", os.path.join(HERE, "..", "canvas", "draw.py"))  # themes, shapes and rings

STYLES = {"cards": ("Hearts and clubs", "lasso"), "fire": ("Flame and frost", "stamp")}
EFFECTS = {"lasso": "Lasso", "stamp": "Rubber stamp"}
THEMES = [("notebook", "Notebook"), ("star-chart", "Star chart"), ("neon-arcade", "Neon arcade")]


def theme(name, version, pieces=None, rings=None):
    """A theme's colours, light or dark, with a piece style and a ring effect."""
    if name == "notebook":
        t = dict(canvas.THEMES[version], style="notebook")
    else:
        t = dict(canvas.BOARD_THEMES[name][version])
    return dict(t, pieces=pieces, rings=rings)


def f(v):
    return canvas.f(v)


# ---- The piece sheet ----

def piece_sheet(style, scale=4):
    """Eight cells per row, a light and a dark row per theme, each cell one 24 dp grid square. Columns:
    red, blue, red last move, blue last move, red captured, blue captured, red preview, blue preview."""
    sp, cols = 24.0, 8
    d = canvas.theme_dot(sp)
    out = []
    rows = [(name, v) for name, _ in THEMES for v in ("light", "dark")]
    for row, (name, version) in enumerate(rows):
        t = theme(name, version, pieces=style)
        y0 = row * sp
        for col in range(cols):
            x0, side = col * sp, ("red", "blue")[col % 2]
            cx, cy = x0 + sp / 2, y0 + sp / 2
            cell = f"s{row}{col}"
            out.append(f'<clipPath id="{cell}"><rect x="{f(x0)}" y="{f(y0)}" width="{f(sp)}" height="{f(sp)}"/></clipPath>')
            g = [canvas.el("rect", x=f(x0), y=f(y0), width=f(sp), height=f(sp), fill=t["paper"])]
            hatch = 5 if name == "notebook" else canvas.HATCH
            if col in (4, 5):  # captured: in the captor's wash and hatching
                captor = "blue" if side == "red" else "red"
                g.append(canvas.el("rect", x=f(x0), y=f(y0), width=f(sp), height=f(sp), fill=t[captor + "_wash"]))
                for k in range(-6, 7):
                    o = k * hatch * math.sqrt(2)
                    a = (cx + o - 20, cy + 20, cx + o + 20, cy - 20) if captor == "red" else (cx + o - 20, cy - 20, cx + o + 20, cy + 20)
                    g.append(canvas.el("line", x1=f(a[0]), y1=f(a[1]), x2=f(a[2]), y2=f(a[3]), stroke=t[captor + "_pale"],
                                       stroke_width=1))
            g.append(canvas.el("line", x1=f(x0), y1=f(cy), x2=f(x0 + sp), y2=f(cy), stroke=t["grid"], stroke_width=1))
            g.append(canvas.el("line", x1=f(cx), y1=f(y0), x2=f(cx), y2=f(y0 + sp), stroke=t["grid"], stroke_width=1))
            if t["style"] == "star":
                g.append(canvas.el("circle", cx=f(cx), cy=f(cy), r=1, fill=t["point"]))
            shape = canvas.piece_d(t, side, cx, cy, d)
            if col >= 6:
                g.append(canvas.el("path", d=shape, fill=t[side + "_pale"]))
                g.append(canvas.el("circle", cx=f(cx), cy=f(cy), r=f(0.45 * sp), fill="none", stroke=t[side],
                                   stroke_width=f(canvas.theme_ring(sp))))
            else:
                if t["style"] == "neon" and col < 4:  # only live pieces glow
                    g.append(canvas.band(t, side, cx, cy, d, t[side + "_pale"], canvas.GLOW))
                g.append(canvas.el("path", d=shape, fill=t[side]))
                if col in (2, 3):
                    g.append(canvas.el("circle", cx=f(cx), cy=f(cy), r=f(d * 0.15), fill=t["paper"]))
                if col in (4, 5):
                    L = d * 0.75 / math.sqrt(2)
                    if side == "red":  # captured by blue: the cut falls
                        cut, col_ = f"M{f(cx - L)} {f(cy - L)}L{f(cx + L)} {f(cy + L)}", t["blue_wash"]
                    else:
                        cut, col_ = f"M{f(cx - L)} {f(cy + L)}L{f(cx + L)} {f(cy - L)}", t["red_wash"]
                    g.append(canvas.el("path", d=cut, stroke=col_, stroke_width=f(d / 6), fill="none"))
            out.append(f'<g clip-path="url(#{cell})">{"".join(g)}</g>')
    w, h = cols * sp, len(rows) * sp
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{f(w * scale)}" height="{f(h * scale)}" '
            f'viewBox="0 0 {f(w)} {f(h)}">{"".join(out)}</svg>\n')


# ---- The ring sheet ----

RING_PATCH = dict(red=[(1, 1), (2, 1), (3, 2), (2, 3), (1, 3), (0, 2), (4, 0)], blue=[(1, 2), (2, 2), (3, 0), (4, 3)],
                  areas=[dict(captor="red", ring=[(1, 1), (2, 1), (3, 2), (2, 3), (1, 3), (0, 2)], inside=[(1, 2), (2, 2)])],
                  last=None)


def ring_sheet(scale=2):
    """A light and a dark row per theme; columns: the theme's own ring, Lasso, Rubber stamp. Each cell is a
    5 x 4 point patch at 24 dp spacing, with a red ring round two blue pieces."""
    sp, cw, ch = 24.0, 120, 96
    out = []
    rows = [(name, v) for name, _ in THEMES for v in ("light", "dark")]
    for row, (name, version) in enumerate(rows):
        for col, effect in enumerate((None, "lasso", "stamp")):
            t = theme(name, version, rings=effect)
            board = canvas.theme_board_svg(t, dict(RING_PATCH), f"r{row}{col}", size=cw, sp=sp)
            out.append(f'<g transform="translate({col * (cw + 8)} {row * (ch + 8)})"><svg width="{cw}" height="{ch}" '
                       f'viewBox="0 0 {cw} {ch}" overflow="hidden">{board}</svg></g>')
    w, h = 3 * cw + 16, len(rows) * (ch + 8) - 8
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w * scale}" height="{h * scale}" viewBox="0 0 {w} {h}">'
            f'{"".join(out)}</svg>\n')


# ---- The colour-blindness check ----

def check_position():
    """The check board of pieces.md (docs/design/pieces/draw.py), in the canvas's terms."""
    pos = notebook.board_position()
    return dict(red=sorted(pos["red"]), blue=sorted(pos["blue"]), last=pos["last"],
                areas=[dict(captor=c, ring=r, inside=i) for c, r, i in pos["areas"]],
                near=pos["threat_near"], far=pos["threat_far"], closing=[(p, "red") for p in pos["closing"]],
                preview=(pos["preview"], pos["to_move"]))


def check_board(t, sim, contrast):
    sp = 25.2
    size = 15 * sp
    px = round(size * 420 / 160)
    board = canvas.theme_board_svg(t, check_position(), "k", size=size, sp=sp, contrast=contrast)
    filt, open_g, close_g = "", "", ""
    if sim != "seen":
        values = " ".join(" ".join(str(v) for v in row) + " 0 0" for row in notebook.SIMULATIONS[sim]) + " 0 0 0 1 0"
        filt = (f'<filter id="sim" x="0" y="0" width="1" height="1" color-interpolation-filters="linearRGB">'
                f'<feColorMatrix type="matrix" values="{values}"/></filter>')
        open_g, close_g = '<g filter="url(#sim)">', "</g>"
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{px}" height="{px}" viewBox="0 0 {f(size)} {f(size)}">'
            f'<defs>{filt}</defs>{open_g}{board}{close_g}</svg>\n')


def render(svg_text, png):
    """The SVG as a PNG, then cut down to 256 colours with ffmpeg so the 120 boards stay small."""
    src, full = png + ".svg", png + ".full.png"
    with open(src, "w") as fh:
        fh.write(svg_text)
    subprocess.run(["rsvg-convert", "-o", full, src], check=True)
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", full, "-vf",
                    "split[a][b];[a]palettegen=max_colors=256:stats_mode=full[p];[b][p]paletteuse=dither=none",
                    "-pix_fmt", "pal8", png], check=True)
    os.remove(src)
    os.remove(full)


def write(name, text):
    with open(os.path.join(HERE, name), "w") as fh:
        fh.write(text)


# ---- How a ring effect moves: key frames of a capture ----
# A capture of two blue pieces, 0 ms being the moment the closing piece lands (motion.md, Capture).
# The times are those of docs/design/cosmetics.md, "How the capture moves".

STRIP_RING = [(3, 2), (4, 3), (4, 4), (3, 5), (2, 4), (2, 3)]
STRIP_INSIDE, STRIP_CLOSING = [(3, 3), (3, 4)], 4  # the closing piece is STRIP_RING[4]
STRIP_FRAMES = {
    "lasso": [(300, "Thrown round, loose"), (450, "The loop is round"), (500, "Pulling tight"),
              (550, "Tight: the ring closes"), (700, "The wash spreads; cuts"), (1250, "Done")],
    "stamp": [(300, "Coming down"), (420, "Coming down"), (500, "Almost down"),
              (550, "Down: the ring closes"), (700, "The cuts"), (1250, "Done")],
}


def prog(u, a, b):
    return min(1.0, max(0.0, (u - a) / (b - a)))


def ease_in(x):
    return x * x


def ease_out(x):
    return 1 - (1 - x) ** 2


def ease_in_out(x):
    return x * x * (3 - 2 * x)


def strip_area(effect, u):
    """The captured area u ms after the closing piece lands, for a ring effect."""
    a = dict(captor="red", ring=STRIP_RING, inside=STRIP_INSIDE, start=STRIP_CLOSING)
    if effect == "lasso":
        if u < 150:
            return None
        a.update(upto=ease_in_out(prog(u, 150, 450)), scale=1.15 - 0.15 * ease_in(prog(u, 450, 550)),
                 fill=ease_out(prog(u, 500, 800)))
    else:
        if u < 250:
            return None
        q = ease_in(prog(u, 250, 550))
        a.update(scale=1.25 - 0.25 * q, tint=q)
    return a


def strip_svg(effect, scale=2):
    """Key frames of a capture with a ring effect, Notebook light above and dark below."""
    sp, size, gap, head = 24.0, 168, 10, 34
    frames = STRIP_FRAMES[effect]
    out = []
    closing = STRIP_RING[STRIP_CLOSING]
    order = sorted(STRIP_INSIDE, key=lambda p: math.hypot(p[0] - closing[0], p[1] - closing[1]))
    for row, version in enumerate(("light", "dark")):
        t = theme("notebook", version, rings=effect)
        for col, (u, label) in enumerate(frames):
            a = strip_area(effect, u)
            cuts = [p for k, p in enumerate(order) if u >= 600 + 40 * k]
            pos = dict(red=STRIP_RING + [(5, 1)], blue=STRIP_INSIDE + [(1, 1), (5, 5), (0, 5), (5, 3)],
                       areas=[a] if a else [], cuts=cuts, last=closing if u >= 200 else None)
            board = canvas.theme_board_svg(t, pos, f"t{row}{col}", size=size, sp=sp)
            x, y = col * (size + gap), row * (size + head + gap)
            out.append(f'<text x="{x}" y="{y + 13}" font-family="Nunito, sans-serif" font-size="12" font-weight="800" '
                       f'fill="#2E2E2E">+{u} ms</text><text x="{x}" y="{y + 28}" font-family="Nunito, sans-serif" '
                       f'font-size="12" fill="#5E5A53">{label}</text>'
                       f'<g transform="translate({x} {y + head})">{board}</g>')
    w, h = len(frames) * (size + gap) - gap, 2 * (size + head) + gap
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w * scale}" height="{h * scale}" viewBox="0 0 {w} {h}">'
            f'<rect width="{w}" height="{h}" fill="#FFFFFF"/>{"".join(out)}</svg>\n')


# ---- Sizes ----

def measure():
    """Each shape's width, height, area (square dot sizes) and reach from the point (dot sizes)."""
    for name, parts in canvas.SHAPES.items():
        pts = [p for part in parts for p in part]
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        # area of the union, by sampling
        n, inside = 400, 0
        for i in range(n):
            for j in range(n):
                x, y = -0.8 + 1.6 * (i + 0.5) / n, -0.8 + 1.6 * (j + 0.5) / n
                if any(contains(part, x, y) for part in parts):
                    inside += 1
        area = inside * (1.6 / n) ** 2
        reach = max(math.hypot(*p) for p in pts)
        print(f"{name:6} width {max(xs) - min(xs):.2f}  height {max(ys) - min(ys):.2f}  "
              f"top {min(ys):.2f}  bottom {max(ys):.2f}  area {area:.2f}  reach {reach:.2f}")


def contains(poly, x, y):
    c = False
    for (x1, y1), (x2, y2) in zip(poly, poly[1:] + poly[:1]):
        if (y1 > y) != (y2 > y) and x < x1 + (y - y1) * (x2 - x1) / (y2 - y1):
            c = not c
    return c


def main():
    what = sys.argv[1] if len(sys.argv) > 1 else "all"
    if what == "measure":
        measure()
        return
    for style in STYLES:
        write(f"cosmetics-pieces-{style}.svg", piece_sheet(style))
    write("cosmetics-rings.svg", ring_sheet())
    for effect in STRIP_FRAMES:
        render(strip_svg(effect), os.path.join(HERE, f"cosmetics-strip-{effect}.png"))
    if what == "sheets":
        return
    for style, (_, effect) in STYLES.items():
        for name, _ in THEMES:
            for version in ("light", "dark"):
                t = theme(name, version, pieces=style, rings=effect)
                for kind, contrast in (("usual", False), ("contrast", True)):
                    for sim in notebook.SIMULATIONS:
                        png = os.path.join(HERE, f"cosmetics-board-{style}-{name}-{version}-{kind}-{sim}.png")
                        render(check_board(t, sim, contrast), png)
                        print(png)


if __name__ == "__main__":
    main()
