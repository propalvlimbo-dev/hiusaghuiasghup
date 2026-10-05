package org.xrose.utils.combat.aura.attack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.impl.movement.AirStuckFeature;
import org.xrose.feature.impl.movement.SprintFeature;
import org.xrose.mixin.accessor.LivingEntityAccessor;
import org.xrose.utils.combat.PlayerInteractionHelper;
import org.xrose.utils.combat.PlayerSimulation;
import org.xrose.utils.combat.ServerSprintTracker;
import org.xrose.utils.combat.StopWatch;
import org.xrose.utils.combat.aura.context.AutoRegressionContext;
import org.xrose.utils.combat.aura.rotations.UniversalRotation;
import org.xrose.utils.combat.aura.target.RaycastAngle;
import org.xrose.utils.combat.aura.util.MathUtils;

public class StrikeManager {
   private static final long CRIT_STARTUP_HOLD_MS = 450L;
   private long nextAttackDelayMs = 550L;
   private final StopWatch attackTimer = new StopWatch();
   private final Pressing clickScheduler = new Pressing();
   private int count = 0;
   private int tickCounter = 0;
   private int shieldSuppressedUntilTick = -1;
   private int lastShieldReleaseTick = -1;
   private boolean waitingForPostReleaseTick = false;
   private boolean wasShieldUpBeforeAttack = false;
   private boolean wasSprintingBeforeAttack = false;
   private boolean onlyCriticalMode = false;
   private boolean forceCriticalNextAttack = false;
   private boolean waitingForFallDistance = false;
   private float requiredFallDistance = 0.0F;
   private boolean sprintResetPerformedThisTick = false;
   private boolean waterAttackInProgress = false;
   private boolean wasSprintingBeforeWaterAttack = false;
   private boolean wasInWater = false;
   private boolean wasInLava = false;
   private boolean wasClimbing = false;
   private boolean wasInCobweb = false;
   private boolean criticalInCobweb = false;
   private boolean sprintResetPending = false;
   private final StopWatch sprintResetTimer = new StopWatch();
   private final StopWatch groundSprintResetCooldown = new StopWatch();
   private final StopWatch sprintForceTimer = new StopWatch();
   private final StopWatch fallDistanceTimer = new StopWatch();
   private final StopWatch waterAttackFinishTimer = new StopWatch();

   void tick() {
      this.tickCounter++;
      this.syncShieldState();
      if (Minecraft.getInstance().player != null) {
         this.checkSprintResetConditions();
         if (this.waterAttackInProgress && this.waterAttackFinishTimer.finished(0.0)) {
            if (this.wasSprintingBeforeWaterAttack) {
               Minecraft.getInstance().player.setSprinting(true);
            }

            this.waterAttackInProgress = false;
         }

         if (this.sprintResetPending && this.sprintResetTimer.finished(0.0)) {
            this.performSprintReset();
         }

         if (Minecraft.getInstance().player.onGround() && this.onlyCriticalMode && this.waitingForFallDistance) {
            this.waitingForFallDistance = false;
            this.forceCriticalNextAttack = false;
            this.sprintResetPerformedThisTick = false;
         }

         if (this.waitingForFallDistance && this.fallDistanceTimer.finished(500.0)) {
            this.waitingForFallDistance = false;
            this.forceCriticalNextAttack = false;
         }

         if (this.waitingForFallDistance && Minecraft.getInstance().player.fallDistance < this.requiredFallDistance) {
            this.forceCriticalNextAttack = false;
         }

         this.sprintResetPerformedThisTick = false;
      }
   }

   void onPacket(PacketSendEvent e) {
      if (e.getPhase() == PacketSendEvent.Phase.PRE && !e.isCancelled()) {
         Object packet = e.getPacket();
         if (packet instanceof ServerboundSwingPacket || packet instanceof ServerboundSetCarriedItemPacket) {
            this.clickScheduler.recalculate();
         } else if (!(packet instanceof ServerboundMovePlayerPacket) && !(packet instanceof ServerboundClientTickEndPacket)) {
            if (packet instanceof ServerboundInteractPacket interactPacket && this.waitingForPostReleaseTick && this.isRightClickInteraction(interactPacket)) {
               e.cancel();
            }
         } else {
            this.waitingForPostReleaseTick = false;
         }
      }
   }

