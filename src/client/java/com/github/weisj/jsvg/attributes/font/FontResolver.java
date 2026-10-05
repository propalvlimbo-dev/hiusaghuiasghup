package com.github.weisj.jsvg.attributes.font;

import com.github.weisj.jsvg.renderer.MeasureContext;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class FontResolver {
   private FontResolver() {
   }

   public static void clearFontCache() {
      FontResolver.FontCache.INSTANCE.cache.clear();
   }

   @NotNull
   public static SVGFont resolve(@NotNull MeasurableFontSpec fontSpec, @NotNull MeasureContext measureContext, @NotNull String defaultFontFamily) {
      FontResolver.FontCache.CacheKey key = new FontResolver.FontCache.CacheKey(fontSpec, measureContext);
      SVGFont cachedFont = FontResolver.FontCache.INSTANCE.cache.get(key);
      if (cachedFont != null) {
         return cachedFont;
      }

      SVGFont resolvedFont = resolveWithoutCache(fontSpec, measureContext, defaultFontFamily);
      FontResolver.FontCache.INSTANCE.cache.put(key, resolvedFont);
      return resolvedFont;
   }

   @NotNull
   public static SVGFont resolveWithoutCache(@NotNull MeasurableFontSpec fontSpec, @NotNull MeasureContext measureContext, @NotNull String defaultFontFamily) {
      String family = findSupportedFontFamily(fontSpec, defaultFontFamily);
      FontStyle style = fontSpec.style();
      float weight = cssWeightToAwtWeight(fontSpec.currentWeight());
      float size = fontSpec.effectiveSize(measureContext);
      float stretch = fontSpec.stretch().orElseIfUnspecified(1.0F).value();
      Map<Attribute, Object> attributes = new HashMap<>(5, 1.0F);
      attributes.put(TextAttribute.FAMILY, family);
      attributes.put(TextAttribute.SIZE, size);
      attributes.put(TextAttribute.WEIGHT, weight);
      attributes.put(TextAttribute.WIDTH, stretch);
      if (style instanceof FontStyle.Normal) {
         attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
      } else if (style instanceof FontStyle.Italic) {
         attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
      } else {
         AffineTransform transform = style.transform();
         if (transform != null) {
            attributes.put(TextAttribute.TRANSFORM, transform);
         }
      }

      Font font = new Font(attributes);
      return new AWTSVGFont(font);
   }

   private static float cssWeightToAwtWeight(float weight) {
      int normalWeight = 400;
      float currentWeight = weight;
      if (currentWeight > normalWeight) {
         float awtWeightCompensationFactor = TextAttribute.WEIGHT_BOLD * normalWeight / 700.0F;
         currentWeight *= awtWeightCompensationFactor;
      }

      return currentWeight / normalWeight;
   }

   @NotNull
   private static String findSupportedFontFamily(@NotNull MeasurableFontSpec fontSpec, @NotNull String defaultFontFamily) {
      String[] families = fontSpec.families();

      for (String family : families) {
         if (FontResolver.FontFamiliesCache.INSTANCE.isSupportedFontFamily(family)) {
            return family;
         }
      }

      return defaultFontFamily;
   }

   @NotNull
   public static List<String> supportedFonts() {
      return Collections.unmodifiableList(Arrays.asList(FontResolver.FontFamiliesCache.INSTANCE.supportedFonts));
   }

   private enum FontCache {
      INSTANCE;

      private final HashMap<FontResolver.FontCache.CacheKey, SVGFont> cache = new HashMap<>();

      // $VF: synthetic method
      private static FontResolver.FontCache[] $values() {
         return new FontResolver.FontCache[]{INSTANCE};
      }

      private static final class CacheKey {
         @NotNull
         private final MeasurableFontSpec spec;
         @NotNull
         private final MeasureContext context;

         private CacheKey(@NotNull MeasurableFontSpec spec, @NotNull MeasureContext context) {
            this.spec = spec;
            this.context = context;
         }

         @Override
         public String toString() {
            return "CacheKey{spec=" + this.spec + ", context=" + this.context + '}';
         }

         @Override
         public boolean equals(Object o) {
            if (this == o) {
               return true;
            }

            if (!(o instanceof FontResolver.FontCache.CacheKey)) {
               return false;
            }

            FontResolver.FontCache.CacheKey cacheKey = (FontResolver.FontCache.CacheKey)o;
            return this.spec.equals(cacheKey.spec) && this.context.equals(cacheKey.context);
         }

         @Override
         public int hashCode() {
            return Objects.hash(this.spec, this.context);
         }
      }
   }

   private enum FontFamiliesCache {
      INSTANCE;

      @NotNull
      private final String[] supportedFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

      boolean isSupportedFontFamily(@NotNull String fontName) {
         for (String supportedFont : this.supportedFonts) {
            if (supportedFont.equalsIgnoreCase(fontName)) {
               return true;
            }
         }

         return false;
      }

      // $VF: synthetic method
      private static FontResolver.FontFamiliesCache[] $values() {
         return new FontResolver.FontFamiliesCache[]{INSTANCE};
      }
   }
}

