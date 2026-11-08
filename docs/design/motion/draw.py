"""Draws the motion mock-ups for docs/design/motion.md: one GIF per scene and a strip of key frames
per moment.

Run from the repository root (needs Python 3, Google Chrome, ffmpeg, and a network connection for
the two Google Fonts):

    python3 docs/design/motion/draw.py            # every GIF and strip
    python3 docs/design/motion/draw.py capture    # only the scenes and strips named

Each scene is drawn at 25 frames a second from the timings on docs/design/motion.md, reusing the
canvas drawing (docs/design/canvas/draw.py), so every colour and size follows the Notebook theme.
Chrome renders the frames in tiled sheets and ffmpeg cuts them into a GIF. A caption above each
GIF names the step and the time since it started.
"""

import concurrent.futures
import importlib.util
import math
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.dont_write_bytecode = True  # importing the canvas script leaves no cache in the repository
_spec = importlib.util.spec_from_file_location("canvas", os.path.join(HERE, "..", "canvas", "draw.py"))
cd = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cd)

el, svg, f, THEMES = cd.el, cd.svg, cd.f, cd.THEMES
W, H = cd.W, cd.H
FPS = 25
STEP = 1000 // FPS  # 40 ms
SP, DOT = 24.0, 12.0
BOARD_TOP = cd.BOARD_TOP
CAP = 30  # caption band above each GIF frame, dp
GIF_SCALE = 1.5  # px per dp in the GIFs
SHEET_SCALE = 2  # px per dp in Chrome
FFMPEG = os.environ.get("FFMPEG", "ffmpeg")


# ------------------------------------------------------------------ timing ----

def bezier(x1, y1, x2, y2):
    def ease(u):
        if u <= 0:
            return 0.0
        if u >= 1:
            return 1.0
        lo, hi = 0.0, 1.0
        for _ in range(40):
            s = (lo + hi) / 2
            x = 3 * (1 - s) ** 2 * s * x1 + 3 * (1 - s) * s * s * x2 + s ** 3
            lo, hi = (s, hi) if x < u else (lo, s)
        s = (lo + hi) / 2
        return 3 * (1 - s) ** 2 * s * y1 + 3 * (1 - s) * s * s * y2 + s ** 3
    return ease


EASE_OUT = bezier(0, 0, 0.2, 1)        # Compose LinearOutSlowInEasing
EASE_IN_OUT = bezier(0.4, 0, 0.2, 1)   # Compose FastOutSlowInEasing
EASE_IN = bezier(0.4, 0, 1, 1)         # Compose FastOutLinearInEasing


def linear(u):
    return min(1.0, max(0.0, u))


def prog(t, a, b, ease=EASE_OUT):
    """0 before a, 1 after b, eased in between."""
    if t <= a:
        return 0.0
    if t >= b:
        return 1.0
    return ease((t - a) / (b - a))


def pop(t, start=0, peak=1.15, up=120, down=80):
    """A piece landing: grows past its size, then settles (0 before start)."""
    if t < start:
        return 0.0
    u = t - start
    if u < up:
        return peak * EASE_OUT(u / up)
    if u < up + down:
        return peak + (1 - peak) * EASE_IN_OUT((u - up) / down)
    return 1.0


def bump(t, start, length, peak=1.3):
    """1, up to peak and back to 1 over length ms."""
    if t < start or t > start + length:
        return 1.0
    return 1 + (peak - 1) * math.sin(math.pi * (t - start) / length)


def wave(t, start, length):
    """0 up to 1 and back to 0 over length ms."""
    if t < start or t > start + length:
        return 0.0
    return math.sin(math.pi * (t - start) / length)


