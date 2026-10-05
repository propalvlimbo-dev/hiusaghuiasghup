package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.container.ContainerNode;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = FeMergeNode.class)
public final class FeMerge extends ContainerNode implements FilterPrimitive {
   public static final String TAG = "feMerge";
   private FilterPrimitiveBase filterPrimitiveBase;
   private FilterChannelKey[] inputChannels;

   @NotNull
   @Override
   public String tagName() {
      return "feMerge";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.filterPrimitiveBase = new FilterPrimitiveBase(attributeNode);
      List<FeMergeNode> nodes = this.childrenOfType(FeMergeNode.class);
      this.inputChannels = new FilterChannelKey[nodes.size()];

      for (int i = 0; i < this.inputChannels.length; i++) {
         this.inputChannels[i] = nodes.get(i).inputChannel();
      }

      this.children().clear();
   }

   @Override
   public boolean isValid() {
      return this.inputChannels.length > 0;
   }

   @NotNull
   @Override
   public Length x() {
      return this.filterPrimitiveBase.x;
   }

   @NotNull
   @Override
   public Length y() {
      return this.filterPrimitiveBase.y;
   }

   @NotNull
   @Override
   public Length width() {
      return this.filterPrimitiveBase.width;
   }

   @NotNull
   @Override
   public Length height() {
      return this.filterPrimitiveBase.height;
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      if (this.inputChannels.length == 0) {
         this.filterPrimitiveBase.saveLayoutResult(filterLayoutContext.resultChannels().get(DefaultFilterChannel.SourceGraphic), filterLayoutContext);
      } else {
         LayoutBounds result = filterLayoutContext.resultChannels().get(this.inputChannels[0]);

         for (int i = 1; i < this.inputChannels.length; i++) {
            LayoutBounds channelBounds = filterLayoutContext.resultChannels().get(this.inputChannels[i]);
            result = result.union(channelBounds);
         }

         this.filterPrimitiveBase.saveLayoutResult(result, filterLayoutContext);
      }
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      if (this.inputChannels.length == 0) {
         this.filterPrimitiveBase.saveResult(this.filterPrimitiveBase.channel(DefaultFilterChannel.SourceGraphic, filterContext), filterContext);
      } else {
         Channel in = this.filterPrimitiveBase.channel(this.inputChannels[0], filterContext);
         Channel result = in;
         if (this.inputChannels.length > 1) {
            BufferedImage dst = in.toBufferedImageNonAliased(context);
            Graphics2D imgGraphics = GraphicsUtil.createGraphics(dst);

            for (int i = 1; i < this.inputChannels.length; i++) {
               Channel channel = this.filterPrimitiveBase.channel(this.inputChannels[i], filterContext);
               imgGraphics.drawImage(context.platformSupport().createImage(channel.producer()), null, context.platformSupport().imageObserver());
            }

            result = new ImageProducerChannel(dst.getSource());
         }

         this.filterPrimitiveBase.saveResult(result, filterContext);
      }
   }

   @Override
   public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
      return this.filterPrimitiveBase.colorInterpolation(filterContext);
   }
}

