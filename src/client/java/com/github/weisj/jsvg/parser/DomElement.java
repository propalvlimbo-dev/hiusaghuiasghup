package com.github.weisj.jsvg.parser;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Experimental;

public interface DomElement {
   @Nullable
   String id();

   @NotNull
   String tagName();

   @NotNull
   List<String> classNames();

   @NotNull
   DomDocument document();

   @NotNull
   List<? extends DomElement> children();

   @Nullable
   String attribute(@NotNull String var1);

   @NotNull
   default String attribute(@NotNull String name, @NotNull String fallback) {
      String value = this.attribute(name);
      return value != null ? value : fallback;
   }

   void setAttribute(@NotNull String var1, @Nullable String var2);

   @Experimental
   @NotNull
   TextContent textContent();

   @Nullable
   DomElement parent();
}
