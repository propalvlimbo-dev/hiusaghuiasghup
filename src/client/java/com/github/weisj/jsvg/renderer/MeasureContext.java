package com.github.weisj.jsvg.renderer;

import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.renderer.animation.AnimationState;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;
import com.google.errorprone.annotations.Immutable;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Immutable
public final class MeasureContext {
   private final float vw;
   private final float vh;
   private final float em;
   private final float rem;
   private final float ex;
   @NotNull
   private final AnimationState animationState;

   public MeasureContext(float vw, float vh, float em, float ex, float rem, @NotNull AnimationState animationState) {
      this.vw = vw;
      this.vh = vh;
      this.em = em;
      this.rem = rem;
      this.ex = ex;
      this.animationState = animationState;
   }

   @NotNull
   public static MeasureContext createInitial(@NotNull FloatSize viewBoxSize, float em, float ex, @NotNull AnimationState animationState) {
      return new MeasureContext(viewBoxSize.width, viewBoxSize.height, em, ex, em, animationState);
   }

   @NotNull
   public MeasureContext deriveRoot(float rem) {
      return new MeasureContext(this.vw, this.vh, this.em, this.ex, rem, this.animationState);
   }

   @NotNull
   public MeasureContext derive(float viewWidth, float viewHeight) {
      return new MeasureContext(viewWidth, viewHeight, this.em, this.ex, this.rem, this.animationState);
   }

   @NotNull
   public MeasureContext derive(@Nullable ViewBox viewBox, float em, float ex) {
      if (viewBox == null && Length.isUnspecified(em) && Length.isUnspecified(ex)) {
         return this;
      }

      float newVw = this.vw;
      float newVh = this.vh;
      if (viewBox != null) {
         if (viewBox.hasSpecifiedWidth()) {
            newVw = viewBox.width;
         }

         if (viewBox.hasSpecifiedHeight()) {
            newVh = viewBox.height;
         }
      }

      float effectiveEm = Length.isUnspecified(em) ? this.em : em;
      float effectiveEx = Length.isUnspecified(ex) ? this.ex : ex;
      return new MeasureContext(newVw, newVh, effectiveEm, effectiveEx, this.rem, this.animationState);
   }

   public float viewWidth() {
      return this.vw;
   }

   public float viewHeight() {
      return this.vh;
   }

   public float normedDiagonalLength() {
      return (float)Math.sqrt((this.vw * this.vw + this.vh * this.vh) / 2.0F);
   }

   public float em() {
      return this.em;
   }

   public float rem() {
      return this.rem;
   }

   public float ex() {
      return this.ex;
   }

   public long timestamp() {
      return this.animationState.timestamp();
   }

   @Override
   public String toString() {
      return "MeasureContext{vw="
         + this.vw
         + ", vh="
         + this.vh
         + ", em="
         + this.em
         + ", rem="
         + this.rem
         + ", ex="
         + this.ex
         + ", animationState="
         + this.animationState
         + '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof MeasureContext)) {
         return false;
      }

      MeasureContext that = (MeasureContext)o;
      return Float.compare(that.vw, this.vw) == 0
         && Float.compare(that.vh, this.vh) == 0
         && Float.compare(that.em, this.em) == 0
         && Float.compare(that.rem, this.rem) == 0
         && Float.compare(that.ex, this.ex) == 0
         && this.animationState.equals(that.animationState);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.vw, this.vh, this.em, this.ex, this.rem, this.animationState);
   }
}

