package com.github.weisj.jsvg.geometry.mesh;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

public class Bezier {
   @NotNull
   public final Float a;
   @NotNull
   public final Float b;
   @NotNull
   public final Float c;
   @NotNull
   public final Float d;

   public Bezier(@NotNull Float a, @NotNull Float b, @NotNull Float c, @NotNull Float d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
   }

   public void appendTo(@NotNull Path2D p) {
      p.curveTo(this.b.x, this.b.y, this.c.x, this.c.y, this.d.x, this.d.y);
   }

   @NotNull
   public Bezier inverse() {
      return new Bezier(this.d, this.c, this.b, this.a);
   }

   @NotNull
   public Split<Bezier> split() {
      Float ab = GeometryUtil.midPoint(this.a, this.b);
      Float bc = GeometryUtil.midPoint(this.b, this.c);
      Float cd = GeometryUtil.midPoint(this.c, this.d);
      Float abbc = GeometryUtil.midPoint(ab, bc);
      Float bccd = GeometryUtil.midPoint(bc, cd);
      Float abbcbccd = GeometryUtil.midPoint(abbc, bccd);
      return new Split<>(new Bezier(this.a, ab, abbc, abbcbccd), new Bezier(abbcbccd, bccd, cd, this.d));
   }

   @NotNull
   public static Bezier straightLine(Float a, Float b) {
      return new LineBezier(a, b);
   }

   @NotNull
   public static Bezier combine(@NotNull Bezier b1, @NotNull Bezier b2, @NotNull Bezier b3) {
      return new Bezier(
         MeshUtil.p(b1.a.x + b2.a.x - b3.a.x, b1.a.y + b2.a.y - b3.a.y),
         MeshUtil.p(b1.b.x + b2.b.x - b3.b.x, b1.b.y + b2.b.y - b3.b.y),
         MeshUtil.p(b1.c.x + b2.c.x - b3.c.x, b1.c.y + b2.c.y - b3.c.y),
         MeshUtil.p(b1.d.x + b2.d.x - b3.d.x, b1.d.y + b2.d.y - b3.d.y)
      );
   }

   public int estimateStepCount(float scaleX, float scaleY) {
      double steps = Math.max(
         Math.max(GeometryUtil.distanceSquared(this.a, this.b, scaleX, scaleY), GeometryUtil.distanceSquared(this.c, this.d, scaleX, scaleY)),
         Math.max(GeometryUtil.distanceSquared(this.a, this.c, scaleX, scaleY) / 4.0, GeometryUtil.distanceSquared(this.b, this.d, scaleX, scaleY) / 4.0)
      );
      steps *= 18.0;
      steps = Math.max(1.0, steps);
      return (Math.getExponent(steps) + 1) / 2;
   }

   @Override
   public String toString() {
      return "Bezier{a=" + this.a + ", b=" + this.b + ", c=" + this.c + ", d=" + this.d + '}';
   }
}

