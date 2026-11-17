"""Cut Pinfold's illustration sheets into single pictures for the app, light and dark.

The prompts, the four inks and where each picture goes are on docs/design/illustrations.md. The
owner makes each sheet in ChatGPT and saves it as <prompt name>.png, for example
opponent-moods.png. This script is the record of how each sheet was cut.

    python3 docs/design/illustrations/make_pictures.py preview SHEET.png [--out DIR]
    python3 docs/design/illustrations/make_pictures.py cut --sources DIR [--date YYYY-MM-DD]
    python3 docs/design/illustrations/make_pictures.py check
    python3 docs/design/illustrations/make_pictures.py self-test

Needs only Python 3.9 or later: it reads and writes PNG files itself. A sheet of 1024 x 1536
pixels takes a few seconds.

1. `preview` finds the separate drawings on a sheet and numbers them in reading order, row by
   row from the top. It writes SHEET-preview.png (cleaned up, on grey, with a numbered box round
   each drawing) into DIR, the system's temporary folder by default.
2. Write the names into SHEETS below: each picture's name and the numbers it is made of. A
   picture can be made of several numbers (three grass tufts drawn apart), and every number must
   be used once. A name that starts with "_" throws its numbers away (a stray mark).
3. `cut` cuts every sheet in DIR that SHEETS names, writes its pictures into the app and adds or
   updates the sheet's row in the asset register (docs/assets.md). Run it again after any change;
   it replaces the sheet's pictures and row.
4. `check` checks the pictures in the app against SHEETS: both versions there, the same shape,
   only the allowed inks, a register row. `self-test` runs every step on two sheets drawn in code.

How a sheet is cleaned. Each pixel is read as one ink laid over the sheet's paper (the colour
round its edge, usually white or transparent) with some coverage: the four inks of
illustrations.md, so an outline's soft edge stays one ink and only fades. Every pixel of a picture
is then exactly one ink, with the coverage as its transparency, and the paper becomes
transparent. Pale paper shut inside a drawing (the middle of a pond) becomes transparent too;
on a sheet with eyes, a patch big enough to be an eyeball becomes opaque white, the one extra
colour the prompts allow. Pieces closer than the sheet's gap belong to one drawing, so a body,
its hatched shadow and two ink drops stay together.

Sizes. All pictures of one sheet are scaled by the same factor, so their lines stay equally
thick: the largest becomes 4 pixels per dp of its size on screen (the phone's highest density),
or keeps its size when it is smaller. After scaling, each pixel is set back to its nearest ink.

The files. Pictures go to app/src/main/res/drawable-nodpi/ as <sheet>_<name>.png and
<sheet>_<name>_dark.png, with the sheet's name in snake case (opponent_moods_happy.png). The dark
version swaps each ink for the dark version's ink and keeps white. Both sit in one folder, and the
app picks one from the theme it draws with, as it does for colours.
"""

import argparse
import datetime
import math
import random
import re
import struct
import sys
import tempfile
import zlib
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional

REPO = Path(__file__).resolve().parents[3]
DRAWABLES = Path("app/src/main/res/drawable-nodpi")
REGISTER = Path("docs/assets.md")
SCRIPT = "docs/design/illustrations/make_pictures.py"

# The four inks of illustrations.md, in the light and the dark version, and white for eyes.
INK_NAMES = ["red", "blue", "red pale", "blue pale"]
LIGHT = [(0xC0, 0x39, 0x2B), (0x1F, 0x4E, 0x9C), (0xEC, 0xBE, 0xB7), (0xBC, 0xCB, 0xEA)]
DARK = [(0xFF, 0x93, 0x84), (0x6B, 0x90, 0xE8), (0x5E, 0x38, 0x34), (0x2F, 0x3F, 0x66)]
WHITE = (255, 255, 255)
WHITE_INK = 4  # the index of white, after the four inks

PX_PER_DP = 4  # the phone's highest density, xxxhdpi
PAPER_BELOW = 0.12  # coverage under this is paper
DRAWN_FROM = 0.3  # coverage from this counts towards a drawing's shape
PALE_BIAS = 20.0  # prefer a full pale ink to a faint dark one (colour distance per unit of fading)
PAD = 2  # transparent pixels kept round each picture, before scaling


