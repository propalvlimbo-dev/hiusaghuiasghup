package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.util.ParserBase;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ParserUtil {
   private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

   private ParserUtil() {
   }

   @NotNull
   public static String removeWhiteSpace(@NotNull String value) {
      return WHITESPACE_PATTERN.matcher(value).replaceAll("");
   }

   public static int parseInt(@Nullable String value, int fallback) {
      if (value == null) {
         return fallback;
      }

      try {
         return Integer.parseInt(value);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   public static float parseFloat(@Nullable String value, float fallback) {
      if (value == null) {
         return fallback;
      }

      try {
         return Float.parseFloat(value);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   @Contract("_,!null -> !null")
   @Nullable
   public static Length parseNumber(@Nullable String value, @Nullable Length fallback) {
      if (value == null) {
         return fallback;
      }

      try {
         return Unit.RAW.valueOf(Float.parseFloat(value));
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   public static float @NotNull [] parseFloatList(@Nullable String value) {
      String[] values = parseStringList(value, SeparatorMode.COMMA_AND_WHITESPACE, new String[0]);
      float[] ret = new float[values.length];

      for (int i = 0; i < ret.length; i++) {
         ret[i] = parseFloat(values[i], 0.0F);
      }

      return ret;
   }

   public static double @NotNull [] parseDoubleList(@Nullable String value) {
      if (value != null && !value.isEmpty()) {
         List<Double> list = new ArrayList<>();
         ParserBase base = new ParserBase(value, 0);

         while (base.hasNext()) {
            list.add(base.nextDouble());
            base.consumeWhiteSpaceOrSeparator();
         }

         return list.stream().mapToDouble(Double::doubleValue).toArray();
      } else {
         return new double[0];
      }
   }

   @NotNull
   public static String[] parseStringList(@Nullable String value, @NotNull ListSplitter splitter) {
      return parseStringList(value, splitter, new String[0]);
   }

   @Contract("_,_,!null -> !null")
   @NotNull
   public static String @Nullable [] parseStringList(@Nullable String value, @NotNull ListSplitter splitter, @NotNull String @Nullable [] fallback) {
      if (value != null && !value.isEmpty()) {
         List<String> list = new ArrayList<>();
         int max = value.length();
         boolean splitOnWhitespace = splitter.splitOnWhitespace();
         int start = 0;
         int i = 0;
         boolean inWhiteSpace = false;
         boolean lastSplitWasWhiteSpace = false;

         while (i < max) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c)) {
               if (splitOnWhitespace) {
                  if (!inWhiteSpace && i - start > 0) {
                     list.add(value.substring(start, i));
                     lastSplitWasWhiteSpace = true;
                  }

                  start = i + 1;
               }

               inWhiteSpace = true;
            } else {
               inWhiteSpace = false;
               ListSplitter.SplitResult result = splitter.testChar(c, i - start, value, i);
               if (result.shouldSplit()) {
                  boolean tokenAlreadyEmittedByWhitespace = lastSplitWasWhiteSpace && i == start;
                  if (!tokenAlreadyEmittedByWhitespace) {
                     list.add(value.substring(start, i));
                  }

                  start = result.shouldIncludeChar() ? i : i + 1;
               }

               lastSplitWasWhiteSpace = false;
            }

            i++;
         }

         if (i - start > 0) {
            list.add(value.substring(start, i));
         }

         return list.toArray(new String[0]);
      } else {
         return fallback;
      }
   }
}

