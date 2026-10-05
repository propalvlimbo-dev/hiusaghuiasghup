package com.github.weisj.jsvg.attributes.value;

import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class ConstantFloat implements FloatValue {
   private final float value;

   public ConstantFloat(float value) {
      this.value = value;
   }

   @Override
   public float get(@NotNull MeasureContext context) {
      return this.value;
   }
}

