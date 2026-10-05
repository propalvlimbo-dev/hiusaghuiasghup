package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.filter.FilterChannelKey;
import com.github.weisj.jsvg.renderer.RenderContext;
import org.jetbrains.annotations.NotNull;

abstract class ChainedFilterPrimitive extends AbstractFilterPrimitive implements FilterPrimitive {
   protected final FilterChannelKey outerLastResult = new ChainedFilterPrimitive.OuterLastResult();

   @NotNull
   protected abstract FilterPrimitive[] primitives();

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      filterLayoutContext.resultChannels().addResult(this.outerLastResult, this.impl().layoutInput(filterLayoutContext));

      for (FilterPrimitive primitive : this.primitives()) {
         primitive.layoutFilter(context, filterLayoutContext);
      }
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      filterContext.resultChannels().addResult(this.outerLastResult, this.impl().inputChannel(filterContext));

      for (FilterPrimitive primitive : this.primitives()) {
         primitive.applyFilter(context, filterContext);
      }
   }

   private static final class OuterLastResult implements FilterChannelKey {
      private final String key = "outer-last-result-" + this.hashCode();

      private OuterLastResult() {
      }

      @NotNull
      @Override
      public Object key() {
         return this.key;
      }
   }
}