def mix(c1, c2, k):
    k = min(1.0, max(0.0, k))
    a = [int(c1[i:i + 2], 16) for i in (1, 3, 5)]
    b = [int(c2[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(x + (y - x) * k):02X}" for x, y in zip(a, b))


# ------------------------------------------------------------- the position ----
# The game of Level 7 drawn on the canvas, before and after its moments.

R1, B1, BIG = cd.R1, cd.B1, cd.BIG
TR, TB = cd.THREAT_RED, cd.THREAT_BLUE
PAIR, CLUSTER = cd.PAIR, cd.CLUSTER


def c(p):
    return (SP / 2 + p[0] * SP, SP / 2 + p[1] * SP)


def fc(p):
    """A board point in frame coordinates."""
    x, y = c(p)
    return x, y + BOARD_TOP


def neighbours(p, q):
    return p != q and max(abs(p[0] - q[0]), abs(p[1] - q[1])) == 1


def position(red, blue, areas, last=None):
    return dict(red=list(red), blue=list(blue), areas=[dict(a) for a in areas], last=last)


def area_state(a, ring=1.0, fill=1.0, origin=None, cuts=None, glow=0.0):
    return dict(area=a, ring=ring, fill=fill, origin=origin or a["ring"][0], cuts=cuts or {}, glow=glow)


# ------------------------------------------------------------------- board ----

def board(t, st, uid):
    """The board at one moment. st holds the pieces and every moving part; see the scenes below."""
    scale = st.get("scale", {})
    captured = {}
    for a in st["areas"]:
        for p in a["area"]["inside"]:
            captured[p] = a
    defs, washes, rings = [], [], []
    for i, a in enumerate(st["areas"]):
        ar = a["area"]
        pts = [c(p) for p in ar["ring"]]
        segs, outline = cd.bowed_segments(pts, seed=str(ar["ring"][0]))
        captor = ar["captor"]
        g = a["glow"]
        wash = mix(t[captor + "_wash"], t[captor + "_pale"], 0.85 * g)
        pale = mix(t[captor + "_pale"], t[captor], 0.35 * g)
        pid = f"h{uid}{i}"
        angle = -45 if captor == "red" else 45
        defs.append(f'<pattern id="{pid}" width="5" height="5" patternUnits="userSpaceOnUse" '
                    f'patternTransform="rotate({angle})">' + el("rect", width=5, height=5, fill=wash) +
                    el("line", x1=0, y1=2.5, x2=5, y2=2.5, stroke=pale, stroke_width=1) + "</pattern>")
        if a["fill"] > 0:
            ox, oy = c(a["origin"])
            reach = max(math.hypot(x - ox, y - oy) for x, y in pts) + 6
            cid = f"k{uid}{i}"
            defs.append(f'<clipPath id="{cid}">' + el("circle", cx=f(ox), cy=f(oy), r=f(reach * a["fill"])) + "</clipPath>")
            washes.append(el("path", d=outline, fill=f"url(#{pid})", clip_path=f"url(#{cid})"))
        if a["ring"] > 0:
            n = len(segs)
            k0 = ar["ring"].index(a["origin"]) if a["origin"] in ar["ring"] else 0
            drawn = a["ring"] * n
            for j in range(n):
                d, w = segs[(k0 + j) % n]
                part = min(1.0, max(0.0, drawn - j))
                if part <= 0:
                    continue
                dash = None if part >= 1 else f"{f(part)} 2"
                rings.append(el("path", d=d, fill="none", stroke=t[captor], stroke_width=f(w * (1 + 0.9 * g)),
                                stroke_linecap="round", pathLength=1 if dash else None, stroke_dasharray=dash))
    grid = []
    for i in range(15):
        x = SP / 2 + i * SP
        sw = 1.5 if i in (0, 14) else 1
        grid.append(el("line", x1=f(x), y1=f(SP / 2), x2=f(x), y2=f(360 - SP / 2), stroke=t["grid"], stroke_width=sw))
        grid.append(el("line", x1=f(SP / 2), y1=f(x), x2=f(360 - SP / 2), y2=f(x), stroke=t["grid"], stroke_width=sw))
    # Links: between whole live pieces of one side; a new piece's links grow towards it.
    links = []
    grow = st.get("grow", {})
    for side in ("red", "blue"):
        live = [p for p in st[side] if p not in captured and scale.get(p, 1) >= 0.999 and p not in grow]
        for i, p in enumerate(live):
            for q in live[i + 1:]:
                if neighbours(p, q):
                    (x1, y1), (x2, y2) = c(p), c(q)
                    links.append(el("line", x1=f(x1), y1=f(y1), x2=f(x2), y2=f(y2), stroke=t[side + "_pale"],
                                    stroke_width=1.5, stroke_linecap="round"))
        for n, g in grow.items():
            if n not in st[side] or g <= 0:
                continue
            for q in live:
                if neighbours(n, q):
                    (x1, y1), (x2, y2) = c(q), c(n)
                    links.append(el("line", x1=f(x1), y1=f(y1), x2=f(x1 + (x2 - x1) * g), y2=f(y1 + (y2 - y1) * g),
                                    stroke=t[side + "_pale"], stroke_width=1.5, stroke_linecap="round"))
    marks = []
    for p, k, w, col in st.get("threats", []):
        x, y = c(p)
        marks.append(el("circle", cx=f(x), cy=f(y), r=f(0.42 * SP * k), fill="none", stroke=col, stroke_width=f(w)))
    for p, r, w in st.get("ripples", []):
        x, y = c(p)
        marks.append(el("circle", cx=f(x), cy=f(y), r=f(r), fill="none", stroke=t[st.get("ripple_side", "blue")],
                        stroke_width=f(w)))
    pieces = []
    for side in ("red", "blue"):
        for p in st[side]:
            s = scale.get(p, 1.0)
            if s <= 0.01:
                continue
            x, y = c(p)
            if side == "red":
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(DOT / 2 * s), fill=t["red"]))
            else:
                h = DOT * 7 / 12 * s
                pieces.append(el("path", d=f"M{f(x)} {f(y - h)}L{f(x + h)} {f(y)}L{f(x)} {f(y + h)}L{f(x - h)} {f(y)}Z",
                                 fill=t["blue"]))
            if p in captured:
                q = captured[p]["cuts"].get(p, 1.0)
                if q > 0:
                    L = DOT * 0.75 / math.sqrt(2)
                    if side == "red":
                        (ax, ay), (bx, by), col = (x - L, y - L), (x + L, y + L), t["blue_wash"]
                    else:
                        (ax, ay), (bx, by), col = (x - L, y + L), (x + L, y - L), t["red_wash"]
                    pieces.append(el("path", d=f"M{f(ax)} {f(ay)}L{f(ax + (bx - ax) * q)} {f(ay + (by - ay) * q)}",
                                     stroke=col, stroke_width=f(DOT / 6), fill="none"))
            if p == st.get("last") and s >= 0.999:
                pieces.append(el("circle", cx=f(x), cy=f(y), r=f(DOT * 0.15), fill=t["paper"]))
    for p, side, k in st.get("closing", []):
        x, y = c(p)
        pieces.append(el("circle", cx=f(x), cy=f(y), r=f(SP / 6 * k), fill="none", stroke=t[side], stroke_width=1.5))
    extra = []
    for p, side, q in st.get("edges", []):  # where the computer just played, on the top and left edges
        x, y = c(p)
        extra.append(el("line", x1=f(x), y1=1.5, x2=f(x), y2=f(1.5 + 8 * q), stroke=t[side], stroke_width=2.5,
                        stroke_linecap="round"))
        extra.append(el("line", x1=1.5, y1=f(y), x2=f(1.5 + 8 * q), y2=f(y), stroke=t[side], stroke_width=2.5,
                        stroke_linecap="round"))
    for p, q1, q2 in st.get("walls", []):
        extra.append(wall(t, p, q1, q2, uid))
    for p, q in st.get("hints", []):
        extra.append(hint(t, p, q))
    for p, side, q in st.get("sparkles", []):
        x, y = c(p)
        for k in range(4):
            a = math.pi / 4 + k * math.pi / 2
            r0, ln = 9 + 8 * q, 6 * math.sin(math.pi * q)
            if ln > 0.2:
                extra.append(el("line", x1=f(x + math.cos(a) * r0), y1=f(y + math.sin(a) * r0),
                                x2=f(x + math.cos(a) * (r0 + ln)), y2=f(y + math.sin(a) * (r0 + ln)),
                                stroke=t[side], stroke_width=2, stroke_linecap="round"))
    for p, q in st.get("bomb", []):  # the bomb dropping on its point
        x, y = c(p)
        s = 0.6 + 0.4 * q
        extra.append(f'<g transform="translate({f(x - 11 * s)} {f(y - 13 * s)}) scale({f(s * 22 / 24)})">' +
                     el("path", d=cd.ICONS["bomb"], fill=t["card"], stroke=t["pencil"], stroke_width=2,
                        stroke_linecap="round", stroke_linejoin="round") + "</g>")
    for p, side, q in st.get("specks", []):
        x, y = c(p)
        for k in range(12):
            a = k * math.pi / 6 + 0.26
            r0, ln = 14 + 30 * EASE_OUT(q), 9 * math.sin(math.pi * q)
            if ln > 0.2:
                extra.append(el("line", x1=f(x + math.cos(a) * r0), y1=f(y + math.sin(a) * r0),
                                x2=f(x + math.cos(a) * (r0 + ln)), y2=f(y + math.sin(a) * (r0 + ln)),
                                stroke=t[side], stroke_width=2.5, stroke_linecap="round"))
    if st.get("burst"):
        q = st["burst"]
        cx, cy = c((4.5, 5))
        allp = st["red"] + st["blue"]
        for k in range(14):
            a = k * 2 * math.pi / 14 + 0.2
            r1, r2 = (64, 78) if k % 2 else (66, 74)
            ends = [(cx + math.cos(a) * r, cy + math.sin(a) * r * 1.1) for r in (r1, (r1 + r2) / 2, r2)]
            if any(math.hypot(ex - c(p)[0], ey - c(p)[1]) < 12 for ex, ey in ends for p in allp):
                continue
            grow_k = 0.7 + 0.3 * EASE_OUT(q)
            ln = (r2 - r1) * math.sin(math.pi * q)
            ra = r1 * grow_k
            if ln > 0.3:
                extra.append(el("line", x1=f(cx + math.cos(a) * ra), y1=f(cy + math.sin(a) * ra * 1.1),
                                x2=f(cx + math.cos(a) * (ra + ln)), y2=f(cy + math.sin(a) * (ra + ln) * 1.1),
                                stroke=t["red"], stroke_width=2.5, stroke_linecap="round"))
    for p, side in st.get("preview", []):
        x, y = c(p)
        pale = t[side + "_pale"]
        piece = (el("circle", cx=f(x), cy=f(y), r=f(DOT / 2), fill=pale) if side == "red" else
                 el("path", d=f"M{f(x)} {f(y - 7)}L{f(x + 7)} {f(y)}L{f(x)} {f(y + 7)}L{f(x - 7)} {f(y)}Z", fill=pale))
        extra.append(piece + el("circle", cx=f(x), cy=f(y), r=f(0.45 * SP), fill="none", stroke=t[side], stroke_width=2))
    dx = st.get("shake", 0.0)
    body = (f"<defs>{''.join(defs)}</defs>" + el("rect", width=360, height=360, fill=t["paper"]) +
            f'<g transform="translate({f(dx)} 0)">' + "".join(washes) + "".join(grid) + "".join(links) +
            "".join(rings) + "".join(marks) + "".join(pieces) + "".join(extra) + "</g>")
    return svg(360, 360, body, label="The board, 15 by 15 points")


def wall(t, p, q1, q2, uid):
    """A wall: a small square drawn in pencil round the point, then hatched."""
    x, y = c(p)
    h = 0.35 * SP
    out = []
    if q1 > 0:
        d = f"M{f(x - h)} {f(y - h)}H{f(x + h)}V{f(y + h)}H{f(x - h)}Z"
        dash = None if q1 >= 1 else f"{f(q1)} 2"
        out.append(el("path", d=d, fill=t["paper"] if q1 >= 1 else "none", stroke=t["pencil"], stroke_width=1.8,
                      stroke_linejoin="round", stroke_linecap="round", pathLength=1 if dash else None,
                      stroke_dasharray=dash))
    lines = 5
    for k in range(lines):  # rising lines x + y = s across the square
        part = min(1.0, max(0.0, q2 * lines - k))
        if part <= 0:
            continue
        sk = -2 * h + (k + 1) * 4 * h / (lines + 1)
        x1, x2 = max(-h, sk - h), min(h, sk + h)
        ax, ay, bx, by = x + x1, y + sk - x1, x + x2, y + sk - x2
        out.append(el("line", x1=f(ax), y1=f(ay), x2=f(ax + (bx - ax) * part), y2=f(ay + (by - ay) * part),
                      stroke=t["graphite"], stroke_width=1.3, stroke_linecap="round"))
    return "".join(out)


