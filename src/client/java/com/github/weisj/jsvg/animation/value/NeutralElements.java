package com.github.weisj.jsvg.animation.value;

import com.github.weisj.jsvg.attributes.value.ConstantFloatList;
import com.github.weisj.jsvg.attributes.value.ConstantLengthTransform;
import com.github.weisj.jsvg.attributes.value.FloatListValue;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.PercentageValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.geometry.size.Percentage;
import org.jetbrains.annotations.NotNull;

public final class NeutralElements {
   @NotNull
   public static final LengthValue NEUTRAL_LENGTH = Length.ZERO;
   @NotNull
   public static final PercentageValue NEUTRAL_PERCENTAGE = Percentage.ONE;
   @NotNull
   public static final TransformValue NEUTRAL_TRANSFORM = ConstantLengthTransform.IDENTITY;
   @NotNull
   public static final FloatListValue NEUTRAL_FLOAT_LIST = ConstantFloatList.EMPTY;

   private NeutralElements() {
   }
}

