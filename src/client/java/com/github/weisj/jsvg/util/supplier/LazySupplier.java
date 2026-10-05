package com.github.weisj.jsvg.util.supplier;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public final class LazySupplier<T> implements Supplier<T> {
   @NotNull
   private final Supplier<T> supplier;
   private T t;

   public LazySupplier(@NotNull Supplier<T> supplier) {
      this.supplier = supplier;
   }

   @NotNull
   @Override
   public T get() {
      if (this.t == null) {
         this.t = this.supplier.get();
      }

      return this.t;
   }
}

