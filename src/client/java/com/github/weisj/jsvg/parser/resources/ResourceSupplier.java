package com.github.weisj.jsvg.parser.resources;

import com.github.weisj.jsvg.renderer.PlatformSupport;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public interface ResourceSupplier<T> {
   @NotNull
   Optional<T> get(@NotNull PlatformSupport var1);
}
