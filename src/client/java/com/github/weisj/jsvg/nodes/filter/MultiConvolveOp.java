package com.github.weisj.jsvg.nodes.filter;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;
import org.jetbrains.annotations.NotNull;

final class MultiConvolveOp implements BufferedImageOp, RasterOp {
   @NotNull
   private final ConvolveOp[] ops;
   @NotNull
   private final ConvolveOp op;

   public MultiConvolveOp(@NotNull ConvolveOp[] ops) {
      if (ops.length == 0) {
         throw new IllegalStateException("Must supply at least one op");
      }

      this.ops = ops;
      this.op = ops[0];
   }

   @Override
   public BufferedImage filter(BufferedImage src, BufferedImage dest) {
      BufferedImage result1 = this.op.filter(src, dest);
      if (this.ops.length == 1) {
         return result1;
      }

      BufferedImage result2 = this.ops[1].filter(result1, null);
      BufferedImage r = result2;

      for (int i = 2; i < this.ops.length; i++) {
         result1 = this.ops[1].filter(result2, result1);
         r = result1;
         result1 = result2;
         result2 = r;
      }

      return r;
   }

   @Override
   public WritableRaster filter(Raster src, WritableRaster dest) {
      WritableRaster result1 = this.op.filter(src, dest);
      if (this.ops.length == 1) {
         return result1;
      }

      WritableRaster result2 = this.ops[1].filter(result1, null);
      WritableRaster r = result2;

      for (int i = 2; i < this.ops.length; i++) {
         result1 = this.ops[1].filter(result2, result1);
         r = result1;
         result1 = result2;
         result2 = r;
      }

      return r;
   }

   @Override
   public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
      return this.op.createCompatibleDestImage(src, destCM);
   }

   @Override
   public WritableRaster createCompatibleDestRaster(Raster src) {
      return this.op.createCompatibleDestRaster(src);
   }

   @Override
   public Rectangle2D getBounds2D(@NotNull BufferedImage src) {
      return this.op.getBounds2D(src);
   }

   @Override
   public Rectangle2D getBounds2D(@NotNull Raster src) {
      return this.op.getBounds2D(src);
   }

   @Override
   public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
      return this.op.getPoint2D(srcPt, dstPt);
   }

   @Override
   public RenderingHints getRenderingHints() {
      return this.op.getRenderingHints();
   }
}

