#!/usr/bin/env python3
"""Сборка текстур ElytrixClient из мастер-картинок (art/*.png).

Чистый Python: свой PNG-кодек (zlib + фильтры), box-ресайз с учётом альфы,
удаление фона (flood fill для иконки, luminance-key для логотипа).

Запуск:  python scripts/make-textures.py
Результат: src/main/resources/{assets,resourcepacks}/...
"""
import os
import struct
import zlib
from collections import deque

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ART = os.path.join(ROOT, "art")
ASSETS = os.path.join(ROOT, "src/main/resources/assets/elytrixclient")
PACK = os.path.join(ROOT, "src/main/resources/resourcepacks/elytrixclient")


# --------------------------------------------------------------------- PNG IO
def read_png(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a png"
    pos, w, h, bitdepth, color, raw = 8, 0, 0, 0, 0, b""
    idat = b""
    while pos < len(data):
        (ln,) = struct.unpack(">I", data[pos:pos + 4])
        typ = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b"IHDR":
            w, h, bitdepth, color, comp, filt, interlace = struct.unpack(">IIBBBBB", chunk)
            assert bitdepth == 8 and interlace == 0, "only 8-bit non-interlaced"
        elif typ == b"IDAT":
            idat += chunk
        elif typ == b"IEND":
            break
    raw = zlib.decompress(idat)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color]
    stride = w * channels
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]
        p += 1
        line = bytearray(raw[p:p + stride])
        p += stride
        if f == 1:
            for i in range(channels, stride):
                line[i] = (line[i] + line[i - channels]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i - channels] if i >= channels else 0
                b = prev[i]
                c = prev[i - channels] if i >= channels else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out[y * stride:(y + 1) * stride] = line
        prev = line
    # -> RGBA
    px = bytearray(w * h * 4)
    for i in range(w * h):
        if channels == 4:
            px[i * 4:i * 4 + 4] = out[i * 4:i * 4 + 4]
        elif channels == 3:
            px[i * 4:i * 4 + 3] = out[i * 3:i * 3 + 3]
            px[i * 4 + 3] = 255
        elif channels == 2:  # gray+alpha
            g, a = out[i * 2], out[i * 2 + 1]
            px[i * 4:i * 4 + 4] = bytes((g, g, g, a))
        else:  # gray
            g = out[i]
            px[i * 4:i * 4 + 4] = bytes((g, g, g, 255))
    return w, h, px


def write_png(path, w, h, px):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = bytearray()
    stride = w * 4
    for y in range(h):
        raw.append(0)
        raw += px[y * stride:(y + 1) * stride]

    def chunk(typ, body):
        return (struct.pack(">I", len(body)) + typ + body
                + struct.pack(">I", zlib.crc32(typ + body) & 0xFFFFFFFF))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)
    print("  ->", os.path.relpath(path, ROOT), f"{w}x{h}", f"{len(png)} B")


# ------------------------------------------------------------------- операции
def key_corner_background(w, h, px, tol=26):
    """Убрать белый фон: заливка от краёв по «почти белым» пикселям."""
    def is_bg(i):
        r, g, b = px[i * 4], px[i * 4 + 1], px[i * 4 + 2]
        return r > 255 - tol and g > 255 - tol and b > 255 - tol

    seen = bytearray(w * h)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            i = y * w + x
            if is_bg(i) and not seen[i]:
                seen[i] = 1
                q.append(i)
    for y in range(h):
        for x in (0, w - 1):
            i = y * w + x
            if is_bg(i) and not seen[i]:
                seen[i] = 1
                q.append(i)
    while q:
        i = q.popleft()
        px[i * 4 + 3] = 0
        x, y = i % w, i // w
        for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
            if 0 <= nx < w and 0 <= ny < h:
                j = ny * w + nx
                if not seen[j] and is_bg(j):
                    seen[j] = 1
                    q.append(j)
    return px


def key_dark_background(w, h, px, lo=22, hi=76):
    """Чёрный фон -> прозрачность: alpha = clamp((яркость-lo)/(hi-lo))."""
    for i in range(w * h):
        r, g, b = px[i * 4], px[i * 4 + 1], px[i * 4 + 2]
        lum = max(r, g, b)
        a = int(max(0.0, min(1.0, (lum - lo) / float(hi - lo))) * 255)
        px[i * 4 + 3] = min(px[i * 4 + 3], a)


