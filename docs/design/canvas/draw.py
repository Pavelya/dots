"""Draws the canvas mock-ups for docs/design/canvas/README.md.

Run from the repository root (needs Python 3 and Google Chrome, and a network connection for the
two Google Fonts):

    python3 docs/design/canvas/draw.py                  # the PNGs in docs/design/canvas/
    python3 docs/design/canvas/draw.py --canvas DIR     # also the design canvas files, in DIR/project/

Every frame is 360 x 800 dp, the size of a common Android phone, drawn in the light and the dark
version. The PNGs are 3 px per dp (1080 x 2400). Every colour and size comes from
docs/design/theme-notebook.md and docs/design/pieces.md. Set CHROME to use another Chrome binary.
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
}


def bowed_segments(points, seed):
    """The ring as a pen stroke: each side bows by up to 0.4 dp and varies in width by up to 15%."""
    rnd = random.Random(seed)
    segs, path = [], []
    n = len(points)
    for i in range(n):
        (x1, y1), (x2, y2) = points[i], points[(i + 1) % n]
        length = math.hypot(x2 - x1, y2 - y1)
        nx, ny = -(y2 - y1) / length, (x2 - x1) / length
        k = rnd.uniform(-0.4, 0.4) * 2
        qx, qy = (x1 + x2) / 2 + nx * k, (y1 + y2) / 2 + ny * k
        segs.append((f"M{f(x1)} {f(y1)} Q{f(qx)} {f(qy)} {f(x2)} {f(y2)}", 2 * (1 + rnd.uniform(-0.15, 0.15))))
        path.append((f"M{f(x1)} {f(y1)} " if i == 0 else "") + f"Q{f(qx)} {f(qy)} {f(x2)} {f(y2)}")
    return segs, " ".join(path) + " Z"


def board_svg(t, key, uid, size=360, sp=24.0, mini=False):
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
    links = []
    if not mini:
        for side in ("red", "blue"):
            live = sorted(p for p in pos[side] if p not in captured)
            for i, p in enumerate(live):
                for q in live[i + 1:]:
                    if max(abs(p[0] - q[0]), abs(p[1] - q[1])) == 1:
                        (x1, y1), (x2, y2) = c(p), c(q)
                        links.append(el("line", x1=f(x1), y1=f(y1), x2=f(x2), y2=f(y2),
                                        stroke=t[side + "_pale"], stroke_width=1.5, stroke_linecap="round"))
    marks = []
    for p in pos.get("near", []):
        x, y = c(p)
        marks.append(el("circle", cx=f(x), cy=f(y), r=f(0.42 * sp), fill="none", stroke=t["hi"], stroke_width=2))
    pieces = []
    for side in ("red", "blue"):
        for p in pos[side]:
            x, y = c(p)
            if side == "red":
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(dot / 2), fill=t["red"]))
            else:
                h = dot * 7 / 12
                pieces.append(el("path", d=f"M{f(x)} {f(y - h)}L{f(x + h)} {f(y)}L{f(x)} {f(y + h)}L{f(x - h)} {f(y)}Z",
                                 fill=t["blue"]))
            if p in captured and not mini:
                L = dot * 0.75 / math.sqrt(2)
                if side == "red":  # captured by blue: the cut falls to the right
                    d, col = f"M{f(x - L)} {f(y - L)}L{f(x + L)} {f(y + L)}", t["blue_wash"]
                else:
                    d, col = f"M{f(x - L)} {f(y + L)}L{f(x + L)} {f(y - L)}", t["red_wash"]
                pieces.append(el("path", d=d, stroke=col, stroke_width=f(dot / 6), fill="none"))
            if p == pos["last"] and not mini:
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(dot * 0.15), fill=t["paper"]))
    for p, side in pos.get("closing", []):
        x, y = c(p)
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(sp / 6), fill="none", stroke=t[side], stroke_width=1.5))
    if pos.get("burst") and not mini:
        cx, cy = c((4.5, 5))
        for k in range(14):
            a = k * 2 * math.pi / 14 + 0.2
            r1, r2 = (64, 78) if k % 2 else (66, 74)
            ends = [(cx + math.cos(a) * r * 1.0, cy + math.sin(a) * r * 1.1) for r in (r1, (r1 + r2) / 2, r2)]
            if any(math.hypot(ex - c(p)[0], ey - c(p)[1]) < 12 for ex, ey in ends for p in pos["red"] + pos["blue"]):
                continue  # a stroke never crosses a piece
            pieces.append(el("line", x1=f(cx + math.cos(a) * r1), y1=f(cy + math.sin(a) * r1 * 1.1),
                             x2=f(cx + math.cos(a) * r2), y2=f(cy + math.sin(a) * r2 * 1.1),
                             stroke=t["red"], stroke_width=2.5, stroke_linecap="round"))
    body = (f"<defs>{''.join(defs)}</defs>" + el("rect", width=size, height=size, fill=t["paper"]) +
            "".join(washes) + "".join(grid) + "".join(links) + "".join(rings) + "".join(marks) + "".join(pieces))
    return svg(size, size, body, label=None if mini else "The board, 15 by 15 points")


# ------------------------------------------------------------- drawn parts ----

def icon(d, ink, size=24, sw=1.8, fill="none"):
    return svg(size, size, el("path", d=d, fill=fill, stroke=ink, stroke_width=sw, stroke_linecap="round",
                              stroke_linejoin="round"), view="0 0 24 24")


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
}
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
    parts = [
        el("ellipse", cx=50, cy=93, rx=30, ry=4, fill=t["blue_pale"]),
        el("path", d=blob, fill=t["blue"]),
        el("circle", cx=88, cy=26, r=3.5, fill=t["blue"]),
        el("circle", cx=14, cy=82, r=2.5, fill=t["blue"]),
        el("circle", cx=93, cy=38, r=2, fill=t["blue"]),
    ]
    white, pupil, line = "#FBF8F1", "#2E2E2E", t["paper"]
    eyes = {"happy": (0, 0, 9), "smug": (3, 1, 9), "upset": (0, 2, 7), "determined": (-2, 0, 8), "sulky": (-4, 2, 8)}[mood]
    for ex in (37, 63):
        parts.append(el("ellipse", cx=ex, cy=46, rx=8, ry=eyes[2], fill=white))
        parts.append(el("circle", cx=ex + eyes[0], cy=47 + eyes[1], r=3.6, fill=pupil))
        parts.append(el("circle", cx=ex + eyes[0] + 1.3, cy=45.5 + eyes[1], r=1.1, fill=white))
    if mood == "smug":  # heavy lids
        parts.append(el("path", d="M28 44 Q37 36 46 44 Z M54 44 Q63 36 72 44 Z", fill=t["blue"]))
        parts.append(el("path", d="M28 44 H46 M54 44 H72", stroke=line, stroke_width=2, stroke_linecap="round"))
    if mood == "sulky":
        parts.append(el("path", d="M28 42 Q37 38 46 42 L46 40 Q37 33 28 40Z M54 42 Q63 38 72 42 L72 40 Q63 33 54 40Z", fill=t["blue"]))
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
            f'height: 16px; background: {t["hi_faint"]}; transform: rotate({angle}deg)"></span>')


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


def root(t, inner, grid=False, margin=False):
    bg = t["paper"]
    if grid:
        bg_img = (f'background-image: linear-gradient({t["grid"]} 1px, transparent 1px), '
                  f'linear-gradient(90deg, {t["grid"]} 1px, transparent 1px); background-size: 24px 24px; '
                  f'background-position: -1px -1px;')
    else:
        bg_img = ""
    m = (f'<div aria-hidden="true" style="position: absolute; left: 58px; top: 0; bottom: 0; width: 1px; '
         f'background: {t["margin"]}"></div>') if margin else ""
    return (f'<div style="position: relative; width: {W}px; height: {H}px; overflow: hidden; background-color: {bg}; '
            f'{bg_img} font-family: Nunito, system-ui, sans-serif; color: {t["pencil"]}">{m}{inner}</div>')


# -------------------------------------------------------------- game screen ----

MOMENTS = {
    "opening": dict(mood="happy", bubble="Good luck!", you=0, opp=0, left=30, mission=0),
    "threat": dict(mood="smug", bubble="Your move…", you=2, opp=1, left=13, mission=2),
    "big": dict(mood="upset", bubble="Ouch!", you=8, opp=1, left=12, mission=8),
    "win": dict(mood="sulky", bubble="Well played.", you=8, opp=1, left=12, mission=8),
}
BOARD_TOP = 308


def mission_note(t, done):
    boxes = []
    for i in range(8):
        tick = el("path", d="M2.5 6.5 l2.5 2.5 l5 -6", fill="none", stroke=t["paper"], stroke_width=1.8,
                  stroke_linecap="round", stroke_linejoin="round") if i < done else ""
        box = el("rect", x=0.75, y=0.75, width=10.5, height=10.5, rx=2.5,
                 fill=t["red"] if i < done else "none", stroke=t["red"] if i < done else t["graphite"], stroke_width=1.5)
        boxes.append(svg(12, 12, box + tick))
    head = "Mission done" if done >= 8 else "Mission"
    return (f'<div style="position: absolute; right: 12px; top: 30px; width: 136px; box-sizing: border-box; '
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
    shape = (el("circle", cx=7, cy=7, r=6, fill=ink) if side == "red" else
             el("path", d="M7 0 L14 7 L7 14 L0 7 Z", fill=ink))
    ul = (f'<span style="position: absolute; left: -4px; bottom: -7px">{underline(ink, len(name) * 11 + 10)}</span>'
          if turn else "")
    return (f'<div style="display: flex; align-items: center; gap: 8px; justify-content: {align}">'
            f'{svg(14, 14, shape)}<span style="position: relative; {TITLE}; color: {ink}">{name}{ul}</span>'
            f'{tally(score, ink)}<span style="{SCORE}; color: {ink}">{score}</span></div>')


def toolbar(t):
    tools = [("Wall", "wall", 2), ("Hint", "hint", 1), ("Bomb", "bomb", 0), ("Double dot", None, 1)]
    out = []
    for name, ic, n in tools:
        ink = t["blue"] if n else t["graphite"]
        border = f"2px solid {t['blue_pale']}" if n else f"2px dashed {t['graphite']}"
        if ic:
            pic = icon(ICONS[ic], ink, 28)
        else:
            pic = svg(30, 16, el("circle", cx=7, cy=8, r=6, fill=t["red"]) +
                      el("path", d="M23 1 L30 8 L23 15 L16 8 Z", fill=t["red"]), view="0 0 30 16")
        badge = (f'<span style="position: absolute; right: -6px; top: -7px; min-width: 22px; height: 22px; '
                 f'border-radius: 11px; background: {t["red"] if n else t["graphite"]}; color: {t["paper"]}; '
                 f'font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center">{n}</span>')
        out.append(f'<button type="button" aria-label="{name}, {n} left" style="position: relative; height: 56px; '
                   f'box-sizing: border-box; border: {border}; border-radius: 14px 18px 12px 20px / 18px 12px 20px 14px; '
                   f'background: {t["card"]}; padding: 0; display: flex; align-items: center; '
                   f'justify-content: center">{pic}{badge}</button>')
    return (f'<div role="toolbar" aria-label="Boosters" style="position: absolute; left: 18px; right: 18px; top: 730px; '
            f'display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px">{"".join(out)}</div>')


def margin_doodles(t):
    """World 1 is the notebook itself: a few pencil doodles in the empty paper around the opponent."""
    ink = t["blue_pale"]
    d = ("M12 132 q8 -10 16 0 q8 10 16 0 M26 186 l4 9 9 1 -7 6 2 9 -8 -5 -8 5 2 -9 -7 -6 9 -1z "
         "M326 236 a10 10 0 1 1 -3 -7 M296 196 l26 -8 -8 26 -5 -11z M296 196 l13 7")
    return svg(W, 300, el("path", d=d, fill="none", stroke=ink, stroke_width=1.6, stroke_linecap="round",
                          stroke_linejoin="round"), style="position: absolute; left: 0; top: 0")


def game_frame(t, key, uid):
    m = MOMENTS[key]
    parts = [margin_doodles(t)]
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
    parts.append(f'<div style="position: absolute; left: 0; top: {BOARD_TOP}px">{board_svg(t, key, uid)}</div>')
    # The player, below the board.
    you_turn = key in ("opening", "threat")
    row_top = 682 if key != "win" else -200
    parts.append(f'<div style="position: absolute; left: 16px; right: 16px; top: {row_top}px; display: flex; '
                 f'align-items: center; justify-content: space-between">'
                 f'{side_row(t, "red", "You", m["you"], you_turn, "flex-start")}'
                 f'<div style="display: flex; align-items: baseline; gap: 6px"><span style="{SCORE}">{m["left"]}</span>'
                 f'<span style="{LABEL}; color: {t["graphite"]}">dots left</span></div></div>')
    if key != "win":
        parts.append(toolbar(t))
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


# -------------------------------------------------------------------- map ----

STOPS = [  # (level, x, y, state, stars)
    (5, 120, 700, "done", 2), (6, 205, 668, "done", 1), (7, 285, 610, "next", 0), (8, 215, 548, "locked", 0),
    (9, 125, 512, "locked", 0), (10, 112, 440, "locked", 0), ("chest", 192, 412, "chest", 0),
    (11, 282, 380, "locked", 0), (12, 300, 312, "locked", 0), (13, 222, 272, "locked", 0),
    (14, 135, 248, "locked", 0), (15, 150, 170, "boss", 0),
]


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


def map_art(t, uid):
    """Ballpoint doodles of World 1. Stand-ins for the illustrations in docs/design/illustrations.md."""
    s = dict(fill="none", stroke_width=1.5, stroke_linecap="round", stroke_linejoin="round")
    defs = hatch(f"hr{uid}", t["red_pale"]) + hatch(f"hb{uid}", t["blue_pale"], 45)
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


def map_stops(t):
    out = []
    done_pts = [(130, 820)] + [(x, y) for (_, x, y, st, _) in STOPS[:3]]
    rest = [(x, y) for (_, x, y, st, _) in STOPS[2:]]
    paths = (el("path", d=spline(done_pts), fill="none", stroke=t["blue"], stroke_width=2.2, stroke_dasharray="7 6",
                stroke_linecap="round") +
             el("path", d=spline(rest), fill="none", stroke=t["graphite"], stroke_width=2.2, stroke_dasharray="1 7",
                stroke_linecap="round"))
    out.append(svg(W, H, paths, style="position: absolute; left: 0; top: 0"))
    for level, x, y, st, stars in STOPS:
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
            out.append(f'<button type="button" aria-label="Chest after level 10" style="position: absolute; '
                       f'left: {x - 24}px; top: {y - 26}px; width: 48px; height: 48px; border: none; background: transparent; '
                       f'padding: 0">{svg(48, 48, body, view="0 0 48 48")}</button>')
        elif st == "boss":
            body = (el("path", d="M8 70 V34 h9 v-7 h7 v7 h7 v-7 h7 v7 h7 v-7 h7 v7 h9 V70 z", fill=t["paper"],
                       stroke=t["pencil"], stroke_width=2, stroke_linejoin="round") +
                    el("path", d="M27 70 v-13 a7 7 0 0 1 14 0 v13 M34 27 V8", fill="none", stroke=t["pencil"],
                       stroke_width=2, stroke_linecap="round") +
                    el("path", d="M34 8 l13 5 -13 5 z", fill=t["red"]) +
                    el("path", d="M2 70 h64", stroke=t["pencil"], stroke_width=2, stroke_linecap="round"))
            out.append(f'<button type="button" aria-label="Level 15, boss" style="position: absolute; left: {x - 34}px; '
                       f'top: {y - 40}px; width: 68px; height: 96px; border: none; background: transparent; padding: 0">'
                       f'{svg(68, 76, body, view="0 0 68 76")}'
                       f'<span style="display: block; text-align: center; {LABEL}; color: {t["pencil"]}">Boss 15</span></button>')
    return "".join(out)


def map_frame(t, uid, extra=""):
    parts = [svg(W, H, map_art(t, uid), style="position: absolute; left: 0; top: 0"), map_stops(t)]
    parts.append(f'<div style="position: absolute; left: 70px; top: 26px">'
                 f'<div style="{LABEL}; color: {t["graphite"]}">World 1</div>'
                 f'<div style="{DISPLAY}; color: {t["blue"]}">Notebook</div>'
                 f'<div style="display: flex; align-items: center; gap: 6px; {LABEL}">Apprentice'
                 f'<span style="color: {t["graphite"]}">·</span>{star(t, 16, True)}14 of 45</div></div>')
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


# ------------------------------------------------------------------ output ----

FRAMES = [  # file stem, canvas name, title, drawer
    ("game-start", "GameStart", "Game: the level opens", lambda t, u: game_frame(t, "opening", u)),
    ("game-threat", "GameThreat", "Game: a threat", lambda t, u: game_frame(t, "threat", u)),
    ("game-big-capture", "GameBigCapture", "Game: right after a big capture", lambda t, u: game_frame(t, "big", u)),
    ("result-win", "ResultWin", "Result: won, two stars", lambda t, u: game_frame(t, "win", u)),
    ("map", "Map", "World map (home)", lambda t, u: map_frame(t, u)),
    ("level-card", "LevelCard", "Level card", lambda t, u: level_card_frame(t, u)),
]


def page(body, title):
    return (f'<!doctype html><html lang="en"><head><meta charset="utf-8"><title>{title}</title>'
            f'<link rel="stylesheet" href="{FONTS.replace("&", "&amp;")}">'
            f'<style>body{{margin:0}} button{{cursor:pointer}}</style></head><body>{body}</body></html>\n')


def dc_page(body, title):
    props = '{"$preview":{"width":360,"height":800}}'
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
    with tempfile.TemporaryDirectory() as tmp:
        for stem, name, title, draw in FRAMES:
            for theme, t in THEMES.items():
                uid = f"{name}{theme[0]}"
                body = draw(t, uid)
                full = f"{title}, {theme}"
                if canvas:
                    dc_name = name + ("" if theme == "light" else "Dark") + ".dc.html"
                    with open(os.path.join(canvas, dc_name), "w") as fh:
                        fh.write(dc_page(body, full))
                src = os.path.join(tmp, f"{stem}-{theme}.html")
                with open(src, "w") as fh:
                    fh.write(page(body, full))
                out = os.path.join(HERE, f"{stem}-{theme}.png")
                subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                                f"--force-device-scale-factor={SCALE}", f"--window-size={W},{H}",
                                "--virtual-time-budget=15000", f"--screenshot={out}", "file://" + src],
                               check=True, capture_output=True)
                print(out)


if __name__ == "__main__":
    main()
