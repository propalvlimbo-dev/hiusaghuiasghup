package wtf.expensive.client.ui.theme;

import wtf.expensive.client.util.render.ColorUtil;

public class Style {
    public final String name;
    public final int[] colors;
    private final boolean rainbow;

    public Style(String name, int... colors) {
        this(name, false, colors);
    }

    public Style(String name, boolean rainbow, int... colors) {
        this.name = name;
        this.rainbow = rainbow;
        this.colors = colors;
    }

    public boolean isRainbow() {
        return rainbow;
    }

    public int getColor(int index) {
        if (rainbow) {
            return ColorUtil.astolfo(10, index, 0.5f, 1.0f, 1.0f);
        }
        return ColorUtil.gradient(5, index, colors);
    }
}
