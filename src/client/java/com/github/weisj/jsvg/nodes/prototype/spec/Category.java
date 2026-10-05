package com.github.weisj.jsvg.nodes.prototype.spec;

import com.github.weisj.jsvg.nodes.SVGNode;
import org.jetbrains.annotations.NotNull;

public enum Category {
   Animation(false),
   BasicShape,
   Container,
   Descriptive(false),
   FilterPrimitive,
   TransferFunctionElement,
   Gradient,
   Graphic,
   GraphicsReferencing,
   Shape,
   Structural,
   TextContent,
   TextContentChild,
   None;

   private final boolean effectivelyAllowed;

   Category() {
      this(true);
   }

   Category(boolean effectivelyAllowed) {
      this.effectivelyAllowed = effectivelyAllowed;
   }

   public boolean isEffectivelyAllowed() {
      return this.effectivelyAllowed;
   }

   @NotNull
   public static Category[] categoriesOf(@NotNull SVGNode node) {
      Class<? extends SVGNode> nodeType = (Class<? extends SVGNode>)node.getClass();
      ElementCategories categories = nodeType.getAnnotation(ElementCategories.class);
      if (categories == null) {
         throw new IllegalStateException("Element <" + node.tagName() + "> doesn't specify element category information");
      } else {
         return categories.value();
      }
   }

   public static boolean hasCategory(@NotNull SVGNode node, @NotNull Category category) {
      Category[] categories = categoriesOf(node);

      for (Category c : categories) {
         if (c == category) {
            return true;
         }
      }

      return false;
   }

   // $VF: synthetic method
   private static Category[] $values() {
      return new Category[]{
         Animation,
         BasicShape,
         Container,
         Descriptive,
         FilterPrimitive,
         TransferFunctionElement,
         Gradient,
         Graphic,
         GraphicsReferencing,
         Shape,
         Structural,
         TextContent,
         TextContentChild,
         None
      };
   }
}
