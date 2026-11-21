"""Draws the canvas mock-ups for docs/design/canvas/README.md.

Run from the repository root (needs Python 3 and Google Chrome, and a network connection for the
two Google Fonts):

    python3 docs/design/canvas/draw.py                  # the PNGs in docs/design/canvas/
    python3 docs/design/canvas/draw.py --canvas DIR     # also the design canvas files, in DIR/project/
    python3 docs/design/canvas/draw.py --only star-chart  # one theme: notebook, star-chart or neon-arcade
    python3 docs/design/canvas/draw.py --only cosmetics   # the piece styles, ring effects and Shop

Every frame is 360 x 800 dp, the size of a common Android phone, or 320 x 568 dp for a small phone,
drawn in the light and the dark
version. The PNGs are 3 px per dp (1080 x 2400). Every colour and size comes from
docs/design/theme-notebook.md and docs/design/pieces.md. The game frames are drawn again in each
board theme, from docs/design/theme-star-chart.md and docs/design/theme-neon-arcade.md. Set CHROME to
use another Chrome binary.
"""

import math
import os
import random
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
CHROME = os.environ.get("CHROME", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
W, H = 360, 800
SCALE = 3

FONTS = ("https://fonts.googleapis.com/css2?family=Caveat:wght@600;700"
         "&family=Nunito:wght@500;700;800&display=block")

THEMES = {
    "light": dict(paper="#FBF8F1", card="#FFFFFF", grid="#D3DFEE", red="#C0392B", blue="#1F4E9C",
                  red_wash="#F6DCD7", blue_wash="#DCE4F4", red_pale="#ECBEB7", blue_pale="#BCCBEA",
                  hi="#A86200", hi_faint="#E9C58F", pencil="#2E2E2E", graphite="#5E5A53",
                  margin="#EBB2AA", shadow="rgba(46,46,46,0.28)", scrim="rgba(46,46,46,0.45)"),
    "dark": dict(paper="#1C1B19", card="#272522", grid="#34383E", red="#FF9384", blue="#6B90E8",
                 red_wash="#3A2421", blue_wash="#1F2940", red_pale="#5E3834", blue_pale="#2F3F66",
                 hi="#F0B04A", hi_faint="#6E5428", pencil="#EDE8DF", graphite="#A8A399",
                 margin="#5A3531", shadow="rgba(0,0,0,0.6)", scrim="rgba(0,0,0,0.55)"),
}

# The board themes (decision 0028): Star chart (theme-star-chart.md) and Neon arcade
# (theme-neon-arcade.md). Each fills Notebook's roles with its own colours: red is gold or magenta,
# blue is ice or cyan, hi is Nova or Volt. Shadow and scrim stay Notebook's, the tape is the red
# side's pale colour, and style picks the board's shapes. Only the game screen takes a board theme:
# the map, its notes and the menus stay Notebook's in every theme.
BOARD_THEMES = {
    "star-chart": {
        "light": dict(THEMES["light"], style="star", paper="#F3F5FA", card="#FFFFFF", grid="#D2DAEA",
                      point="#8F9CBF", red="#9A5B00", blue="#0E5C85", red_wash="#F1E7D9", blue_wash="#DCE7F0",
                      red_pale="#E2CDAD", blue_pale="#B5CDE0", hi="#B8326E", hi_faint="#E6B3C9",
                      pencil="#1A2240", graphite="#525C78", margin="#D2DAEA", tape="#E2CDAD"),
        "dark": dict(THEMES["dark"], style="star", paper="#0F1733", card="#1A2448", grid="#26315C",
                     point="#4A5A8E", red="#F6CB6E", blue="#6DBAE6", red_wash="#2C2A3F", blue_wash="#152A4C",
                     red_pale="#6B5B45", blue_pale="#2D5678", hi="#FF8DB5", hi_faint="#6A3559",
                     pencil="#E6EAF7", graphite="#9CA8CA", margin="#26315C", tape="#6B5B45"),
    },
    "neon-arcade": {
        "light": dict(THEMES["light"], style="neon", paper="#F6F4FB", card="#FFFFFF", grid="#DCD6EB",
                      red="#C8106E", blue="#00778E", red_wash="#FADDEB", blue_wash="#D8F1F5",
                      red_pale="#F5B9D7", blue_pale="#A3DDE7", red_core="#F77DB7", blue_core="#5FC9DB",
                      hi="#9C5E00", hi_faint="#EBCB8E", pencil="#1B1730", graphite="#5A5574",
                      margin="#DCD6EB", tape="#F5B9D7"),
        # The computer's face in the dark: the body takes the light version's deeper cyan, so the white
        # eyes stand out, and its lines and shadow take the bright cyan (theme page, The faces).
        "dark": dict(THEMES["dark"], style="neon", paper="#0A0A12", card="#1A1730", grid="#2A2546",
                     red="#FF3D8B", blue="#26E5FF", red_wash="#3A1630", blue_wash="#0D3440",
                     red_pale="#6A1D4A", blue_pale="#0A4450", red_core="#FFC2DA", blue_core="#C4F8FF",
                     hi="#F5F23A", hi_faint="#5C5A1E", pencil="#F3F3FF", graphite="#A8A4CA",
                     margin="#2A2546", tape="#6A1D4A", face_body="#00778E", face_shadow="#26E5FF",
                     face_line="#26E5FF"),
    },
}

# Type styles from the theme page. Caveat only at 24 sp and above, never for numbers or buttons.
DISPLAY = "font-family: Caveat, cursive; font-weight: 700; font-size: 40px; line-height: 48px"
HEADLINE = "font-family: Caveat, cursive; font-weight: 600; font-size: 28px; line-height: 36px"
HAND24 = "font-family: Caveat, cursive; font-weight: 700; font-size: 24px; line-height: 28px"
TITLE = "font-weight: 700; font-size: 20px; line-height: 28px"
SCORE = "font-weight: 800; font-size: 24px; line-height: 28px; font-variant-numeric: tabular-nums"
BODY = "font-weight: 500; font-size: 16px; line-height: 24px"
LABEL = "font-weight: 700; font-size: 14px; line-height: 20px"
SMALL = "font-weight: 500; font-size: 12px; line-height: 16px"

BUTTON_SHAPE = "border-radius: 16px 20px 14px 22px / 20px 14px 22px 16px"


def f(v):
    return f"{v:.2f}".rstrip("0").rstrip(".")


def el(name, **a):
    """One SVG element with an explicit closing tag (the canvas format needs them)."""
    attrs = " ".join(f'{k.rstrip("_").replace("_", "-")}="{v}"' for k, v in a.items() if v is not None)
    return f"<{name} {attrs}></{name}>"


def svg(w, h, body, view=None, style="", label=None):
    a11y = f'role="img" aria-label="{label}"' if label else 'aria-hidden="true"'
    return (f'<svg width="{f(w)}" height="{f(h)}" viewBox="{view or f"0 0 {f(w)} {f(h)}"}" {a11y} '
            f'style="display: block; overflow: visible; {style}">{body}</svg>')


# ---------------------------------------------------------------- the game ----
# One game of Level 7 (Siege) on a 15 x 15 board. Red is the player and moves first; blue is the
# computer, six dots ahead. Each moment adds to the one before, so the frames tell one story.

def area(captor, ring, inside):
    return dict(captor=captor, ring=ring, inside=inside)


OPENING_RED = [(6, 7), (8, 7)]
OPENING_BLUE = [(7, 6), (7, 8), (4, 4), (10, 4), (4, 10), (10, 10), (2, 7), (12, 7)]

R1 = area("red", [(4, 9), (5, 10), (5, 11), (4, 12), (3, 11), (3, 10)], [(4, 10), (4, 11)])
B1 = area("blue", [(11, 2), (12, 3), (11, 4), (10, 3)], [(11, 3)])
BIG = area("red", [(4, 3), (5, 3), (6, 4), (6, 5), (6, 6), (5, 7), (4, 7), (3, 6), (3, 5), (3, 4)],
           [(4, 4), (5, 4), (4, 5), (5, 5), (4, 6), (5, 6)])
PAIR = [(8, 7), (9, 8)]
BLUE_ATTACK = [(8, 6), (9, 7), (10, 8), (8, 8), (7, 7)]  # (9, 9) would close it
CLUSTER = [(4, 4), (5, 4), (4, 5), (5, 5), (4, 6), (5, 6)]

THREAT_RED = OPENING_RED + R1["ring"] + [(11, 3), (9, 8)] + [p for p in BIG["ring"] if p != (5, 7)]
THREAT_BLUE = OPENING_BLUE + B1["ring"] + BLUE_ATTACK + CLUSTER[1:] + [(4, 11), (11, 11), (12, 12)]

POSITIONS = {
    "opening": dict(red=OPENING_RED, blue=OPENING_BLUE, areas=[], last=None),
    "threat": dict(red=THREAT_RED, blue=THREAT_BLUE, areas=[R1, B1], last=(7, 7),
                   near=PAIR + CLUSTER, closing=[((9, 9), "blue"), ((5, 7), "red")]),
    "big": dict(red=THREAT_RED + [(5, 7)], blue=THREAT_BLUE, areas=[R1, B1, BIG], last=(5, 7), burst=True),
    "win": dict(red=THREAT_RED + [(5, 7)], blue=THREAT_BLUE, areas=[R1, B1, BIG], last=(5, 7)),
    # Undo's preview, from the threat: your last dot (9, 8) and the computer's answer (7, 7), which put
    # your pair in danger, show pale in dashed rings. The pair's threat marks go; blue's six keep theirs.
    "undo": dict(red=[p for p in THREAT_RED if p != (9, 8)], blue=[p for p in THREAT_BLUE if p != (7, 7)],
                 areas=[R1, B1], last=None, near=CLUSTER, closing=[((5, 7), "red")],
                 ghosts=[((9, 8), "red"), ((7, 7), "blue")]),
    # Placing the first dot by drag (decision 0024): the finger is down at (10, 7); the preview's row and
    # column run out from its ring to the board's edges, under the pieces.
    "drag": dict(red=OPENING_RED, blue=OPENING_BLUE, areas=[], last=None, drag=((10, 7), "red")),
}

# Aiming a booster by drag (D-18), from the threat. The booster preview is open and the finger is down on
# a target; the guides run in Pencil from the booster's mark. Wall: on (9, 9), the point that would close
# blue's ring round your pair. Bomb: on (8, 7), where its plus takes four blue dots (motion.md, Boosters).
THREAT_MARKS = dict(near=PAIR + CLUSTER, closing=[((9, 9), "blue"), ((5, 7), "red")])
for _key, _aim in (("wall-aim", dict(booster="wall", point=(9, 9))), ("bomb-aim", dict(booster="bomb", point=(8, 7)))):
    POSITIONS[_key] = dict(red=THREAT_RED, blue=THREAT_BLUE, areas=[R1, B1], last=(7, 7), aim=_aim, **THREAT_MARKS)


# Free play and two players in an open-ended game (decision 0025, D-16): one position on a 15 x 15 board.
# Red leads 4 to 1. Its two rings run to the top and right edges, a chain runs in from the left edge, and
# one group of three in the lower middle is not joined to the edge. After a stop, only such groups can
# still be captured (the engine's Grounding.ungrounded).
FREE_R1 = area("red", [(6, 3), (7, 4), (6, 5), (5, 4)], [(6, 4)])
FREE_R2 = area("red", [(9, 9), (10, 8), (11, 8), (12, 9), (11, 10), (10, 11), (9, 10)], [(10, 9), (11, 9), (10, 10)])
FREE_B1 = area("blue", [(2, 3), (3, 4), (2, 5), (1, 4)], [(2, 4)])
FREE_RED = (FREE_R1["ring"] + [(6, 2), (7, 1), (7, 0)] + FREE_R2["ring"] + [(13, 10), (14, 10)] +
            [(0, 8), (1, 8), (2, 7), (3, 7)] + [(5, 11), (6, 11), (6, 12)] + [(2, 4)])
FREE_BLUE = (FREE_B1["ring"] + [(0, 4)] + [(6, 4), (10, 9), (11, 9), (10, 10)] + [(4, 11), (7, 11), (6, 13)] +
             [(8, 14), (8, 13), (9, 12)] + [(9, 5), (10, 4), (11, 3), (12, 3), (13, 2), (14, 2)] +
             [(3, 9), (4, 6), (8, 7), (7, 8)])
FREE = dict(red=FREE_RED, blue=FREE_BLUE, areas=[FREE_R1, FREE_R2, FREE_B1], last=(7, 11))


def open_dots(pos, side):
    """side's live dots in groups that touch no edge, joined across or diagonally: the dots that can
    still be captured after a stop (Grounding.ungrounded)."""
    captured = {p for a in pos["areas"] for p in a["inside"]}
    live = set(pos[side]) - captured
    out, seen = [], set()
    for start in sorted(live):
        if start in seen:
            continue
        group, stack = [], [start]
        seen.add(start)
        while stack:
            x, y = stack.pop()
            group.append((x, y))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    q = (x + dx, y + dy)
                    if q in live and q not in seen:
                        seen.add(q)
                        stack.append(q)
        if not any(x in (0, 14) or y in (0, 14) for x, y in group):
            out += group
    return sorted(out)


def swapped(pos, drop=()):
    """The same position with the sides swapped and mirrored left to right; drop leaves out some of the
    dots that were red."""
    m = lambda p: (14 - p[0], p[1])  # noqa: E731
    return dict(red=[m(p) for p in pos["blue"]], blue=[m(p) for p in pos["red"] if p not in drop],
                areas=[area("blue" if a["captor"] == "red" else "red", [m(p) for p in a["ring"]],
                            [m(p) for p in a["inside"]]) for a in pos["areas"]],
                last=m(pos["last"]))


# The computer leads 4 to 1 and has only two dots not joined to the edge, so it stops: even if the player
# captured both, it would still win (SearchAi.shouldStop).
FREE_COMPUTER = swapped(FREE, drop=[(6, 12)])
POSITIONS["free"] = FREE
POSITIONS["free-stop"] = dict(FREE, open=open_dots(FREE, "red"))
POSITIONS["free-computer-stops"] = dict(FREE_COMPUTER, open=open_dots(FREE_COMPUTER, "blue"))
POSITIONS["two-done"] = dict(FREE, open=open_dots(FREE, "red"), last=(8, 7))
assert len(POSITIONS["free-stop"]["open"]) == 3 and len(POSITIONS["free-computer-stops"]["open"]) == 2


def plus_of(p):
    """The bomb's plus: the point and its four neighbours on the board, centre first."""
    x, y = p
    return [q for q in ((x, y), (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)) if 0 <= q[0] < 15 and 0 <= q[1] < 15]


def aim_targets(pos):
    """The points the open booster may be aimed at (the engine's rules): for a wall every empty point,
    for a bomb every point whose plus holds a live blue dot."""
    taken = set(pos["red"]) | set(pos["blue"])
    captured = {p for a in pos["areas"] for p in a["inside"]}
    live_blue = set(pos["blue"]) - captured
    points = [(x, y) for y in range(15) for x in range(15)]
    if pos["aim"]["booster"] == "wall":
        return [p for p in points if p not in taken and p not in captured]
    return [p for p in points if any(q in live_blue for q in plus_of(p))]


def aim_hits(pos):
    """The blue dots the bomb would take: the live ones in its plus."""
    if not pos["aim"]["point"]:
        return []
    captured = {p for a in pos["areas"] for p in a["inside"]}
    return [q for q in plus_of(pos["aim"]["point"]) if q in pos["blue"] and q not in captured]


def aim_reach(pos, sp):
    """How far from the aimed point each guide starts (left, right, up, down): at the wall's square,
    0.35 of the spacing out, or past the bomb's plus, at the outer edge of its last ring."""
    if pos["aim"]["booster"] == "wall":
        return (0.35 * sp,) * 4
    plus = set(plus_of(pos["aim"]["point"]))
    x, y = pos["aim"]["point"]
    return tuple((1.46 if q in plus else 0.46) * sp for q in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))


def aim_under(t, pos, c, sp):
    """The marks of a booster preview that lie under the links and guides: the bomb's plus on its wash,
    the targets (small dots in the pale ink of the side to move) and the wall's dashed square."""
    out = []
    aim = pos["aim"]
    if aim["booster"] == "bomb" and aim["point"]:
        for q in plus_of(aim["point"]):
            x, y = c(q)
            out.append(el("circle", cx=f(x), cy=f(y), r=f(0.46 * sp), fill=t["red_wash"]))
    for q in aim_targets(pos):
        if aim["booster"] == "wall" and q == aim["point"]:
            continue
        x, y = c(q)
        out.append(el("circle", cx=f(x), cy=f(y), r=f(0.1 * sp), fill=t["red_pale"]))
    if aim["booster"] == "wall" and aim["point"]:
        out.append(wall_mark(t, *c(aim["point"]), sp, dashed=True))
    return "".join(out)


def aim_over(t, pos, c, sp):
    """The bomb's plus rings, dashed in the ink of the side to move, over the threat rings."""
    if pos["aim"]["booster"] != "bomb" or not pos["aim"]["point"]:
        return ""
    out = []
    for q in plus_of(pos["aim"]["point"]):
        x, y = c(q)
        out.append(el("circle", cx=f(x), cy=f(y), r=f(0.46 * sp), fill="none", stroke=t["red"], stroke_width=1.5,
                      stroke_dasharray="3 3"))
    return "".join(out)


def wall_mark(t, x, y, sp, dashed=False):
    """The wall's mark: a square, 0.7 of the spacing, in Pencil, hatched with five Graphite lines; in the
    booster preview its outline is dashed (motion.md, Boosters: Wall)."""
    h = 0.35 * sp
    w = max(0.075 * sp, 1.5)
    out = []
    for k in range(1, 6):  # rising lines, evenly across the square's diagonal
        d = 4 * h * k / 6
        x1, y1 = x - h + max(0, d - 2 * h), y - h + min(d, 2 * h)
        x2, y2 = x - h + min(d, 2 * h), y - h + max(0, d - 2 * h)
        out.append(el("line", x1=f(x1), y1=f(y1), x2=f(x2), y2=f(y2), stroke=t["graphite"], stroke_width=f(w * 0.6)))
    out.append(el("rect", x=f(x - h), y=f(y - h), width=f(2 * h), height=f(2 * h), fill="none", stroke=t["pencil"],
                  stroke_width=f(w), stroke_dasharray="3 2" if dashed else None))
    return "".join(out)

