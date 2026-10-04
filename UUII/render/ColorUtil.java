package wtf.expensive.client.util.render;

import java.awt.Color;

public final class ColorUtil {
    public static final int GREEN = rgba(36, 218, 118, 255);
    public static final int YELLOW = rgba(255, 196, 67, 255);
    public static final int ORANGE = rgba(255, 134, 0, 255);
    public static final int RED = rgba(239, 72, 54, 255);

    private ColorUtil() {
    }

    public static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getRed(int hex) {
        return (hex >> 16) & 0xFF;
    }

    public static int getGreen(int hex) {
        return (hex >> 8) & 0xFF;
    }

    public static int getBlue(int hex) {
        return hex & 0xFF;
    }

    public static int getAlpha(int hex) {
        return (hex >>> 24) & 0xFF;
    }

    public static int reAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int interpolate(int from, int to, float progress) {
        float inverse = 1f - progress;
        int a = (int) (getAlpha(from) * inverse + getAlpha(to) * progress);
        int r = (int) (getRed(from) * inverse + getRed(to) * progress);
        int g = (int) (getGreen(from) * inverse + getGreen(to) * progress);
        int b = (int) (getBlue(from) * inverse + getBlue(to) * progress);
        return rgba(r, g, b, a);
    }
    public static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360f * colors.length);
        if (colorIndex >= colors.length) {
            colorIndex = colors.length - 1;
        }
        int first = colors[colorIndex];
        int second = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolate(first, second, angle / 360f * colors.length - colorIndex);
    }

    public static int astolfo(int speed, int offset, float saturation, float brightness, float alpha) {
        long time = System.currentTimeMillis();
        float hue = ((time / speed + offset) % 360L) / 360f;
        return reAlpha(Color.HSBtoRGB(hue, saturation, brightness),
                Math.clamp((int) (alpha * 255f), 0, 255));
    }

    public static int fromHex(String hex) {
        return reAlpha(Integer.parseInt(hex.substring(1), 16), 255);
    }
}
