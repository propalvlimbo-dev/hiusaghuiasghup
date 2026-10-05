package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.nodes.Anchor;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.HasGeometryContext;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.impl.HasGeometryContextImpl;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.impl.NodeRenderer;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.AttributeUtil;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D.Float;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories({Category.Graphic, Category.TextContent})
@PermittedContent(categories = {Category.Animation, Category.Descriptive, Category.TextContentChild}, anyOf = Anchor.class, charData = true)
public final class Text extends LinearTextContainer<TextLayoutGroup> implements HasGeometryContext.ByDelegate, Renderable {
   public static final String TAG = "text";
   private HasGeometryContext geometryContext;

   @NotNull
   @Override
   public String tagName() {
      return "text";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.geometryContext = HasGeometryContextImpl.parse(attributeNode);
   }

   @NotNull
   @Override
   public HasGeometryContext geometryContextDelegate() {
      return this.geometryContext;
   }

   @Override
   public boolean isVisible(@NotNull RenderContext context) {
      return this.isVisible;
   }

   @Override
   protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
      return node instanceof TextSegment || node instanceof TextLayoutGroup;
   }

   @Override
   protected void doAdd(@NotNull SVGNode node) {
      if (node instanceof TextSegment) {
         this.lastLinearLayoutGroup().segments().add((TextSegment)node);
      }

      if (node instanceof TextLayoutGroup) {
         this.children.add((TextLayoutGroup)node);
      }
   }

   @Override
   public void addContent(@NotNull TextContent.Segment content) {
      if (!content.isConstant() || !content.text().isEmpty()) {
         if (!this.children.isEmpty() || !content.isConstant() || !AttributeUtil.isBlank(content.text())) {
            LinearTextLayoutGroup linearTextLayoutGroup = this.lastLinearLayoutGroup();
            linearTextLayoutGroup.segments().add(new StringTextSegment(this, linearTextLayoutGroup, linearTextLayoutGroup.segments().size(), content));
         }
      }
   }

   @NotNull
   private LinearTextLayoutGroup lastLinearLayoutGroup() {
      TextLayoutGroup lastGroup = this.children.isEmpty() ? null : this.children.get(this.children.size() - 1);
      if (!(lastGroup instanceof LinearTextLayoutGroup)) {
         lastGroup = new LinearTextLayoutGroup(this);
         this.children.add(lastGroup);
      }

      return (LinearTextLayoutGroup)lastGroup;
   }

   @Override
   public void render(@NotNull RenderContext context, @NotNull Output output) {
      Point2D start = null;

      for (TextLayoutGroup layoutGroup : this.children()) {
         RenderContext currentContext = context;
         if (layoutGroup instanceof TextContainer) {
            currentContext = NodeRenderer.setupRenderContext(layoutGroup, context);
         }

         start = layoutGroup.renderText(start, currentContext, output);
      }
   }

   @NotNull
   @Override
   protected Shape glyphShape(@NotNull RenderContext context) {
      Path2D shape = new Float();
      Point2D start = null;

      for (TextLayoutGroup layoutGroup : this.children()) {
         start = layoutGroup.appendGlyphShape(start, context, shape);
      }

      return shape;
   }

   @NotNull
   @Override
   public GlyphCursor createLocalCursor(boolean isInitial, @NotNull GlyphCursor current) {
      return !isInitial ? current : super.createLocalCursor(isInitial, current);
   }
}

