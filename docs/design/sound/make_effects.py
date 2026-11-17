"""Make Pinfold's 17 sound effects (19 files) for the app, and check them.

The recipes and the reasons behind them are on docs/design/sound.md. This script is the prompt of
the 14 effects made in code, and the record of how the other three were cut from the owner's
source sounds.

    python3 docs/design/sound/make_effects.py                    # the 14 effects made in code
    python3 docs/design/sound/make_effects.py --sources DIR      # all 17, DIR holds fx_*.wav
    python3 docs/design/sound/make_effects.py --check            # check the files in the app

Needs Python 3.9 or later and vorbis-tools (oggenc and oggdec: `brew install vorbis-tools`, or
`apt install vorbis-tools`). The files go to app/src/main/assets/audio/ as sfx_<name>.ogg: mono,
48 kHz, Ogg Vorbis. Every run gives the same files.

Loudness. Every file is normalised to the same momentary loudness: the loudest 400 ms of the file,
weighted as in ITU-R BS.1770 (the "M max" of a loudness meter). Effects shorter than 400 ms are
measured with silence after them. A pen tap is very short and sharp, so at that loudness its
first millisecond would clip: a limiter lowers such peaks, by up to about 3.5 dB, for a
millisecond or two. The check fails if the files differ by more than 2 dB, if one peaks above -1 dBFS, or if
the set of files differs from the list below.
"""

import argparse
import math
import random
import shutil
import subprocess
import sys
import tempfile
import wave
import zlib
from array import array
from pathlib import Path

RATE = 48000
REPO = Path(__file__).resolve().parents[3]
OUT = REPO / "app" / "src" / "main" / "assets" / "audio"

TARGET_LUFS = -22.0  # momentary maximum every file is normalised to
LIMIT_DB = -1.5  # peaks above this are limited before encoding
MAX_PEAK_DB = -1.0  # no sample above this, after encoding
MAX_SPREAD_DB = 2.0  # the V-08 check
QUALITY = "4"  # oggenc quality, about 64 kbit/s for mono

# Notes, A4 = 440 Hz. Effects made in code use only D, E, G and A (sound.md, rule 5).
NOTE = {
    name + str(octave): 440.0 * 2 ** ((semis - 9) / 12 + octave - 4)
    for octave in range(2, 9)
    for name, semis in (("D", 2), ("E", 4), ("G", 7), ("A", 9))
}


# ---------------------------------------------------------------- building blocks


class Sound:
    """A mono buffer of a fixed length that voices are added into."""

    def __init__(self, seconds):
        self.x = [0.0] * round(seconds * RATE)

    def add(self, at, voice, gain=1.0):
        start = round(at * RATE)
        for i, v in enumerate(voice):
            j = start + i
            if j >= len(self.x):
                break
            self.x[j] += gain * v


def envelope(i, attack, tau):
    """Raised-cosine attack of `attack` seconds, then an exponential fall with time constant tau."""
    t = i / RATE
    if t < attack:
        return 0.5 - 0.5 * math.cos(math.pi * t / attack)
    return math.exp(-(t - attack) / tau)


def partials(freq, parts, attack, seconds, cents=None):
    """Sum of sine partials: parts is [(ratio, amplitude, tau)]. cents(t) bends the pitch."""
    n = round(seconds * RATE)
    out = [0.0] * n
    for ratio, amp, tau in parts:
        if freq * ratio > 18000:
            continue
        phase = 0.0
        end = min(n, round((attack + tau * 9.2) * RATE))  # 9.2 tau: below -80 dB
        for i in range(end):
            f = freq * ratio
            if cents:
                f *= 2 ** (cents(i / RATE) / 1200)
            phase += 2 * math.pi * f / RATE
            out[i] += amp * envelope(i, attack, tau) * math.sin(phase)
    return out


def mallet(note, seconds=1.0, ring=1.0, bright=1.0):
    """A soft felt mallet on a small metal bar: a pure tone, faint overtones, a 5 ms start.

    The tone falls quickly just after the strike, then rings on more slowly, as a struck bar does.
    """
    f = NOTE[note]
    tau = ring * 0.55 * (NOTE["D5"] / f) ** 0.5
    return partials(f, [(1, 0.55, tau * 0.15), (1, 0.45, tau),
                        (4, 0.10 * bright, tau / 4), (10, 0.025 * bright, tau / 10)],
                    0.005, seconds)


