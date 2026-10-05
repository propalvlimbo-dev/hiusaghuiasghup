package com.github.weisj.jsvg.attributes.filter;

import org.jetbrains.annotations.NotNull;

public interface FilterChannelKey {
   @NotNull
   Object key();

   class StringKey implements FilterChannelKey {
      @NotNull
      private final String key;

      public StringKey(@NotNull String key) {
         this.key = key;
      }

      @NotNull
      @Override
      public Object key() {
         return this.key;
      }

      @Override
      public String toString() {
         return "StringKey{key='" + this.key + '\'' + '}';
      }
   }
}
