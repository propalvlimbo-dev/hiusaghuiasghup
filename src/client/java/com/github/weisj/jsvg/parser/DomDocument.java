package com.github.weisj.jsvg.parser;

import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DomDocument {
   @NotNull
   LoaderContext loaderContext();

   void registerNamedElement(@NotNull String var1, @Nullable Object var2);

   @Nullable
   <T> T getElementById(@NotNull Class<T> var1, @Nullable String var2);

   @Nullable
   URI rootURI();
}
