package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.attributes.value.Value;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.util.PathUtil;
import java.awt.geom.Path2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AnimatedPath implements Value<Path2D> {
   @NotNull
   private final AnimatedFloatList list;
   @Nullable
   private Path2D cache;
   private final boolean closed;

   public AnimatedPath(@NotNull AnimatedFloatList list, boolean closed) {
      this.list = list;
      this.closed = closed;
   }

   @NotNull
   public Path2D get(@NotNull MeasureContext context) {
      if (this.cache == null || this.list.isDirty(context.timestamp())) {
         this.cache = PathUtil.setPolyLine(this.cache, this.list.get(context), this.closed);
      }

      return this.cache;
   }
}