DRAG_DASH = (4, 3)  # drag's guides: dashes and gaps along each arm, dp


def guide_arms(x, y, sp, size, q=1.0, reach=None):
    """Drag's guides: four arms from the preview ring (0.45 of the spacing from the point) out to the
    board's outermost grid lines. q (0 to 1) is how far they have been drawn out from the ring. While a
    booster is aimed, reach gives where each arm starts instead (left, right, up, down); an arm whose
    start lies past the board's edge is not drawn."""
    lo, hi = sp / 2, size - sp / 2
    rl, rr, ru, rd = reach or (0.45 * sp,) * 4
    arms = [(x - rl, y, lo, y), (x + rr, y, hi, y), (x, y - ru, x, lo), (x, y + rd, x, hi)]
    return [(a, b, a + (c - a) * q, b + (d - b) * q) for a, b, c, d in arms
            if (c - a) * (c - x) + (d - b) * (d - y) > 0]


def guides(t, side, x, y, sp, size, q=1.0, reach=None):
    """Drag's guides in the ink of the side to move (or in Pencil while a booster is aimed: side "pencil"):
    1.5 dp, dashed, each arm's dashes starting at the ring, or at the booster's mark."""
    return "".join(el("line", x1=f(a), y1=f(b), x2=f(c), y2=f(d), stroke=t[side], stroke_width=1.5,
                      stroke_dasharray=f"{DRAG_DASH[0]} {DRAG_DASH[1]}")
                   for a, b, c, d in guide_arms(x, y, sp, size, q, reach) if abs(c - a) + abs(d - b) > 0.1)


def preview(t, side, x, y, sp, dot):
    """The preview: the moving side's piece in its pale colour inside a ring in its ink (pieces.md)."""
    if side == "red":
        piece = el("circle", cx=f(x), cy=f(y), r=f(dot / 2), fill=t["red_pale"])
    else:
        h = dot * 7 / 12
        piece = el("path", d=f"M{f(x)} {f(y - h)}L{f(x + h)} {f(y)}L{f(x)} {f(y + h)}L{f(x - h)} {f(y)}Z",
                   fill=t["blue_pale"])
    return piece + el("circle", cx=f(x), cy=f(y), r=f(0.45 * sp), fill="none", stroke=t[side], stroke_width=2)


def bowed_controls(points, seed):
    """Each side of a pen-stroke ring: (start, the quadratic's control point, end, width at 2 dp)."""
    rnd = random.Random(seed)
    out = []
    n = len(points)
    for i in range(n):
        (x1, y1), (x2, y2) = points[i], points[(i + 1) % n]
        length = math.hypot(x2 - x1, y2 - y1)
        nx, ny = -(y2 - y1) / length, (x2 - x1) / length
        k = rnd.uniform(-0.4, 0.4) * 2
        q = ((x1 + x2) / 2 + nx * k, (y1 + y2) / 2 + ny * k)
        out.append(((x1, y1), q, (x2, y2), 2 * (1 + rnd.uniform(-0.15, 0.15))))
    return out


def bowed_segments(points, seed):
    """The ring as a pen stroke: each side bows by up to 0.4 dp and varies in width by up to 15%."""
    segs, path = [], []
    for i, ((x1, y1), (qx, qy), (x2, y2), w) in enumerate(bowed_controls(points, seed)):
        segs.append((f"M{f(x1)} {f(y1)} Q{f(qx)} {f(qy)} {f(x2)} {f(y2)}", w))
        path.append((f"M{f(x1)} {f(y1)} " if i == 0 else "") + f"Q{f(qx)} {f(qy)} {f(x2)} {f(y2)}")
    return segs, " ".join(path) + " Z"


def board_svg(t, key, uid, size=360, sp=24.0, mini=False, links=False):
    """The board of moment key. Links between touching dots of one side are drawn only when links is
    True: the player turns them on in Settings, and they are off by default."""
    pos = POSITIONS[key]
    c = lambda p: (sp / 2 + p[0] * sp, sp / 2 + p[1] * sp)  # noqa: E731
    dot = min(20.0, max(9.0, sp / 2)) if not mini else sp * 0.62
    ring_w = 1 if mini else 1
    captured = {p for a in pos["areas"] for p in a["inside"]}
    defs, washes, rings = [], [], []
    for i, a in enumerate(pos["areas"]):
        pts = [c(p) for p in a["ring"]]
        segs, outline = bowed_segments(pts, seed=str(a["ring"][0]))
        wash, pale = t[a["captor"] + "_wash"], t[a["captor"] + "_pale"]
        angle = -45 if a["captor"] == "red" else 45
        pid = f"h{uid}{i}"
        defs.append(f'<pattern id="{pid}" width="5" height="5" patternUnits="userSpaceOnUse" '
                    f'patternTransform="rotate({angle})">' + el("rect", width=5, height=5, fill=wash) +
                    el("line", x1=0, y1=2.5, x2=5, y2=2.5, stroke=pale, stroke_width=1) + "</pattern>")
        washes.append(el("path", d=outline, fill=f"url(#{pid})"))
        for d, w in segs:
            rings.append(el("path", d=d, fill="none", stroke=t[a["captor"]], stroke_width=f(w * ring_w if not mini else 1),
                            stroke_linecap="round"))
    grid = []
    for i in range(15):
        edge = i in (0, 14)
        x = sp / 2 + i * sp
        sw = (1.5 if edge else 1) if not mini else (0.75 if edge else 0.5)
        grid.append(el("line", x1=f(x), y1=f(sp / 2), x2=f(x), y2=f(size - sp / 2), stroke=t["grid"], stroke_width=sw))
        grid.append(el("line", x1=f(sp / 2), y1=f(x), x2=f(size - sp / 2), y2=f(x), stroke=t["grid"], stroke_width=sw))
    link_lines = []
    if links and not mini:
        for side in ("red", "blue"):
            live = sorted(p for p in pos[side] if p not in captured)
            for i, p in enumerate(live):
                for q in live[i + 1:]:
                    if max(abs(p[0] - q[0]), abs(p[1] - q[1])) == 1:
                        (x1, y1), (x2, y2) = c(p), c(q)
                        link_lines.append(el("line", x1=f(x1), y1=f(y1), x2=f(x2), y2=f(y2),
                                        stroke=t[side + "_pale"], stroke_width=1.5, stroke_linecap="round"))
    drag = []
    if pos.get("drag"):
        p, side = pos["drag"]
        drag.append(guides(t, side, *c(p), sp, size))
    aim = pos.get("aim")
    under = aim_under(t, pos, c, sp) if aim else ""
    if aim:
        drag.append(guides(t, "pencil", *c(aim["point"]), sp, size, reach=aim_reach(pos, sp)))
    marks = []
    for p in pos.get("near", []):
        x, y = c(p)
        marks.append(el("circle", cx=f(x), cy=f(y), r=f(0.42 * sp), fill="none", stroke=t["hi"], stroke_width=2))
    for p in pos.get("open", []):
        # After a stop (or on Stop's note): the stopper's dots not joined to the edge, in a dashed Highlighter ring.
        x, y = c(p)
        marks.append(el("circle", cx=f(x), cy=f(y), r=f(0.42 * sp), fill="none", stroke=t["hi"], stroke_width=1.5,
                        stroke_dasharray="3 2.5"))
    if aim:
        marks.append(aim_over(t, pos, c, sp))
    hits = aim_hits(pos) if aim and aim["booster"] == "bomb" else []
    pieces = []
    for side in ("red", "blue"):
        for p in pos[side]:
            x, y = c(p)
            if side == "red":
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(dot / 2), fill=t["red"]))
            else:
                # The bomb's hits show in Blue pale: its plus rings already mark them.
                h = dot * 7 / 12
                pieces.append(el("path", d=f"M{f(x)} {f(y - h)}L{f(x + h)} {f(y)}L{f(x)} {f(y + h)}L{f(x - h)} {f(y)}Z",
                                 fill=t["blue_pale"] if p in hits else t["blue"]))
            if p in captured and not mini:
                L = dot * 0.75 / math.sqrt(2)
                if side == "red":  # captured by blue: the cut falls to the right
                    d, col = f"M{f(x - L)} {f(y - L)}L{f(x + L)} {f(y + L)}", t["blue_wash"]
                else:
                    d, col = f"M{f(x - L)} {f(y + L)}L{f(x + L)} {f(y - L)}", t["red_wash"]
                pieces.append(el("path", d=d, stroke=col, stroke_width=f(dot / 6), fill="none"))
            if p == pos["last"] and not mini and p not in hits:
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(dot * 0.15), fill=t["paper"]))
    for p, side in pos.get("ghosts", []):
        # Undo's preview: the piece in its pale colour inside a dashed Graphite ring (nothing see-through).
        x, y = c(p)
        if side == "red":
            pieces.append(el("circle", cx=f(x), cy=f(y), r=f(dot / 2), fill=t["red_pale"]))
        else:
            h = dot * 7 / 12
            pieces.append(el("path", d=f"M{f(x)} {f(y - h)}L{f(x + h)} {f(y)}L{f(x)} {f(y + h)}L{f(x - h)} {f(y)}Z",
                             fill=t["blue_pale"]))
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(0.45 * sp), fill="none", stroke=t["graphite"],
                         stroke_width=1.5, stroke_dasharray="3 3"))
    for p, side in pos.get("closing", []):
        x, y = c(p)
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(sp / 6), fill="none", stroke=t[side], stroke_width=1.5))
    if pos.get("drag"):
        p, side = pos["drag"]
        pieces.append(preview(t, side, *c(p), sp, dot))
    if pos.get("burst") and not mini:
        pieces.extend(burst(t, pos, c))
    body = (f"<defs>{''.join(defs)}</defs>" + el("rect", width=size, height=size, fill=t["paper"]) +
            "".join(washes) + "".join(grid) + under + "".join(link_lines) + "".join(drag) + "".join(rings) + "".join(marks) +
            "".join(pieces))
    return svg(size, size, body, label=None if mini else "The board, 15 by 15 points")


def burst(t, pos, c):
    """The big capture's burst: short strokes in the captor's ink round the six, never across a piece."""
    out = []
    cx, cy = c((4.5, 5))
    for k in range(14):
        a = k * 2 * math.pi / 14 + 0.2
        r1, r2 = (64, 78) if k % 2 else (66, 74)
        ends = [(cx + math.cos(a) * r * 1.0, cy + math.sin(a) * r * 1.1) for r in (r1, (r1 + r2) / 2, r2)]
        if any(math.hypot(ex - c(p)[0], ey - c(p)[1]) < 12 for ex, ey in ends for p in pos["red"] + pos["blue"]):
            continue  # a stroke never crosses a piece
        out.append(el("line", x1=f(cx + math.cos(a) * r1), y1=f(cy + math.sin(a) * r1 * 1.1),
                      x2=f(cx + math.cos(a) * r2), y2=f(cy + math.sin(a) * r2 * 1.1),
                      stroke=t["red"], stroke_width=2.5, stroke_linecap="round"))
    return out


# ------------------------------------------------------- the board themes ----
# Star chart and Neon arcade draw the same position with their own pieces, rings and areas. Every
# size and the drawing order come from their theme pages; "contrast" is high-contrast pieces.

STAR_ACROSS, STAR_INNER = 1.4, 0.55              # star: points' circle in dot sizes; inner corners' share
HEAD_ACROSS, TAIL_LEAVES = 0.75, 70              # comet: head in dot sizes; where the tail leaves it, degrees
MIDDLE_STREAK, SIDE_STREAK, STREAK_SPREAD = 1.4, 1.05, 0.3
NOTCH, NOTCH_SPREAD = 0.65, 0.11
TRIANGLE_REACH = 0.66                            # triangle: corners from its centre, in dot sizes
GLOW, CORE, BAND, HATCH = 1.5, 1 / 3, 1.5, 6     # dp, share of the ring, dp, dp


def theme_dot(sp, contrast=False):
    return min(24.0, max(9.0, sp * 0.6)) if contrast else min(20.0, max(9.0, sp / 2))


def theme_ring(sp):
    return min(3.0, max(1.5, sp / 12))


def poly(pts):
    return "M" + " L".join(f"{f(x)} {f(y)}" for x, y in pts) + " Z"


# ------------------------------------------- piece styles and ring effects ----
# The cosmetics of docs/design/cosmetics.md (D-29). A piece style changes only the pieces and a ring
# effect only the rings, inside any theme and drawn with its inks. t["pieces"] and t["rings"] name
# them; without them a theme draws its own. Shapes are in dot sizes, centred on the point, y down.

def wound(pts):
    """pts turned one way round, so that overlapping parts of one piece add up (nonzero fill)."""
    area2 = sum(x1 * y2 - x2 * y1 for (x1, y1), (x2, y2) in zip(pts, pts[1:] + pts[:1]))
    return pts if area2 > 0 else pts[::-1]


def circle_pts(cx, cy, r, n=32):
    return [(cx + r * math.cos(2 * math.pi * k / n), cy + r * math.sin(2 * math.pi * k / n)) for k in range(n)]


def capsule_pts(angle, reach, width, n=10, start=None):
    """A bar with round ends at angle degrees: through the centre, its tips reach from it; or from start
    (x, y), reaching that far."""
    a, r = math.radians(angle), width / 2
    ux, uy, nx, ny = math.cos(a), math.sin(a), -math.sin(a), math.cos(a)
    if start is not None:
        mx, my = start[0] + ux * reach / 2, start[1] + uy * reach / 2
        return [(mx + px, my + py) for px, py in capsule_pts(angle, reach / 2, width, n)]
    out = []
    for end in (1, -1):
        cx, cy = ux * (reach - r) * end, uy * (reach - r) * end
        for k in range(n + 1):
            b = math.pi * k / n
            out.append((cx + (nx * math.cos(b) + ux * math.sin(b)) * r * end,
                        cy + (ny * math.cos(b) + uy * math.sin(b)) * r * end))
    return out


def cubic_pts(p0, c1, c2, p1, n=12):
    return [tuple((1 - s) ** 3 * a + 3 * (1 - s) ** 2 * s * b + 3 * (1 - s) * s ** 2 * c + s ** 3 * e
                  for a, b, c, e in zip(p0, c1, c2, p1)) for s in (k / n for k in range(n))]


HEART_AREA, HEART_N = 0.80, 64      # heart: area in square dot sizes; points round the curve


def heart_shape():
    """The heart x = 16 sin^3 t, y = -(13 cos t - 5 cos 2t - 2 cos 3t - cos 4t), scaled to HEART_AREA
    and centred on its centroid. Its area is 180 pi in its own units."""
    k = math.sqrt(HEART_AREA / (180 * math.pi))
    pts = [(16 * math.sin(s) ** 3, -(13 * math.cos(s) - 5 * math.cos(2 * s) - 2 * math.cos(3 * s) - math.cos(4 * s)))
           for s in (2 * math.pi * i / HEART_N for i in range(HEART_N))]
    a = sum(x1 * y2 - x2 * y1 for (x1, y1), (x2, y2) in zip(pts, pts[1:] + pts[:1])) / 2
    cy = sum((y1 + y2) * (x1 * y2 - x2 * y1) for (x1, y1), (x2, y2) in zip(pts, pts[1:] + pts[:1])) / (6 * a)
    return [[(x * k, (y - cy) * k) for x, y in pts]]


CLUB_LOBE, CLUB_REACH, CLUB_HUB = 0.27, 0.30, 0.20                   # club: lobes, their centres, the hub
CLUB_STEM = [(-0.07, 0.15), (0.07, 0.15), (0.22, 0.58), (-0.22, 0.58)]  # its foot


def club_shape():
    """Three round lobes, up, lower left and lower right, round a hub, on a foot."""
    lobes = [circle_pts(CLUB_REACH * math.cos(math.radians(a)), CLUB_REACH * math.sin(math.radians(a)), CLUB_LOBE)
             for a in (-90, 30, 150)]
    return lobes + [circle_pts(0, 0, CLUB_HUB), CLUB_STEM]


FLAME = [  # cubic Bezier sides, from the main tip clockwise: start, two controls; the next start ends each
    ((0.08, -0.68), (0.34, -0.42), (0.50, -0.16)),
    ((0.48, 0.12), (0.47, 0.37), (0.26, 0.52)),
    ((0.00, 0.52), (-0.27, 0.52), (-0.48, 0.36)),
    ((-0.48, 0.10), (-0.48, -0.12), (-0.43, -0.30)),
    ((-0.34, -0.46), (-0.28, -0.30), (-0.20, -0.20)),
    ((-0.12, -0.16), (-0.13, -0.40), (-0.05, -0.55)),
]
FROST_ARM, FROST_REACH, FROST_HUB = 0.22, 0.62, 0.24    # snowflake: arm width, tips from the centre, hub
FROST_TWIG, FROST_TWIG_AT, FROST_TWIG_LONG = 0.15, 0.36, 0.20  # its twigs: width, where on the arm, length


def flame_shape():
    """A flame, point up and leaning a little right, with a smaller tongue on its left."""
    pts = []
    for i, (p0, c1, c2) in enumerate(FLAME):
        pts += cubic_pts(p0, c1, c2, FLAME[(i + 1) % len(FLAME)][0])
    return [pts]


def frost_shape():
    """A snowflake: six arms with round tips, one straight up, round a hub, each arm with a pair of
    twigs pointing out at 50 degrees."""
    twigs = []
    for a in range(-90, 270, 60):
        base = (FROST_TWIG_AT * math.cos(math.radians(a)), FROST_TWIG_AT * math.sin(math.radians(a)))
        twigs += [capsule_pts(a + turn, FROST_TWIG_LONG, FROST_TWIG, 6, start=base) for turn in (-50, 50)]
    return [capsule_pts(a, FROST_REACH, FROST_ARM) for a in (90, 30, -30)] + [circle_pts(0, 0, FROST_HUB)] + twigs


SHAPES = {"heart": heart_shape(), "club": club_shape(), "flame": flame_shape(), "frost": frost_shape()}
PIECE_STYLES = {  # piece style: (the red side's shape, the blue side's)
    "cards": ("heart", "club"),
    "fire": ("flame", "frost"),
}


def shape_d(name, x, y, d):
    return "".join(poly([(x + px * d, y + py * d) for px, py in wound(part)]) for part in SHAPES[name])


