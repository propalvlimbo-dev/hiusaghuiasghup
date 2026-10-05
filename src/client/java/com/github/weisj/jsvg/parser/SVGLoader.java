package com.github.weisj.jsvg.parser;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.impl.StaxSVGLoader;
import com.github.weisj.jsvg.parser.impl.StreamUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SVGLoader {
   static final Logger LOGGER = LogFactory.createLogger(SVGLoader.class);
   private final StaxSVGLoader loader = new StaxSVGLoader();

   @Nullable
   public SVGDocument load(@NotNull URL xmlBase) {
      return this.load(xmlBase, LoaderContext.createDefault());
   }

   @Nullable
   public SVGDocument load(@NotNull URL xmlBase, @NotNull LoaderContext loaderContext) {
      try {
         URI uri = xmlBase.toURI();
         return this.load(xmlBase.openStream(), uri, loaderContext);
      } catch (URISyntaxException | IOException e) {
         LOGGER.log(Logger.Level.WARNING, String.format("Could not read %s", xmlBase), e);
         return null;
      }
   }

   @Nullable
   public SVGDocument load(@NotNull InputStream inputStream, @Nullable URI xmlBase, @NotNull LoaderContext loaderContext) {
      try {
         InputStream is = StreamUtil.createDocumentInputStream(inputStream);

         SVGDocument var5;
         try {
            var5 = this.load(this.loader.createXMLInput(is), xmlBase, loaderContext);
         } catch (Throwable var8) {
            if (is != null) {
               try {
                  is.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (is != null) {
            is.close();
         }

         return var5;
      } catch (IOException e) {
         LOGGER.log(Logger.Level.WARNING, "Could not wrap input stream", e);
         return null;
      }
   }

   @Nullable
   public SVGDocument load(@NotNull XMLInput xmlInput, @Nullable URI xmlBase, @NotNull LoaderContext loaderContext) {
      try {
         return this.loader.load(xmlInput, xmlBase, loaderContext);
      } catch (Exception e) {
         LOGGER.log(Logger.Level.WARNING, "Could not load SVG", e);
         return null;
      }
   }
}

