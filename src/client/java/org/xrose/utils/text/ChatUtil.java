package org.xrose.utils.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.xrose.context.MinecraftContext;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;

public final class ChatUtil {
   private static final Pattern EMOJI_ALIAS = Pattern.compile(":[A-Za-z0-9_+\\-]+:");
   private static final String PREFIX = "Elytrix: ";

   public static void print(String message) {
      info(message);
   }

   public static void header(String message) {
      send(":sparkles:  " + message);
   }

   public static void info(String message) {
      send(":information_source:  " + message);
   }

   public static void success(String message) {
      send(":white_check_mark:  " + message);
   }

   public static void error(String message) {
      send(":x:  " + message);
   }

   public static void usage(String message) {
      send(":keyboard:  " + message);
   }

   public static void entry(String emoji, String title, String detail) {
      send(emoji + "  " + title + (detail != null && !detail.isBlank() ? "  •  " + detail : ""));
   }

   public static void send(String message) {
      Minecraft mc = MinecraftContext.mc;
      if (mc.gui != null) {
         mc.gui.hud.getChat().addClientSystemMessage(gradient("Elytrix: " + message));
      }
   }

   private static MutableComponent gradient(String text) {
      MutableComponent result = Component.empty();
      int startColor = Theme.getAccent();
      int endColor = ColorUtil.lerp(startColor, -1, 0.62F);
      Matcher matcher = EMOJI_ALIAS.matcher(text);
      int cursor = 0;

      while (cursor < text.length()) {
         if (matcher.find(cursor) && matcher.start() == cursor) {
            appendColored(result, matcher.group(), gradientColor(startColor, endColor, cursor, text.length()));
            cursor = matcher.end();
         } else {
            int codePoint = text.codePointAt(cursor);
            appendColored(result, new String(Character.toChars(codePoint)), gradientColor(startColor, endColor, cursor, text.length()));
            cursor += Character.charCount(codePoint);
         }
      }

      return result;
   }

   private static int gradientColor(int start, int end, int index, int length) {
      return ColorUtil.lerp(start, end, length <= 1 ? 0.0F : (float)index / (length - 1));
   }

   private static void appendColored(MutableComponent target, String text, int color) {
      target.append(Component.literal(text).withStyle(Style.EMPTY.withColor(color & 16777215)));
   }

   @Generated
   private ChatUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}