def piece_d(t, side, x, y, d):
    """The outline of a piece in a board theme, as an SVG path."""
    if t.get("pieces"):
        return shape_d(PIECE_STYLES[t["pieces"]][side != "red"], x, y, d)
    if t["style"] == "notebook" and side == "blue":  # Notebook's diamond, 7/6 of the dot size
        h = d * 7 / 12
        return f"M{f(x)} {f(y - h)}L{f(x + h)} {f(y)}L{f(x)} {f(y + h)}L{f(x - h)} {f(y)}Z"
    if t["style"] == "star" and side == "red":  # a five-pointed star, one point up
        r = d * STAR_ACROSS / 2
        return poly([(x + (r if k % 2 == 0 else r * STAR_INNER) * math.cos(math.radians(-90 + 36 * k)),
                      y + (r if k % 2 == 0 else r * STAR_INNER) * math.sin(math.radians(-90 + 36 * k)))
                     for k in range(10)])
    if t["style"] == "star":  # a comet: a round head and three streaks up and to the left
        h = d * HEAD_ACROSS / 2
        ux = uy = -1 / math.sqrt(2)
        nx, ny = -uy, ux
        at = lambda a, c: (x + (ux * a + nx * c) * d, y + (uy * a + ny * c) * d)  # noqa: E731
        co, si = math.cos(math.radians(TAIL_LEAVES)), math.sin(math.radians(TAIL_LEAVES))
        a = (x + (ux * co + nx * si) * h, y + (uy * co + ny * si) * h)
        b = (x + (ux * co - nx * si) * h, y + (uy * co - ny * si) * h)
        pts = [a, at(SIDE_STREAK, STREAK_SPREAD), at(NOTCH, NOTCH_SPREAD), at(MIDDLE_STREAK, 0),
               at(NOTCH, -NOTCH_SPREAD), at(SIDE_STREAK, -STREAK_SPREAD), b]
        return poly(pts)[:-2] + f" A{f(h)} {f(h)} 0 1 0 {f(a[0])} {f(a[1])} Z"
    if side == "red":  # a disc
        r = d / 2
        return f"M{f(x - r)} {f(y)} A{f(r)} {f(r)} 0 1 0 {f(x + r)} {f(y)} A{f(r)} {f(r)} 0 1 0 {f(x - r)} {f(y)} Z"
    r = d * TRIANGLE_REACH  # a triangle, corner up, its centre on the point
    return poly([(x + r * math.cos(math.radians(a)), y + r * math.sin(math.radians(a))) for a in (-90, 30, 150)])


def band(t, side, x, y, d, colour, width, clip=None):
    """A band of width dp round a piece's outline, with round corners: a glow, or a high-contrast band."""
    return el("path", d=piece_d(t, side, x, y, d), fill=colour, stroke=colour, stroke_width=f(2 * width),
              stroke_linejoin="round", clip_path=clip)


def theme_piece(t, side, size):
    """A side's piece beside its name in the score row, at a 12 dp dot, without glow."""
    comet = t["style"] == "star" and side == "blue" and not t.get("pieces")
    x, y = (size - 5.5, size - 5.5) if comet else (size / 2, size / 2)
    return svg(size, size, el("path", d=piece_d(t, side, x, y, 12), fill=t[side]))


LASSO_WIDTH, LASSO_TWIST, LASSO_SLANT, LASSO_STRIPE = 1.6, 4, 55, 0.3  # Lasso, in ring widths and twists per grid step
STAMP_GAP = 2.5                                                      # Stamp: dp between the ring's edge and its inner line


def ring_sides(t, pts, seed):
    """Each side of a ring: (start, the quadratic's control point or None, end, width as a share of the ring's)."""
    if t["style"] == "notebook":
        return [(a, q, b, wf / 2) for a, q, b, wf in bowed_controls(pts, seed)]
    return [(pts[i], None, pts[(i + 1) % len(pts)], 1.0) for i in range(len(pts))]


def rope(sides, w, sp, ink, pale):
    """Lasso's ring: a cord LASSO_WIDTH ring widths wide in the captor's ink, crossed by slanted twists in
    its pale colour, a whole number of them on each side of the ring."""
    half = LASSO_WIDTH * w / 2
    cord, twists = [], []
    for a, q, b, _ in sides:
        q = q or ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
        cord.append(f"M{f(a[0])} {f(a[1])} Q{f(q[0])} {f(q[1])} {f(b[0])} {f(b[1])}")
        length = math.hypot(b[0] - a[0], b[1] - a[1])
        n = max(1, round(length / (sp / LASSO_TWIST)))
        for j in range(n):
            s = (j + 0.5) / n
            x = (1 - s) ** 2 * a[0] + 2 * (1 - s) * s * q[0] + s ** 2 * b[0]
            y = (1 - s) ** 2 * a[1] + 2 * (1 - s) * s * q[1] + s ** 2 * b[1]
            dx = 2 * (1 - s) * (q[0] - a[0]) + 2 * s * (b[0] - q[0])
            dy = 2 * (1 - s) * (q[1] - a[1]) + 2 * s * (b[1] - q[1])
            norm = math.hypot(dx, dy)
            ux, uy = dx / norm, dy / norm
            r = math.radians(LASSO_SLANT)  # the twist leans LASSO_SLANT degrees from the cord's line
            vx, vy = ux * math.cos(r) - uy * math.sin(r), ux * math.sin(r) + uy * math.cos(r)
            k = half * 0.92 / math.sin(r)
            twists.append(f"M{f(x - vx * k)} {f(y - vy * k)}L{f(x + vx * k)} {f(y + vy * k)}")
    return [el("path", d=" ".join(cord), fill="none", stroke=ink, stroke_width=f(2 * half), stroke_linecap="round"),
            el("path", d=" ".join(twists), fill="none", stroke=pale, stroke_width=f(LASSO_STRIPE * w))]


def mix(a, b, share):
    """Colour a blended share of the way to colour b (solid colours: nothing on the board is see-through)."""
    if share >= 1 or b is None:
        return b
    ca, cb = (tuple(int(h.lstrip("#")[i:i + 2], 16) for i in (0, 2, 4)) for h in (a, b))
    return "#" + "".join(f"{round(x + (y - x) * share):02X}" for x, y in zip(ca, cb))


def part_of(sides, share):
    """The first share of a ring's sides, as drawn so far, the last side cut where the pen has got to."""
    lengths = [math.hypot(b[0] - a[0], b[1] - a[1]) for a, _, b, _ in sides]
    left, out = share * sum(lengths), []
    for (a, q, b, wf), length in zip(sides, lengths):
        if left <= 0:
            break
        if left >= length:
            out.append((a, q, b, wf))
        else:
            q, s = q or ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2), left / length
            aq = (a[0] + (q[0] - a[0]) * s, a[1] + (q[1] - a[1]) * s)
            qb = (q[0] + (b[0] - q[0]) * s, q[1] + (b[1] - q[1]) * s)
            out.append((a, aq, (aq[0] + (qb[0] - aq[0]) * s, aq[1] + (qb[1] - aq[1]) * s), wf))
        left -= length
    return out


def theme_board_svg(t, key, uid, size=360, sp=24.0, links=False, contrast=False):
    """The board of moment key (or a position) in a board theme, in its page's drawing order. Notebook
    draws here too when a piece style or a ring effect is in use."""
    pos = POSITIONS[key] if isinstance(key, str) else key
    n = round(size / sp)
    c = lambda p: (sp / 2 + p[0] * sp, sp / 2 + p[1] * sp)  # noqa: E731
    d, w, star = theme_dot(sp, contrast), theme_ring(sp), t["style"] == "star"
    nb, neon, effect = t["style"] == "notebook", t["style"] == "neon", t.get("rings")
    hatch = 5 if nb else HATCH
    captured = {p: a["captor"] for a in pos["areas"] for p in a["inside"]}
    defs, fills, glows, tubes, cores = [], [], [], [], []
    for i, a in enumerate(pos["areas"]):
        pts, k = [c(p) for p in a["ring"]], a["captor"]
        if a.get("scale", 1) != 1:  # a ring on its way to its place (the motion strips)
            mx, my = sum(x for x, _ in pts) / len(pts), sum(y for _, y in pts) / len(pts)
            pts = [(mx + (x - mx) * a["scale"], my + (y - my) * a["scale"]) for x, y in pts]
        ink, wash, pale, core = (mix(t["paper"], t.get(k + x), a.get("tint", 1)) for x in ("", "_wash", "_pale", "_core"))
        sides = ring_sides(t, pts, str(a["ring"][0]))
        drawn = sides if a.get("upto", 1) >= 1 else part_of(sides[a["start"]:] + sides[:a["start"]], a["upto"])
        outline = bowed_segments(pts, str(a["ring"][0]))[1] if nb else poly(pts)
        traced = outline if drawn is sides else "".join(
            (f"M{f(a_[0])} {f(a_[1])}" if j == 0 else "") + (f" Q{f(q[0])} {f(q[1])} {f(b[0])} {f(b[1])}" if q else
                                                              f" L{f(b[0])} {f(b[1])}") for j, (a_, q, b, _) in enumerate(drawn))
        lit = neon and a.get("lit", True)
        angle = -45 if k == "red" else 45  # red's areas rise to the right (/), blue's fall (\)
        defs.append(f'<clipPath id="c{uid}{i}">{el("path", d=outline)}</clipPath>'
                    f'<pattern id="h{uid}{i}" width="{hatch}" height="{hatch}" patternUnits="userSpaceOnUse" '
                    f'patternTransform="rotate({angle})">' + el("rect", width=hatch, height=hatch, fill=wash) +
                    el("line", x1=0, y1=hatch / 2, x2=hatch, y2=hatch / 2, stroke=pale, stroke_width=1) +
                    "</pattern>")
        if a.get("fill", 1) >= 1:
            fills.append(el("path", d=outline, fill=f"url(#h{uid}{i})"))
        elif a["fill"] > 0:  # the wash spreading from the closing piece
            ox, oy = c(a["ring"][a["start"]])
            r = a["fill"] * max(math.hypot(x - ox, y - oy) for x, y in pts)
            defs.append(f'<clipPath id="f{uid}{i}">{el("circle", cx=f(ox), cy=f(oy), r=f(r))}</clipPath>')
            fills.append(el("path", d=outline, fill=f"url(#h{uid}{i})", clip_path=f"url(#f{uid}{i})"))
        line = lambda colour, width: el("path", d=traced, fill="none", stroke=colour, stroke_width=f(width),  # noqa: E731
                                        stroke_linejoin="round", stroke_linecap="round")
        if effect == "lasso":  # a cord with slanted twists, in a neon glow as wide as the cord
            if lit:
                glows.append(line(pale, LASSO_WIDTH * w + 2 * GLOW))
            tubes.extend(rope(drawn, w, sp, ink, pale))
            continue
        if lit:  # a neon tube: glow, tube and core, each drawn for every ring in turn
            glows.append(line(pale, w + 2 * GLOW))
            cores.append(line(core, w * CORE))
        if nb:  # Notebook's pen stroke, side by side
            tubes.extend(el("path", d=f"M{f(a_[0])} {f(a_[1])} Q{f(q[0])} {f(q[1])} {f(b[0])} {f(b[1])}", fill="none",
                            stroke=ink, stroke_width=f(share * w), stroke_linecap="round") for a_, q, b, share in drawn)
        else:
            tubes.append(line(ink, w))
        if effect == "stamp":  # a thin inner line in the captor's ink, STAMP_GAP inside the ring's edge
            iw = max(1.0, w / 2)
            g = w / 2 + STAMP_GAP + iw / 2
            defs.append(f'<mask id="m{uid}{i}" maskUnits="userSpaceOnUse" x="0" y="0" width="{f(size)}" '
                        f'height="{f(size)}">' + el("path", d=outline, fill="#FFFFFF") +
                        el("path", d=outline, fill="none", stroke="#000000", stroke_width=f(2 * g - iw),
                           stroke_linejoin="round") + "</mask>")
            tubes.append(el("path", d=outline, fill="none", stroke=ink, stroke_width=f(2 * g + iw),
                            stroke_linejoin="round", mask=f"url(#m{uid}{i})"))
    grid = []
    for i in range(n):
        x = sp / 2 + i * sp
        sw = 1.5 if i in (0, n - 1) else 1
        grid.append(el("line", x1=f(x), y1=f(sp / 2), x2=f(x), y2=f(size - sp / 2), stroke=t["grid"], stroke_width=sw))
        grid.append(el("line", x1=f(sp / 2), y1=f(x), x2=f(size - sp / 2), y2=f(x), stroke=t["grid"], stroke_width=sw))
    if star:  # a chart point at every crossing; a piece covers its own
        grid += [el("circle", cx=f(sp / 2 + i * sp), cy=f(sp / 2 + j * sp), r=1, fill=t["point"])
                 for i in range(n) for j in range(n)]
    link_lines = []
    if links:
        for side in ("red", "blue"):
            live = sorted(p for p in pos[side] if p not in captured)
            for i, p in enumerate(live):
                for q in live[i + 1:]:
                    if max(abs(p[0] - q[0]), abs(p[1] - q[1])) == 1:
                        (x1, y1), (x2, y2) = c(p), c(q)
                        link_lines.append(el("line", x1=f(x1), y1=f(y1), x2=f(x2), y2=f(y2),
                                             stroke=t[side + "_pale"], stroke_width=1.5, stroke_linecap="round"))
    drag = [guides(t, pos["drag"][1], *c(pos["drag"][0]), sp, size)] if pos.get("drag") else []
    # Bands: a neon piece's glow (live pieces only), or with high-contrast pieces the surface under
    # each piece: Paper, the wash of the area it lies in, or both halves for a piece on a ring.
    bands, pieces = [], []
    rings_of = {p: i for i, a in enumerate(pos["areas"]) for p in a["ring"]}
    for side in ("red", "blue"):
        for p in pos[side]:
            x, y = c(p)
            if contrast:
                surface = t[captured[p] + "_wash"] if p in captured else t["paper"]
                bands.append(band(t, side, x, y, d, surface, BAND))
                if p in rings_of:
                    i = rings_of[p]
                    bands.append(band(t, side, x, y, d, t[pos["areas"][i]["captor"] + "_wash"], BAND,
                                      clip=f"url(#c{uid}{i})"))
            elif neon and p not in captured or neon and p not in pos.get("cuts", captured):
                bands.append(band(t, side, x, y, d, t[side + "_pale"], GLOW))
            pieces.append(el("path", d=piece_d(t, side, x, y, d), fill=t[side]))
            if p in captured and p in pos.get("cuts", captured):
                L = d * 0.75 / math.sqrt(2)
                if side == "red":  # in blue's area: the cut falls, along blue's hatching
                    cut, col = f"M{f(x - L)} {f(y - L)}L{f(x + L)} {f(y + L)}", t["blue_wash"]
                else:
                    cut, col = f"M{f(x - L)} {f(y + L)}L{f(x + L)} {f(y - L)}", t["red_wash"]
                pieces.append(el("path", d=cut, stroke=col, stroke_width=f(d / 6), fill="none"))
            if p == pos["last"]:
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(d * 0.15), fill=t["paper"]))
    marks = [el("circle", cx=f(c(p)[0]), cy=f(c(p)[1]), r=f(0.42 * sp), fill="none", stroke=t["hi_faint"],
                stroke_width=1.5) for p in pos.get("far", [])]
    marks += [el("circle", cx=f(c(p)[0]), cy=f(c(p)[1]), r=f(0.42 * sp), fill="none", stroke=t["hi"], stroke_width=2)
              for p in pos.get("near", [])]
    for p, side in pos.get("ghosts", []):
        x, y = c(p)
        pieces.append(el("path", d=piece_d(t, side, x, y, d), fill=t[side + "_pale"]))
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(0.45 * sp), fill="none", stroke=t["graphite"],
                         stroke_width=1.5, stroke_dasharray="3 3"))
    for p, side in pos.get("closing", []):
        x, y = c(p)
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(sp / 6), fill="none", stroke=t[side], stroke_width=1.5))
    if pos.get("drag") or pos.get("preview"):
        p, side = pos.get("drag") or pos["preview"]
        x, y = c(p)
        pieces.append(el("path", d=piece_d(t, side, x, y, d), fill=t[side + "_pale"],
                         stroke=t[side] if contrast else None, stroke_width=1.5 if contrast else None,
                         stroke_linejoin="round" if contrast else None))
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(0.45 * sp), fill="none", stroke=t[side], stroke_width=f(w)))
    if pos.get("burst"):
        pieces.extend(burst(t, pos, c))
    body = (f"<defs>{''.join(defs)}</defs>" + el("rect", width=size, height=size, fill=t["paper"]) +
            "".join(fills + grid + link_lines + drag + bands + glows + tubes + cores + marks + pieces))
    return svg(size, size, body, label=f"The board, {n} by {n} points")


def any_board_svg(t, key, uid, size=360, sp=24.0, links=False, contrast=False):
    """Notebook's board, or a board theme's, or any board with a piece style or a ring effect."""
    if t.get("style") or t.get("pieces") or t.get("rings"):
        t = dict(t, style=t.get("style", "notebook"))
        return theme_board_svg(t, key, uid, size, sp, links, contrast)
    return board_svg(t, key, uid, size, sp, links=links)


# ------------------------------------------------------------- drawn parts ----

def icon(d, ink, size=24, sw=1.8, fill="none", dots=()):
    body = el("path", d=d, fill=fill, stroke=ink, stroke_width=sw, stroke_linecap="round", stroke_linejoin="round")
    body += "".join(el("circle", cx=x, cy=y, r=r, fill=ink) for x, y, r in dots)
    return svg(size, size, body, view="0 0 24 24")


def named_icon(name, ink, size=24, sw=1.8):
    """An icon from ICONS, with its filled dots from ICON_DOTS."""
    return icon(ICONS[name], ink, size, sw, dots=ICON_DOTS.get(name, ()))


