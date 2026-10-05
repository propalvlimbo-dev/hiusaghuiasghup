package com.github.weisj.jsvg.nodes.animation;

import com.github.weisj.jsvg.animation.value.AnimatedTransform;
import com.github.weisj.jsvg.attributes.transform.TransformPart;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.nodes.prototype.spec.Category;
import com.github.weisj.jsvg.nodes.prototype.spec.ElementCategories;
import com.github.weisj.jsvg.nodes.prototype.spec.PermittedContent;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ElementCategories(Category.Animation)
@PermittedContent(categories = Category.Descriptive)
public final class AnimateTransform extends BaseAnimationNode {
   public static final String TAG = "animatetransform";
   private TransformPart.TransformType type;

   @NotNull
   @Override
   public String tagName() {
      return "animatetransform";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.type = attributeNode.getEnum("type", TransformPart.TransformType.TRANSLATE);
   }

   @Nullable
   public AnimatedTransform animatedTransform(@NotNull TransformValue initial, @NotNull AttributeNode attributeNode) {
      if (this.track == null) {
         return null;
      }

      TransformPart[] transforms = new TransformPart[this.values.length];
      TransformPart identity = TransformPart.identityOfType(this.type);

      for (int i = 0; i < this.values.length; i++) {
         if (isPlaceholder(this.values[i])) {
            transforms[i] = identity;
         } else {
            TransformPart part = attributeNode.parser().parseTransformPart(this.type, this.values[i]);
            if (part == null) {
               return null;
            }

            transforms[i] = part;
         }
      }

      return new AnimatedTransform(this.track, initial, transforms);
   }
}

