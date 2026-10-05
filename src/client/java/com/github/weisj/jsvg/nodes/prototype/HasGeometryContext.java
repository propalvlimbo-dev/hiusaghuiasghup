package com.github.weisj.jsvg.nodes.prototype;

import com.github.weisj.jsvg.attributes.Coordinate;
import com.github.weisj.jsvg.attributes.transform.TransformBox;
import com.github.weisj.jsvg.attributes.value.LengthValue;
import com.github.weisj.jsvg.attributes.value.TransformValue;
import com.github.weisj.jsvg.nodes.ClipPath;
import com.github.weisj.jsvg.nodes.Mask;
import com.github.weisj.jsvg.nodes.filter.Filter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HasGeometryContext extends Transformable, HasClip, HasFilter {
   interface ByDelegate extends HasGeometryContext {
      @NotNull
      HasGeometryContext geometryContextDelegate();

      @Nullable
      @Override
      default ClipPath clipPath() {
         return this.geometryContextDelegate().clipPath();
      }

      @Nullable
      @Override
      default Mask mask() {
         return this.geometryContextDelegate().mask();
      }

      @Nullable
      @Override
      default Filter filter() {
         return this.geometryContextDelegate().filter();
      }

      @Nullable
      @Override
      default TransformValue transform() {
         return this.geometryContextDelegate().transform();
      }

      @Override
      default TransformBox transformBox() {
         return this.geometryContextDelegate().transformBox();
      }

      @NotNull
      @Override
      default Coordinate<LengthValue> transformOrigin() {
         return this.geometryContextDelegate().transformOrigin();
      }
   }
}