ICONS = {
    "pause": "M12 2.5 q9.5 .3 9.5 9.5 q-.3 9.5 -9.5 9.5 q-9.5 -.3 -9.5 -9.5 q.3 -9.3 9.5 -9.5 M9.5 8 q-.3 4 0 8 M14.5 8 q.3 4 0 8",
    "wall": "M3 7.5h18v11H3z M3 13h18 M9 7.5v5.5 M15 13v5.5 M6 13v5.5 M18 7.5v5.5",
    "hint": "M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 3z M12 0.5v0 M3 4l1 1 M21 4l-1 1",
    "bomb": "M11 21a7 7 0 1 0 0-14 7 7 0 0 0 0 14z M15 8l3-3 M18 5l1.5-1.5 M19.5 7.5h2 M17 2.5V1 M8 12.5a3 3 0 0 1 3-2.5",
    "shop": "M5 8h14l-1.5 12h-11z M9 8a3 3 0 0 1 6 0",
    "stats": "M5 20V12M10 20V6M15 20v-9M20 20V9M3 20h18",
    "help": "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M9.6 9.3a2.5 2.5 0 1 1 3.5 2.3c-.7.3-1.1.9-1.1 1.6M12 16.6h.01",
    "settings": "M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1",
    "close": "M6 6l12 12M18 6L6 18",
    "retry": "M20 12a8 8 0 1 1-2.3-5.7 M20 4v5h-5",
    "next": "M3 12h16M13 6l6 6-6 6",
    "target": "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9z M12 12.5v-1",
    "calendar": "M3.5 5h17v15h-17z M3.5 10h17M8 3v4M16 3v4",
    # Stop: a flag on its pole, waving, as a player plants it to say "my dots stay where they are".
    "flag": "M6 21.5V3 M6 4.5q3 -1.8 6 0t6.5 0v8.5q-3.5 1.8 -6.5 0t-6 0",
    "tick": "M4.5 12.5l4.5 4.5 10.5-10.5",
    # Free play's plain Undo: the arrow alone.
    "back": "M9 14 L4 9 L9 4 M4 9 h10.5 a5.5 5.5 0 0 1 0 11 H11",
    # Undo: a dot at the bottom left, and a pen stroke from it that loops up and back to an arrowhead
    # pointing left: the dot goes back. Free play's plain Undo button is the arrow alone.
    "undo": "M10.6 19.6 q2.4 -.2 4.5 -.1 q5.3 .1 5.4 -5.5 q-.1 -5.4 -5.5 -5.5 q-4.7 .1 -9.4 0 "
            "M8.9 4.7 q-2 2 -3.7 3.8 q1.7 1.8 3.8 3.8",
}
ICON_DOTS = {"undo": [(7, 19.6, 2.4)]}
STAR = "M12 2.8l2.7 5.6 6.1.8-4.5 4.2 1.1 6.1L12 16.6l-5.4 2.9 1.1-6.1-4.5-4.2 6.1-.8z"


def star(t, size, filled, uid=""):
    if filled:
        body = (el("path", d=STAR, fill=t["red"], stroke=t["red"], stroke_width=1.6, stroke_linejoin="round"))
    else:
        body = el("path", d=STAR, fill="none", stroke=t["graphite"], stroke_width=1.6, stroke_linejoin="round")
    return svg(size, size, body, view="0 0 24 24")


def tally(n, ink):
    d = []
    for i in range(n):
        g, k = divmod(i, 5)
        x0 = g * 24
        if k < 4:
            x = x0 + 2 + k * 4.5
            d.append(f"M{f(x)} {2 + (k % 2) * 0.6}q{(-1) ** k * 0.6} 7 0 {14 - (k % 2)}")
        else:
            d.append(f"M{x0 - 1} 14 Q{x0 + 9} 9 {x0 + 19} 3")
    width = max(24, math.ceil(n / 5) * 24)
    return svg(width, 18, el("path", d=" ".join(d), fill="none", stroke=ink, stroke_width=1.8, stroke_linecap="round"))


def underline(ink, width):
    return svg(width, 8, el("path", d=f"M2 5 Q{width * 0.4} 1 {width * 0.7} 4 T{width - 2} 3", fill="none",
                            stroke=ink, stroke_width=2.2, stroke_linecap="round"))


def face(t, mood, size):
    """The computer: a round blot of blue ink. Eyes keep their own colours in both themes."""
    blob = ("M50 18 C68 16 84 30 85 50 C87 60 84 70 78 77 C80 84 78 90 74 90 C70 90 70 84 68 82 "
            "C60 87 48 88 38 85 C26 82 16 72 15 57 C13 36 30 19 50 18 Z")
    ink = t.get("face_body", t["blue"])
    parts = [
        el("ellipse", cx=50, cy=93, rx=30, ry=4, fill=t.get("face_shadow", t["blue_pale"])),
        el("path", d=blob, fill=ink),
        el("circle", cx=88, cy=26, r=3.5, fill=ink),
        el("circle", cx=14, cy=82, r=2.5, fill=ink),
        el("circle", cx=93, cy=38, r=2, fill=ink),
    ]
    white, pupil, line = "#FBF8F1", "#2E2E2E", t.get("face_line", t["paper"])
    eyes = {"happy": (0, 0, 9), "smug": (3, 1, 9), "upset": (0, 2, 7), "determined": (-2, 0, 8), "sulky": (-4, 2, 8)}[mood]
    for ex in (37, 63):
        parts.append(el("ellipse", cx=ex, cy=46, rx=8, ry=eyes[2], fill=white))
        parts.append(el("circle", cx=ex + eyes[0], cy=47 + eyes[1], r=3.6, fill=pupil))
        parts.append(el("circle", cx=ex + eyes[0] + 1.3, cy=45.5 + eyes[1], r=1.1, fill=white))
    if mood == "smug":  # heavy lids
        parts.append(el("path", d="M28 44 Q37 36 46 44 Z M54 44 Q63 36 72 44 Z", fill=ink))
        parts.append(el("path", d="M28 44 H46 M54 44 H72", stroke=line, stroke_width=2, stroke_linecap="round"))
    if mood == "sulky":
        parts.append(el("path", d="M28 42 Q37 38 46 42 L46 40 Q37 33 28 40Z M54 42 Q63 38 72 42 L72 40 Q63 33 54 40Z", fill=ink))
    brows = {"happy": "M30 32 Q37 27 44 31 M56 31 Q63 27 70 32", "smug": "M29 35 L44 35 M56 31 Q63 25 71 30",
             "upset": "M30 33 L43 37 M70 33 L57 37", "determined": "M30 32 L44 37 M70 32 L56 37",
             "sulky": "M30 34 Q37 31 44 34 M56 34 Q63 31 70 34"}[mood]
    mouth = {"happy": "M36 64 Q50 78 64 64", "smug": "M39 67 Q52 72 63 61", "upset": "M38 73 Q50 63 62 73",
             "determined": "M40 68 H60", "sulky": "M41 69 Q45 66 50 69 T59 69"}[mood]
    parts.append(el("path", d=brows + " " + mouth, fill="none", stroke=line, stroke_width=2.6, stroke_linecap="round"))
    if mood == "upset":
        parts.append(el("path", d="M74 52 q4 7 0 9 q-4 -2 0 -9z", fill=white))
    return svg(size, size, "".join(parts), view="0 0 100 100")


def tape(t, left, top, w=52, angle=-3):
    return (f'<span aria-hidden="true" style="position: absolute; left: {left}px; top: {top}px; width: {w}px; '
            f'height: 16px; background: {t.get("tape", t["hi_faint"])}; transform: rotate({angle}deg)"></span>')


def note_style(t, angle):
    return (f'background: {t["card"]}; box-shadow: 0 2px 10px {t["shadow"]}; transform: rotate({angle}deg); '
            f'color: {t["pencil"]}')


def main_button(t, text, extra="", icon_d=None, width="100%"):
    ic = icon(icon_d, t["paper"], 22, 2.2) if icon_d else ""
    return (f'<button type="button" style="width: {width}; height: 56px; border: none; {BUTTON_SHAPE}; '
            f'background: {t["blue"]}; color: {t["paper"]}; font-family: inherit; {TITLE}; display: flex; '
            f'align-items: center; justify-content: center; gap: 8px; padding: 0; {extra}">{text}{ic}</button>')


def icon_button(t, name, label, ink=None, size=48, isize=26, style=""):
    return (f'<button type="button" aria-label="{label}" style="width: {size}px; height: {size}px; border: none; '
            f'background: transparent; padding: 0; display: flex; align-items: center; justify-content: center; '
            f'{style}">{icon(ICONS[name], ink or t["pencil"], isize)}</button>')


def root(t, inner, grid=False, margin=False, w=W, h=H):
    bg = t["paper"]
    if grid:
        bg_img = (f'background-image: linear-gradient({t["grid"]} 1px, transparent 1px), '
                  f'linear-gradient(90deg, {t["grid"]} 1px, transparent 1px); background-size: 24px 24px; '
                  f'background-position: -1px -1px;')
    else:
        bg_img = ""
    m = (f'<div aria-hidden="true" style="position: absolute; left: 58px; top: 0; bottom: 0; width: 1px; '
         f'background: {t["margin"]}"></div>') if margin else ""
    return (f'<div style="position: relative; width: {w}px; height: {h}px; overflow: hidden; background-color: {bg}; '
            f'{bg_img} font-family: Nunito, system-ui, sans-serif; color: {t["pencil"]}">{m}{inner}</div>')


# -------------------------------------------------------------- game screen ----

MOMENTS = {
    "opening": dict(mood="happy", bubble="Good luck!", you=0, opp=0, left=30, mission=0),
    "threat": dict(mood="smug", bubble="Your move…", you=2, opp=1, left=13, mission=2),
    "big": dict(mood="upset", bubble="Ouch!", you=8, opp=1, left=12, mission=8),
    "win": dict(mood="sulky", bubble="Well played.", you=8, opp=1, left=12, mission=8),
    "undo": dict(mood="smug", bubble="Take it back?", you=2, opp=1, left=13, mission=2),
    "drag": dict(mood="happy", bubble="Good luck!", you=0, opp=0, left=30, mission=0),
    "wall-aim": dict(mood="smug", bubble="Your move…", you=2, opp=1, left=13, mission=2),
    "bomb-aim": dict(mood="smug", bubble="Your move…", you=2, opp=1, left=13, mission=2),
}
NOTES = {  # a booster preview's note: icon, name, its one line, the main button
    "undo": ("undo", "Undo", "takes back the pale dots.", "Take back"),
    "wall-aim": ("wall", "Wall", "blocks one empty point.", "Block it"),
    "bomb-aim": ("bomb", "Bomb", "removes the computer’s dots in a +.", "Bomb it"),
}
BOARD_TOP = 308


def mission_note(t, done, right=12, top=30):
    boxes = []
    for i in range(8):
        tick = el("path", d="M2.5 6.5 l2.5 2.5 l5 -6", fill="none", stroke=t["paper"], stroke_width=1.8,
                  stroke_linecap="round", stroke_linejoin="round") if i < done else ""
        box = el("rect", x=0.75, y=0.75, width=10.5, height=10.5, rx=2.5,
                 fill=t["red"] if i < done else "none", stroke=t["red"] if i < done else t["graphite"], stroke_width=1.5)
        boxes.append(svg(12, 12, box + tick))
    head = "Mission done" if done >= 8 else "Mission"
    return (f'<div style="position: absolute; right: {right}px; top: {top}px; width: 136px; box-sizing: border-box; '
            f'padding: 8px 10px 9px; {note_style(t, 2)}">{tape(t, 44, -8, 44, -4)}'
            f'<div style="{SMALL}; color: {t["graphite"]}">{head}</div>'
            f'<div style="{LABEL}">Capture 8 dots</div>'
            f'<div style="display: flex; gap: 3px; margin-top: 3px">{"".join(boxes)}</div></div>')


def bubble(t, text, left, top):
    return (f'<div style="position: absolute; left: {left}px; top: {top}px; background: {t["card"]}; '
            f'border: 1.5px solid {t["blue"]}; border-radius: 16px 16px 16px 3px; padding: 2px 12px 4px; '
            f'{HAND24}; color: {t["blue"]}; white-space: nowrap">{text}</div>')


def side_row(t, side, name, score, turn, align):
    ink = t[side]
    if t.get("style"):
        shape = theme_piece(t, side, 18)
    else:
        shape = svg(14, 14, el("circle", cx=7, cy=7, r=6, fill=ink) if side == "red" else
                    el("path", d="M7 0 L14 7 L7 14 L0 7 Z", fill=ink))
    ul = (f'<span style="position: absolute; left: -4px; bottom: -7px">{underline(ink, len(name) * 11 + 10)}</span>'
          if turn else "")
    return (f'<div style="display: flex; align-items: center; gap: 8px; justify-content: {align}">'
            f'{shape}<span style="position: relative; {TITLE}; color: {ink}">{name}{ul}</span>'
            f'{tally(score, ink)}<span style="{SCORE}; color: {ink}">{score}</span></div>')


# The five boosters (decisions 0012 and 0023), as the player has them in Level 7. Undo is last, nearest
# the right thumb, in the same order as the engine and the Boosters screen.
BOOSTERS = [  # name, icon, count, the line on the Boosters screen
    ("Wall", "wall", 2, "Mark one empty point nobody can use."),
    ("Hint", "hint", 1, "The computer shows your best move."),
    ("Bomb", "bomb", 0, "Removes the computer's dots in a + of 5 points."),
    ("Double dot", None, 1, "Place two dots in one turn."),
    ("Undo", "undo", 1, "Take back your last dot and the computer's answer."),
]


def booster_pic(t, ic, ink, size=28):
    if ic:
        return named_icon(ic, ink, size)
    return svg(size + 2, size * 16 / 28, el("circle", cx=7, cy=8, r=6, fill=t["red"] if ink != t["graphite"] else ink) +
               el("path", d="M23 1 L30 8 L23 15 L16 8 Z", fill=t["red"] if ink != t["graphite"] else ink), view="0 0 30 16")


def toolbar(t, top=730, side=12, gap=10, undo_ready=True):
    """The tool bar under the player's line. Undo waits, grey with its count, until there is a turn to take back."""
    out = []
    for name, ic, n, _ in BOOSTERS:
        waiting = name == "Undo" and not undo_ready
        ink = t["blue"] if n and not waiting else t["graphite"]
        border = f"2px solid {t['blue_pale']}" if n else f"2px dashed {t['graphite']}"
        badge = (f'<span style="position: absolute; right: -6px; top: -7px; min-width: 22px; height: 22px; '
                 f'border-radius: 11px; background: {t["red"] if n else t["graphite"]}; color: {t["paper"]}; '
                 f'font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center">{n}</span>')
        label = f"{name}, {n} left" + (", after your first move" if waiting else "")
        out.append(f'<button type="button" aria-label="{label}" style="position: relative; height: 56px; '
                   f'box-sizing: border-box; border: {border}; border-radius: 14px 18px 12px 20px / 18px 12px 20px 14px; '
                   f'background: {t["paper"] if waiting else t["card"]}; padding: 0; display: flex; align-items: center; '
                   f'justify-content: center">{booster_pic(t, ic, ink)}{badge}</button>')
    return (f'<div role="toolbar" aria-label="Boosters" style="position: absolute; left: {side}px; right: {side}px; '
            f'top: {top}px; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: {gap}px">'
            f'{"".join(out)}</div>')


def undo_note(t, side=10, bottom=12, pad=10, angle=-0.6, key="undo", ready=True):
    """A booster preview's note, Undo's by default. Undo has no point to aim at: the board shows the turn it
    takes back, pale in dashed rings, and the note says so in one line. Wall and Bomb show their mark at
    the point aimed at; until one is chosen (not ready) the main button says how, pale, and cannot be
    pressed. The note lies over the player's line and the tool bar."""
    ic, name, line, go = NOTES[key]
    # The line keeps to one line; the app shrinks a long one to fit (11 to 16 sp).
    fit = "; font-size: 14px" if len(name + line) > 34 else ""
    button = main_button(t, go) if ready else main_button(t, "Drag to a point", f"background: {t['blue_pale']}; color: {t['blue']}")
    cancel = (f'<button type="button" style="height: 56px; box-sizing: border-box; border: 2px solid {t["blue"]}; '
              f'{BUTTON_SHAPE}; background: {t["card"]}; color: {t["blue"]}; font-family: inherit; {LABEL}; '
              f'padding: 0">Cancel</button>')
    return (f'<div role="dialog" aria-label="{name}" style="position: absolute; left: {side}px; right: {side}px; '
            f'bottom: {bottom}px; box-sizing: border-box; padding: {pad}px {pad + 2}px; display: flex; '
            f'flex-direction: column; gap: {pad - 2}px; {note_style(t, angle)}">{tape(t, 20, -8, 44, -3)}'
            f'<div style="display: flex; align-items: center; gap: 8px">{named_icon(ic, t["blue"], 28)}'
            f'<span style="{BODY}; white-space: nowrap{fit}"><b style="color: {t["blue"]}">{name}</b> {line}</span></div>'
            f'<div style="display: grid; grid-template-columns: 1fr 2fr; gap: 10px">{cancel}'
            f'{button}</div></div>')


def thumb(x, y, opacity=0.93):
    """A thumb whose tip rests on (x, y), reaching in from the lower right. It is drawn over the frame to
    show where the finger is; it is not part of the game."""
    return (f'<div aria-hidden="true" style="position: absolute; left: {f(x - 30)}px; top: {f(y - 16)}px; '
            f'width: 60px; height: 170px; border-radius: 30px 30px 26px 26px / 34px 34px 20px 20px; '
            f'background: #D9B49A; opacity: {f(opacity)}; transform-origin: 30px 16px; transform: rotate(-38deg); '
            f'box-shadow: 0 3px 10px rgba(0,0,0,0.3)"><div style="position: absolute; left: 15px; top: 7px; '
            f'width: 30px; height: 34px; border-radius: 15px 15px 9px 9px; background: #EBD0BF"></div></div>')


def drag_thumb(board_top, sp, key="drag"):
    """The thumb of a drag frame, its tip on the dragged point (the canvas shows it a little see-through,
    so the preview under it can be checked)."""
    pos = POSITIONS[key]
    p = pos["aim"]["point"] if pos.get("aim") else pos["drag"][0]
    # Lighter while a booster is aimed: its mark is larger than a preview and lies mostly under the thumb.
    return thumb(sp / 2 + p[0] * sp, board_top + sp / 2 + p[1] * sp, opacity=0.6 if pos.get("aim") else 0.8)


DRAGGED = ("drag", "wall-aim", "bomb-aim")  # frames with a finger on the board
NOTED = ("undo", "wall-aim", "bomb-aim")  # frames with a booster preview's note over the tool bar


def petals(cx, cy, n=5, r=6, size=3):
    """Small round petals in a ring, as one path."""
    out = []
    for i in range(n):
        a = 2 * math.pi * i / n - math.pi / 2
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        out.append(f"M{f(x + size)} {f(y)} a{size} {size} 0 1 0 {-2 * size} 0 a{size} {size} 0 1 0 {2 * size} 0")
    return " ".join(out)


