package com.github.weisj.jsvg.geometry.mesh;

public final class Subdivided<T> {
   public final T northWest;
   public final T northEast;
   public final T southWest;
   public final T southEast;

   public Subdivided(T northWest, T northEast, T southWest, T southEast) {
      this.northWest = northWest;
      this.northEast = northEast;
      this.southWest = southWest;
      this.southEast = southEast;
   }
}

