package org.xrose.utils.combat.aura.target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.util.Tuple;

public class MultiPoint {
   private static final int DATASET_SWITCH_MIN_HITS = 15;
   private static final int DATASET_SWITCH_MAX_HITS = 17;
   private static final int POINT_SWITCH_MIN_TICKS = 41;
   private static final int POINT_SWITCH_MAX_TICKS = 45;
   private static final int TELEPORT_TOP_CANDIDATES = 4;
   private static final double MAX_OFFSET_LENGTH = 0.5;
   private static final double MIN_GRID_SPACING = 0.15;
   private static final int MAX_GRID_STEPS_XZ = 14;
   private static final int GRID_STEPS_Y = 10;
   private static final double AIM_ASSIST_HEIGHT = 0.85;
   private static final MultiPoint.PointPreset DEFAULT_PRESET = new MultiPoint.PointPreset(
      new MultiPoint.AimPoint(0.0, 0.94, 0.0),
      new MultiPoint.AimPoint(0.0, 0.86, 0.0),
      new MultiPoint.AimPoint(0.0, 0.78, 0.0),
      new MultiPoint.AimPoint(0.0, 0.7, 0.0),
      new MultiPoint.AimPoint(0.0, 0.62, 0.0),
      new MultiPoint.AimPoint(0.0, 0.54, 0.0),
      new MultiPoint.AimPoint(0.0, 0.46, 0.0),
      new MultiPoint.AimPoint(0.24, 0.86, 0.0),
      new MultiPoint.AimPoint(-0.24, 0.86, 0.0),
      new MultiPoint.AimPoint(0.0, 0.86, 0.24),
      new MultiPoint.AimPoint(0.0, 0.86, -0.24),
      new MultiPoint.AimPoint(0.24, 0.78, 0.24),
      new MultiPoint.AimPoint(0.24, 0.78, -0.24),
      new MultiPoint.AimPoint(-0.24, 0.78, 0.24),
      new MultiPoint.AimPoint(-0.24, 0.78, -0.24),
      new MultiPoint.AimPoint(0.36, 0.7, 0.0),
      new MultiPoint.AimPoint(-0.36, 0.7, 0.0),
      new MultiPoint.AimPoint(0.0, 0.7, 0.36),
      new MultiPoint.AimPoint(0.0, 0.7, -0.36),
      new MultiPoint.AimPoint(0.22, 0.62, 0.22),
      new MultiPoint.AimPoint(0.22, 0.62, -0.22),
      new MultiPoint.AimPoint(-0.22, 0.62, 0.22),
      new MultiPoint.AimPoint(-0.22, 0.62, -0.22)
   );
   private static final MultiPoint.PointPreset SPOOKY_PRESET = buildSpookyPreset();
   private static final MultiPoint.PointDataset[] DATASETS = new MultiPoint.PointDataset[]{
      new MultiPoint.PointDataset(0.14, 0.94, 0.7, 6, 0.24, true, 0.25, 0.0, 0.12, 0.16, 0.78),
      new MultiPoint.PointDataset(0.24, 0.98, 0.84, 10, 0.32, true, 0.2, 0.01, 0.16, 0.2, 0.66),
      new MultiPoint.PointDataset(0.1, 0.86, 0.54, 8, 0.38, true, 0.28, 0.02, 0.18, 0.24, 0.62),
      new MultiPoint.PointDataset(0.34, 0.99, 0.9, 12, 0.28, true, 0.18, 0.0, 0.14, 0.18, 0.7),
      new MultiPoint.PointDataset(0.42, 0.96, 0.78, 7, 0.18, false, 0.35, 0.03, 0.08, 0.1, 0.74),
      new MultiPoint.PointDataset(0.18, 0.74, 0.58, 9, 0.44, true, 0.22, 0.01, 0.22, 0.26, 0.58),
      new MultiPoint.PointDataset(0.52, 1.0, 0.92, 11, 0.2, true, 0.42, 0.0, 0.1, 0.14, 0.72),
      new MultiPoint.PointDataset(0.06, 0.68, 0.42, 7, 0.34, true, 0.3, 0.04, 0.2, 0.22, 0.6),
      new MultiPoint.PointDataset(0.3, 0.88, 0.64, 13, 0.46, true, 0.16, 0.02, 0.26, 0.28, 0.52),
      new MultiPoint.PointDataset(0.6, 0.99, 0.86, 6, 0.4, true, 0.24, 0.0, 0.18, 0.2, 0.64),
      new MultiPoint.PointDataset(0.12, 0.98, 0.76, 15, 0.12, false, 0.12, 0.06, 0.14, 0.12, 0.8),
      new MultiPoint.PointDataset(0.2, 0.9, 0.7, 5, 0.5, true, 0.26, 0.0, 0.3, 0.3, 0.48),
      new MultiPoint.PointDataset(0.36, 0.82, 0.72, 8, 0.26, true, 0.38, 0.02, 0.12, 0.18, 0.68),
      new MultiPoint.PointDataset(0.08, 0.52, 0.36, 6, 0.42, true, 0.32, 0.03, 0.24, 0.24, 0.56),
      new MultiPoint.PointDataset(0.7, 1.0, 0.94, 9, 0.3, true, 0.34, 0.01, 0.16, 0.16, 0.76),
      new MultiPoint.PointDataset(0.16, 0.92, 0.62, 12, 0.56, true, 0.18, 0.01, 0.34, 0.32, 0.44),
      new MultiPoint.PointDataset(0.28, 0.94, 0.82, 10, 0.08, false, 0.48, 0.08, 0.06, 0.08, 0.86),
      new MultiPoint.PointDataset(0.04, 0.98, 0.5, 16, 0.36, true, 0.1, 0.03, 0.28, 0.26, 0.5),
      new MultiPoint.PointDataset(0.48, 0.98, 0.88, 14, 0.52, true, 0.2, 0.0, 0.32, 0.34, 0.46),
      new MultiPoint.PointDataset(0.22, 0.78, 0.66, 4, 0.16, false, 0.55, 0.1, 0.04, 0.06, 0.9),
      new MultiPoint.PointDataset(0.1, 1.0, 0.8, 18, 0.48, true, 0.14, 0.02, 0.36, 0.36, 0.42),
      new MultiPoint.PointDataset(0.4, 0.9, 0.56, 9, 0.6, true, 0.24, 0.0, 0.4, 0.38, 0.4),
      new MultiPoint.PointDataset(0.58, 0.98, 0.74, 7, 0.24, true, 0.3, 0.04, 0.18, 0.18, 0.66)
   };
   private final Random random = new Random();
   private Vec3 offset = Vec3.ZERO;
   private int datasetIndex = 0;
   private int lockedCandidateIndex = -1;
   private int lockedDatasetIndex = -1;
   private int lockedEntityId = -1;
   private int pointHoldTicks = 0;
   private int nextPointSwitchTicks = 0;
   private int lastAttackCount = 0;
   private int nextDatasetSwitchHitCount = -1;
   private int aimAssistEntityId = -1;
   private double aimAssistHeightNoise;
   private long aimAssistHeightNoiseUpdatedAtMs;
   private double aimAssistCurrentPointX;
   private double aimAssistCurrentPointY;
   private double aimAssistCurrentPointZ;
   private double aimAssistNextPointX;
   private double aimAssistNextPointY;
   private double aimAssistNextPointZ;
   private long aimAssistNextPointChangeAtMs;
   private double aimAssistPointLerpSpeed;

