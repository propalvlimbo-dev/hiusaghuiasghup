package com.github.weisj.jsvg.geometry.mesh;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

final class LineBezier extends Bezier {
   LineBezier(@NotNull Float a, @NotNull Float b) {
      super(a, GeometryUtil.lerp(0.33333334F, b, a), GeometryUtil.lerp(0.6666667F, b, a), b);
   }

   @Override
   public void appendTo(@NotNull Path2D p) {
      p.lineTo(this.d.x, this.d.y);
   }

   @NotNull
   @Override
   public Split<Bezier> split() {
      Float mid = GeometryUtil.midPoint(this.a, this.d);
      return new Split<>(new LineBezier(this.a, mid), new LineBezier(mid, this.d));
   }
}

