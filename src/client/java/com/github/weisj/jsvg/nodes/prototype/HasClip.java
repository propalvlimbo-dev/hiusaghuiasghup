package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import org.jetbrains.annotations.Nullable;

public interface HasClip {
   @Nullable
   ClipPath clipPath();

   @Nullable
   Mask mask();
}
