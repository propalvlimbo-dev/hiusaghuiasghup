package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.DomDocument;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ExternalDocumentLoader implements DefaultElementLoader.DocumentLoader {
   private static final Logger LOGGER = LogFactory.createLogger(ExternalDocumentLoader.class);
   @NotNull
   private final Map<URI, ExternalDocumentLoader.CachedDocument> cache = new HashMap<>();

   @Nullable
   @Override
   public DomDocument resolveDocument(@NotNull DomDocument document, @NotNull String name) {
      return name.isEmpty() ? document : this.locateDocument(document, name);
   }

   @Nullable
   private DomDocument locateDocument(@NotNull DomDocument document, @NotNull String name) {
      URI documentUri = document.loaderContext().externalResourcePolicy().resolveResourceURI(document.rootURI(), name);
      if (documentUri == null) {
         return null;
      }

      try {
         URL documentUrl = documentUri.toURL();
         synchronized (this.cache) {
            ExternalDocumentLoader.CachedDocument cachedDocument = this.cache.get(documentUri);
            if (cachedDocument != null) {
               ParsedDocument cached = cachedDocument.document;
               if (cached == null) {
                  throw new IllegalStateException("Reference cycle containing external document: " + documentUri);
               }

               return cached;
            }
         }

         ExternalDocumentLoader.CachedDocument cachedDocument = new ExternalDocumentLoader.CachedDocument();
         synchronized (this.cache) {
            this.cache.put(documentUri, cachedDocument);
         }

         InputStream is = StreamUtil.createDocumentInputStream(documentUrl.openStream());

         ParsedDocument parsedDocument;
         label93: {
            ParsedDocument var22;
            try {
               SVGDocumentBuilder builder = new StaxSVGLoader().parse(is, documentUri, document.loaderContext());
               if (builder == null) {
                  parsedDocument = null;
                  break label93;
               }

               builder.preProcess();
               parsedDocument = builder.parsedDocument();
               synchronized (this.cache) {
                  cachedDocument.document = parsedDocument;
               }

               var22 = parsedDocument;
            } catch (Throwable var14) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var11) {
                     var14.addSuppressed(var11);
                  }
               }

               throw var14;
            }

            if (is != null) {
               is.close();
            }

            return var22;
         }

         if (is != null) {
            is.close();
         }

         return parsedDocument;
      } catch (Exception e) {
         LOGGER.log(Logger.Level.WARNING, String.format("Failed to load external document: %s from %s - %s", name, documentUri, e.getMessage()));
         return null;
      }
   }

   private static final class CachedDocument {
      @Nullable
      private ParsedDocument document;

      private CachedDocument() {
      }
   }
}

