package com.github.weisj.jsvg.attributes.value;

import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class ConstantLengthTransform implements TransformValue {
   public static final ConstantLengthTransform IDENTITY = new ConstantLengthTransform(Collections.emptyList());
   public static final ConstantLengthTransform INHERITED = new ConstantLengthTransform(Collections.emptyList());
   @NotNull
   private final List<TransformPart> parts;

   public ConstantLengthTransform(@NotNull List<TransformPart> parts) {
      this.parts = parts;
   }

   @NotNull
   @Override
   public AffineTransform get(@NotNull MeasureContext context) {
      AffineTransform transform = new AffineTransform();

      for (TransformPart part : this.parts) {
         part.applyToTransform(transform, context);
      }

      return transform;
   }
}

