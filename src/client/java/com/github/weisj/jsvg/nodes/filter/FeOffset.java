package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.UnitType;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeOffset extends AbstractFilterPrimitive {
   public static final String TAG = "feOffset";
   private float dx;
   private float dy;

   @NotNull
   @Override
   public String tagName() {
      return "feOffset";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.dx = attributeNode.getFloat("dx", 0.0F);
      this.dy = attributeNode.getFloat("dy", 0.0F);
   }

   private Double offset(@Nullable AffineTransform at, @NotNull UnitType filterPrimitiveUnits, @NotNull Rectangle2D elementBounds) {
      Double off = new Double(this.dx, this.dy);
      if (at != null) {
         off.x = off.x * GeometryUtil.scaleXOfTransform(at);
         off.y = off.y * GeometryUtil.scaleYOfTransform(at);
      }

      if (filterPrimitiveUnits == UnitType.ObjectBoundingBox) {
         off.x = off.x * elementBounds.getWidth();
         off.y = off.y * elementBounds.getHeight();
      }

      return off;
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      LayoutBounds input = this.impl().layoutInput(filterLayoutContext);
      Double off = this.offset(null, filterLayoutContext.primitiveUnits(), filterLayoutContext.elementBounds());
      LayoutBounds result = input.translate((float)off.x, (float)off.y, filterLayoutContext);
      this.impl().saveLayoutResult(result, filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      Channel in = this.impl().inputChannel(filterContext);
      Channel result = in;
      if (this.dx != 0.0F || this.dy != 0.0F) {
         AffineTransform at = filterContext.info().output().transform();
         Double off = this.offset(at, filterContext.primitiveUnits(), filterContext.info().elementBounds());
         AffineTransform transform = AffineTransform.getTranslateInstance(off.x, off.y);
         AffineTransformOp op = new AffineTransformOp(transform, filterContext.renderingHints());
         result = in.applyFilter(new BufferedImageFilter(op));
      }

      this.impl().saveResult(result, filterContext);
   }
}

