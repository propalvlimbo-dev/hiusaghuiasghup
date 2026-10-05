package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.attributes.filter.DefaultFilterChannel;
import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Unit;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;

public final class FilterPrimitiveBase {
   @NotNull
   final Length x;
   @NotNull
   final Length y;
   @NotNull
   final Length width;
   @NotNull
   final Length height;
   @NotNull
   private final FilterChannelKey inputChannel;
   @NotNull
   private final FilterChannelKey resultChannel;
   private final ColorInterpolation colorInterpolation;

   public FilterPrimitiveBase(@NotNull AttributeNode attributeNode) {
      this.x = attributeNode.getLength("x", PercentageDimension.WIDTH, Unit.PERCENTAGE_WIDTH.valueOf(0.0F));
      this.y = attributeNode.getLength("y", PercentageDimension.HEIGHT, Unit.PERCENTAGE_HEIGHT.valueOf(0.0F));
      this.width = attributeNode.getLength("width", PercentageDimension.WIDTH, Unit.PERCENTAGE_WIDTH.valueOf(100.0F));
      this.height = attributeNode.getLength("height", PercentageDimension.HEIGHT, Unit.PERCENTAGE_HEIGHT.valueOf(100.0F));
      this.inputChannel = attributeNode.getFilterChannelKey("in", DefaultFilterChannel.LastResult);
      this.resultChannel = attributeNode.getFilterChannelKey("result", DefaultFilterChannel.LastResult);
      this.colorInterpolation = attributeNode.getEnum("color-interpolation-filters", ColorInterpolation.Inherit);
   }

   public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
      return filterContext.colorInterpolation(this.colorInterpolation);
   }

   @NotNull
   public Channel channel(@NotNull FilterChannelKey key, @NotNull FilterContext context) {
      return context.getChannel(key);
   }

   @NotNull
   public Channel inputChannel(@NotNull FilterContext context) {
      return this.channel(this.inputChannel, context);
   }

   @NotNull
   public LayoutBounds layoutInput(@NotNull FilterLayoutContext context) {
      return context.resultChannels().get(this.inputChannel);
   }

   public void noop(@NotNull FilterContext context) {
      this.saveResult(this.inputChannel(context), context);
   }

   public void saveLayoutResult(@NotNull LayoutBounds outputBounds, @NotNull FilterLayoutContext filterLayoutContext) {
      this.saveResultImpl(outputBounds, filterLayoutContext.resultChannels());
   }

   public void saveResult(@NotNull Channel output, @NotNull FilterContext filterContext) {
      this.saveResultImpl(output, filterContext.resultChannels());
   }

   private <T> void saveResultImpl(@NotNull T value, @NotNull ChannelStorage<T> storage) {
      storage.addResult(this.resultChannel, value);
      if (this.resultChannel != DefaultFilterChannel.LastResult) {
         storage.addResult(DefaultFilterChannel.LastResult, value);
      }
   }
}

