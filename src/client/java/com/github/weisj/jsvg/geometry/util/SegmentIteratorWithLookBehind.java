package com.github.weisj.jsvg.geometry.util;

import com.github.weisj.jsvg.geometry.size.Length;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public final class SegmentIteratorWithLookBehind {
   @NotNull
   private final PathIterator pathIterator;
   private float maxLookBehindLength;
   private float currentLookBehindLength = 0.0F;
   private final ArrayList<SegmentIteratorWithLookBehind.Segment> lookBehind = new ArrayList<>();
   private SegmentIteratorWithLookBehind.Segment currentSegment;
   private final float[] cords = new float[2];
   private float moveToX;
   private float moveToY;
   private int lookBehindCursor = -1;

   public SegmentIteratorWithLookBehind(@NotNull PathIterator pathIterator, float maxLookBehindLength) {
      this.pathIterator = pathIterator;
      this.maxLookBehindLength = maxLookBehindLength;
      this.prepareFirstSegment();
   }

   private void prepareFirstSegment() {
      this.currentSegment = new SegmentIteratorWithLookBehind.Segment(Float.NaN, Float.NaN, Float.NaN, Float.NaN);
      this.moveToNext();
      if (Length.isUnspecified(this.currentSegment.xStart) || Length.isUnspecified(this.currentSegment.yStart)) {
         throw new IllegalStateException("Path iterator did not establish starting position");
      }
   }

   @NotNull
   public PathIterator pathIterator() {
      return this.pathIterator;
   }

   public void setMaxLookBehindLength(float maxLookBehindLength) {
      this.maxLookBehindLength = maxLookBehindLength;
      this.trimLookBehindIfNecessary();
   }

   public float maxLookBehindLength() {
      return this.maxLookBehindLength;
   }

   public boolean hasNext() {
      return this.lookBehindCursor >= 0 || !this.pathIterator.isDone();
   }

   public boolean isDone() {
      return !this.hasNext();
   }

   public boolean hasPrevious() {
      return this.lookBehindCursor < this.lookBehind.size() - 1;
   }

   @NotNull
   public SegmentIteratorWithLookBehind.Segment currentSegment() {
      return this.lookBehindCursor >= 0 ? this.lookBehind.get(this.lookBehind.size() - 1 - this.lookBehindCursor) : this.currentSegment;
   }

   public void moveToPrevious() {
      if (!this.hasPrevious()) {
         throw new IllegalStateException("Can't move back anymore. Maximum capacity is " + this.maxLookBehindLength);
      }

      this.lookBehindCursor++;
   }

   public void moveToNext() {
      if (this.lookBehindCursor >= 0) {
         this.lookBehindCursor--;
      } else {
         SegmentIteratorWithLookBehind.Segment nextSegment = new SegmentIteratorWithLookBehind.Segment(
            this.currentSegment.xEnd, this.currentSegment.yEnd, Float.NaN, Float.NaN
         );

         label32:
         while (!this.pathIterator.isDone()) {
            switch (this.pathIterator.currentSegment(this.cords)) {
               case 0:
                  nextSegment.xStart = this.moveToX = this.cords[0];
                  nextSegment.yStart = this.moveToY = this.cords[1];
                  nextSegment.moveHappened = true;
                  this.pathIterator.next();
                  break;
               case 1:
                  nextSegment.xEnd = this.cords[0];
                  nextSegment.yEnd = this.cords[1];
                  this.pathIterator.next();
                  break label32;
               case 2:
               case 3:
               default:
                  throw new IllegalStateException("Unsupported segment type");
               case 4:
                  nextSegment.xEnd = this.moveToX;
                  nextSegment.yEnd = this.moveToY;
                  this.pathIterator.next();
                  break label32;
            }
         }

         if (Float.isNaN(nextSegment.xEnd) || Float.isNaN(nextSegment.yEnd)) {
            nextSegment.xEnd = nextSegment.xStart;
            nextSegment.yEnd = nextSegment.yStart;
         }

         if (this.maxLookBehindLength > 0.0F) {
            this.lookBehind.add(this.currentSegment);
            this.currentLookBehindLength = this.currentLookBehindLength + (float)this.currentSegment.length();
            this.trimLookBehindIfNecessary();
         }

         this.currentSegment = nextSegment;
         return;
      }
   }

   private void trimLookBehindIfNecessary() {
      while (GeometryUtil.notablyGreater(this.currentLookBehindLength, this.maxLookBehindLength)) {
         SegmentIteratorWithLookBehind.Segment segment = this.lookBehind.get(0);
         double segmentLength = segment.length();
         if (!(this.currentLookBehindLength - segmentLength < this.maxLookBehindLength)) {
            this.currentLookBehindLength -= (float)segmentLength;
            this.lookBehind.remove(0);
            continue;
         }
         break;
      }
   }

   public static final class Segment {
      public float xStart;
      public float yStart;
      public float xEnd;
      public float yEnd;
      public boolean moveHappened;

      private Segment(float xStart, float yStart, float xEnd, float yEnd) {
         this.xStart = xStart;
         this.yStart = yStart;
         this.xEnd = xEnd;
         this.yEnd = yEnd;
      }

      public double length() {
         return GeometryUtil.lineLength(this.xStart, this.yStart, this.xEnd, this.yEnd);
      }

      @Override
      public String toString() {
         return String.format("[%.2f,%.2f] -> [%.2f,%.2f] (moved: %b)", this.xStart, this.yStart, this.xEnd, this.yEnd, this.moveHappened);
      }
   }
}

