package org.xrose.feature.impl.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.EventPhase;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.game.RotationUpdateEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.event.events.render.Render3DEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.movement.AirStuckFeature;
import org.xrose.feature.impl.movement.ElytraTargetFeature;
import org.xrose.feature.impl.visual.TargetESPFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.FriendManager;
import org.xrose.utils.combat.PlayerInteractionHelper;
import org.xrose.utils.combat.PlayerSimulation;
import org.xrose.utils.combat.PredictUtils;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.attack.StrikeManager;
import org.xrose.utils.combat.aura.attack.StrikerConstructor;
import org.xrose.utils.combat.aura.context.AutoRegressionContext;
import org.xrose.utils.combat.aura.impl.LinearConstructor;
import org.xrose.utils.combat.aura.impl.RotateConstructor;
import org.xrose.utils.combat.aura.rotations.FTNewAngle;
import org.xrose.utils.combat.aura.rotations.FunTimeSmoothMode;
import org.xrose.utils.combat.aura.rotations.FunTimeSnapSmoothMode;
import org.xrose.utils.combat.aura.rotations.FunTimeTestMode;
import org.xrose.utils.combat.aura.rotations.LegitCxMode;
import org.xrose.utils.combat.aura.rotations.RWAngle;
import org.xrose.utils.combat.aura.rotations.SmoothAngle;
import org.xrose.utils.combat.aura.rotations.SnapAngle;
import org.xrose.utils.combat.aura.rotations.SpookyAnkaSmoothMode;
import org.xrose.utils.combat.aura.rotations.USpookyTimeRotations;
import org.xrose.utils.combat.aura.rotations.UniversalRotation;
import org.xrose.utils.combat.aura.target.MultiPoint;
import org.xrose.utils.combat.aura.target.RwWallBypassHelper;
import org.xrose.utils.combat.aura.target.TargetFinder;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.combat.aura.util.Tuple;
import org.xrose.utils.render.world.WorldMeshRenderer;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class AuraFeature extends Feature {
   private static AuraFeature instance;
   public static LivingEntity target;
   private final TargetFinder targetSelector = new TargetFinder();
   private final MultiPoint pointFinder = new MultiPoint();
   private final StrikerConstructor attackPerpetrator = new StrikerConstructor();
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "ReallyWorld", "ReallyWorld", "FunTimeSnap", "Universal", "Legit CX"));
   public final NumberSetting attackRange = this.register(new NumberSetting("Range", 3.0, 2.5, 6.0, 0.01, " blocks"));
   public final NumberSetting lookRange = this.register(new NumberSetting("Look Range", 1.5, 0.0, 10.0, 0.1, " blocks"));
   public final MultiSelectSetting targetType = this.register(
      new MultiSelectSetting("Targets", Set.of("Players"), "Players", "Friends", "Mobs", "Animals", "Invisible", "Armor Stands")
   );
   public final ModeSetting targetPriority = this.register(new ModeSetting("Priority", "Distance", "Distance", "Health", "Armor", "FOV", "Combined"));
   public final MultiSelectSetting options = this.register(
      new MultiSelectSetting("Options", Set.of("Pause While Using"), "Pause While Using", "Ignore Walls", "Release Shield", "Sync TPS")
   );
   public final ModeSetting moveFix = this.register(new ModeSetting("Move Fix", "Focused", "Focused", "Free", "Chase", "Target"));
   public final ModeSetting sprintMode = this.register(new ModeSetting("Sprint", "Normal", "Hvh", "Normal", "Legit", "None"));
   public final BooleanSetting onlyCriticals = this.register(new BooleanSetting("Only Crits", true));
   public final BooleanSetting smartCriticals = this.register(new BooleanSetting("Smart Crits", false).visibleWhen(() -> this.onlyCriticals.getValue()));
   public final BooleanSetting wallBypass = this.register(new BooleanSetting("Wall Bypass", false).visibleWhen(() -> this.options.isSelected("Ignore Walls")));
   public final BooleanSetting rwWallBypass = this.register(new BooleanSetting("RW Wall Bypass", false).visibleWhen(() -> this.wallBypass.getValue()));
   public final ModeSetting elytraSlowdownMode = this.register(
      new ModeSetting("Замедление", "По радиусу", "По радиусу", "Перед ударом").visibleWhen(ElytraTargetFeature::isElytraSlowdownActive)
   );
   public final NumberSetting preHitTicks = this.register(
      new NumberSetting("Тики до удара", 3.0, 1.0, 10.0, 1.0, " ticks")
         .visibleWhen(() -> ElytraTargetFeature.isElytraSlowdownActive() && this.elytraSlowdownMode.is("Перед ударом"))
   );
   public final BooleanSetting hitAfterOvertake = this.register(new BooleanSetting("Бить токо после перегона", true));
   public final BooleanSetting elytraTurnaround = this.register(new BooleanSetting("Разворот на элитрах", true));
   public final BooleanSetting showPredictPoint = this.register(new BooleanSetting("Показать предикт точку", true));
   public boolean isTurnaroundActive = false;
   public float preddict = 4.0F;
   public static boolean isSlowdownActive = false;
   private LivingEntity lastTarget;
   private boolean legitBackStop;
   private long activationTimeMs;
   private float tps = 20.0F;
   private float adjustTicks;
   private long timestamp;
   private int reducedHitboxAttackCounter;
   private Angle funTimeSnapLastAngle;
   private final FunTimeSnapSmoothMode funTimeSnapSmoothMode = new FunTimeSnapSmoothMode();
   private Vec3 correctionAnchor;
   private LivingEntity correctionAnchorTarget;

   public AuraFeature() {
      super("Aura", "Automatically attacks selected entitiess", FeatureCategory.COMBAT, -1);
      this.smartCriticals.visibleWhen(this.onlyCriticals::getValue);
      this.wallBypass.visibleWhen(() -> this.options.isSelected("Ignore Walls"));
      instance = this;
   }

   public static AuraFeature getInstance() {
      if (instance == null) {
         instance = FeatureManager.INSTANCE.getFeature(AuraFeature.class);
      }

      return instance;
   }

   public static AuraFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(AuraFeature.class);
   }

   public static float activeTps() {
      return instance == null ? 20.0F : instance.tps;
   }

   @Override
   protected void onEnable() {
      this.activationTimeMs = System.currentTimeMillis();
      this.timestamp = System.nanoTime();
      this.tps = 20.0F;
      this.adjustTicks = 0.0F;
      this.reducedHitboxAttackCounter = 0;
      AutoRegressionContext context = AutoRegressionContext.getInstance();
      context.setCdMinecraft(83L);
      context.hitContentClear();
   }

   @Override
   protected void onDisable() {
      this.activationTimeMs = 0L;
      this.timestamp = 0L;
      this.tps = 20.0F;
      this.adjustTicks = 0.0F;
      this.reducedHitboxAttackCounter = 0;
      this.targetSelector.releaseTarget();
      target = null;
      this.lastTarget = null;
      this.correctionAnchor = null;
      this.correctionAnchorTarget = null;
      this.legitBackStop = false;
      AutoRegressionContext.getInstance().hitContentClear();
      this.finishRotationOnRelease(Minecraft.getInstance());
   }

   @EventTarget(priority = 100)
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client != null && client.player != null && client.level != null) {
         this.attackPerpetrator.tick();
      } else {
         this.targetSelector.releaseTarget();
         target = null;
         AngleConnection.INSTANCE.restoreVanillaLook();
      }
   }

   @EventTarget
   public void onPacket(PacketSendEvent event) {
      this.attackPerpetrator.onPacket(event);
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (this.isSyncWithTpsEnabled() && event.getPacket() instanceof ClientboundSetTimePacket) {
         long currentTime = System.nanoTime();
         if (this.timestamp == 0L) {
            this.timestamp = currentTime;
         } else {
            long delay = currentTime - this.timestamp;
            if (delay <= 0L) {
               this.timestamp = currentTime;
            } else {
               float boundedTPS = Mth.clamp(20.0F * (1.0E9F / (float)delay), 0.0F, 20.0F);
               this.tps = (float)limitDecimals(boundedTPS, 2);
               this.adjustTicks = boundedTPS - 20.0F;
               this.timestamp = currentTime;
            }
         }
      }
   }

   @EventTarget
   public void onInput(PlayerInputEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (this.isEnabled() && client.player != null && client.level != null && event.getKeyPresses() != null) {
         if (target != null && target.isAlive()) {
            if (ElytraTargetFeature.isElytraSlowdownActive() && isSlowdownActive && client.player.isFallFlying()) {
               event.setDirectionalLow(false, false, false, false);
            } else {
               this.applyTargetOrChaseCorrection(event, client);
            }
         }
      }
   }

   @EventTarget
   public void onRotationUpdate(RotationUpdateEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (this.isEnabled() && client.player != null && client.level != null) {
         if (event.getPhase() == EventPhase.PRE) {
            this.restoreLegitBackStop();
            target = this.updateTarget();
            if (target != null) {
               if (client.player.isFallFlying() && ElytraTargetFeature.getEnabled() == null) {
                  this.lastTarget = null;
                  this.rotateBackToPlayer(client);
                  return;
               }

               this.updateElytraState();
               if (this.mode.is("ReallyWorld") && this.rwWallBypass.getValue()) {
                  RwWallBypassHelper.tryBreakRwWallBlockPacket();
               }

               this.rotateToTarget(this.getConfig());
               this.lastTarget = target;
            } else {
               this.lastTarget = null;
               this.rotateBackToPlayer(client);
            }
         } else {
            if (event.getPhase() == EventPhase.POST && target != null) {
               boolean anyElytra = client.player.isFallFlying() || target.isFallFlying();
               if (anyElytra && (ElytraTargetFeature.getEnabled() == null || !ElytraTargetFeature.getInstance().canAttack(target))) {
                  return;
               }

               this.updateElytraSlowdown();
               this.performAuraAttack(this.getConfig());
               this.updateSprint(this.getConfig());
            }
         }
      }
   }

   private void updateElytraState() {
      this.isTurnaroundActive = false;
      if (target != null && target.isFallFlying()) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null) {
            Vec3 predict = PredictUtils.predict(target, ElytraTargetFeature.getPredictValueFloat());
            double distToPredict = client.player.getEyePosition().distanceTo(predict);
            this.preddict = !this.hitAfterOvertake.getValue() && !ElytraTargetFeature.isHitAfterOvertake() ? 4.0F : 2.7F;
            if (distToPredict <= this.preddict && this.elytraTurnaround.getValue()) {
               this.isTurnaroundActive = true;
            }
         }
      }
   }

   private void updateElytraSlowdown() {
      Minecraft client = Minecraft.getInstance();
      isSlowdownActive = false;
      if (client.player != null && target != null) {
         if (ElytraTargetFeature.isElytraSlowdownActive() && client.player.isFallFlying()) {
            Vec3 predict = PredictUtils.predict(target, ElytraTargetFeature.getPredictValueFloat());
            double distToPredict = client.player.getEyePosition().distanceTo(predict);
            if (this.elytraSlowdownMode.is("Перед ударом")) {
               isSlowdownActive = this.attackPerpetrator.getAttackHandler().canAttack(this.getConfig(), this.preHitTicks.getValue().intValue());
            } else {
               isSlowdownActive = distToPredict < ElytraTargetFeature.getSlowdownRadius()
                  && this.attackPerpetrator.getAttackHandler().canAttack(this.getConfig(), 2);
            }

            if (isSlowdownActive && this.canStopSprinting()) {
               client.player.setSprinting(false);
            }
         }
      }
   }

   private boolean canStopSprinting() {
      Minecraft mc = Minecraft.getInstance();
      if (target != null && mc.player != null) {
         StrikeManager handler = this.attackPerpetrator.getAttackHandler();
         if (!handler.canAttack(this.getConfig(), 1) && !handler.canAttack(this.getConfig(), 0)) {
            return false;
         }

         PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(1);
         return simulated != null && simulated.fallDistance != 0.0F;
      } else {
         return false;
      }
   }

   @EventTarget
   public void onRender3D(Render3DEvent event) {
      if (this.isEnabled() && this.showPredictPoint.getValue() && ElytraTargetFeature.isShowPredictPoint()) {
         Minecraft client = event.getClient();
         if (target != null && target.isFallFlying() && client.player != null && client.levelRenderer != null) {
            Vec3 predictPos = PredictUtils.predict(target, ElytraTargetFeature.getPredictValueFloat());
            int color = predictColor();
            List<WorldMeshRenderer.Line> lines = new ArrayList<>(12);
            addPredictCube(lines, predictPos, color, 0.35F);
            WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, List.of(), List.of()));
         }
      }
   }

   private static void addPredictCube(List<WorldMeshRenderer.Line> lines, Vec3 center, int color, float s) {
      Vec3 minMinMin = new Vec3(center.x - s, center.y - s, center.z - s);
      Vec3 minMinMax = new Vec3(center.x - s, center.y - s, center.z + s);
      Vec3 minMaxMin = new Vec3(center.x - s, center.y + s, center.z - s);
      Vec3 minMaxMax = new Vec3(center.x - s, center.y + s, center.z + s);
      Vec3 maxMinMin = new Vec3(center.x + s, center.y - s, center.z - s);
      Vec3 maxMinMax = new Vec3(center.x + s, center.y - s, center.z + s);
      Vec3 maxMaxMin = new Vec3(center.x + s, center.y + s, center.z - s);
      Vec3 maxMaxMax = new Vec3(center.x + s, center.y + s, center.z + s);
      lines.add(new WorldMeshRenderer.Line(minMinMin, minMinMax, color));
      lines.add(new WorldMeshRenderer.Line(minMinMin, minMaxMin, color));
      lines.add(new WorldMeshRenderer.Line(minMinMin, maxMinMin, color));
      lines.add(new WorldMeshRenderer.Line(minMinMax, minMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(minMinMax, maxMinMax, color));
      lines.add(new WorldMeshRenderer.Line(minMaxMin, minMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(minMaxMin, maxMaxMin, color));
      lines.add(new WorldMeshRenderer.Line(maxMinMin, maxMinMax, color));
      lines.add(new WorldMeshRenderer.Line(maxMinMin, maxMaxMin, color));
      lines.add(new WorldMeshRenderer.Line(maxMinMax, maxMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(minMaxMax, maxMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(maxMaxMin, maxMaxMax, color));
   }

   private static int predictColor() {
      TargetESPFeature esp = TargetESPFeature.getEnabled();
      return esp != null ? esp.getMarkerColor() : -16711800;
   }

   private void restoreLegitBackStop() {
      if (this.legitBackStop) {
         this.legitBackStop = false;
         Minecraft mc = Minecraft.getInstance();
         if (mc.options != null && mc.player != null) {
            mc.options.keyUp.setDown(PlayerInteractionHelper.isKey(mc.options.keyUp));
         }
      }
   }

   private void updateSprint(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (this.hasStopSprint()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.options != null) {
            StrikeManager handler = this.attackPerpetrator.getAttackHandler();
            if (handler.canAttack(config, 1) || handler.canAttack(config, 0)) {
               boolean sprint = mc.options.keySprint.isDown();
               boolean forward = mc.options.keyUp.isDown();
               if (this.sprintMode.is("Legit")) {
                  sprint = false;
                  if (mc.player.isSprinting()) {
                     forward = false;
                     this.legitBackStop = true;
                  }
               }

               if (this.sprintMode.is("Normal")) {
                  if (mc.player.isSprinting()) {
                     mc.player.setSprinting(false);
                  }

                  sprint = false;
               }

               mc.options.keySprint.setDown(sprint);
               mc.options.keyUp.setDown(forward);
            }
         }
      }
   }

   private boolean hasStopSprint() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == null ? false : !this.sprintMode.is("None") && !this.hasMovementRestrictions();
   }

   private boolean hasMovementRestrictions() {
      Player player = Minecraft.getInstance().player;
      return player == null
         ? false
         : player.hasEffect(MobEffects.BLINDNESS)
            || player.hasEffect(MobEffects.LEVITATION)
            || PlayerInteractionHelper.isBoxInBlock(player.getBoundingBox().inflate(-0.001), Blocks.COBWEB)
            || player.isInWater()
            || player.isSwimming()
            || player.isInLava()
            || player.onClimbable()
            || player.getAbilities().flying;
   }

   public float finalDistance() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.player.isFallFlying() && ElytraTargetFeature.getEnabled() != null) {
         return ElytraTargetFeature.getFindRangeFloat();
      }

      Float airStuckRange = airStuckAttackRange();
      return airStuckRange != null ? airStuckRange : this.attackRange.getFloat() + this.lookRange.getFloat();
   }

   public float attackDistance() {
      Float airStuckRange = airStuckAttackRange();
      return airStuckRange != null ? airStuckRange : this.attackRange.getFloat();
   }

   private static Float airStuckAttackRange() {
      AirStuckFeature airStuck = FeatureManager.INSTANCE.getEnabled(AirStuckFeature.class);
      return airStuck != null ? airStuck.getAuraRange() : null;
   }

   public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
      Minecraft client = Minecraft.getInstance();
      if (target != null && client.player != null) {
         boolean ignoreWalls = this.options.isSelected("Ignore Walls");
         Tuple<Vec3, AABB> point = this.pointFinder
            .computeVector(target, this.attackDistance(), AngleConnection.INSTANCE.getRotation(), this.getSmoothMode().randomValue(), ignoreWalls);
         Vec3 computedPoint = point.getA();
         AABB box = this.adjustAttackBox(point.getB());
         if (this.isElytraPredictAimActive()) {
            Vec3 predictedPoint = PredictUtils.predict(target, ElytraTargetFeature.getPredictValueFloat());
            computedPoint = predictedPoint;
            box = this.adjustAttackBox(target.getBoundingBox().move(predictedPoint.subtract(target.position())));
         }

         Angle angle = MathAngle.fromVec3d(computedPoint.subtract(client.player.getEyePosition()));
         boolean tpsSync = this.options.isSelected("Sync TPS");
         return new StrikerConstructor.AttackPerpetratorConfigurable(
            target,
            angle,
            this.attackDistance(),
            this.options.getValue(),
            this.mode,
            box,
            this.onlyCriticals.getValue(),
            this.smartCriticals.getValue(),
            tpsSync,
            false,
            this.sprintMode.getValue()
         );
      } else {
         AABB fallbackBox = client.player != null ? client.player.getBoundingBox() : new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
         return new StrikerConstructor.AttackPerpetratorConfigurable(
            client.player, MathAngle.cameraAngle(), this.attackDistance(), this.options.getValue(), this.mode, fallbackBox, this.onlyCriticals.getValue()
         );
      }
   }

   public boolean isElytraPredictAimActive() {
      return target != null && target.isFallFlying() && ElytraTargetFeature.isPredictateActive() && !this.isTurnaroundActive;
   }

   public AngleConfig getRotationConfig() {
      return new AngleConfig(this.getSmoothMode(), true, this.moveFix.is("Free"));
   }

   public RotateConstructor getSmoothMode() {
      return switch ((String)this.mode.getValue()) {
         case "Smooth" -> SmoothAngle.INSTANCE;
         case "SpookyAnka" -> new SpookyAnkaSmoothMode();
         case "SPAngle" -> new SpookyAnkaSmoothMode();
         case "Snap" -> new SnapAngle();
         case "ReallyWorld" -> new RWAngle();
         case "FunTime" -> new FunTimeSmoothMode();
         case "FunTimeTest" -> FunTimeTestMode.INSTANCE;
         case "FunTimeSnap" -> this.funTimeSnapSmoothMode;
         case "Universal" -> new UniversalRotation();
         case "FT-New" -> FTNewAngle.INSTANCE;
         case "SpookyTime" -> new USpookyTimeRotations();
         case "Legit CX" -> LegitCxMode.INSTANCE;
         default -> new LinearConstructor();
      };
   }

   private void applyTargetOrChaseCorrection(PlayerInputEvent event, Minecraft client) {
      LivingEntity currentTarget = target;
      if (currentTarget != null && currentTarget.isAlive()) {
         boolean inWater = client.player.isInWater() || client.player.isUnderWater();
         if (!AngleConnection.INSTANCE.isReturning()) {
            boolean focusedFix = this.moveFix.is("Focused");
            boolean targetFix = this.moveFix.is("Target");
            boolean chaseFix = this.moveFix.is("Chase");
            boolean freeFix = this.moveFix.is("Free");
            boolean canAttack = false;
            StrikeManager attackHandler = this.attackPerpetrator.getAttackHandler();
            if (attackHandler != null) {
               canAttack = attackHandler.canAttack(this.getConfig(), 1) && client.player.distanceTo(currentTarget) <= this.attackDistance() && !inWater;
            }

            if (focusedFix) {
               if (canAttack) {
                  event.setDirectionalLow(false, false, false, false);
               }
            } else if (canAttack) {
               event.setDirectionalLow(false, false, false, false);
            } else if (!freeFix) {
               boolean w = client.options.keyUp.isDown();
               boolean s = client.options.keyDown.isDown();
               boolean a = client.options.keyLeft.isDown();
               boolean d = client.options.keyRight.isDown();
               if (!inWater) {
                  if (!targetFix || !w && !s && !a && !d) {
                     if (chaseFix) {
                        if (w || s || a || d) {
                           Vec3 playerPos = client.player.position();
                           Vec3 center = this.elytraMoveAnchor(currentTarget);
                           float targetYaw = currentTarget.getYRot();
                           double rad = Math.toRadians(targetYaw);
                           Vec3 forwardDir = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad)).normalize();
                           Vec3 rightDir = new Vec3(-forwardDir.z, 0.0, forwardDir.x).normalize();
                           Vec3 leftDir = rightDir.scale(-1.0);
                           double offset = currentTarget.getBbWidth() / 2.0 + 0.1;
                           Vec3 offsetVec = Vec3.ZERO;
                           if (w) {
                              offsetVec = offsetVec.add(forwardDir);
                           }

                           if (s) {
                              offsetVec = offsetVec.add(forwardDir.scale(-1.0));
                           }

                           if (a) {
                              offsetVec = offsetVec.add(leftDir);
                           }

                           if (d) {
                              offsetVec = offsetVec.add(rightDir);
                           }

                           Vec3 moveTargetVec = center;
                           if (offsetVec.lengthSqr() > 0.0) {
                              moveTargetVec = center.add(offsetVec.normalize().scale(offset));
                           }

                           this.moveToward(event, playerPos, moveTargetVec, AngleConnection.INSTANCE.getMoveRotation().getYaw());
                        }
                     }
                  } else {
                     this.moveToward(event, client.player.position(), this.elytraMoveAnchor(currentTarget), AngleConnection.INSTANCE.getMoveRotation().getYaw());
                  }
               }
            }
         }
      }
   }

   private Vec3 elytraMoveAnchor(LivingEntity currentTarget) {
      Vec3 desired = this.isElytraPredictAimActive()
         ? PredictUtils.predict(currentTarget, ElytraTargetFeature.getPredictValueFloat())
         : currentTarget.getBoundingBox().getCenter();
      if (this.correctionAnchor != null && this.correctionAnchorTarget == currentTarget && !(this.correctionAnchor.distanceTo(desired) > 8.0)) {
         this.correctionAnchor = this.correctionAnchor.add(desired.subtract(this.correctionAnchor).scale(0.4));
      } else {
         this.correctionAnchor = desired;
      }

      this.correctionAnchorTarget = currentTarget;
      return this.correctionAnchor;
   }

   private void moveToward(PlayerInputEvent event, Vec3 playerPos, Vec3 targetPos, float yaw) {
      Vec3 targetFlat = new Vec3(targetPos.x, playerPos.y, targetPos.z);
      Vec3 dir = targetFlat.subtract(playerPos);
      if (dir.lengthSqr() < 1.0E-7) {
         event.setDirectionalLow(false, false, false, false);
      } else {
         float moveAngle = (float)Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0F;
         float angleDiff = Mth.wrapDegrees(moveAngle - yaw);
         boolean forward = false;
         boolean back = false;
         boolean left = false;
         boolean right = false;
         if (angleDiff >= -22.5F && angleDiff < 22.5F) {
            forward = true;
         } else if (angleDiff >= 22.5F && angleDiff < 67.5F) {
            forward = true;
            right = true;
         } else if (angleDiff >= 67.5F && angleDiff < 112.5F) {
            right = true;
         } else if (angleDiff >= 112.5F && angleDiff < 157.5F) {
            back = true;
            right = true;
         } else if (angleDiff >= -67.5F && angleDiff < -22.5F) {
            forward = true;
            left = true;
         } else if (angleDiff >= -112.5F && angleDiff < -67.5F) {
            left = true;
         } else if (angleDiff >= -157.5F && angleDiff < -112.5F) {
            back = true;
            left = true;
         } else {
            back = true;
         }

         event.setDirectionalLow(forward, back, left, right);
      }
   }

   public boolean shouldCancelInteractItem(InteractionHand hand) {
      if (this.shouldCancelUseInteractions()) {
         return true;
      }

      StrikerConstructor.AttackPerpetratorConfigurable config = this.getConfig();
      return this.attackPerpetrator.getAttackHandler().shouldCancelShieldUse(hand);
   }

   public boolean shouldSuppressAirUsePacket(InteractionHand hand) {
      return this.shouldCancelInteractItem(hand);
   }

   public boolean shouldCancelInteractBlock() {
      return this.shouldCancelUseInteractions() ? true : this.attackPerpetrator.getAttackHandler().shouldCancelUseItemOn();
   }

   public boolean shouldSuppressBlockUsePacket() {
      return this.shouldCancelInteractBlock();
   }

   public boolean shouldCancelEntityInteraction() {
      return this.shouldCancelUseInteractions() ? true : this.attackPerpetrator.getAttackHandler().shouldCancelEntityInteraction();
   }

   public boolean shouldSuppressEntityUsePacket() {
      return this.shouldCancelEntityInteraction();
   }

   public boolean shouldBlockUseInteractions() {
      return false;
   }

   public boolean shouldPauseForUse() {
      return false;
   }

   public boolean shouldCancelUseInteractions() {
      return false;
   }

   public boolean isSyncWithTpsEnabled() {
      return this.options.isSelected("Sync TPS");
   }

   public StrikerConstructor getAttackPerpetrator() {
      return this.attackPerpetrator;
   }

   public ModeSetting getMode() {
      return this.mode;
   }

   public NumberSetting getRange() {
      return this.attackRange;
   }

   public NumberSetting getLookRange() {
      return this.lookRange;
   }

   public MultiSelectSetting getTargets() {
      return this.targetType;
   }

   public ModeSetting getTargetPriority() {
      return this.targetPriority;
   }

   public MultiSelectSetting getOptions() {
      return this.options;
   }

   public ModeSetting getMoveFix() {
      return this.moveFix;
   }

   public BooleanSetting getOnlyCrits() {
      return this.onlyCriticals;
   }

   public BooleanSetting getOnlyCriticals() {
      return this.onlyCriticals;
   }

   public BooleanSetting getSmartCrits() {
      return this.smartCriticals;
   }

   public BooleanSetting getSmartCriticals() {
      return this.smartCriticals;
   }

   public long getActivationTimeMs() {
      return this.activationTimeMs;
   }

   public float getTps() {
      return this.tps;
   }

   public float getAdjustTicks() {
      return this.adjustTicks;
   }

   public LivingEntity getTarget() {
      return this.isEnabled() ? target : null;
   }

   public LivingEntity getCurrentTarget() {
      return this.getTarget();
   }

   public boolean shouldAutoJump(LocalPlayer player) {
      return this.isEnabled() && target != null && this.onlyCriticals.getValue();
   }

   private LivingEntity updateTarget() {
      Minecraft client = Minecraft.getInstance();
      if (client.level == null) {
         this.targetSelector.releaseTarget();
         return null;
      } else {
         Predicate<LivingEntity> filter = this.createEntityFilter();
         boolean ignoreWalls = this.options.isSelected("Ignore Walls");
         this.targetSelector.searchTargets(client.level.entitiesForRendering(), this.finalDistance(), 360.0F, ignoreWalls, filter);
         this.targetSelector.validateTarget(filter);
         return this.targetSelector.getCurrentTarget();
      }
   }

   private Predicate<LivingEntity> createEntityFilter() {
      Set<String> types = new HashSet<>(this.targetType.getValue());
      return entity -> {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || entity == mc.player || !entity.isAlive()) {
            return false;
         }

         if (entity.isInvisible() && !types.contains("Invisible")) {
            return false;
         }

         if (entity instanceof Player player) {
            if (AntiBotFeature.shouldIgnore(player)) {
               return false;
            } else {
               return FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) ? types.contains("Friends") : types.contains("Players");
            }
         } else if (entity instanceof Animal) {
            return types.contains("Animals");
         } else if (entity instanceof Mob) {
            return types.contains("Mobs");
         } else {
            return entity instanceof ArmorStand ? types.contains("Armor Stands") : false;
         }
      };
   }

   private void rotateBackToPlayer(Minecraft client) {
      if (client != null && client.player != null) {
         AngleConnection controller = AngleConnection.INSTANCE;
         if (this.mode.is("FunTimeSnap")) {
            Angle current = controller.getCurrentAngle();
            Angle playerAngle = new Angle(client.player.getYRot(), client.player.getXRot());
            controller.rotateTo(playerAngle, 1, this.getRotationConfig(), TaskPriority.HIGH_IMPORTANCE_1, this);
         } else if (!this.mode.is("SpookyTime") && !this.mode.is("Smooth") && !this.mode.is("Universal")) {
            Angle playerAngle = new Angle(client.player.getYRot(), client.player.getXRot());
            controller.rotateTo(playerAngle, 1, this.getRotationConfig(), TaskPriority.HIGH_IMPORTANCE_1, this);
         } else {
            Angle current = controller.getCurrentAngle();
            client.player.setYRot(current.getYaw());
            client.player.setXRot(current.getPitch());
            controller.reset();
         }
      }
   }

   private void finishRotationOnRelease(Minecraft client) {
      AngleConnection controller = AngleConnection.INSTANCE;
      if (client == null || client.player == null) {
         controller.reset();
      } else if (!this.mode.is("SpookyTime") && !this.mode.is("Smooth") && !this.mode.is("Universal")) {
         AngleConfig returnConfig = new AngleConfig(this.getSmoothMode(), false, false);
         Angle backAngle = new Angle(client.player.getYRot(), client.player.getXRot());
         controller.clear();
         controller.rotateTo(backAngle, 12, returnConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
      } else {
         if (controller.getCurrentAngle() != null) {
            Angle current = controller.getCurrentAngle();
            client.player.setYRot(current.getYaw());
            client.player.setXRot(current.getPitch());
         }

         UniversalRotation.resetRotation();
         controller.reset();
      }
   }

   private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
      AngleConnection controller = AngleConnection.INSTANCE;
      Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
      if (this.mode.is("FunTimeSnap")) {
         this.funTimeSnapLastAngle = controller.getCurrentAngle() != null ? controller.getCurrentAngle() : config.getAngle();
      }

      controller.rotateTo(rotation, target, 1, this.getRotationConfig(), TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   private void performAuraAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      this.attackPerpetrator.performAttack(config);
      this.reducedHitboxAttackCounter++;
   }

   private AABB adjustAttackBox(AABB box) {
      if (box != null && this.shouldUseReducedHitbox()) {
         Vec3 center = box.getCenter();
         double halfX = Math.max(box.getXsize() * 0.7, 0.01);
         double halfZ = Math.max(box.getZsize() * 0.7, 0.01);
         return new AABB(center.x - halfX, box.minY, center.z - halfZ, center.x + halfX, box.maxY, center.z + halfZ);
      } else {
         return box;
      }
   }

   private boolean shouldUseReducedHitbox() {
      return this.reducedHitboxAttackCounter % 4 < 2;
   }

   private static double limitDecimals(double value, int decimalPlaces) {
      return Math.round(value * Math.pow(10.0, decimalPlaces)) / Math.pow(10.0, decimalPlaces);
   }
}

