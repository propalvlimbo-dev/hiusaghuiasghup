package com.github.weisj.jsvg.parser.impl;

import org.jetbrains.annotations.NotNull;

public enum SeparatorMode implements ListSplitter {
   COMMA_ONLY(',', false),
   SEMICOLON_ONLY(';', false),
   WHITESPACE_ONLY('\u0000', true),
   COMMA_AND_WHITESPACE(',', true);

   private final boolean allowWhitespace;
   private final char separator;

   SeparatorMode(char separator, boolean allowWhitespace) {
      this.allowWhitespace = allowWhitespace;
      this.separator = separator;
   }

   @Override
   public boolean splitOnWhitespace() {
      return this.allowWhitespace;
   }

   @Override
   public ListSplitter.SplitResult testChar(char c, int subwordIndex, @NotNull String s, int stringIndex) {
      return this.separator != 0 && c == this.separator ? ListSplitter.SplitResult.YES : ListSplitter.SplitResult.NO;
   }

   // $VF: synthetic method
   private static SeparatorMode[] $values() {
      return new SeparatorMode[]{COMMA_ONLY, SEMICOLON_ONLY, WHITESPACE_ONLY, COMMA_AND_WHITESPACE};
   }
}
