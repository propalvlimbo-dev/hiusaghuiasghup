package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.parser.TextContent;
import com.github.weisj.jsvg.renderer.RenderContext;
import com.github.weisj.jsvg.util.supplier.ConstantSupplier;
import java.text.BreakIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class StringTextSegment implements TextSegment {
   private final Supplier<List<String>> codepoints;
   @NotNull
   private final TextContainer<?> parent;
   @NotNull
   private final TextLayoutGroup layoutGroup;
   private final int index;
   @Nullable
   GlyphRun currentGlyphRun = null;
   @Nullable
   RenderContext currentRenderContext = null;

   public StringTextSegment(@NotNull TextContainer<?> parent, @NotNull TextLayoutGroup layoutGroup, int index, @NotNull TextContent.Segment content) {
      this.parent = parent;
      this.layoutGroup = layoutGroup;
      this.index = index;
      if (content.isConstant()) {
         this.codepoints = new ConstantSupplier<>(segmentCodepoints(content.text()));
      } else {
         this.codepoints = new StringTextSegment.CachedCodepoints(content);
      }
   }

   @Override
   public boolean isSegmentVisible(@NotNull RenderContext currentContext) {
      return this.parent.isVisible(currentContext);
   }

   @NotNull
   public List<String> codepoints() {
      return this.codepoints.get();
   }

   public boolean isLastSegmentInParent() {
      return this.index == this.layoutGroup.segments().size() - 1;
   }

   @NotNull
   private static List<String> segmentCodepoints(String text) {
      BreakIterator it = BreakIterator.getCharacterInstance();
      it.setText(new StringCharacterIterator(text));
      int start = it.first();
      List<String> characters = new ArrayList<>();

      for (int end = it.next(); end != -1; end = it.next()) {
         characters.add(text.substring(start, end));
         start = end;
      }

      return characters;
   }

   private static class CachedCodepoints implements Supplier<List<String>> {
      @NotNull
      private final TextContent.Segment segment;
      private List<@NotNull String> codepoints;
      @Nullable
      private String lastText;

      private CachedCodepoints(@NotNull TextContent.Segment segment) {
         this.segment = segment;
      }

      public List<String> get() {
         String text = this.segment.text();
         if (Objects.equals(text, this.lastText)) {
            return this.codepoints;
         }

         this.lastText = text;
         this.codepoints = StringTextSegment.segmentCodepoints(text);
         return this.codepoints;
      }
   }
}

