package com.github.weisj.jsvg.geometry.mesh;

import com.github.weisj.jsvg.geometry.util.GeometryUtil;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Path2D.Float;
import org.jetbrains.annotations.NotNull;

public final class CoonPatch {
   public Bezier north;
   public Bezier east;
   public Bezier south;
   public Bezier west;
   @NotNull
   public final CoonValues coonValues;

   CoonPatch(Bezier north, Bezier east, Bezier south, Bezier west) {
      this(north, east, south, west, new CoonValues(MeshUtil.p(0.0F, 0.0F), MeshUtil.p(1.0F, 0.0F), MeshUtil.p(1.0F, 1.0F), MeshUtil.p(0.0F, 1.0F)));
   }

   @NotNull
   public static CoonPatch createUninitialized() {
      return new CoonPatch(null, null, null, null);
   }

   CoonPatch(Bezier north, Bezier east, Bezier south, Bezier west, @NotNull CoonValues coonValues) {
      this.north = north;
      this.east = east;
      this.south = south;
      this.west = west;
      this.coonValues = coonValues;
   }

   @NotNull
   public Shape toShape() {
      Path2D p = new Float(0);
      p.moveTo(this.north.a.x, this.north.a.y);
      this.north.appendTo(p);
      this.east.appendTo(p);
      this.south.appendTo(p);
      this.west.appendTo(p);
      p.closePath();
      return p;
   }

   public Subdivided<CoonPatch> subdivide() {
      java.awt.geom.Point2D.Float midNorthLinear = GeometryUtil.midPoint(this.north.a, this.north.d);
      java.awt.geom.Point2D.Float midSouthLinear = GeometryUtil.midPoint(this.south.d, this.south.a);
      java.awt.geom.Point2D.Float midWestLinear = GeometryUtil.midPoint(this.north.a, this.south.d);
      java.awt.geom.Point2D.Float midEastLinear = GeometryUtil.midPoint(this.north.d, this.south.a);
      Split<Bezier> northSplit = this.north.split();
      Split<Bezier> southSplit = this.south.split();
      Split<Bezier> westSplit = this.west.split();
      Split<Bezier> eastSplit = this.east.split();
      Bezier northLeft = northSplit.left;
      Bezier northRight = northSplit.right;
      Bezier southLeft = southSplit.right;
      Bezier southRight = southSplit.left;
      Bezier westBottom = westSplit.left;
      Bezier westTop = westSplit.right;
      Bezier eastTop = eastSplit.left;
      Bezier eastBottom = eastSplit.right;
      Bezier midNorthSouth = new Bezier(
         GeometryUtil.midPoint(this.north.a, this.south.d),
         GeometryUtil.midPoint(this.north.b, this.south.c),
         GeometryUtil.midPoint(this.north.c, this.south.b),
         GeometryUtil.midPoint(this.north.d, this.south.a)
      );
      Bezier midEastWest = new Bezier(
         GeometryUtil.midPoint(this.east.a, this.west.d),
         GeometryUtil.midPoint(this.east.b, this.west.c),
         GeometryUtil.midPoint(this.east.c, this.west.b),
         GeometryUtil.midPoint(this.east.d, this.west.a)
      );
      Split<Bezier> splitNorthSouth = Bezier.combine(
            midEastWest, Bezier.straightLine(northLeft.d, southLeft.a), Bezier.straightLine(midNorthLinear, midSouthLinear)
         )
         .split();
      Split<Bezier> splitWestEast = Bezier.combine(midNorthSouth, Bezier.straightLine(westTop.a, eastTop.d), Bezier.straightLine(midWestLinear, midEastLinear))
         .split();
      java.awt.geom.Point2D.Float midNorthValue = GeometryUtil.midPoint(this.coonValues.north, this.coonValues.east);
      java.awt.geom.Point2D.Float midWestValue = GeometryUtil.midPoint(this.coonValues.north, this.coonValues.west);
      java.awt.geom.Point2D.Float midSouthValue = GeometryUtil.midPoint(this.coonValues.west, this.coonValues.south);
      java.awt.geom.Point2D.Float midEastValue = GeometryUtil.midPoint(this.coonValues.east, this.coonValues.south);
      java.awt.geom.Point2D.Float gridMidValue = GeometryUtil.midPoint(midSouthValue, midNorthValue);
      CoonValues northWestWeights = new CoonValues(this.coonValues.north, midNorthValue, gridMidValue, midWestValue);
      CoonValues northEastWeights = new CoonValues(midNorthValue, this.coonValues.east, midEastValue, gridMidValue);
      CoonValues southWestWeights = new CoonValues(midWestValue, gridMidValue, midSouthValue, this.coonValues.west);
      CoonValues southEastWeights = new CoonValues(gridMidValue, midEastValue, this.coonValues.south, midSouthValue);
      CoonPatch northWest = new CoonPatch(northLeft, splitNorthSouth.left, splitWestEast.left.inverse(), westTop, northWestWeights);
      CoonPatch northEast = new CoonPatch(northRight, eastTop, splitWestEast.right.inverse(), splitNorthSouth.left.inverse(), northEastWeights);
      CoonPatch southWest = new CoonPatch(splitWestEast.left, splitNorthSouth.right, southLeft, westBottom, southWestWeights);
      CoonPatch southEast = new CoonPatch(splitWestEast.right, eastBottom, southRight, splitNorthSouth.right.inverse(), southEastWeights);
      return new Subdivided<>(northWest, northEast, southWest, southEast);
   }

   @Override
   public String toString() {
      return "CoonPatch{north="
         + this.north
         + ", east="
         + this.east
         + ", south="
         + this.south
         + ", west="
         + this.west
         + ", coonValues="
         + this.coonValues
         + '}';
   }
}

