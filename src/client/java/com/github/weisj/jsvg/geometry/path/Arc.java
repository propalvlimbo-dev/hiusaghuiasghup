package com.github.weisj.jsvg.geometry.path;

import com.github.weisj.jsvg.util.ShapeUtil;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.Arc2D.Double;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class Arc extends PathCommand {
   private final float rx;
   private final float ry;
   private final float xAxisRot;
   private final boolean largeArc;
   private final boolean sweep;
   private final float x;
   private final float y;

   public Arc(boolean isRelative, float rx, float ry, float xAxisRot, boolean largeArc, boolean sweep, float x, float y) {
      super(isRelative, 6);
      this.rx = rx;
      this.ry = ry;
      this.xAxisRot = xAxisRot;
      this.largeArc = largeArc;
      this.sweep = sweep;
      this.x = x;
      this.y = y;
   }

   @Override
   public void appendPath(@NotNull Path2D path, @NotNull BuildHistory hist) {
      Float offset = this.offset(hist);
      this.arcTo(path, this.rx, this.ry, this.xAxisRot, this.largeArc, this.sweep, this.x + offset.x, this.y + offset.y, hist.lastPoint.x, hist.lastPoint.y);
      hist.setLast(path.getCurrentPoint());
   }

   private void arcTo(@NotNull Path2D path, float rx, float ry, float angle, boolean largeArcFlag, boolean sweepFlag, float x, float y, float x0, float y0) {
      if (rx == 0.0F || ry == 0.0F) {
         path.lineTo(x, y);
      } else if (x0 != x || y0 != y) {
         Arc2D arc = computeRawArc(x0, y0, rx, ry, angle, largeArcFlag, sweepFlag, x, y);
         AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(angle), arc.getCenterX(), arc.getCenterY());
         Shape s = ShapeUtil.transformShape(arc, t);
         path.append(s, true);
      }
   }

   @NotNull
   private static Arc2D computeRawArc(double x0, double y0, double rx, double ry, double angle, boolean largeArcFlag, boolean sweepFlag, double x, double y) {
      double dx2 = (x0 - x) / 2.0;
      double dy2 = (y0 - y) / 2.0;
      angle = Math.toRadians(angle % 360.0);
      double cosAngle = Math.cos(angle);
      double sinAngle = Math.sin(angle);
      double x1 = cosAngle * dx2 + sinAngle * dy2;
      double y1 = -sinAngle * dx2 + cosAngle * dy2;
      rx = Math.abs(rx);
      ry = Math.abs(ry);
      double pRx = rx * rx;
      double pRy = ry * ry;
      double pX1 = x1 * x1;
      double pY1 = y1 * y1;
      double radiiCheck = pX1 / pRx + pY1 / pRy;
      if (radiiCheck > 1.0) {
         rx = Math.sqrt(radiiCheck) * rx;
         ry = Math.sqrt(radiiCheck) * ry;
         pRx = rx * rx;
         pRy = ry * ry;
      }

      double sign = largeArcFlag == sweepFlag ? -1.0 : 1.0;
      double sq = (pRx * pRy - pRx * pY1 - pRy * pX1) / (pRx * pY1 + pRy * pX1);
      sq = sq < 0.0 ? 0.0 : sq;
      double coefficient = sign * Math.sqrt(sq);
      double cx1 = coefficient * (rx * y1 / ry);
      double cy1 = coefficient * -(ry * x1 / rx);
      double sx2 = (x0 + x) / 2.0;
      double sy2 = (y0 + y) / 2.0;
      double cx = sx2 + (cosAngle * cx1 - sinAngle * cy1);
      double cy = sy2 + (sinAngle * cx1 + cosAngle * cy1);
      double ux = (x1 - cx1) / rx;
      double uy = (y1 - cy1) / ry;
      double vx = (-x1 - cx1) / rx;
      double vy = (-y1 - cy1) / ry;
      double n = Math.sqrt(ux * ux + uy * uy);
      double p = ux;
      sign = uy < 0.0 ? -1.0 : 1.0;
      double angleStart = Math.toDegrees(sign * Math.acos(p / n));
      n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
      p = ux * vx + uy * vy;
      sign = ux * vy - uy * vx < 0.0 ? -1.0 : 1.0;
      double angleExtent = Math.toDegrees(sign * Math.acos(p / n));
      if (!sweepFlag && angleExtent > 0.0) {
         angleExtent -= 360.0;
      } else if (sweepFlag && angleExtent < 0.0) {
         angleExtent += 360.0;
      }

      angleExtent %= 360.0;
      angleStart %= 360.0;
      Double arc = new Double();
      arc.x = cx - rx;
      arc.y = cy - ry;
      arc.width = rx * 2.0;
      arc.height = ry * 2.0;
      arc.start = -angleStart;
      arc.extent = -angleExtent;
      return arc;
   }

   @Override
   public String toString() {
      return "A " + this.rx + " " + this.ry + " " + this.xAxisRot + " " + this.largeArc + " " + this.sweep + " " + this.x + " " + this.y;
   }
}

