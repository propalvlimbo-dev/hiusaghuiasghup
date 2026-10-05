package com.github.weisj.jsvg.renderer.impl.context;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.PlatformSupport;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.geom.AffineTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RenderContextAccessor {
   private static RenderContextAccessor.Accessor instance;

   private RenderContextAccessor() {
   }

   public static RenderContextAccessor.Accessor instance() {
      if (instance == null) {
         throw new IllegalStateException("RenderContextAccessor not initialized");
      } else {
         return instance;
      }
   }

   public static void setInstance(RenderContextAccessor.Accessor accessor) {
      if (instance != null) {
         throw new IllegalStateException("RenderContextAccessor already initialized");
      }

      instance = accessor;
   }

   static {
      try {
         Class.forName(RenderContext.class.getName());
      } catch (ClassNotFoundException e) {
         throw new IllegalStateException(e);
      }
   }

   public interface Accessor {
      @NotNull
      RenderContext createInitial(@Nullable SVGPaint var1, @NotNull PlatformSupport var2, @NotNull MeasureContext var3);

      @NotNull
      RenderContext deriveForSurface(@NotNull RenderContext var1);

      @NotNull
      RenderContext deriveForChildGraphics(@NotNull RenderContext var1);

      @NotNull
      RenderContext deriveForNode(
         @NotNull RenderContext var1,
         @Nullable Mutator<PaintContext> var2,
         @Nullable Mutator<MeasurableFontSpec> var3,
         @Nullable FontRenderContext var4,
         @Nullable ContextElementAttributes var5,
         @NotNull Object var6
      );

      @NotNull
      RenderContext setupInnerViewRenderContext(@NotNull ViewBox var1, @NotNull RenderContext var2, boolean var3);

      @NotNull
      StrokeContext strokeContext(@NotNull RenderContext var1);

      @NotNull
      FontRenderContext fontRenderContext(@NotNull RenderContext var1);

      @NotNull
      FillRule fillRule(@NotNull RenderContext var1);

      @NotNull
      PaintOrder paintOrder(@NotNull RenderContext var1);

      @NotNull
      SVGFont font(@NotNull RenderContext var1);

      @Nullable
      SVGPaint currentColor(@NotNull RenderContext var1);

      void setRootTransform(@NotNull RenderContext var1, @NotNull AffineTransform var2);

      void setRootTransform(@NotNull RenderContext var1, @NotNull AffineTransform var2, @NotNull AffineTransform var3);
   }
}