def hint(t, p, q):
    """The hint: the point circled twice in the player's ink, like the next level on the map."""
    x, y = c(p)
    out = []
    for k, (ox, oy, r) in enumerate(((0.5, -0.3, 11.5), (-0.6, 0.5, 13))):
        part = min(1.0, max(0.0, q * 2 - k))
        if part <= 0:
            continue
        dash = None if part >= 1 else f"{f(part)} 2"
        out.append(el("circle", cx=f(x + ox), cy=f(y + oy), r=f(r), fill="none", stroke=t["red"], stroke_width=2,
                      stroke_linecap="round", pathLength=1 if dash else None, stroke_dasharray=dash,
                      transform=f"rotate({-120 + 40 * k} {f(x + ox)} {f(y + oy)})"))
    return "".join(out)


# -------------------------------------------------------------- the screen ----

def side_row(t, side, name, score, turn, align, pop_k=1.0, chip=""):
    ink = t[side]
    shape = (el("circle", cx=7, cy=7, r=6, fill=ink) if side == "red" else
             el("path", d="M7 0 L14 7 L7 14 L0 7 Z", fill=ink))
    ulw = len(name) * 11 + 10
    ul = ""
    if turn > 0:
        ul = (f'<span style="position: absolute; left: -4px; bottom: -7px; display: block; height: 8px; '
              f'width: {f(ulw * turn)}px; overflow: hidden">{cd.underline(ink, ulw)}</span>')
    return (f'<div style="display: flex; align-items: center; gap: 8px; justify-content: {align}">'
            f'{svg(14, 14, shape)}<span style="position: relative; {cd.TITLE}; color: {ink}">{name}{ul}</span>{chip}'
            f'{cd.tally(score, ink)}<span style="display: inline-block; transform: scale({f(pop_k)}); {cd.SCORE}; '
            f'color: {ink}">{score}</span></div>')


def left_block(t, ui):
    n = ui.get("left")
    warn = ui.get("warn", 0.0)
    col = mix(t["pencil"], t["hi"], warn)
    row = ""
    for s in ui.get("row", []):
        if s > 0.01:
            row += svg(10, 10, el("circle", cx=5, cy=5, r=f(4 * s), fill=t["red"]))
    row = f'<span style="display: flex; gap: 3px; margin-right: 4px">{row}</span>' if row else ""
    last = ui.get("last_dot", 0.0)
    if last > 0:
        txt = (f'<span style="display: inline-block; overflow: hidden; white-space: nowrap; width: {f(92 * last)}px; '
               f'{cd.HAND24}; color: {t["hi"]}">Last dot!</span>')
        return f'<div style="display: flex; align-items: center; gap: 6px">{row}{txt}</div>'
    label = "dot left" if n == 1 else "dots left"
    return (f'<div style="display: flex; align-items: center; gap: 6px">{row}'
            f'<span style="display: inline-block; transform: scale({f(ui.get("left_pop", 1.0))}); {cd.SCORE}; '
            f'color: {col}">{n}</span><span style="{cd.LABEL}; color: {t["graphite"]}">{label}</span></div>')


def toolbar(t, counts, pops=(1, 1, 1, 1), dy=0.0, tap=None):
    tools = [("Wall", "wall"), ("Hint", "hint"), ("Bomb", "bomb"), ("Double dot", None)]
    out = []
    for i, ((name, ic), n) in enumerate(zip(tools, counts)):
        ink = t["blue"] if n else t["graphite"]
        border = f"2px solid {t['blue_pale']}" if n else f"2px dashed {t['graphite']}"
        pic = (cd.icon(cd.ICONS[ic], ink, 28) if ic else
               svg(30, 16, el("circle", cx=7, cy=8, r=6, fill=t["red"]) +
                   el("path", d="M23 1 L30 8 L23 15 L16 8 Z", fill=t["red"]), view="0 0 30 16"))
        badge = (f'<span style="position: absolute; right: -6px; top: -7px; min-width: 22px; height: 22px; '
                 f'border-radius: 11px; background: {t["red"] if n else t["graphite"]}; color: {t["paper"]}; '
                 f'font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center; '
                 f'transform: scale({f(pops[i])})">{n}</span>')
        ring = ""
        if tap and tap[0] == i:
            ring = touch(28 + 0, 28, tap[1], t)
        out.append(f'<div style="position: relative; height: 56px; box-sizing: border-box; border: {border}; '
                   f'border-radius: 14px 18px 12px 20px / 18px 12px 20px 14px; background: {t["card"]}; '
                   f'display: flex; align-items: center; justify-content: center">{pic}{badge}{ring}</div>')
    return (f'<div style="position: absolute; left: 18px; right: 18px; top: {f(730 + dy)}px; display: grid; '
            f'grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px">{"".join(out)}</div>')


def touch(x, y, q, t):
    """A tap, as a grey ring spreading from the finger (not part of the game: it shows where the tap is)."""
    if q <= 0 or q >= 1:
        return ""
    r = 8 + 18 * EASE_OUT(q)
    return (f'<span style="position: absolute; left: {f(x - r)}px; top: {f(y - r)}px; width: {f(2 * r)}px; '
            f'height: {f(2 * r)}px; border-radius: 50%; border: 3px solid {t["graphite"]}; box-sizing: border-box; '
            f'opacity: {f(1 - q)}"></span>')


def thumb(x, y):
    """A thumb resting on the board (drawn over the frame; not part of the game)."""
    return (f'<div style="position: absolute; left: {f(x - 30)}px; top: {f(y - 16)}px; width: 60px; height: 170px; '
            f'border-radius: 30px 30px 26px 26px / 34px 34px 20px 20px; background: #D9B49A; opacity: 0.93; '
            f'transform-origin: 30px 16px; transform: rotate(-38deg); box-shadow: 0 3px 10px rgba(0,0,0,0.3)">'
            f'<div style="position: absolute; left: 15px; top: 7px; width: 30px; height: 34px; '
            f'border-radius: 15px 15px 9px 9px; background: #EBD0BF"></div></div>')


def flying(text, x, y, k, ink, t, size=24, caption=""):
    halo = f"text-shadow: 0 0 3px {t['paper']}, 0 0 6px {t['paper']}, 0 0 10px {t['paper']}"
    cap = (f'<span style="{cd.HEADLINE}; font-weight: 700; line-height: 28px; color: {ink}; {halo}; '
           f'white-space: nowrap">{caption}</span>') if caption else ""
    return (f'<div style="position: absolute; left: {f(x)}px; top: {f(y)}px; transform: translate(-50%, -{size // 2}px) '
            f'scale({f(k)}) rotate(-6deg); display: flex; flex-direction: column; align-items: center">'
            f'<span style="font-weight: 800; font-size: {size}px; line-height: {size}px; color: {ink}; {halo}">{text}</span>'
            f'{cap}</div>')


def screen(t, ui, uid):
    """The game screen of the canvas, with every moving part taken from ui."""
    parts = [cd.margin_doodles(t)]
    pdy = ui.get("pause_dy", 0)
    parts.append(f'<div style="position: absolute; left: 4px; top: {f(28 + pdy)}px">'
                 f'{cd.icon_button(t, "pause", "Pause", isize=30)}</div>')
    parts.append(f'<div style="position: absolute; left: 0; right: 0; top: 40px; text-align: center; {cd.LABEL}; '
                 f'color: {t["graphite"]}">Level 7</div>')
    rot, mk = ui.get("mission_rot", 0.0), ui.get("mission_pop", 1.0)
    parts.append(f'<div style="transform-origin: 280px 66px; transform: rotate({f(rot)}deg) scale({f(mk)})">'
                 f'{cd.mission_note(t, ui.get("mission", 0))}</div>')
    parts.append(f'<div style="position: absolute; left: 64px; top: 100px; width: 124px; height: 124px; '
                 f'box-sizing: border-box; padding: 8px; {cd.note_style(t, -3)}">{cd.tape(t, 36, -9, 52, 4)}'
                 f'{cd.face(t, ui.get("mood", "happy"), 108)}</div>')
    if ui.get("bubble"):
        parts.append(cd.bubble(t, ui["bubble"], 198, 132))
    turn = ui.get("turn", ("you", 1.0))
    parts.append(f'<div style="position: absolute; left: 0; right: 0; top: 242px">'
                 f'{side_row(t, "blue", "Computer", ui.get("opp", 0), turn[1] if turn[0] == "opp" else 0, "center", ui.get("opp_pop", 1.0))}</div>')
    parts.append(f'<div style="position: absolute; left: 0; top: {BOARD_TOP}px">{board(t, ui["board"], uid)}</div>')
    chip = ""
    if ui.get("chip"):
        txt, k = ui["chip"]
        if k > 0.01:
            chip = (f'<span style="display: inline-block; transform: scale({f(k)}); background: {t["red"]}; '
                    f'color: {t["paper"]}; border-radius: 10px; padding: 0 7px; {cd.LABEL}">{txt}</span>')
    parts.append(f'<div style="position: absolute; left: 16px; right: 16px; top: 682px; display: flex; '
                 f'align-items: center; justify-content: space-between">'
                 f'{side_row(t, "red", "You", ui.get("you", 0), turn[1] if turn[0] == "you" else 0, "flex-start", ui.get("you_pop", 1.0), chip)}'
                 f'{left_block(t, ui)}</div>')
    parts.append(toolbar(t, ui.get("tools", (2, 1, 0, 1)), ui.get("tool_pops", (1, 1, 1, 1)), ui.get("tool_dy", 0),
                         ui.get("tool_tap")))
    parts.extend(ui.get("over", []))
    return cd.root(t, "".join(parts))


