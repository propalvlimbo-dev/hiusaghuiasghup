package com.github.weisj.jsvg.geometry.util;

import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.FloatInsets;
import com.github.weisj.jsvg.renderer.RenderContext;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Point2D.Float;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GeometryUtil {
   private static final float EPS = 1.0E-4F;

   private GeometryUtil() {
   }

   public static boolean approximatelyEqual(double a, double b) {
      return Math.abs(a - b) < 1.0E-4F;
   }

   public static boolean approximatelyZero(double a) {
      return approximatelyEqual(a, 0.0);
   }

   public static boolean notablyGreater(double a, double b) {
      return a - b > 1.0E-4F;
   }

   public static boolean approximatelyNegative(double a) {
      return a < 1.0E-4F;
   }

   public static boolean approximatelyPositive(double a) {
      return a > -1.0E-4F;
   }

   public static double scaleXOfTransform(@Nullable AffineTransform at) {
      if (at == null) {
         return 1.0;
      }

      double sx = at.getScaleX();
      double shy = at.getShearY();
      return Math.sqrt(sx * sx + shy * shy);
   }

   public static double scaleYOfTransform(@Nullable AffineTransform at) {
      if (at == null) {
         return 1.0;
      }

      double sy = at.getScaleY();
      double shx = at.getShearX();
      return Math.sqrt(sy * sy + shx * shx);
   }

   @NotNull
   public static Float midPoint(@NotNull Float x, @NotNull Float y) {
      return new Float((x.x + y.x) / 2.0F, (x.y + y.y) / 2.0F);
   }

   @NotNull
   public static Float lerp(float t, @NotNull Float a, @NotNull Float b) {
      return new Float(lerp(t, b.x, a.x), lerp(t, b.y, a.y));
   }

   public static float lerp(float t, float a, float b) {
      return (1.0F - t) * a + t * b;
   }

   public static double lerp(float t, double a, double b) {
      return (1.0F - t) * a + t * b;
   }

   public static double distanceSquared(@NotNull Float p1, @NotNull Float p2, float scaleX, float scaleY) {
      return distanceSquared(scaleX * p1.x, scaleY * p1.y, scaleX * p2.x, scaleY * p2.y);
   }

   public static double distanceSquared(@NotNull Float p1, @NotNull Float p2) {
      return distanceSquared(p1.x, p1.y, p2.x, p2.y);
   }

   public static double distanceSquared(double x1, double y1, double x2, double y2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      return dx * dx + dy * dy;
   }

   public static double pathLength(@NotNull Shape shape) {
      return pathLength(shape.getPathIterator(null));
   }

   private static double pathLength(@NotNull PathIterator pathIterator) {
      PathLengthCalculator pathLengthCalculator = new PathLengthCalculator();
      double length = 0.0;
      double[] args = new double[6];

      while (!pathIterator.isDone()) {
         length += pathLengthCalculator.segmentLength(pathIterator.currentSegment(args), args);
         pathIterator.next();
      }

      return length;
   }

   public static boolean isSingleClosedPath(@NotNull Shape shape) {
      PathIterator pathIterator = shape.getPathIterator(null);
      int numPaths = 0;
      int numClosedPaths = 0;
      float[] segment = new float[6];

      while (!pathIterator.isDone()) {
         switch (pathIterator.currentSegment(segment)) {
            case 0:
               numPaths++;
               break;
            case 4:
               numClosedPaths++;
         }

         if (numPaths > 1 || numClosedPaths > 1) {
            return false;
         }

         pathIterator.next();
      }

      return numPaths == 1 && numClosedPaths == 1;
   }

   public static double lineLength(double x1, double y1, double x2, double y2) {
      return Math.sqrt(distanceSquared(x1, y1, x2, y2));
   }

   @NotNull
   public static Rectangle2D containingBoundsAfterTransform(@NotNull AffineTransform transform, @NotNull Rectangle2D rect) {
      if (transform.isIdentity()) {
         return rect.getBounds2D();
      }

      Double p1 = new Double(rect.getX(), rect.getY());
      Double p2 = new Double(rect.getX() + rect.getWidth(), rect.getY());
      Double p3 = new Double(rect.getX(), rect.getY() + rect.getHeight());
      Double p4 = new Double(rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight());
      Rectangle2D r1 = rect.getBounds2D();
      r1.setFrameFromDiagonal(transform.transform(p1, p1), transform.transform(p2, p2));
      Rectangle2D r2 = rect.getBounds2D();
      r2.setFrameFromDiagonal(transform.transform(p3, p3), transform.transform(p4, p4));
      Rectangle2D.union(r1, r2, r1);
      return r1;
   }

   public static float left(@NotNull Rectangle2D rect) {
      return (float)rect.getX();
   }

   public static float top(@NotNull Rectangle2D rect) {
      return (float)rect.getY();
   }

   public static float right(@NotNull Rectangle2D rect) {
      return (float)(rect.getX() + rect.getWidth());
   }

   public static float bottom(@NotNull Rectangle2D rect) {
      return (float)(rect.getY() + rect.getHeight());
   }

   @NotNull
   public static Rectangle2D grow(@NotNull Rectangle2D bounds, FloatInsets grow) {
      return new java.awt.geom.Rectangle2D.Double(
         bounds.getX() - grow.left(),
         bounds.getY() - grow.top(),
         bounds.getWidth() + grow.left() + grow.right(),
         bounds.getHeight() + grow.top() + grow.bottom()
      );
   }

   @NotNull
   public static Rectangle2D grow(@NotNull Rectangle2D bounds, double increase) {
      return new java.awt.geom.Rectangle2D.Double(
         bounds.getX() - increase, bounds.getY() - increase, bounds.getWidth() + 2.0 * increase, bounds.getHeight() + 2.0 * increase
      );
   }

   @NotNull
   public static FloatInsets max(@NotNull FloatInsets in1, @NotNull FloatInsets in2) {
      return new FloatInsets(
         Math.max(in1.top(), in2.top()), Math.max(in1.left(), in2.left()), Math.max(in1.bottom(), in2.bottom()), Math.max(in1.right(), in2.right())
      );
   }

   @NotNull
   public static FloatInsets min(@NotNull FloatInsets in1, @NotNull FloatInsets in2) {
      return new FloatInsets(
         Math.min(in1.top(), in2.top()), Math.min(in1.left(), in2.left()), Math.min(in1.bottom(), in2.bottom()), Math.min(in1.right(), in2.right())
      );
   }

   @NotNull
   public static FloatInsets overhangInsets(@NotNull Rectangle2D reference, @NotNull Rectangle2D bounds) {
      return new FloatInsets(
         Math.max(0.0F, top(reference) - top(bounds)),
         Math.max(0.0F, left(reference) - left(bounds)),
         Math.max(0.0F, bottom(bounds) - bottom(reference)),
         Math.max(0.0F, right(bounds) - right(reference))
      );
   }

   @NotNull
   public static String compactRepresentation(@NotNull Rectangle2D rect) {
      return "[" + rect.getX() + ", " + rect.getY() + ", " + rect.getWidth() + "x" + rect.getHeight() + "]";
   }

   @NotNull
   public static Rectangle2D toIntegerBounds(@NotNull Rectangle2D in, @NotNull Rectangle2D out) {
      double minY = Math.floor(in.getMinY());
      double minX = Math.floor(in.getMinX());
      double maxX = Math.ceil(in.getMaxX());
      double maxY = Math.ceil(in.getMaxY());
      out.setFrame(minX, minY, maxX - minX, maxY - minY);
      return out;
   }

   @NotNull
   public static Rectangle2D adjustForAliasing(@NotNull Rectangle2D r) {
      return toIntegerBounds(r, r);
   }

   @NotNull
   public static AffineTransform createInverse(@NotNull AffineTransform at) {
      try {
         return at.createInverse();
      } catch (NoninvertibleTransformException e) {
         throw new IllegalStateException(e);
      }
   }

   @Nullable
   public static AffineTransform toAwtTransform(@NotNull RenderContext context, @Nullable TransformValue transform) {
      return transform == null ? null : transform.get(context.measureContext());
   }

   @NotNull
   public static Point2D lastPointOnPath(@NotNull PathIterator pathIterator) {
      Point2D lastPoint = new Double();
      Point2D lastMoveToPoint = new Double();
      double[] args = new double[6];

      while (!pathIterator.isDone()) {
         switch (pathIterator.currentSegment(args)) {
            case 0:
               lastPoint.setLocation(args[0], args[1]);
               lastMoveToPoint.setLocation(args[0], args[1]);
               break;
            case 1:
               lastPoint.setLocation(args[0], args[1]);
               break;
            case 2:
               lastPoint.setLocation(args[2], args[3]);
               break;
            case 3:
               lastPoint.setLocation(args[4], args[5]);
               break;
            case 4:
               lastPoint.setLocation(lastMoveToPoint.getX(), lastMoveToPoint.getY());
         }

         pathIterator.next();
      }

      return lastPoint;
   }

   @NotNull
   public static Rectangle2D convertBounds(
      @NotNull RenderContext context, @NotNull Rectangle2D r, @NotNull GeometryUtil.Space from, @NotNull GeometryUtil.Space to
   ) {
      if (from == to) {
         return r;
      }

      Rectangle2D out = r;
      if (from == GeometryUtil.Space.USER) {
         if (to == GeometryUtil.Space.ROOT) {
            out = containingBoundsAfterTransform(context.userSpaceTransform(), r);
         } else if (to == GeometryUtil.Space.DEVICE) {
            out = containingBoundsAfterTransform(context.userSpaceTransform(), r);
            out = containingBoundsAfterTransform(context.rootTransform(), out);
         }
      }

      if (from == GeometryUtil.Space.ROOT) {
         if (to == GeometryUtil.Space.USER) {
            out = containingBoundsAfterTransform(createInverse(context.rootTransform()), r);
         } else if (to == GeometryUtil.Space.DEVICE) {
            out = containingBoundsAfterTransform(context.rootTransform(), r);
         }
      }

      if (from == GeometryUtil.Space.DEVICE) {
         if (to == GeometryUtil.Space.USER) {
            out = containingBoundsAfterTransform(createInverse(context.rootTransform()), r);
            out = containingBoundsAfterTransform(createInverse(context.userSpaceTransform()), out);
         } else if (to == GeometryUtil.Space.ROOT) {
            out = containingBoundsAfterTransform(createInverse(context.rootTransform()), r);
         }
      }

      return out;
   }

   @NotNull
   public static Rectangle2D userBoundsToDeviceBounds(@NotNull RenderContext context, @NotNull Rectangle2D r) {
      return convertBounds(context, r, GeometryUtil.Space.USER, GeometryUtil.Space.DEVICE);
   }

   @NotNull
   public static Point2D getLocation(@NotNull Rectangle2D r) {
      return new Double(r.getX(), r.getY());
   }

   public static boolean isValidRect(@NotNull Rectangle2D r) {
      return !java.lang.Double.isNaN(r.getX())
         && !java.lang.Double.isNaN(r.getY())
         && !java.lang.Double.isNaN(r.getWidth())
         && !java.lang.Double.isNaN(r.getHeight());
   }

   @NotNull
   public static AffineTransform interpolate(@NotNull AffineTransform a, @NotNull AffineTransform b, float t) {
      return TransformUtil.interpolate(a, b, t);
   }

   public enum Space {
      USER,
      ROOT,
      DEVICE;

      // $VF: synthetic method
      private static GeometryUtil.Space[] $values() {
         return new GeometryUtil.Space[]{USER, ROOT, DEVICE};
      }
   }
}