def pluck(note, seconds=1.0, ring=1.0, cents=None):
    """A soft nylon string: warmer and shorter than the mallet."""
    f = NOTE[note]
    tau = ring * 0.30
    parts = [(n, abs(math.sin(math.pi * n * 0.2)) / n ** 1.6, tau / (1 + 0.45 * (n - 1)))
             for n in range(1, 13)]
    return partials(f, parts, 0.003, seconds, cents)


def wood(note, soft=False):
    """A short, hollow knock with a little pitch: it falls 3% in its first 10 ms."""
    f = NOTE[note]
    parts = [(1, 1.0, 0.040), (2.57, 0.25 if soft else 0.45, 0.018), (4.1, 0.05 if soft else 0.15, 0.008)]
    body = partials(f, parts, 0.004 if soft else 0.0008, 0.25,
                    cents=lambda t: -50 * (1 - math.exp(-t / 0.010)))
    if not soft:  # the stick's click
        rng = random.Random(7)
        click = lowpass([rng.uniform(-1, 1) * math.exp(-i / (0.0015 * RATE)) for i in range(240)], 3000)
        for i, v in enumerate(click):
            body[i] += 0.25 * v
    return body


def thump(seconds=0.7):
    """A low, soft tone that falls quickly in pitch: 120 Hz to 45 Hz."""
    out, phase = [], 0.0
    for i in range(round(seconds * RATE)):
        t = i / RATE
        phase += 2 * math.pi * (45 + 75 * math.exp(-t / 0.04)) / RATE
        out.append(envelope(i, 0.004, 0.12) * math.sin(phase))
    return out


def paper_click():
    """A short, soft paper click: 6 ms of noise between 1.5 and 6 kHz."""
    rng = random.Random(11)
    noise = [rng.uniform(-1, 1) * math.exp(-i / (0.002 * RATE)) for i in range(round(0.006 * RATE))]
    return lowpass(highpass(noise, 1500), 6000)


def biquad(x, b, a):
    b0, b1, b2 = b
    a1, a2 = a
    y, x1, x2, y1, y2 = [], 0.0, 0.0, 0.0, 0.0
    for v in x:
        o = b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2, x1, y2, y1 = x1, v, y1, o
        y.append(o)
    return y


def _rbj(kind, freq, q=0.7071):
    w = 2 * math.pi * freq / RATE
    alpha, c = math.sin(w) / (2 * q), math.cos(w)
    if kind == "low":
        b = ((1 - c) / 2, 1 - c, (1 - c) / 2)
    else:
        b = ((1 + c) / 2, -(1 + c), (1 + c) / 2)
    a0 = 1 + alpha
    return [v / a0 for v in b], (-2 * c / a0, (1 - alpha) / a0)


def lowpass(x, freq):
    return biquad(x, *_rbj("low", freq))


def highpass(x, freq):
    return biquad(x, *_rbj("high", freq))


def fade(x, fade_in, fade_out):
    """Raised-cosine fades at both ends, in seconds; the file always ends at zero."""
    n_in, n_out = round(fade_in * RATE), round(fade_out * RATE)
    for i in range(min(n_in, len(x))):
        x[i] *= 0.5 - 0.5 * math.cos(math.pi * i / n_in)
    for i in range(min(n_out, len(x))):
        x[len(x) - 1 - i] *= 0.5 - 0.5 * math.cos(math.pi * i / n_out)
    return x


# ---------------------------------------------------------------- the 14 effects made in code
# Each follows its row in sound.md, "Sounds made in code". Gains inside a sound set the balance
# between its notes; the file's loudness is set afterwards.


def ring_close():  # mallet D5, then A5 60 ms later; 400 ms
    s = Sound(0.40)
    s.add(0.000, mallet("D5", 0.4))
    s.add(0.060, mallet("A5", 0.4), 0.9)
    return fade(s.x, 0, 0.08)


def captured():  # mallet A4, then E4 80 ms later, softer; 500 ms
    s = Sound(0.50)
    s.add(0.000, mallet("A4", 0.5, bright=0.6))
    s.add(0.080, mallet("E4", 0.5, bright=0.4), 0.6)
    return fade(s.x, 0, 0.10)


