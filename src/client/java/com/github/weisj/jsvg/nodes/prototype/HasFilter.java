package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.nodes.filter.Filter;
import org.jetbrains.annotations.Nullable;

public interface HasFilter {
   @Nullable
   Filter filter();
}
