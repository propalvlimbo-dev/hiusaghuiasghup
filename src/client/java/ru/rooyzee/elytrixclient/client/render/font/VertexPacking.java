package ru.rooyzee.elytrixclient.client.render.font;

/**
 * Утилиты упаковки вершинных атрибутов.
 * Портировано из delta-26.2 VertexPacking.
 */
final class VertexPacking {

    static final float SIZE_SCALE = 8.0F;
    static final float RADIUS_SCALE = 16.0F;

    private VertexPacking() {}

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static int clampByte(int value) {
        return clamp(value, 0, 255);
    }

    static int packRadius(float px) {
        return clamp(Math.round(px * RADIUS_SCALE), 0, 32767);
    }

    static int packU8Pair(int lo, int hi) {
        return clampByte(lo) | (clampByte(hi) << 8);
    }
}