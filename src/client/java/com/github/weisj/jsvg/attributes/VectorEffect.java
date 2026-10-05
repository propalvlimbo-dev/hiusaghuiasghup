package com.github.weisj.jsvg.attributes;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.renderer.output.Output;
import com.github.weisj.jsvg.util.ShapeUtil;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum VectorEffect implements HasMatchName {
   None(0),
   NonScalingStroke("non-scaling-stroke", 0),
   NonScalingSize("non-scaling-size", 1),
   NonRotation("non-rotation", 2),
   FixedPosition("fixed-position", 4);

   @NotNull
   private final String matchName;
   private final int flag;

   VectorEffect(int flag) {
      this.matchName = this.name();
      this.flag = flag;
   }

   VectorEffect(@NotNull String matchName, int flag) {
      this.matchName = matchName;
      this.flag = flag;
   }

   @NotNull
   public static Set<VectorEffect> parse(@NotNull AttributeNode attributeNode) {
      String[] vectorEffectsRaw = attributeNode.getStringList("vector-effect");
      EnumSet<VectorEffect> vectorEffects = EnumSet.noneOf(VectorEffect.class);

      for (String effect : vectorEffectsRaw) {
         vectorEffects.add(attributeNode.parser().parseEnum(effect, None));
      }

      return vectorEffects;
   }

   @NotNull
   @Override
   public String matchName() {
      return this.matchName;
   }

   private static int flags(@NotNull Set<VectorEffect> effects) {
      int flag = 0;

      for (VectorEffect effect : effects) {
         flag |= effect.flag;
      }

      return flag;
   }

   public static void applyEffects(
      @NotNull Set<VectorEffect> effects, @NotNull Output output, @NotNull RenderContext context, @Nullable AffineTransform elementTransform
   ) {
      int flags = flags(effects);
      if (flags != 0) {
         AffineTransform shapeTransform = new AffineTransform(context.userSpaceTransform());
         double x0 = elementTransform != null ? elementTransform.getTranslateX() : 0.0;
         double y0 = elementTransform != null ? elementTransform.getTranslateY() : 0.0;
         updateTransformForFlags(flags, shapeTransform, x0, y0);
         output.setTransform(context.rootTransform());
         output.applyTransform(shapeTransform);
      }
   }

   @NotNull
   public static Shape applyNonScalingStroke(@NotNull Output output, @NotNull RenderContext context, @NotNull Stroke stroke, @NotNull Shape shape) {
      Shape strokedShape = ShapeUtil.transformShape(shape, context.userSpaceTransform());
      strokedShape = stroke.createStrokedShape(strokedShape);
      return ShapeUtil.transformShape(strokedShape, GeometryUtil.createInverse(context.userSpaceTransform()));
   }

   private static void updateTransformForFlags(int flags, @NotNull AffineTransform transform, double x0, double y0) {
      switch (flags) {
         case 1: {
            double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
            if (detRoot == 0.0) {
               return;
            }

            double detRootInv = 1.0 / detRoot;
            transform.setTransform(
               transform.getScaleX() * detRootInv,
               transform.getShearY() * detRootInv,
               transform.getShearX() * detRootInv,
               transform.getScaleY() * detRootInv,
               transform.getTranslateX(),
               transform.getTranslateY()
            );
            break;
         }
         case 2: {
            double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
            transform.setTransform(detRoot, 0.0, 0.0, detRoot, transform.getTranslateX(), transform.getTranslateY());
            break;
         }
         case 3:
            transform.setTransform(1.0, 0.0, 0.0, 1.0, transform.getTranslateX(), transform.getTranslateY());
            break;
         case 4:
            transform.setTransform(transform.getScaleX(), transform.getShearY(), transform.getShearX(), transform.getScaleY(), x0, y0);
            break;
         case 5: {
            double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
            if (detRoot == 0.0) {
               return;
            }

            double detRootInv = 1.0 / detRoot;
            transform.setTransform(
               transform.getScaleX() * detRootInv,
               transform.getShearY() * detRootInv,
               transform.getShearX() * detRootInv,
               transform.getScaleY() * detRootInv,
               x0,
               y0
            );
            break;
         }
         case 6: {
            double detRoot = Math.sqrt(Math.abs(transform.getDeterminant()));
            transform.setTransform(detRoot, 0.0, 0.0, detRoot, x0, y0);
            break;
         }
         case 7:
            transform.setTransform(1.0, 0.0, 0.0, 1.0, x0, y0);
      }
   }

   // $VF: synthetic method
   private static VectorEffect[] $values() {
      return new VectorEffect[]{None, NonScalingStroke, NonScalingSize, NonRotation, FixedPosition};
   }

   private static final class Flags {
      private static final int NON_SCALING_SIZE = 1;
      private static final int NON_ROTATING = 2;
      private static final int FIXED_POSITION = 4;
   }
}
