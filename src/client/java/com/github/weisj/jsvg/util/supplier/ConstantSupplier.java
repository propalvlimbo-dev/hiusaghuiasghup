package com.github.weisj.jsvg.util.supplier;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public final class ConstantSupplier<T> implements Supplier<T> {
   @NotNull
   private final T t;

   public ConstantSupplier(@NotNull T t) {
      this.t = t;
   }

   @NotNull
   @Override
   public T get() {
      return this.t;
   }
}

