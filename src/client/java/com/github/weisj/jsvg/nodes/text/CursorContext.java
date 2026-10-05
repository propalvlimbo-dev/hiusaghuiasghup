package com.github.weisj.jsvg.nodes.text;

import org.jetbrains.annotations.NotNull;

public interface CursorContext {
   @NotNull
   GlyphCursor createLocalCursor(boolean var1, @NotNull GlyphCursor var2);

   void cleanUpLocalCursor(@NotNull GlyphCursor var1, @NotNull GlyphCursor var2);
}
