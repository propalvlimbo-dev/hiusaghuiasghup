package com.github.weisj.jsvg.geometry.path;

import com.github.weisj.jsvg.geometry.mesh.Bezier;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface BezierPathCommand {
   @NotNull
   Bezier createBezier(@NotNull Float var1);
}