# ------------------------------------------------------------------ scenes ----
# Each scene: frame(theme, T, uid) -> (html, caption); T is ms from the start of the GIF.

def placed(t_ms, at):
    """Scale of a piece that lands at time `at` (ms)."""
    return pop(t_ms, at)


def fading_edges(p, T, at):
    """Edge marks of the computer's earlier move: they go when the player's dot lands at `at`."""
    if T < at:
        return [(p, "blue", 1.0)]
    q = prog(T, at, at + 150, EASE_IN)
    return [(p, "blue", 1 - q)] if q < 1 else []


def base_threat_ui():
    return dict(mood="smug", bubble="Your move…", you=2, opp=1, left=13, mission=2, tools=(2, 1, 0, 1))


def computer_lands(T, at, p, st, side="blue"):
    """The computer's dot lands at time at: the piece, its links, two ripples and the edge marks."""
    u = T - at
    st["scale"][p] = placed(T, at)
    st["grow"][p] = prog(u, 80, 230)
    ripples = []
    for start in (0, 200):
        q = prog(u, start, start + 700, EASE_OUT)
        if 0 < q < 1:
            ripples.append((p, 11 + (48 - 11) * q, 3 - 2.5 * q))
    st["ripples"] = st.get("ripples", []) + ripples
    st["ripple_side"] = side
    if u >= 0:
        st["edges"] = [(p, side, prog(u, 0, 150))]
    if u >= 200:
        st["last"] = p


def scene_turn(theme, T, uid, finger=True):
    t = THEMES[theme]
    you_at, opp_at = 400, 900
    you_p, opp_p = (9, 9), (10, 9)
    red = TR + ([you_p] if T >= you_at else [])
    blue = TB + ([opp_p] if T >= opp_at else [])
    st = position(red, blue, [area_state(R1), area_state(B1)], last=(7, 7))
    st["scale"], st["grow"] = {}, {}
    st["edges"] = fading_edges((7, 7), T, you_at)
    if T < you_at:
        st["preview"] = [(you_p, "red")]
    else:
        st["scale"][you_p] = placed(T, you_at)
        st["grow"][you_p] = prog(T - you_at, 80, 230)
        if T >= you_at + 200:
            st["last"] = you_p
    if T >= opp_at:
        computer_lands(T, opp_at, opp_p, st)
    ui = base_threat_ui()
    ui.update(board=st, left=13 if T < you_at else 12)
    if T < you_at:
        ui["turn"] = ("you", 1.0)
    elif T < opp_at:
        ui["turn"] = ("opp", prog(T - you_at, 0, 200))
    else:
        ui["turn"] = ("you", prog(T - opp_at, 0, 200))
    x, y = fc(you_p)
    ui["over"] = [thumb(x + 3, y + 4)] if finger else []
    if T < you_at:
        cap = ("Your thumb confirms a dot", "")
    elif T < opp_at:
        cap = ("Your dot lands, its link grows", f"+{T - you_at} ms")
    else:
        cap = ("The computer's dot lands", f"+{T - opp_at} ms")
    return screen(t, ui, uid), cap


CAPTURE_AT = 400


def capture_parts(T, at, a, closing, big):
    """The capture timeline from the moment the closing dot lands (at)."""
    u = T - at
    if big:
        ring = prog(u, 150, 650, EASE_IN_OUT)
        fill = prog(u, 650, 950)
        cut0, cut_gap = 700, 30
    else:
        ring = prog(u, 150, 550, EASE_IN_OUT)
        fill = prog(u, 500, 800)
        cut0, cut_gap = 600, 40
    inside = sorted(a["inside"], key=lambda p: math.hypot(p[0] - closing[0], p[1] - closing[1]))
    cuts = {p: prog(u, cut0 + k * cut_gap, cut0 + k * cut_gap + 100, linear) for k, p in enumerate(inside)}
    return area_state(a, ring=ring, fill=fill, origin=closing, cuts=cuts)


def score_flight(t, T, at, n, start, big, you_before, mission_before):
    """+N pops at the area, flies to the player's tally; the tally, number and mission note take it."""
    u = T - at
    p0, p1, f0, f1 = (850, 1000, 1000, 1250) if big else (700, 850, 850, 1100)
    over, you, mission, you_pop, mpop = [], you_before, mission_before, 1.0, 1.0
    end = (110, 696)
    if p0 <= u < f1:
        if u < f1 and u < f0:
            k = prog(u, p0, p1, EASE_OUT) * (1.0 if not big else 1.0)
            x, y = start
        else:
            q = prog(u, f0, f1, EASE_IN)
            cx, cy = (start[0] + end[0]) / 2 + 90, min(start[1], end[1]) + 40
            x = (1 - q) ** 2 * start[0] + 2 * (1 - q) * q * cx + q * q * end[0]
            y = (1 - q) ** 2 * start[1] + 2 * (1 - q) * q * cy + q * q * end[1]
            k = 1 - 0.45 * q
        over.append(flying(f"+{n}", x, y, k, t["red"], t, 40 if big else 24))
    if big and p0 <= u < f1 + 150:
        k = prog(u, p0, p1) * (1 - prog(u, f1, f1 + 150, EASE_IN))
        if k > 0.01:
            over.append(f'<div style="position: absolute; left: {start[0]}px; top: {start[1] + 26}px; '
                        f'transform: translate(-50%, 0) scale({f(k)}) rotate(-6deg); {cd.HEADLINE}; font-weight: 700; '
                        f'line-height: 28px; color: {t["red"]}; white-space: nowrap; text-shadow: 0 0 3px {t["paper"]}, '
                        f'0 0 6px {t["paper"]}, 0 0 10px {t["paper"]}">Six at once!</div>')
    if u >= f1:
        you, mission = you_before + n, mission_before + n
        you_pop = bump(u, f1, 150, 1.3)
        mpop = bump(u, f1, 150, 1.06)
    return over, you, mission, you_pop, mpop


def scene_capture(theme, T, uid):
    t = THEMES[theme]
    at, closing = CAPTURE_AT, (3, 11)
    red = [p for p in TR if p != closing] + ([closing] if T >= at else [])
    areas = [area_state(B1)]
    if T >= at:
        areas.append(capture_parts(T, at, R1, closing, big=False))
    st = position(red, TB, areas, last=(12, 12))
    st["scale"], st["grow"] = {}, {}
    st["edges"] = fading_edges((12, 12), T, at)
    if T >= at:
        st["scale"][closing] = placed(T, at)
        st["grow"][closing] = prog(T - at, 80, 230)
        if T >= at + 200:
            st["last"] = closing
    u = T - at
    over, you, mission, you_pop, mpop = score_flight(t, T, at, 2, (fc((4, 10.5))[0] + 8, fc((4, 10.5))[1]), False, 0, 0)
    ui = dict(mood="upset" if u >= 800 else "happy", bubble="Ouch!" if u >= 800 else "Your move…", you=you, opp=1,
              left=17 if T < at else 16, mission=mission, mission_pop=mpop, you_pop=you_pop, board=st, over=over,
              turn=("you", 1.0) if T < at else ("opp", prog(u, 0, 200)))
    cap = ("Before: your dot will close the ring", "") if T < at else ("A capture of 2", f"+{u} ms")
    return screen(t, ui, uid), cap


