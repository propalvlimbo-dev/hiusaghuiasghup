package com.github.weisj.jsvg.paint.impl;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.parser.PaintParser;
import com.github.weisj.jsvg.parser.impl.ParserUtil;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultPaintParser implements PaintParser {
   private static final Logger LOGGER = LogFactory.createLogger(DefaultPaintParser.class);

   @Nullable
   @Override
   public Color parseColor(@NotNull String value) {
      if (value.isEmpty()) {
         return null;
      }

      try {
         if (value.charAt(0) == '#') {
            int rgba = -16777216;
            switch (value.length()) {
               case 4:
                  rgba = this.parseHex(
                     new char[]{value.charAt(1), value.charAt(1), value.charAt(2), value.charAt(2), value.charAt(3), value.charAt(3), 'F', 'F'}
                  );
                  break;
               case 5:
                  rgba = this.parseHex(
                     new char[]{
                        value.charAt(1), value.charAt(1), value.charAt(2), value.charAt(2), value.charAt(3), value.charAt(3), value.charAt(4), value.charAt(4)
                     }
                  );
               case 6:
               case 8:
               default:
                  break;
               case 7:
                  rgba = this.parseHex(
                     new char[]{value.charAt(1), value.charAt(2), value.charAt(3), value.charAt(4), value.charAt(5), value.charAt(6), 'F', 'F'}
                  );
                  break;
               case 9:
                  rgba = this.parseHex(value.substring(1).toCharArray());
            }

            return new Color(rgba, true);
         } else if (value.length() > 3 && value.substring(0, 3).equalsIgnoreCase("rgb")) {
            boolean isRgba = value.length() > 4 && (value.charAt(3) == 'a' || value.charAt(3) == 'A');
            int startIndex = isRgba ? 5 : 4;
            String[] values = ParserUtil.parseStringList(value.substring(startIndex, value.length() - 1), SeparatorMode.COMMA_AND_WHITESPACE);
            isRgba = isRgba && values.length >= 4;
            return new Color(
               this.parseColorComponent(values[0], false),
               this.parseColorComponent(values[1], false),
               this.parseColorComponent(values[2], false),
               isRgba ? this.parseColorComponent(values[3], true) : 255
            );
         } else {
            return DefaultPaintParser.ColorLookup.colorMap().get(value.toLowerCase(Locale.ENGLISH));
         }
      } catch (Exception e) {
         LOGGER.log(Logger.Level.INFO, e.getMessage(), e);
         return null;
      }
   }

   @Nullable
   @Override
   public SVGPaint parsePaint(@Nullable String value) {
      if (value == null) {
         return null;
      }

      String lower = value.toLowerCase(Locale.ENGLISH);
      if ("none".equals(lower) || "transparent".equals(lower)) {
         return PredefinedPaints.NONE;
      }

      if ("currentcolor".equals(lower)) {
         return PredefinedPaints.CURRENT_COLOR;
      }

      if ("context-fill".equals(lower)) {
         return PredefinedPaints.CONTEXT_FILL;
      }

      if ("context-stroke".equals(lower)) {
         return PredefinedPaints.CONTEXT_STROKE;
      }

      Color color = this.parseColor(lower);
      return color == null ? null : new AwtSVGPaint(color);
   }

   private int parseColorComponent(String value, boolean percentage) {
      float parsed;
      if (value.endsWith("%")) {
         parsed = ParserUtil.parseFloat(value.substring(0, value.length() - 1), 0.0F);
         parsed /= 100.0F;
         parsed *= 255.0F;
      } else {
         parsed = ParserUtil.parseFloat(value, 0.0F);
         if (percentage) {
            parsed *= 255.0F;
         }
      }

      return Math.min(255, Math.max(0, (int)parsed));
   }

   private int parseHex(char[] chars) {
      int r = this.charToColorInt(chars[0]) << 4 | this.charToColorInt(chars[1]);
      int g = this.charToColorInt(chars[2]) << 4 | this.charToColorInt(chars[3]);
      int b = this.charToColorInt(chars[4]) << 4 | this.charToColorInt(chars[5]);
      int a = this.charToColorInt(chars[6]) << 4 | this.charToColorInt(chars[7]);
      return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   private int charToColorInt(char c) {
      if (c >= '0' && c <= '9') {
         return c - 48;
      } else if (c >= 'a' && c <= 'z') {
         return c - 97 + 10;
      } else {
         return c >= 65 && c <= 90 ? c - 65 + 10 : 0;
      }
   }

   private static final class ColorLookup {
      private static Map<String, Color> colorMap() {
         return DefaultPaintParser.ColorLookup.Cache.INSTANCE.colorMap;
      }

      private enum Cache {
         INSTANCE;

         private final Map<String, Color> colorMap;

         Cache() {
            Map<String, Color> map = new HashMap<>(144);
            map.put("aliceblue", new Color(15792383));
            map.put("antiquewhite", new Color(16444375));
            map.put("aqua", new Color(65535));
            map.put("aquamarine", new Color(8388564));
            map.put("azure", new Color(15794175));
            map.put("beige", new Color(16119260));
            map.put("bisque", new Color(16770244));
            map.put("black", new Color(0));
            map.put("blanchedalmond", new Color(16772045));
            map.put("blue", new Color(255));
            map.put("blueviolet", new Color(9055202));
            map.put("brown", new Color(10824234));
            map.put("burlywood", new Color(14596231));
            map.put("cadetblue", new Color(6266528));
            map.put("chartreuse", new Color(8388352));
            map.put("chocolate", new Color(13789470));
            map.put("coral", new Color(16744272));
            map.put("cornflowerblue", new Color(6591981));
            map.put("cornsilk", new Color(16775388));
            map.put("crimson", new Color(14423100));
            map.put("cyan", new Color(65535));
            map.put("darkblue", new Color(139));
            map.put("darkcyan", new Color(35723));
            map.put("darkgoldenrod", new Color(12092939));
            map.put("darkgray", new Color(11119017));
            map.put("darkgrey", new Color(11119017));
            map.put("darkgreen", new Color(25600));
            map.put("darkkhaki", new Color(12433259));
            map.put("darkmagenta", new Color(9109643));
            map.put("darkolivegreen", new Color(5597999));
            map.put("darkorange", new Color(16747520));
            map.put("darkorchid", new Color(10040012));
            map.put("darkred", new Color(9109504));
            map.put("darksalmon", new Color(15308410));
            map.put("darkseagreen", new Color(9419919));
            map.put("darkslateblue", new Color(4734347));
            map.put("darkslategray", new Color(3100495));
            map.put("darkslategrey", new Color(3100495));
            map.put("darkturquoise", new Color(52945));
            map.put("darkviolet", new Color(9699539));
            map.put("deeppink", new Color(16716947));
            map.put("deepskyblue", new Color(49151));
            map.put("dimgray", new Color(6908265));
            map.put("dimgrey", new Color(6908265));
            map.put("dodgerblue", new Color(2003199));
            map.put("feldspar", new Color(13734517));
            map.put("firebrick", new Color(11674146));
            map.put("floralwhite", new Color(16775920));
            map.put("forestgreen", new Color(2263842));
            map.put("fuchsia", new Color(16711935));
            map.put("gainsboro", new Color(14474460));
            map.put("ghostwhite", new Color(16316671));
            map.put("gold", new Color(16766720));
            map.put("goldenrod", new Color(14329120));
            map.put("gray", new Color(8421504));
            map.put("grey", new Color(8421504));
            map.put("green", new Color(32768));
            map.put("greenyellow", new Color(11403055));
            map.put("honeydew", new Color(15794160));
            map.put("hotpink", new Color(16738740));
            map.put("indianred", new Color(13458524));
            map.put("indigo", new Color(4915330));
            map.put("ivory", new Color(16777200));
            map.put("khaki", new Color(15787660));
            map.put("lavender", new Color(15132410));
            map.put("lavenderblush", new Color(16773365));
            map.put("lawngreen", new Color(8190976));
            map.put("lemonchiffon", new Color(16775885));
            map.put("lightblue", new Color(11393254));
            map.put("lightcoral", new Color(15761536));
            map.put("lightcyan", new Color(14745599));
            map.put("lightgoldenrodyellow", new Color(16448210));
            map.put("lightgray", new Color(13882323));
            map.put("lightgrey", new Color(13882323));
            map.put("lightgreen", new Color(9498256));
            map.put("lightpink", new Color(16758465));
            map.put("lightsalmon", new Color(16752762));
            map.put("lightseagreen", new Color(2142890));
            map.put("lightskyblue", new Color(8900346));
            map.put("lightslateblue", new Color(8679679));
            map.put("lightslategray", new Color(7833753));
            map.put("lightslategrey", new Color(7833753));
            map.put("lightsteelblue", new Color(11584734));
            map.put("lightyellow", new Color(16777184));
            map.put("lime", new Color(65280));
            map.put("limegreen", new Color(3329330));
            map.put("linen", new Color(16445670));
            map.put("magenta", new Color(16711935));
            map.put("maroon", new Color(8388608));
            map.put("mediumaquamarine", new Color(6737322));
            map.put("mediumblue", new Color(205));
            map.put("mediumorchid", new Color(12211667));
            map.put("mediumpurple", new Color(9662680));
            map.put("mediumseagreen", new Color(3978097));
            map.put("mediumslateblue", new Color(8087790));
            map.put("mediumspringgreen", new Color(64154));
            map.put("mediumturquoise", new Color(4772300));
            map.put("mediumvioletred", new Color(13047173));
            map.put("midnightblue", new Color(1644912));
            map.put("mintcream", new Color(16121850));
            map.put("mistyrose", new Color(16770273));
            map.put("moccasin", new Color(16770229));
            map.put("navajowhite", new Color(16768685));
            map.put("navy", new Color(128));
            map.put("oldlace", new Color(16643558));
            map.put("olive", new Color(8421376));
            map.put("olivedrab", new Color(7048739));
            map.put("orange", new Color(16753920));
            map.put("orangered", new Color(16729344));
            map.put("orchid", new Color(14315734));
            map.put("palegoldenrod", new Color(15657130));
            map.put("palegreen", new Color(10025880));
            map.put("paleturquoise", new Color(11529966));
            map.put("palevioletred", new Color(14184595));
            map.put("papayawhip", new Color(16773077));
            map.put("peachpuff", new Color(16767673));
            map.put("peru", new Color(13468991));
            map.put("pink", new Color(16761035));
            map.put("plum", new Color(14524637));
            map.put("powderblue", new Color(11591910));
            map.put("purple", new Color(8388736));
            map.put("red", new Color(16711680));
            map.put("rosybrown", new Color(12357519));
            map.put("royalblue", new Color(4286945));
            map.put("saddlebrown", new Color(9127187));
            map.put("salmon", new Color(16416882));
            map.put("sandybrown", new Color(16032864));
            map.put("seagreen", new Color(3050327));
            map.put("seashell", new Color(16774638));
            map.put("sienna", new Color(10506797));
            map.put("silver", new Color(12632256));
            map.put("skyblue", new Color(8900331));
            map.put("slateblue", new Color(6970061));
            map.put("slategray", new Color(7372944));
            map.put("slategrey", new Color(7372944));
            map.put("snow", new Color(16775930));
            map.put("springgreen", new Color(65407));
            map.put("steelblue", new Color(4620980));
            map.put("tan", new Color(13808780));
            map.put("teal", new Color(32896));
            map.put("thistle", new Color(14204888));
            map.put("tomato", new Color(16737095));
            map.put("turquoise", new Color(4251856));
            map.put("violet", new Color(15631086));
            map.put("violetred", new Color(13639824));
            map.put("wheat", new Color(16113331));
            map.put("white", new Color(16777215));
            map.put("whitesmoke", new Color(16119285));
            map.put("yellow", new Color(16776960));
            map.put("yellowgreen", new Color(10145074));
            this.colorMap = Collections.unmodifiableMap(map);
         }

         // $VF: synthetic method
         private static DefaultPaintParser.ColorLookup.Cache[] $values() {
            return new DefaultPaintParser.ColorLookup.Cache[]{INSTANCE};
         }
      }
   }
}

