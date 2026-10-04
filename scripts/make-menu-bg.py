#!/usr/bin/env python3
"""
Фон правой части панели ElytrixClient (область карточек 350×350 GUI-единиц).

Свой рисунок под стиль клиента: графитовый градиент, мягкое розовое и
бело-розовое свечение по углам, едва заметная «хакерская» сетка точек и
два повёрнутых скруглённых квадрата-контура. Правые углы уже скруглены
(радиус 8 GUI-единиц), левый край прямой — к нему примыкает сайдбар.

Без PIL: PNG собирается вручную (zlib + struct), как и остальные скрипты.

    python scripts/make-menu-bg.py
"""
import math
import os
import random
import struct
import zlib

S = 3                      # пикселей на GUI-единицу (350 → 1050 px)
W = H = 350 * S
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "elytrixclient", "textures", "gui", "menu_bg_0.png")

TOP = (0x16, 0x13, 0x1B)
BOTTOM = (0x0C, 0x0A, 0x0F)
PINK = (0xFF, 0x4F, 0xC3)
BLUSH = (0xFF, 0xD6, 0xEE)
EDGE_A = (0x2B, 0x23, 0x32)
EDGE_B = (0x3D, 0x24, 0x45)


def clamp(v, a=0.0, b=1.0):
    return a if v < a else (b if v > b else v)


def mix(c1, c2, t):
    return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(3))


def over(dst, src, a):
    return tuple(dst[i] + (src[i] - dst[i]) * a for i in range(3))


def rdiamond(px, py, cx, cy, half, r):
    """SDF скруглённого квадрата, повёрнутого на 45°."""
    x, y = px - cx, py - cy
    k = math.sqrt(0.5)
    qx, qy = abs((x + y) * k), abs((y - x) * k)
    ax, ay = qx - half + r, qy - half + r
    outside = math.hypot(max(ax, 0.0), max(ay, 0.0))
    inside = min(max(ax, ay), 0.0)
    return outside + inside - r


def corner_mask(px, py):
    """Скругление правых углов (радиус 8 GUI)."""
    r = 8 * S
    if px > W - r and py < r:
        d = math.hypot(px - (W - r), py - r) - r
    elif px > W - r and py > H - r:
        d = math.hypot(px - (W - r), py - (H - r)) - r
    else:
        return 1.0
    return clamp(0.5 - d)


def main():
    rnd = random.Random(7)
    big = (250 * S, 238 * S, 150 * S, 22 * S)      # cx, cy, half, radius
    small = (300 * S, 214 * S, 72 * S, 12 * S)
    thick = 12 * S
    glow_a = (-30 * S, 380 * S, 230 * S)          # розовое свечение слева снизу
    glow_b = (380 * S, -20 * S, 200 * S)          # бело-розовое справа сверху
    grid = 14 * S

    raw = bytearray()
    for y in range(H):
        raw.append(0)
        for x in range(W):
            t = clamp((x * 0.35 + y) / (W * 0.35 + H))
            c = mix(TOP, BOTTOM, t)

            d = math.hypot(x - glow_a[0], y - glow_a[1]) / glow_a[2]
            c = over(c, PINK, 0.085 * clamp(1 - d) ** 2)
            d = math.hypot(x - glow_b[0], y - glow_b[1]) / glow_b[2]
            c = over(c, BLUSH, 0.05 * clamp(1 - d) ** 2)

            # сетка точек — очень тихая, только намёк на «терминал»
            gx, gy = x % grid - grid / 2, y % grid - grid / 2
            dot = clamp(0.5 - (math.hypot(gx, gy) - 0.9 * S))
            c = over(c, (255, 255, 255), 0.03 * dot)

            # большой контур: толстая рамка с градиентом вдоль диагонали
            sd = rdiamond(x, y, *big)
            ring = clamp(0.5 - (abs(sd + thick / 2) - thick / 2))
            if ring > 0:
                g = clamp((x + y) / (W + H) * 1.4 - 0.2)
                c = over(c, mix(EDGE_A, EDGE_B, g), 0.6 * ring)
            # внутренний тонкий розовый штрих
            sd2 = rdiamond(x, y, *small)
            line = clamp(0.5 - (abs(sd2) - 0.6 * S))
            if line > 0:
                c = over(c, PINK, 0.16 * line)

            n = (rnd.random() - 0.5) * 1.2          # дизеринг против полос
            a = int(round(255 * corner_mask(x, y)))
            raw += bytes((int(clamp(c[0] + n, 0, 255)), int(clamp(c[1] + n, 0, 255)),
                          int(clamp(c[2] + n, 0, 255)), a))

    def chunk(typ, body):
        return (struct.pack(">I", len(body)) + typ + body
                + struct.pack(">I", zlib.crc32(typ + body) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as f:
        f.write(png)
    print(OUT, W, "x", H, len(png) // 1024, "KB")


if __name__ == "__main__":
    main()
