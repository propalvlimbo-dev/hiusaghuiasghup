package com.github.weisj.jsvg.parser.resources;

import com.github.weisj.jsvg.parser.resources.impl.DefaultResourcePolicy;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResourcePolicy {
   ResourcePolicy DENY_ALL = new DefaultResourcePolicy(0);
   ResourcePolicy DENY_EXTERNAL = new DefaultResourcePolicy(8);
   ResourcePolicy ALLOW_RELATIVE = new DefaultResourcePolicy(9);
   ResourcePolicy ALLOW_ALL = new DefaultResourcePolicy(15);

   @Nullable
   URI resolveResourceURI(@Nullable URI var1, @NotNull String var2);

   @Nullable
   URI resolveResourceURI(@Nullable URI var1, @NotNull URI var2);
}
