package com.github.weisj.jsvg.nodes.prototype.impl;

import com.github.weisj.jsvg.attributes.font.AttributeFontSpec;
import com.github.weisj.jsvg.attributes.font.FontParser;
import com.github.weisj.jsvg.attributes.font.MeasurableFontSpec;
import com.github.weisj.jsvg.nodes.prototype.HasContext;
import com.github.weisj.jsvg.nodes.prototype.Mutator;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.impl.context.FontRenderContext;
import com.github.weisj.jsvg.renderer.impl.context.PaintContext;
import org.jetbrains.annotations.NotNull;

public final class HasContextImpl implements HasContext {
   @NotNull
   private final PaintContext paintContext;
   @NotNull
   private final FontRenderContext fontRenderContext;
   @NotNull
   private final AttributeFontSpec fontSpec;

   private HasContextImpl(@NotNull PaintContext paintContext, @NotNull FontRenderContext fontRenderContext, @NotNull AttributeFontSpec fontSpec) {
      this.paintContext = paintContext;
      this.fontRenderContext = fontRenderContext;
      this.fontSpec = fontSpec;
   }

   @NotNull
   public static HasContext parse(@NotNull AttributeNode attributeNode) {
      return new HasContextImpl(PaintContext.parse(attributeNode), FontRenderContext.parse(attributeNode), FontParser.parseFontSpec(attributeNode));
   }

   @NotNull
   @Override
   public Mutator<MeasurableFontSpec> fontSpec() {
      return this.fontSpec;
   }

   @NotNull
   @Override
   public FontRenderContext fontRenderContext() {
      return this.fontRenderContext;
   }

   @NotNull
   @Override
   public Mutator<PaintContext> paintContext() {
      return this.paintContext;
   }
}

