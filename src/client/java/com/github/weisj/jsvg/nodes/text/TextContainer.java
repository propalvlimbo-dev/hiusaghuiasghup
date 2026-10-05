package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.VectorEffect;
import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.text.LengthAdjust;
import com.github.weisj.jsvg.attributes.text.TextAnchor;
import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.container.BaseContainerNode;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.HasShape;
import com.github.weisj.jsvg.nodes.prototype.HasVectorEffects;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasContextImpl;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.context.RenderContextAccessor;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

abstract class TextContainer<T> extends BaseContainerNode<T> implements HasShape, HasContext.ByDelegate, HasVectorEffects {
   protected final List<@NotNull T> children = new ArrayList<>();
   protected boolean isVisible;
   protected AttributeFontSpec fontSpec;
   protected LengthAdjust lengthAdjust;
   protected Length textLength;
   private HasContext context;
   private Set<VectorEffect> vectorEffects;

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.fontSpec = FontParser.parseFontSpec(attributeNode);
      this.lengthAdjust = attributeNode.getEnum("lengthAdjust", LengthAdjust.Spacing);
      this.textLength = attributeNode.getLength("textLength", PercentageDimension.NONE, Length.UNSPECIFIED);
      if (this.textLength.raw() < 0.0F) {
         this.textLength = Length.UNSPECIFIED;
      }

      this.isVisible = Renderable.parseVisibility(attributeNode);
      this.context = HasContextImpl.parse(attributeNode);
      this.vectorEffects = VectorEffect.parse(attributeNode);
   }

   @NotNull
   @Override
   public Set<VectorEffect> vectorEffects() {
      return this.vectorEffects;
   }

   @NotNull
   @Override
   public HasContext contextDelegate() {
      return this.context;
   }

   @Override
   public List<? extends @NotNull T> children() {
      return this.children;
   }

   public boolean isVisible(@NotNull RenderContext context) {
      return this.isVisible;
   }

   @NotNull
   abstract Shape glyphShape(@NotNull RenderContext var1);

   public final TextAnchor textAnchor(@NotNull RenderContext context) {
      return RenderContextAccessor.instance().fontRenderContext(context).textAnchor();
   }

   @NotNull
   @Override
   public final Shape untransformedElementShape(@NotNull RenderContext context, @NotNull HasShape.Box box) {
      Shape shape = this.glyphShape(context);
      switch (box) {
         case BoundingBox:
            return shape;
         case StrokeBox:
            Area area = new Area(shape);
            area.add(new Area(context.stroke(1.0F).createStrokedShape(shape)));
            return area;
         default:
            throw new IllegalStateException("Unexpected value: " + box);
      }
   }

   @NotNull
   @Override
   public Rectangle2D untransformedElementBounds(@NotNull RenderContext context, HasShape.Box box) {
      return this.untransformedElementShape(context, box).getBounds2D();
   }
}

