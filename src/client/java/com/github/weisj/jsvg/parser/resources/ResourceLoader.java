package com.github.weisj.jsvg.parser.resources;

import com.github.weisj.jsvg.parser.DomDocument;
import java.io.IOException;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ResourceLoader {
   @Nullable
   ResourceSupplier<RenderableResource> loadImage(@NotNull DomDocument var1, @NotNull URI var2) throws IOException;
}