def capture_big():  # thump, with a mallet and pluck chord D4, A4, E5 on it; 700 ms
    s = Sound(0.70)
    s.add(0.000, thump(0.7), 0.9)
    for note in ("D4", "A4", "E5"):
        s.add(0.010, mallet(note, 0.7), 0.35)
        s.add(0.010, pluck(note, 0.7), 0.30)
    return fade(s.x, 0, 0.15)


def threat():  # wood G3 twice, 120 ms apart; 300 ms
    s = Sound(0.30)
    s.add(0.000, wood("G3"))
    s.add(0.120, wood("G3"), 0.85)
    return fade(s.x, 0, 0.05)


def dots_low():  # wood D4, soft; 200 ms
    s = Sound(0.20)
    s.add(0.000, wood("D4", soft=True))
    return fade(s.x, 0, 0.05)


def boss_turn():  # pluck D3 and G3 together, then a slow wobble in pitch; 800 ms
    def wobble(t):  # starts at 200 ms, 3.5 wobbles a second, up to 45 cents
        if t < 0.2:
            return 0.0
        depth = 45 * min(1.0, (t - 0.2) / 0.3)
        return depth * math.sin(2 * math.pi * 3.5 * (t - 0.2))

    s = Sound(0.80)
    for note in ("D3", "G3"):
        s.add(0.000, pluck(note, 0.8, ring=2.0, cents=wobble), 0.5)
    return fade(s.x, 0, 0.15)


def double_dot():  # mallet D5, then D6 80 ms later; 300 ms
    s = Sound(0.30)
    s.add(0.000, mallet("D5", 0.3))
    s.add(0.080, mallet("D6", 0.3), 0.9)
    return fade(s.x, 0, 0.06)


def area_light():  # mallet D5; the app plays it faster for each later area; 300 ms
    s = Sound(0.30)
    s.add(0.000, mallet("D5", 0.3))
    return fade(s.x, 0, 0.08)


def win():  # mallet D5, G5, A5, D6, 120 ms apart, the last held, over a pluck chord D4, A4; 1.5 s
    s = Sound(1.50)
    for k, note in enumerate(("D5", "G5", "A5")):
        s.add(0.120 * k, mallet(note, 1.5), 0.8)
    s.add(0.360, mallet("D6", 1.5, ring=2.5), 0.9)
    for note in ("D4", "A4"):
        s.add(0.000, pluck(note, 1.5, ring=1.5), 0.35)
    return fade(s.x, 0, 0.40)


def lose():  # mallet A4, G4, E4, 180 ms apart, the last soft; 1.2 s
    s = Sound(1.20)
    s.add(0.000, mallet("A4", 1.2, bright=0.5))
    s.add(0.180, mallet("G4", 1.2, bright=0.5), 0.9)
    s.add(0.360, mallet("E4", 1.2, ring=1.6, bright=0.3), 0.6)
    return fade(s.x, 0, 0.35)


def draw():  # mallet D5, then A4; 800 ms
    s = Sound(0.80)
    s.add(0.000, mallet("D5", 0.8))
    s.add(0.200, mallet("A4", 0.8), 0.95)
    return fade(s.x, 0, 0.20)


def star():  # a short, soft paper click, then mallet D6; 400 ms
    s = Sound(0.40)
    s.add(0.000, paper_click(), 0.5)
    s.add(0.015, mallet("D6", 0.4))
    return fade(s.x, 0, 0.10)


def world_open():  # mallet D5 to E6, 80 ms apart, then a pluck and mallet chord D4, A4, E5, held; 2 s
    s = Sound(2.00)
    for k, note in enumerate(("D5", "E5", "G5", "A5", "D6", "E6")):
        s.add(0.080 * k, mallet(note, 2.0), 0.55 + 0.05 * k)
    for note in ("D4", "A4", "E5"):
        s.add(0.480, mallet(note, 1.6, ring=3.0), 0.40)
        s.add(0.480, pluck(note, 1.6, ring=2.5), 0.30)
    return fade(s.x, 0, 0.50)


def reward():  # mallet A6, D7, E7, A7 within 150 ms, over a pluck chord D4, A4; 600 ms
    s = Sound(0.60)
    for k, note in enumerate(("A6", "D7", "E7", "A7")):
        s.add(0.050 * k, mallet(note, 0.6, ring=1.5), 0.6)
    for note in ("D4", "A4"):
        s.add(0.000, pluck(note, 0.6), 0.45)
    return fade(s.x, 0, 0.15)


