package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.parser.DomDocument;
import com.github.weisj.jsvg.parser.ElementLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class DefaultElementLoader implements ElementLoader {
   private static final DefaultElementLoader.DocumentLoader DEFAULT_DOCUMENT_LOADER = new DefaultElementLoader.DefaultDocumentLoader();
   private final DefaultElementLoader.DocumentLoader documentLoader;

   DefaultElementLoader(DefaultElementLoader.AllowExternalResources allowExternalResources) {
      this.documentLoader = createDocumentLoader(allowExternalResources);
   }

   @NotNull
   private static DefaultElementLoader.DocumentLoader createDocumentLoader(DefaultElementLoader.AllowExternalResources allowExternalResources) {
      return allowExternalResources == DefaultElementLoader.AllowExternalResources.DENY ? DEFAULT_DOCUMENT_LOADER : new ExternalDocumentLoader();
   }

   @Nullable
   @Override
   public <T> T loadElement(@NotNull Class<T> type, @Nullable String value, @NotNull DomDocument document) {
      Url url = Url.parse(value, Url.RequireFragment.YES);
      if (url == null) {
         return null;
      }

      DomDocument resolutionDocument = document;
      if (url.url() != null) {
         resolutionDocument = this.documentLoader.resolveDocument(document, url.url());
         if (resolutionDocument == null) {
            return null;
         }
      }

      return resolutionDocument.getElementById(type, url.fragment());
   }

   enum AllowExternalResources {
      DENY,
      ALLOW;

      // $VF: synthetic method
      private static DefaultElementLoader.AllowExternalResources[] $values() {
         return new DefaultElementLoader.AllowExternalResources[]{DENY, ALLOW};
      }
   }

   private static class DefaultDocumentLoader implements DefaultElementLoader.DocumentLoader {
      private DefaultDocumentLoader() {
      }

      @Nullable
      @Override
      public DomDocument resolveDocument(@NotNull DomDocument document, @NotNull String name) {
         return name.isEmpty() ? document : null;
      }
   }

   interface DocumentLoader {
      @Nullable
      DomDocument resolveDocument(@NotNull DomDocument var1, @NotNull String var2);
   }
}