   private static MultiPoint.PointPreset buildSpookyPreset() {
      List<MultiPoint.AimPoint> points = new ArrayList<>();
      double[] centerHeights = new double[]{0.98, 0.94, 0.9, 0.86, 0.82, 0.78, 0.74, 0.7, 0.66, 0.62, 0.58, 0.54};

      for (double y : centerHeights) {
         points.add(new MultiPoint.AimPoint(0.0, y, 0.0));
      }

      double[] upperHeights = new double[]{0.94, 0.9, 0.86};
      double[] bodyHeights = new double[]{0.8, 0.74};
      double[] lowerHeights = new double[]{0.68, 0.6};

      for (double y : upperHeights) {
         addCardinalPoints(points, y, 0.12);
         addCardinalPoints(points, y, 0.2);
         addDiagonalPoints(points, y, 0.14);
      }

      for (double y : bodyHeights) {
         addCardinalPoints(points, y, 0.16);
         addCardinalPoints(points, y, 0.28);
         addCardinalPoints(points, y, 0.38);
         addDiagonalPoints(points, y, 0.18);
         addDiagonalPoints(points, y, 0.28);
      }

      for (double y : lowerHeights) {
         addCardinalPoints(points, y, 0.18);
         addCardinalPoints(points, y, 0.32);
         addDiagonalPoints(points, y, 0.22);
         addDiagonalPoints(points, y, 0.32);
      }

      return new MultiPoint.PointPreset(points.toArray(MultiPoint.AimPoint[]::new));
   }

