package org.xrose.utils.combat.aura;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventManager;
import org.xrose.event.EventTarget;
import org.xrose.event.events.EventPhase;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.game.RotationUpdateEvent;
import org.xrose.event.events.input.PlayerVelocityStrafeEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.utils.combat.aura.back.BackAngle;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.combat.aura.util.TaskProcessor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public class AngleConnection {
   public static final AngleConnection INSTANCE = new AngleConnection();
   private static final RotateConstructor USE_ITEM_RETURN_SMOOTH = new BackAngle(0.95F);
   private final TaskProcessor<AngleConstructor> rotationPlanTaskProcessor = new TaskProcessor<>();
   private AngleConstructor lastRotationPlan;
   private Angle currentAngle;
   private Angle previousAngle;
   private Angle serverAngle = Angle.DEFAULT;
   private Angle fakeAngle;
   private Angle previousFakeAngle;
   private Float fakeBodyYaw;
   private Float previousFakeBodyYaw;
   private boolean returning;
   private int forcePacketRotationTicks;
   private boolean useItemReturnPending;
   private int staleInactiveTicks;
   private int resetPhaseStartTick = -1;
   private boolean rotationRequestedThisTick;
   private boolean queuedRotationTaskThisTick;

   public void setRotation(Angle value) {
      if (value == null) {
         this.previousAngle = this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
      } else {
         this.previousAngle = this.currentAngle;
      }

      this.currentAngle = value;
   }

   public Angle getCurrentAngle() {
      return this.currentAngle;
   }

   public Angle getRotation() {
      return this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
   }

   public Angle getServerAngle() {
      return this.serverAngle;
   }

   public float getPacketYaw() {
      return this.getPacketRotation().getYaw();
   }

   public float getPacketPitch() {
      return Mth.clamp(this.getPacketRotation().getPitch(), -90.0F, 90.0F);
   }

   public boolean shouldApplyPacketRotation() {
      return this.currentAngle != null || this.fakeAngle != null || this.forcePacketRotationTicks > 0 && this.previousAngle != null;
   }

   public void forcePacketRotation(int ticks) {
      if (ticks > 0) {
         this.forcePacketRotationTicks = Math.max(this.forcePacketRotationTicks, ticks);
      }
   }

   public Angle getFakeRotation() {
      if (this.fakeAngle != null) {
         return this.fakeAngle;
      } else {
         return this.currentAngle != null ? this.currentAngle : (this.previousAngle != null ? this.previousAngle : MathAngle.cameraAngle());
      }
   }

   public Angle getFakeAngle() {
      return this.fakeAngle;
   }

   public Angle getPreviousFakeRotation() {
      return this.previousFakeAngle != null ? this.previousFakeAngle : this.getFakeRotation();
   }

   public float getFakeBodyYaw() {
      return this.fakeBodyYaw != null ? this.fakeBodyYaw : this.getFakeRotation().getYaw();
   }

   public float getPreviousFakeBodyYaw() {
      return this.previousFakeBodyYaw != null ? this.previousFakeBodyYaw : this.getFakeBodyYaw();
   }

   public void setFakeRotation(Angle angle) {
      angle = this.normalizeAngle(angle, this.fakeAngle);
      if (angle == null) {
         this.previousFakeAngle = null;
         this.fakeAngle = null;
         this.previousFakeBodyYaw = null;
         this.fakeBodyYaw = null;
      } else {
         this.previousFakeAngle = this.fakeAngle != null ? this.fakeAngle : angle;
         this.fakeAngle = angle;
         float targetYaw = angle.getYaw();
         if (this.fakeBodyYaw == null) {
            this.fakeBodyYaw = targetYaw;
            this.previousFakeBodyYaw = targetYaw;
         } else {
            this.previousFakeBodyYaw = this.fakeBodyYaw;
            float diff = Mth.wrapDegrees(targetYaw - this.fakeBodyYaw);
            this.fakeBodyYaw = this.fakeBodyYaw + Mth.clamp(diff, -8.0F, 8.0F);
         }
      }
   }

   public Angle getPreviousRotation() {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return this.currentAngle != null ? this.currentAngle : Angle.DEFAULT;
      } else {
         return this.currentAngle != null && this.previousAngle != null ? this.previousAngle : new Angle(client.player.yRotO, client.player.xRotO);
      }
   }

   public Angle getMoveRotation() {
      AngleConstructor rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : MathAngle.cameraAngle();
   }

   public boolean isMoveCorrectionActive() {
      AngleConstructor rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection();
   }

   public boolean isFreeCorrectionActive() {
      AngleConstructor rotationPlan = this.getCurrentRotationPlan();
      return this.isMoveCorrectionActive() && rotationPlan != null && rotationPlan.isFreeCorrection();
   }

   public boolean isReturning() {
      return this.returning;
   }

   public boolean isUseItemReturnPending() {
      return this.useItemReturnPending;
   }

   public AngleConstructor getCurrentRotationPlan() {
      AngleConstructor activePlan = this.rotationPlanTaskProcessor.fetchActiveTaskValue();
      return activePlan != null ? activePlan : this.lastRotationPlan;
   }

   public void rotateTo(Angle.VecRotation vecRotation, LivingEntity entity, int reset, AngleConfig configurable, TaskPriority taskPriority, Object provider) {
      this.rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, int reset, AngleConfig configurable, TaskPriority taskPriority, Object provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, AngleConfig configurable, TaskPriority taskPriority, Object provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, 1), taskPriority, provider);
   }

   public void rotateTo(AngleConstructor plan, TaskPriority taskPriority, Object provider) {
      this.returning = false;
      this.useItemReturnPending = false;
      this.rotationRequestedThisTick = true;
      this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<>(1, taskPriority.getPriority(), provider, plan));
   }

   @EventTarget
   public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent event) {
      AngleConstructor rotationPlan = this.getCurrentRotationPlan();
      if (rotationPlan != null && rotationPlan.isMoveCorrection()) {
         event.setVelocity(this.fixVelocity(event.getVelocity(), event.getMovementInput(), event.getSpeed()));
      }
   }

   @EventTarget
   public void onGameTick(GameTickEvent event) {
      this.tick(event.getClient());
   }

   public void tick(Minecraft client) {
      if (client != null && client.player != null && client.level != null) {
         if (this.forcePacketRotationTicks > 0) {
            this.forcePacketRotationTicks--;
         }

         this.rotationRequestedThisTick = false;
         this.queuedRotationTaskThisTick = false;
         EventManager.call(new RotationUpdateEvent(EventPhase.PRE));
         this.advanceRotation(client);
         this.queuedRotationTaskThisTick = this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null;
         if (this.useItemReturnPending && !this.shouldDelayUseAction()) {
            this.useItemReturnPending = false;
         }

         if (!this.hasActiveRotationOwner() && this.hasAnyRotationState()) {
            this.staleInactiveTicks++;
            if (this.staleInactiveTicks >= 6) {
               this.hardResetStaleRotationState();
               return;
            }
         } else {
            this.staleInactiveTicks = 0;
         }

         EventManager.call(new RotationUpdateEvent(EventPhase.POST));
      } else {
         this.reset();
      }
   }

   private void advanceRotation(Minecraft client) {
      AngleConstructor activePlan = this.getCurrentRotationPlan();
      if (activePlan != null) {
         Angle clientAngle = new Angle(client.player.getYRot(), client.player.getXRot());
         if (this.returning && this.currentAngle != null && computeRotationDifference(this.currentAngle, clientAngle) < 1.0) {
            this.restoreVanillaLook();
         } else {
            boolean hasActiveTask = this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null;
            if (!hasActiveTask && this.lastRotationPlan != null) {
               if (this.resetPhaseStartTick < 0) {
                  this.resetPhaseStartTick = this.rotationPlanTaskProcessor.tickCounter();
               }

               if (this.rotationPlanTaskProcessor.tickCounter() - this.resetPhaseStartTick > 10) {
                  this.setRotation(null);
                  this.setFakeRotation(null);
                  this.lastRotationPlan = null;
                  this.rotationPlanTaskProcessor.clear();
                  this.returning = false;
                  this.useItemReturnPending = false;
                  this.resetPhaseStartTick = -1;
                  return;
               }
            } else {
               this.resetPhaseStartTick = -1;
            }

            if (this.lastRotationPlan != null) {
               double differenceFromCurrentToPlayer = computeRotationDifference(this.serverAngle, clientAngle);
               if (activePlan.getTicksUntilReset() <= this.rotationPlanTaskProcessor.tickCounter()
                  && differenceFromCurrentToPlayer < activePlan.getResetThreshold()) {
                  client.player.setYRot(this.getRotation().getYaw());
                  client.player.setXRot(this.getRotation().getPitch());
                  this.setRotation(null);
                  this.setFakeRotation(null);
                  this.lastRotationPlan = null;
                  this.rotationPlanTaskProcessor.clear();
                  this.returning = false;
                  this.useItemReturnPending = false;
                  this.resetPhaseStartTick = -1;
                  return;
               }
            }

            Angle baseAngle = this.currentAngle != null ? this.currentAngle : clientAngle;
            Angle newAngle = activePlan.nextRotation(baseAngle, this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
            this.setRotation(newAngle);
            this.lastRotationPlan = activePlan;
            this.rotationPlanTaskProcessor.tick(1);
         }
      }
   }

   public void applyPlayerRotation(Minecraft client) {
      if (client != null && client.player != null && this.currentAngle != null) {
         client.player.setYRot(this.currentAngle.getYaw());
         client.player.setXRot(this.currentAngle.getPitch());
         client.player.setYHeadRot(this.currentAngle.getYaw());
         client.player.setYBodyRot(this.currentAngle.getYaw());
      }
   }

   public void clear() {
      this.rotationPlanTaskProcessor.clear();
      this.currentAngle = null;
      this.previousAngle = null;
      this.lastRotationPlan = null;
      this.useItemReturnPending = false;
   }

   public void startReturning() {
      this.startReturning(null);
   }

   public void startReturningForUse() {
      this.useItemReturnPending = true;
      this.startReturning(USE_ITEM_RETURN_SMOOTH);
   }

   public boolean requestUseRotationRestore() {
      if (!this.shouldDelayUseAction()) {
         this.useItemReturnPending = false;
         return false;
      }

      if (!this.useItemReturnPending) {
         this.startReturningForUse();
      }

      return true;
   }

   public boolean shouldDelayUseAction() {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return false;
      }

      Angle cameraAngle = MathAngle.cameraAngle();
      return this.returning
         || this.currentAngle != null && computeRotationDifference(this.currentAngle, cameraAngle) > 0.35
         || this.fakeAngle != null && computeRotationDifference(this.fakeAngle, cameraAngle) > 0.35
         || this.forcePacketRotationTicks > 0 && this.previousAngle != null && computeRotationDifference(this.previousAngle, cameraAngle) > 0.35;
   }

   public void reset() {
      this.clear();
      this.fakeAngle = null;
      this.previousFakeAngle = null;
      this.fakeBodyYaw = null;
      this.previousFakeBodyYaw = null;
      this.serverAngle = Angle.DEFAULT;
      this.returning = false;
      this.forcePacketRotationTicks = 0;
      this.staleInactiveTicks = 0;
      this.resetPhaseStartTick = -1;
      this.rotationRequestedThisTick = false;
      this.queuedRotationTaskThisTick = false;
   }

   public void restoreVanillaLook() {
      this.reset();
      this.syncVanillaHeadAndBody();
   }

   public void syncAfterFirstPersonRestore() {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         this.reset();
      } else {
         Angle playerAngle = MathAngle.cameraAngle();
         Angle syncedAngle = this.normalizeAngle(playerAngle, this.currentAngle != null ? this.currentAngle : playerAngle);
         this.currentAngle = syncedAngle;
         this.previousAngle = syncedAngle;
         this.fakeAngle = null;
         this.previousFakeAngle = null;
         this.fakeBodyYaw = null;
         this.previousFakeBodyYaw = null;
         this.returning = false;
         this.forcePacketRotationTicks = 0;
         this.useItemReturnPending = false;
      }
   }

   public void onMovePacket(ServerboundMovePlayerPacket packet) {
      if (packet != null && packet.hasRotation()) {
         this.serverAngle = new Angle(packet.getYRot(1.0F), packet.getXRot(1.0F));
      }
   }

   public void onPlayerPosition(ClientboundPlayerPositionPacket packet) {
      if (packet != null) {
         this.serverAngle = new Angle(packet.change().yRot(), packet.change().xRot());
      }
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket movePacket) {
            this.onMovePacket(movePacket);
         }
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (!event.isCancelled()) {
         if (event.getPacket() instanceof ClientboundPlayerPositionPacket positionPacket) {
            this.onPlayerPosition(positionPacket);
         }
      }
   }

   public static double computeRotationDifference(Angle a, Angle b) {
      return Math.hypot(Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return Mth.wrapDegrees(a - b);
   }

   private Vec3 fixVelocity(Vec3 currentVelocity, Vec3 movementInput, float speed) {
      if (this.currentAngle == null) {
         return currentVelocity;
      }

      double length = movementInput.lengthSqr();
      if (length < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 scaled = (length > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
      float sin = Mth.sin(this.currentAngle.getYaw() * (float) (Math.PI / 180.0));
      float cos = Mth.cos(this.currentAngle.getYaw() * (float) (Math.PI / 180.0));
      return new Vec3(scaled.x * cos - scaled.z * sin, scaled.y, scaled.z * cos + scaled.x * sin);
   }

   private Angle getPacketRotation() {
      if (this.currentAngle != null) {
         return this.currentAngle;
      } else if (this.fakeAngle != null) {
         return this.fakeAngle;
      } else {
         return this.forcePacketRotationTicks > 0 && this.previousAngle != null ? this.previousAngle : MathAngle.cameraAngle();
      }
   }

   private Angle normalizeAngle(Angle angle, Angle reference) {
      if (angle == null) {
         return null;
      }

      float yaw = angle.getYaw();
      if (reference != null) {
         yaw = reference.getYaw() + Mth.wrapDegrees(yaw - reference.getYaw());
      } else {
         yaw = Mth.wrapDegrees(yaw);
      }

      return new Angle(yaw, Mth.clamp(angle.getPitch(), -90.0F, 90.0F));
   }

   private void startReturning(RotateConstructor returnSmooth) {
      AngleConstructor sourcePlan = this.rotationPlanTaskProcessor.fetchActiveTaskValue();
      AngleConstructor returnPlan = sourcePlan != null ? sourcePlan : this.lastRotationPlan;
      if (returnPlan != null && returnSmooth != null) {
         returnPlan = this.cloneRotationPlan(returnPlan, returnSmooth);
      } else if (returnPlan == null && returnSmooth != null && this.currentAngle != null) {
         returnPlan = new AngleConstructor(this.currentAngle, this.currentAngle.toVector(), null, returnSmooth, 1, 1.0F, false, false);
      }

      this.lastRotationPlan = returnPlan;
      this.rotationPlanTaskProcessor.clear();
      this.returning = this.currentAngle != null && this.lastRotationPlan != null;
      if (!this.returning && !this.shouldDelayUseAction()) {
         this.useItemReturnPending = false;
      }
   }

   private AngleConstructor cloneRotationPlan(AngleConstructor sourcePlan, RotateConstructor returnSmooth) {
      return new AngleConstructor(
         sourcePlan.getAngle(),
         sourcePlan.getVec3d(),
         sourcePlan.getEntity(),
         returnSmooth,
         sourcePlan.getTicksUntilReset(),
         sourcePlan.getResetThreshold(),
         sourcePlan.isMoveCorrection(),
         sourcePlan.isFreeCorrection()
      );
   }

   private boolean hasAnyRotationState() {
      return this.currentAngle != null
         || this.fakeAngle != null
         || this.previousAngle != null
         || this.previousFakeAngle != null
         || this.lastRotationPlan != null
         || this.returning
         || this.useItemReturnPending;
   }

   private boolean hasActiveRotationOwner() {
      return this.rotationRequestedThisTick
         || this.queuedRotationTaskThisTick
         || this.returning
         || this.useItemReturnPending
         || this.forcePacketRotationTicks > 0;
   }

   private void hardResetStaleRotationState() {
      this.restoreVanillaLook();
   }

   private void syncVanillaHeadAndBody() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         float yaw = client.player.getYRot();
         client.player.setYHeadRot(yaw);
         client.player.setYBodyRot(yaw);
      }
   }
}

