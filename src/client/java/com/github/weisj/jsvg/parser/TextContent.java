package com.github.weisj.jsvg.parser;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public interface TextContent {
   @Experimental
   @NotNull
   List<TextContent.Segment> contentAfterChildIndex(int var1);

   interface Segment {
      @NotNull
      String text();

      boolean isConstant();
   }
}
