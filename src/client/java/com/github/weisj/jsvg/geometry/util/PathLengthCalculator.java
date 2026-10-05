package com.github.weisj.jsvg.geometry.util;

public final class PathLengthCalculator {
   private double x = 0.0;
   private double y = 0.0;
   private double xStart = this.x;
   private double yStart = this.y;

   public double segmentLength(int segmentType, double[] coords) {
      double segmentLength = 0.0;
      switch (segmentType) {
         case 0:
            this.x = coords[0];
            this.y = coords[1];
            this.xStart = this.x;
            this.yStart = this.y;
            break;
         case 1:
            segmentLength = this.lineLength(this.x, this.y, coords[0], coords[1]);
            this.x = coords[0];
            this.y = coords[1];
            break;
         case 2:
            segmentLength = this.quadraticParametricLength(this.x, this.y, coords[0], coords[1], coords[2], coords[3]);
            this.x = coords[2];
            this.y = coords[3];
            break;
         case 3:
            segmentLength = this.cubicParametricLength(this.x, this.y, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
            this.x = coords[4];
            this.y = coords[5];
            break;
         case 4:
            segmentLength = this.lineLength(this.x, this.y, this.xStart, this.yStart);
            this.x = this.xStart;
            this.y = this.yStart;
            break;
         default:
            throw new IllegalStateException();
      }

      return segmentLength;
   }

   private double lineLength(double x1, double y1, double x2, double y2) {
      return GeometryUtil.lineLength(x1, y1, x2, y2);
   }

   private double quadraticParametricLength(double ax, double ay, double bx, double by, double cx, double cy) {
      if (ax == cx && ay == cy) {
         return ax == bx && ay == by ? 0.0 : this.lineLength(ax, ay, bx, by);
      }

      if ((ax != bx || ay != by) && (cx != bx || cy != by)) {
         double ax0 = bx - ax;
         double ay0 = by - ay;
         double ax1 = ax - 2.0 * bx + cx;
         double ay1 = ay - 2.0 * by + cy;
         if (ax1 == 0.0 && ay1 == 0.0) {
            return 2.0 * this.lineLength(0.0, 0.0, ax0, ay0);
         }

         double c = 4.0 * this.dot2D(ax1, ay1, ax1, ay1);
         double b = 8.0 * this.dot2D(ax0, ay0, ax1, ay1);
         double a = 4.0 * this.dot2D(ax0, ay0, ax0, ay0);
         double q = 4.0 * a * c - b * b;
         double twoCpB = 2.0 * c + b;
         double sumCBA = c + b + a;
         double l0 = 0.25 / c * (twoCpB * Math.sqrt(sumCBA) - b * Math.sqrt(a));
         if (q == 0.0) {
            return l0;
         }

         double l1 = q / (8.0 * Math.pow(c, 1.5)) * (Math.log(2.0 * Math.sqrt(c * sumCBA) + twoCpB) - Math.log(2.0 * Math.sqrt(c * a) + b));
         return l0 + l1;
      } else {
         return this.lineLength(ax, ay, cx, cy);
      }
   }

   private double dot2D(double x1, double y1, double x2, double y2) {
      return x1 * x2 + y1 * y2;
   }

   private double cubicParametricLength(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
      double[] nodes = new double[]{
         -0.9815606342467192,
         -0.9041172563704749,
         -0.7699026741943047,
         -0.5873179542866175,
         -0.3678314989981802,
         -0.1252334085114689,
         0.1252334085114689,
         0.3678314989981802,
         0.5873179542866175,
         0.7699026741943047,
         0.9041172563704749,
         0.9815606342467192
      };
      double[] weights = new double[]{
         0.0471753363865118,
         0.1069393259953184,
         0.1600783285433462,
         0.2031674267230659,
         0.2334925365383548,
         0.2491470458134028,
         0.2491470458134028,
         0.2334925365383548,
         0.2031674267230659,
         0.1600783285433462,
         0.1069393259953184,
         0.0471753363865118
      };
      double a1x = bx - ax;
      double a1y = by - ay;
      double a2x = cx - bx;
      double a2y = cy - by;
      double a3x = dx - cx;
      double a3y = dy - cy;
      double sum = 0.0;

      for (int i = 0; i < nodes.length; i++) {
         double t = (nodes[i] + 1.0) * 0.5;
         double mt = 1.0 - t;
         double dtx = a1x * mt * mt + 2.0 * a2x * mt * t + a3x * t * t;
         double dty = a1y * mt * mt + 2.0 * a2y * mt * t + a3y * t * t;
         sum += weights[i] * Math.sqrt(dtx * dtx + dty * dty);
      }

      return 1.5 * sum;
   }
}