def star_path(cx, cy, r):
    pts = []
    for i in range(10):
        a = math.pi * i / 5 - math.pi / 2
        k = r if i % 2 == 0 else r * 0.45
        pts.append(f"{f(cx + k * math.cos(a))} {f(cy + k * math.sin(a))}")
    return "M" + " L".join(pts) + " Z"


# A world's board touches (decision 0028): a few pale pencil doodles in the empty paper beside the
# opponent, never near the board. Drawn in code, in the chosen theme's pale inks.
TOUCHES = {
    1: [("blue_pale", "M12 132 q8 -10 16 0 q8 10 16 0 M26 186 l4 9 9 1 -7 6 2 9 -8 -5 -8 5 2 -9 -7 -6 9 -1z "
                      "M326 236 a10 10 0 1 1 -3 -7 M296 196 l26 -8 -8 26 -5 -11z M296 196 l13 7")],
    2: [("red_pale", petals(28, 134) + " M26 134 a2 2 0 1 0 4 0 a2 2 0 1 0 -4 0"),
        ("blue_pale", "M28 142 v16 M28 152 q6 -8 12 -6 q-4 8 -12 6 M20 214 q2 -18 20 -20 q0 18 -20 20 M20 214 l12 -12 "
                      "M318 252 v-14 M312 238 q-1 -9 2 -10 l3 4 l1 -5 l1 5 l3 -4 q3 1 2 10 z"),
        ("red_pale", "M312 204 v8 M312 207 c-5 -9 -14 -6 -11 1 c2 4 7 2 11 -1 M312 207 c5 -9 14 -6 11 1 c-2 4 -7 2 -11 -1 "
                     "M312 209 c-4 2 -7 7 -3 7 c2 0 3 -4 3 -7 M312 209 c4 2 7 7 3 7 c-2 0 -3 -4 -3 -7")],
    3: [("blue_pale", "M34 122 a16 16 0 1 0 0 32 a22 22 0 0 1 0 -32 z " + star_path(30, 200, 9) +
                      " M318 228 v18 M309 237 h18"),
        ("red_pale", "M318 190 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 M318 193 l-20 12 M320 196 l-14 14 M317 190 l-22 4")],
    4: [("blue_pale", "M14 138 q13 -11 26 0 q-13 11 -26 0 z M40 138 l7 -5 v10 z "
                      "M296 212 h28 l-5 7 h-18 z M310 212 V190 M310 193 l11 15 h-11 M300 240 q4 -5 8 0 q4 5 8 0 q4 -5 8 0"),
        ("red_pale", "M18 212 q12 -26 24 0 z M30 212 v-13 M24 212 l3 -12 M36 212 l-3 -12")],
}


def margin_doodles(t, world=1):
    """A world's board touches: a few pencil doodles in the empty paper around the opponent."""
    body = "".join(el("path", d=d, fill="none", stroke=t[ink], stroke_width=1.6, stroke_linecap="round",
                      stroke_linejoin="round") for ink, d in TOUCHES[world])
    return svg(W, 300, body, style="position: absolute; left: 0; top: 0")


def game_frame(t, key, uid, links=False, world=1, contrast=False):
    m = MOMENTS[key]
    parts = [margin_doodles(t, world)]
    if key != "win":
        parts.append(f'<div style="position: absolute; left: 4px; top: 28px">{icon_button(t, "pause", "Pause", isize=30)}</div>')
    parts.append(f'<div style="position: absolute; left: 0; right: 0; top: 40px; text-align: center; {LABEL}; '
                 f'color: {t["graphite"]}">Level 7</div>')
    if key != "opening":
        parts.append(mission_note(t, m["mission"]))
    # The opponent, above the board.
    parts.append(f'<div style="position: absolute; left: 64px; top: 100px; width: 124px; height: 124px; '
                 f'box-sizing: border-box; padding: 8px; {note_style(t, -3)}">{tape(t, 36, -9, 52, 4)}'
                 f'{face(t, m["mood"], 108)}</div>')
    parts.append(bubble(t, m["bubble"], 198, 132))
    parts.append(f'<div style="position: absolute; left: 0; right: 0; top: 242px">'
                 f'{side_row(t, "blue", "Computer", m["opp"], False, "center")}</div>')
    parts.append(f'<div style="position: absolute; left: 0; top: {BOARD_TOP}px">{any_board_svg(t, key, uid, links=links, contrast=contrast)}</div>')
    # The player, below the board.
    you_turn = key in ("opening", "threat", "drag") + NOTED
    row_top = 682 if key != "win" else -200
    parts.append(f'<div style="position: absolute; left: 16px; right: 16px; top: {row_top}px; display: flex; '
                 f'align-items: center; justify-content: space-between">'
                 f'{side_row(t, "red", "You", m["you"], you_turn, "flex-start")}'
                 f'<div style="display: flex; align-items: baseline; gap: 6px"><span style="{SCORE}">{m["left"]}</span>'
                 f'<span style="{LABEL}; color: {t["graphite"]}">dots left</span></div></div>')
    if key != "win" and key not in NOTED:
        parts.append(toolbar(t, undo_ready=key not in ("opening", "drag")))
    if key in NOTED:
        parts.append(undo_note(t, key=key))
    if key in DRAGGED:
        parts.append(drag_thumb(BOARD_TOP, 24, key))
    if key == "opening":
        parts.append(f'<div style="position: absolute; left: 36px; right: 36px; top: 222px; box-sizing: border-box; '
                     f'padding: 16px 18px 12px; text-align: center; {note_style(t, -1.5)}">{tape(t, 118, -9, 60, 3)}'
                     f'<div style="{LABEL}; color: {t["graphite"]}">World 1 · Level 7</div>'
                     f'<div style="{DISPLAY}; color: {t["blue"]}">Siege</div>'
                     f'<div style="display: flex; align-items: center; justify-content: center; gap: 8px; margin-top: 4px">'
                     f'{icon(ICONS["target"], t["red"], 24)}<span style="{TITLE}">Capture 8 dots</span></div>'
                     f'<div style="{BODY}; color: {t["graphite"]}; margin-top: 4px">The computer starts 6 dots ahead.</div></div>')
    if key == "big":
        halo = f"text-shadow: 0 0 3px {t['paper']}, 0 0 6px {t['paper']}, 0 0 10px {t['paper']}"
        arc = (f"M150 {BOARD_TOP + 196} Q178 {BOARD_TOP + 236} 170 {BOARD_TOP + 262} "
               f"M150 {BOARD_TOP + 352} Q136 {BOARD_TOP + 372} 118 {BOARD_TOP + 382}")
        parts.append(svg(W, H, el("path", d=arc, fill="none", stroke=t["red"], stroke_width=2, stroke_dasharray="4 6",
                                  stroke_linecap="round"), style="position: absolute; left: 0; top: 0"))
        parts.append(f'<div style="position: absolute; left: 132px; top: {BOARD_TOP + 258}px; transform: rotate(-6deg); '
                     f'display: flex; flex-direction: column; align-items: center">'
                     f'<span style="font-weight: 800; font-size: 40px; line-height: 40px; color: {t["red"]}; {halo}">+6</span>'
                     f'<span style="{HEADLINE}; font-weight: 700; line-height: 28px; color: {t["red"]}; {halo}">Six at once!</span></div>')
    if key == "win":
        stars = "".join(f'<span style="transform: translateY({4 if i != 1 else -4}px)">{star(t, 56 if i == 1 else 48, i < 2)}</span>'
                        for i in range(3))
        retry = (f'<button type="button" aria-label="Retry" style="width: 56px; height: 56px; flex-shrink: 0; '
                 f'box-sizing: border-box; border: 2px solid {t["graphite"]}; {BUTTON_SHAPE}; background: {t["card"]}; '
                 f'padding: 0; display: flex; align-items: center; justify-content: center">{icon(ICONS["retry"], t["pencil"], 26)}</button>')
        parts.append(f'<div role="dialog" aria-label="You won" style="position: absolute; left: 20px; right: 20px; '
                     f'bottom: 22px; box-sizing: border-box; padding: 20px 18px 18px; display: flex; '
                     f'flex-direction: column; align-items: center; gap: 6px; text-align: center; {note_style(t, -1.5)}">'
                     f'{tape(t, 126, -9, 64, -3)}'
                     f'<div style="{DISPLAY}; color: {t["red"]}">You won!</div>'
                     f'<div style="display: flex; align-items: center; gap: 10px">{stars}</div>'
                     f'<div style="{BODY}; margin-top: 4px">8 captured · 12 dots to spare</div>'
                     f'<div style="{SMALL}; color: {t["graphite"]}">Three stars need 14 dots to spare</div>'
                     f'<div style="align-self: stretch; display: flex; gap: 12px; margin-top: 10px">{retry}'
                     f'{main_button(t, "Next", "flex: 1", ICONS["next"])}</div></div>')
    return root(t, "".join(parts))


def small_game_frame(t, key, uid):
    """The game on a small phone, 320 x 568: a 21 dp board (315 dp) and the same five-booster tool bar.
    The opponent shrinks to a small photo beside its name; the speech bubble has no room and waits."""
    sw, sh, top = 320, 568, 141
    m = MOMENTS[key]
    parts = [f'<div style="position: absolute; left: 0; top: 24px">{icon_button(t, "pause", "Pause", isize=28)}</div>',
             f'<div style="position: absolute; left: 54px; top: 38px; {LABEL}; color: {t["graphite"]}">Level 7</div>',
             mission_note(t, m["mission"], right=8, top=28),
             f'<div style="position: absolute; left: 18px; top: 74px; width: 62px; height: 62px; box-sizing: border-box; '
             f'padding: 3px; {note_style(t, -3)}">{tape(t, 15, -7, 32, 4)}{face(t, m["mood"], 56)}</div>',
             f'<div style="position: absolute; left: 96px; top: 104px">{side_row(t, "blue", "Computer", m["opp"], False, "flex-start")}</div>',
             f'<div style="position: absolute; left: 0; top: {top}px">{any_board_svg(t, key, uid, size=315, sp=21)}</div>',
             f'<div style="position: absolute; left: 12px; right: 12px; top: 462px; display: flex; align-items: center; '
             f'justify-content: space-between">{side_row(t, "red", "You", m["you"], True, "flex-start")}'
             f'<div style="display: flex; align-items: baseline; gap: 6px"><span style="{SCORE}">{m["left"]}</span>'
             f'<span style="{LABEL}; color: {t["graphite"]}">dots left</span></div></div>']
    if key in NOTED:
        parts.append(undo_note(t, side=6, bottom=12, pad=6, angle=0, key=key))
    else:
        parts.append(toolbar(t, top=500, side=10, gap=8, undo_ready=key != "drag"))
    if key in DRAGGED:
        parts.append(drag_thumb(top, 21, key))
    return root(t, "".join(parts), w=sw, h=sh)


def boosters_frame(t, uid):
    """The Boosters screen, from a booster on the Level card: what each does, how many the player has."""
    rows = []
    for name, ic, n, line in BOOSTERS:
        ink = t["blue"] if n else t["graphite"]
        border = f"2px solid {t['blue']}" if n else f"2px dashed {t['graphite']}"
        rows.append(f'<div style="display: flex; align-items: center; gap: 12px; padding: 6px 0; '
                    f'border-bottom: 1px dashed {t["grid"]}">'
                    f'<span style="width: 52px; height: 52px; flex-shrink: 0; box-sizing: border-box; border: {border}; '
                    f'border-radius: 14px 18px 12px 20px / 18px 12px 20px 14px; display: flex; align-items: center; '
                    f'justify-content: center">{booster_pic(t, ic, ink)}</span>'
                    f'<div style="flex: 1; min-width: 0; display: flex; flex-direction: column">'
                    f'<span style="{HAND24}">{name}</span>'
                    f'<span style="{BODY}; font-size: 14px; line-height: 20px; color: {t["graphite"]}">{line}</span></div>'
                    f'<span style="{SCORE}; color: {t["red"] if n else t["graphite"]}">×{n}</span></div>')
    card = (f'<div role="dialog" aria-label="Boosters" style="position: absolute; left: 12px; right: 12px; bottom: 16px; '
            f'box-sizing: border-box; padding: 12px 16px 16px; display: flex; flex-direction: column; gap: 4px; '
            f'{note_style(t, 0.8)}">{tape(t, 140, -9, 56, -3)}'
            f'<div style="display: flex; align-items: center"><span style="flex: 1; {DISPLAY}; color: {t["blue"]}">Boosters</span>'
            f'{icon_button(t, "close", "Close", style="margin-right: -10px")}</div>'
            f'{"".join(rows)}'
            f'<div style="{SMALL}; color: {t["graphite"]}; margin-top: 8px">Earned by playing: stars, chests and the daily '
            f'streak. Never sold. One booster a turn, and Undo on top. Every level can be won without them.</div></div>')
    scrim = f'<div aria-hidden="true" style="position: absolute; inset: 0; background: {t["scrim"]}"></div>'
    return map_frame(t, uid, scrim + card)


# ------------------------------------------------- free play and two players ----
# An open-ended game (decision 0025, D-16): no dot limit. The side to move may stop; the other side then
# places as many dots as it likes to capture what is left, and ends the game with Done.

FREE_MOMENTS = {  # mode, the computer's mood and bubble, who stopped, who moves, scores (you, them)
    "free-open": dict(mode="free", mood="determined", bubble="Your move…", stopper=None, turn="red", score=(4, 1)),
    "free-stop": dict(mode="free", mood="determined", bubble="Your move…", stopper=None, turn="red", score=(4, 1)),
    "free-computer-stops": dict(mode="free", mood="smug", bubble="I’ll stop here.", stopper="blue", turn="red",
                                score=(1, 4)),
    "two-done": dict(mode="two", stopper="red", turn="blue", score=(4, 1)),
}
FREE_POSITION = {"free-open": "free", "free-stop": "free-stop", "free-computer-stops": "free-computer-stops",
                 "two-done": "two-done"}


def stopped_chip(t):
    """Beside the name of the side that stopped, until the game ends: a flag and "stopped"."""
    return (f'<span style="height: 26px; box-sizing: border-box; border: 1.5px solid {t["graphite"]}; border-radius: 13px; '
            f'display: inline-flex; align-items: center; gap: 4px; padding: 0 9px 0 6px; {SMALL}; font-weight: 700; '
            f'color: {t["graphite"]}">{icon(ICONS["flag"], t["graphite"], 16, 2)}stopped</span>')


def open_ring(t, size=16):
    """The dashed Highlighter ring of a dot not joined to the edge, as a glyph in a line of text."""
    return svg(size, size, el("circle", cx=size / 2, cy=size / 2, r=size / 2 - 1.5, fill="none", stroke=t["hi"],
                                stroke_width=1.5, stroke_dasharray="3 2.5"))


def free_tool(t, name, ic, col):
    """A free-play tool: its icon and its name, in the campaign's tool shape (no count)."""
    return (f'<button type="button" style="grid-column: {col}; height: 56px; box-sizing: border-box; '
            f'border: 2px solid {t["blue_pale"]}; border-radius: 14px 18px 12px 20px / 18px 12px 20px 14px; '
            f'background: {t["card"]}; padding: 0; display: flex; flex-direction: column; align-items: center; '
            f'justify-content: center; gap: 1px; font-family: inherit; {SMALL}; font-weight: 700; color: {t["blue"]}">'
            f'{icon(ICONS[ic], t["blue"], 24, 1.9)}{name}</button>')


def free_toolbar(t, mode, top=730, side=12, gap=10):
    """Free play: Stop at the left, Undo at the right, where the campaign keeps its Undo. Two players: Stop
    alone (no undo between two people)."""
    tools = free_tool(t, "Stop", "flag", 1) + (free_tool(t, "Undo", "back", 5) if mode == "free" else "")
    return (f'<div role="toolbar" aria-label="Tools" style="position: absolute; left: {side}px; right: {side}px; '
            f'top: {top}px; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: {gap}px">{tools}</div>')


def collect_bar(t, mode, top=730, side=12):
    """For the side that collects after the other stopped, in place of the tool bar: Undo (free play only),
    what to do, and Done, the one big button."""
    undo = (f'<button type="button" aria-label="Undo" style="width: 56px; height: 56px; flex-shrink: 0; '
            f'box-sizing: border-box; border: 2px solid {t["blue_pale"]}; border-radius: 14px 18px 12px 20px / '
            f'18px 12px 20px 14px; background: {t["card"]}; padding: 0; display: flex; align-items: center; '
            f'justify-content: center">{icon(ICONS["back"], t["blue"], 24, 1.9)}</button>') if mode == "free" else ""
    return (f'<div style="position: absolute; left: {side}px; right: {side}px; top: {top}px; display: flex; '
            f'align-items: center; gap: 12px">{undo}'
            f'<span style="flex: 1; min-width: 0; {LABEL}; color: {t["pencil"]}">{"" if mode == "free" else "Blue, "}'
            f'{"C" if mode == "free" else "c"}apture what is left, then tap Done.</span>'
            f'{main_button(t, "Done", "", ICONS["tick"], width="118px")}</div>')


def stop_note(t, mode, side=10, bottom=10, pad=6, angle=-0.6):
    """Stop's note: the second tap. Like a booster preview's note it lies over the player's line and the tool
    bar and leaves the board where it is. The board circles the dots that can still be taken."""
    other = "the computer" if mode == "free" else "Blue"
    n = len(POSITIONS["free-stop"]["open"])
    risk = (f"{n} of your dots aren’t joined to the edge." if n else "All your dots are joined to the edge.")
    cancel = (f'<button type="button" style="height: 56px; box-sizing: border-box; border: 2px solid {t["blue"]}; '
              f'{BUTTON_SHAPE}; background: {t["card"]}; color: {t["blue"]}; font-family: inherit; {LABEL}; '
              f'padding: 0">Cancel</button>')
    return (f'<div role="dialog" aria-label="Stop" style="position: absolute; left: {side}px; right: {side}px; '
            f'bottom: {bottom}px; box-sizing: border-box; padding: {pad}px {pad + 4}px; display: flex; '
            f'flex-direction: column; gap: 2px; {note_style(t, angle)}">{tape(t, 20, -8, 44, -3)}'
            f'<div style="display: flex; align-items: center; gap: 8px">{icon(ICONS["flag"], t["blue"], 22, 2)}'
            f'<span style="{BODY}; font-size: 14px; line-height: 22px; white-space: nowrap"><b style="color: {t["blue"]}">'
            f'Stop?</b> {other[0].upper() + other[1:]} takes what is left.</span></div>'
            f'<div style="display: flex; align-items: center; gap: 6px; padding-left: 2px; {SMALL}; font-weight: 700; '
            f'color: {t["graphite"]}">{open_ring(t)}{risk}</div>'
            f'<div style="display: grid; grid-template-columns: 1fr 2fr; gap: 10px; margin-top: 2px">{cancel}'
            f'{main_button(t, "Stop here")}</div></div>')