def big_state(T, at, skip_at=None):
    """The big capture; a tap at skip_at jumps to the end."""
    t_eff = T if skip_at is None or T < skip_at else at + 5000
    closing = (5, 7)
    red = TR + ([closing] if T >= at else [])
    areas = [area_state(R1), area_state(B1)]
    st = position(red, TB, areas, last=(7, 7))
    st["scale"], st["grow"] = {}, {}
    st["edges"] = fading_edges((7, 7), t_eff, at)
    u = t_eff - at
    if T < at:
        st["threats"] = [(p, 1.0, 2, None) for p in PAIR + CLUSTER]
        st["closing"] = [((9, 9), "blue", 1.0), ((5, 7), "red", 1.0)]
    else:
        areas.append(capture_parts(t_eff, at, BIG, closing, big=True))
        st["scale"][closing] = placed(t_eff, at)
        st["grow"][closing] = prog(u, 80, 230)
        if u >= 200:
            st["last"] = closing
        # The pair is still one move from capture after the big capture.
        st["threats"] = [(p, 1.0, 2, None) for p in PAIR]
        st["closing"] = [((9, 9), "blue", 1.0)]
        if 650 <= u <= 830:
            v = (u - 650) / 180
            st["shake"] = 2 * math.sin(2 * math.pi * 3 * v) * (1 - v)
        if 650 <= u <= 900:
            st["burst"] = (u - 650) / 250
    return st, u


def scene_big(theme, T, uid):
    t = THEMES[theme]
    take2 = 2600  # the second take starts here: the same capture, skipped by a tap
    if T < take2:
        at, skip_at, tt = CAPTURE_AT, None, T
    else:
        tt = T - take2
        at, skip_at = CAPTURE_AT, CAPTURE_AT + 500
    st, u = big_state(tt, at, skip_at)
    for i, th in enumerate(st.get("threats", [])):
        st["threats"][i] = (th[0], th[1], th[2], t["hi"])
    start = (156, BOARD_TOP + 262)  # below the area, where the canvas frame shows it
    over, you, mission, you_pop, mpop = score_flight(t, at + u, at, 6, start, True, 2, 2)
    ui = dict(mood="upset" if u >= 900 else "smug", bubble="Ouch!" if u >= 900 else "Your move…", you=you, opp=1,
              left=13 if tt < at else 12, mission=mission, mission_pop=mpop, you_pop=you_pop, board=st, over=over,
              turn=("you", 1.0) if tt < at else ("opp", prog(u, 0, 200)))
    if skip_at is not None and skip_at <= tt < skip_at + 300:
        x, y = fc((10, 12))
        ui["over"] = ui["over"] + [touch(x, y, (tt - skip_at) / 300, t)]
    if tt < at:
        cap = ("Before: your dot will close the ring", "")
    elif skip_at is None:
        cap = ("A big capture: 6 at once", f"+{u} ms")
    elif tt < skip_at:
        cap = ("Again, and a tap at +500 ms", f"+{tt - at} ms")
    else:
        cap = ("The tap finishes it at once", f"+{tt - at} ms")
    return screen(t, ui, uid), cap


def threat_rings(t, T, at, groups, closings):
    """Threat rings appear at `at` and pulse three times, then stay still."""
    u = T - at
    rings, closing = [], []
    if u < 0:
        return rings, closing
    q = prog(u, 0, 250)
    k, w, kc = 1.3 - 0.3 * q, 2.0, q
    col = mix(t["paper"], t["hi"], q)
    if 250 <= u < 250 + 3 * 1600:
        v = ((u - 250) % 1600) / 1600
        s = (1 - math.cos(2 * math.pi * v)) / 2
        s = EASE_IN_OUT(s)
        k, w, kc = 1 + 0.08 * s, 2 + 1.5 * s, 1 + 0.35 * s
    for p in groups:
        rings.append((p, k, w, col))
    for p, side in closings:
        closing.append((p, side, kc))
    return rings, closing


def scene_threat(theme, T, uid):
    t = THEMES[theme]
    at = 400  # the computer's dot that makes the threat
    p = (7, 7)
    blue = [b for b in TB if b != p] + ([p] if T >= at else [])
    st = position(TR, blue, [area_state(R1), area_state(B1)], last=(12, 12) if T < at else None)
    st["scale"], st["grow"] = {}, {}
    if T >= at:
        computer_lands(T, at, p, st)
    rings, closing = threat_rings(t, T, at + 200, PAIR + CLUSTER, [((9, 9), "blue"), ((5, 7), "red")])
    st["threats"], st["closing"] = rings, closing
    ui = base_threat_ui()
    ui.update(board=st, mood="smug" if T >= at + 200 else "happy", bubble="Your move…" if T >= at + 200 else "Hmm…",
              turn=("you", prog(T - at, 0, 200)) if T >= at else ("opp", 1.0))
    u = T - at - 200
    if T < at:
        cap = ("Before: the computer's move", "")
    elif u < 0:
        cap = ("The computer's dot lands", f"+{T - at} ms")
    elif u < 250:
        cap = ("The threat rings appear", f"+{u} ms")
    elif u < 250 + 4800:
        cap = (f"Pulse {1 + (u - 250) // 1600} of 3", f"+{u} ms")
    else:
        cap = ("Then they stay still", f"+{u} ms")
    return screen(t, ui, uid), cap


RUN_RED = [(12, 10), (1, 1), (13, 5), (7, 12), (1, 13)]
RUN_BLUE = [(13, 10), (2, 1), (13, 4), (8, 12), (2, 13)]


