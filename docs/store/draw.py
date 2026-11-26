"""Draws Pinfold's Google Play images from the app's screenshot-test pictures.

Run from the repository root:

    python3 -m venv /tmp/pinfold-venv && /tmp/pinfold-venv/bin/pip install pillow
    /tmp/pinfold-venv/bin/python docs/store/draw.py

It reads the screenshots CI records in app/src/test/screenshots/ (decision 0021) and the
Caveat font in app/src/main/res/font/, and writes into docs/store/:

- feature-graphic.png  1024 x 500, the feature graphic
- screenshot-1.png ... screenshot-8.png  1080 x 1920 (9:16), the phone screenshots

The game pictures are pasted at their own size (720 x 1600), never scaled up, so the game stays
sharp. Only the theme picture (6) and the feature graphic's board are scaled down.
Captions and alt texts live in docs/store/README.md; the captions here must match it.
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHOTS = os.path.join(ROOT, "app", "src", "test", "screenshots")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "caveat_bold.ttf")
OUT = os.path.join(ROOT, "docs", "store")

# Notebook theme colours (docs/design/theme-notebook.md).
BLUE_INK = (0x1F, 0x4E, 0x9C)
GRID_ON_BLUE = (0x2B, 0x5C, 0xAC)  # Blue ink, a little lighter: faint squares on the background
PAPER = (0xFB, 0xF8, 0xF1)
RED_INK = (0xC0, 0x39, 0x2B)
TAPE = (0xE9, 0xC5, 0x8F)  # Highlighter faint, the colour of the tape on the game's notes

# The screenshots, in the order Google Play shows them. The first shows a capture (R-05).
SCREENS = [
    ("game_big_capture", "Trap their dots in your ring"),
    ("game_kraken_attacks", "Bosses that bend the rules"),
    ("game_bomb_aimed", "Boosters for the tight spots"),
    ("map_world_3_page", "75 levels in four worlds"),
    ("daily_board", "A fresh puzzle every day"),
    (["game_big_capture_star_chart", "game_big_capture_neon_arcade_dark", "game_big_capture_dark"],
     "Paper, stars or neon, light or dark"),
    ("shop_earn_only", "Earn every look with stars"),
    ("play_setup", "Or face a friend on one phone"),
]

W, H = 1080, 1920
CAPTION_BAND = 320  # 16.7% of the height: Play wants taglines under 20% of the image
SCREEN_W, SCREEN_H = 720, 1600


def shot(name):
    path = os.path.join(SHOTS, name + ".png")
    if not os.path.exists(path):
        sys.exit(f"missing {path}: record the screenshots first (CLAUDE.md, Recording screenshots)")
    return Image.open(path).convert("RGB")


def grid_background(w, h, step, colour=BLUE_INK, line=GRID_ON_BLUE, width=2):
    im = Image.new("RGB", (w, h), colour)
    d = ImageDraw.Draw(im)
    off = (w % step) // 2
    for x in range(off, w, step):
        d.line([(x, 0), (x, h)], fill=line, width=width)
    for y in range(off, h, step):
        d.line([(0, y), (w, y)], fill=line, width=width)
    return im


def rounded(im, radius):
    """The picture with rounded corners, as RGBA."""
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.width - 1, im.height - 1], radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def paste_with_shadow(base, im, xy, blur=22, offset=(0, 14), alpha=110):
    """Pastes an RGBA picture with a soft shadow under it."""
    shadow = Image.new("RGBA", base.size, (0, 0, 0, 0))
    a = im.split()[3].point(lambda v: v * alpha // 255)
    black = Image.new("RGBA", im.size, (10, 20, 40, 255))
    black.putalpha(a)
    shadow.alpha_composite(black, (xy[0] + offset[0], xy[1] + offset[1]))
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    base.alpha_composite(shadow)
    base.alpha_composite(im, xy)


def tape(base, centre, size=(180, 50), angle=-3):
    """A strip of paper tape, like the one that holds the game's notes."""
    strip = Image.new("RGBA", size, TAPE + (225,))
    strip = strip.rotate(angle, expand=True, resample=Image.BICUBIC)
    base.alpha_composite(strip, (centre[0] - strip.width // 2, centre[1] - strip.height // 2))


def caption(base, text, band_h, size):
    d = ImageDraw.Draw(base)
    font = ImageFont.truetype(FONT, size)
    while d.textlength(text, font=font) > base.width - 120:
        size -= 4
        font = ImageFont.truetype(FONT, size)
    l, t, r, b = d.textbbox((0, 0), text, font=font)
    x = (base.width - (r - l)) // 2 - l
    y = (band_h - (b - t)) // 2 - t - 6
    d.text((x, y), text, font=font, fill=PAPER)
    return font


def one_screen(name, text):
    base = grid_background(W, H, 60).convert("RGBA")
    caption(base, text, CAPTION_BAND, 104)
    screen = rounded(shot(name), 40)
    x, y = (W - SCREEN_W) // 2, CAPTION_BAND
    paste_with_shadow(base, screen, (x, y))  # the bottom 40 px of rounding fall off the image
    tape(base, (W // 2, y + 4))
    return base


def three_screens(names, text):
    base = grid_background(W, H, 60).convert("RGBA")
    caption(base, text, CAPTION_BAND, 104)
    scale = 0.62
    w, h = int(SCREEN_W * scale), int(SCREEN_H * scale)
    places = [(-7, 235, 790), (7, 845, 790), (0, 540, 1060)]  # angle, centre x, centre y
    for (angle, cx, cy), name in zip(places, names):
        screen = rounded(shot(name).resize((w, h), Image.LANCZOS), 26)
        screen = screen.rotate(angle, expand=True, resample=Image.BICUBIC)
        paste_with_shadow(base, screen, (cx - screen.width // 2, cy - screen.height // 2))
    return base


def feature_graphic():
    fw, fh = 1024, 500
    base = grid_background(fw, fh, 40).convert("RGBA")

    # Right: the board of screenshot 1 on a paper card, held by tape. Kept clear of the middle,
    # where Play may draw the video's play button, and of the outer edges, which Play may crop.
    board = shot("game_big_capture").crop((22, 638, 698, 1314)).resize((300, 300), Image.LANCZOS)
    pad = 20
    card = Image.new("RGB", (board.width + 2 * pad, board.height + 2 * pad), PAPER)
    card.paste(board, (pad, pad))
    card = rounded(card, 10).rotate(4, expand=True, resample=Image.BICUBIC)
    cx, cy = 775, 250
    paste_with_shadow(base, card, (cx - card.width // 2, cy - card.height // 2), blur=14,
                      offset=(0, 10))
    tape(base, (cx - 6, cy - card.height // 2 + 22), size=(130, 38), angle=-5)

    # Left: the name in the logo's handwriting, a red pen underline, and one line.
    d = ImageDraw.Draw(base)
    name_font = ImageFont.truetype(FONT, 140)
    line_font = ImageFont.truetype(FONT, 52)
    left = 90
    l, t, r, b = d.textbbox((0, 0), "Pinfold", font=name_font)
    d.text((left - l, 95 - t), "Pinfold", font=name_font, fill=PAPER)
    uy = 95 + (b - t) + 20
    d.line([(left + 4, uy + 4), (left + (r - l) * 0.55, uy), (left + (r - l) - 6, uy + 6)],
           fill=RED_INK, width=9, joint="curve")
    for i, line in enumerate(["Close a ring.", "Capture the dots."]):
        d.text((left, uy + 28 + i * 62), line, font=line_font, fill=PAPER)
    return base.convert("RGB")


def main():
    feature_graphic().save(os.path.join(OUT, "feature-graphic.png"), optimize=True)
    for i, (name, text) in enumerate(SCREENS, start=1):
        im = three_screens(name, text) if isinstance(name, list) else one_screen(name, text)
        im.convert("RGB").save(os.path.join(OUT, f"screenshot-{i}.png"), optimize=True)
    print("wrote docs/store/feature-graphic.png and docs/store/screenshot-1..8.png")


if __name__ == "__main__":
    main()