   private static void addCardinalPoints(List<MultiPoint.AimPoint> points, double y, double radius) {
      points.add(new MultiPoint.AimPoint(radius, y, 0.0));
      points.add(new MultiPoint.AimPoint(-radius, y, 0.0));
      points.add(new MultiPoint.AimPoint(0.0, y, radius));
      points.add(new MultiPoint.AimPoint(0.0, y, -radius));
   }

   private static void addDiagonalPoints(List<MultiPoint.AimPoint> points, double y, double radius) {
      points.add(new MultiPoint.AimPoint(radius, y, radius));
      points.add(new MultiPoint.AimPoint(radius, y, -radius));
      points.add(new MultiPoint.AimPoint(-radius, y, radius));
      points.add(new MultiPoint.AimPoint(-radius, y, -radius));
   }

   public Tuple<Vec3, AABB> computeVector(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3 velocity, boolean ignoreWalls) {
      if (Minecraft.getInstance().player == null) {
         return new Tuple<>(entity.getEyePosition(), entity.getBoundingBox());
      }

      if (this.isSpookyDuelV2Mode()) {
         return this.computeSpookyDuelV2Vector(entity, maxDistance, ignoreWalls);
      }

      if (this.isFunTimeMode()) {
         Tuple<List<Vec3>, AABB> candidatePoints = this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls);
         Vec3 bestVector = this.findTeleportVector(entity, candidatePoints.getA(), candidatePoints.getB(), initialAngle, this.currentDataset());
         this.offset = Vec3.ZERO;
         Vec3 fallback = entity.getEyePosition();
         return new Tuple<>((bestVector == null ? fallback : bestVector).add(this.offset), candidatePoints.getB());
      }

      if (this.isAimAssistMode()) {
         return this.computeAimAssistVector(entity, maxDistance, ignoreWalls);
      }