def scene_running_out(theme, T, uid):
    t = THEMES[theme]
    t0, gap = 400, 1000
    red, blue = list(TR), list(TB)
    st = position(red, blue, [area_state(R1), area_state(B1)], last=(7, 7))
    st["scale"], st["grow"] = {}, {}
    left, row, warn, left_pop, last_dot, rot = 6, [], 0.0, 1.0, 0.0, 0.0
    turn = ("you", 1.0)
    prev = (7, 7)
    for k, (rp, bp) in enumerate(zip(RUN_RED, RUN_BLUE)):
        ra, ba = t0 + k * gap, t0 + k * gap + 500
        if T < ba:
            st["edges"] = fading_edges(prev, T, ra)
        prev = bp
        if T >= ra:
            st["red"].append(rp)
            st["scale"][rp] = placed(T, ra)
            st["grow"][rp] = prog(T - ra, 80, 230)
            if T >= ra + 200:
                st["last"] = rp
            left = 5 - k
            turn = ("opp", prog(T - ra, 0, 200))
        if T >= ba:
            st["blue"].append(bp)
            st["ripples"] = []
            computer_lands(T, ba, bp, st)
            turn = ("you", prog(T - ba, 0, 200))
    # The counter: Highlighter from 5 left, a row of the dots left, a pop on each dot spent.
    if T >= t0:
        warn = prog(T - t0, 0, 200, linear)
        u0 = T - t0
        row = [prog(u0, 50 * i, 50 * i + 150) for i in range(5)]
        for k in range(1, 5):
            ra = t0 + k * gap
            if T >= ra:
                row[5 - k] = 1 - prog(T - ra, 0, 150, EASE_IN)
        spent = [t0 + k * gap for k in range(5) if T >= t0 + k * gap]
        left_pop = bump(T, spent[-1], 300, 1.3) if spent else 1.0
        three = t0 + 2 * gap
        if three <= T <= three + 400:
            rot = 3 * math.sin(2 * math.pi * (T - three) / 400)
        if T >= t0 + 4 * gap:
            last_dot = prog(T - t0 - 4 * gap, 0, 300, linear)
    ui = base_threat_ui()
    ui.update(board=st, left=left, row=row, warn=warn, left_pop=left_pop, last_dot=last_dot, mission_rot=rot,
              turn=turn)
    if T < t0:
        cap = ("6 dots left", "")
    else:
        k = min(4, (T - t0) // gap)
        u = T - t0 - k * gap
        names = ["5 left: the count turns", "4 left", "3 left: the mission note nudges", "2 left", "1 left: Last dot!"]
        cap = (names[k], f"+{u} ms")
    return screen(t, ui, uid), cap


BOOSTER_PARTS = [  # name, length of the effect (ms)
    ("Wall", 400), ("Hint", 500), ("Bomb", 450), ("Double dot", 1350),
]
LEAD, HOLD = 500, 900


def scene_boosters(theme, T, uid):
    t = THEMES[theme]
    start = 0
    for i, (name, length) in enumerate(BOOSTER_PARTS):
        total = LEAD + length + HOLD
        if T < start + total or i == len(BOOSTER_PARTS) - 1:
            return booster_frame(t, i, name, T - start - LEAD, uid)
        start += total


def booster_frame(t, i, name, u, uid):
    counts = [2, 1, 1, 1]
    st = position(TR, TB, [area_state(R1), area_state(B1)], last=(7, 7))
    st["scale"], st["grow"] = {}, {}
    ui = base_threat_ui()
    pops = [1, 1, 1, 1]
    if u >= 0:
        counts[i] -= 1
        pops[i] = bump(u, 0, 200, 1.35)
    tap = (i, (u + 400) / 300) if -400 <= u < -100 else None
    p = (9, 9)
    turn = ("you", 1.0)
    over = []
    st["edges"] = fading_edges((7, 7), u, 600) if name == "Double dot" else [((7, 7), "blue", 1.0)]
    if name == "Wall" and u >= 0:
        st["walls"] = [(p, prog(u, 0, 200, EASE_IN_OUT), prog(u, 200, 400, linear))]
    if name == "Hint" and u >= 0:
        st["hints"] = [(p, prog(u, 0, 500, linear))]
    if name == "Bomb":
        centre = (8, 7)
        hit = [q for q in TB if abs(q[0] - centre[0]) + abs(q[1] - centre[1]) <= 1]
        if u >= 0:
            if u < 150:
                st["bomb"] = [(centre, prog(u, 0, 150))]
            for q in hit:
                if u < 150:
                    s = 1.0
                elif u < 220:
                    s = 1 + 0.3 * prog(u, 150, 220)
                else:
                    s = 1.3 * (1 - prog(u, 220, 350, EASE_IN))
                st["scale"][q] = s
            if u >= 350:
                st["blue"] = [q for q in st["blue"] if q not in hit]
                if st["last"] in hit:
                    st["last"] = None
            if 150 <= u <= 450:
                st["specks"] = [(centre, "blue", (u - 150) / 300)]
            if 150 <= u <= 300:
                v = (u - 150) / 150
                st["shake"] = 2 * math.sin(2 * math.pi * 2 * v) * (1 - v)
    if name == "Double dot" and u >= 0:
        first, second = (9, 9), (10, 9)
        for q, at in ((first, 600), (second, 1200)):
            if u >= at:
                st["red"].append(q)
                st["scale"][q] = pop(u, at)
                st["grow"][q] = prog(u - at, 80, 230)
                if u >= at + 200:
                    st["last"] = q
                if u <= at + 250:
                    st.setdefault("sparkles", []).append((q, "red", (u - at) / 250))
        if u < 600:
            ui["chip"] = ("×2", prog(u, 0, 300))
        elif u < 1200:
            ui["chip"] = ("×1", bump(u, 600, 200, 1.25))
        else:
            ui["chip"] = ("×1", 1 - prog(u, 1200, 1350, EASE_IN))
            turn = ("opp", prog(u - 1200, 0, 200))
        ui["left"] = 13 - (u >= 600) - (u >= 1200)
    ui.update(board=st, tools=tuple(counts), tool_pops=tuple(pops), tool_tap=tap, turn=turn, over=over)
    if u < 0:
        cap = (f"{name}: chosen, then Confirm", "")
    else:
        cap = (f"{name}", f"+{u} ms")
    return screen(t, ui, uid), cap


END_AT = 400  # the end sequence starts when the capture that finishes the mission has played out


def result_note(t, slide, rot, head, stars):
    """The result note of the canvas; slide 0..1 moves it up from below the screen."""
    st = []
    for i, (kind, k) in enumerate(stars):
        size = 56 if i == 1 else 48
        if kind == "earned":
            if k <= 0:
                body = ""
            else:
                s = 1.8 - 0.8 * k
                body = (f'<g transform="translate(12 12) rotate({f(-15 * (1 - k))}) scale({f(s)}) translate(-12 -12)">' +
                        el("path", d=cd.STAR, fill=t["red"], stroke=t["red"], stroke_width=1.6, stroke_linejoin="round") +
                        "</g>")
        else:
            dash = None if k >= 1 else f"{f(k)} 2"
            body = el("path", d=cd.STAR, fill="none", stroke=t["graphite"], stroke_width=1.6, stroke_linejoin="round",
                      pathLength=1 if dash else None, stroke_dasharray=dash) if k > 0 else ""
        st.append(f'<span style="transform: translateY({4 if i != 1 else -4}px)">{svg(size, size, body, view="0 0 24 24")}</span>')
    retry = (f'<div style="width: 56px; height: 56px; flex-shrink: 0; box-sizing: border-box; '
             f'border: 2px solid {t["graphite"]}; {cd.BUTTON_SHAPE}; background: {t["card"]}; display: flex; '
             f'align-items: center; justify-content: center">{cd.icon(cd.ICONS["retry"], t["pencil"], 26)}</div>')
    dy = (1 - slide) * 440
    return (f'<div style="position: absolute; left: 20px; right: 20px; bottom: 22px; box-sizing: border-box; '
            f'padding: 20px 18px 18px; display: flex; flex-direction: column; align-items: center; gap: 6px; '
            f'text-align: center; background: {t["card"]}; box-shadow: 0 2px 10px {t["shadow"]}; color: {t["pencil"]}; '
            f'transform: translateY({f(dy)}px) rotate({f(rot)}deg)">{cd.tape(t, 126, -9, 64, -3)}'
            f'<div style="{cd.DISPLAY}; color: {t["red"]}; clip-path: inset(0 {f(100 - 100 * head)}% 0 0)">You won!</div>'
            f'<div style="display: flex; align-items: center; gap: 10px">{"".join(st)}</div>'
            f'<div style="{cd.BODY}; margin-top: 4px">8 captured · 12 dots to spare</div>'
            f'<div style="{cd.SMALL}; color: {t["graphite"]}">Three stars need 14 dots to spare</div>'
            f'<div style="align-self: stretch; display: flex; gap: 12px; margin-top: 10px">{retry}'
            f'{cd.main_button(t, "Next", "flex: 1", cd.ICONS["next"])}</div></div>')


AREA_ORDER = [(B1, "opp"), (R1, "you"), (BIG, "you")]


def scene_end(theme, T, uid):
    """The big capture has just finished the mission, so the game ends."""
    t = THEMES[theme]
    st = position(TR + [(5, 7)], TB, [], last=(5, 7))
    st["scale"], st["grow"] = {}, {}
    u = T - END_AT
    you_pop = opp_pop = 1.0
    for k, (a, side) in enumerate(AREA_ORDER):
        a0 = 300 + 150 * k
        st["areas"].append(area_state(a, glow=wave(u, a0, 300)))
        if side == "you":
            you_pop = max(you_pop, bump(u, a0, 300, 1.3))
        else:
            opp_pop = max(opp_pop, bump(u, a0, 300, 1.3))
    st["threats"] = [(p, 1.0, 2, t["hi"]) for p in PAIR] if u < 0 else []
    st["closing"] = [((9, 9), "blue", 1.0)] if u < 0 else []
    off = prog(u, 0, 250, EASE_IN)
    S = 1400  # the note has settled
    slide = prog(u, 1100, 1400, EASE_OUT)
    # The note tips a little past its slant and settles: 0 to -2.5 degrees, then back to -1.5.
    rot = -2.5 * prog(u, 1100, 1300) if u < 1300 else -2.5 + prog(u, 1300, 1400, EASE_IN_OUT)
    over = []
    if u >= 1100:
        head = prog(u, S, S + 300, linear)
        stars = [("earned", prog(u, S + 300, S + 500, EASE_IN)), ("earned", prog(u, S + 600, S + 800, EASE_IN)),
                 ("empty", prog(u, S + 900, S + 1100, linear))]
        over.append(result_note(t, slide, rot, head, stars))
    ui = dict(mood="sulky" if u >= 1100 else "upset", bubble="Well played." if u >= 1100 else "Ouch!", you=8, opp=1,
              left=12, mission=8, board=st, you_pop=you_pop, opp_pop=opp_pop, tool_dy=110 * off, pause_dy=-80 * off,
              turn=("opp", 1.0) if u < 0 else ("opp", 0), over=over)
    if u < 0:
        cap = ("The big capture finishes the mission", "")
    elif u < 1100:
        cap = ("End: the areas light up in turn", f"+{u} ms")
    elif u < S:
        cap = ("The result note comes up", f"+{u} ms")
    else:
        cap = ("Stars, one by one", f"+{u - S} ms after the note")
    return screen(t, ui, uid), cap


# The world map after Next: the stars fly to level 7, the path inks in, level 8 opens.

STOPS0 = list(cd.STOPS)


def with_stops(changes):
    stops = []
    for s in STOPS0:
        lvl = s[0]
        if lvl in changes:
            st, stars = changes[lvl]
            stops.append((lvl, s[1], s[2], st, stars))
        else:
            stops.append(s)
    return stops


def stop_xy(level):
    for s in STOPS0:
        if s[0] == level:
            return s[1], s[2]


def ink_segment():
    """The map path from level 7 to level 8 as it is drawn once inked (cd.spline over the inked stops)."""
    pts = [(130, 820)] + [(x, y) for (_, x, y, _, _) in STOPS0[:4]]
    p0, p1, p2, p3 = pts[2], pts[3], pts[4], pts[4]
    c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
    c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
    return f"M{p1[0]} {p1[1]} C{f(c1[0])} {f(c1[1])} {f(c2[0])} {f(c2[1])} {p2[0]} {p2[1]}"


def level_card(t, uid):
    """The canvas level card, renamed to level 8, without the map under it."""
    real = cd.map_frame
    cd.map_frame = lambda t_, uid_, extra="", done=3: extra
    try:
        extra = cd.level_card_frame(t, uid)
    finally:
        cd.map_frame = real
    card = extra[extra.index("</div>") + len("</div>"):]
    return card.replace("Level 7", "Level 8").replace("Siege", "Crossroads").replace(", Siege", ", Crossroads")


def scene_unlock(theme, T, uid):
    t = THEMES[theme]
    x7, y7 = stop_xy(7)
    x8, y8 = stop_xy(8)
    over = []
    if T < 900:
        stops, done = with_stops({}), 3
    elif T < 1200:
        stops, done = with_stops({7: ("none", 0)}), 3
    elif T < 1600:
        stops, done = with_stops({7: ("done", 2)}), 3
    elif T < 2000:
        stops, done = with_stops({7: ("done", 2), 8: ("none", 0)}), 4
    else:
        stops, done = with_stops({7: ("done", 2), 8: ("next", 0)}), 4
    if 900 <= T < 1200:  # the empty star slots, under the stars that just landed
        over.append(f'<div style="position: absolute; left: {f(x7 - 21)}px; top: {f(y7 - 38)}px; display: flex; gap: 1px">'
                    + "".join(cd.star(t, 13, False) for _ in range(3)) + "</div>")
    # stars flying from where the result note showed them
    for k in range(2):
        a = 400 + 100 * k
        if a <= T < 1200:
            q = prog(T, a, a + 400, EASE_IN_OUT)
            sx, sy = 160 + 56 * k, 600
            ex, ey = x7 - 21 + 14 * k + 6.5, y7 - 22 - 16 + 6.5
            cx, cy = (sx + ex) / 2 - 40, min(sy, ey) - 90
            x = (1 - q) ** 2 * sx + 2 * (1 - q) * q * cx + q * q * ex
            y = (1 - q) ** 2 * sy + 2 * (1 - q) * q * cy + q * q * ey
            size = 48 + (13 - 48) * q
            over.append(f'<div style="position: absolute; left: {f(x - size / 2)}px; top: {f(y - size / 2)}px">'
                        f'{cd.star(t, size, True)}</div>')
    if 900 <= T < 1200:
        q = prog(T, 900, 1200)
        d = 44 * q
        over.append(f'<div style="position: absolute; left: {f(x7 - 22)}px; top: {f(y7 - 22)}px; width: 44px; '
                    f'height: 44px; display: flex; align-items: center; justify-content: center">'
                    f'<div style="width: {f(d)}px; height: {f(d)}px; border-radius: 50%; background: {t["blue"]}; '
                    f'color: {t["paper"]}; display: flex; align-items: center; justify-content: center; '
                    f'font-size: {f(16 * q)}px; font-weight: 800">7</div></div>')
        k = 1 - prog(T, 900, 1050, EASE_IN)
        if k > 0.01:
            over.append(f'<div style="position: absolute; left: {f(x7 - 28)}px; top: {f(y7 - 28)}px; width: 56px; '
                        f'height: 56px; box-sizing: border-box; border: 3px solid {t["red"]}; border-radius: 50%; '
                        f'box-shadow: 0 0 0 4px {t["paper"]}, 0 0 0 6.5px {t["red"]}; transform: scale({f(k)})"></div>')
    if 1200 <= T < 1600:
        q = prog(T, 1200, 1600, EASE_IN_OUT)
        seg = ink_segment()
        body = (f'<defs><mask id="m{uid}" maskUnits="userSpaceOnUse" x="0" y="0" width="{W}" height="{H}">' +
                el("path", d=seg, fill="none", stroke="#fff", stroke_width=8, pathLength=1,
                   stroke_dasharray=f"{f(q)} 2") + "</mask></defs>" +
                el("path", d=seg, fill="none", stroke=t["paper"], stroke_width=4, mask=f"url(#m{uid})") +
                el("path", d=seg, fill="none", stroke=t["blue"], stroke_width=2.2, stroke_dasharray="7 6",
                   stroke_linecap="round", mask=f"url(#m{uid})"))
        over.append(svg(W, H, body, style="position: absolute; left: 0; top: 0"))
    if 1600 <= T < 2000:
        loops = ""
        for k, (r, w) in enumerate(((26.5, 3), (31.75, 2.5))):
            q = prog(T, 1600 + 200 * k, 1800 + 200 * k, linear)
            if q > 0:
                dash = None if q >= 1 else f"{f(q)} 2"
                loops += el("circle", cx=x8, cy=y8, r=r, fill="none", stroke=t["red"], stroke_width=w,
                            pathLength=1 if dash else None, stroke_dasharray=dash, transform=f"rotate(-100 {x8} {y8})")
        disc = (f'<div style="position: absolute; left: {x8 - 25}px; top: {y8 - 25}px; width: 50px; height: 50px; '
                f'border-radius: 50%; background: {t["card"]}; color: {t["red"]}; display: flex; align-items: center; '
                f'justify-content: center; font-size: 20px; font-weight: 800">8</div>')
        over.append(disc + svg(W, H, loops, style="position: absolute; left: 0; top: 0"))
        lab = prog(T, 1700, 2000, linear)
        if lab > 0:
            arrow = svg(34, 20, el("path", d="M32 12 q-14 6 -28 -2 M4 10 l7 -6 M4 10 l8 3", fill="none",
                                   stroke=t["red"], stroke_width=2, stroke_linecap="round", stroke_linejoin="round"),
                        style="transform: scaleX(-1)")
            over.append(f'<span style="position: absolute; left: {x8 - 104}px; top: {y8 - 20}px; display: flex; '
                        f'align-items: center; gap: 2px; {cd.HAND24}; font-size: 28px; color: {t["red"]}; '
                        f'clip-path: inset(0 {f(100 - 100 * lab)}% 0 0)">next!{arrow}</span>')
    if T >= 2200:
        q = prog(T, 2200, 2500, EASE_OUT)
        over.append(f'<div style="position: absolute; inset: 0; background: {t["scrim"]}; opacity: {f(q)}"></div>')
        over.append(f'<div style="position: absolute; inset: 0; transform: translateY({f((1 - q) * 640)}px)">'
                    f'{level_card(t, uid)}</div>')
    real = cd.STOPS
    cd.STOPS = stops
    try:
        html = cd.map_frame(t, uid, "".join(over), done=done)
    finally:
        cd.STOPS = real
    if T < 400:
        cap = ("Next: back on the map", "")
    elif T < 900:
        cap = ("The new stars fly to level 7", f"+{T - 400} ms")
    elif T < 1200:
        cap = ("Level 7 fills in", f"+{T - 400} ms")
    elif T < 1600:
        cap = ("The path inks in to level 8", f"+{T - 400} ms")
    elif T < 2200:
        cap = ("Level 8 is circled: next!", f"+{T - 400} ms")
    else:
        cap = ("Level 8's card comes up", f"+{T - 400} ms")
    return html, cap


# ------------------------------------------------------------------ output ----

_taken = set(TR) | set(TB) | {(5, 7)}
for _p in RUN_RED + RUN_BLUE + [(9, 9), (10, 9), (12, 10)]:
    assert _p not in _taken, f"{_p} is already taken"
    _taken.add(_p) if _p not in ((9, 9), (10, 9), (12, 10)) else None

FULL = (0, 0, W, H)
SCENES = {  # name: (drawer, length in ms, crop x, y, w, h)
    "turn": (scene_turn, 2800, (0, 296, W, 420)),
    "capture": (scene_capture, 2600, FULL),
    "big-capture": (scene_big, 4800, FULL),
    "threat": (scene_threat, 6600, (0, 296, W, 380)),
    "running-out": (scene_running_out, 6000, FULL),
    "boosters": (scene_boosters, sum(LEAD + n + HOLD for _, n in BOOSTER_PARTS), (0, 296, W, 494)),
    "end": (scene_end, 4300, FULL),
    "unlock": (scene_unlock, 3600, FULL),
}


def sq(p, size):
    """A square crop of the frame around a board point."""
    x, y = fc(p)
    return (x - size / 2, y - size / 2, size, size)


STRIPS = {  # name: (scene, crop, scale, [(T, label)])
    "placing": (lambda th, T, u: scene_turn(th, T, u, finger=False), sq((9, 8.5), 120), 3, [(360, "preview"), (400, "0 ms"), (440, "40"), (480, "80"),
                                                (520, "120"), (560, "160"), (600, "200")]),
    "computer-move": ("turn", sq((10, 9.5), 168), 2, [(880, "before"), (900, "0 ms"), (1000, "100"), (1100, "200"),
                                                      (1300, "400"), (1500, "600"), (1800, "900")]),
    "capture": ("capture", FULL, 1, [(CAPTURE_AT + u, f"{u} ms" if u == 0 else str(u)) for u in
                                        (0, 300, 600, 750, 950, 1250)]),
    "big-capture": ("big-capture", FULL, 1, [(CAPTURE_AT + u, f"{u} ms" if u == 0 else str(u)) for u in
                                                (0, 400, 700, 900, 1100, 1400)]),
    "threat": ("threat", (60, BOARD_TOP + 48, 216, 228), 1.5, [(600, "0 ms"), (725, "125"), (850, "250"),
                                                                (1650, "1050"), (2450, "1850"), (5450, "5050")]),
    "running-out": ("running-out", (0, 664, W, 56), 2, [(380, "6 left"), (700, "5 left"), (1700, "4 left"),
                                                        (2560, "3 left"), (3700, "2 left"), (4900, "1 left")]),
    "end": ("end", FULL, 1, [(END_AT + u, f"{u} ms" if u == 0 else str(u)) for u in (0, 250, 450, 750, 1250, 1400)]),
    "stars": ("end", (0, 470, W, 330), 1, [(END_AT + 1400 + u, f"{u} ms" if u == 0 else str(u)) for u in
                                           (0, 150, 300, 400, 500, 800, 1100)]),
    "unlock": ("unlock", FULL, 1, [(T, f"{T - 400} ms") for T in (400, 700, 1050, 1400, 1800, 2500)]),
}


def booster_strip():
    rows = []
    start = 0
    times = {"Wall": (0, 100, 200, 300, 400), "Hint": (0, 125, 250, 375, 500), "Bomb": (0, 150, 220, 300, 450),
             "Double dot": (0, 300, 600, 700, 1200, 1350)}
    for name, length in BOOSTER_PARTS:
        rows.append([(start + LEAD + u, f"{name} {u} ms" if u == 0 else str(u)) for u in times[name]])
        start += LEAD + length + HOLD
    return rows


def cell(html, crop, caption=None, label=None):
    x, y, w, h = crop
    cap = ""
    if caption is not None:
        cap = (f'<div style="height: {CAP}px; box-sizing: border-box; padding: 0 10px; display: flex; '
               f'align-items: center; justify-content: space-between; gap: 8px; background: #2E2E2E; color: #FBF8F1; '
               f'font-family: Nunito, sans-serif; font-weight: 700; font-size: 13px; white-space: nowrap; overflow: hidden">'
               f'<span style="overflow: hidden; text-overflow: ellipsis">{caption[0]}</span>'
               f'<span style="font-variant-numeric: tabular-nums">{caption[1]}</span></div>')
    lab = ""
    if label is not None:
        lab = (f'<div style="height: 22px; display: flex; align-items: center; justify-content: center; '
               f'font-family: Nunito, sans-serif; font-weight: 800; font-size: 13px; color: #2E2E2E; '
               f'background: #FFFFFF">{label}</div>')
    return (f'<div style="width: {f(w)}px; overflow: hidden; flex-shrink: 0">{cap}'
            f'<div style="position: relative; width: {f(w)}px; height: {f(h)}px; overflow: hidden">'
            f'<div style="position: absolute; left: {f(-x)}px; top: {f(-y)}px; '
            f'">{html}</div></div>{lab}</div>')


def shoot(cells, cols, cell_w, cell_h, out, scale, gap=0, bg="#FFFFFF"):
    rows = math.ceil(len(cells) / cols)
    width, height = cols * cell_w + (cols - 1) * gap, rows * cell_h + (rows - 1) * gap
    body = (f'<div style="display: grid; grid-template-columns: repeat({cols}, {f(cell_w)}px); gap: {gap}px; '
            f'background: {bg}; width: {f(width)}px">{"".join(cells)}</div>')
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, "sheet.html")
        with open(src, "w") as fh:
            fh.write(cd.page(body, "Motion sheet"))
        subprocess.run([cd.CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        f"--user-data-dir={os.path.join(tmp, 'profile')}",
                        f"--force-device-scale-factor={scale}", f"--window-size={math.ceil(width)},{math.ceil(height)}",
                        "--virtual-time-budget=20000", f"--screenshot={out}", "file://" + src],
                       check=True, capture_output=True)


