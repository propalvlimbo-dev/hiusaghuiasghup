package com.github.weisj.jsvg.parser.impl;

import org.jetbrains.annotations.NotNull;

public interface ListSplitter {
   boolean splitOnWhitespace();

   ListSplitter.SplitResult testChar(char var1, int var2, @NotNull String var3, int var4);

   enum SplitResult {
      YES,
      YES_INCLUDING_CHAR,
      NO;

      public boolean shouldSplit() {
         return this != NO;
      }

      public boolean shouldIncludeChar() {
         return this == YES_INCLUDING_CHAR;
      }

      // $VF: synthetic method
      private static ListSplitter.SplitResult[] $values() {
         return new ListSplitter.SplitResult[]{YES, YES_INCLUDING_CHAR, NO};
      }
   }
}