@dataclass
class Sheet:
    prompt: str  # where the prompt is, for the register
    what: str  # "What it is", for the register
    dp: int  # the largest size of a picture on the screen, in dp
    eyes: bool = False  # big pale patches inside a drawing are white eyeballs
    gap: float = 0.02  # pieces closer than this share of the sheet's shorter side are one drawing
    pictures: Optional[Dict[str, List[int]]] = None  # name: numbers from `preview`; None until cut


ON_PAGE = "on docs/design/illustrations.md"

# Every sheet, by its prompt's name. `pictures` is filled in when the owner's sheet arrives.
SHEETS: Dict[str, Sheet] = {
    "opponent-moods": Sheet(f"Prompt 1 {ON_PAGE}", "The computer's six moods, above the board", 108, eyes=True),
    "opponent-strengths": Sheet(f"Prompt 2 {ON_PAGE}", "The computer's three strengths, on the level card", 48, eyes=True),
    "boss-world-1": Sheet(f"Prompt 3 {ON_PAGE}", "The World 1 boss, the ink bottle, above the board and on its level card", 108, eyes=True),
    "map-world-1-nature": Sheet(f"Prompt 4 {ON_PAGE}", "World 1 map page: landscape doodles beside the path", 140),
    "map-world-1-places": Sheet(f"Prompt 5 {ON_PAGE}", "World 1 map page: castle, chest, cottage and flag on the path", 68),
}


# ---------------------------------------------------------------------------------------------
# PNG files


def read_png(path):
    """Width, height and pixels as (r, g, b, a) tuples, row by row. 8-bit, not interlaced."""
    data = Path(path).read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        sys.exit(f"{path}: not a PNG file")
    pos, idat, palette, alpha = 8, [], None, None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
        elif kind == b"PLTE":
            palette = body
        elif kind == b"tRNS":
            alpha = body
        elif kind == b"IDAT":
            idat.append(body)
        elif kind == b"IEND":
            break
    if depth != 8 or interlace:
        sys.exit(f"{path}: needs 8 bits per channel and no interlacing; save it again as a plain PNG")
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(b"".join(idat))
    stride = w * channels
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(h):
        kind, line = raw[i], bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        if kind == 1:
            for x in range(channels, stride):
                line[x] = (line[x] + line[x - channels]) & 255
        elif kind == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif kind == 3:
            for x in range(stride):
                left = line[x - channels] if x >= channels else 0
                line[x] = (line[x] + ((left + prev[x]) >> 1)) & 255
        elif kind == 4:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                b = prev[x]
                c = prev[x - channels] if x >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line)
        prev = line
    px = []
    for line in rows:
        if ctype == 6:
            px.extend(zip(line[0::4], line[1::4], line[2::4], line[3::4]))
        elif ctype == 2:
            px.extend((r, g, b, 255) for r, g, b in zip(line[0::3], line[1::3], line[2::3]))
        elif ctype == 4:
            px.extend((v, v, v, a) for v, a in zip(line[0::2], line[1::2]))
        elif ctype == 0:
            px.extend((v, v, v, 255) for v in line)
        else:
            for v in line:
                a = alpha[v] if alpha is not None and v < len(alpha) else 255
                px.append((palette[3 * v], palette[3 * v + 1], palette[3 * v + 2], a))
    return w, h, px


def write_png(path, w, h, px):
    """Writes (r, g, b, a) pixels, row by row, as an 8-bit RGBA PNG."""
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        for r, g, b, a in px[y * w:(y + 1) * w]:
            raw += bytes((r, g, b, a))

    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))

    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                           + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


# ---------------------------------------------------------------------------------------------
# Cleaning a sheet


@dataclass
class Clean:
    w: int
    h: int
    ink: list  # per pixel: the index of its ink (WHITE_INK for white)
    cover: list  # per pixel: 0.0 (paper) to 1.0
    pieces: list = field(default_factory=list)  # numbered drawings: (x0, y0, x1, y1), x1 and y1 included


def snap(c, paper, cache):
    """The ink and coverage that best explain colour c as one ink over the paper."""
    hit = cache.get(c)
    if hit is None:
        best = None
        e = (c[0] - paper[0], c[1] - paper[1], c[2] - paper[2])
        for k, p in enumerate(LIGHT):
            d = (p[0] - paper[0], p[1] - paper[1], p[2] - paper[2])
            dd = d[0] * d[0] + d[1] * d[1] + d[2] * d[2]
            if dd == 0:
                continue
            a = min(1.0, max(0.0, (e[0] * d[0] + e[1] * d[1] + e[2] * d[2]) / dd))
            miss = math.sqrt(sum((e[i] - a * d[i]) ** 2 for i in range(3)))
            score = miss + PALE_BIAS * (1 - a)
            if best is None or score < best[0]:
                best = (score, k, a)
        hit = cache[c] = (best[1], best[2])
    return hit


