#!/usr/bin/env python3
"""Генератор спрайтов-форм ElytrixClient (для сглаженного GUI без шейдеров).

Проблема: скруглённые углы, круги и тени, нарисованные прямоугольниками
(`fill`), дают «лестницу» на краях — отсюда ощущение пиксельного интерфейса.
Здесь формы считаются как SDF (расстояние до фигуры) и покрываются по альфе:
край получается сглаженным. В игре такие спрайты растягиваются по 9 частям
(углы не растягиваются, края — да), поэтому сглаживание сохраняется на любом
размере панели.

Без PIL: PNG собирается вручную (zlib + struct).

Запуск:  python scripts/make-ui-shapes.py
Куда:    src/main/resources/assets/elytrixclient/textures/gui/shapes/
"""
import math
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets",
                   "elytrixclient", "textures", "gui", "shapes")

# Игра рисует GUI в «единицах интерфейса» и умножает их на GUI Scale. Для каждого
# масштаба держим свой набор спрайтов (_x1.._x4): тогда сглаженный край ложится
# ровно в пиксели и не превращается в ступеньки.
SCALES = [1, 2, 3, 4]
# радиусы скруглений, на которые есть спрайты (код выбирает ближайший)
RADII = [2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 24, 28]
# диаметры круглых спрайтов (код выбирает ближайший, поэтому масштаб почти 1:1)
DISC_SIZES = [4, 6, 8, 10, 12, 16, 20, 24, 32, 48, 64]


# ────────────────────────────────────────────────────────────── SDF
def rrect(cx, cy, hw, hh, r):
    """Расстояние до скруглённого прямоугольника (центр cx,cy, полуразмеры hw,hh)."""
    def f(x, y):
        dx = abs(x - cx) - (hw - r)
        dy = abs(y - cy) - (hh - r)
        outside = math.hypot(max(dx, 0.0), max(dy, 0.0))
        return outside + min(max(dx, dy), 0.0) - r
    return f


def disc(cx, cy, r):
    return lambda x, y: math.hypot(x - cx, y - cy) - r


# ────────────────────────────────────────────────────────────── PNG
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


def blank(w, h):
    return bytearray(w * h * 4)


def put(px, w, x, y, alpha):
    i = (y * w + x) * 4
    px[i] = 255
    px[i + 1] = 255
    px[i + 2] = 255
    px[i + 3] = alpha


# ────────────────────────────────────────────────────────────── формы
def render_round(r, k=1):
    """Скруглённый прямоугольник r+углы: спрайт (2r+2)x(2r+2), углы — дуги r x r."""
    s = (2 * r + 2) * k
    sdf = rrect(s / 2.0, s / 2.0, s / 2.0, s / 2.0, float(r * k))
    px = blank(s, s)
    for y in range(s):
        for x in range(s):
            d = sdf(x + 0.5, y + 0.5)
            cov = max(0.0, min(1.0, 0.5 - d))
            put(px, s, x, y, int(round(cov * 255)))
    return s, s, px


def render_shadow(k=1):
    """Мягкая тень: скруглённый прямоугольник с размытым краем (falloff)."""
    s = 64 * k
    pad = 12.0 * k      # отступ формы от края спрайта = ширина размытия
    radius = 18.0 * k
    half = s / 2.0 - pad
    sdf = rrect(s / 2.0, s / 2.0, half, half, radius)
    px = blank(s, s)
    for y in range(s):
        for x in range(s):
            d = sdf(x + 0.5, y + 0.5)
            if d <= 0:
                a = 1.0
            else:
                t = max(0.0, 1.0 - d / pad)
                a = t * t * t          # мягкий, «гауссов»-похожий спад
            put(px, s, x, y, int(round(max(0.0, min(1.0, a)) * 255)))
    return s, s, px


def render_disc(s):
    """Круг со сглаженным краем (для точек, огоньков, концов полос)."""
    sdf = disc(s / 2.0, s / 2.0, s / 2.0 - 0.5)  # s уже в пикселях нужного масштаба
    px = blank(s, s)
    for y in range(s):
        for x in range(s):
            d = sdf(x + 0.5, y + 0.5)
            cov = max(0.0, min(1.0, 0.5 - d))
            put(px, s, x, y, int(round(cov * 255)))
    return s, s, px