      MultiPoint.PointDataset dataset = this.currentDataset();
      Tuple<List<Vec3>, AABB> candidatePoints = this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset());
      Vec3 bestVector = this.findTeleportVector(entity, candidatePoints.getA(), candidatePoints.getB(), initialAngle, dataset);
      Vec3 fallback = entity.getEyePosition();
      return new Tuple<>(bestVector == null ? fallback : bestVector, candidatePoints.getB());
   }

   public Tuple<List<Vec3>, AABB> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      if (this.isSpookyDuelV2Mode()) {
         return new Tuple<>(this.generateSpookyDuelV2CandidatePoints(entity, maxDistance, ignoreWalls), entity.getBoundingBox());
      } else if (this.isFunTimeMode()) {
         return this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls);
      } else if (this.isAimAssistMode()) {
         Tuple<Vec3, AABB> point = this.computeAimAssistVector(entity, maxDistance, ignoreWalls);
         return new Tuple<>(List.of(point.getA()), point.getB());
      } else {
         return this.generatePresetCandidatePoints(entity, maxDistance, ignoreWalls, this.currentPreset());
      }
   }

   private Tuple<Vec3, AABB> computeAimAssistVector(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      AABB entityBox = entity.getBoundingBox();
      if (Minecraft.getInstance().player == null) {
         return new Tuple<>(entity.getEyePosition(), entityBox);
      }

      this.updateAimAssistPoint(entity);
      double horizontalRadius = entity.getBbWidth() * 0.5;
      double clampedHeight = Mth.clamp(0.85 + this.aimAssistHeightNoise + this.aimAssistCurrentPointY, 0.05, 0.95);
      Vec3 point = entity.position()
         .add(this.aimAssistCurrentPointX * horizontalRadius, entity.getBbHeight() * clampedHeight, this.aimAssistCurrentPointZ * horizontalRadius);
      if (!this.isValidPoint(Minecraft.getInstance().player.getEyePosition(), point, maxDistance, ignoreWalls)) {
         Vec3 fallback = entity.getEyePosition();
         if (this.isValidPoint(Minecraft.getInstance().player.getEyePosition(), fallback, maxDistance, ignoreWalls)) {
            return new Tuple<>(fallback, entityBox);
         }
      }

      return new Tuple<>(point, entityBox);
   }

   private Tuple<List<Vec3>, AABB> generateFunTimeCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      AABB entityBox = entity.getBoundingBox();
      double rawStepY = entityBox.getYsize() / 10.0;
      double stepY = rawStepY <= 0.0 ? 0.1 : rawStepY;
      List<Vec3> list = new ArrayList<>();
      if (Minecraft.getInstance().player == null) {
         return new Tuple<>(list, entityBox);
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();
      Vec3 center = entityBox.getCenter();

      for (double y = entityBox.minY; y <= entityBox.maxY + 1.0E-4; y += stepY) {
         Vec3 point = new Vec3(center.x, y, center.z);
         if (this.isValidPoint(playerEye, point, maxDistance, ignoreWalls)) {
            list.add(point);
         }
      }

      return new Tuple<>(list, entityBox);
   }

   private Tuple<List<Vec3>, AABB> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointDataset dataset) {
      AABB entityBox = entity.getBoundingBox();
      Vec3 center = entityBox.getCenter();
      double height = Math.max(entityBox.getYsize(), 0.1);
      double minY = entityBox.minY + height * dataset.minHeight;
      double maxY = entityBox.minY + height * dataset.maxHeight;
      double stepY = Math.max((maxY - minY) / Math.max(dataset.verticalSamples, 1), 0.05);
      double lateralRadius = Math.min(entityBox.getXsize(), entityBox.getZsize()) * dataset.lateralRadius;
      List<Vec3> list = new ArrayList<>();
      if (Minecraft.getInstance().player == null) {
         return new Tuple<>(list, entityBox);
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();

      for (double y = minY; y <= maxY + 1.0E-4; y += stepY) {
         this.addValidPoint(list, playerEye, new Vec3(center.x, y, center.z), maxDistance, ignoreWalls);
         if (!(lateralRadius <= 0.0)) {
            this.addValidPoint(list, playerEye, new Vec3(center.x + lateralRadius, y, center.z), maxDistance, ignoreWalls);
            this.addValidPoint(list, playerEye, new Vec3(center.x - lateralRadius, y, center.z), maxDistance, ignoreWalls);
            this.addValidPoint(list, playerEye, new Vec3(center.x, y, center.z + lateralRadius), maxDistance, ignoreWalls);
            this.addValidPoint(list, playerEye, new Vec3(center.x, y, center.z - lateralRadius), maxDistance, ignoreWalls);
            if (dataset.diagonalPoints) {
               double diagonal = lateralRadius * 0.70710678118;
               this.addValidPoint(list, playerEye, new Vec3(center.x + diagonal, y, center.z + diagonal), maxDistance, ignoreWalls);
               this.addValidPoint(list, playerEye, new Vec3(center.x + diagonal, y, center.z - diagonal), maxDistance, ignoreWalls);
               this.addValidPoint(list, playerEye, new Vec3(center.x - diagonal, y, center.z + diagonal), maxDistance, ignoreWalls);
               this.addValidPoint(list, playerEye, new Vec3(center.x - diagonal, y, center.z - diagonal), maxDistance, ignoreWalls);
            }
         }
      }

      return new Tuple<>(list, entityBox);
   }

   private Tuple<List<Vec3>, AABB> generatePresetCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls, MultiPoint.PointPreset preset) {
      AABB entityBox = entity.getBoundingBox();
      Vec3 center = entityBox.getCenter();
      double height = Math.max(entityBox.getYsize(), 0.1);
      double halfX = entityBox.getXsize() * 0.5;
      double halfZ = entityBox.getZsize() * 0.5;
      List<Vec3> list = new ArrayList<>();
      if (Minecraft.getInstance().player == null) {
         return new Tuple<>(list, entityBox);
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();

      for (MultiPoint.AimPoint aimPoint : preset.points) {
         Vec3 point = new Vec3(center.x + halfX * aimPoint.x, entityBox.minY + height * aimPoint.y, center.z + halfZ * aimPoint.z);
         this.addValidPoint(list, playerEye, point, maxDistance, ignoreWalls);
      }

      return new Tuple<>(list, entityBox);
   }

   public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      if (Minecraft.getInstance().player == null) {
         return false;
      } else if (this.isSpookyDuelV2Mode()) {
         return !this.generateSpookyDuelV2CandidatePoints(entity, maxDistance, ignoreWalls).isEmpty();
      } else if (this.isFunTimeMode()) {
         return !this.generateFunTimeCandidatePoints(entity, maxDistance, ignoreWalls).getA().isEmpty();
      } else {
         return this.isAimAssistMode()
            ? this.isValidPoint(
               Minecraft.getInstance().player.getEyePosition(), this.computeAimAssistVector(entity, maxDistance, ignoreWalls).getA(), maxDistance, ignoreWalls
            )
            : !this.generateCandidatePoints(entity, maxDistance, ignoreWalls, this.currentDataset()).getA().isEmpty();
      }
   }

   private void addValidPoint(List<Vec3> points, Vec3 startPoint, Vec3 endPoint, float maxDistance, boolean ignoreWalls) {
      if (this.isValidPoint(startPoint, endPoint, maxDistance, ignoreWalls)) {
         points.add(endPoint);
      }
   }

   private boolean isSpookyDuelV2Mode() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura != null && aura.isEnabled() && aura.getMode().is("SpookyDuelV2");
   }

   private Tuple<Vec3, AABB> computeSpookyDuelV2Vector(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      AABB entityBox = entity.getBoundingBox();
      if (Minecraft.getInstance().player == null) {
         return new Tuple<>(entity.getEyePosition(), entityBox);
      }

      List<Vec3> candidates = this.generateSpookyDuelV2CandidatePoints(entity, maxDistance, ignoreWalls);
      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();
      double tight = Math.max(0.0, maxDistance - 0.3);
      double tightSq = tight * tight;
      List<Vec3> suitable = new ArrayList<>();

      for (Vec3 point : candidates) {
         if (playerEye.distanceToSqr(point) < tightSq) {
            suitable.add(point);
         }
      }

      Vec3 bestPoint = this.findValidCenterOrNearest(suitable, maxDistance, ignoreWalls);
      if (bestPoint == null) {
         bestPoint = this.findValidCenterOrNearest(candidates, maxDistance, ignoreWalls);
      }

      if (bestPoint == null) {
         bestPoint = this.nearestToEye(candidates, playerEye);
      }

      return new Tuple<>(bestPoint == null ? entity.getEyePosition() : bestPoint, entityBox);
   }

   private List<Vec3> generateSpookyDuelV2CandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      AABB entityBox = entity.getBoundingBox();
      List<Vec3> list = new ArrayList<>();
      if (Minecraft.getInstance().player == null) {
         return list;
      }

      double lenX = entityBox.getXsize();
      double lenY = entityBox.getYsize();
      double lenZ = entityBox.getZsize();
      int stepsX = this.computeGridSteps(lenX);
      int stepsZ = this.computeGridSteps(lenZ);
      int ySteps = Math.max(2, 10);
      double stepY = lenY / (ySteps - 1);
      double stepX = stepsX <= 1 ? 0.0 : lenX / (stepsX - 1);
      double stepZ = stepsZ <= 1 ? 0.0 : lenZ / (stepsZ - 1);
      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();

      for (int iy = 0; iy < ySteps; iy++) {
         double y = entityBox.minY + iy * stepY;

         for (int ix = 0; ix < stepsX; ix++) {
            double x = entityBox.minX + ix * stepX;

            for (int iz = 0; iz < stepsZ; iz++) {
               double z = entityBox.minZ + iz * stepZ;
               Vec3 point = new Vec3(x, y, z);
               if (this.isValidPoint(playerEye, point, maxDistance, ignoreWalls)) {
                  list.add(point);
               }
            }
         }
      }

      return list;
   }

   private int computeGridSteps(double length) {
      if (length <= 0.0) {
         return 1;
      }

      int bySpacing = (int)Math.ceil(length / 0.15) + 1;
      return Math.max(2, Math.min(bySpacing, 14));
   }

   private Vec3 findValidCenterOrNearest(List<Vec3> points, float maxDistance, boolean ignoreWalls) {
      if (points.isEmpty()) {
         return null;
      }

      if (Minecraft.getInstance().player == null) {
         return null;
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();
      Vec3 centroid = this.computeCentroid(points);
      if (this.isValidPoint(playerEye, centroid, maxDistance, ignoreWalls)) {
         return centroid;
      }

      Vec3 nearest = null;
      double best = Double.MAX_VALUE;

      for (Vec3 point : points) {
         if (this.isValidPoint(playerEye, point, maxDistance, ignoreWalls)) {
            double distance = point.distanceToSqr(centroid);
            if (distance < best) {
               best = distance;
               nearest = point;
            }
         }
      }

      return nearest;
   }

   private Vec3 computeCentroid(List<Vec3> points) {
      double sx = 0.0;
      double sy = 0.0;
      double sz = 0.0;

      for (Vec3 point : points) {
         sx += point.x;
         sy += point.y;
         sz += point.z;
      }

      return new Vec3(sx / points.size(), sy / points.size(), sz / points.size());
   }

   private Vec3 nearestToEye(List<Vec3> points, Vec3 playerEye) {
      Vec3 nearest = null;
      double best = Double.MAX_VALUE;

      for (Vec3 point : points) {
         double distance = point.distanceToSqr(playerEye);
         if (distance < best) {
            best = distance;
            nearest = point;
         }
      }

      return nearest;
   }

   private boolean isValidPoint(Vec3 startPoint, Vec3 endPoint, float maxDistance, boolean ignoreWalls) {
      if (startPoint.distanceToSqr(endPoint) > maxDistance * maxDistance) {
         return false;
      }

      if (ignoreWalls) {
         if (this.isWallBypassEnabled()) {
            HitResult result = RaycastAngle.raycast(startPoint, endPoint, Block.COLLIDER);
            if (result == null || result.getType() != Type.BLOCK) {
               return true;
            } else {
               return result instanceof BlockHitResult blockHit ? WallUtils.isPassthrough(blockHit) : false;
            }
         } else {
            return true;
         }
      } else {
         HitResult result = RaycastAngle.raycast(startPoint, endPoint, Block.COLLIDER);
         return result == null || result.getType() != Type.BLOCK;
      }
   }

   private boolean isWallBypassEnabled() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura != null && aura.isEnabled() && aura.wallBypass.getValue();
   }

   private Vec3 findBestVector(List<Vec3> candidatePoints, AABB entityBox, Angle initialAngle, MultiPoint.PointDataset dataset) {
      if (Minecraft.getInstance().player == null) {
         return null;
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();
      return candidatePoints.stream()
         .min(Comparator.comparingDouble(point -> this.calculatePointScore(playerEye, point, entityBox, initialAngle, dataset)))
         .orElse(null);
   }

   private Vec3 findTeleportVector(LivingEntity entity, List<Vec3> candidatePoints, AABB entityBox, Angle initialAngle, MultiPoint.PointDataset dataset) {
      if (candidatePoints.isEmpty()) {
         this.resetLockedPoint();
         return null;
      }

      this.pointHoldTicks++;
      boolean entityChanged = this.lockedEntityId != entity.getId();
      boolean shouldSwitchPoint = this.nextPointSwitchTicks <= 0 || this.pointHoldTicks >= this.nextPointSwitchTicks;
      if (entityChanged
         || this.lockedDatasetIndex != this.datasetIndex
         || this.lockedCandidateIndex < 0
         || this.lockedCandidateIndex >= candidatePoints.size()
         || shouldSwitchPoint) {
         this.lockedCandidateIndex = this.findTeleportVectorIndex(candidatePoints, entityBox, initialAngle, dataset, this.lockedCandidateIndex);
         this.lockedDatasetIndex = this.datasetIndex;
         this.lockedEntityId = entity.getId();
         this.pointHoldTicks = 0;
         this.nextPointSwitchTicks = this.randomPointHoldTicks();
      }

      return candidatePoints.get(this.lockedCandidateIndex);
   }

   private int findTeleportVectorIndex(List<Vec3> candidatePoints, AABB entityBox, Angle initialAngle, MultiPoint.PointDataset dataset, int previousIndex) {
      List<MultiPoint.PointScore> scores = new ArrayList<>();
      if (Minecraft.getInstance().player == null) {
         return 0;
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();

      for (int i = 0; i < candidatePoints.size(); i++) {
         double score = this.calculatePointScore(playerEye, candidatePoints.get(i), entityBox, initialAngle, dataset);
         scores.add(new MultiPoint.PointScore(i, score));
      }

      scores.sort(Comparator.comparingDouble(MultiPoint.PointScore::score));
      int limit = Math.min(4, scores.size());
      if (limit <= 1) {
         return scores.getFirst().index();
      }

      List<Integer> selectable = new ArrayList<>();
      Vec3 previousPoint = previousIndex >= 0 && previousIndex < candidatePoints.size() ? candidatePoints.get(previousIndex) : null;

      for (int i = 0; i < limit; i++) {
         int index = scores.get(i).index();
         if (index != previousIndex && (previousPoint == null || !(previousPoint.distanceTo(candidatePoints.get(index)) < 0.045))) {
            selectable.add(index);
         }
      }

      if (selectable.isEmpty()) {
         for (int i = 0; i < limit; i++) {
            int index = scores.get(i).index();
            if (index != previousIndex) {
               selectable.add(index);
            }
         }
      }

      return selectable.isEmpty() ? scores.getFirst().index() : selectable.get(this.random.nextInt(selectable.size()));
   }

   private Vec3 findFunTimeBestVector(List<Vec3> candidatePoints, Angle initialAngle) {
      if (Minecraft.getInstance().player == null) {
         return null;
      }

      Vec3 playerEye = Minecraft.getInstance().player.getEyePosition();
      return candidatePoints.stream().min(Comparator.comparingDouble(point -> this.calculateRotationDifference(playerEye, point, initialAngle))).orElse(null);
   }

   private double calculatePointScore(Vec3 startPoint, Vec3 endPoint, AABB entityBox, Angle initialAngle, MultiPoint.PointDataset dataset) {
      double rotationDifference = this.calculateRotationDifference(startPoint, endPoint, initialAngle);
      double height = Math.max(entityBox.getYsize(), 0.1);
      double heightFactor = (endPoint.y - entityBox.minY) / height;
      double heightPenalty = Math.abs(heightFactor - dataset.preferredHeight) * dataset.heightBias;
      double lateralDistance = Math.hypot(endPoint.x - entityBox.getCenter().x, endPoint.z - entityBox.getCenter().z);
      double lateralPenalty = lateralDistance * dataset.lateralBias;
      double noise = this.random.nextDouble() * dataset.randomScore;
      return rotationDifference + heightPenalty + lateralPenalty + noise;
   }

   private double calculateRotationDifference(Vec3 startPoint, Vec3 endPoint, Angle initialAngle) {
      Angle targetAngle = MathAngle.fromVec3d(endPoint.subtract(startPoint));
      Angle delta = MathAngle.calculateDelta(initialAngle, targetAngle);
      return Math.hypot(delta.getYaw(), delta.getPitch());
   }

   private void updateOffset(Vec3 velocity, MultiPoint.PointDataset dataset) {
      Vec3 drift = new Vec3(
         this.random.nextGaussian() * dataset.offsetScale * 0.45,
         this.random.nextGaussian() * dataset.offsetScale * 0.22,
         this.random.nextGaussian() * dataset.offsetScale * 0.45
      );
      if (velocity == null) {
         this.offset = this.clampOffset(this.offset.scale(dataset.offsetRetention).add(drift));
      } else {
         Vec3 velocityPush = new Vec3(
            this.random.nextGaussian() * velocity.x * dataset.offsetScale * 2.0,
            this.random.nextGaussian() * velocity.y * dataset.offsetScale * 1.35,
            this.random.nextGaussian() * velocity.z * dataset.offsetScale * 2.0
         );
         this.offset = this.clampOffset(this.offset.scale(dataset.offsetRetention).add(velocityPush).add(drift));
      }
   }

   private void updateFunTimeOffset(Vec3 velocity) {
      if (velocity == null) {
         this.offset = Vec3.ZERO;
      } else {
         this.offset = new Vec3(this.random.nextGaussian() * velocity.x, this.random.nextGaussian() * velocity.y, this.random.nextGaussian() * velocity.z);
      }
   }

   private boolean isFunTimeMode() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura != null && aura.isEnabled() && aura.getMode().is("FunTime");
   }

   private boolean isAimAssistMode() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura != null && aura.isEnabled() && aura.getMode().is("AimAssist");
   }

   private MultiPoint.PointPreset currentPreset() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura == null || !aura.isEnabled() || !aura.getMode().is("SpookyTime") && !aura.getMode().is("SPAngle") ? DEFAULT_PRESET : SPOOKY_PRESET;
   }

   private Vec3 clampOffset(Vec3 value) {
      double length = value.length();
      return !(length <= 0.5) && length != 0.0 ? value.scale(0.5 / length) : value;
   }

   private void updateAimAssistPoint(LivingEntity entity) {
      long now = System.currentTimeMillis();
      if (this.aimAssistEntityId != entity.getId()) {
         this.aimAssistEntityId = entity.getId();
         this.aimAssistCurrentPointX = 0.0;
         this.aimAssistCurrentPointY = 0.0;
         this.aimAssistCurrentPointZ = 0.0;
         this.aimAssistHeightNoise = this.randomDouble(-0.02, 0.02);
         this.aimAssistHeightNoiseUpdatedAtMs = now;
         this.aimAssistPointLerpSpeed = this.randomDouble(0.025, 0.065);
         this.randomizeAimAssistPoint(now);
      }

      if (now - this.aimAssistHeightNoiseUpdatedAtMs > this.randomLong(180L, 450L)) {
         this.aimAssistHeightNoise = Mth.clamp(this.aimAssistHeightNoise + this.randomDouble(-0.006, 0.006), -0.025, 0.025);
         this.aimAssistHeightNoiseUpdatedAtMs = now;
      }

      if (now >= this.aimAssistNextPointChangeAtMs) {
         this.randomizeAimAssistPoint(now);
      }

      double lerpSpeed = this.aimAssistPointLerpSpeed + this.randomDouble(-0.002, 0.002);
      this.aimAssistCurrentPointX = this.aimAssistCurrentPointX + (this.aimAssistNextPointX - this.aimAssistCurrentPointX) * lerpSpeed;
      this.aimAssistCurrentPointY = this.aimAssistCurrentPointY + (this.aimAssistNextPointY - this.aimAssistCurrentPointY) * lerpSpeed;
      this.aimAssistCurrentPointZ = this.aimAssistCurrentPointZ + (this.aimAssistNextPointZ - this.aimAssistCurrentPointZ) * lerpSpeed;
   }

   private void randomizeAimAssistPoint(long now) {
      this.aimAssistNextPointX = this.randomDouble(-0.35, 0.35);
      this.aimAssistNextPointY = this.randomDouble(-0.12, 0.12);
      this.aimAssistNextPointZ = this.randomDouble(-0.35, 0.35);
      this.aimAssistNextPointChangeAtMs = now + this.randomLong(600L, 1900L);
      this.aimAssistPointLerpSpeed = this.randomDouble(0.008, 0.025);
   }

   private double randomDouble(double min, double max) {
      return min + this.random.nextDouble() * (max - min);
   }

   private long randomLong(long min, long max) {
      return min + (long)(this.random.nextDouble() * (max - min + 1L));
   }

   private MultiPoint.PointDataset currentDataset() {
      AuraFeature aura = AuraFeature.getInstance();
      StrikeManager attackHandler = aura == null ? null : aura.getAttackPerpetrator().getAttackHandler();
      int attackCount = attackHandler == null ? this.lastAttackCount : attackHandler.getCount();
      if (this.nextDatasetSwitchHitCount >= 0 && attackCount >= this.lastAttackCount) {
         this.lastAttackCount = attackCount;
         if (attackCount < this.nextDatasetSwitchHitCount) {
            return DATASETS[this.datasetIndex];
         }

         int step = 1 + this.random.nextInt(DATASETS.length - 1);
         this.datasetIndex = (this.datasetIndex + step) % DATASETS.length;
         this.resetLockedPoint();
         this.nextDatasetSwitchHitCount = attackCount + this.randomHitsUntilSwitch();
         return DATASETS[this.datasetIndex];
      } else {
         this.lastAttackCount = attackCount;
         this.nextDatasetSwitchHitCount = attackCount + this.randomHitsUntilSwitch();
         return DATASETS[this.datasetIndex];
      }
   }

   private void resetLockedPoint() {
      this.lockedCandidateIndex = -1;
      this.lockedDatasetIndex = -1;
      this.lockedEntityId = -1;
      this.pointHoldTicks = 0;
      this.nextPointSwitchTicks = 0;
   }

   private int randomPointHoldTicks() {
      return 41 + this.random.nextInt(5);
   }

   private int randomHitsUntilSwitch() {
      return 15 + this.random.nextInt(3);
   }

   private static class AimPoint {
      private final double x;
      private final double y;
      private final double z;

      private AimPoint(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }

   private static class PointDataset {
      private final double minHeight;
      private final double maxHeight;
      private final double preferredHeight;
      private final int verticalSamples;
      private final double lateralRadius;
      private final boolean diagonalPoints;
      private final double heightBias;
      private final double lateralBias;
      private final double randomScore;
      private final double offsetScale;
      private final double offsetRetention;

      private PointDataset(
         double minHeight,
         double maxHeight,
         double preferredHeight,
         int verticalSamples,
         double lateralRadius,
         boolean diagonalPoints,
         double heightBias,
         double lateralBias,
         double randomScore,
         double offsetScale,
         double offsetRetention
      ) {
         this.minHeight = minHeight;
         this.maxHeight = maxHeight;
         this.preferredHeight = preferredHeight;
         this.verticalSamples = verticalSamples;
         this.lateralRadius = lateralRadius;
         this.diagonalPoints = diagonalPoints;
         this.heightBias = heightBias;
         this.lateralBias = lateralBias;
         this.randomScore = randomScore;
         this.offsetScale = offsetScale;
         this.offsetRetention = offsetRetention;
      }
   }

   private static class PointPreset {
      private final MultiPoint.AimPoint[] points;

      private PointPreset(MultiPoint.AimPoint... points) {
         this.points = points;
      }
   }

   private record PointScore(int index, double score) {
   }
}