def two_photo(t, size):
    """Between two people, Blue's photo holds its piece, as in the app."""
    h = size * 0.42
    c = size / 2
    return svg(size, size, el("path", d=f"M{f(c)} {f(c - h)}L{f(c + h)} {f(c)}L{f(c)} {f(c + h)}L{f(c - h)} {f(c)}Z",
                              fill=t["blue"]))


def free_frame(t, key, uid):
    m = FREE_MOMENTS[key]
    mode, stopper = m["mode"], m["stopper"]
    you, them = m["score"]
    you_name, opp_name = ("You", "Computer") if mode == "free" else ("Red", "Blue")
    parts = [f'<div style="position: absolute; left: 4px; top: 28px">{icon_button(t, "pause", "Pause", isize=30)}</div>',
             f'<div style="position: absolute; left: 0; right: 0; top: 40px; text-align: center; {LABEL}; '
             f'color: {t["graphite"]}">{"Free play" if mode == "free" else "Two players"}</div>']
    photo = face(t, m["mood"], 108) if mode == "free" else two_photo(t, 108)
    parts.append(f'<div style="position: absolute; left: 64px; top: 100px; width: 124px; height: 124px; '
                 f'box-sizing: border-box; padding: 8px; {note_style(t, -3)}">{tape(t, 36, -9, 52, 4)}{photo}</div>')
    if mode == "free":
        parts.append(bubble(t, m["bubble"], 198, 132))
    chip = stopped_chip(t) if stopper == "blue" else ""
    parts.append(f'<div style="position: absolute; left: 0; right: 0; top: 242px; display: flex; justify-content: center; '
                 f'align-items: center; gap: 12px">{side_row(t, "blue", opp_name, them, m["turn"] == "blue", "center")}'
                 f'{chip}</div>')
    parts.append(f'<div style="position: absolute; left: 0; top: {BOARD_TOP}px">'
                 f'{board_svg(t, FREE_POSITION[key], uid)}</div>')
    chip = stopped_chip(t) if stopper == "red" else ""
    noted = key == "free-stop"
    if not noted:
        parts.append(f'<div style="position: absolute; left: 16px; right: 16px; top: 682px; display: flex; '
                     f'align-items: center; gap: 12px">{side_row(t, "red", you_name, you, m["turn"] == "red", "flex-start")}'
                     f'{chip}<span style="margin-left: auto; {LABEL}; color: {t["graphite"]}">no limit</span></div>')
    if noted:
        parts.append(stop_note(t, mode))
    elif stopper:
        parts.append(collect_bar(t, mode))
    else:
        parts.append(free_toolbar(t, mode))
    return root(t, "".join(parts))


# ---------------------------------------------------- Play a game: the setup ----
# The small link on Play a game opens the setup in the same note (D-16): board size and game length, each
# a row of three choices, above the two cards. The choices are remembered for the next game.

def size_pic(ink, lines):
    """A board size's small picture: a square of squared paper, denser for a bigger board."""
    d = " ".join(f"M{f(2 + 16 * k / (lines - 1))} 2V18M2 {f(2 + 16 * k / (lines - 1))}H18" for k in range(lines))
    return svg(20, 20, el("path", d=d, fill="none", stroke=ink, stroke_width=1.1, stroke_linecap="round"))


def choice(t, picked, main, sub=None, pic=None, height=56):
    ink = t["paper"] if picked else t["blue"]
    style = (f'background: {t["blue"]}; border: 2px solid {t["blue"]}' if picked else
             f'background: {t["card"]}; border: 2px solid {t["blue_pale"]}')
    sub_html = f'<span style="{SMALL}; font-weight: 500; color: {ink}">{sub}</span>' if sub else ""
    return (f'<button type="button" aria-pressed="{"true" if picked else "false"}" style="height: {height}px; '
            f'box-sizing: border-box; {style}; border-radius: 14px 18px 12px 20px / 18px 12px 20px 14px; padding: 0 4px; '
            f'display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 0; '
            f'font-family: inherit; color: {ink}"><span style="display: flex; align-items: center; gap: 6px; {LABEL}; '
            f'font-size: {15 if pic else 14}px; white-space: nowrap">{pic(ink) if pic else ""}{main}</span>{sub_html}</button>')


def play_card(t, ink, title, sub, pic, height, side=16):
    play = svg(20, 20, el("path", d="M5 3 L17 10 L5 17 Z", fill=t["paper"]))
    return (f'<button type="button" style="height: {height}px; box-sizing: border-box; border: none; {BUTTON_SHAPE}; '
            f'background: {t[ink]}; color: {t["paper"]}; font-family: inherit; padding: 0 {side}px; display: flex; '
            f'align-items: center; gap: {side - 4}px; text-align: left">{pic}<span style="flex: 1; display: flex; '
            f'flex-direction: column"><span style="{HAND24}; white-space: nowrap">{title}</span>'
            f'<span style="{SMALL}; font-weight: 700">{sub}</span></span>{play}</button>')


def computer_pic(t, size=44):
    return svg(size, size, el("circle", cx=24, cy=24, r=17, fill="none", stroke=t["paper"], stroke_width=2.2) +
               el("circle", cx=18, cy=22, r=2, fill=t["paper"]) + el("circle", cx=30, cy=22, r=2, fill=t["paper"]) +
               el("path", d="M17 30 q7 5 14 0", fill="none", stroke=t["paper"], stroke_width=2.2, stroke_linecap="round"),
               view="0 0 48 48")


def people_pic(t, size=44):
    body = (el("circle", cx=9, cy=7, r=4, fill="none", stroke=t["paper"], stroke_width=1.6) +
            el("path", d="M2 21 q7 -10 14 0", fill="none", stroke=t["paper"], stroke_width=1.6) +
            el("circle", cx=19, cy=7, r=4, fill="none", stroke=t["paper"], stroke_width=1.6) +
            el("path", d="M12 21 q7 -10 14 0", fill="none", stroke=t["paper"], stroke_width=1.6))
    return svg(size, size * 22 / 28, body, view="0 0 28 22")


LENGTHS = [("Short", "33 dots"), ("Long", "67 dots"), ("Open-ended", "no limit")]
LENGTH_LINES = {  # the line under the lengths, for the one chosen
    "Short": "Each side places 33 dots. More captures wins.",
    "Long": "Each side places 67 dots. More captures wins.",
    "Open-ended": "As on paper. Tap Stop once your dots are safe.",
}


def setup_note(t, small=False, length="Open-ended"):
    """Play a game with its setup open: board size and game length above the two cards."""
    ch, card, gap, pad = (48, 64, 8, 12) if small else (56, 80, 10, 16)
    sizes = "".join(choice(t, n == 15, f"{n} × {n}", pic=lambda ink, k=k: size_pic(ink, k), height=ch)
                    for n, k in ((12, 4), (15, 5), (20, 7)))
    lengths = "".join(choice(t, name == length, name, sub, height=ch) for name, sub in LENGTHS)
    label = f'{LABEL}; color: {t["graphite"]}; margin-bottom: -4px'
    grid = f'display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: {gap}px'
    return (f'<div role="dialog" aria-label="Play a game" style="position: absolute; left: 10px; right: 10px; '
            f'bottom: {12 if small else 16}px; box-sizing: border-box; padding: {pad - 4}px {pad}px {pad}px; display: flex; '
            f'flex-direction: column; gap: {gap}px; {note_style(t, 0.6)}">{tape(t, 40, -8, 56, -4)}'
            f'<div style="display: flex; align-items: center"><span style="flex: 1; {DISPLAY}; color: {t["blue"]}">'
            f'Play a game</span>{icon_button(t, "close", "Close", style="margin-right: -10px")}</div>'
            f'<div style="{label}">Board</div><div style="{grid}">{sizes}</div>'
            f'<div style="{label}">Game length</div><div style="{grid}">{lengths}</div>'
            f'<div style="display: flex; align-items: center; gap: 6px; {SMALL}; font-weight: 700; color: {t["graphite"]}">'
            f'{icon(ICONS["flag"], t["graphite"], 16, 2) if length == "Open-ended" else ""}{LENGTH_LINES[length]}</div>'
            f'<div style="display: flex; flex-direction: column; gap: {gap}px; margin-top: {2 if small else 6}px">'
            f'{play_card(t, "blue", "Against the computer", "new game", computer_pic(t, 34 if small else 44), card, 10 if small else 16)}'
            f'{play_card(t, "red", "Two players", "new game", people_pic(t, 34 if small else 44), card, 10 if small else 16)}</div></div>')


def replace_note(t, uid):
    """With the setup open, a card starts a new game; if that kind of game is saved, this note asks first,
    taped over the cards. It shows the saved game, so the player knows what would end."""
    keep = (f'<button type="button" style="height: 56px; box-sizing: border-box; border: 2px solid {t["blue"]}; '
            f'{BUTTON_SHAPE}; background: {t["card"]}; color: {t["blue"]}; font-family: inherit; {LABEL}; '
            f'padding: 0">Keep it</button>')
    return (f'<div role="dialog" aria-label="Replace your saved game?" style="position: absolute; left: 22px; right: 22px; '
            f'bottom: 30px; box-sizing: border-box; padding: 14px 16px 16px; display: flex; flex-direction: column; '
            f'gap: 10px; {note_style(t, -1.2)}">{tape(t, 130, -9, 60, 3)}'
            f'<div style="{HEADLINE}; font-weight: 700; color: {t["blue"]}">Replace your saved game?</div>'
            f'<div style="display: flex; gap: 12px; align-items: center">'
            f'<div style="border: 1px solid {t["grid"]}; flex-shrink: 0">{board_svg(t, "free", uid + "m", size=96, sp=96 / 15, mini=True)}</div>'
            f'<div style="display: flex; flex-direction: column; gap: 2px; min-width: 0">'
            f'<span style="{LABEL}">Against the computer</span>'
            f'<span style="{SMALL}; color: {t["graphite"]}">15 × 15 · Short · {33 - len(FREE["red"])} dots left</span>'
            f'<span style="{SMALL}; font-weight: 700"><span style="color: {t["red"]}">You 4</span> · '
            f'<span style="color: {t["blue"]}">Computer 1</span></span></div></div>'
            f'<div style="{SMALL}; color: {t["graphite"]}">It ends, and a new open-ended game starts.</div>'
            f'<div style="display: grid; grid-template-columns: 1fr 2fr; gap: 10px">{keep}{main_button(t, "New game")}</div>'
            f'</div>')


def setup_frame(t, uid, replace=False):
    scrim = f'<div aria-hidden="true" style="position: absolute; inset: 0; background: {t["scrim"]}"></div>'
    extra = scrim + setup_note(t)
    if replace:
        extra += replace_note(t, uid)
    return map_frame(t, uid, extra)


def small_setup_frame(t, uid):
    """The setup at 320 x 568: the map's top shows behind, the note keeps every choice and both cards."""
    scrim = f'<div aria-hidden="true" style="position: absolute; inset: 0; background: {t["scrim"]}"></div>'
    return (f'<div style="position: relative; width: 320px; height: 568px; overflow: hidden; background: {t["paper"]}; '
            f'font-family: Nunito, system-ui, sans-serif; color: {t["pencil"]}">'
            f'<div style="position: absolute; left: -20px; top: 0">{map_frame(t, uid)}</div>{scrim}'
            f'{setup_note(t, small=True)}</div>')


# -------------------------------------------------------------------- map ----

STOPS = [  # (level, x, y, state, stars)
    (5, 120, 700, "done", 2), (6, 205, 668, "done", 1), (7, 285, 610, "next", 0), (8, 215, 548, "locked", 0),
    (9, 125, 512, "locked", 0), (10, 112, 440, "locked", 0), ("chest", 192, 412, "chest", 0),
    (11, 282, 380, "locked", 0), (12, 300, 312, "locked", 0), (13, 222, 272, "locked", 0),
    (14, 135, 248, "locked", 0), (15, 150, 170, "boss", 0),
]

# A later world's stretch of the path, at the same places: levels 10 to 20, a chest after 15.
STOPS_LATE = [(lv if isinstance(lv, str) else lv + 5, x, y, st, n) for lv, x, y, st, n in STOPS]

WORLDS = {  # number: (name, the player's title, the world's stars)
    1: ("Countryside", "Apprentice", "14 of 45"),
    2: ("Garden", "Apprentice", "21 of 60"),
    3: ("Night sky", "Strategist", "18 of 60"),
    4: ("Sea", "Master", "9 of 60"),
}


