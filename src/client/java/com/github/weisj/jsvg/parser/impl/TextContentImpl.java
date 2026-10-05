package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.parser.TextContent;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class TextContentImpl implements TextContent {
   @NotNull
   private final ParsedElement parent;
   @NotNull
   private final List<List<TextContent.Segment>> contentLists = new ArrayList<>();

   public TextContentImpl(@NotNull ParsedElement parent) {
      this.parent = parent;

      for (int i = 0; i <= parent.children().size(); i++) {
         this.contentLists.add(new ArrayList<>());
      }
   }

   private void ensureSize() {
      while (this.contentLists.size() < this.parent.children().size() + 1) {
         this.contentLists.add(new ArrayList<>());
      }
   }

   @NotNull
   public List<List<TextContent.Segment>> contentLists() {
      return this.contentLists;
   }

   @NotNull
   public List<TextContent.Segment> currentContentList() {
      this.ensureSize();
      return this.contentLists.get(this.contentLists.size() - 1);
   }

   public void addContentList() {
      this.contentLists.add(new ArrayList<>());
   }

   @NotNull
   @Override
   public List<TextContent.Segment> contentAfterChildIndex(int childIndex) {
      this.ensureSize();
      return this.contentLists.get(childIndex + 1);
   }
}

