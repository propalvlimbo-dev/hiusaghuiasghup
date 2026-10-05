package com.github.weisj.jsvg.nodes.container;

import com.github.weisj.jsvg.attributes.Overflow;
import com.github.weisj.jsvg.attributes.PreserveAspectRatio;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.MeasureContext;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseInnerViewContainer extends CommonRenderableContainerNode {
   protected ViewBox viewBox;
   protected PreserveAspectRatio preserveAspectRatio;
   private Overflow overflow;

   @NotNull
   protected abstract Point2D outerLocation(@NotNull MeasureContext var1);

   @Nullable
   protected abstract Point2D anchorLocation(@NotNull MeasureContext var1);

   @NotNull
   public abstract FloatSize size(@NotNull RenderContext var1);

   @NotNull
   protected abstract Overflow defaultOverflow();

   @Nullable
   public ViewBox viewBox(@NotNull RenderContext context) {
      return this.viewBox != null ? this.viewBox : new ViewBox(this.size(context));
   }

   @NotNull
   public ViewBox staticViewBox(@NotNull FloatSize fallbackSize) {
      return this.viewBox != null ? this.viewBox : new ViewBox(fallbackSize);
   }

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.viewBox = attributeNode.getViewBox();
      this.preserveAspectRatio = PreserveAspectRatio.parse(attributeNode.getValue("preserveAspectRatio"), attributeNode.parser());
      this.overflow = attributeNode.getEnum("overflow", this.defaultOverflow());
   }

   public void renderWithEstablishedViewBox(@NotNull RenderContext context, @NotNull Output output) {
      super.render(context, output);
   }

   @Override
   public void render(@NotNull RenderContext context, @NotNull Output output) {
      this.renderWithSize(this.size(context), this.viewBox(context), context, output);
   }

   public final void renderWithSize(@NotNull FloatSize useSiteSize, @Nullable ViewBox view, @NotNull RenderContext context, @NotNull Output output) {
      RenderContext innerContext = this.createInnerContextForViewBox(useSiteSize, view, context, output);
      this.renderWithEstablishedViewBox(innerContext, output);
   }

   protected boolean inheritAttributes() {
      return true;
   }

   @NotNull
   private RenderContext createInnerContext(@NotNull RenderContext context, @NotNull ViewBox viewBox) {
      return NodeRenderer.setupInnerViewRenderContext(viewBox, context, this.inheritAttributes());
   }

   @NotNull
   private ViewBox computeOuterViewBox(@NotNull RenderContext context, @NotNull FloatSize useSiteSize) {
      MeasureContext measureContext = context.measureContext();
      Point2D outerPos = this.outerLocation(measureContext);
      ViewBox vb = new ViewBox(outerPos, useSiteSize);
      if (Length.isUnspecified(vb.width) || Length.isUnspecified(vb.height)) {
         FloatSize size = this.size(context);
         if (Length.isUnspecified(vb.width)) {
            vb.width = size.width;
         }

         if (Length.isUnspecified(vb.height)) {
            vb.height = size.height;
         }
      }

      return vb;
   }

   @NotNull
   public final RenderContext createInnerContextForViewBox(
      @NotNull FloatSize useSiteSize, @Nullable ViewBox view, @NotNull RenderContext context, @NotNull Output output
   ) {
      ViewBox outerViewBox = this.computeOuterViewBox(context, useSiteSize);
      ViewBox innerViewBox = view;
      AffineTransform viewTransform = innerViewBox != null ? this.preserveAspectRatio.computeViewportTransform(outerViewBox.size(), innerViewBox) : null;
      if (innerViewBox == null) {
         innerViewBox = new ViewBox(outerViewBox.size());
      }

      RenderContext innerContext = this.createInnerContext(context, innerViewBox);
      MeasureContext innerMeasure = innerContext.measureContext();
      Point2D anchorPos = this.anchorLocation(innerMeasure);
      if (this.overflow.establishesClip()) {
         ViewBox clipViewBox = new ViewBox(outerViewBox);
         if (anchorPos != null) {
            Point2D clipAnchor = anchorPos;
            if (viewTransform != null) {
               clipAnchor = new Double();
               viewTransform.transform(anchorPos, clipAnchor);
            }

            clipViewBox.x = clipViewBox.x + (float)clipAnchor.getX();
            clipViewBox.y = clipViewBox.y + (float)clipAnchor.getY();
         }

         output.applyClip(clipViewBox);
      }

      innerContext.translate(output, outerViewBox.location());
      if (viewTransform != null) {
         innerContext.transform(output, viewTransform);
      }

      if (anchorPos != null) {
         innerContext.translate(output, anchorPos);
      }

      return innerContext;
   }
}