# ────────────────────────────────────────────────────────────── логотип
LOGO_SRC = os.path.join(ROOT, "src", "main", "resources", "assets",
                        "elytrixclient", "textures", "gui", "logo.png")
LOGO_UNITS = 88          # размер, в котором логотип рисуется на экране загрузки


def read_png(path):
    """Минимальный декодер PNG (8 бит, RGB/RGBA, фильтры 0..4)."""
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos, w, h, ctype, idat = 8, 0, 0, 6, bytearray()
    while pos < len(data):
        ln = struct.unpack(">I", data[pos:pos + 4])[0]
        typ = data[pos + 4:pos + 8]
        body = data[pos + 8:pos + 8 + ln]
        if typ == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", body[:10])
            assert depth == 8 and ctype in (2, 6), f"ожидался 8-бит RGB/RGBA, а тут {depth}/{ctype}"
        elif typ == b"IDAT":
            idat += body
        pos += 12 + ln
    ch = 4 if ctype == 6 else 3
    raw = zlib.decompress(bytes(idat))
    stride = w * ch
    out = bytearray(w * h * 4)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if f == 1:
            for i in range(ch, stride):
                line[i] = (line[i] + line[i - ch]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i - ch] if i >= ch else 0
                b = prev[i]
                c = prev[i - ch] if i >= ch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        for x in range(w):
            i = (y * w + x) * 4
            if ch == 4:
                out[i:i + 4] = line[x * 4:x * 4 + 4]
            else:
                out[i] = line[x * 3]
                out[i + 1] = line[x * 3 + 1]
                out[i + 2] = line[x * 3 + 2]
                out[i + 3] = 255
        prev = line
    return w, h, out


def resample(px, w, h, nw, nh):
    """Усреднение по площади (с учётом альфы), чтобы не было тёмной каймы."""
    out = bytearray(nw * nh * 4)
    for y in range(nh):
        y0, y1 = y * h / nh, (y + 1) * h / nh
        for x in range(nw):
            x0, x1 = x * w / nw, (x + 1) * w / nw
            rs = gs = bs = asum = wsum = 0.0
            for sy in range(int(y0), min(h, max(int(y0) + 1, math.ceil(y1)))):
                for sx in range(int(x0), min(w, max(int(x0) + 1, math.ceil(x1)))):
                    i = (sy * w + sx) * 4
                    a = px[i + 3] / 255.0
                    rs += px[i] * a
                    gs += px[i + 1] * a
                    bs += px[i + 2] * a
                    asum += a
                    wsum += 1.0
            i = (y * nw + x) * 4
            if asum > 0:
                out[i] = int(round(rs / asum))
                out[i + 1] = int(round(gs / asum))
                out[i + 2] = int(round(bs / asum))
            out[i + 3] = int(round(asum / max(1.0, wsum) * 255))
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    made = []

    for k in SCALES:
        for r in RADII:
            w, h, px = render_round(r, k)
            name = f"round_{r}_x{k}.png"
            write_png(os.path.join(OUT, name), w, h, px)
            made.append(name)

        w, h, px = render_shadow(k)
        name = f"shadow_x{k}.png"
        write_png(os.path.join(OUT, name), w, h, px)
        made.append(name)

        # круги: рисуем 1:1 (в пикселях текущего масштаба), без растягивания текстуры
        for d in DISC_SIZES:
            w, h, px = render_disc(d * k)
            name = f"disc_{d}_x{k}.png"
            write_png(os.path.join(OUT, name), w, h, px)
            made.append(name)

    # логотип экрана загрузки — под каждый масштаб, чтобы не мылить/не пикселить
    if os.path.exists(LOGO_SRC):
        w, h, px = read_png(LOGO_SRC)
        for k in SCALES:
            size = LOGO_UNITS * k
            out = resample(px, w, h, size, size)
            name = f"logo_{LOGO_UNITS}_x{k}.png"
            write_png(os.path.join(OUT, name), size, size, out)
            made.append(f"{name} ({size}x{size})")
    else:
        print("!! нет", LOGO_SRC, "- логотип пропущен")

    print(f"Формы UI -> {os.path.relpath(OUT, ROOT)}")
    print(f"Готово: {len(made)} спрайтов (примеры: {made[0]}, {made[-1]}).")


if __name__ == "__main__":
    main()
