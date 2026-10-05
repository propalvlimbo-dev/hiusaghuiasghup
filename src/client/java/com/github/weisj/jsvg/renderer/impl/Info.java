package com.github.weisj.jsvg.renderer.impl;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.renderer.output.impl.Graphics2DOutput;
import com.github.weisj.jsvg.util.BlittableImage;
import com.github.weisj.jsvg.util.ImageUtil;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class Info implements AutoCloseable {
   @NotNull
   protected final RenderContext context;
   @NotNull
   protected final Output output;
   @NotNull
   private final Renderable renderable;

   Info(@NotNull Renderable renderable, @NotNull RenderContext context, @NotNull Output output) {
      this.renderable = renderable;
      this.context = context;
      this.output = output;
   }

   @NotNull
   public Renderable renderable() {
      return this.renderable;
   }

   @NotNull
   public Output output() {
      return this.output;
   }

   @NotNull
   public RenderContext context() {
      return this.context;
   }

   @Override
   public void close() {
      this.output.dispose();
   }

   static final class InfoWithIsolation extends Info {
      @NotNull
      private final BlittableImage blittableImage;
      @NotNull
      private final Output imageOutput;
      @NotNull
      private final ElementBounds elementBounds;
      @NotNull
      private final IsolationEffects isolationEffects;
      @Nullable
      private final Filter.FilterInfo filterInfo;

      @Nullable
      static Info.InfoWithIsolation create(
         @NotNull Renderable renderable,
         @NotNull RenderContext context,
         @NotNull Output output,
         @NotNull ElementBounds elementBounds,
         @NotNull IsolationEffects effects
      ) {
         Rectangle2D clipBounds = null;
         Rectangle2D bounds = null;
         Filter.FilterBounds filterBounds = null;
         Filter.FilterInfo filterInfo = null;
         Filter filter = effects.filter;
         Mask mask = effects.mask;
         ClipPath clipPath = effects.clipPath;
         if (filter != null) {
            filterBounds = filter.createFilterBounds(output, context, elementBounds);
            if (filterBounds != null) {
               bounds = filterBounds.filterRegion();
               clipBounds = filterBounds.effectiveFilterArea();
            }
         }

         if (mask != null || clipPath != null) {
            bounds = elementBounds.geometryBox();
         }

         if (bounds == null) {
            return null;
         }

         RenderContext imageContext = RenderContextAccessor.instance().deriveForSurface(context);
         BlittableImage blitImage = BlittableImage.create(
            ImageUtil::createCompatibleTransparentImage, context, clipBounds, bounds, elementBounds.boundingBox(), UnitType.UserSpaceOnUse, imageContext
         );
         if (blitImage == null) {
            return null;
         }

         Graphics2D g = blitImage.createGraphics();
         g.setRenderingHints(output.renderingHints());
         Output imageOutput = new Graphics2DOutput(g);
         if (filter != null && filterBounds != null) {
            filterInfo = new Filter.FilterInfo(blitImage, imageOutput, filterBounds);
         }

         return new Info.InfoWithIsolation(renderable, context, output, imageOutput, blitImage, elementBounds, effects, filterInfo);
      }

      private InfoWithIsolation(
         @NotNull Renderable renderable,
         @NotNull RenderContext context,
         @NotNull Output output,
         @NotNull Output imageOutput,
         @NotNull BlittableImage blittableImage,
         @NotNull ElementBounds elementBounds,
         @NotNull IsolationEffects isolationEffects,
         @Nullable Filter.FilterInfo filterInfo
      ) {
         super(renderable, context, output);
         this.blittableImage = blittableImage;
         this.imageOutput = imageOutput;
         this.elementBounds = elementBounds;
         this.isolationEffects = isolationEffects;
         this.filterInfo = filterInfo;
      }

      @NotNull
      @Override
      public Output output() {
         return this.imageOutput;
      }

      @NotNull
      @Override
      public RenderContext context() {
         return this.blittableImage.context();
      }

      @Override
      public void close() {
         Output previousOutput = this.output;
         BufferedImage result = this.blittableImage.image();
         if (this.isolationEffects.filter != null) {
            assert this.filterInfo != null;
            result = this.isolationEffects.filter.applyFilter(previousOutput, this.context, this.filterInfo);
            previousOutput.applyClip(this.filterInfo.filterRegion());
         }

         if (this.isolationEffects.mask != null) {
            previousOutput.setPaint(() -> this.isolationEffects.mask.createMaskPaint(previousOutput, this.context, this.elementBounds));
         }

         if (this.isolationEffects.clipPath != null) {
            this.isolationEffects.clipPath.applyClip(previousOutput, this.context, this.elementBounds);
         }

         this.blittableImage.prepareForBlitting(previousOutput);
         previousOutput.drawImage(result, this.context.platformSupport().imageObserver());
         this.imageOutput.dispose();
         super.close();
      }
   }
}

