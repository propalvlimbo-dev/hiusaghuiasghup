package com.github.weisj.jsvg.renderer.impl.context;

import com.github.weisj.jsvg.paint.SVGPaint;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class ContextElementAttributes {
   @NotNull
   public final SVGPaint fillPaint;
   @NotNull
   public final SVGPaint strokePaint;

   public ContextElementAttributes(@NotNull SVGPaint fillPaint, @NotNull SVGPaint strokePaint) {
      this.fillPaint = fillPaint;
      this.strokePaint = strokePaint;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof ContextElementAttributes)) {
         return false;
      }

      ContextElementAttributes that = (ContextElementAttributes)o;
      return this.fillPaint.equals(that.fillPaint) && this.strokePaint.equals(that.strokePaint);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.fillPaint, this.strokePaint);
   }

   @Override
   public String toString() {
      return "ContextElementAttributes{fillPaint=" + this.fillPaint + ", strokePaint=" + this.strokePaint + '}';
   }
}

