package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

abstract class AbstractCompositeFilterPrimitive extends AbstractFilterPrimitive {
   private FilterChannelKey inputChannel2;

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.inputChannel2 = attributeNode.getFilterChannelKey("in2", DefaultFilterChannel.LastResult);
   }

   @NotNull
   protected abstract Composite composite();

   @NotNull
   private Channel sourceChannel(@NotNull FilterPrimitiveBase impl, @NotNull FilterContext filterContext) {
      return impl.inputChannel(filterContext);
   }

   @NotNull
   private Channel destinationChannel(@NotNull FilterPrimitiveBase impl, @NotNull FilterContext filterContext) {
      return impl.channel(this.inputChannel2, filterContext);
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      LayoutBounds in = this.impl().layoutInput(filterLayoutContext);
      LayoutBounds in2 = filterLayoutContext.resultChannels().get(this.inputChannel2);
      this.impl().saveLayoutResult(in.union(in2), filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      FilterPrimitiveBase impl = this.impl();
      BufferedImage dst = this.destinationChannel(impl, filterContext).toBufferedImageNonAliased(context);
      Image other = context.platformSupport().createImage(this.sourceChannel(impl, filterContext).producer());
      Graphics2D imgGraphics = GraphicsUtil.createGraphics(dst);
      imgGraphics.setComposite(this.computeComposite(filterContext));
      imgGraphics.drawImage(other, null, context.platformSupport().imageObserver());
      imgGraphics.dispose();
      impl.saveResult(new ImageProducerChannel(dst.getSource()), filterContext);
   }

   @NotNull
   private Composite computeComposite(@NotNull FilterContext filterContext) {
      Composite comp = this.composite();
      if (comp instanceof AbstractBlendComposite) {
         ColorInterpolation colorInterpolation = this.colorInterpolation(filterContext);
         ((AbstractBlendComposite)comp).setConvertToLinearRGB(colorInterpolation == ColorInterpolation.LinearRGB);
      }

      return comp;
   }
}