MADE_IN_CODE = {
    "sfx_ring_close": ring_close,
    "sfx_captured": captured,
    "sfx_capture_big": capture_big,
    "sfx_threat": threat,
    "sfx_dots_low": dots_low,
    "sfx_boss_turn": boss_turn,
    "sfx_double_dot": double_dot,
    "sfx_area_light": area_light,
    "sfx_win": win,
    "sfx_lose": lose,
    "sfx_draw": draw,
    "sfx_star": star,
    "sfx_world_open": world_open,
    "sfx_reward": reward,
}

# ---------------------------------------------------------------- the 3 effects from source sounds
# Each source file holds several takes; these are the ones kept, chosen for a single clean event
# with the quietest room around it. Times are in seconds from the start of the source file.
# (name, source file, start, length, silence before it, high-pass Hz, low-pass Hz or None, fade-out)

FROM_SOURCES = [
    ("sfx_place_1", "fx_pen_tap_1.wav", 3.148, 0.150, 0.0, 80, None, 0.090),
    ("sfx_place_2", "fx_pen_tap_2.wav", 1.714, 0.150, 0.0, 80, None, 0.090),
    ("sfx_place_3", "fx_pen_tap_3.wav", 2.408, 0.150, 0.0, 80, None, 0.090),
    # The quick scribble at the start of the file, and its tail.
    ("sfx_ring_draw", "fx_pen_circle.wav", 0.000, 0.400, 0.0, 80, None, 0.240),
    # The thud's main hit (1.198 s) lands 150 ms into the file, when the bomb lands (sound.md,
    # Boosters). The low-pass softens a light rattle 100 ms after the hit.
    ("sfx_bomb", "fx_book_drop.wav", 1.170, 0.328, 0.122, 40, 4000, 0.200),
]


def read_wav(path):
    """A WAV file as mono floats at RATE."""
    with wave.open(str(path)) as w:
        if w.getframerate() != RATE:
            sys.exit(f"{path}: {w.getframerate()} Hz; this script expects {RATE} Hz")
        width, channels = w.getsampwidth(), w.getnchannels()
        raw = w.readframes(w.getnframes())
    if width == 2:
        samples = array("h", raw)
        scale = 32768.0
    elif width == 3:
        samples = [int.from_bytes(raw[i:i + 3], "little", signed=True) for i in range(0, len(raw), 3)]
        scale = 8388608.0
    else:
        sys.exit(f"{path}: {8 * width}-bit samples; use 16 or 24-bit WAV")
    return [sum(samples[i:i + channels]) / channels / scale for i in range(0, len(samples), channels)]


def from_source(folder, file, start, length, silence, hp, lp, fade_out):
    x = read_wav(Path(folder) / file)
    a = round(start * RATE)
    # Filter a little before the cut so the filters have settled when the sound starts.
    pre = min(a, round(0.05 * RATE))
    cut = highpass(x[a - pre:a + round(length * RATE)], hp)
    if lp:
        cut = lowpass(cut, lp)
    cut = fade(cut[pre:], 0.001, fade_out)
    return [0.0] * round(silence * RATE) + cut


# ---------------------------------------------------------------- loudness, files, check


def loudness(x):
    """Momentary maximum loudness (BS.1770 K-weighting, 400 ms windows every 10 ms), in LUFS."""
    # K-weighting filters for 48 kHz, from ITU-R BS.1770-4.
    k = biquad(x + [0.0] * round(0.4 * RATE),
               (1.53512485958697, -2.69169618940638, 1.19839281085285),
               (-1.69065929318241, 0.73248077421585))
    k = biquad(k, (1.0, -2.0, 1.0), (-1.99004745483398, 0.99007225036621))
    win, step = round(0.4 * RATE), round(0.01 * RATE)
    sums = [0.0]
    for v in k:
        sums.append(sums[-1] + v * v)
    best = max(sums[i + win] - sums[i] for i in range(0, len(k) - win + 1, step))
    return -0.691 + 10 * math.log10(best / win + 1e-20)


def peak_db(x):
    return 20 * math.log10(max(abs(v) for v in x) + 1e-20)


