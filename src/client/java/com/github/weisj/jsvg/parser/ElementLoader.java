package com.github.weisj.jsvg.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ElementLoader {
   @Nullable
   <T> T loadElement(@NotNull Class<T> var1, @Nullable String var2, @NotNull DomDocument var3);
}
