package com.github.weisj.jsvg.parser.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.jetbrains.annotations.NotNull;

public final class StreamUtil {
   private StreamUtil() {
   }

   @NotNull
   public static InputStream createDocumentInputStream(@NotNull InputStream is) throws IOException {
      BufferedInputStream bin = new BufferedInputStream(is);
      bin.mark(2);
      int b0 = bin.read();
      int b1 = bin.read();
      bin.reset();
      return (b1 << 8 | b0) == 35615 ? new GZIPInputStream(bin) : bin;
   }
}

