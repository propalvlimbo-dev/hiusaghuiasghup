package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.ColorInterpolation;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFilterPrimitive extends AbstractSVGNode implements FilterPrimitive {
   private FilterPrimitiveBase filterPrimitiveBase;

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.filterPrimitiveBase = new FilterPrimitiveBase(attributeNode);
   }

   @NotNull
   protected final FilterPrimitiveBase impl() {
      return this.filterPrimitiveBase;
   }

   @NotNull
   @Override
   public Length x() {
      return this.impl().x;
   }

   @NotNull
   @Override
   public Length y() {
      return this.impl().y;
   }

   @NotNull
   @Override
   public Length width() {
      return this.impl().width;
   }

   @NotNull
   @Override
   public Length height() {
      return this.impl().height;
   }

   @Override
   public ColorInterpolation colorInterpolation(@NotNull FilterContext filterContext) {
      return this.impl().colorInterpolation(filterContext);
   }
}

