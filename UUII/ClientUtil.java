package wtf.expensive.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import wtf.expensive.client.managment.Managment;

import java.awt.Color;

public final class ClientUtil {
    public static volatile boolean legitMode = false;

    private ClientUtil() {
    }

    public static MutableComponent gradient(String text, int start, int end) {
        MutableComponent result = Component.empty();
        Color from = new Color(start);
        Color to = new Color(end);
        int length = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            float progress = (float) i / length;
            int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * progress);
            int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
            int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * progress);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb((red << 16) | (green << 8) | blue))));
        }
        return result;
    }

    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int start = 0x5A69FF;
        int end = 0xF562FF;
        if (Managment.STYLE_MANAGER != null && Managment.STYLE_MANAGER.getCurrentStyle() != null) {
            wtf.expensive.client.ui.theme.Style theme = Managment.STYLE_MANAGER.getCurrentStyle();
            if (theme.isRainbow()) {
                start = theme.getColor(0);
                end = theme.getColor(30);
            } else if (theme.colors.length >= 2) {
                start = theme.colors[0];
                end = theme.colors[theme.colors.length - 1];
            }
        }
        mc.gui.hud.getChat().addClientSystemMessage(gradient("Expensive Client", start, end)
                .append(Component.literal(ChatFormatting.DARK_GRAY + " -> " + ChatFormatting.RESET + message)));
    }
}