def limit(x, ceiling_db):
    """Hold every sample under the ceiling: the gain dips 1 ms before a peak and recovers in 10 ms."""
    c = 10 ** (ceiling_db / 20)
    need = [min(1.0, c / abs(v)) if v else 1.0 for v in x]
    look = round(0.001 * RATE)
    held = [min(need[max(0, i - look):i + look + 1]) for i in range(len(x))]
    rel, att = 1 - math.exp(-1 / (0.010 * RATE)), 1 - math.exp(-1 / (0.001 * RATE))
    g = held[:]
    for i in range(1, len(g)):  # recover slowly after a peak
        g[i] = min(held[i], g[i - 1] + (1 - g[i - 1]) * rel)
    for i in range(len(g) - 2, -1, -1):  # dip smoothly before it
        g[i] = min(g[i], g[i + 1] + (1 - g[i + 1]) * att)
    return [v * k for v, k in zip(x, g)], 20 * math.log10(min(g))


def write_ogg(name, x, folder):
    reduction = 0.0
    for _ in range(40):  # limiting lowers the loudness a little, so normalise and limit again
        gain = 10 ** ((TARGET_LUFS - loudness(x)) / 20)
        x = [v * gain for v in x]
        if peak_db(x) <= LIMIT_DB:
            break
        x, r = limit(x, LIMIT_DB)
        reduction = min(reduction, r)
        if abs(loudness(x) - TARGET_LUFS) < 0.1:
            break
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(array("h", (max(-32767, min(32767, round(v * 32767))) for v in x)).tobytes())
        serial = str(zlib.crc32(name.encode()) & 0x7FFFFFFF)  # fixed, so every run gives the same file
        subprocess.run(["oggenc", "-Q", "-q", QUALITY, "--serial", serial,
                        "-o", str(folder / f"{name}.ogg"), str(wav)], check=True)
    limited = f", peaks limited by {-reduction:.1f} dB" if reduction else ""
    print(f"  {name}.ogg  {len(x) / RATE * 1000:5.0f} ms{limited}")


def expected_names():
    return sorted(list(MADE_IN_CODE) + [row[0] for row in FROM_SOURCES])


def check(folder):
    """Decode every effect in the app and check the set, the loudness and the peaks."""
    problems = []
    found = sorted(p.stem for p in folder.glob("sfx_*.ogg"))
    for name in sorted(set(expected_names()) - set(found)):
        problems.append(f"{name}.ogg is missing")
    for name in sorted(set(found) - set(expected_names())):
        problems.append(f"{name}.ogg is not in sound.md's list")
    levels = {}
    print(f"  {'file':24} {'length':>7} {'loudness':>9} {'peak':>9}")
    with tempfile.TemporaryDirectory() as tmp:
        for name in found:
            wav = Path(tmp) / f"{name}.wav"
            subprocess.run(["oggdec", "-Q", "-o", str(wav), str(folder / f"{name}.ogg")], check=True)
            x = read_wav(wav)
            levels[name], peak = loudness(x), peak_db(x)
            print(f"  {name + '.ogg':24} {len(x) / RATE * 1000:5.0f} ms {levels[name]:5.1f} LUFS {peak:5.1f} dBFS")
            if peak > MAX_PEAK_DB + 0.5:  # encoding may add a little to the peak
                problems.append(f"{name}.ogg peaks at {peak:.1f} dBFS")
    if levels:
        spread = max(levels.values()) - min(levels.values())
        print(f"  loudness spread: {spread:.1f} dB (at most {MAX_SPREAD_DB:.0f} dB)")
        if spread > MAX_SPREAD_DB:
            problems.append(f"loudness differs by {spread:.1f} dB across the set")
    for p in problems:
        print("PROBLEM: " + p)
    return not problems


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--sources", help="folder with fx_pen_tap_1.wav to _3, fx_pen_circle.wav, fx_book_drop.wav")
    ap.add_argument("--check", action="store_true", help="only check the files in the app")
    ap.add_argument("--out", default=str(OUT), help="where to write the .ogg files")
    args = ap.parse_args()
    out = Path(args.out)
    for tool in ("oggenc", "oggdec"):
        if not shutil.which(tool):
            sys.exit(f"{tool} not found; install vorbis-tools")
    if args.check:
        sys.exit(0 if check(out) else 1)
    out.mkdir(parents=True, exist_ok=True)
    print(f"Writing to {out}")
    for name, make in MADE_IN_CODE.items():
        write_ogg(name, make(), out)
    if args.sources:
        for name, *row in FROM_SOURCES:
            write_ogg(name, from_source(args.sources, *row), out)
    else:
        print("  (no --sources: the effects cut from the owner's sounds were left as they are)")


if __name__ == "__main__":
    main()
