package org.xrose.utils.combat.aura.target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.FriendManager;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.impl.LinearConstructor;

public class TargetFinder {
   private static final int FAKE_PLAYER_ENTITY_ID = -81374659;
   private final MultiPoint pointFinder = new MultiPoint();
   private LivingEntity currentTarget;
   private List<LivingEntity> potentialTargets = List.of();

   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   public void releaseTarget() {
      this.currentTarget = null;
   }

   public void validateTarget(Predicate<LivingEntity> predicate) {
      if (this.currentTarget == null || !predicate.test(this.currentTarget) || !this.potentialTargets.contains(this.currentTarget)) {
         LivingEntity nextTarget = this.findFirstMatch(predicate).orElse(null);
         if (nextTarget == null) {
            if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
               this.releaseTarget();
            }
         } else {
            this.currentTarget = nextTarget;
         }
      }
   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
      this.searchTargets(entities, maxDistance, maxFov, ignoreWalls, entity -> true);
   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls, Predicate<LivingEntity> predicate) {
      TargetFinder.SortMode sortMode = this.resolveSortMode();
      boolean needsFov = maxFov < 360.0F || sortMode == TargetFinder.SortMode.FOV || sortMode == TargetFinder.SortMode.COMBINED;
      if (this.currentTarget != null
         && (
            !predicate.test(this.currentTarget)
               || !this.pointFinder.hasValidPoint(this.currentTarget, maxDistance, ignoreWalls)
               || needsFov && this.getFov(this.currentTarget, maxDistance, ignoreWalls) > maxFov
         )) {
         this.releaseTarget();
      }

      this.potentialTargets = this.createTargets(entities, maxDistance, maxFov, ignoreWalls, predicate, sortMode, needsFov);
   }

   private double getFov(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Vec3 attackVector = this.pointFinder
         .computeVector(entity, maxDistance, AngleConnection.INSTANCE.getRotation(), new LinearConstructor().randomValue(), ignoreWalls)
         .getA();
      return RaycastAngle.rayTrace(maxDistance, entity.getBoundingBox())
         ? 0.0
         : AngleConnection.computeRotationDifference(MathAngle.cameraAngle(), MathAngle.calculateAngle(attackVector));
   }

   private List<LivingEntity> createTargets(
      Iterable<Entity> entities,
      float maxDistance,
      float maxFov,
      boolean ignoreWalls,
      Predicate<LivingEntity> predicate,
      TargetFinder.SortMode sortMode,
      boolean needsFov
   ) {
      List<TargetFinder.TargetCandidate> candidates = new ArrayList<>();
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return List.of();
      }

      double maxDistanceSqr = maxDistance * maxDistance;

      for (Entity entity : entities) {
         if (entity instanceof LivingEntity living
            && predicate.test(living)
            && !(living.distanceToSqr(client.player) > maxDistanceSqr)
            && this.pointFinder.hasValidPoint(living, maxDistance, ignoreWalls)) {
            double fov = 0.0;
            if (needsFov) {
               fov = this.getFov(living, maxDistance, ignoreWalls);
               if (fov >= maxFov) {
                  continue;
               }
            }

            candidates.add(
               new TargetFinder.TargetCandidate(
                  living, living.distanceTo(client.player), living.getHealth() + living.getAbsorptionAmount(), living.getArmorValue(), fov
               )
            );
         }
      }

      TargetFinder.PriorityBounds bounds = TargetFinder.PriorityBounds.of(candidates);
      candidates.sort(this.getComparator(sortMode, bounds));
      List<LivingEntity> result = new ArrayList<>(candidates.size());

      for (TargetFinder.TargetCandidate candidate : candidates) {
         result.add(candidate.entity());
      }

      return result;
   }

   private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
      return this.potentialTargets.stream().filter(predicate).findFirst();
   }

   private TargetFinder.SortMode resolveSortMode() {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura != null && aura.getTargetPriority() != null) {
         return switch ((String)aura.getTargetPriority().getValue()) {
            case "Health" -> TargetFinder.SortMode.HEALTH;
            case "Armor" -> TargetFinder.SortMode.ARMOR;
            case "FOV" -> TargetFinder.SortMode.FOV;
            case "Combined" -> TargetFinder.SortMode.COMBINED;
            default -> TargetFinder.SortMode.DISTANCE;
         };
      } else {
         return TargetFinder.SortMode.DISTANCE;
      }
   }

   private Comparator<TargetFinder.TargetCandidate> getComparator(TargetFinder.SortMode sortMode, TargetFinder.PriorityBounds bounds) {
      return switch (sortMode) {
         case DISTANCE -> Comparator.comparingDouble(TargetFinder.TargetCandidate::distance);
         case HEALTH -> Comparator.comparingDouble(TargetFinder.TargetCandidate::health)
            .thenComparing(Comparator.comparingInt(TargetFinder.TargetCandidate::armor).reversed())
            .thenComparingDouble(TargetFinder.TargetCandidate::distance)
            .thenComparingDouble(TargetFinder.TargetCandidate::fov);
         case ARMOR -> Comparator.comparingInt(TargetFinder.TargetCandidate::armor)
            .reversed()
            .thenComparingDouble(TargetFinder.TargetCandidate::health)
            .thenComparingDouble(TargetFinder.TargetCandidate::distance)
            .thenComparingDouble(TargetFinder.TargetCandidate::fov);
         case FOV -> Comparator.comparingDouble(TargetFinder.TargetCandidate::fov)
            .thenComparingDouble(TargetFinder.TargetCandidate::distance)
            .thenComparingDouble(TargetFinder.TargetCandidate::health)
            .thenComparing(Comparator.comparingInt(TargetFinder.TargetCandidate::armor).reversed());
         case COMBINED -> Comparator.comparingDouble(bounds::combinedScore)
            .thenComparingDouble(TargetFinder.TargetCandidate::distance)
            .thenComparingDouble(TargetFinder.TargetCandidate::health)
            .thenComparing(Comparator.comparingInt(TargetFinder.TargetCandidate::armor).reversed())
            .thenComparingDouble(TargetFinder.TargetCandidate::fov);
      };
   }

   public static class EntityFilter {
      private final Set<String> targetSettings;

      public EntityFilter(Set<String> targetSettings) {
         this.targetSettings = targetSettings;
      }

      public boolean isValid(LivingEntity entity) {
         Minecraft client = Minecraft.getInstance();
         if (client.player == null || entity == client.player || !entity.isAlive() || entity.getHealth() <= 0.0F) {
            return false;
         } else if (entity.getId() == -81374659) {
            return false;
         } else if (entity.isInvisible() && !this.targetSettings.contains("Invisible")) {
            return false;
         } else if (entity instanceof Player player) {
            return FriendManager.INSTANCE.isFriend(player.getGameProfile().name())
               ? this.targetSettings.contains("Friends")
               : this.targetSettings.contains("Players");
         } else if (entity instanceof Animal) {
            return this.targetSettings.contains("Animals");
         } else if (entity instanceof Mob) {
            return this.targetSettings.contains("Mobs");
         } else {
            return entity instanceof ArmorStand ? this.targetSettings.contains("Armor Stands") : false;
         }
      }
   }

   private record PriorityBounds(
      float minDistance, float maxDistance, float minHealth, float maxHealth, int minArmor, int maxArmor, double minFov, double maxFov
   ) {
      private static TargetFinder.PriorityBounds of(List<TargetFinder.TargetCandidate> candidates) {
         if (candidates.isEmpty()) {
            return new TargetFinder.PriorityBounds(0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0, 0.0);
         }

         float minDistance = Float.MAX_VALUE;
         float maxDistance = -Float.MAX_VALUE;
         float minHealth = Float.MAX_VALUE;
         float maxHealth = -Float.MAX_VALUE;
         int minArmor = Integer.MAX_VALUE;
         int maxArmor = Integer.MIN_VALUE;
         double minFov = Double.MAX_VALUE;
         double maxFov = -Double.MAX_VALUE;

         for (TargetFinder.TargetCandidate candidate : candidates) {
            minDistance = Math.min(minDistance, candidate.distance());
            maxDistance = Math.max(maxDistance, candidate.distance());
            minHealth = Math.min(minHealth, candidate.health());
            maxHealth = Math.max(maxHealth, candidate.health());
            minArmor = Math.min(minArmor, candidate.armor());
            maxArmor = Math.max(maxArmor, candidate.armor());
            minFov = Math.min(minFov, candidate.fov());
            maxFov = Math.max(maxFov, candidate.fov());
         }

         return new TargetFinder.PriorityBounds(minDistance, maxDistance, minHealth, maxHealth, minArmor, maxArmor, minFov, maxFov);
      }

      private double combinedScore(TargetFinder.TargetCandidate candidate) {
         return this.normalize(candidate.distance(), this.minDistance, this.maxDistance)
            + this.normalize(candidate.health(), this.minHealth, this.maxHealth)
            + 1.0
            - this.normalize(candidate.armor(), this.minArmor, this.maxArmor)
            + this.normalize(candidate.fov(), this.minFov, this.maxFov);
      }

      private double normalize(double value, double min, double max) {
         return Double.compare(max, min) == 0 ? 0.0 : (value - min) / (max - min);
      }
   }

   public enum SortMode {
      DISTANCE,
      HEALTH,
      ARMOR,
      FOV,
      COMBINED;

      // $VF: synthetic method
      private static TargetFinder.SortMode[] $values() {
         return new TargetFinder.SortMode[]{DISTANCE, HEALTH, ARMOR, FOV, COMBINED};
      }
   }

   private record TargetCandidate(LivingEntity entity, float distance, float health, int armor, double fov) {
   }
}

