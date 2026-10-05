package com.github.weisj.jsvg.nodes.container;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.prototype.Container;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseContainerNode<E> extends AbstractSVGNode implements Container<E> {
   private static final boolean EXHAUSTIVE_CHECK = true;
   private static final Logger LOGGER = LogFactory.createLogger(BaseContainerNode.class);

   @Override
   public final void addChild(@Nullable String id, @NotNull SVGNode node) {
      if (this.isAcceptableType(node) && this.acceptChild(id, node)) {
         this.doAdd(node);
      }
   }

   protected abstract void doAdd(@NotNull SVGNode var1);

   protected boolean acceptChild(@Nullable String id, @NotNull SVGNode node) {
      return true;
   }

   protected boolean isAcceptableType(@NotNull SVGNode node) {
      PermittedContent allowedNodes = this.getClass().getAnnotation(PermittedContent.class);
      if (allowedNodes == null) {
         throw new IllegalStateException(String.format("Element <%s> doesn't specify permitted content information", this.tagName()));
      }

      if (allowedNodes.any()) {
         return true;
      }

      Category[] categories = Category.categoriesOf(node);
      Class<? extends SVGNode> nodeType = (Class<? extends SVGNode>)node.getClass();
      BaseContainerNode.CategoryCheckResult result = this.doIntersect(allowedNodes.categories(), categories);
      if (result == BaseContainerNode.CategoryCheckResult.Allowed) {
         return true;
      }

      for (Class<? extends SVGNode> type : allowedNodes.anyOf()) {
         if (type.isAssignableFrom(nodeType)) {
            return true;
         }
      }

      if (result != BaseContainerNode.CategoryCheckResult.Excluded) {
         LOGGER.log(Logger.Level.WARNING, () -> String.format("Element <%s> not allowed in <%s> (or not implemented)", node.tagName(), this.tagName()));
      }

      return false;
   }

   private BaseContainerNode.CategoryCheckResult doIntersect(Category[] requested, Category[] provided) {
      BaseContainerNode.CategoryCheckResult result = BaseContainerNode.CategoryCheckResult.Denied;

      for (Category request : requested) {
         boolean effectivelyAllowed = request.isEffectivelyAllowed();
         if (!effectivelyAllowed) {
         }

         for (Category category : provided) {
            if (request == category) {
               if (effectivelyAllowed) {
                  return BaseContainerNode.CategoryCheckResult.Allowed;
               }

               result = BaseContainerNode.CategoryCheckResult.Excluded;
            }
         }
      }

      return result;
   }

   private enum CategoryCheckResult {
      Allowed,
      Denied,
      Excluded;

      // $VF: synthetic method
      private static BaseContainerNode.CategoryCheckResult[] $values() {
         return new BaseContainerNode.CategoryCheckResult[]{Allowed, Denied, Excluded};
      }
   }
}

