#!/usr/bin/env python3
"""Генератор иконок ElytrixClient (16x16, белые, с прозрачным фоном).

Каждая иконка описывается набором SDF-примитивов (функций «расстояние до фигуры»),
поэтому края получаются сглаженными без PIL и без внешних картинок.
Цвет иконки задаётся уже в игре — при отрисовке текстура тонируется (blit с color).

Запуск:  python scripts/generate-icons.py
Результат: src/main/resources/assets/elytrixclient/textures/gui/icons/*.png
"""
import math
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "elytrixclient", "textures", "gui", "icons")
SIZE = 16


# ────────────────────────────────────────────────────────────── SDF-примитивы
def seg(ax, ay, bx, by, w):
    vx, vy = bx - ax, by - ay
    l2 = vx * vx + vy * vy

    def f(x, y):
        wx, wy = x - ax, y - ay
        t = 0.0 if l2 == 0 else max(0.0, min(1.0, (wx * vx + wy * vy) / l2))
        return math.hypot(wx - t * vx, wy - t * vy) - w / 2.0
    return f


def poly(points):
    n = len(points)

    def f(x, y):
        d = 1e9
        inside = False
        for i in range(n):
            ax, ay = points[i]
            bx, by = points[(i + 1) % n]
            d = min(d, seg(ax, ay, bx, by, 0.0)(x, y))
            if (ay > y) != (by > y) and x < (bx - ax) * (y - ay) / (by - ay) + ax:
                inside = not inside
        return -d if inside else d
    return f


def disc(cx, cy, r):
    return lambda x, y: math.hypot(x - cx, y - cy) - r


def ring(cx, cy, r, w):
    return lambda x, y: abs(math.hypot(x - cx, y - cy) - r) - w / 2.0


def rrect(cx, cy, hw, hh, r):
    def f(x, y):
        dx = abs(x - cx) - (hw - r)
        dy = abs(y - cy) - (hh - r)
        outside = math.hypot(max(dx, 0.0), max(dy, 0.0))
        return outside + min(max(dx, dy), 0.0) - r
    return f


def rrect_ring(cx, cy, hw, hh, r, w):
    inner = rrect(cx, cy, hw, hh, r)
    return lambda x, y: abs(inner(x, y)) - w / 2.0


def union(*fns):
    return lambda x, y: min(fn(x, y) for fn in fns)


def diff(a, b):
    return lambda x, y: max(a(x, y), -b(x, y))


def half_top(cy):
    """Полуплоскость y < cy (для дуг wifi и т.п.)."""
    return lambda x, y: y - cy


def intersect(a, b):
    return lambda x, y: max(a(x, y), b(x, y))