def regions(w, h, inside, seeds=None, diagonal=False):
    """Connected regions of the pixels where inside[i] is true, as lists of pixel indexes."""
    seen = bytearray(w * h)
    steps = [(-1, 0), (1, 0), (0, -1), (0, 1)] + ([(-1, -1), (1, -1), (-1, 1), (1, 1)] if diagonal else [])
    out = []
    for start in (seeds if seeds is not None else range(w * h)):
        if seen[start] or not inside[start]:
            continue
        seen[start] = 1
        region, queue = [], deque([start])
        while queue:
            i = queue.popleft()
            region.append(i)
            x, y = i % w, i // w
            for dx, dy in steps:
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h:
                    j = ny * w + nx
                    if not seen[j] and inside[j]:
                        seen[j] = 1
                        queue.append(j)
        out.append(region)
    return out


def survives(region, w, radius):
    """Whether the region is thick enough to keep something after shrinking it by radius pixels."""
    cells = set(region)
    for _ in range(radius):
        cells = {i for i in cells if i - 1 in cells and i + 1 in cells and i - w in cells and i + w in cells}
        if not cells:
            return False
    return True


def merge(boxes, gap):
    """Joins boxes (x0, y0, x1, y1, pixels) closer than gap, round after round, until none are."""
    while True:
        parent = list(range(len(boxes)))

        def root(i):
            while parent[i] != i:
                parent[i] = parent[parent[i]]
                i = parent[i]
            return i

        order = sorted(range(len(boxes)), key=lambda i: boxes[i][0])
        for n, a in enumerate(order):
            p = boxes[a]
            for b in order[n + 1:]:
                q = boxes[b]
                if q[0] - p[2] > gap:
                    break
                if max(p[0] - q[2], q[1] - p[3], p[1] - q[3]) <= gap:
                    parent[root(a)] = root(b)
        groups = {}
        for i, b in enumerate(boxes):
            groups.setdefault(root(i), []).append(b)
        joined = [(min(b[0] for b in g), min(b[1] for b in g), max(b[2] for b in g), max(b[3] for b in g),
                   sum(b[4] for b in g)) for g in groups.values()]
        if len(joined) == len(boxes):
            return joined
        boxes = joined