def spline(pts):
    d = f"M{pts[0][0]} {pts[0][1]}"
    for i in range(len(pts) - 1):
        p0, p1, p2 = pts[max(0, i - 1)], pts[i], pts[i + 1]
        p3 = pts[min(len(pts) - 1, i + 2)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += f" C{f(c1[0])} {f(c1[1])} {f(c2[0])} {f(c2[1])} {p2[0]} {p2[1]}"
    return d


def hatch(uid, ink, angle=-45, gap=4):
    return (f'<pattern id="{uid}" width="{gap}" height="{gap}" patternUnits="userSpaceOnUse" '
            f'patternTransform="rotate({angle})">' + el("line", x1=0, y1=gap / 2, x2=gap, y2=gap / 2, stroke=ink,
                                                         stroke_width=1) + "</pattern>")


def map_art(t, uid, world=1):
    """Ballpoint doodles of a world's map page. Stand-ins for the illustrations in
    docs/design/illustrations.md, at the places and sizes the pictures take."""
    s = dict(fill="none", stroke_width=1.5, stroke_linecap="round", stroke_linejoin="round")
    defs = hatch(f"hr{uid}", t["red_pale"]) + hatch(f"hb{uid}", t["blue_pale"], 45)
    if world != 1:
        return f"<defs>{defs}</defs>" + {2: garden_art, 3: night_art, 4: sea_art}[world](t, uid, s)
    a = []
    # hills along the bottom
    hill = "M58 800 V770 Q120 742 190 768 Q250 736 320 760 Q345 752 360 756 V800 Z"
    a.append(el("path", d=hill, fill=f"url(#hr{uid})"))
    a.append(el("path", d="M58 770 Q120 742 190 768 Q250 736 320 760 Q345 752 360 756", stroke=t["red"], **s))
    # mountains behind the boss
    a.append(el("path", d="M214 238 L250 194 L268 216 L292 182 L338 238 Z", fill=f"url(#hb{uid})"))
    a.append(el("path", d="M206 238 L250 194 L268 216 L292 182 L344 238 M284 192 l8 10 8 -8", stroke=t["blue"], **s))
    # pines
    for x, y, k in ((76, 352, 1), (98, 340, 1.2), (88, 368, 0.8)):
        a.append(el("path", d=f"M{x} {y - 34 * k} L{x - 11 * k} {y - 14 * k} L{x - 5 * k} {y - 14 * k} "
                              f"L{x - 14 * k} {y} L{x + 14 * k} {y} L{x + 5 * k} {y - 14 * k} L{x + 11 * k} {y - 14 * k} Z "
                              f"M{x} {y} v{6 * k}", stroke=t["blue"], **s))
    # round tree
    a.append(el("path", d="M290 486 q-10 -2 -8 -14 q-6 -14 10 -18 q4 -14 20 -8 q14 -6 18 8 q12 4 6 18 q4 12 -10 14 z",
                fill=f"url(#hr{uid})"))
    a.append(el("path", d="M290 486 q-10 -2 -8 -14 q-6 -14 10 -18 q4 -14 20 -8 q14 -6 18 8 q12 4 6 18 q4 12 -10 14 z "
                          "M306 486 v18 M300 504 h14 M306 492 l-6 -6", stroke=t["red"], **s))
    # pond with reeds
    a.append(el("ellipse", cx=96, cy=604, rx=34, ry=11, fill=f"url(#hb{uid})"))
    a.append(el("path", d="M62 604 a34 11 0 1 0 68 0 a34 11 0 1 0 -68 0 M80 604 h14 M100 609 h12 "
                          "M70 596 q2 -18 0 -28 M76 597 q4 -14 8 -22 M66 597 q-4 -12 -6 -18",
                stroke=t["blue"], **s))
    # house
    a.append(el("path", d="M282 676 l22 -18 l22 18 z", fill=f"url(#hr{uid})"))
    a.append(el("path", d="M282 676 l22 -18 l22 18 M286 674 v26 h36 v-26 M300 700 v-12 h8 v12 M312 664 v-8 h5 v12",
                stroke=t["red"], **s))
    # grass tufts
    for x, y in ((170, 470), (250, 520), (160, 590), (330, 560), (70, 470), (250, 340), (180, 200), (340, 400)):
        a.append(el("path", d=f"M{x} {y} l-3 -7 M{x + 3} {y} l0 -9 M{x + 6} {y} l3 -6", stroke=t["blue_pale"], **s))
    return f"<defs>{defs}</defs>" + "".join(a)


TUFTS = ((170, 470), (160, 590), (330, 560), (70, 470), (180, 200), (340, 400), (250, 520))


def garden_art(t, uid, s):
    """World 2: flower beds, a hedge, seed rows, a fence and garden things."""
    hr, hb, a = f"url(#hr{uid})", f"url(#hb{uid})", []
    # the hedge (2-12, Hedge) and a picket fence along the bottom
    top = "M58 772" + " q11 -16 22 0" * 14
    a.append(el("path", d=top + " V800 H58 Z", fill=hb))
    a.append(el("path", d=top, stroke=t["blue"], **s))
    pickets = " ".join(f"M{x} 800 V780 l3 -5 l3 5 V800" for x in range(66, 356, 18))
    a.append(el("path", d=pickets + " M62 786 H358 M62 795 H358", stroke=t["red"], **s))
    # sunflowers beside the boss (2-6, Flower Beds)
    for x, y in ((236, 200), (268, 194), (302, 206)):
        a.append(el("circle", cx=x, cy=y, r=8, fill=hr))
        rays = " ".join(f"M{f(x + 10 * math.cos(k * math.pi / 4))} {f(y + 10 * math.sin(k * math.pi / 4))} "
                        f"L{f(x + 14 * math.cos(k * math.pi / 4))} {f(y + 14 * math.sin(k * math.pi / 4))}" for k in range(8))
        a.append(el("path", d=f"M{x + 8} {y} a8 8 0 1 0 -16 0 a8 8 0 1 0 16 0 {rays} M{x} {y + 14} V238 "
                              f"M{x} 228 q8 -9 13 -3 q-6 7 -13 3", stroke=t["red"], **s))
    # tulips
    for x, y in ((74, 350), (92, 340), (108, 354)):
        cup = f"M{x - 7} {y} q-2 -12 1 -15 l3 6 l3 -7 l3 7 l3 -6 q3 3 1 15 z"
        a.append(el("path", d=cup, fill=hr))
        a.append(el("path", d=cup, stroke=t["red"], **s))
        a.append(el("path", d=f"M{x} {y} V376 M{x} 368 q-8 -3 -9 -12 q8 2 9 12", stroke=t["blue"], **s))
    # a watering can
    a.append(el("path", d="M290 504 V474 h30 V504 z", fill=hb))
    a.append(el("path", d="M290 504 V474 h30 V504 z M320 486 L340 466 M335 462 l9 9 M294 474 q11 -20 22 0",
                stroke=t["blue"], **s))
    a.append(el("path", d="M346 478 v5 M351 471 v5", stroke=t["blue_pale"], **s))
    # seed rows (2-18, Few Seeds)
    for y in (588, 600, 612):
        a.append(el("path", d=f"M64 {y} q32 -9 64 0 z", fill=hb))
        a.append(el("path", d=f"M64 {y} q32 -9 64 0", stroke=t["blue"], **s))
    a.append(el("path", d=" ".join(f"M{x} {y - 4} v-6 q-4 -3 -6 0 M{x} {y - 8} q4 -3 6 0"
                                   for y in (588, 600) for x in (78, 96, 114)), stroke=t["blue"], **s))
    # a wheelbarrow
    tray = "M282 672 h42 l-8 16 h-26 z"
    a.append(el("path", d=tray, fill=hr))
    a.append(el("path", d=tray + " M282 672 l-8 -4 M292 688 v10 M316 688 l4 4", stroke=t["red"], **s))
    a.append(el("circle", cx=322, cy=696, r=6, stroke=t["blue"], **s))
    # a butterfly and small flowers
    a.append(el("path", d="M250 334 v12 M250 338 c-6 -10 -16 -6 -12 2 c2 4 8 2 12 -2 M250 338 c6 -10 16 -6 12 2 "
                          "c-2 4 -8 2 -12 -2 M250 341 c-4 2 -8 8 -4 8 c2 0 4 -4 4 -8 M250 341 c4 2 8 8 4 8 c-2 0 -4 -4 -4 -8",
                stroke=t["red"], **s))
    for x, y in TUFTS:
        a.append(el("path", d=petals(x, y - 6, 5, 3.5, 2) + f" M{x} {y - 2} v6", stroke=t["blue_pale"], **s))
    return "".join(a)


def night_art(t, uid, s):
    """World 3: the moon, stars, a comet, a lake and a crater, on the same paper."""
    hr, hb, a = f"url(#hr{uid})", f"url(#hb{uid})", []
    # hills along the bottom, at night
    hill = "M58 800 V770 Q120 742 190 768 Q250 736 320 760 Q345 752 360 756 V800 Z"
    a.append(el("path", d=hill, fill=hb))
    a.append(el("path", d="M58 770 Q120 742 190 768 Q250 736 320 760 Q345 752 360 756", stroke=t["blue"], **s))
    # the moon and twin stars beside the boss (3-6, Full Moon; 3-18, Twin Stars)
    moon = "M310 184 a26 26 0 1 0 0 52 a34 34 0 0 1 0 -52 z"
    a.append(el("path", d=moon, fill=hb))
    a.append(el("path", d=moon, stroke=t["blue"], **s))
    for cx, cy in ((234, 204), (256, 222)):
        a.append(el("path", d=star_path(cx, cy, 9), fill=hr))
        a.append(el("path", d=star_path(cx, cy, 9), stroke=t["red"], **s))
    # a telescope
    tube = "M70 346 L104 326 L109 334 L75 354 Z"
    a.append(el("path", d=tube, fill=hb))
    a.append(el("path", d=tube + " M70 346 l-5 3 l4 7 l5 -3 M90 344 L78 376 M90 344 V376 M90 344 L102 376",
                stroke=t["blue"], **s))
    # a ringed planet
    a.append(el("circle", cx=308, cy=478, r=15, fill=hr))
    a.append(el("circle", cx=308, cy=478, r=15, stroke=t["red"], **s))
    a.append(el("ellipse", cx=308, cy=478, rx=29, ry=7, transform="rotate(-15 308 478)", stroke=t["red"], **s))
    # a lake with the moon in it (3-1, the lake)
    a.append(el("ellipse", cx=96, cy=604, rx=34, ry=11, fill=hb))
    a.append(el("path", d="M62 604 a34 11 0 1 0 68 0 a34 11 0 1 0 -68 0 M94 598 a5 5 0 1 0 0 10 a7 7 0 0 1 0 -10 "
                          "M72 606 h10 M110 602 h10", stroke=t["blue"], **s))
    # a crater (3-12, Crater)
    rim = "M280 696 q24 -26 48 0 z"
    a.append(el("path", d=rim, fill=hr))
    a.append(el("path", d=rim + " M290 696 q14 -12 28 0", stroke=t["red"], **s))
    # a small comet and twinkles
    a.append(el("path", d="M262 332 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 M262 335 l-24 12 M264 338 l-18 16 "
                          "M261 331 l-26 4", stroke=t["red"], **s))
    for x, y in TUFTS:
        a.append(el("path", d=f"M{x} {y - 10} v10 M{x - 5} {y - 5} h10", stroke=t["blue_pale"], **s))
    return "".join(a)


def sea_art(t, uid, s):
    """World 4: waves, an island, a boat, rocks, an oyster with its pearl and a fish."""
    hr, hb, a = f"url(#hr{uid})", f"url(#hb{uid})", []
    # the sea along the bottom
    wave = "M58 766" + " q11 -8 22 0 q11 8 22 0" * 7
    a.append(el("path", d=wave + " V800 H58 Z", fill=hb))
    a.append(el("path", d=wave + " M62 784" + " q10 -6 20 0 q10 6 20 0" * 7, stroke=t["blue"], **s))
    # an island with a palm beside the boss
    isle = "M226 238 q52 -32 104 0 z"
    a.append(el("path", d=isle, fill=hr))
    a.append(el("path", d=isle + " M282 222 q2 -14 8 -30 M290 192 q-14 -4 -22 6 M290 192 q-8 -12 -20 -10 "
                                 "M290 192 q12 -8 22 -2 M290 192 q10 2 14 14", stroke=t["red"], **s))
    a.append(el("path", d="M214 244 q6 -4 12 0 q6 4 12 0 M318 244 q6 -4 12 0 q6 4 12 0", stroke=t["blue"], **s))
    # a sailing boat
    hull = "M66 360 h44 l-8 12 h-28 z"
    a.append(el("path", d=hull, fill=hb))
    a.append(el("path", d=hull + " M88 360 V320", stroke=t["blue"], **s))
    sail = "M91 324 L91 356 L112 356 z"
    a.append(el("path", d=sail, fill=hr))
    a.append(el("path", d=sail + " M88 320 l9 3 l-9 3", stroke=t["red"], **s))
    a.append(el("path", d="M60 378" + " q6 -4 12 0" * 5, stroke=t["blue_pale"], **s))
    # rocks (4-6, Rocky Shore)
    rocks = "M282 504 q2 -22 19 -24 q15 0 17 24 z M314 504 q4 -13 14 -13 q11 2 11 13 z"
    a.append(el("path", d=rocks, fill=hb))
    a.append(el("path", d=rocks, stroke=t["blue"], **s))
    a.append(el("path", d="M276 508" + " q6 -4 12 0" * 6, stroke=t["blue_pale"], **s))
    # a scallop shell and its pearl (4-12, Pearls)
    shell = "M84 612 l-18 -22 q18 -20 36 0 z"
    ribs = " ".join(f"M84 612 L{x} {y}" for x, y in ((72, 584), (80, 578), (88, 578), (96, 584)))
    a.append(el("path", d=shell, fill=hr))
    a.append(el("path", d=shell + " " + ribs + " M78 612 h12 l-2 5 h-8 z", stroke=t["red"], **s))
    a.append(el("circle", cx=116, cy=608, r=7, fill=hb))
    a.append(el("circle", cx=116, cy=608, r=7, stroke=t["blue"], **s))
    # a jumping fish
    fish = "M284 684 q15 -15 30 0 q-15 11 -30 0 z"
    a.append(el("path", d=fish, fill=hb))
    a.append(el("path", d=fish + " M314 684 l9 -7 v14 z", stroke=t["blue"], **s))
    a.append(el("circle", cx=292, cy=682, r=1, fill=t["blue"]))
    a.append(el("path", d="M280 702 q4 -5 8 0 M316 702 q4 -5 8 0 M298 704 q4 -5 8 0", stroke=t["blue_pale"], **s))
    # a gull and small waves
    a.append(el("path", d="M240 338 q5 -7 10 0 q5 -7 10 0", stroke=t["blue"], **s))
    for x, y in TUFTS:
        a.append(el("path", d=f"M{x - 8} {y - 3} q4 -4 8 0 q4 4 8 0", stroke=t["blue_pale"], **s))
    return "".join(a)


def boss_stop(t, world):
    """A world's boss stop in a 68 x 76 box, in the ink of its state; the flag is red."""
    ink = dict(stroke=t["pencil"], stroke_width=2, stroke_linejoin="round", stroke_linecap="round")
    flag = lambda x, y: el("path", d=f"M{x} {y} l13 5 -13 5 z", fill=t["red"])
    ground = el("path", d="M2 70 h64", stroke=t["pencil"], stroke_width=2, stroke_linecap="round")
    if world == 1:
        return (el("path", d="M8 70 V34 h9 v-7 h7 v7 h7 v-7 h7 v7 h7 v-7 h7 v7 h9 V70 z", fill=t["paper"],
                   stroke=t["pencil"], stroke_width=2, stroke_linejoin="round") +
                el("path", d="M27 70 v-13 a7 7 0 0 1 14 0 v13 M34 27 V8", fill="none", stroke=t["pencil"],
                   stroke_width=2, stroke_linecap="round") +
                el("path", d="M34 8 l13 5 -13 5 z", fill=t["red"]) + ground)
    if world == 2:  # the Mole: a garden shed with a molehill at its door
        return (el("path", d="M12 70 V38 L36 20 L60 38 V70 z", fill=t["paper"], **ink) +
                el("path", d="M30 70 V54 h12 V70 M18 44 h8 v8 h-8 z M36 20 V6", fill="none", **ink) + flag(36, 6) +
                el("path", d="M0 70 q11 -18 24 0 z", fill=t["paper"], **ink) +
                el("path", d="M7 63 l2 -2 M14 61 l2 1 M10 67 l2 -1", fill="none", **ink) + ground)
    if world == 3:  # the Comet: an observatory with its telescope out
        return (el("path", d="M14 70 V44 h40 V70 z", fill=t["paper"], **ink) +
                el("path", d="M12 44 a22 22 0 0 1 44 0 z", fill=t["paper"], **ink) +
                el("path", d="M30 70 v-12 a4 4 0 0 1 8 0 v12 M37 34 l13 -14 M20 30 V10", fill="none", **ink) +
                flag(20, 10) + ground)
    # the Kraken: a tower on a rock, with tentacles round it
    return (el("path", d="M20 70 V34 h-2 v-8 h6 v5 h5 v-5 h6 v5 h5 v-5 h6 v8 h-2 V70 z", fill=t["paper"], **ink) +
            el("path", d="M30 70 v-10 a4 4 0 0 1 8 0 v10 M33 26 V8 M18 70 q-11 -4 -11 -17 q0 -8 6 -8 q4 0 3 4 "
                          "M50 70 q11 -6 10 -21 q-1 -6 -6 -5 q-3 1 -1 4", fill="none", **ink) + flag(33, 8) +
            el("path", d="M0 72" + " q4.25 -3 8.5 0" * 8, fill="none", **ink))


def map_stops(t, done=3, world=1):
    """The path and the stops. The path is inked up to the stop at index done - 1."""
    stops = STOPS if world == 1 else STOPS_LATE
    out = []
    done_pts = [(130, 820)] + [(x, y) for (_, x, y, st, _) in stops[:done]]
    rest = [(x, y) for (_, x, y, st, _) in stops[done - 1:]]
    paths = (el("path", d=spline(done_pts), fill="none", stroke=t["blue"], stroke_width=2.2, stroke_dasharray="7 6",
                stroke_linecap="round") +
             el("path", d=spline(rest), fill="none", stroke=t["graphite"], stroke_width=2.2, stroke_dasharray="1 7",
                stroke_linecap="round"))
    out.append(svg(W, H, paths, style="position: absolute; left: 0; top: 0"))
    for i, (level, x, y, st, stars) in enumerate(stops):
        if st == "done":
            d = 44
            out.append(f'<button type="button" aria-label="Level {level}, {stars} stars" style="position: absolute; '
                       f'left: {x - d // 2}px; top: {y - d // 2}px; width: {d}px; height: {d}px; border: none; '
                       f'border-radius: 50%; background: {t["blue"]}; color: {t["paper"]}; font-family: inherit; '
                       f'font-size: 16px; font-weight: 800; padding: 0">{level}</button>')
            st_html = "".join(star(t, 13, i < stars) for i in range(3))
            out.append(f'<span style="position: absolute; left: {x - 21}px; top: {y - d // 2 - 16}px; display: flex; '
                       f'gap: 1px">{st_html}</span>')
        elif st == "next":
            d = 56
            out.append(f'<button type="button" aria-label="Level {level}, next" style="position: absolute; '
                       f'left: {x - d // 2}px; top: {y - d // 2}px; width: {d}px; height: {d}px; box-sizing: border-box; '
                       f'border: 3px solid {t["red"]}; border-radius: 50%; background: {t["card"]}; '
                       f'box-shadow: 0 0 0 4px {t["paper"]}, 0 0 0 6.5px {t["red"]}; color: {t["red"]}; '
                       f'font-family: inherit; font-size: 20px; font-weight: 800; padding: 0">{level}</button>')
            arrow = svg(34, 20, el("path", d="M32 12 q-14 6 -28 -2 M4 10 l7 -6 M4 10 l8 3", fill="none", stroke=t["red"],
                                   stroke_width=2, stroke_linecap="round", stroke_linejoin="round"),
                        style="transform: scaleX(-1)")
            out.append(f'<span aria-hidden="true" style="position: absolute; left: {x - 104}px; top: {y - 20}px; '
                       f'display: flex; align-items: center; gap: 2px; {HAND24}; font-size: 28px; color: {t["red"]}">'
                       f'next!{arrow}</span>')
        elif st == "locked":
            d = 44
            out.append(f'<button type="button" aria-label="Level {level}, locked" style="position: absolute; '
                       f'left: {x - d // 2}px; top: {y - d // 2}px; width: {d}px; height: {d}px; box-sizing: border-box; '
                       f'border: 2px dashed {t["graphite"]}; border-radius: 50%; background: {t["paper"]}; '
                       f'color: {t["graphite"]}; font-family: inherit; font-size: 16px; font-weight: 700; padding: 0">{level}</button>')
        elif st == "chest":
            body = (el("path", d="M8 22 q16 -14 32 0 v18 h-32 z", fill=t["card"], stroke=t["hi"], stroke_width=2,
                       stroke_linejoin="round") +
                    el("path", d="M8 26 h32 M21 24 h6 v7 h-6 z M12 12 l-3 -4 M24 7 v-5 M36 12 l3 -4", fill="none",
                       stroke=t["hi"], stroke_width=2, stroke_linecap="round", stroke_linejoin="round"))
            out.append(f'<button type="button" aria-label="Chest after level {stops[i - 1][0]}" style="position: absolute; '
                       f'left: {x - 24}px; top: {y - 26}px; width: 48px; height: 48px; border: none; background: transparent; '
                       f'padding: 0">{svg(48, 48, body, view="0 0 48 48")}</button>')
        elif st == "boss":
            body = boss_stop(t, world)
            out.append(f'<button type="button" aria-label="Level {level}, boss" style="position: absolute; left: {x - 34}px; '
                       f'top: {y - 40}px; width: 68px; height: 96px; border: none; background: transparent; padding: 0">'
                       f'{svg(68, 76, body, view="0 0 68 76")}'
                       f'<span style="display: block; text-align: center; {LABEL}; color: {t["pencil"]}">Boss {level}</span></button>')
    return "".join(out)


def map_frame(t, uid, extra="", done=3, world=1):
    name, title, stars = WORLDS[world]
    parts = [svg(W, H, map_art(t, uid, world), style="position: absolute; left: 0; top: 0"), map_stops(t, done, world)]
    parts.append(f'<div style="position: absolute; left: 70px; top: 26px">'
                 f'<div style="{LABEL}; color: {t["graphite"]}">World {world}</div>'
                 f'<div style="{DISPLAY}; color: {t["blue"]}">{name}</div>'
                 f'<div style="display: flex; align-items: center; gap: 6px; {LABEL}">{title}'
                 f'<span style="color: {t["graphite"]}">·</span>{star(t, 16, True)}{stars}</div></div>')
    parts.append(f'<div style="position: absolute; right: 6px; top: 26px">{icon_button(t, "settings", "Settings", t["pencil"])}</div>')
    parts.append(f'<button type="button" style="position: absolute; right: 12px; top: 120px; width: 158px; height: 56px; '
                 f'border: none; padding: 6px 8px; display: flex; align-items: center; gap: 6px; text-align: left; '
                 f'font-family: inherit; {note_style(t, 3)}">{tape(t, 40, -8, 44, -4)}'
                 f'{icon(ICONS["calendar"], t["red"], 24)}<span style="display: flex; flex-direction: column">'
                 f'<span style="{HAND24}; white-space: nowrap">Today\'s puzzle</span>'
                 f'<span style="{SMALL}; font-weight: 700; color: {t["graphite"]}">Streak 6</span></span></button>')
    icons = "".join(
        f'<button type="button" style="width: 56px; height: 52px; border: none; background: transparent; padding: 0; '
        f'display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; '
        f'font-family: inherit; {SMALL}; font-weight: 700; color: {t["graphite"]}">{icon(ICONS[n], t["blue"], 24)}{lab}</button>'
        for n, lab in (("shop", "Shop"), ("stats", "Stats"), ("help", "Rules")))
    parts.append(f'<div style="position: absolute; left: 1px; bottom: 22px; display: flex; flex-direction: column; '
                 f'gap: 4px">{icons}</div>')
    people = svg(28, 22, el("circle", cx=9, cy=7, r=4, fill="none", stroke=t["red"], stroke_width=1.8) +
                 el("path", d="M2 21 q7 -10 14 0", fill="none", stroke=t["red"], stroke_width=1.8) +
                 el("circle", cx=19, cy=7, r=4, fill="none", stroke=t["blue"], stroke_width=1.8) +
                 el("path", d="M12 21 q7 -10 14 0", fill="none", stroke=t["blue"], stroke_width=1.8), view="0 0 28 22")
    parts.append(f'<button type="button" style="position: absolute; right: 16px; bottom: 26px; height: 52px; '
                 f'box-sizing: border-box; border: 2px solid {t["graphite"]}; {BUTTON_SHAPE}; background: {t["card"]}; '
                 f'color: {t["pencil"]}; font-family: inherit; {LABEL}; display: flex; align-items: center; gap: 8px; '
                 f'padding: 0 16px">{people}Play a game</button>')
    return root(t, "".join(parts) + extra, grid=True, margin=True)


def level_card_frame(t, uid):
    torn = svg(340, 10, el("path", d="M0 10 L0 4 " + " ".join(f"L{x} {4 + (3 if (x // 10) % 2 else -2)}" for x in range(10, 341, 10)) + " L340 10 Z",
                           fill=t["card"]), view="0 0 340 10", style="position: absolute; left: 0; top: -9px; width: 100%")
    chips = "".join(
        f'<button type="button" aria-label="{name}, {n}: what it does" style="height: 48px; box-sizing: border-box; '
        f'border: 1.5px dashed {t["blue"]}; border-radius: 24px; background: transparent; display: flex; '
        f'align-items: center; gap: 4px; padding: 0 12px; font-family: inherit; {LABEL}; color: {t["blue"]}">'
        f'{icon(ICONS[ic], t["blue"], 20, 2)}×{n}</button>'
        for name, ic, n in (("Wall", "wall", 2), ("Hint", "hint", 1)))
    rows = "".join(
        f'<div style="display: flex; align-items: center; gap: 8px"><span style="display: flex">'
        f'{"".join(star(t, 16, i < k) for i in range(3))}</span><span style="{BODY}">{text}</span></div>'
        for k, text in ((1, "Win"), (2, "Win with 8 dots to spare"), (3, "Win with 14 dots to spare")))
    card = (f'<div role="dialog" aria-label="Level 7, Siege" style="position: absolute; left: 10px; right: 10px; bottom: 16px; '
            f'box-sizing: border-box; padding: 14px 16px 16px; display: flex; flex-direction: column; gap: 12px; '
            f'{note_style(t, -0.8)}">{torn}'
            f'<div style="display: flex; align-items: flex-start">'
            f'<div style="flex: 1"><div style="{LABEL}; color: {t["graphite"]}">World 1 · Level 7</div>'
            f'<div style="{DISPLAY}; color: {t["blue"]}">Siege</div></div>'
            f'{icon_button(t, "close", "Close", style="margin: -6px -8px 0 0")}</div>'
            f'<div style="display: flex; align-items: center; gap: 8px; margin-top: -6px">{icon(ICONS["target"], t["red"], 24)}'
            f'<span style="{TITLE}">Capture 8 dots</span></div>'
            f'<div style="display: flex; gap: 14px; align-items: center">'
            f'<div style="border: 1px solid {t["grid"]}; flex-shrink: 0">{board_svg(t, "opening", uid + "m", size=120, sp=8, mini=True)}</div>'
            f'<div style="display: flex; flex-direction: column; gap: 6px; min-width: 0">'
            f'<div style="display: flex; align-items: center; gap: 8px">{face(t, "determined", 48)}'
            f'<div><div style="{LABEL}; color: {t["blue"]}">Computer</div>'
            f'<div style="{SMALL}; color: {t["graphite"]}">plays sharp<br>starts 6 dots ahead</div></div></div>'
            f'<div style="{SMALL}; color: {t["graphite"]}">You have 30 dots. Boosters:</div>'
            f'<div style="display: flex; gap: 8px">{chips}</div></div></div>'
            f'<div style="display: flex; flex-direction: column; gap: 2px">{rows}</div>'
            f'{main_button(t, "Start")}</div>')
    scrim = f'<div aria-hidden="true" style="position: absolute; inset: 0; background: {t["scrim"]}"></div>'
    return map_frame(t, uid, scrim + card)


# ---------------------------------------------------- the Shop's small boards ----
# The Shop (D-27 wireframes, Shop) shows every item on a small board of 5 x 5 points: a red ring round
# one blue piece, and a few loose pieces of each side. Themes draw their own; a piece style or a ring
# effect is drawn in the theme in use, in the version the phone picks (docs/design/cosmetics.md).

SHOP_BOARD = dict(red=[(1, 2), (2, 1), (3, 2), (2, 3), (0, 4)], blue=[(2, 2), (4, 1), (3, 4), (0, 0)],
                  areas=[area("red", [(1, 2), (2, 1), (3, 2), (2, 3)], [(2, 2)])], last=(3, 4))
SHOP_ITEMS = {  # tab: [(name, piece style, ring effect, status, status ink)]
    "pieces": [("Standard", None, None, "In use", "red"), ("Hearts and clubs", "cards", None, "Owned", "graphite"),
               ("Flame and frost", "fire", None, "$0.99", "pencil")],
    "rings": [("Standard", None, None, "In use", "red"), ("Lasso", None, "lasso", "$0.99", "pencil"),
              ("Rubber stamp", None, "stamp", "Owned", "graphite")],
}


def shop_board(t, pieces, rings, uid, size):
    """An item on the Shop's small board, drawn in the theme in use (here t)."""
    board = dict(t, style=t.get("style", "notebook"), pieces=pieces, rings=rings)
    return theme_board_svg(board, SHOP_BOARD, uid, size=size, sp=size / 5)


def shop_frame(t, uid, tab):
    """The Shop on its Pieces or Rings tab, as on the D-27 wireframe, with each item on its small board.
    The screen is Notebook's in every theme; the boards are drawn in the theme in use, here Notebook."""
    tabs = "".join(
        f'<button type="button" role="tab" aria-selected="{"true" if key == tab else "false"}" style="height: 48px; '
        + (f'border: 2px solid {t["blue"]}; border-bottom: none; border-radius: 12px 14px 0 0; background: {t["card"]}; '
           f'color: {t["blue"]}; margin-bottom: -2px; ' if key == tab else
           f'border: none; background: transparent; color: {t["graphite"]}; ')
        + f'font-family: inherit; {LABEL}; font-size: 15px; padding: 0 14px">{label}</button>'
        for key, label in (("themes", "Themes"), ("pieces", "Pieces"), ("rings", "Rings")))
    tiles = []
    for k, (name, pieces, rings, status, ink) in enumerate(SHOP_ITEMS[tab]):
        tiles.append(f'<button type="button" style="border: none; background: {t["card"]}; box-shadow: 0 1px 4px {t["shadow"]}; '
                     f'transform: rotate({(-1, 1, 0.5, -0.8)[k]}deg); color: {t["pencil"]}; font-family: inherit; padding: 8px; '
                     f'display: flex; flex-direction: column; gap: 6px; text-align: left">'
                     f'{shop_board(t, pieces, rings, f"{uid}{k}", 140)}'
                     f'<span style="{HAND24}">{name}</span>'
                     f'<span style="{SMALL}; font-weight: 800; color: {t[ink]}">{status}</span></button>')
    back = icon("M15 5l-7 7 7 7", t["blue"], 26, 2)
    inner = (f'<div style="position: absolute; left: 4px; top: 28px; display: flex; align-items: center; gap: 8px">'
             f'<button type="button" aria-label="Back to the map" style="width: 48px; height: 48px; border: none; '
             f'background: transparent; padding: 0; display: flex; align-items: center; justify-content: center">{back}</button>'
             f'<span style="{DISPLAY}; color: {t["blue"]}">Shop</span></div>'
             f'<div role="tablist" aria-label="Kinds" style="position: absolute; left: 0; right: 0; top: 92px; display: flex; '
             f'gap: 4px; padding: 0 16px; border-bottom: 2px solid {t["blue"]}">{tabs}</div>'
             f'<div style="position: absolute; left: 16px; right: 16px; top: 160px; display: grid; '
             f'grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px">{"".join(tiles)}</div>'
             f'<div style="position: absolute; left: 0; right: 0; bottom: 28px; text-align: center; {LABEL}; '
             f'color: {t["graphite"]}; text-decoration: underline">Restore purchases</div>')
    return root(t, inner, grid=True)


# ------------------------------------------------------------------ output ----

SMALL_PHONE = (320, 568)
FRAMES = [  # file stem, canvas name, title, drawer, size
    ("game-start", "GameStart", "Game: the level opens", lambda t, u: game_frame(t, "opening", u), (W, H)),
    ("game-threat", "GameThreat", "Game: a threat", lambda t, u: game_frame(t, "threat", u), (W, H)),
    ("game-big-capture", "GameBigCapture", "Game: right after a big capture", lambda t, u: game_frame(t, "big", u), (W, H)),
    ("result-win", "ResultWin", "Result: won, two stars", lambda t, u: game_frame(t, "win", u), (W, H)),
    ("map", "Map", "World map (home)", lambda t, u: map_frame(t, u), (W, H)),
    ("level-card", "LevelCard", "Level card", lambda t, u: level_card_frame(t, u), (W, H)),
    ("game-undo", "GameUndo", "Game: Undo's preview", lambda t, u: game_frame(t, "undo", u), (W, H)),
    ("boosters", "Boosters", "Boosters", lambda t, u: boosters_frame(t, u), (W, H)),
    ("small-game-threat", "SmallGameThreat", "Small phone: a threat", lambda t, u: small_game_frame(t, "threat", u),
     SMALL_PHONE),
    ("small-game-undo", "SmallGameUndo", "Small phone: Undo's preview", lambda t, u: small_game_frame(t, "undo", u),
     SMALL_PHONE),
    ("game-drag", "GameDrag", "Game: placing by drag", lambda t, u: game_frame(t, "drag", u), (W, H)),
    ("small-game-drag", "SmallGameDrag", "Small phone: placing by drag", lambda t, u: small_game_frame(t, "drag", u),
     SMALL_PHONE),
    ("game-wall-drag", "GameWallDrag", "Game: Wall aimed by drag", lambda t, u: game_frame(t, "wall-aim", u), (W, H)),
    ("small-game-wall-drag", "SmallGameWallDrag", "Small phone: Wall aimed by drag",
     lambda t, u: small_game_frame(t, "wall-aim", u), SMALL_PHONE),
    ("game-bomb-drag", "GameBombDrag", "Game: Bomb aimed by drag", lambda t, u: game_frame(t, "bomb-aim", u), (W, H)),
    ("game-threat-links", "GameThreatLinks", "Game: a threat, links on",
     lambda t, u: game_frame(t, "threat", u, links=True), (W, H)),
    # Free play and two players: the setup, Stop and Done (D-16).
    ("play-setup", "PlaySetup", "Play a game: the setup", lambda t, u: setup_frame(t, u), (W, H)),
    ("play-replace", "PlayReplace", "Play a game: replace the saved game?",
     lambda t, u: setup_frame(t, u, replace=True), (W, H)),
    ("game-free-open", "GameFreeOpen", "Free play: an open-ended game", lambda t, u: free_frame(t, "free-open", u), (W, H)),
    ("game-free-stop", "GameFreeStop", "Free play: Stop's note", lambda t, u: free_frame(t, "free-stop", u), (W, H)),
    ("game-computer-stops", "GameComputerStops", "Free play: the computer stopped",
     lambda t, u: free_frame(t, "free-computer-stops", u), (W, H)),
    ("game-two-done", "GameTwoDone", "Two players: Red stopped, Blue collects",
     lambda t, u: free_frame(t, "two-done", u), (W, H)),
    ("small-play-setup", "SmallPlaySetup", "Small phone: Play a game, the setup", lambda t, u: small_setup_frame(t, u),
     SMALL_PHONE),
] + [
    (f"map-world-{n}", f"MapWorld{n}", f"Map page: World {n}, {WORLDS[n][0]}",
     lambda t, u, n=n: map_frame(t, u, world=n), (W, H)) for n in (2, 3, 4)
] + [
    (f"game-world-{n}", f"GameWorld{n}", f"Game: a threat in World {n}, with its board touches",
     lambda t, u, n=n: game_frame(t, "threat", u, world=n), (W, H)) for n in (2, 3, 4)
]


# The board themes draw the game frames only, plus the threat with high-contrast pieces.
THEME_STEMS = ("game-start", "game-threat", "game-big-capture", "game-drag", "result-win", "small-game-threat")
THEME_FRAMES = [fr for stem in THEME_STEMS for fr in FRAMES if fr[0] == stem] + [
    ("game-threat-contrast", "GameThreatContrast", "Game: a threat, high-contrast pieces",
     lambda t, u: game_frame(t, "threat", u, contrast=True), (W, H)),
]
THEME_NAMES = {"star-chart": ("StarChart", "Star chart"), "neon-arcade": ("NeonArcade", "Neon arcade")}

# The piece styles and ring effects (docs/design/cosmetics.md): each in Notebook and in one other theme,
# light and dark. A piece style on the threat, which shows every piece and mark; a ring effect right
# after the big capture, which shows three rings. Then the Shop's two tabs for them.
COSMETICS = [  # file name, canvas name, title, piece style, ring effect, moment, the other theme
    ("hearts-and-clubs", "HeartsAndClubs", "Hearts and clubs", "cards", None, "threat", "neon-arcade"),
    ("flame-and-frost", "FlameAndFrost", "Flame and frost", "fire", None, "threat", "star-chart"),
    ("lasso", "Lasso", "Lasso", None, "lasso", "big", "star-chart"),
    ("rubber-stamp", "RubberStamp", "Rubber stamp", None, "stamp", "big", "neon-arcade"),
]
SHOP_FRAMES = [("shop-pieces", "ShopPieces", "Shop: piece styles", "pieces"),
               ("shop-rings", "ShopRings", "Shop: ring effects", "rings")]


def jobs(only=None):
    """Every frame to draw: (file stem, canvas file name, title, drawer, size, theme colours, uid)."""
    out = []
    if only in (None, "notebook"):
        for stem, name, title, draw, size in FRAMES:
            for version, t in THEMES.items():
                out.append((f"{stem}-{version}", name + ("" if version == "light" else "Dark"),
                            f"{title}, {version}", draw, size, t, f"{name}{version[0]}"))
    for theme, versions in BOARD_THEMES.items():
        if only not in (None, theme):
            continue
        camel, label = THEME_NAMES[theme]
        for stem, name, title, draw, size in THEME_FRAMES:
            for version, t in versions.items():
                out.append((f"{stem}-{theme}-{version}", name + camel + ("" if version == "light" else "Dark"),
                            f"{title}, {label}, {version}", draw, size, t, f"{name}{camel[0]}{version[0]}"))
    if only in (None, "cosmetics"):
        for stem, name, title, pieces, rings, moment, other in COSMETICS:
            for theme in ("notebook", other):
                camel, label = THEME_NAMES.get(theme, ("Notebook", "Notebook"))
                for version in ("light", "dark"):
                    base = dict(THEMES[version], style="notebook") if theme == "notebook" else BOARD_THEMES[theme][version]
                    t = dict(base, pieces=pieces, rings=rings)
                    out.append((f"cosmetic-{stem}-{theme}-{version}", name + camel + ("" if version == "light" else "Dark"),
                                f"{title}, {label}, {version}", lambda t, u, m=moment: game_frame(t, m, u), (W, H), t,
                                f"{name}{camel[0]}{version[0]}"))
        for stem, name, title, tab in SHOP_FRAMES:
            for version, t in THEMES.items():
                out.append((f"{stem}-{version}", name + ("" if version == "light" else "Dark"), f"{title}, {version}",
                            lambda t, u, tab=tab: shop_frame(t, u, tab), (W, H), t, f"{name}{version[0]}"))
    return out


def page(body, title):
    return (f'<!doctype html><html lang="en"><head><meta charset="utf-8"><title>{title}</title>'
            f'<link rel="stylesheet" href="{FONTS.replace("&", "&amp;")}">'
            f'<style>body{{margin:0}} button{{cursor:pointer}}</style></head><body>{body}</body></html>\n')


def dc_page(body, title, size=(W, H)):
    props = '{"$preview":{"width":%d,"height":%d}}' % size
    return (f'<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>{title}</title>\n'
            f'<script src="./support.js"></script>\n</head>\n<body>\n<x-dc>\n<helmet>\n'
            f'<link rel="stylesheet" href="{FONTS.replace("&", "&amp;")}">\n<style>\nbody{{margin:0}}\n</style>\n'
            f'</helmet>\n{body}\n</x-dc>\n<script type="text/x-dc" data-dc-script data-props=\'{props}\'>\n'
            f'class Component extends DCLogic {{\nrenderVals() {{\nreturn {{}};\n}}\n}}\n</script>\n</body>\n</html>\n')


def main():
    canvas = None
    if "--canvas" in sys.argv:
        canvas = os.path.join(sys.argv[sys.argv.index("--canvas") + 1], "project")
        os.makedirs(canvas, exist_ok=True)
    only = sys.argv[sys.argv.index("--only") + 1] if "--only" in sys.argv else None
    with tempfile.TemporaryDirectory() as tmp:
        for stem, dc_name, full, draw, size, t, uid in jobs(only):
            body = draw(t, uid)
            if canvas:
                with open(os.path.join(canvas, dc_name + ".dc.html"), "w") as fh:
                    fh.write(dc_page(body, full, size))
            src = os.path.join(tmp, f"{stem}.html")
            with open(src, "w") as fh:
                fh.write(page(body, full))
            out = os.path.join(HERE, f"{stem}.png")
            subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                            f"--force-device-scale-factor={SCALE}", f"--window-size={size[0]},{size[1]}",
                            "--virtual-time-budget=15000", f"--screenshot={out}", "file://" + src],
                           check=True, capture_output=True)
            print(out)


if __name__ == "__main__":
    main()