def make_gif(name, theme, workdir):
    draw, length, crop = SCENES[name]
    x, y, w, h = crop
    times = list(range(0, length, STEP))
    cells = []
    for i, T in enumerate(times):
        html, caption = draw(theme, T, f"{name[:3]}{theme[0]}{i}")
        cells.append(cell(html, crop, caption))
    ch = h + CAP
    cols = max(1, int(7800 / SHEET_SCALE // w))
    rows = max(1, int(7800 / SHEET_SCALE // ch))
    per = cols * rows
    frames_dir = os.path.join(workdir, f"{name}-{theme}")
    os.makedirs(frames_dir, exist_ok=True)
    jobs = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for s in range(0, len(cells), per):
            chunk = cells[s:s + per]
            chunk = chunk + [chunk[-1]] * (per - len(chunk))
            sheet = os.path.join(workdir, f"{name}-{theme}-sheet{s // per}.png")
            jobs.append((pool.submit(shoot, chunk, cols, w, ch, sheet, SHEET_SCALE), sheet, s))
        for job, sheet, s in jobs:
            job.result()
            subprocess.run([FFMPEG, "-v", "error", "-y", "-i", sheet, "-vf", f"untile={cols}x{rows}",
                            "-start_number", str(s), os.path.join(frames_dir, "f%05d.png")], check=True)
    for k in range(len(times), math.ceil(len(times) / per) * per):
        os.remove(os.path.join(frames_dir, f"f{k:05d}.png"))
    out = os.path.join(HERE, f"{name}-{theme}.gif")
    width = round(w * GIF_SCALE)
    subprocess.run([FFMPEG, "-v", "error", "-y", "-framerate", str(FPS), "-i", os.path.join(frames_dir, "f%05d.png"),
                    "-vf", f"scale={width}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=96:stats_mode=full[p];"
                           f"[b][p]paletteuse=dither=none:diff_mode=rectangle",
                    "-loop", "0", out], check=True)
    print(out, os.path.getsize(out) // 1024, "KB")


def make_strip(name, rows, scene, crop, scale):
    draw = scene if callable(scene) else SCENES[scene][0]
    x, y, w, h = crop
    cols = max(len(r) for r in rows)
    cells = []
    for r, row in enumerate(rows):
        for i, (T, label) in enumerate(row):
            html, _ = draw("light", T, f"s{name[:3]}{r}{i}")
            cells.append(cell(html, crop, label=label))
        cells.extend(f'<div style="width: {f(w)}px"></div>' for _ in range(cols - len(row)))
    out = os.path.join(HERE, f"strip-{name}.png")
    shoot(cells, cols, w, h + 22, out, scale, gap=6, bg="#D3DFEE")
    print(out, os.path.getsize(out) // 1024, "KB")


def main():
    only = set(sys.argv[1:])
    with tempfile.TemporaryDirectory() as work:
        for name, (scene, crop, scale, times) in STRIPS.items():
            if not only or name in only:
                make_strip(name, [times], scene, crop, scale)
        if not only or "boosters" in only:
            make_strip("boosters", booster_strip(), "boosters", sq((9, 8), 120), 2)
        for name in SCENES:
            if not only or name in only:
                for theme in THEMES:
                    make_gif(name, theme, work)
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
