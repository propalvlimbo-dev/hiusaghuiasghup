package com.github.weisj.jsvg.nodes.filter;

import com.github.weisj.jsvg.attributes.Animatable;
import com.github.weisj.jsvg.attributes.Inherited;
import com.github.weisj.jsvg.attributes.filter.LayoutBounds;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.geometry.size.Percentage;
import com.github.weisj.jsvg.nodes.animation.Animate;
import com.github.weisj.jsvg.nodes.animation.Set;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.paint.SVGPaint;
import com.github.weisj.jsvg.paint.impl.AwtSVGPaint;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.impl.Graphics2DOutput;
import com.github.weisj.jsvg.renderer.output.impl.GraphicsUtil;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.jetbrains.annotations.NotNull;

@ElementCategories(Category.FilterPrimitive)
@PermittedContent(anyOf = {Animate.class, Set.class})
public final class FeFlood extends AbstractFilterPrimitive {
   public static final String TAG = "feflood";
   private SVGPaint floodColor;
   private PercentageValue floodOpacity;

   @NotNull
   @Override
   public String tagName() {
      return "feflood";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.floodColor = attributeNode.getPaint("flood-color", new AwtSVGPaint(Color.BLACK), Inherited.NO, Animatable.YES);
      this.floodOpacity = attributeNode.getPercentage("flood-opacity", Percentage.ONE, Inherited.NO, Animatable.YES);
   }

   @Override
   public void layoutFilter(@NotNull RenderContext context, @NotNull FilterLayoutContext filterLayoutContext) {
      LayoutBounds layoutBounds = new LayoutBounds(filterLayoutContext.filterPrimitiveRegion(context.measureContext(), this), new FloatInsets());
      this.impl().saveLayoutResult(layoutBounds, filterLayoutContext);
   }

   @Override
   public void applyFilter(@NotNull RenderContext context, @NotNull FilterContext filterContext) {
      Filter.FilterInfo info = filterContext.info();
      BufferedImage img = new BufferedImage(info.imageWidth, info.imageHeight, 2);
      float opacity = this.floodOpacity.get(context.measureContext());
      if (opacity != 0.0F) {
         Graphics2D graphics = GraphicsUtil.createGraphics(img);
         graphics.setComposite(AlphaComposite.Src.derive(opacity));
         Rectangle rect = new Rectangle(0, 0, img.getWidth(), img.getHeight());
         this.floodColor.fillShape(new Graphics2DOutput(graphics), context, rect, rect);
         graphics.dispose();
      }

      this.impl().saveResult(new ImageProducerChannel(img.getSource()), filterContext);
   }
}

