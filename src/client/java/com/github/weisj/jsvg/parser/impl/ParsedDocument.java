package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.animation.AnimationPeriod;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
import com.github.weisj.jsvg.parser.DomDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ParsedDocument implements DomDocument {
   private final Map<String, Object> namedElements = new HashMap<>();
   @Nullable
   private final URI rootURI;
   @NotNull
   private final LoaderContext loaderContext;
   @NotNull
   private final LoadHelper loadHelper;
   private int currentDepth;
   @NotNull
   private AnimationPeriod animationPeriod = new AnimationPeriod(0L, 0L, false);

   public ParsedDocument(@Nullable URI rootURI, @NotNull LoaderContext loaderContext, @NotNull LoadHelper loadHelper) {
      this.rootURI = rootURI;
      this.loaderContext = loaderContext;
      this.loadHelper = loadHelper;
   }

   @NotNull
   @Override
   public LoaderContext loaderContext() {
      return this.loaderContext;
   }

   @NotNull
   public LoadHelper loadHelper() {
      return this.loadHelper;
   }

   @Override
   public void registerNamedElement(@NotNull String name, @Nullable Object element) {
      this.namedElements.put(name, element);
   }

   @Internal
   int currentNestingDepth() {
      return this.currentDepth;
   }

   @Internal
   void setCurrentNestingDepth(int depth) {
      this.currentDepth = depth;
   }

   @Nullable
   @Override
   public <T> T getElementById(@NotNull Class<T> type, @Nullable String id) {
      if (id == null) {
         return null;
      }

      Object node = this.namedElements.get(id);
      if (!type.equals(ParsedElement.class) && node instanceof ParsedElement) {
         node = ((ParsedElement)node).nodeEnsuringBuildStatus(this.currentNestingDepth());
      }

      return type.isInstance(node) ? type.cast(node) : null;
   }

   public boolean hasElementWithId(@NotNull String id) {
      return this.namedElements.containsKey(id);
   }

   @Nullable
   @Override
   public URI rootURI() {
      return this.rootURI;
   }

   @NotNull
   public AnimationPeriod animationPeriod() {
      return this.animationPeriod;
   }

   public void registerAnimatedElement(@NotNull BaseAnimationNode animate) {
      this.animationPeriod = this.animationPeriod.derive(animate.track());
   }
}

