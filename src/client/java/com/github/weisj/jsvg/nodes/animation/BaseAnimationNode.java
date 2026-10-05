package com.github.weisj.jsvg.nodes.animation;

import com.github.weisj.jsvg.animation.Additive;
import com.github.weisj.jsvg.animation.AnimationValuesType;
import com.github.weisj.jsvg.animation.Track;
import com.github.weisj.jsvg.nodes.AbstractSVGNode;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseAnimationNode extends AbstractSVGNode {
   @NotNull
   private static final String INITIAL_PLACEHOLDER = "<<initial>>";
   protected String[] values;
   @Nullable
   protected Track track;

   BaseAnimationNode() {
   }

   @Nullable
   public static String attributeName(@NotNull AttributeNode attributeNode) {
      return attributeNode.getValue("attributeName");
   }

   protected static boolean isPlaceholder(@NotNull String value) {
      return value == "<<initial>>";
   }

   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      String from = attributeNode.getValue("from");
      String to = attributeNode.getValue("to");
      String by = attributeNode.getValue("by");
      if (to != null) {
         by = null;
      }

      AnimationValuesType valuesType = null;
      if (attributeNode.getValue("values") != null) {
         this.values = attributeNode.getStringList("values", SeparatorMode.SEMICOLON_ONLY);
         valuesType = AnimationValuesType.VALUES;
      } else if (from != null && to != null) {
         this.values = new String[]{from, to};
         valuesType = AnimationValuesType.FROM_TO;
      } else if (from != null && by != null) {
         this.values = new String[]{from, by};
         valuesType = AnimationValuesType.FROM_BY;
      } else if (by != null) {
         this.values = new String[]{"<<initial>>", by};
         valuesType = AnimationValuesType.BY;
      } else if (to != null) {
         this.values = new String[]{"<<initial>>", to};
         valuesType = AnimationValuesType.TO;
      }

      if (valuesType != null) {
         Additive additive = valuesType.endIsBy() ? Additive.SUM : attributeNode.getEnum("additive", Additive.REPLACE);
         this.track = Track.parse(attributeNode, valuesType, additive);
      }
   }

   @Nullable
   public Track track() {
      return this.track;
   }
}

