package com.github.weisj.jsvg.attributes.value;

import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class ConstantFloatList implements FloatListValue {
   @NotNull
   public static final FloatListValue EMPTY = new ConstantFloatList(new float[0]);
   private final float @NotNull [] value;

   public ConstantFloatList(float @NotNull [] value) {
      this.value = value;
   }

   @Override
   public float @NotNull [] get(@NotNull MeasureContext context) {
      return this.value;
   }

   public float[] value() {
      return this.value;
   }
}

