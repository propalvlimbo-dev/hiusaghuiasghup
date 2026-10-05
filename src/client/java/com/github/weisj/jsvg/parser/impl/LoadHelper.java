package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.parser.ElementLoader;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.resources.ResourceLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import org.jetbrains.annotations.NotNull;

public final class LoadHelper {
   @NotNull
   private final AttributeParser attributeParser;
   @NotNull
   private final LoaderContext loaderContext;

   public LoadHelper(@NotNull AttributeParser attributeParser, @NotNull LoaderContext loaderContext) {
      this.attributeParser = attributeParser;
      this.loaderContext = loaderContext;
   }

   @NotNull
   public AttributeParser attributeParser() {
      return this.attributeParser;
   }

   @NotNull
   public ResourceLoader resourceLoader() {
      return this.loaderContext.resourceLoader();
   }

   @NotNull
   public ElementLoader elementLoader() {
      return this.loaderContext.elementLoader();
   }

   @NotNull
   public ResourcePolicy externalResourcePolicy() {
      return this.loaderContext.externalResourcePolicy();
   }
}

