#!/usr/bin/env python3
"""Синтез звуков меню ElytrixClient (свои, без чужих сэмплов).

Два набора: soft («Мягкий» — тёплые синусы) и glass («Стеклянный» — FM-колокольчики).
События: click, hover, on, off, open, close.
Результат: src/main/resources/assets/elytrixclient/sounds/ui/<set>/<event>.ogg (Vorbis, моно 44.1 кГц).

Нужен ffmpeg с libvorbis:
    pip install --target /tmp/tools/py imageio-ffmpeg
    FFMPEG=$(ls /tmp/tools/py/imageio_ffmpeg/binaries/ffmpeg-*) python3 scripts/make-ui-sounds.py
"""
import math
import os
import random
import struct
import subprocess
import tempfile
import wave

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "elytrixclient", "sounds", "ui")
FFMPEG = os.environ.get("FFMPEG", "ffmpeg")


def silence(sec):
    return [0.0] * int(SR * sec)


def tone(f0, f1, dur, decay, amp=1.0, attack=0.003, harm=0.0):
    """Синус со скольжением частоты f0→f1 и экспоненциальным затуханием."""
    n = int(SR * dur)
    out = []
    ph = 0.0
    for i in range(n):
        t = i / SR
        f = f0 + (f1 - f0) * min(1.0, t / dur)
        ph += 2 * math.pi * f / SR
        env = math.exp(-t * decay) * min(1.0, t / attack)
        out.append(amp * env * (math.sin(ph) + harm * math.sin(2 * ph)))
    return out


def bell(f, dur, decay, amp=1.0, ratio=3.5, index=2.2):
    """FM-колокольчик: модулятор с затухающим индексом — «стеклянный» тембр."""
    n = int(SR * dur)
    out = []
    for i in range(n):
        t = i / SR
        env = math.exp(-t * decay) * min(1.0, t / 0.002)
        mod = index * math.exp(-t * decay * 1.8) * math.sin(2 * math.pi * f * ratio * t)
        out.append(amp * env * math.sin(2 * math.pi * f * t + mod))
    return out


def noise_swoosh(dur, rise=True, amp=0.35, seed=1):
    """Мягкий «вжух»: шум через однополюсный фильтр с плавающей частотой среза."""
    rnd = random.Random(seed)
    n = int(SR * dur)
    out = []
    y = 0.0
    for i in range(n):
        p = i / n
        fc = 500 + 3500 * (p if rise else 1 - p)
        a = 1 - math.exp(-2 * math.pi * fc / SR)
        y += a * (rnd.uniform(-1, 1) - y)
        env = math.sin(math.pi * p) ** 2
        out.append(amp * env * y)
    return out


def mix(*tracks, offsets=None):
    offsets = offsets or [0.0] * len(tracks)
    n = max(int(o * SR) + len(t) for t, o in zip(tracks, offsets))
    out = [0.0] * n
    for t, o in zip(tracks, offsets):
        s = int(o * SR)
        for i, v in enumerate(t):
            out[s + i] += v
    return out


def finish(samples, peak=0.55):
    m = max(1e-9, max(abs(v) for v in samples))
    k = peak / m
    fade = int(SR * 0.004)
    out = [v * k for v in samples]
    for i in range(min(fade, len(out))):
        out[-1 - i] *= i / fade
    return out + silence(0.01)


SETS = {
    "soft": {
        "click": lambda: mix(tone(1500, 950, 0.07, 55, harm=0.15), noise_swoosh(0.006, amp=0.2)),
        "hover": lambda: tone(2300, 2100, 0.035, 90, amp=0.6),
        "on": lambda: mix(tone(880, 880, 0.09, 40), tone(1320, 1320, 0.12, 35), offsets=[0, 0.055]),
        "off": lambda: mix(tone(1320, 1320, 0.09, 40), tone(880, 880, 0.12, 35), offsets=[0, 0.055]),
        "open": lambda: mix(noise_swoosh(0.16, True, 0.5), tone(660, 990, 0.18, 14, amp=0.5)),
        "close": lambda: mix(noise_swoosh(0.14, False, 0.5), tone(990, 560, 0.16, 16, amp=0.5)),
    },
    "glass": {
        "click": lambda: bell(2600, 0.12, 38),
        "hover": lambda: bell(3500, 0.05, 80, amp=0.5, index=1.2),
        "on": lambda: mix(bell(1568, 0.16, 26), bell(2349, 0.2, 22), offsets=[0, 0.06]),
        "off": lambda: mix(bell(2349, 0.16, 26), bell(1568, 0.2, 22), offsets=[0, 0.06]),
        "open": lambda: mix(bell(1047, 0.25, 14), bell(1319, 0.25, 14), bell(1568, 0.3, 12),
                            offsets=[0, 0.045, 0.09]),
        "close": lambda: mix(bell(1568, 0.22, 16), bell(1175, 0.26, 14), offsets=[0, 0.06]),
    },
}

# громкость, с которой событие звучит относительно остальных
PEAK = {"hover": 0.22, "click": 0.5, "on": 0.45, "off": 0.42, "open": 0.5, "close": 0.45}


def write_wav(path, samples):
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v)) * 32767)) for v in samples))


def main():
    count = 0
    with tempfile.TemporaryDirectory() as tmp:
        for set_name, events in SETS.items():
            os.makedirs(os.path.join(OUT, set_name), exist_ok=True)
            for ev, make in events.items():
                wav = os.path.join(tmp, f"{set_name}_{ev}.wav")
                write_wav(wav, finish(make(), PEAK[ev]))
                ogg = os.path.join(OUT, set_name, f"{ev}.ogg")
                subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-i", wav,
                                "-c:a", "libvorbis", "-q:a", "5", ogg], check=True)
                count += 1
    print(f"Звуки меню: {count} файлов -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
