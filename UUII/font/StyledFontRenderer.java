package wtf.expensive.client.util.font;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import wtf.expensive.client.util.render.ColorUtil;

public final class StyledFontRenderer {
    private StyledFontRenderer() {
    }

    public static float width(Font font, Component text) {
        return font.getSplitter().stringWidth(text);
    }

    public static float width(Font font, String text, Style style) {
        return font.getSplitter().stringWidth(Component.literal(text).withStyle(style));
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, Component text,
                            float x, float y, int color) {
        graphics.text(font, text, Math.round(x), Math.round(y), color, false);
    }

    public static void drawCentered(GuiGraphicsExtractor graphics, Font font, Component text,
                                    float centerX, float y, int color) {
        draw(graphics, font, text, centerX - width(font, text) / 2f, y, color);
    }

    public static void drawGradient(GuiGraphicsExtractor graphics, Font font, String text, Style style,
                                    float x, float y, int firstColor, int secondColor) {
        if (text.isEmpty()) {
            return;
        }

        int[] codePoints = text.codePoints().toArray();
        float cursor = x;
        for (int i = 0; i < codePoints.length; i++) {
            String glyph = new String(Character.toChars(codePoints[i]));
            Component component = Component.literal(glyph).withStyle(style);
            float progress = codePoints.length == 1 ? 0f : i / (float) (codePoints.length - 1);
            draw(graphics, font, component, cursor, y,
                    ColorUtil.interpolate(firstColor, secondColor, progress));
            cursor += width(font, component);
        }
    }
}
