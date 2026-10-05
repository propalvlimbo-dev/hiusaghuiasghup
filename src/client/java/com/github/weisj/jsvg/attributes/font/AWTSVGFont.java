package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.text.EmojiGlyph;
import com.github.weisj.jsvg.nodes.text.Glyph;
import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AWTSVGFont implements SVGFont {
   @NotNull
   private final Font font;
   private final FontRenderContext frc = new FontRenderContext(null, true, true);
   private final HashMap<String, Glyph> glyphCache;
   @Nullable
   private LineMetrics lineMetrics;
   private float[] baselineOffsets;
   private float exHeight = Float.NaN;
   private float mathBaseline = Float.NaN;

   public AWTSVGFont(@NotNull Font font) {
      this.font = font;
      this.glyphCache = new HashMap<>();
   }

   @NotNull
   public Font font() {
      return this.font;
   }

   @NotNull
   @Override
   public Glyph codepointGlyph(@NotNull String codepoint) {
      Glyph glyph = this.glyphCache.get(codepoint);
      if (glyph != null) {
         return glyph;
      }

      glyph = this.createGlyph(codepoint.toCharArray());
      this.glyphCache.put(codepoint, glyph);
      return glyph;
   }

   @NotNull
   @Override
   public String family() {
      return this.font.getFamily();
   }

   @Override
   public int size() {
      return this.font.getSize();
   }

   @NotNull
   private LineMetrics lineMetrics() {
      if (this.lineMetrics == null) {
         this.lineMetrics = this.font.getLineMetrics("Ax-", 0, 1, this.frc);
      }

      return this.lineMetrics;
   }

   @Override
   public float effectiveExHeight() {
      if (Length.isUnspecified(this.exHeight)) {
         this.exHeight = (float)this.codepointGlyph("x").glyphOutline().getBounds2D().getHeight();
      }

      return this.exHeight;
   }

   @Override
   public float effectiveEmHeight() {
      return this.font.getSize2D();
   }

   @Override
   public float mathematicalBaseline() {
      if (Length.isUnspecified(this.mathBaseline)) {
         this.mathBaseline = -this.effectiveExHeight() / 2.0F;
      }

      return this.mathBaseline;
   }

   private float[] baselineOffsets() {
      if (this.baselineOffsets == null) {
         this.baselineOffsets = this.lineMetrics().getBaselineOffsets();
      }

      return this.baselineOffsets;
   }

   @Override
   public float hangingBaseline() {
      return this.baselineOffsets()[2];
   }

   @Override
   public float romanBaseline() {
      return this.baselineOffsets()[0];
   }

   @Override
   public float centerBaseline() {
      return this.baselineOffsets()[1];
   }

   @Override
   public float middleBaseline() {
      return (this.romanBaseline() - this.effectiveExHeight()) / 2.0F;
   }

   @Override
   public float textUnderBaseline() {
      return this.lineMetrics().getUnderlineOffset();
   }

   @Override
   public float textOverBaseline() {
      return this.textUnderBaseline() - this.effectiveEmHeight();
   }

   @NotNull
   private Glyph createGlyph(char @NotNull [] codepoint) {
      GlyphVector glyphVector = this.font.createGlyphVector(this.frc, codepoint);
      GlyphMetrics gm = glyphVector.getGlyphMetrics(0);
      float advance = gm.getAdvanceX();
      if (isPossibleEmoji(codepoint)) {
         return new EmojiGlyph(String.valueOf(codepoint), advance);
      }

      Shape shape = glyphVector.getOutline(0.0F, 0.0F);
      return new Glyph(shape, advance, gm.getBounds2D().isEmpty());
   }

   private static boolean isPossibleEmoji(char @NotNull [] codepoint) {
      for (char c : codepoint) {
         if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
            return true;
         }
      }

      return false;
   }
}

