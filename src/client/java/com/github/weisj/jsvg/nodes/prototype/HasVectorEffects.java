package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.attributes.VectorEffect;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface HasVectorEffects {
   @NotNull
   Set<VectorEffect> vectorEffects();
}
