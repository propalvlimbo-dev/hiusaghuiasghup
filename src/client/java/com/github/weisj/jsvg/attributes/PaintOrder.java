package com.github.weisj.jsvg.attributes;

import com.github.weisj.jsvg.parser.impl.AttributeNode;
import com.github.weisj.jsvg.parser.impl.AttributeParser;
import com.github.weisj.jsvg.parser.impl.SeparatorMode;
import com.github.weisj.jsvg.util.AttributeUtil;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PaintOrder {
   public static final PaintOrder NORMAL = new PaintOrder(PaintOrder.Phase.FILL, PaintOrder.Phase.STROKE, PaintOrder.Phase.MARKERS);
   @NotNull
   private final PaintOrder.Phase[] phases;

   public PaintOrder(@NotNull PaintOrder.Phase... phases) {
      this.phases = phases;
   }

   @NotNull
   public PaintOrder.Phase[] phases() {
      return this.phases;
   }

   @Nullable
   public static PaintOrder parse(@NotNull AttributeNode attributeNode) {
      String value = attributeNode.getValue("paint-order");
      AttributeParser parser = attributeNode.parser();
      if (value == null) {
         return null;
      }

      if ("inherit".equals(value)) {
         return null;
      }

      if ("none".equals(value)) {
         return NORMAL;
      }

      if ("normal".equals(value)) {
         return NORMAL;
      }

      String[] rawPhases = parser.parseStringList(value, SeparatorMode.COMMA_AND_WHITESPACE);
      PaintOrder.Phase[] phases = new PaintOrder.Phase[3];
      int length = Math.min(phases.length, rawPhases.length);
      int phasesIndex = 0;

      for (int rawPhasesIndex = 0; phasesIndex < length && rawPhasesIndex < length; rawPhasesIndex++) {
         PaintOrder.Phase phase = parser.parseEnum(rawPhases[rawPhasesIndex], PaintOrder.Phase.class);
         if (phase != null && !AttributeUtil.arrayContains(phases, phase)) {
            phases[phasesIndex] = phase;
            phasesIndex++;
         }
      }

      while (phasesIndex < 3) {
         phases[phasesIndex] = findNextInNormalOrder(phases, phasesIndex);
         phasesIndex++;
      }

      return new PaintOrder(phases);
   }

   @NotNull
   private static PaintOrder.Phase findNextInNormalOrder(@NotNull PaintOrder.Phase[] phases, int maxIndex) {
      for (PaintOrder.Phase phase : NORMAL.phases()) {
         boolean found = false;

         for (int i = 0; i < maxIndex; i++) {
            if (phases[i] == phase) {
               found = true;
               break;
            }
         }

         if (!found) {
            return phase;
         }
      }

      throw new IllegalStateException();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         PaintOrder that = (PaintOrder)o;
         return Arrays.equals(this.phases, that.phases);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.phases);
   }

   @Override
   public String toString() {
      return "PaintOrder" + Arrays.toString(this.phases);
   }

   public enum Phase {
      FILL,
      STROKE,
      MARKERS;

      // $VF: synthetic method
      private static PaintOrder.Phase[] $values() {
         return new PaintOrder.Phase[]{FILL, STROKE, MARKERS};
      }
   }
}

