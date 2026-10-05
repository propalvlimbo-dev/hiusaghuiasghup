package com.github.weisj.jsvg.util.supplier;

import com.google.errorprone.annotations.Immutable;

@Immutable
@FunctionalInterface
public interface ImmutableSupplier<T> {
   T get();
}
