package com.github.weisj.jsvg.nodes.text;

import com.github.weisj.jsvg.attributes.value.PercentageDimension;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.parser.impl.AttributeNode;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

abstract class LinearTextContainer<T> extends TextContainer<T> implements CursorContext {
   protected Length[] x;
   protected Length[] y;
   protected Length[] dx;
   protected Length[] dy;
   protected float[] rotate;

   @MustBeInvokedByOverriders
   @Override
   public void build(@NotNull AttributeNode attributeNode) {
      super.build(attributeNode);
      this.x = attributeNode.getLengthList("x", PercentageDimension.WIDTH);
      this.y = attributeNode.getLengthList("y", PercentageDimension.HEIGHT);
      this.dx = attributeNode.getLengthList("dx", PercentageDimension.WIDTH);
      this.dy = attributeNode.getLengthList("dy", PercentageDimension.HEIGHT);
      this.rotate = attributeNode.getFloatList("rotate");
   }

   @NotNull
   @Override
   public GlyphCursor createLocalCursor(boolean isInitial, @NotNull GlyphCursor current) {
      GlyphCursor local = current.derive();
      if (this.x.length != 0) {
         local.xLocations = this.x;
         local.xOff = 0;
      }

      if (this.y.length != 0) {
         local.yLocations = this.y;
         local.yOff = 0;
      }

      if (this.dx.length != 0) {
         local.xDeltas = this.dx;
         local.dyOff = 0;
      }

      if (this.dy.length != 0) {
         local.yDeltas = this.dy;
         local.dyOff = 0;
      }

      if (this.rotate.length != 0) {
         local.rotations = this.rotate;
         local.rotOff = 0;
      }

      return local;
   }

   @Override
   public void cleanUpLocalCursor(@NotNull GlyphCursor current, @NotNull GlyphCursor local) {
      current.updateFrom(local);
      if (this.x.length == 0) {
         current.xOff = local.xOff;
      }

      if (this.y.length == 0) {
         current.yOff = local.yOff;
      }

      if (this.dx.length == 0) {
         current.dxOff = local.dxOff;
      }

      if (this.dy.length == 0) {
         current.dyOff = local.dyOff;
      }

      if (this.rotate.length == 0) {
         current.rotOff = local.rotOff;
      }
   }
}

