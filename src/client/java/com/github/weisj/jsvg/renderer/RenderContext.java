package com.github.weisj.jsvg.renderer;

import com.github.weisj.jsvg.attributes.FillRule;
import com.github.weisj.jsvg.attributes.PaintOrder;
import com.github.weisj.jsvg.attributes.font.FontResolver;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.attributes.font.SVGFont;
import com.github.weisj.jsvg.attributes.stroke.StrokeResolver;
import com.github.weisj.jsvg.nodes.SVG;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.PredefinedPaints;
import com.github.weisj.jsvg.renderer.impl.PaintResolver;
import com.github.weisj.jsvg.renderer.impl.context.ContextElementAttributes;
import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.impl.context.StrokeContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RenderContext {
   @NotNull
   private final PlatformSupport platformSupport;
   @NotNull
   private final MeasureContext measureContext;
   @NotNull
   private final PaintContext paintContext;
   @NotNull
   private final FontRenderContext fontRenderContext;
   @NotNull
   private final MeasurableFontSpec fontSpec;
   @Nullable
   private final ContextElementAttributes contextElementAttributes;
   @NotNull
   private final AffineTransform rootTransform;
   @NotNull
   private final AffineTransform userSpaceTransform;

   @NotNull
   private static RenderContext createInitial(@Nullable SVGPaint currentColor, @NotNull PlatformSupport awtSupport, @NotNull MeasureContext measureContext) {
      SVGPaint color = currentColor != null ? currentColor : PredefinedPaints.DEFAULT_PAINT;
      return new RenderContext(
         awtSupport,
         new AffineTransform(),
         new AffineTransform(),
         PaintContext.createDefault(color),
         measureContext,
         FontRenderContext.createDefault(),
         MeasurableFontSpec.createDefault(),
         null
      );
   }

   private RenderContext(
      @NotNull PlatformSupport platformSupport,
      @NotNull AffineTransform rootTransform,
      @NotNull AffineTransform userSpaceTransform,
      @NotNull PaintContext paintContext,
      @NotNull MeasureContext measureContext,
      @NotNull FontRenderContext fontRenderContext,
      @NotNull MeasurableFontSpec fontSpec,
      @Nullable ContextElementAttributes contextElementAttributes
   ) {
      this.platformSupport = platformSupport;
      this.rootTransform = rootTransform;
      this.userSpaceTransform = userSpaceTransform;
      this.paintContext = paintContext;
      this.measureContext = measureContext;
      this.fontRenderContext = fontRenderContext;
      this.fontSpec = fontSpec;
      this.contextElementAttributes = contextElementAttributes;
   }

   @NotNull
   private RenderContext derive(
      @Nullable Mutator<PaintContext> context,
      @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
      @Nullable ViewBox viewBox,
      @Nullable FontRenderContext frc,
      @Nullable ContextElementAttributes contextAttributes,
      RenderContext.EstablishRootMeasure establishRootMeasure
   ) {
      return this.deriveImpl(context, attributeFontSpec, viewBox, frc, contextAttributes, null, establishRootMeasure);
   }

   @NotNull
   private RenderContext deriveImpl(
      @Nullable Mutator<PaintContext> context,
      @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
      @Nullable ViewBox viewBox,
      @Nullable FontRenderContext frc,
      @Nullable ContextElementAttributes contextAttributes,
      @Nullable AffineTransform rootTransform,
      RenderContext.EstablishRootMeasure establishRootMeasure
   ) {
      if (context == null && viewBox == null && attributeFontSpec == null && frc == null) {
         return this;
      }

      PaintContext newPaintContext = this.paintContext;
      MeasurableFontSpec newFontSpec = this.fontSpec;
      if (context != null) {
         newPaintContext = context.mutate(this.paintContext);
      }

      if (attributeFontSpec != null) {
         newFontSpec = attributeFontSpec.mutate(newFontSpec);
      }

      ContextElementAttributes newContextAttributes = this.contextElementAttributes;
      if (contextAttributes != null) {
         newContextAttributes = contextAttributes;
      }

      float em = newFontSpec.emSize(this.measureContext);
      float ex = SVGFont.exFromEm(em);
      MeasureContext newMeasureContext = this.measureContext.derive(viewBox, em, ex);
      if (establishRootMeasure == RenderContext.EstablishRootMeasure.YES) {
         newMeasureContext = newMeasureContext.deriveRoot(em);
      }

      FontRenderContext effectiveFrc = this.fontRenderContext.derive(frc);
      AffineTransform newRootTransform = rootTransform != null ? rootTransform : this.rootTransform;
      return new RenderContext(
         this.platformSupport,
         newRootTransform,
         new AffineTransform(this.userSpaceTransform),
         newPaintContext,
         newMeasureContext,
         effectiveFrc,
         newFontSpec,
         newContextAttributes
      );
   }

   @NotNull
   private RenderContext deriveForChildGraphics() {
      return this.derive(t -> t, null, null, null, null, RenderContext.EstablishRootMeasure.NO);
   }

   @NotNull
   private RenderContext deriveForSurface() {
      return this.deriveImpl(t -> t, null, null, null, null, new AffineTransform(this.rootTransform), RenderContext.EstablishRootMeasure.NO);
   }

   @NotNull
   private RenderContext deriveForNode(
      @Nullable Mutator<PaintContext> context,
      @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
      @Nullable FontRenderContext frc,
      @Nullable ContextElementAttributes contextAttributes,
      @NotNull Object node
   ) {
      RenderContext.EstablishRootMeasure establishRootMeasure = node instanceof SVG && ((SVG)node).isTopLevel()
         ? RenderContext.EstablishRootMeasure.YES
         : RenderContext.EstablishRootMeasure.NO;
      return this.derive(context, attributeFontSpec, null, frc, contextAttributes, establishRootMeasure);
   }

   @NotNull
   public AffineTransform rootTransform() {
      return this.rootTransform;
   }

   @NotNull
   public AffineTransform userSpaceTransform() {
      return this.userSpaceTransform;
   }

   public void translate(@NotNull Output output, @NotNull Point2D dp) {
      this.translate(output, dp.getX(), dp.getY());
   }

   public void translate(@NotNull Output output, double dx, double dy) {
      output.translate(dx, dy);
      this.userSpaceTransform.translate(dx, dy);
   }

   public void scale(@NotNull Output output, double sx, double sy) {
      output.scale(sx, sy);
      this.userSpaceTransform.scale(sx, sy);
   }

   public void rotate(@NotNull Output output, double angle) {
      output.rotate(angle);
      this.userSpaceTransform.rotate(angle);
   }

   public void transform(@NotNull Output output, @NotNull AffineTransform at) {
      output.applyTransform(at);
      this.userSpaceTransform.concatenate(at);
   }

   @NotNull
   public PlatformSupport platformSupport() {
      return this.platformSupport;
   }

   @NotNull
   public MeasureContext measureContext() {
      return this.measureContext;
   }

   @NotNull
   public SVGPaint strokePaint() {
      return PaintResolver.resolvePaint(this.paintContext.strokePaint, this.paintContext, this.contextElementAttributes);
   }

   @NotNull
   public SVGPaint fillPaint() {
      return PaintResolver.resolvePaint(this.paintContext.fillPaint, this.paintContext, this.contextElementAttributes);
   }

   public float rawOpacity() {
      return this.paintContext.opacity.get(this.measureContext);
   }

   public float fillOpacity() {
      assert this.paintContext.fillOpacity != null;
      return this.paintContext.fillOpacity.get(this.measureContext) * this.paintContext.opacity.get(this.measureContext);
   }

   public float strokeOpacity() {
      assert this.paintContext.strokeOpacity != null;
      return this.paintContext.strokeOpacity.get(this.measureContext) * this.paintContext.opacity.get(this.measureContext);
   }

   @NotNull
   public Stroke stroke(float pathLengthFactor) {
      return StrokeResolver.resolve(pathLengthFactor, this.measureContext, this.strokeContext());
   }

   @NotNull
   private StrokeContext strokeContext() {
      assert this.paintContext.strokeContext != null;
      return this.paintContext.strokeContext;
   }

   @NotNull
   private FontRenderContext fontRenderContext() {
      return this.fontRenderContext;
   }

   @Nullable
   private ContextElementAttributes contextElementAttributes() {
      return this.contextElementAttributes;
   }

   @NotNull
   private FillRule fillRule() {
      FillRule fillRule = this.paintContext.fillRule;
      return fillRule != null ? fillRule : FillRule.Nonzero;
   }

   @NotNull
   private PaintOrder paintOrder() {
      PaintOrder paintOrder = this.paintContext.paintOrder;
      return paintOrder != null ? paintOrder : PaintOrder.NORMAL;
   }

   @NotNull
   private SVGFont font() {
      return FontResolver.resolve(this.fontSpec, this.measureContext, this.platformSupport.fontFamily());
   }

   private void setRootTransform(@NotNull AffineTransform rootTransform) {
      this.rootTransform.setTransform(rootTransform);
      this.userSpaceTransform.setToIdentity();
   }

   private void setRootTransform(@NotNull AffineTransform rootTransform, @NotNull AffineTransform userSpaceTransform) {
      this.rootTransform.setTransform(rootTransform);
      this.userSpaceTransform.setTransform(userSpaceTransform);
   }

   @Override
   public String toString() {
      return "RenderContext{platformSupport="
         + this.platformSupport
         + ", measureContext="
         + this.measureContext
         + ", paintContext="
         + this.paintContext
         + ", fontRenderContext="
         + this.fontRenderContext
         + ", fontSpec="
         + this.fontSpec
         + ", contextElementAttributes="
         + this.contextElementAttributes
         + ", rootTransform="
         + this.rootTransform
         + ", userSpaceTransform="
         + this.userSpaceTransform
         + '}';
   }

   static {
      RenderContextAccessor.setInstance(
         new RenderContextAccessor.Accessor() {
            @NotNull
            @Override
            public RenderContext createInitial(@Nullable SVGPaint currentColor, @NotNull PlatformSupport awtSupport, @NotNull MeasureContext measureContext) {
               return RenderContext.createInitial(currentColor, awtSupport, measureContext);
            }

            @NotNull
            @Override
            public RenderContext deriveForSurface(@NotNull RenderContext context) {
               return context.deriveForSurface();
            }

            @NotNull
            @Override
            public RenderContext deriveForChildGraphics(@NotNull RenderContext context) {
               return context.deriveForChildGraphics();
            }

            @NotNull
            @Override
            public RenderContext deriveForNode(
               @NotNull RenderContext context,
               @Nullable Mutator<PaintContext> paintContextMutator,
               @Nullable Mutator<MeasurableFontSpec> attributeFontSpec,
               @Nullable FontRenderContext frc,
               @Nullable ContextElementAttributes contextAttributes,
               @NotNull Object node
            ) {
               return context.deriveForNode(paintContextMutator, attributeFontSpec, frc, contextAttributes, node);
            }

            @NotNull
            @Override
            public RenderContext setupInnerViewRenderContext(@NotNull ViewBox viewBox, @NotNull RenderContext context, boolean inheritAttributes) {
               if (inheritAttributes) {
                  return context.derive(null, null, viewBox, null, null, RenderContext.EstablishRootMeasure.NO);
               }

               MeasureContext newMeasure = context.measureContext().derive(viewBox, Float.NaN, Float.NaN);
               return new RenderContext(
                  context.platformSupport(),
                  new AffineTransform(),
                  new AffineTransform(),
                  PaintContext.createDefault(PredefinedPaints.DEFAULT_PAINT),
                  newMeasure,
                  FontRenderContext.createDefault(),
                  MeasurableFontSpec.createDefault(),
                  context.contextElementAttributes()
               );
            }

            @NotNull
            @Override
            public StrokeContext strokeContext(@NotNull RenderContext context) {
               return context.strokeContext();
            }

            @NotNull
            @Override
            public FontRenderContext fontRenderContext(@NotNull RenderContext context) {
               return context.fontRenderContext();
            }

            @NotNull
            @Override
            public FillRule fillRule(@NotNull RenderContext context) {
               return context.fillRule();
            }

            @NotNull
            @Override
            public PaintOrder paintOrder(@NotNull RenderContext context) {
               return context.paintOrder();
            }

            @NotNull
            @Override
            public SVGFont font(@NotNull RenderContext context) {
               return context.font();
            }

            @Nullable
            @Override
            public SVGPaint currentColor(@NotNull RenderContext context) {
               return context.paintContext.color;
            }

            @Override
            public void setRootTransform(@NotNull RenderContext context, @NotNull AffineTransform rootTransform) {
               context.setRootTransform(rootTransform);
            }

            @Override
            public void setRootTransform(@NotNull RenderContext context, @NotNull AffineTransform rootTransform, @NotNull AffineTransform userSpaceTransform) {
               context.setRootTransform(rootTransform, userSpaceTransform);
            }
         }
      );
   }

   private enum EstablishRootMeasure {
      YES,
      NO;

      // $VF: synthetic method
      private static RenderContext.EstablishRootMeasure[] $values() {
         return new RenderContext.EstablishRootMeasure[]{YES, NO};
      }
   }
}