def clean(w, h, px, sheet):
    """Sets every pixel to one ink with a coverage, and finds and numbers the drawings."""
    flat = [(r, g, b) if a == 255 else ((r * a + 255 * (255 - a)) // 255, (g * a + 255 * (255 - a)) // 255,
                                        (b * a + 255 * (255 - a)) // 255) for r, g, b, a in px]
    edge = [flat[i] for i in range(w)] + [flat[(h - 1) * w + i] for i in range(w)] \
        + [flat[y * w] for y in range(h)] + [flat[y * w + w - 1] for y in range(h)]
    paper = tuple(sorted(c[k] for c in edge)[len(edge) // 2] for k in range(3))
    cache = {}
    ink, cover = [0] * (w * h), [0.0] * (w * h)
    for i, c in enumerate(flat):
        ink[i], cover[i] = snap(c, paper, cache)
    # Paper reached from the sheet's edge is background; pale paper shut inside a drawing is a hole,
    # or an eyeball on a sheet with eyes.
    pale = [a < 0.5 for a in cover]
    border = list(range(w)) + list(range((h - 1) * w, h * w)) + [y * w for y in range(h)] + [y * w + w - 1 for y in range(h)]
    outside = bytearray(w * h)
    for region in regions(w, h, pale, seeds=border):
        for i in region:
            outside[i] = 1
    eye_radius = max(2, round(min(w, h) / 250))
    for region in regions(w, h, [pale[i] and not outside[i] for i in range(w * h)]):
        if sheet.eyes and survives(region, w, eye_radius):
            for i in region:
                ink[i], cover[i] = WHITE_INK, 1.0
        else:
            for i in region:
                if cover[i] < PAPER_BELOW:
                    cover[i] = 0.0
    for i in range(w * h):
        if outside[i] and cover[i] < PAPER_BELOW:
            cover[i] = 0.0
    result = Clean(w, h, ink, cover)
    # Drawings: pieces of ink merged while they are closer than the gap; then lone specks go.
    boxes = []
    for region in regions(w, h, [a >= DRAWN_FROM for a in cover], diagonal=True):
        xs, ys = [i % w for i in region], [i // w for i in region]
        boxes.append((min(xs), min(ys), max(xs), max(ys), len(region)))
    speck = max(8, w * h // 50000)
    boxes = [b[:4] for b in merge(boxes, max(1, round(min(w, h) * sheet.gap))) if b[4] >= speck]
    # Reading order: rows from the top, each row from the left.
    boxes.sort(key=lambda b: (b[1] + b[3]) / 2)
    rows = []
    for b in boxes:
        middle = (b[1] + b[3]) / 2
        if rows and middle <= min(r[3] for r in rows[-1]):
            rows[-1].append(b)
        else:
            rows.append([b])
    result.pieces = [b for row in rows for b in sorted(row, key=lambda b: b[0])]
    return result


# ---------------------------------------------------------------------------------------------
# Pictures


def area_weights(source, target):
    """For each target pixel, the source pixels it covers and how much of each."""
    scale = source / target
    out = []
    for t in range(target):
        lo, hi = t * scale, (t + 1) * scale
        out.append([(s, (min(hi, s + 1) - max(lo, s)) / scale) for s in range(int(lo), min(source, math.ceil(hi)))])
    return out


def nearest(c, inks):
    return min(range(len(inks)), key=lambda k: sum((c[i] - inks[k][i]) ** 2 for i in range(3)))


def picture(clean_sheet, box, keep, scale, eyes):
    """A picture cut from box, keeping pixels for which keep(x, y) is true: (w, h, ink, cover)."""
    x0, y0 = max(0, box[0] - PAD), max(0, box[1] - PAD)
    x1, y1 = min(clean_sheet.w - 1, box[2] + PAD), min(clean_sheet.h - 1, box[3] + PAD)
    w, h = x1 - x0 + 1, y1 - y0 + 1
    ink, cover = [0] * (w * h), [0.0] * (w * h)
    for y in range(h):
        for x in range(w):
            if keep(x0 + x, y0 + y):
                i = (y0 + y) * clean_sheet.w + x0 + x
                ink[y * w + x], cover[y * w + x] = clean_sheet.ink[i], clean_sheet.cover[i]
    if scale >= 1:
        return w, h, ink, cover
    # Average the inks by area (weighted by coverage), then set each pixel back to its nearest ink.
    inks = LIGHT + [WHITE]
    tw, th = max(1, round(w * scale)), max(1, round(h * scale))
    across, down = area_weights(w, tw), area_weights(h, th)
    rows = []
    for y in range(h):
        row = []
        for weights in across:
            r = g = b = a = 0.0
            for s, f in weights:
                c = cover[y * w + s] * f
                if c:
                    k = inks[ink[y * w + s]]
                    r, g, b, a = r + k[0] * c, g + k[1] * c, b + k[2] * c, a + c
            row.append((r, g, b, a))
        rows.append(row)
    allowed = inks if eyes else LIGHT
    out_ink, out_cover = [0] * (tw * th), [0.0] * (tw * th)
    for ty, weights in enumerate(down):
        for tx in range(tw):
            r = g = b = a = 0.0
            for s, f in weights:
                p = rows[s][tx]
                r, g, b, a = r + p[0] * f, g + p[1] * f, b + p[2] * f, a + p[3] * f
            if a * 255 >= 1:
                out_ink[ty * tw + tx] = nearest((r / a, g / a, b / a), allowed)
                out_cover[ty * tw + tx] = min(1.0, a)
    return tw, th, out_ink, out_cover


def pixels(ink, cover, version):
    inks = version + [WHITE]
    return [(*inks[k], round(a * 255)) if round(a * 255) else (0, 0, 0, 0) for k, a in zip(ink, cover)]


def snake(name):
    return name.replace("-", "_")


def cut_sheet(root, name, sheet, source, date):
    """Writes the sheet's pictures into the app and its row into the register. Returns the files."""
    w, h, px = read_png(source)
    sheet_clean = clean(w, h, px, sheet)
    count = len(sheet_clean.pieces)
    used = sorted(n for numbers in sheet.pictures.values() for n in numbers)
    if used != list(range(1, count + 1)):
        sys.exit(f"{name}: the sheet has drawings 1 to {count}, but SHEETS uses {used}; run preview and fix SHEETS")
    for title in sheet.pictures:
        if not re.fullmatch(r"_?[a-z0-9_]+", title):
            sys.exit(f"{name}: picture name {title!r} must be lower case letters, digits and _")
    owner = [0] * (w * h)  # the drawing number of each pixel near a drawing
    for n, b in enumerate(sheet_clean.pieces, start=1):
        for y in range(max(0, b[1] - PAD), min(h, b[3] + PAD + 1)):
            for x in range(max(0, b[0] - PAD), min(w, b[2] + PAD + 1)):
                owner[y * w + x] = n
    kept = {title: numbers for title, numbers in sheet.pictures.items() if not title.startswith("_")}

    def union(numbers):
        bs = [sheet_clean.pieces[n - 1] for n in numbers]
        return min(b[0] for b in bs), min(b[1] for b in bs), max(b[2] for b in bs), max(b[3] for b in bs)

    longest = max(max(b[2] - b[0], b[3] - b[1]) + 1 + 2 * PAD for b in map(union, kept.values()))
    scale = min(1.0, sheet.dp * PX_PER_DP / longest)
    folder = root / DRAWABLES
    prefix = snake(name) + "_"
    for old in folder.glob(prefix + "*.png"):
        old.unlink()
    written = []
    for title, numbers in kept.items():
        mine = set(numbers)
        pw, ph, ink, cover = picture(sheet_clean, union(numbers), lambda x, y: owner[y * w + x] in mine, scale, sheet.eyes)
        for version, suffix in ((LIGHT, ""), (DARK, "_dark")):
            path = folder / f"{prefix}{title}{suffix}.png"
            write_png(path, pw, ph, pixels(ink, cover, version))
            written.append(path)
    edits = (f"Cut from the owner's sheet `{name}.png` (kept outside the repository) by `{SCRIPT}`: "
             f"{len(kept)} pictures; every pixel set to one of the four inks{' or white' if sheet.eyes else ''}; "
             f"dark version by swapping the inks; scaled to {round(scale * 100)}%")
    row = [f"`{(DRAWABLES / (prefix + '*.png')).as_posix()}`", f"{sheet.what}, light and dark",
           "ChatGPT image generation (owner's Pro plan)", date, sheet.prompt,
           "Ours; OpenAI's terms assign output rights to the user", "the owner, with ChatGPT", edits]
    put_row(root / REGISTER, row)
    return written


def put_row(register, cells):
    """Adds the row to the register's table, or replaces the row for the same file."""
    lines = register.read_text().split("\n")
    line = "| " + " | ".join(c.replace("|", "\\|") for c in cells) + " |"
    start = lines.index("## Register")
    last = None
    for i in range(start + 1, len(lines)):
        if lines[i].startswith("## "):
            break
        if lines[i].startswith("|"):
            last = i
            if lines[i].startswith(f"| {cells[0]} |"):
                lines[i] = line
                register.write_text("\n".join(lines))
                return
    lines.insert(last + 1, line)
    register.write_text("\n".join(lines))


# ---------------------------------------------------------------------------------------------
# Commands


DIGITS = ["111101101101111", "010110010010111", "111001111100111", "111001111001111", "101101111001001",
          "111100111001111", "111100111101111", "111001001001001", "111101111101111", "111101111001111"]


def preview(source, out):
    name = Path(source).stem
    sheet = SHEETS.get(name, Sheet("", "", 100))
    w, h, px = read_png(source)
    sheet_clean = clean(w, h, px, sheet)
    inks = LIGHT + [WHITE]
    grey = (200, 200, 200)
    img = []
    for k, a in zip(sheet_clean.ink, sheet_clean.cover):
        c = inks[k]
        img.append(tuple(round(c[i] * a + grey[i] * (1 - a)) for i in range(3)) + (255,))
    mark, size = (230, 0, 120, 255), max(2, w // 200)
    for n, (x0, y0, x1, y1) in enumerate(sheet_clean.pieces, start=1):
        for x in range(x0 - 3, x1 + 4):
            for y in (y0 - 3, y1 + 3):
                if 0 <= x < w and 0 <= y < h:
                    img[y * w + x] = mark
        for y in range(y0 - 3, y1 + 4):
            for x in (x0 - 3, x1 + 3):
                if 0 <= x < w and 0 <= y < h:
                    img[y * w + x] = mark
        for d, digit in enumerate(str(n)):
            for cell, on in enumerate(DIGITS[int(digit)]):
                if on == "1":
                    for yy in range(size):
                        for xx in range(size):
                            x = x0 + d * 4 * size + (cell % 3) * size + xx
                            y = y0 - 3 - 6 * size + (cell // 3) * size + yy
                            if 0 <= x < w and 0 <= y < h:
                                img[y * w + x] = mark
    path = Path(out) / f"{name}-preview.png"
    write_png(path, w, h, img)
    print(f"{name}: {len(sheet_clean.pieces)} drawings; numbered on {path}")
    return sheet_clean


def cut(root, sources, date):
    done = 0
    for name, sheet in SHEETS.items():
        source = Path(sources) / f"{name}.png"
        if not source.exists():
            continue
        if sheet.pictures is None:
            sys.exit(f"{name}: run preview on {source} and write its pictures into SHEETS first")
        day = date or datetime.date.fromtimestamp(source.stat().st_mtime).isoformat()
        files = cut_sheet(root, name, sheet, source, day)
        print(f"{name}: {len(files) // 2} pictures, light and dark")
        done += 1
    if not done:
        sys.exit(f"no sheet named in SHEETS is in {sources}")


def check(root, sheets=None):
    """Problems with the pictures in the app; an empty list when all is well."""
    sheets = SHEETS if sheets is None else sheets
    problems = []
    register = (root / REGISTER).read_text()
    folder = root / DRAWABLES
    for name, sheet in sheets.items():
        prefix = snake(name) + "_"
        files = sorted(folder.glob(prefix + "*.png")) if folder.exists() else []
        if sheet.pictures is None:
            if files:
                problems.append(f"{name}: pictures in the app, but no pictures in SHEETS")
            continue
        wanted = {f"{prefix}{t}{s}.png" for t in sheet.pictures if not t.startswith("_") for s in ("", "_dark")}
        found = {f.name for f in files}
        problems += [f"{name}: missing {f}" for f in sorted(wanted - found)]
        problems += [f"{name}: {f} is not in SHEETS" for f in sorted(found - wanted)]
        if f"| `{(DRAWABLES / (prefix + '*.png')).as_posix()}` |" not in register:
            problems.append(f"{name}: no row in {REGISTER}")
        for title in sheet.pictures:
            light, dark = folder / f"{prefix}{title}.png", folder / f"{prefix}{title}_dark.png"
            if title.startswith("_") or not light.exists() or not dark.exists():
                continue
            lw, lh, lp = read_png(light)
            dw, dh, dp = read_png(dark)
            if (lw, lh) != (dw, dh):
                problems.append(f"{light.name}: {lw}x{lh}, but its dark version is {dw}x{dh}")
                continue
            allowed = set(LIGHT + ([WHITE] if sheet.eyes else []))
            swap = dict(zip(LIGHT + [WHITE], DARK + [WHITE]))
            for (r, g, b, a), (dr, dg, db, da) in zip(lp, dp):
                if a != da:
                    problems.append(f"{light.name}: its dark version is not the same shape")
                    break
                if a and (r, g, b) not in allowed:
                    problems.append(f"{light.name}: colour #{r:02X}{g:02X}{b:02X} is not one of the inks")
                    break
                if a and swap[(r, g, b)] != (dr, dg, db):
                    problems.append(f"{dark.name}: #{dr:02X}{dg:02X}{db:02X} is not the dark ink for #{r:02X}{g:02X}{b:02X}")
                    break
    return problems


# ---------------------------------------------------------------------------------------------
# The self-test: two sheets drawn in code


class Drawing:
    """A sheet drawn at three times its size and then shrunk, so its edges are soft like a real one."""

    SS = 3

    def __init__(self, w, h, paper):
        self.w, self.h = w, h
        self.px = [paper] * (w * self.SS * h * self.SS)

    def fill(self, box, inside, colour):
        s = self.SS
        x0, y0, x1, y1 = (round(v * s) for v in box)
        for y in range(max(0, y0), min(self.h * s, y1)):
            for x in range(max(0, x0), min(self.w * s, x1)):
                if inside((x + 0.5) / s, (y + 0.5) / s):
                    self.px[y * self.w * s + x] = colour

    def disc(self, cx, cy, r, colour):
        self.fill((cx - r, cy - r, cx + r, cy + r), lambda x, y: (x - cx) ** 2 + (y - cy) ** 2 <= r * r, colour)

    def ring(self, cx, cy, r, width, colour):
        self.fill((cx - r - width, cy - r - width, cx + r + width, cy + r + width),
                  lambda x, y: abs(math.hypot(x - cx, y - cy) - r) <= width / 2, colour)

    def line(self, ax, ay, bx, by, width, colour):
        def near(x, y):
            dx, dy = bx - ax, by - ay
            t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)))
            return math.hypot(x - ax - t * dx, y - ay - t * dy) <= width / 2
        self.fill((min(ax, bx) - width, min(ay, by) - width, max(ax, bx) + width, max(ay, by) + width), near, colour)

    def hatch(self, cx, cy, rx, ry, step, width, colour):
        """Parallel slanted lines inside an ellipse, with no outline."""
        def on(x, y):
            return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1 and (x + y) % step < width
        self.fill((cx - rx, cy - ry, cx + rx, cy + ry), on, colour)

    def pixels(self, transparent_paper=False):
        s, out, rng = self.SS, [], random.Random(7)
        paper = self.px[0]
        for y in range(self.h):
            for x in range(self.w):
                block = [self.px[(y * s + j) * self.w * s + x * s + i] for j in range(s) for i in range(s)]
                if transparent_paper:
                    inked = [c for c in block if c != paper]
                    if not inked:
                        out.append((0, 0, 0, 0))
                        continue
                    c = tuple(sum(p[k] for p in inked) // len(inked) for k in range(3))
                    out.append(c + (round(255 * len(inked) / len(block)),))
                else:
                    c = tuple(min(255, max(0, sum(p[k] for p in block) // len(block) + rng.randint(-2, 2)))
                              for k in range(3))
                    out.append(c + (255,))
        return out


def faces_sheet(path):
    """Two blue creatures with white eyes, a hatched shadow and ink drops; transparent paper."""
    d = Drawing(480, 260, (1, 1, 1))  # a colour no ink uses, to mark the transparent paper
    red, blue, blue_pale, pupil = LIGHT[0], LIGHT[1], LIGHT[3], (20, 20, 40)
    for cx in (130, 350):
        d.hatch(cx, 205, 60, 14, 9, 3, blue_pale)  # shadow, touching the body
        d.disc(cx, 120, 70, blue)
        for ex in (cx - 26, cx + 26):
            d.disc(ex, 105, 17, WHITE)
            d.disc(ex + 3, 108, 7, pupil)
        d.line(cx - 22, 150, cx + 22, 150, 4, blue_pale)  # mouth
        d.disc(cx + 78, 70, 5, blue)  # an ink drop, apart from the body but within the gap
    d.disc(350, 45, 20, red)  # a cap on the second one
    write_png(path, d.w, d.h, d.pixels(transparent_paper=True))


def doodles_sheet(path):
    """A tree, a pond, three grass tufts, a cloud, a hatched hill and a speck, on cream paper."""
    d = Drawing(720, 420, (251, 249, 244))
    red, blue, red_pale, blue_pale = LIGHT
    d.hatch(110, 120, 46, 46, 10, 3, red_pale)  # tree crown hatching inside its outline
    d.ring(110, 120, 50, 5, red)
    d.line(110, 172, 110, 215, 5, red)
    d.ring(330, 130, 55, 5, blue)  # pond: pale paper shut inside
    d.line(305, 125, 355, 125, 3, blue_pale)
    for gx in (520, 580, 640):  # three grass tufts, far apart
        for dx in (-8, 0, 8):
            d.line(gx, 150, gx + dx, 118, 3, blue)
    d.ring(170, 330, 45, 5, blue)  # cloud
    d.hatch(480, 340, 90, 40, 10, 3, blue_pale)  # hill, hatching only
    d.disc(650, 380, 0.7, blue)  # a speck
    write_png(path, d.w, d.h, d.pixels())


def self_test():
    sheets = {
        "test-faces": Sheet("A sheet drawn in code", "Test faces", 20, eyes=True,
                            pictures={"plain": [1], "capped": [2]}),
        "test-doodles": Sheet("A sheet drawn in code", "Test doodles", 200,
                              pictures={"tree": [1], "pond": [2], "grass": [3, 4, 5], "cloud": [6], "hill": [7]}),
    }
    with tempfile.TemporaryDirectory() as tmp:
        root, sources = Path(tmp) / "repo", Path(tmp) / "sheets"
        (root / REGISTER).parent.mkdir(parents=True)
        (root / REGISTER).write_text("# Asset register\n\n## Register\n\n| File | What it is | Tool and plan | Date"
                                     " | Prompt or source | Licence | Author | Edits |\n| --- | --- | --- | --- |"
                                     " --- | --- | --- | --- |\n\n## After\n")
        sources.mkdir()
        faces_sheet(sources / "test-faces.png")
        doodles_sheet(sources / "test-doodles.png")
        failures = []

        def expect(ok, what):
            print(("ok    " if ok else "FAIL  ") + what)
            if not ok:
                failures.append(what)

        SHEETS.update(sheets)
        try:
            faces = preview(sources / "test-faces.png", tmp)
            doodles = preview(sources / "test-doodles.png", tmp)
            expect(len(faces.pieces) == 2, "the faces sheet has 2 drawings (body, shadow and drop together)")
            expect(len(doodles.pieces) == 7, "the doodles sheet has 7 drawings (three tufts apart, no speck)")
            xs = [round((b[0] + b[2]) / 2) for b in doodles.pieces]
            expect(xs[:5] == sorted(xs[:5]) and xs[5] < xs[6], "drawings are numbered row by row, left to right")
            for _ in range(2):  # twice: a second cut replaces the files and the row
                for name in sheets:
                    cut_sheet(root, name, sheets[name], sources / f"{name}.png", "2026-11-17")
            files = sorted(p.name for p in (root / DRAWABLES).glob("*.png"))
            expect(len(files) == 2 * 2 + 5 * 2, f"14 files, light and dark ({len(files)})")
            expect(check(root, sheets) == [], "check passes: only the inks, dark versions swapped, rows there")
            _, _, plain = read_png(root / DRAWABLES / "test_faces_plain.png")
            expect(any(p[:3] == WHITE and p[3] == 255 for p in plain), "the eyes are opaque white")
            expect(not any(p[:3] == WHITE and p[3] for p in
                           sum((read_png(f)[2] for f in (root / DRAWABLES).glob("test_doodles_*.png")), [])),
                   "the doodles have no white")
            pw, ph, pond = read_png(root / DRAWABLES / "test_doodles_pond.png")
            expect(pond[(ph // 2 - 15) * pw + pw // 2][3] == 0, "the inside of the pond is transparent")
            expect(pond[(ph // 2) * pw + 3][:3] == LIGHT[1], "the pond's outline is blue ink")
            tw, th, tree = read_png(root / DRAWABLES / "test_doodles_tree.png")
            expect(LIGHT[2] in {p[:3] for p in tree if p[3]}, "the tree's hatching is pale red")
            fw, fh, _ = read_png(root / DRAWABLES / "test_faces_plain.png")
            expect(max(fw, fh) <= 20 * PX_PER_DP, f"faces scaled to at most 80 px ({fw}x{fh})")
            gw, gh, _ = read_png(root / DRAWABLES / "test_doodles_grass.png")
            expect(gw > 3 * gh, f"the three tufts are one picture ({gw}x{gh})")
            rows = [l for l in (root / REGISTER).read_text().split("\n") if l.startswith("| `")]
            expect(len(rows) == 2, "one register row per sheet, after cutting twice")
            cells = [re.split(r"(?<!\\)\|", r.strip().strip("|")) for r in rows]
            expect(all(len(c) == 8 and all(x.strip() for x in c) for c in cells), "each row has 8 filled cells")
            # A broken picture fails the check.
            bad = root / DRAWABLES / "test_doodles_cloud.png"
            bw, bh, bp = read_png(bad)
            bp[next(i for i, q in enumerate(bp) if q[3] == 255)] = (1, 2, 3, 255)
            write_png(bad, bw, bh, bp)
            expect(any("not one of the inks" in p for p in check(root, sheets)), "check finds a wrong colour")
        finally:
            for name in sheets:
                SHEETS.pop(name)
        if failures:
            sys.exit(f"{len(failures)} failed")
        print("all passed")


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("preview", help="number the drawings on a sheet")
    p.add_argument("sheet")
    p.add_argument("--out", default=tempfile.gettempdir())
    c = sub.add_parser("cut", help="cut the sheets in a folder into the app")
    c.add_argument("--sources", required=True)
    c.add_argument("--date", help="the day the sheets were made (default: each file's date)")
    sub.add_parser("check", help="check the pictures in the app")
    sub.add_parser("self-test", help="run every step on two sheets drawn in code")
    args = parser.parse_args()
    if args.command == "preview":
        preview(args.sheet, args.out)
    elif args.command == "cut":
        cut(REPO, args.sources, args.date)
    elif args.command == "check":
        problems = check(REPO)
        print("\n".join(problems) or "all pictures pass")
        sys.exit(1 if problems else 0)
    else:
        self_test()


if __name__ == "__main__":
    main()
