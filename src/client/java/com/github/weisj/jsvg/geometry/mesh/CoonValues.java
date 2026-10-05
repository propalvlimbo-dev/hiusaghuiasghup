package com.github.weisj.jsvg.geometry.mesh;

import java.awt.geom.Point2D.Float;

public final class CoonValues {
   public final Float north;
   public final Float east;
   public final Float south;
   public final Float west;

   public CoonValues(Float north, Float east, Float south, Float west) {
      this.north = north;
      this.east = east;
      this.south = south;
      this.west = west;
   }

   @Override
   public String toString() {
      return "CoonValues{north=" + this.north + ", east=" + this.east + ", south=" + this.south + ", west=" + this.west + '}';
   }
}

