package com.github.weisj.jsvg.nodes.prototype;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Mutator<T> {
   @NotNull
   T mutate(@NotNull T var1);
}
