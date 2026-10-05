package com.github.weisj.jsvg.geometry.path;

import java.awt.geom.Path2D;
import org.jetbrains.annotations.NotNull;

public final class Terminal extends PathCommand {
   Terminal() {
      super(1);
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      path.closePath();
      hist.setLast(hist.startPoint);
   }

   @Override
   public String toString() {
      return "Z";
   }
}

