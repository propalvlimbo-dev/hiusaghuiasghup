package com.github.weisj.jsvg.nodes.mesh;

import com.github.weisj.jsvg.geometry.mesh.Bezier;
import com.github.weisj.jsvg.geometry.mesh.CoonPatch;
import com.github.weisj.jsvg.geometry.size.Length;
import com.github.weisj.jsvg.nodes.SVGNode;
import com.github.weisj.jsvg.nodes.Stop;
import java.awt.geom.Point2D.Float;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

final class MeshBuilder {

   private MeshBuilder() {
   }

   static void buildMesh(@NotNull MeshGradient meshGradient, @NotNull Float origin) {
      Float start = origin;
      int patchCount = -1;
      List<? extends SVGNode> rows = meshGradient.children();
      int rowIndex = 0;

      for (int rowCount = rows.size(); rowIndex < rowCount; rowIndex++) {
         SVGNode child = rows.get(rowIndex);
         MeshRow row = (MeshRow)child;
         int rowPatchCount = row.children().size();
         if (patchCount == -1) {
            patchCount = rowPatchCount;
         }

         if (rowPatchCount != patchCount) {
            throw new IllegalStateException("Every mesh row needs to specify the same amount of patched");
         }

         List<? extends SVGNode> patchesInRow = row.children();

         for (int patchIndex = 0; patchIndex < rowPatchCount; patchIndex++) {
            SVGNode node = patchesInRow.get(patchIndex);
            MeshPatch patch = (MeshPatch)node;
            List<? extends SVGNode> stops = patch.children();
            int stopCount = stops.size();
            int requiredStops = stopsForPatchPosition(rowIndex, patchIndex);
            if (stopCount < requiredStops) {
               throw new IllegalStateException("Not enough stops specified");
            }

            int offset = offsetForPatchPosition(rowIndex);
            stopCount = requiredStops;
            MeshPatch patchAbove = null;
            if (offset == 1) {
               patchAbove = (MeshPatch)((MeshRow)rows.get(rowIndex - 1)).children().get(patchIndex);
               patch.coonPatch.north = patchAbove.coonPatch.south.inverse();
               patch.north = patchAbove.west;
            }

            MeshPatch patchLeft = null;
            if (offset + stopCount < 4) {
               patchLeft = (MeshPatch)patchesInRow.get(patchIndex - 1);
               patch.coonPatch.west = patchLeft.coonPatch.east.inverse();
               patch.west = patchLeft.south;
            } else if (patchIndex > 0) {
               patchLeft = (MeshPatch)patchesInRow.get(patchIndex - 1);
            }

            for (int stopIndex = 0; stopIndex < stopCount; stopIndex++) {
               Stop stop = (Stop)stops.get(stopIndex);
               switch (stopIndex + offset) {
                  case 0:
                     patch.coonPatch.north = Objects.requireNonNull(stop.bezierCommand()).createBezier(start);
                     start = patch.coonPatch.north.d;
                     patch.north = patchIndex > 0 ? Objects.requireNonNull(patchLeft).east : stop.color();
                     break;
                  case 1:
                     patch.coonPatch.east = Objects.requireNonNull(stop.bezierCommand()).createBezier(patch.coonPatch.north.d);
                     patch.east = offset == 1 ? Objects.requireNonNull(patchAbove).south : stop.color();
                     break;
                  case 2:
                     patch.coonPatch.south = Objects.requireNonNull(stop.bezierCommand()).createBezier(patch.coonPatch.east.d);
                     patch.south = stop.color();
                     break;
                  case 3:
                     patch.coonPatch.west = Objects.requireNonNull(stop.bezierCommand()).createBezier(patch.coonPatch.south.d);
                     patch.coonPatch.west.d.setLocation(patch.coonPatch.north.a);
                     patch.west = stop.color();
                     break;
                  default:
                     assert false;
               }
            }

            if (offset + stopCount < 4) {
               assert patchLeft != null;
               patch.coonPatch.south.d.setLocation(patchLeft.coonPatch.east.d);
            }

            try {
               assertPatchDefined(patch.coonPatch);
            } catch (IllegalStateException e) {
               throw new IllegalStateException(rowIndex + "," + patchIndex + " " + patch.coonPatch, e);
            }
         }
      }
   }

   private static int stopsForPatchPosition(int rowIndex, int patchIndex) {
      if (rowIndex > 0) {
         return patchIndex > 0 ? 2 : 3;
      } else {
         return patchIndex > 0 ? 3 : 4;
      }
   }

   private static int offsetForPatchPosition(int rowIndex) {
      return rowIndex > 0 ? 1 : 0;
   }

   private static void assertPatchDefined(@NotNull CoonPatch coonPatch) {
      if (coonPatch.north == null) {
         throw new IllegalStateException("North path is null");
      }

      if (coonPatch.east == null) {
         throw new IllegalStateException("East path is null");
      }

      if (coonPatch.south == null) {
         throw new IllegalStateException("South path is null");
      }

      if (coonPatch.west == null) {
         throw new IllegalStateException("West path is null");
      }

      if (hasUnspecifiedPoint(coonPatch.north)) {
         throw new IllegalStateException("North path has unspecified point");
      }

      if (hasUnspecifiedPoint(coonPatch.east)) {
         throw new IllegalStateException("East path has unspecified point");
      }

      if (hasUnspecifiedPoint(coonPatch.south)) {
         throw new IllegalStateException("South path has unspecified point");
      }

      if (hasUnspecifiedPoint(coonPatch.west)) {
         throw new IllegalStateException("West path has unspecified point");
      }
   }

   private static boolean hasUnspecifiedPoint(Bezier bezier) {
      if (isUnspecified(bezier.a)) {
         return true;
      } else if (isUnspecified(bezier.b)) {
         return true;
      } else {
         return isUnspecified(bezier.c) ? true : isUnspecified(bezier.d);
      }
   }

   private static boolean isUnspecified(@NotNull Float p) {
      return !Length.isSpecified(p.x) || !Length.isSpecified(p.y);
   }
}

