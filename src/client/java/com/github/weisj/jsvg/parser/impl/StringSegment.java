package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.parser.TextContent;
import org.jetbrains.annotations.NotNull;

public final class StringSegment implements TextContent.Segment {
   @NotNull
   private final String text;

   public StringSegment(@NotNull String text) {
      this.text = text;
   }

   @NotNull
   @Override
   public String text() {
      return this.text;
   }

   @Override
   public boolean isConstant() {
      return true;
   }
}

