package com.github.weisj.jsvg.parser;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface DomProcessor {
   void process(@NotNull DomElement var1);
}
