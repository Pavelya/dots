"""Runs the colour-blindness check of docs/design/pieces.md on the app's own screenshots.

Run from the repository root (needs Python 3 and rsvg-convert, from librsvg):

    python3 docs/design/pieces/simulate.py

It reads the board screenshots that the app's tests draw (app/src/test/screenshots/board_pieces*.png:
every piece and mark, with the usual and with high-contrast pieces, light and dark) and writes
each one as seen, in greyscale, and as seen with protanopia, deuteranopia and tritanopia, as
app-<look>-<theme>-<view>.png next to this file. The simulations are the ones draw.py uses.
"""

import base64
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from draw import SIMULATIONS  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
SCREENSHOTS = os.path.join(HERE, "..", "..", "..", "app", "src", "test", "screenshots")

# Output name: screenshot.
SOURCES = {
    "app-usual-light": "board_pieces.png",
    "app-usual-dark": "board_pieces_dark.png",
    "app-contrast-light": "board_pieces_contrast.png",
    "app-contrast-dark": "board_pieces_contrast_dark.png",
}


def png_size(data):
    """Width and height from a PNG's header."""
    return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")


def simulated_svg(data, sim):
    w, h = png_size(data)
    image = f'<image width="{w}" height="{h}" href="data:image/png;base64,{base64.b64encode(data).decode()}"/>'
    if sim is None:
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}">{image}</svg>\n'
    values = " ".join(" ".join(str(v) for v in row) + " 0 0" for row in SIMULATIONS[sim]) + " 0 0 0 1 0"
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}"><defs>'
            f'<filter id="sim" x="0" y="0" width="1" height="1" color-interpolation-filters="linearRGB">'
            f'<feColorMatrix type="matrix" values="{values}"/></filter></defs>'
            f'<g filter="url(#sim)">{image}</g></svg>\n')


def main():
    for name, source in SOURCES.items():
        with open(os.path.join(SCREENSHOTS, source), "rb") as fh:
            data = fh.read()
        for sim in SIMULATIONS:
            src = os.path.join(HERE, "_sim.svg")
            with open(src, "w") as fh:
                fh.write(simulated_svg(data, None if sim == "seen" else sim))
            subprocess.run(["rsvg-convert", "-o", os.path.join(HERE, f"{name}-{sim}.png"), src], check=True)
            os.remove(src)


if __name__ == "__main__":
    main()
