"""Cut Pinfold's six music loops from the owner's Suno tracks, and check them.

The tracks, their prompts and where each one plays are on docs/design/sound.md (Music). This
script is the record of how each loop was cut.

    python3 docs/design/sound/make_music.py --sources DIR    # DIR holds music_*.wav
    python3 docs/design/sound/make_music.py --check          # check the files in the app

Needs Python 3.9 or later and vorbis-tools (oggenc and oggdec: `brew install vorbis-tools`, or
`apt install vorbis-tools`). The files go to app/src/main/assets/audio/ as music_<name>.ogg:
stereo, 48 kHz, Ogg Vorbis. Every run gives the same files. It takes about a minute.

How a loop is cut. A Suno track never repeats itself exactly, so a loop joins two places where
the music is nearly the same: the end of the loop runs on into the bars that follow its start.
Both cut points sit on a beat, 10 ms before the note that starts it, and the loop is a whole
number of beats long. In the last 300 ms of the loop, the music that came before the loop's start
fades in while the track fades out, so the end flows into the start without a click.

How the points were chosen. A search (librosa, outside this script) compared every pair of beats
in each track on two things: whether the 16 beats around one point sound like the 16 around the
other (instruments and chords), and whether the jump across the join is no bigger than an
ordinary step from one beat to the next in the same track. It kept the longest loop that passed
both and stays clear of the track's intro and ending. To hear a join, play the last seconds of a
loop and then its first seconds.

Loudness. Every loop is set to the same integrated loudness, -23 LUFS (ITU-R BS.1770, the
broadcast standard), so the music does not jump when it fades from the map into a level. The
tracks need no limiting: they peak well under -1 dBFS at that level. The check fails if a loop
is missing or extra, if its length differs from the list below by more than 1 ms, if its
loudness is more than 0.5 dB from the target, or if it peaks above -1 dBFS.
"""

import argparse
import math
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

TARGET_LUFS = -23.0  # integrated loudness of every loop
MAX_PEAK_DB = -1.0
CROSSFADE = 0.300  # seconds at the end of the loop where it fades into its own start
QUALITY = "3"  # oggenc quality, about 110 kbit/s for stereo

# (file, start, end): seconds in the owner's master music_<name>.wav. The loop is [start, end).
LOOPS = [
    ("music_map", 14.281, 82.279),  # 136 beats at 120 BPM, 68.0 s
    ("music_notebook", 14.778, 155.775),  # 184 beats at 78 BPM, 141.0 s
    ("music_garden", 20.835, 134.959),  # 160 beats at 83 BPM, 114.1 s
    ("music_night_sky", 23.151, 169.043),  # 166 beats at 68 BPM, 145.9 s
    ("music_sea", 16.203, 177.215),  # 288 beats at 107 BPM, 161.0 s
    ("music_boss", 9.306, 50.218),  # 68 beats at 100 BPM, 40.9 s
]


def read_wav(path):
    """A 16-bit stereo WAV as two float arrays at RATE."""
    with wave.open(str(path)) as w:
        if w.getframerate() != RATE or w.getnchannels() != 2 or w.getsampwidth() != 2:
            sys.exit(f"{path}: expected 16-bit stereo at {RATE} Hz")
        raw = array("h", w.readframes(w.getnframes()))
    if sys.byteorder == "big":
        raw.byteswap()
    return array("f", raw[0::2]), array("f", raw[1::2])


def cut_loop(left, right, start, end):
    """Samples [start, end) of each channel, the last CROSSFADE seconds fading into the start."""
    a, b, x = round(start * RATE), round(end * RATE), round(CROSSFADE * RATE)
    if a < x or b > len(left):
        sys.exit(f"loop {start}-{end} s does not fit in the track")
    out = []
    for ch in (left, right):
        loop = ch[a:b]
        n = len(loop)
        for k in range(x):
            t = (k + 0.5) / x
            fade_out, fade_in = math.cos(t * math.pi / 2), math.sin(t * math.pi / 2)
            # The track running on towards `end`, and the music just before `start`.
            loop[n - x + k] = fade_out * ch[b - x + k] + fade_in * ch[a - x + k]
        out.append(loop)
    return out


def k_weight(x):
    """BS.1770-4 K-weighting at 48 kHz: a high shelf, then a high-pass."""
    y = array("f", bytes(4 * len(x)))
    for (b0, b1, b2), (a1, a2) in (
        ((1.53512485958697, -2.69169618940638, 1.19839281085285), (-1.69065929318241, 0.73248077421585)),
        ((1.0, -2.0, 1.0), (-1.99004745483398, 0.99007225036621)),
    ):
        x1 = x2 = y1 = y2 = 0.0
        for i, v in enumerate(x):
            out = b0 * v + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2, x1, y2, y1 = x1, v, y1, out
            y[i] = out
        x = y
        y = array("f", bytes(4 * len(x)))
    return x


