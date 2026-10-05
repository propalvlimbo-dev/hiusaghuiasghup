package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.geometry.util.SegmentIteratorWithLookBehind;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PathGlyphCursor extends GlyphCursor {
   private float remainingSegmentLength;
   private float segmentLength;
   private float retainedLengthAtStart;
   private boolean shouldRenderCurrentGlyph;
   private SegmentIteratorWithLookBehind.Segment currentSegment;
   @NotNull
   private final SegmentIteratorWithLookBehind segmentIterator;

   PathGlyphCursor(@NotNull PathIterator pathIterator, float startOffset) {
      super(0.0F, 0.0F, new AffineTransform());
      this.segmentIterator = new SegmentIteratorWithLookBehind(pathIterator, 0.0F);
      this.setupInitialData();
      this.advance(startOffset);
   }

   PathGlyphCursor(@NotNull GlyphCursor cursor, @NotNull PathIterator pathIterator, float startOffset) {
      super(cursor);
      this.segmentIterator = new SegmentIteratorWithLookBehind(pathIterator, 0.0F);
      this.setupInitialData();
      this.advance(startOffset);
   }

   @NotNull
   PathIterator pathIterator() {
      return this.segmentIterator.pathIterator();
   }

   private void setupInitialData() {
      this.currentSegment = this.segmentIterator.currentSegment();
      this.segmentLength = this.remainingSegmentLength = (float)this.currentSegment.length();
      this.x = this.currentSegment.xStart;
      this.y = this.currentSegment.yStart;
   }

   private PathGlyphCursor(@NotNull PathGlyphCursor pathCursor) {
      super(pathCursor);
      this.segmentIterator = pathCursor.segmentIterator;
      this.remainingSegmentLength = pathCursor.remainingSegmentLength;
      this.segmentLength = pathCursor.segmentLength;
      this.currentSegment = pathCursor.currentSegment;
   }

   @Override
   GlyphCursor derive() {
      return new PathGlyphCursor(this);
   }

   @Override
   void updateFrom(GlyphCursor local) {
      super.updateFrom(local);
      assert local instanceof PathGlyphCursor;
      PathGlyphCursor glyphCursor = (PathGlyphCursor)local;
      this.remainingSegmentLength = glyphCursor.remainingSegmentLength;
      this.segmentLength = glyphCursor.segmentLength;
      this.currentSegment = glyphCursor.currentSegment;
   }

   @Override
   public void setAdvancement(@NotNull GlyphAdvancement advancement) {
      super.setAdvancement(advancement);
      this.segmentIterator.setMaxLookBehindLength(Math.max(advancement.maxLookBehind(), this.segmentIterator.maxLookBehindLength()));
   }

   @NotNull
   @Override
   Float currentLocation(@NotNull MeasureContext measure) {
      return new Float(this.x, this.y);
   }

   @Nullable
   @Override
   AffineTransform advance(@NotNull MeasureContext measure, @NotNull Glyph glyph) {
      if (this.segmentIterator.isDone() && GeometryUtil.approximatelyNegative(this.remainingSegmentLength)) {
         return null;
      }

      float deltaX = this.nextDeltaX(measure);
      if (deltaX != 0.0F) {
         this.advance(deltaX);
      }

      float advanceDist = this.advancement.glyphAdvancement(glyph);
      float halfAdvance = advanceDist / 2.0F;
      this.advance(halfAdvance);
      float walkedFraction = halfAdvance / this.segmentLength;
      float slopeX = walkedFraction * (this.currentSegment.xEnd - this.currentSegment.xStart);
      float slopeY = walkedFraction * (this.currentSegment.yEnd - this.currentSegment.yStart);
      float anchorX = this.x - slopeX;
      float anchorY = this.y - slopeY;
      this.shouldRenderCurrentGlyph = GeometryUtil.approximatelyNegative(this.retainedLengthAtStart);
      if (this.segmentIterator.isDone() && GeometryUtil.approximatelyNegative(this.remainingSegmentLength)) {
         return null;
      }

      this.advance(halfAdvance);
      this.transform.setToTranslation(anchorX, anchorY);
      float charRotation = this.calculateSegmentRotation(anchorX, anchorY, this.x + slopeX, this.y + slopeY);
      this.transform.rotate(charRotation, 0.0, 0.0);
      float deltaY = this.nextDeltaY(measure);
      if (deltaY != 0.0F) {
         float nx = -(this.y - anchorX);
         float ny = this.x - anchorY;
         float nn = deltaY / this.norm(nx, ny);
         this.transform.translate(nx * nn, ny * nn);
      }

      return this.advancement.glyphTransform(this.transform);
   }

   @Override
   void advanceSpacing(float letterSpacing) {
      this.advance(this.advancement.spacingAdvancement(letterSpacing));
   }

   @Override
   boolean shouldRenderCurrentGlyph() {
      return this.shouldRenderCurrentGlyph;
   }

   private void advance(float distance) {
      if (distance >= 0.0F) {
         this.advanceInsideSegment(this.advanceIntoSegment(this.adjustForRetainedLength(distance)));
      } else {
         this.advanceInsideSegment(-this.reverseIntoSegment(-distance));
      }
   }

   private float travelledSegmentLength() {
      return this.segmentLength - this.remainingSegmentLength;
   }

   private float adjustForRetainedLength(float distance) {
      if (distance > 0.0F && GeometryUtil.approximatelyPositive(this.retainedLengthAtStart)) {
         float delta = Math.min(distance, this.retainedLengthAtStart);
         this.retainedLengthAtStart -= delta;
         return distance - delta;
      } else {
         return distance;
      }
   }

   private float advanceIntoSegment(float distance) {
      if (GeometryUtil.approximatelyNegative(distance)) {
         return 0.0F;
      }

      while (this.segmentIterator.hasNext() && this.remainingSegmentLength < distance) {
         distance -= this.remainingSegmentLength;
         this.segmentIterator.moveToNext();
         this.currentSegment = this.segmentIterator.currentSegment();
         this.x = this.currentSegment.xStart;
         this.y = this.currentSegment.yStart;
         this.segmentLength = (float)this.currentSegment.length();
         this.remainingSegmentLength = this.segmentLength;
      }

      return distance;
   }

   private float reverseIntoSegment(float distance) {
      if (GeometryUtil.approximatelyNegative(distance)) {
         return 0.0F;
      }

      while (this.segmentIterator.hasPrevious() && this.travelledSegmentLength() < distance) {
         distance -= this.travelledSegmentLength();
         this.segmentIterator.moveToPrevious();
         this.currentSegment = this.segmentIterator.currentSegment();
         this.x = this.currentSegment.xEnd;
         this.y = this.currentSegment.yEnd;
         this.segmentLength = (float)this.currentSegment.length();
         this.remainingSegmentLength = 0.0F;
      }

      return distance;
   }

   private void advanceInsideSegment(float distance) {
      if (!GeometryUtil.approximatelyZero(distance)) {
         if (distance < 0.0F && -distance > this.travelledSegmentLength()) {
            this.retainedLengthAtStart += -1.0F * distance;
         } else {
            float fractionWalked = distance / this.segmentLength;
            this.x = this.x + (this.currentSegment.xEnd - this.currentSegment.xStart) * fractionWalked;
            this.y = this.y + (this.currentSegment.yEnd - this.currentSegment.yStart) * fractionWalked;
            this.remainingSegmentLength -= distance;
         }
      }
   }

   private float calculateSegmentRotation(float x1, float y1, float x2, float y2) {
      return (float)Math.atan2(y2 - y1, x2 - x1);
   }

   private float norm(float a, float b) {
      return (float)Math.sqrt(a * a + b * b);
   }
}

