package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class GlyphCursor {
   @NotNull
   public final AbstractGlyphRun.Metrics completeGlyphRunMetrics;
   @NotNull
   protected GlyphAdvancement advancement = GlyphAdvancement.defaultAdvancement();
   protected float x;
   protected float y;
   protected int glyphOffset;
   protected final AffineTransform transform;
   protected Length[] xLocations;
   protected int xOff;
   protected Length[] xDeltas;
   protected int dxOff;
   protected Length[] yLocations;
   protected int yOff;
   protected Length[] yDeltas;
   protected int dyOff;
   protected float[] rotations;
   protected int rotOff;

   GlyphCursor(float x, float y, @NotNull AffineTransform transform) {
      this(x, y, transform, AbstractGlyphRun.Metrics.createDefault());
   }

   private GlyphCursor(float x, float y, @NotNull AffineTransform transform, @NotNull AbstractGlyphRun.Metrics metrics) {
      this.x = x;
      this.y = y;
      this.transform = transform;
      this.completeGlyphRunMetrics = metrics;
      this.glyphOffset = 0;
   }

   GlyphCursor(@NotNull GlyphCursor c) {
      this(c.x, c.y, c.transform, c.completeGlyphRunMetrics);
      this.advancement = c.advancement;
      this.glyphOffset = 0;
      this.xLocations = c.xLocations;
      this.xOff = c.xOff;
      this.yLocations = c.yLocations;
      this.yOff = c.yOff;
      this.xDeltas = c.xDeltas;
      this.dxOff = c.dxOff;
      this.yDeltas = c.yDeltas;
      this.dyOff = c.dyOff;
      this.rotations = c.rotations;
      this.rotOff = c.rotOff;
   }

   GlyphCursor derive() {
      return new GlyphCursor(this);
   }

   void updateFrom(GlyphCursor local) {
      this.x = local.x;
      this.y = local.y;
   }

   public void setAdvancement(@NotNull GlyphAdvancement advancement) {
      this.advancement = advancement;
   }

   public GlyphAdvancement advancement() {
      return this.advancement;
   }

   @NotNull
   Float currentLocation(@NotNull MeasureContext measure) {
      return new Float(this.currentX(measure) + this.currentXDelta(measure), this.currentY(measure) + this.currentYDelta(measure));
   }

   @Nullable
   AffineTransform advance(@NotNull MeasureContext measure, @NotNull Glyph glyph) {
      this.x = this.nextX(measure);
      this.x = this.x + this.nextDeltaX(measure);
      this.y = this.nextY(measure);
      this.y = this.y + this.nextDeltaY(measure);
      this.transform.setToTranslation(this.x, this.y);
      double rotation = this.nextRotation();
      if (rotation != 0.0) {
         this.transform.rotate(rotation);
      }

      this.glyphOffset++;
      this.x = this.x + this.advancement.glyphAdvancement(glyph);
      return this.advancement.glyphTransform(this.transform);
   }

   void advanceSpacing(float letterSpacing) {
      this.x = this.x + this.advancement.spacingAdvancement(letterSpacing);
   }

   private boolean currentGlyphHasXOverride() {
      return this.xLocations != null && this.xOff < this.xLocations.length;
   }

   private float currentX(@NotNull MeasureContext measure) {
      return this.currentGlyphHasXOverride() ? this.xLocations[this.xOff].resolve(measure) : this.x;
   }

   private float currentXDelta(@NotNull MeasureContext measure) {
      return this.currentGlyphHasDeltaXOverride() ? this.xDeltas[this.dxOff].resolve(measure) : 0.0F;
   }

   private float currentY(@NotNull MeasureContext measure) {
      return this.currentGlyphHasYOverride() ? this.yLocations[this.yOff].resolve(measure) : this.y;
   }

   private float currentYDelta(@NotNull MeasureContext measure) {
      return this.currentGlyphHasDeltaYOverride() ? this.yDeltas[this.dyOff].resolve(measure) : 0.0F;
   }

   private float nextX(@NotNull MeasureContext measure) {
      if (this.currentGlyphHasXOverride()) {
         this.x = this.xLocations[this.xOff].resolve(measure);
         this.xOff++;
      }

      return this.x;
   }

   private boolean currentGlyphHasDeltaXOverride() {
      return this.xDeltas != null && this.dxOff < this.xDeltas.length;
   }

   protected final float nextDeltaX(@NotNull MeasureContext measure) {
      return this.currentGlyphHasDeltaXOverride() ? this.xDeltas[this.dxOff++].resolve(measure) : 0.0F;
   }

   private boolean currentGlyphHasYOverride() {
      return this.yLocations != null && this.yOff < this.yLocations.length;
   }

   private float nextY(@NotNull MeasureContext measure) {
      if (this.currentGlyphHasYOverride()) {
         this.y = this.yLocations[this.yOff].resolve(measure);
         this.yOff++;
      }

      return this.y;
   }

   private boolean currentGlyphHasDeltaYOverride() {
      return this.yDeltas != null && this.dyOff < this.yDeltas.length;
   }

   protected final float nextDeltaY(@NotNull MeasureContext measure) {
      return this.currentGlyphHasDeltaYOverride() ? this.yDeltas[this.dyOff++].resolve(measure) : 0.0F;
   }

   private boolean currentGlyphHasRotationOverride() {
      return this.rotations != null && this.rotations.length != 0;
   }

   private double nextRotation() {
      if (this.currentGlyphHasRotationOverride()) {
         float rotation = this.rotations[this.rotOff];
         this.rotOff = Math.min(this.rotations.length - 1, this.rotOff + 1);
         return Math.toRadians(rotation);
      } else {
         return 0.0;
      }
   }

   boolean isCurrentGlyphAutoLayout() {
      return !this.currentGlyphHasXOverride()
         && !this.currentGlyphHasDeltaXOverride()
         && !this.currentGlyphHasYOverride()
         && !this.currentGlyphHasDeltaYOverride()
         && (!this.currentGlyphHasRotationOverride() || this.rotations[this.rotOff] % 360.0F == 0.0F);
   }

   boolean shouldRenderCurrentGlyph() {
      return true;
   }
}

