package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.renderer.RenderContext;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class DummyFilterPrimitive extends AbstractFilterPrimitive {
   @NotNull
   private final String tag;

   public DummyFilterPrimitive(@NotNull String tag) {
      this.tag = tag;
   }

   @NotNull
   @Override
   public String tagName() {
      return this.tag;
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      this.impl().saveLayoutResult(this.impl().layoutInput(filterLayoutContext), filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      this.impl().noop(filterContext);
   }
}

