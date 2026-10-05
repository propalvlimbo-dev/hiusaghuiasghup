package com.github.weisj.jsvg.nodes.filter;

import org.jetbrains.annotations.NotNull;

final class IllegalFilterStateException extends IllegalStateException {
   public IllegalFilterStateException(@NotNull String s) {
      super(s);
   }
}

