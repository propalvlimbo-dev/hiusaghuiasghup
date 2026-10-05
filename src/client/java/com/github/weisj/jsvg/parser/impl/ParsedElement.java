package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.animation.BaseAnimationNode;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.Renderable;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.parser.TextContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ParsedElement implements DomElement {
   @Nullable
   private final String id;
   @NotNull
   private final ParsedDocument document;
   @Nullable
   private final ParsedElement parent;
   @NotNull
   private final AttributeNode attributeNode;
   @NotNull
   private final SVGNode node;
   @NotNull
   private final List<ParsedElement> children = new ArrayList<>();
   @NotNull
   private final List<ParsedElement> indirectChildren = new ArrayList<>();
   @NotNull
   private final Map<String, List<ParsedElement>> animationElements = new HashMap<>();
   private TextContentImpl textContent = null;
   final CharacterDataParser characterDataParser;
   @NotNull
   private ParsedElement.BuildStatus buildStatus = ParsedElement.BuildStatus.NOT_BUILT;
   private int outgoingPaths = -1;

   ParsedElement(@Nullable String id, @NotNull ParsedDocument document, @Nullable ParsedElement parent, @NotNull AttributeNode element, @NotNull SVGNode node) {
      this.document = document;
      this.parent = parent;
      this.attributeNode = element;
      this.node = node;
      this.id = id;
      PermittedContent permittedContent = node.getClass().getAnnotation(PermittedContent.class);
      if (permittedContent == null) {
         throw new IllegalStateException("Element <" + node.tagName() + "> doesn't specify permitted content");
      }

      if (permittedContent.charData()) {
         this.characterDataParser = new CharacterDataParser();
      } else {
         this.characterDataParser = null;
      }
   }

   @Nullable
   @Override
   public String id() {
      return this.id;
   }

   @NotNull
   @Override
   public String tagName() {
      return this.attributeNode.tagName();
   }

   @NotNull
   @Override
   public List<String> classNames() {
      return this.attributeNode.classNames();
   }

   @NotNull
   public ParsedDocument document() {
      return this.document;
   }

   @NotNull
   @Override
   public List<ParsedElement> children() {
      return this.children;
   }

   @Nullable
   @Override
   public String attribute(@NotNull String name) {
      return this.attributeNode.getValue(name);
   }

   @Override
   public void setAttribute(@NotNull String name, @Nullable String value) {
      if (value == null) {
         this.attributeNode.attributes().remove(name);
      } else {
         this.attributeNode.attributes().put(name, value);
      }
   }

   @NotNull
   public TextContentImpl textContent() {
      if (this.textContent == null) {
         this.textContent = new TextContentImpl(this);
      }

      return this.textContent;
   }

   @NotNull
   public Map<String, List<ParsedElement>> animationElements() {
      return this.animationElements;
   }

   @Nullable
   public ParsedElement parent() {
      return this.parent;
   }

   @NotNull
   public SVGNode node() {
      return this.node;
   }

   @NotNull
   public SVGNode nodeEnsuringBuildStatus(int depth) {
      if (this.buildStatus == ParsedElement.BuildStatus.IN_PROGRESS) {
         this.cyclicDependencyDetected();
      } else if (this.buildStatus == ParsedElement.BuildStatus.NOT_BUILT) {
         this.build(depth);
      }

      return this.node;
   }

   @NotNull
   public AttributeNode attributeNode() {
      return this.attributeNode;
   }

   void addChild(@NotNull ParsedElement parsedElement) {
      if (Category.hasCategory(parsedElement.node, Category.Animation)) {
         String attributeName = BaseAnimationNode.attributeName(parsedElement.attributeNode());
         this.animationElements.computeIfAbsent(attributeName, k -> new ArrayList<>()).add(parsedElement);
      }

      this.children.add(parsedElement);
   }

   void addIndirectChild(@NotNull ParsedElement parsedElement) {
      this.indirectChildren.add(parsedElement);
   }

   private void addChildrenAndContent() {
      if (this.node instanceof Container) {
         int contentListsSize = this.textContent == null ? 0 : this.textContent.contentLists().size();

         for (int i = 0; i < this.children.size(); i++) {
            if (i < contentListsSize) {
               assert this.textContent != null;
               this.addContentList(this.textContent.contentLists().get(i));
            }

            ParsedElement child = this.children.get(i);
            ((Container)this.node).addChild(child.id, child.node);
         }

         for (int i = this.children.size(); i < contentListsSize; i++) {
            assert this.textContent != null;
            this.addContentList(this.textContent.contentLists().get(i));
         }
      } else if (this.textContent != null) {
         for (List<TextContent.Segment> contentList : this.textContent.contentLists()) {
            this.addContentList(contentList);
         }
      }
   }

   private void addContentList(@NotNull List<TextContent.Segment> contentList) {
      for (TextContent.Segment text : contentList) {
         this.node.addContent(text);
      }
   }

   void build(int depth) {
      if (this.buildStatus != ParsedElement.BuildStatus.FINISHED) {
         if (this.buildStatus == ParsedElement.BuildStatus.IN_PROGRESS) {
            this.cyclicDependencyDetected();
         } else {
            this.buildStatus = ParsedElement.BuildStatus.IN_PROGRESS;
            int maxNestingDepth = this.attributeNode.document().loaderContext().documentLimits().maxNestingDepth();
            if (depth > maxNestingDepth) {
               throw new IllegalStateException(
                  String.format("Maximum nesting depth reached %d > %d.%n", depth, maxNestingDepth)
                     + "Note: You can configure this using LoaderContext#documentLimits()"
               );
            }

            this.addChildrenAndContent();
            this.attributeNode.prepareForNodeBuilding();

            for (ParsedElement child : this.children) {
               child.build(depth + 1);
            }

            this.document().setCurrentNestingDepth(depth);
            this.node.build(this.attributeNode);
            this.buildStatus = ParsedElement.BuildStatus.FINISHED;
         }
      }
   }

   int outgoingPaths() {
      if (this.outgoingPaths == -1) {
         this.outgoingPaths = 0;

         for (ParsedElement child : this.children) {
            if (child.node instanceof Renderable) {
               this.outgoingPaths = this.outgoingPaths + child.outgoingPaths();
            }
         }

         for (ParsedElement child : this.indirectChildren) {
            this.outgoingPaths = this.outgoingPaths + child.outgoingPaths();
         }

         this.outgoingPaths = Math.max(this.outgoingPaths, 1);
      }

      return this.outgoingPaths;
   }

   @Override
   public String toString() {
      return "ParsedElement{node=" + this.node + '}';
   }

   private void cyclicDependencyDetected() {
      throw new IllegalStateException("Cyclic dependency involving node '" + this.id + "' detected.");
   }

   private enum BuildStatus {
      NOT_BUILT,
      IN_PROGRESS,
      FINISHED;

      // $VF: synthetic method
      private static ParsedElement.BuildStatus[] $values() {
         return new ParsedElement.BuildStatus[]{NOT_BUILT, IN_PROGRESS, FINISHED};
      }
   }
}

