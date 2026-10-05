package com.github.weisj.jsvg.geometry.util;

import java.awt.geom.IllegalPathStateException;
import java.awt.geom.PathIterator;

public final class ReversePathIterator implements PathIterator {
   private final int windingRule;
   private final double[] coordinates;
   private final int[] segmentTypes;
   private int coordIndex = 0;
   private int segmentIndex = 0;

   public ReversePathIterator(PathIterator original) {
      this(original, original.getWindingRule());
   }

   public ReversePathIterator(PathIterator original, int windingRule) {
      this.windingRule = windingRule;
      double[] coords = new double[16];
      int coordPos = 0;
      int[] segTypes = new int[8];
      int segPos = 0;
      boolean first = true;
      double[] temp = new double[6];

      while (!original.isDone()) {
         if (segPos == segTypes.length) {
            int[] dummy = new int[2 * segPos];
            System.arraycopy(segTypes, 0, dummy, 0, segPos);
            segTypes = dummy;
         }

         int segType = segTypes[segPos++] = original.currentSegment(temp);
         if (first) {
            if (segType != 0) {
               throw new IllegalPathStateException("missing initial moveto in path definition");
            }

            first = false;
         }

         int copy = coordinatesForSegmentType(segType);
         if (copy > 0) {
            if (coordPos + copy > coords.length) {
               double[] dummy = new double[coords.length * 2];
               System.arraycopy(coords, 0, dummy, 0, coords.length);
               coords = dummy;
            }

            for (int c = 0; c < copy; c++) {
               coords[coordPos++] = temp[c];
            }
         }

         original.next();
      }

      this.coordinates = new double[coordPos];

      for (int p = coordPos / 2 - 1; p >= 0; p--) {
         this.coordinates[2 * p] = coords[coordPos - 2 * p - 2];
         this.coordinates[2 * p + 1] = coords[coordPos - 2 * p - 1];
      }

      this.segmentTypes = new int[segPos];
      if (segPos > 0) {
         boolean pendingClose = false;
         int sr = 0;
         this.segmentTypes[sr++] = 0;

         for (int s = segPos - 1; s > 0; s--) {
            switch (segTypes[s]) {
               case 0:
                  if (pendingClose) {
                     pendingClose = false;
                     this.segmentTypes[sr++] = 4;
                  }

                  this.segmentTypes[sr++] = 0;
                  break;
               case 4:
                  pendingClose = true;
                  break;
               default:
                  this.segmentTypes[sr++] = segTypes[s];
            }
         }

         if (pendingClose) {
            this.segmentTypes[sr] = 4;
         }
      }
   }

   @Override
   public int getWindingRule() {
      return this.windingRule;
   }

   private static int coordinatesForSegmentType(int segtype) {
      switch (segtype) {
         case 0:
         case 1:
            return 2;
         case 2:
            return 4;
         case 3:
            return 6;
         default:
            return 0;
      }
   }

   @Override
   public void next() {
      this.coordIndex = this.coordIndex + coordinatesForSegmentType(this.segmentTypes[this.segmentIndex++]);
   }

   @Override
   public boolean isDone() {
      return this.segmentIndex >= this.segmentTypes.length;
   }

   @Override
   public int currentSegment(double[] coords) {
      int segmentType = this.segmentTypes[this.segmentIndex];
      int copy = coordinatesForSegmentType(segmentType);
      if (copy > 0) {
         System.arraycopy(this.coordinates, this.coordIndex, coords, 0, copy);
      }

      return segmentType;
   }

   @Override
   public int currentSegment(float[] coords) {
      int segmentType = this.segmentTypes[this.segmentIndex];
      int copy = coordinatesForSegmentType(segmentType);
      if (copy > 0) {
         for (int c = 0; c < copy; c++) {
            coords[c] = (float)this.coordinates[this.coordIndex + c];
         }
      }

      return segmentType;
   }
}

