package com.github.weisj.jsvg.parser.css.impl;

import com.github.weisj.jsvg.parser.DomElement;
import com.github.weisj.jsvg.parser.css.StyleProperty;
import com.github.weisj.jsvg.parser.css.StyleSheet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public final class SimpleStyleSheet implements StyleSheet {
   @NotNull
   private final Map<String, List<StyleProperty>> classRules = new HashMap<>();
   @NotNull
   private final Map<String, List<StyleProperty>> idRules = new HashMap<>();
   @NotNull
   private final Map<String, List<StyleProperty>> tagNameRules = new HashMap<>();

   @NotNull
   public Map<String, List<StyleProperty>> classRules() {
      return this.classRules;
   }

   @NotNull
   public Map<String, List<StyleProperty>> idRules() {
      return this.idRules;
   }

   @NotNull
   public Map<String, List<StyleProperty>> tagNameRules() {
      return this.tagNameRules;
   }

   void addTagNameRules(@NotNull String tagName, @NotNull List<StyleProperty> rule) {
      this.tagNameRules.computeIfAbsent(tagName, k -> new ArrayList<>()).addAll(rule);
   }

   void addClassRules(@NotNull String className, @NotNull List<StyleProperty> rule) {
      this.classRules.computeIfAbsent(className, k -> new ArrayList<>()).addAll(rule);
   }

   void addIdRules(@NotNull String id, @NotNull List<StyleProperty> rule) {
      this.idRules.computeIfAbsent(id, k -> new ArrayList<>()).addAll(rule);
   }

   @Override
   public void forEachMatchingRule(@NotNull DomElement element, @NotNull StyleSheet.RuleConsumer ruleConsumer) {
      List<StyleProperty> rules = this.tagNameRules.get(element.tagName());
      if (rules != null) {
         rules.forEach(ruleConsumer::applyRule);
      }

      if (element.id() != null) {
         rules = this.idRules.get(element.id());
         if (rules != null) {
            rules.forEach(ruleConsumer::applyRule);
         }
      }

      for (String className : element.classNames()) {
         rules = this.classRules.get(className);
         if (rules != null) {
            rules.forEach(ruleConsumer::applyRule);
         }
      }
   }
}

