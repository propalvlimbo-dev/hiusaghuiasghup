package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.paint.impl.DefaultPaintParser;
import com.github.weisj.jsvg.parser.DocumentLimits;
import com.github.weisj.jsvg.parser.DomProcessor;
import com.github.weisj.jsvg.parser.ElementLoader;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.PaintParser;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.impl.SimpleCssParser;
import com.github.weisj.jsvg.parser.resources.ResourceLoader;
import com.github.weisj.jsvg.parser.resources.ResourcePolicy;
import com.github.weisj.jsvg.parser.resources.impl.DefaultResourcePolicy;
import com.github.weisj.jsvg.parser.resources.impl.SynchronousResourceLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MutableLoaderContext implements LoaderContext, LoaderContext.Builder {
   private static final ResourceLoader DEFAULT_RESOURCE_LOADER = new SynchronousResourceLoader();
   private static final ElementLoader DEFAULT_ELEMENT_LOADER = new DefaultElementLoader(DefaultElementLoader.AllowExternalResources.DENY);
   private static final CssParser DEFAULT_CSS_PARSER = new SimpleCssParser();
   private static final PaintParser DEFAULT_PAINT_PARSER = new DefaultPaintParser();
   @Nullable
   private DomProcessor preProcessor = null;
   @NotNull
   private CssParser cssParser = DEFAULT_CSS_PARSER;
   @NotNull
   private PaintParser paintParser = DEFAULT_PAINT_PARSER;
   @NotNull
   private ResourceLoader resourceLoader = DEFAULT_RESOURCE_LOADER;
   @NotNull
   private ElementLoader elementLoader = DEFAULT_ELEMENT_LOADER;
   @NotNull
   private ResourcePolicy resourcePolicy = ResourcePolicy.DENY_EXTERNAL;
   @NotNull
   private DocumentLimits documentLimits = DocumentLimits.DEFAULT;

   @NotNull
   public static MutableLoaderContext createDefault() {
      return new MutableLoaderContext();
   }

   @Nullable
   @Override
   public DomProcessor preProcessor() {
      return this.preProcessor;
   }

   @NotNull
   @Override
   public CssParser cssParser() {
      return this.cssParser;
   }

   @NotNull
   @Override
   public PaintParser paintParser() {
      return this.paintParser;
   }

   @NotNull
   @Override
   public ResourceLoader resourceLoader() {
      return this.resourceLoader;
   }

   @NotNull
   @Override
   public ElementLoader elementLoader() {
      return this.elementLoader;
   }

   @NotNull
   @Override
   public ResourcePolicy externalResourcePolicy() {
      return this.resourcePolicy;
   }

   @NotNull
   @Override
   public DocumentLimits documentLimits() {
      return this.documentLimits;
   }

   @NotNull
   @Override
   public LoaderContext.Builder preProcessor(@Nullable DomProcessor preProcessor) {
      this.preProcessor = preProcessor;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext.Builder cssParser(@NotNull CssParser cssParser) {
      this.cssParser = cssParser;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext.Builder paintParser(@NotNull PaintParser paintParser) {
      this.paintParser = paintParser;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext.Builder resourceLoader(@NotNull ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext.Builder elementLoader(@NotNull ElementLoader elementLoader) {
      this.elementLoader = elementLoader;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext.Builder externalResourcePolicy(@NotNull ResourcePolicy policy) {
      this.resourcePolicy = policy;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext.Builder documentLimits(@NotNull DocumentLimits documentLimits) {
      this.documentLimits = documentLimits;
      return this;
   }

   @NotNull
   @Override
   public LoaderContext build() {
      if (!(this.resourcePolicy instanceof DefaultResourcePolicy) || ((DefaultResourcePolicy)this.resourcePolicy).allowsExternalResources()) {
         this.elementLoader = new DefaultElementLoader(DefaultElementLoader.AllowExternalResources.ALLOW);
      }

      return this;
   }
}

