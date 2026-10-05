package com.github.weisj.jsvg.parser.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Url {
   @NotNull
   private final String rawUrl;
   @Nullable
   private final String url;
   @Nullable
   private final String fragment;

   public Url(@NotNull String rawUrl, @Nullable String url, @Nullable String fragment) {
      this.rawUrl = rawUrl;
      this.url = url;
      this.fragment = fragment;
   }

   @Nullable
   public static Url parse(@Nullable String value, Url.RequireFragment requireFragment) {
      if (value == null) {
         return null;
      }

      String urlString = value;
      if (urlString.startsWith("url(")) {
         if (!urlString.endsWith(")")) {
            return null;
         }

         urlString = ParserUtil.removeWhiteSpace(urlString.substring(4, urlString.length() - 1));
      }

      String[] split = urlString.split("#", 2);
      if (split.length == 0) {
         return null;
      }

      if (requireFragment == Url.RequireFragment.YES && split.length != 2) {
         return null;
      }

      String url = nullIfEmpty(split[0]);
      String fragment = nullIfEmpty(split.length == 2 ? split[1] : null);
      return url == null && fragment == null ? null : new Url(urlString, url, fragment);
   }

   @Nullable
   private static String nullIfEmpty(@Nullable String s) {
      return s != null && !s.isEmpty() ? s : null;
   }

   @NotNull
   public String rawUrl() {
      return this.rawUrl;
   }

   @Nullable
   public String url() {
      return this.url;
   }

   @Nullable
   public String fragment() {
      return this.fragment;
   }

   public enum RequireFragment {
      YES,
      NO;

      // $VF: synthetic method
      private static Url.RequireFragment[] $values() {
         return new Url.RequireFragment[]{YES, NO};
      }
   }
}

