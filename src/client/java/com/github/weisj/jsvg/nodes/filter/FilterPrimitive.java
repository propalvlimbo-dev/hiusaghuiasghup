package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.RenderContext;
import org.jetbrains.annotations.NotNull;

public interface FilterPrimitive {
   @NotNull
   Length y();

   @NotNull
   Length x();

   @NotNull
   Length width();

   @NotNull
   Length height();

   default boolean isValid() {
      return true;
   }

   void layoutFilter(@NotNull RenderContext var1, @NotNull FilterLayoutContext var2);

   void applyFilter(@NotNull RenderContext var1, @NotNull FilterContext var2);

   ColorInterpolation colorInterpolation(@NotNull FilterContext var1);
}
