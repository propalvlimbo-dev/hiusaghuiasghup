package org.xrose.utils.combat.aura.attack;

import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.utils.combat.aura.Angle;

public class StrikerConstructor {
   private StrikeManager attackHandler = new StrikeManager();

   public void tick() {
      this.getAttackHandler().tick();
   }

   public void onPacket(PacketSendEvent event) {
      this.getAttackHandler().onPacket(event);
   }

   public void performAttack(StrikerConstructor.AttackPerpetratorConfigurable configurable) {
      this.getAttackHandler().handleAttack(configurable);
   }

   public StrikeManager getAttackHandler() {
      if (this.attackHandler == null) {
         this.attackHandler = new StrikeManager();
      }

      return this.attackHandler;
   }

   public static class AttackPerpetratorConfigurable {
      private final LivingEntity target;
      private final Angle angle;
      private final float maximumRange;
      private final boolean onlyCritical;
      private final boolean smartCriticals;
      private final boolean shouldBreakShield;
      private final boolean shouldUnPressShield;
      private final boolean eatAndAttack;
      private final boolean multiPoints;
      private final boolean ignoreWalls;
      private final boolean tpsSync;
      private final boolean legacyPvp;
      private final AABB box;
      private final ModeSetting aimMode;
      private final String sprintMode;

      public AttackPerpetratorConfigurable(LivingEntity target, Angle angle, float maximumRange, Set<String> options, ModeSetting aimMode, AABB box) {
         this(target, angle, maximumRange, options, aimMode, box, options.contains("Only Crits"), false, false, false, "Legit");
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target, Angle angle, float maximumRange, Set<String> options, ModeSetting aimMode, AABB box, boolean onlyCritical
      ) {
         this(target, angle, maximumRange, options, aimMode, box, onlyCritical, false, false, false, "Legit");
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target,
         Angle angle,
         float maximumRange,
         Set<String> options,
         ModeSetting aimMode,
         AABB box,
         boolean onlyCritical,
         boolean tpsSync,
         boolean legacyPvp
      ) {
         this(target, angle, maximumRange, options, aimMode, box, onlyCritical, false, tpsSync, legacyPvp, "Legit");
      }

      public AttackPerpetratorConfigurable(
         LivingEntity target,
         Angle angle,
         float maximumRange,
         Set<String> options,
         ModeSetting aimMode,
         AABB box,
         boolean onlyCritical,
         boolean smartCriticals,
         boolean tpsSync,
         boolean legacyPvp,
         String sprintMode
      ) {
         Set<String> safeOptions = options == null ? Set.of() : options;
         this.target = target;
         this.angle = angle;
         this.maximumRange = maximumRange;
         this.onlyCritical = onlyCritical;
         this.smartCriticals = smartCriticals;
         this.shouldBreakShield = safeOptions.contains("Break Shield");
         this.shouldUnPressShield = safeOptions.contains("Release Shield");
         this.eatAndAttack = safeOptions.contains("Pause While Using");
         this.multiPoints = safeOptions.contains("Multi Points");
         this.ignoreWalls = safeOptions.contains("Ignore Walls");
         this.tpsSync = tpsSync;
         this.legacyPvp = legacyPvp;
         this.box = box;
         this.aimMode = aimMode;
         this.sprintMode = sprintMode != null ? sprintMode : "Legit";
      }

      public LivingEntity getTarget() {
         return this.target;
      }

      public Angle getAngle() {
         return this.angle;
      }

      public float getMaximumRange() {
         return this.maximumRange;
      }

      public boolean isOnlyCritical() {
         return this.onlyCritical;
      }

      public boolean isSmartCriticals() {
         return this.smartCriticals;
      }

      public boolean isShouldBreakShield() {
         return this.shouldBreakShield;
      }

      public boolean isShouldUnPressShield() {
         return this.shouldUnPressShield;
      }

      public boolean isEatAndAttack() {
         return this.eatAndAttack;
      }

      public boolean isMultiPoints() {
         return this.multiPoints;
      }

      public boolean isIgnoreWalls() {
         return this.ignoreWalls;
      }

      public boolean isTpsSync() {
         return this.tpsSync;
      }

      public boolean isLegacyPvp() {
         return this.legacyPvp;
      }

      public AABB getBox() {
         return this.box;
      }

      public ModeSetting getAimMode() {
         return this.aimMode;
      }

      public String getSprintMode() {
         return this.sprintMode;
      }
   }
}