def restore_outline(w, h, px, steps=2, color=(14, 10, 26), alpha=230):
    """Вернуть тёмный контур буквам: дилатация маски + заливка тёмным."""
    mask = [1 if px[i * 4 + 3] > 60 else 0 for i in range(w * h)]
    for _ in range(steps):
        nm = mask[:]
        for y in range(h):
            for x in range(w):
                i = y * w + x
                if mask[i]:
                    continue
                for nx, ny in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)):
                    if 0 <= nx < w and 0 <= ny < h and mask[ny * w + nx]:
                        nm[i] = 1
                        break
        mask = nm
    for i in range(w * h):
        if mask[i] and px[i * 4 + 3] < 215:
            px[i * 4], px[i * 4 + 1], px[i * 4 + 2], px[i * 4 + 3] = color[0], color[1], color[2], alpha


def resize(w, h, px, nw, nh):
    """Box-ресайз (усреднение по площади) с премультипликацией альфы."""
    out = bytearray(nw * nh * 4)
    for y in range(nh):
        y0, y1 = y * h // nh, max(y * h // nh + 1, (y + 1) * h // nh)
        for x in range(nw):
            x0, x1 = x * w // nw, max(x * w // nw + 1, (x + 1) * w // nw)
            rs = gs = bs = as_ = n = 0
            for yy in range(y0, y1):
                base = yy * w
                for xx in range(x0, x1):
                    i = (base + xx) * 4
                    a = px[i + 3]
                    rs += px[i] * a
                    gs += px[i + 1] * a
                    bs += px[i + 2] * a
                    as_ += a
                    n += 1
            o = (y * nw + x) * 4
            if as_ == 0:
                out[o:o + 4] = b"\x00\x00\x00\x00"
            else:
                out[o] = min(255, rs // as_)
                out[o + 1] = min(255, gs // as_)
                out[o + 2] = min(255, bs // as_)
                out[o + 3] = min(255, as_ // n)
    return nw, nh, out


def bbox_alpha(w, h, px, thr=8):
    xs, ys = [], []
    for y in range(h):
        for x in range(w):
            if px[(y * w + x) * 4 + 3] > thr:
                xs.append(x)
                ys.append(y)
    if not xs:
        return 0, 0, w, h
    return min(xs), min(ys), max(xs) + 1, max(ys) + 1


def crop(w, h, px, box):
    x0, y0, x1, y1 = box
    cw, ch = x1 - x0, y1 - y0
    out = bytearray(cw * ch * 4)
    for y in range(ch):
        src = ((y + y0) * w + x0) * 4
        out[y * cw * 4:(y + 1) * cw * 4] = px[src:src + cw * 4]
    return cw, ch, out


def blit(dw, dh, dst, sw, sh, src, dx, dy):
    for y in range(sh):
        if not (0 <= dy + y < dh):
            continue
        row = (y * sw) * 4
        drows = ((dy + y) * dw + dx) * 4
        dst[drows:drows + sw * 4] = src[row:row + sw * 4]
    return dst


def fit_into(sw, sh, src, max_w, max_h):
    """Вписать картинку в рамку с сохранением пропорций (без апскейла выше рамки)."""
    scale = min(max_w / float(sw), max_h / float(sh))
    nw, nh = max(1, int(round(sw * scale))), max(1, int(round(sh * scale)))
    return resize(sw, sh, src, nw, nh)


# ----------------------------------------------------------------------- main
def main():
    print("Иконка клиента (icon-master.png):")
    w, h, px = read_png(os.path.join(ART, "icon-master.png"))
    px = key_corner_background(w, h, px)
    for size, name in ((128, "icon.png"), (256, "window_icon_256.png"),
                       (128, "window_icon_128.png"), (64, "window_icon_64.png"),
                       (32, "window_icon_32.png"), (16, "window_icon_16.png")):
        nw, nh, out = resize(w, h, px, size, size)
        write_png(os.path.join(ASSETS, name), nw, nh, out)

    # Тот же значок 256x256, но под textures/ — его рисует экран загрузки
    # (GuiGraphicsExtractor.blit по Identifier).
    nw, nh, out = resize(w, h, px, 256, 256)
    write_png(os.path.join(ASSETS, "textures/gui/logo.png"), nw, nh, out)
    print("Готово.")


if __name__ == "__main__":
    main()