   public void resetPendingState() {
      this.shieldSuppressedUntilTick = -1;
      this.lastShieldReleaseTick = -1;
      this.waitingForPostReleaseTick = false;
      this.wasShieldUpBeforeAttack = false;
   }

   public boolean shouldCancelShieldUse(InteractionHand hand) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && hand != null) {
         this.syncShieldState();
         return this.waitingForPostReleaseTick
            ? true
            : this.tickCounter <= this.shieldSuppressedUntilTick && mc.player.getItemInHand(hand).getItem() == Items.SHIELD;
      } else {
         return false;
      }
   }

   public boolean shouldCancelUseItemOn() {
      return this.waitingForPostReleaseTick;
   }

   public boolean shouldCancelEntityInteraction() {
      return this.waitingForPostReleaseTick;
   }

   public void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (config != null && mc.player != null && mc.gameMode != null) {
         this.onlyCriticalMode = config.isOnlyCritical() && !config.isSmartCriticals();
         boolean canAttack = this.canAttack(config, 1);
         if (canAttack) {
            this.wasSprintingBeforeAttack = mc.player.isSprinting();
            this.wasShieldUpBeforeAttack = this.isUsingShield(mc.player);
            this.preAttackEntity(config);
         }

         if (RaycastAngle.rayTrace(config) && this.canAttack(config, 0) && this.canAttackWhileSprinting(config)) {
            boolean hvhMode = "Hvh".equals(config.getSprintMode());
            if (hvhMode) {
               mc.player.setSprinting(false);
               if (mc.player.connection != null) {
                  mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
               }
            }

            this.attackEntity(config);
            if (hvhMode && mc.options != null) {
               mc.options.keySprint.setDown(true);
            }
         }
      }
   }

   boolean preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || config == null) {
         return true;
      }

      if (mc.player.isFallFlying()) {
         return true;
      }

      if (config.isShouldUnPressShield() && mc.player.isUsingItem() && mc.player.getUseItem().getItem().equals(Items.SHIELD)) {
         mc.gameMode.releaseUsingItem(mc.player);
         mc.player.stopUsingItem();
      }

      boolean isInCobweb = PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().inflate(-0.001), Blocks.COBWEB);
      if (isInCobweb && config.isOnlyCritical()) {
         this.forceCriticalNextAttack = true;
         return true;
      }

      if (config.isOnlyCritical() && config.isSmartCriticals()) {
         boolean isInWater = mc.player.isInWater() || mc.player.isSwimming();
         if (isInWater) {
            this.wasSprintingBeforeWaterAttack = mc.player.isSprinting();
            this.setSprinting(false);
            this.waterAttackInProgress = true;
            this.waterAttackFinishTimer.reset();
            this.forceCriticalNextAttack = false;
         } else {
            this.forceCriticalNextAttack = false;
            this.resetSprintForAttack(config);
         }
      } else if (config.isOnlyCritical()) {
         boolean isInWater = mc.player.isInWater() || mc.player.isSwimming();
         if (isInWater) {
            this.wasSprintingBeforeWaterAttack = mc.player.isSprinting();
            this.setSprinting(false);
            this.waterAttackInProgress = true;
            this.waterAttackFinishTimer.reset();
            this.forceCriticalNextAttack = true;
            this.sprintForceTimer.reset();
         } else {
            this.resetSprintForCriticalAttack(config);
         }
      } else if (config.isSmartCriticals()) {
         boolean isInWater = mc.player.isInWater() || mc.player.isSwimming();
         if (isInWater) {
            this.wasSprintingBeforeWaterAttack = mc.player.isSprinting();
            this.setSprinting(false);
            this.waterAttackInProgress = true;
            this.waterAttackFinishTimer.reset();
            this.forceCriticalNextAttack = false;
         } else {
            this.forceCriticalNextAttack = false;
            this.resetSprintForAttack(config);
         }
      } else {
         this.forceCriticalNextAttack = false;
         this.resetSprintForAttack(config);
      }

      return true;
   }

   private boolean isRandomAttackDelayMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config != null && config.getAimMode() != null) {
         String aimMode = config.getAimMode().getValue();
         return "SpookyDuel".equals(aimMode) || "SPAngle".equals(aimMode);
      } else {
         return false;
      }
   }

   private boolean shouldDelayAttackAfterShieldRelease(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config == null || !config.isShouldUnPressShield()) {
         return false;
      } else if (this.waitingForPostReleaseTick) {
         return true;
      } else {
         return this.lastShieldReleaseTick == this.tickCounter ? true : this.releaseShieldForAttack();
      }
   }

   private boolean isSprinting() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == null ? false : ServerSprintTracker.isServerSprinting() && !mc.player.isFallFlying() && !mc.player.isInWater();
   }

   private void setSprinting(boolean sprinting) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         mc.player.setSprinting(sprinting);
         if (mc.options != null) {
            mc.options.keySprint.setDown(sprinting);
         }
      }
   }

   private void checkSprintResetConditions() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         boolean isInWater = mc.player.isInWater() || mc.player.isSwimming();
         boolean isInLava = mc.player.isInLava();
         boolean isClimbing = mc.player.onClimbable();
         boolean isInCobweb = PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().inflate(-0.001), Blocks.COBWEB);
         if (!isInWater && this.wasInWater && this.waterAttackInProgress) {
            this.waterAttackInProgress = false;
         }

         if (isInLava != this.wasInLava) {
            this.triggerSprintReset();
         }

         if (isClimbing != this.wasClimbing) {
            this.triggerSprintReset();
         }

         if (isInCobweb && !this.wasInCobweb) {
            this.criticalInCobweb = true;
            this.forceCriticalNextAttack = true;
         } else if (!isInCobweb && this.wasInCobweb) {
            this.criticalInCobweb = false;
            this.forceCriticalNextAttack = false;
         }

         this.wasInWater = isInWater;
         this.wasInLava = isInLava;
         this.wasClimbing = isClimbing;
         this.wasInCobweb = isInCobweb;
      }
   }

   private void triggerSprintReset() {
      this.sprintResetPending = true;
      this.sprintResetTimer.reset();
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         this.setSprinting(false);
         if (!this.isNoReset()) {
            SprintFeature.tickStop = 2;
         }
      }
   }

   private void performSprintReset() {
      if (Minecraft.getInstance().player != null) {
         this.setSprinting(false);
         if (!this.isNoReset()) {
            SprintFeature.tickStop = 2;
         }

         this.sprintResetPending = false;
      }
   }

   private boolean isNoReset() {
      SprintFeature sprintFeature = SprintFeature.getEnabled();
      return sprintFeature != null && sprintFeature.noReset.getValue();
   }

   private boolean canAttackWhileSprinting(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (!this.isSprinting()) {
         return true;
      }

      String sprintMode = config.getSprintMode();
      return "Hvh".equals(sprintMode) || "None".equals(sprintMode) ? true : mc.player.isFallFlying() || this.hasSprintGateRestrictions();
   }

   private boolean hasSprintGateRestrictions() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player.hasEffect(MobEffects.BLINDNESS)
         || mc.player.hasEffect(MobEffects.LEVITATION)
         || PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().inflate(-0.001), Blocks.COBWEB)
         || mc.player.isInLava()
         || mc.player.onClimbable()
         || mc.player.getAbilities().flying;
   }

   private boolean shouldResetSprintForLegit(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      }

      if (config.isOnlyCritical()) {
         return true;
      }

      if (this.isSmartCritJumpRequired() && !mc.player.onGround()) {
         return true;
      }

      if (!mc.player.getAbilities().flying && !mc.player.hasEffect(MobEffects.LEVITATION) && !mc.player.isFallFlying()) {
         boolean isMoving = mc.player.zza != 0.0F || mc.player.xxa != 0.0F;
         if (!isMoving) {
            return true;
         }

         if (!mc.player.onGround()) {
            return false;
         }

         double speed = Math.sqrt(
            mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z
         );
         return speed <= 0.15;
      } else {
         return true;
      }
   }

   private void resetSprintForCriticalAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         boolean isInWater = mc.player.isInWater() || mc.player.isSwimming();
         boolean isInCobweb = PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().inflate(-0.001), Blocks.COBWEB);
         if (isInWater) {
            this.wasSprintingBeforeWaterAttack = mc.player.isSprinting();
            this.setSprinting(false);
            this.waterAttackInProgress = true;
            this.waterAttackFinishTimer.reset();
            this.forceCriticalNextAttack = true;
            this.sprintForceTimer.reset();
         } else if (isInCobweb) {
            this.forceCriticalNextAttack = true;
         } else {
            if (mc.player.onGround()) {
               if (mc.player.isSprinting() && !this.sprintResetPerformedThisTick && this.groundSprintResetCooldown.finished(0.0)) {
                  this.wasSprintingBeforeAttack = true;
                  this.setSprinting(false);
                  SprintFeature.tickStop = 3;
                  this.sprintResetPerformedThisTick = true;
                  this.groundSprintResetCooldown.reset();
                  this.waitingForFallDistance = true;
                  this.requiredFallDistance = 0.0F;
                  this.forceCriticalNextAttack = false;
                  this.sprintForceTimer.reset();
                  this.fallDistanceTimer.reset();
               }
            } else if (mc.player.isSprinting()) {
               this.wasSprintingBeforeAttack = true;
               this.setSprinting(false);
               SprintFeature.tickStop = 3;
               float currentFallDistance = (float)mc.player.fallDistance;
               if (currentFallDistance >= 0.0F) {
                  this.forceCriticalNextAttack = true;
               } else {
                  this.waitingForFallDistance = true;
                  this.requiredFallDistance = 0.0F;
                  this.forceCriticalNextAttack = false;
               }

               this.sprintForceTimer.reset();
               this.fallDistanceTimer.reset();
            }
         }
      }
   }

   private void resetSprintForAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         boolean isInWater = mc.player.isInWater() || mc.player.isSwimming();
         boolean isInCobweb = PlayerInteractionHelper.isBoxInBlock(mc.player.getBoundingBox().inflate(-0.001), Blocks.COBWEB);
         if (isInWater) {
            this.wasSprintingBeforeWaterAttack = mc.player.isSprinting();
            this.setSprinting(false);
            this.waterAttackInProgress = true;
            this.waterAttackFinishTimer.reset();
         } else if (!isInCobweb) {
            String sprintMode = config.getSprintMode();
            if ("Legit".equals(sprintMode)) {
               if (mc.player.isSprinting() && this.shouldResetSprintForLegit(config)) {
                  this.wasSprintingBeforeAttack = true;
                  this.setSprinting(false);
                  SprintFeature.tickStop = 2;
               }
            } else if ("Normal".equals(sprintMode) && mc.player.isSprinting()) {
               this.wasSprintingBeforeAttack = true;
               this.setSprinting(false);
               SprintFeature.tickStop = 2;
            }
         }
      }
   }

   public void syncShieldState() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options == null || !mc.options.keyUse.isDown()) {
         this.shieldSuppressedUntilTick = -1;
      }
   }

   private boolean releaseShieldForAttack() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.gameMode != null) {
         if (mc.player.isUsingItem() && !mc.player.getUseItem().isEmpty() && mc.player.getUseItem().getItem() == Items.SHIELD) {
            mc.gameMode.releaseUsingItem(mc.player);
            mc.player.stopUsingItem();
            this.lastShieldReleaseTick = this.tickCounter;
            this.shieldSuppressedUntilTick = this.tickCounter;
            this.waitingForPostReleaseTick = true;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void attackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.gameMode != null && config.getTarget() != null) {
         mc.gameMode.attack(mc.player, config.getTarget());
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.attackTimer.reset();
         this.forceCriticalNextAttack = false;
         this.waitingForFallDistance = false;
         this.requiredFallDistance = 0.0F;
         this.sprintResetPerformedThisTick = false;
         this.count++;
         this.reRaiseShieldIfHeld(config);
         if (this.isRandomAttackDelayMode(config)) {
            this.nextAttackDelayMs = (long)MathUtils.getRandom(400.0F, 450.0F);
         }

         if (config != null && config.getAimMode() != null && "FunTime".equals(config.getAimMode().getValue())) {
            AutoRegressionContext context = AutoRegressionContext.getInstance();
            context.updateLastAttackTime();
            context.hitContentQueue();
         }

         if (config != null && config.getAimMode() != null && "Universal".equals(config.getAimMode().getValue()) && Math.random() < 0.005) {
            UniversalRotation.onAttack();
         }
      }
   }

   private void reRaiseShieldIfHeld(StrikerConstructor.AttackPerpetratorConfigurable config) {
      Minecraft mc = Minecraft.getInstance();
      if (config != null && config.isShouldUnPressShield() && this.wasShieldUpBeforeAttack) {
         if (mc.player != null && mc.gameMode != null) {
            if (mc.options != null && mc.options.keyUse.isDown()) {
               if (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == Items.SHIELD) {
                  this.shieldSuppressedUntilTick = -1;
                  this.waitingForPostReleaseTick = false;
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
               }
            }
         }
      }
   }

   private boolean isUsingShield(Player player) {
      return player.isUsingItem() && player.getUseItem().getItem() == Items.SHIELD;
   }

   private boolean isRightClickInteraction(ServerboundInteractPacket packet) {
      return true;
   }

   public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
      for (int i = 0; i <= ticks; i++) {
         if (this.canCrit(config, i)) {
            return true;
         }
      }

      return false;
   }

   public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.options != null && config != null) {
         if (mc.player.isUsingItem() && mc.player.getUseItem().getItem() != Items.SHIELD && config.isEatAndAttack()) {
            return false;
         } else if (!this.clickScheduler.isCooldownComplete(config.isTpsSync(), ticks)) {
            return false;
         } else if (this.isAirStuckActive()) {
            return true;
         } else if (config.isOnlyCritical() && !this.hasCriticalAttackStrength(ticks)) {
            return false;
         } else {
            PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
            if (simulated == null) {
               return false;
            } else if (!config.isOnlyCritical()) {
               return true;
            } else if (this.isAirAttackMode(simulated)) {
               return true;
            } else if (this.hasMovementRestrictions(simulated)) {
               return true;
            } else {
               boolean tightSpaceCritical = this.isInsideTightSpace(simulated);
               boolean smartCritJumpRequired = this.isSmartCritJumpRequired();
               boolean smartCritJumpIntent = smartCritJumpRequired && this.isSmartCritJumpIntent();
               boolean debuffFallback = this.hasDebuffThatBreaksCrit(simulated);
               if (this.shouldHoldCriticalOnStartup(simulated, smartCritJumpRequired, smartCritJumpIntent)) {
                  return false;
               } else if (smartCritJumpRequired && !smartCritJumpIntent && !debuffFallback) {
                  return simulated.onGround || this.isPlayerInCriticalState(simulated, ticks);
               } else if (debuffFallback) {
                  return true;
               } else {
                  return tightSpaceCritical ? this.isTightSpaceCriticalWindow(simulated) : this.isPlayerInCriticalState(simulated, ticks);
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean hasMovementRestrictions(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(MobEffects.BLINDNESS)
         || simulated.hasStatusEffect(MobEffects.LEVITATION)
         || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.inflate(-0.001), Blocks.COBWEB)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || simulated.player.getAbilities().flying;
   }

   private boolean isInsideTightSpace(PlayerSimulation simulated) {
      return !PlayerInteractionHelper.canChangeIntoPose(Pose.STANDING, simulated.pos)
         || simulated.horizontalCollision
         || this.hasHorizontalBoxPressure(simulated)
         || this.countBlockingSides(simulated) >= 2;
   }

   private boolean isTightSpaceCriticalWindow(PlayerSimulation simulated) {
      if (this.isPvpTradeCriticalWindow(simulated)) {
         return true;
      }

      float requiredFallDistance = this.resolveCriticalFallDistance(simulated, true);
      double requiredVelocity = this.resolveCriticalVelocity(simulated, true);
      if (simulated.horizontalCollision || this.hasHorizontalBoxPressure(simulated)) {
         requiredFallDistance = Math.min(requiredFallDistance, 0.004F);
         requiredVelocity = Math.max(requiredVelocity, -0.01);
      }

      return !simulated.onGround && simulated.fallDistance > requiredFallDistance && simulated.velocity.y < requiredVelocity;
   }

   private boolean hasHorizontalBoxPressure(PlayerSimulation simulated) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && simulated != null) {
         AABB expandedBox = simulated.boundingBox.inflate(0.22, 0.0, 0.22).deflate(1.0E-7);
         return !mc.level.noCollision(simulated.player, expandedBox);
      } else {
         return false;
      }
   }

   private int countBlockingSides(PlayerSimulation simulated) {
      int blockingSides = 0;
      if (this.isLateralSideBlocked(simulated, 0.72, 0.0)) {
         blockingSides++;
      }

      if (this.isLateralSideBlocked(simulated, -0.72, 0.0)) {
         blockingSides++;
      }

      if (this.isLateralSideBlocked(simulated, 0.0, 0.72)) {
         blockingSides++;
      }

      if (this.isLateralSideBlocked(simulated, 0.0, -0.72)) {
         blockingSides++;
      }

      return blockingSides;
   }

   private boolean isLateralSideBlocked(PlayerSimulation simulated, double offsetX, double offsetZ) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && simulated != null) {
         double footY = simulated.boundingBox.minY + 0.1;
         double bodyY = Math.min(simulated.boundingBox.maxY - 0.1, simulated.boundingBox.minY + 0.95);
         return this.isSolidCollisionBlock(BlockPos.containing(simulated.pos.x + offsetX, footY, simulated.pos.z + offsetZ))
            || this.isSolidCollisionBlock(BlockPos.containing(simulated.pos.x + offsetX, bodyY, simulated.pos.z + offsetZ));
      } else {
         return false;
      }
   }

   private boolean isSolidCollisionBlock(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return false;
      }

      BlockState state = mc.level.getBlockState(pos);
      return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty();
   }

   private boolean isAirStuckActive() {
      return FeatureManager.INSTANCE.getEnabled(AirStuckFeature.class) != null;
   }

   private boolean hasCriticalAttackStrength(float ticks) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.getAttackStrengthScale(ticks) > 0.88F;
   }

   private boolean hasCriticalRestrictions(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(MobEffects.BLINDNESS)
         || simulated.hasStatusEffect(MobEffects.LEVITATION)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || simulated.isSwimming
         || simulated.isFallFlying
         || simulated.player.getAbilities().flying;
   }

   private boolean hasDebuffThatBreaksCrit(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(MobEffects.BLINDNESS) || simulated.hasStatusEffect(MobEffects.LEVITATION);
   }

   private boolean isSmartCritJumpRequired() {
      AuraFeature aura = AuraFeature.getInstance();
      return aura != null && aura.isEnabled() && aura.getOnlyCriticals().getValue() && aura.getSmartCriticals().getValue();
   }

   private boolean isSmartCritJumpIntent() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return false;
      } else if (mc.options != null && mc.options.keyJump.isDown()) {
         return true;
      } else if (mc.player.isJumping()) {
         return true;
      } else {
         return ((LivingEntityAccessor)mc.player).getNoJumpDelay() > 0 && !mc.player.onGround()
            ? true
            : !mc.player.onGround() && mc.player.getDeltaMovement().y > 0.08;
      }
   }

   private boolean isAirAttackMode(PlayerSimulation simulated) {
      Minecraft mc = Minecraft.getInstance();
      return simulated.isFallFlying || simulated.player.getAbilities().flying || mc.player != null && mc.player.isFallFlying();
   }

   private boolean shouldHoldCriticalOnStartup(PlayerSimulation simulated, boolean smartCritJumpRequired, boolean smartCritJumpIntent) {
      AuraFeature aura = AuraFeature.getInstance();
      if (aura == null || !aura.isEnabled() || !aura.getOnlyCriticals().getValue()) {
         return false;
      } else if (smartCritJumpRequired && !smartCritJumpIntent) {
         return false;
      } else {
         long activatedAt = aura.getActivationTimeMs();
         if (activatedAt <= 0L || System.currentTimeMillis() - activatedAt > 450L) {
            return false;
         } else if (simulated.onGround) {
            return false;
         } else {
            return simulated.fallDistance > 0.0F ? false : simulated.velocity.y > -0.03;
         }
      }
   }

   private boolean isPlayerInCriticalState(PlayerSimulation simulated, int ticks) {
      if (simulated.onGround || this.hasCriticalRestrictions(simulated)) {
         return false;
      }

      if (this.isPvpTradeCriticalWindow(simulated)) {
         return true;
      }

      boolean fallingByDistance = simulated.fallDistance > this.resolveCriticalFallDistance(simulated, false);
      boolean fallingByVelocity = simulated.velocity.y < this.resolveCriticalVelocity(simulated, false);
      boolean leavingGroundSoon = ticks > 0 ? !PlayerSimulation.simulateLocalPlayer(ticks).onGround : !simulated.onGround;
      return fallingByDistance && fallingByVelocity && leavingGroundSoon;
   }

   private boolean isPlayerInCriticalState(PlayerSimulation simulated) {
      if (simulated.onGround || this.hasCriticalRestrictions(simulated)) {
         return false;
      }

      if (this.isPvpTradeCriticalWindow(simulated)) {
         return true;
      }

      boolean fallingByDistance = simulated.fallDistance > this.resolveCriticalFallDistance(simulated, false);
      boolean fallingByVelocity = simulated.velocity.y < this.resolveCriticalVelocity(simulated, false);
      return !simulated.onGround && fallingByDistance && fallingByVelocity;
   }

   private boolean isPvpTradeCriticalWindow(PlayerSimulation simulated) {
      return this.isPvpPressureState(simulated) && !simulated.onGround && simulated.fallDistance > 0.0F && simulated.velocity.y < -0.01;
   }

   private boolean isPvpPressureState(PlayerSimulation simulated) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.hurtTime > 0 || simulated.hasStatusEffect(MobEffects.SLOWNESS);
   }

   private float resolveCriticalFallDistance(PlayerSimulation simulated, boolean tightSpace) {
      float required = tightSpace ? 0.01F : 0.03F;
      if (this.isPvpPressureState(simulated)) {
         required = Math.min(required, tightSpace ? 0.008F : 0.012F);
      }

      if (simulated.horizontalCollision) {
         required = Math.min(required, 0.012F);
      }

      return required;
   }

   private double resolveCriticalVelocity(PlayerSimulation simulated, boolean tightSpace) {
      double required = tightSpace ? -0.02 : -0.03;
      if (this.isPvpPressureState(simulated)) {
         required = Math.max(required, tightSpace ? -0.012 : -0.018);
      }

      if (simulated.horizontalCollision) {
         required = Math.max(required, -0.015);
      }

      return required;
   }

   public StopWatch getAttackTimer() {
      return this.attackTimer;
   }

   public int getCount() {
      return this.count;
   }
}