# ────────────────────────────────────────────────────────────── иконки
ICONS = {
    "home": union(
        seg(2.6, 8.2, 8, 2.6, 2.2), seg(8, 2.6, 13.4, 8.2, 2.2),
        rrect(8, 10.4, 4.6, 3.6, 1.4),
    ),
    "bots": diff(
        union(
            rrect(8, 7.2, 4.2, 3.4, 1.6),
            seg(8, 3.6, 8, 4.4, 1.6), disc(8, 2.9, 1.1),
            rrect(8, 12.6, 3.4, 2.5, 1.2),
        ),
        union(disc(6.7, 7.0, 1.05), disc(9.3, 7.0, 1.05)),
    ),
    "proxy": union(
        ring(8, 8, 5.6, 1.4),
        seg(2.4, 8, 13.6, 8, 1.4),
        seg(5.9, 3.4, 5.9, 12.6, 1.2),
        seg(10.1, 3.4, 10.1, 12.6, 1.2),
    ),
    "console": union(
        rrect_ring(8, 8, 5.7, 4.7, 1.7, 1.4),
        seg(4.4, 6.0, 6.3, 8.0, 1.35), seg(6.3, 8.0, 4.4, 10.0, 1.35),
        seg(7.7, 10.4, 11.4, 10.4, 1.35),
    ),
    "settings": union(
        ring(8, 8, 3.2, 2.0),
        *[rrect(8 + 5.1 * math.cos(a), 8 + 5.1 * math.sin(a), 1.2, 1.2, 0.55)
          for a in [math.pi * i / 4 for i in range(8)]],
    ),
    "user": union(disc(8, 5.6, 2.7), rrect(8, 11.8, 4.1, 3.0, 1.8)),
    "list": union(
        seg(5.6, 4.6, 12.6, 4.6, 1.5), seg(5.6, 8.0, 12.6, 8.0, 1.5),
        seg(5.6, 11.4, 12.6, 11.4, 1.5),
        disc(3.3, 4.6, 0.95), disc(3.3, 8.0, 0.95), disc(3.3, 11.4, 0.95),
    ),
    "server": union(
        rrect(8, 5.4, 5.6, 2.6, 0.9), rrect(8, 10.6, 5.6, 2.6, 0.9),
        disc(4.2, 5.4, 0.85), disc(4.2, 10.6, 0.85),
    ),
    "shield": poly([(8, 2.0), (13.4, 4.2), (12.6, 10.0), (8, 14.0), (3.4, 10.0), (2.6, 4.2)]),
    "folder": union(rrect(8, 10.0, 6.0, 4.2, 1.3), rrect(5.8, 5.4, 2.7, 1.3, 0.6)),
    "chart": union(
        rrect(4.6, 10.6, 1.25, 2.6, 0.5), rrect(8.0, 9.0, 1.25, 4.2, 0.5),
        rrect(11.4, 6.6, 1.25, 6.6, 0.5), seg(2.8, 13.4, 13.2, 13.4, 1.2),
    ),
    "key": union(
        ring(5.6, 5.6, 2.5, 1.5), seg(7.5, 7.5, 12.8, 12.8, 1.6),
        seg(10.4, 10.4, 12.2, 8.6, 1.5),
    ),
    "globe": union(
        ring(8, 8, 5.7, 1.4), seg(2.6, 8, 13.4, 8, 1.3),
        intersect(ring(8, 8, 3.0, 1.3), half_top(8.0)),
        lambda x, y: abs(abs(x - 8) + abs(y - 8) - 6.0) - 1.2,
    ),
    "clock": union(
        ring(8, 8, 5.7, 1.5), seg(8, 4.9, 8, 8.4, 1.4), seg(8, 8.4, 10.9, 9.9, 1.4),
    ),
    "wifi": union(
        intersect(ring(8, 13.0, 10.0, 1.5), half_top(13.0)),
        intersect(ring(8, 13.0, 6.4, 1.5), half_top(13.0)),
        intersect(ring(8, 13.0, 3.0, 1.5), half_top(13.0)),
        disc(8, 12.4, 1.15),
    ),
    "search": union(ring(7.0, 7.0, 3.5, 1.6), seg(9.6, 9.6, 12.8, 12.8, 1.7)),
    "plus": union(seg(8, 4.0, 8, 12.0, 1.8), seg(4.0, 8, 12.0, 8, 1.8)),
    "minus": seg(4.0, 8, 12.0, 8, 1.8),
    "dots": union(disc(4.0, 8, 1.1), disc(8.0, 8, 1.1), disc(12.0, 8, 1.1)),
    "bolt": union(
        seg(9.2, 2.4, 6.4, 8.6, 1.6), seg(6.4, 8.6, 9.8, 8.0, 1.6),
        seg(9.8, 8.0, 7.2, 13.6, 1.6),
    ),
    "play": poly([(5.0, 3.4), (12.6, 8.0), (5.0, 12.6)]),
    "stop": rrect(8, 8, 3.4, 3.4, 1.0),
    "refresh": union(
        diff(ring(8, 8, 5.0, 1.5), disc(12.3, 3.9, 2.7)),
        poly([(12.6, 2.2), (14.4, 5.6), (10.8, 5.9)]),
    ),
    "close": union(seg(4.6, 4.6, 11.4, 11.4, 1.6), seg(11.4, 4.6, 4.6, 11.4, 1.6)),
    "chevron": union(seg(5.0, 6.4, 8.0, 9.6, 1.6), seg(8.0, 9.6, 11.0, 6.4, 1.6)),
    "check": union(seg(4.0, 8.4, 6.6, 11.0, 1.8), seg(6.6, 11.0, 12.0, 5.2, 1.8)),
    "trash": union(
        rrect_ring(8, 9.4, 4.0, 4.5, 1.0, 1.3),
        seg(4.2, 4.9, 11.8, 4.9, 1.3), rrect(8, 3.4, 1.6, 0.9, 0.4),
    ),
    "copy": union(rrect_ring(6.0, 6.0, 3.6, 3.6, 1.0, 1.3), rrect_ring(10.0, 10.0, 3.6, 3.6, 1.0, 1.3)),
}


# ────────────────────────────────────────────────────────────── растеризация
def write_png(path, w, h, px):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        raw += px[y * w * 4:(y + 1) * w * 4]

    def chunk(typ, body):
        return (struct.pack(">I", len(body)) + typ + body
                + struct.pack(">I", zlib.crc32(typ + body) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def render(sdf, size=SIZE):
    px = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            d = sdf(x + 0.5, y + 0.5)
            cov = max(0.0, min(1.0, 0.5 - d))
            a = int(round(cov * 255))
            i = (y * size + x) * 4
            px[i] = 255
            px[i + 1] = 255
            px[i + 2] = 255
            px[i + 3] = a
    return px


# Игра рисует GUI в «единицах интерфейса» и увеличивает их на GUI Scale.
# Чтобы сглаженные края не превращались в ступеньки из квадратов, держим
# варианты иконки под каждый масштаб: _x1 = 16px, _x2 = 32px, _x3 = 48px, _x4 = 64px.
SCALES = [1, 2, 3, 4]


def main():
    print(f"Иконки {SIZE}x{SIZE} (варианты x1..x4) -> {os.path.relpath(OUT, ROOT)}")
    total = 0
    for name, sdf in sorted(ICONS.items()):
        for k in SCALES:
            size = SIZE * k
            px = render(sdf, size)
            write_png(os.path.join(OUT, f"{name}_x{k}.png"), size, size, px)
            total += 1
    print(f"Готово: {len(ICONS)} иконок x {len(SCALES)} масштаба = {total} файлов.")


if __name__ == "__main__":
    main()
