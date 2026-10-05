package com.github.weisj.jsvg.attributes.value;

import com.github.weisj.jsvg.renderer.MeasureContext;
import org.jetbrains.annotations.NotNull;

public final class ConstantValue<T> implements Value<T> {
   private final T value;

   public ConstantValue(T value) {
      this.value = value;
   }

   @Override
   public T get(@NotNull MeasureContext context) {
      return this.value;
   }

   public T get() {
      return this.value;
   }
}