def loudness(channels, scale=32768.0):
    """Integrated loudness in LUFS (BS.1770-4: 400 ms blocks every 100 ms, gated)."""
    step = RATE // 10
    chunks = None
    for ch in channels:
        k = k_weight(ch)
        sums = [sum(v * v for v in k[i:i + step]) for i in range(0, len(k) - step + 1, step)]
        chunks = sums if chunks is None else [p + q for p, q in zip(chunks, sums)]
    blocks = [sum(chunks[i:i + 4]) / (4 * step) / scale ** 2 for i in range(len(chunks) - 3)]
    lufs = [-0.691 + 10 * math.log10(p + 1e-20) for p in blocks]
    kept = [p for p, l in zip(blocks, lufs) if l > -70]
    gate = -0.691 + 10 * math.log10(sum(kept) / len(kept)) - 10
    kept = [p for p, l in zip(blocks, lufs) if l > -70 and l > gate]
    return -0.691 + 10 * math.log10(sum(kept) / len(kept))


def peak_db(channels, scale=32768.0):
    return 20 * math.log10(max(max(max(ch), -min(ch)) for ch in channels) / scale + 1e-20)


def write_ogg(name, channels, folder):
    gain = 10 ** ((TARGET_LUFS - loudness(channels)) / 20)
    left, right = (array("h", (max(-32767, min(32767, round(v * gain))) for v in ch)) for ch in channels)
    pcm = array("h", bytes(4 * len(left)))
    pcm[0::2], pcm[1::2] = left, right
    if sys.byteorder == "big":
        pcm.byteswap()
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f"{name}.wav"
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        serial = str(zlib.crc32(name.encode()) & 0x7FFFFFFF)  # fixed, so every run gives the same file
        subprocess.run(["oggenc", "-Q", "-q", QUALITY, "--serial", serial,
                        "-o", str(folder / f"{name}.ogg"), str(wav)], check=True)
    print(f"  {name + '.ogg':22} {len(left) / RATE:6.2f} s, gain {20 * math.log10(gain):+.1f} dB")


def check(folder):
    """Decode every music loop in the app and check the set, the lengths, the loudness and the peaks."""
    problems = []
    expected = {name: round(end * RATE) - round(start * RATE) for name, start, end in LOOPS}
    found = sorted(p.stem for p in folder.glob("music_*.ogg"))
    for name in sorted(set(expected) - set(found)):
        problems.append(f"{name}.ogg is missing")
    for name in sorted(set(found) - set(expected)):
        problems.append(f"{name}.ogg is not in this script's list")
    print(f"  {'file':22} {'length':>8} {'loudness':>10} {'peak':>10}")
    with tempfile.TemporaryDirectory() as tmp:
        for name in found:
            wav = Path(tmp) / f"{name}.wav"
            subprocess.run(["oggdec", "-Q", "-o", str(wav), str(folder / f"{name}.ogg")], check=True)
            channels = read_wav(wav)
            n, lufs, peak = len(channels[0]), loudness(channels), peak_db(channels)
            print(f"  {name + '.ogg':22} {n / RATE:6.2f} s {lufs:5.1f} LUFS {peak:5.1f} dBFS")
            if name in expected and abs(n - expected[name]) > RATE // 1000:
                problems.append(f"{name}.ogg is {n} samples long, not {expected[name]}")
            if abs(lufs - TARGET_LUFS) > 0.5:
                problems.append(f"{name}.ogg is at {lufs:.1f} LUFS, not {TARGET_LUFS:.0f}")
            if peak > MAX_PEAK_DB:
                problems.append(f"{name}.ogg peaks at {peak:.1f} dBFS")
    for p in problems:
        print("PROBLEM: " + p)
    return not problems


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--sources", help="folder with the owner's music_*.wav")
    ap.add_argument("--check", action="store_true", help="only check the files in the app")
    ap.add_argument("--out", default=str(OUT), help="where to write the .ogg files")
    args = ap.parse_args()
    out = Path(args.out)
    for tool in ("oggenc", "oggdec"):
        if not shutil.which(tool):
            sys.exit(f"{tool} not found; install vorbis-tools")
    if args.check:
        sys.exit(0 if check(out) else 1)
    if not args.sources:
        sys.exit("give --sources, the folder with music_*.wav, or --check")
    out.mkdir(parents=True, exist_ok=True)
    print(f"Writing to {out}")
    for name, start, end in LOOPS:
        write_ogg(name, cut_loop(*read_wav(Path(args.sources) / f"{name}.wav"), start, end), out)


if __name__ == "__main__":
    main()
