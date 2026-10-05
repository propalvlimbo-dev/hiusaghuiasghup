package com.github.weisj.jsvg.parser;

import com.github.weisj.jsvg.parser.impl.ListSplitter;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import org.jetbrains.annotations.NotNull;

public final class NumberListSplitter implements ListSplitter {
   public static final NumberListSplitter INSTANCE = new NumberListSplitter();

   @Override
   public boolean splitOnWhitespace() {
      return true;
   }

   @Override
   public ListSplitter.SplitResult testChar(char c, int subwordIndex, @NotNull String s, int stringIndex) {
      ListSplitter.SplitResult result = SeparatorMode.COMMA_AND_WHITESPACE.testChar(c, subwordIndex, s, stringIndex);
      if (result.shouldSplit()) {
         return result;
      }

      if (subwordIndex > 0) {
         int signIndex = "+-".indexOf(c);
         if (signIndex != -1 && stringIndex > 0 && s.charAt(stringIndex - 1) != 'e') {
            return ListSplitter.SplitResult.YES_INCLUDING_CHAR;
         }
      }

      return ListSplitter.SplitResult.NO;
   }
}

