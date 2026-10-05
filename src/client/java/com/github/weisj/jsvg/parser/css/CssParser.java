package com.github.weisj.jsvg.parser.css;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface CssParser {
   @NotNull
   StyleSheet parse(@NotNull List<char[]> var1);
}
