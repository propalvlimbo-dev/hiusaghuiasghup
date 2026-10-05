package org.xrose.feature.impl.movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.PlayerInputEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.utils.FriendManager;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.impl.LinearConstructor;
import org.xrose.utils.combat.aura.util.MathUtils;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.inventory.InventorySwap;
import org.xrose.utils.inventory.InventoryUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoDodgeFeature extends Feature {
   private static final String OPT_ARROWS = "Tridents/Arrows";
   private static final String OPT_DEBUFFS = "Debuffs";
   private static final int POTION_COLOR_A = 4737096;
   private static final int POTION_COLOR_B = 3329330;
   private static final long SAFE_ZONE_LIFETIME_MS = 400L;
   private static final long KELP_USE_COOLDOWN_MS = 500L;
   private static final double ARROW_WATER_DRAG = 0.6;
   public final MultiSelectSetting targets = this.register(new MultiSelectSetting("Targets", Set.of("Tridents/Arrows"), "Tridents/Arrows", "Debuffs"));
   private final Map<Integer, AutoDodgeFeature.ArrowState> arrowStates = new HashMap<>();
   private final Set<Integer> ignoredArrows = new HashSet<>();
   private final Map<String, AutoDodgeFeature.PlayerPotionInfo> playerPotionInfos = new HashMap<>();
   private final Map<Integer, Integer> potionEntityColors = new HashMap<>();
   private int dodgeTicks;
   private long safeZoneExpireMs;
   private long lastKelpUseMs;
   private AABB safeZone;
   private Float dodgeYaw;

   public AutoDodgeFeature() {
      super("Auto Dodge", "Auto dodges projectiles and harmful splash potions", FeatureCategory.MOVEMENT, -1);
   }

   public static AutoDodgeFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(AutoDodgeFeature.class);
   }

   @Override
   protected void onDisable() {
      this.arrowStates.clear();
      this.ignoredArrows.clear();
      this.playerPotionInfos.clear();
      this.potionEntityColors.clear();
      this.safeZone = null;
      this.dodgeYaw = null;
      this.dodgeTicks = 0;
      AngleConnection.INSTANCE.startReturning();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft mc = event.getClient();
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) {
         this.clearTransientState();
      } else if (!player.isUsingItem() && !player.hasEffect(MobEffects.SLOWNESS)) {
         this.dodgeYaw = null;
         if (this.targets.isSelected("Debuffs")) {
            this.dodgePotion();
         }

         if (this.targets.isSelected("Tridents/Arrows")) {
            this.scanArrows();
         }

         if (this.safeZone != null && System.currentTimeMillis() - this.safeZoneExpireMs > 400L) {
            this.safeZone = null;
         }

         if (this.targets.isSelected("Debuffs")) {
            this.trackPlayerPotions();
            this.prunePotionColors();
         }
      } else {
         this.dodgeYaw = null;
      }
   }

   @EventTarget
   public void onInput(PlayerInputEvent event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null && event.getKeyPresses() != null) {
         if (this.safeZone != null) {
            double centerX = (this.safeZone.minX + this.safeZone.maxX) / 2.0;
            double centerZ = (this.safeZone.minZ + this.safeZone.maxZ) / 2.0;
            this.moveToward(event, player.position(), new Vec3(centerX, player.getY(), centerZ), player.getYRot());
         } else {
            if (this.dodgeYaw != null) {
               double rad = Math.toRadians(this.dodgeYaw.floatValue());
               Vec3 away = player.position().add(new Vec3(-Math.sin(rad), 0.0, Math.cos(rad)).scale(6.0));
               this.moveToward(event, player.position(), away, player.getYRot());
            }
         }
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPacket() instanceof ClientboundAddEntityPacket packet && packet.getType() == EntityTypes.SPLASH_POTION) {
         Vec3 pos = new Vec3(packet.getX(), packet.getY(), packet.getZ());
         Integer color = this.matchThrower(pos, packet.getMovement());
         if (color != null) {
            this.potionEntityColors.put(packet.getId(), color);
         }
      }
   }

   private void dodgePotion() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         int slot = this.findDriedKelpSlot();
         if (slot >= 0 && !player.getCooldowns().isOnCooldown(new ItemStack(Items.DRIED_KELP))) {
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
               if (entity instanceof ThrownSplashPotion potion) {
                  Integer color = this.potionEntityColors.get(entity.getId());
                  if (color != null && (color == 4737096 || color == 3329330)) {
                     String thrower = this.findPlayerByPotionColor(color);
                     if ((thrower == null || !FriendManager.INSTANCE.isFriend(thrower)) && this.willPotionHit(potion)) {
                        this.dodgeAwayFromPotion(potion);
                        break;
                     }
                  }
               }
            }
         } else {
            this.dodgeTicks = 0;
         }
      }
   }

   private void dodgeAwayFromPotion(ThrownSplashPotion potion) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         double dx = potion.getX() - player.getX();
         double dy = potion.getY() - (player.getY() + player.getEyeHeight());
         double dz = potion.getZ() - player.getZ();
         float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
         float pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
         float targetYaw = yaw + MathUtils.getRandom(-3.0F, 3.0F);
         float targetPitch = pitch + MathUtils.getRandom(-3.0F, 3.0F);
         AngleConnection.INSTANCE
            .rotateTo(new Angle(targetYaw, targetPitch), 1, new AngleConfig(new LinearConstructor(), true, true), TaskPriority.HIGH_IMPORTANCE_2, this);
         if (Math.abs(Mth.wrapDegrees(player.getYRot() - targetYaw)) < 8.0F && Math.abs(player.getXRot() - targetPitch) < 8.0F) {
            this.dodgeYaw = Mth.wrapDegrees(targetYaw - 180.0F);
            if (this.dodgeTicks >= 2) {
               this.useDriedKelp();
               this.dodgeTicks = 0;
            }

            this.dodgeTicks++;
         }
      }
   }

   private boolean willPotionHit(ThrownSplashPotion potion) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return false;
      }

      Vec3 pos = potion.position();
      if (pos.distanceTo(player.position()) < 1.5) {
         return false;
      }

      Vec3 motion = potion.getDeltaMovement();
      AABB playerBox = player.getBoundingBox().inflate(2.5);

      for (int i = 0; i < 25; i++) {
         for (int j = 0; j < 4; j++) {
            Vec3 next = pos.add(motion.scale(0.25));
            if (new AABB(pos, next).inflate(0.1).intersects(playerBox)) {
               return true;
            }

            pos = next;
         }

         motion = motion.scale(potion.isInWater() ? 0.8 : 0.99).add(0.0, potion.isNoGravity() ? 0.0 : -0.05, 0.0);
      }

      return false;
   }

   private void trackPlayerPotions() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         this.playerPotionInfos.entrySet().removeIf(entry -> now - entry.getValue().time > 3000L);

         for (Player other : mc.level.players()) {
            if (other != player && !(player.getEyePosition().distanceTo(other.getEyePosition()) > 50.0)) {
               String name = other.getGameProfile().name();
               Vec3 pos = other.position();
               Vec3 look = other.getViewVector(1.0F);
               ItemStack mainHand = other.getMainHandItem();
               ItemStack offHand = other.getOffhandItem();
               if (mainHand.getItem() instanceof SplashPotionItem) {
                  this.playerPotionInfos.put(name, new AutoDodgeFeature.PlayerPotionInfo(potionColor(mainHand), pos, look, now));
               } else if (offHand.getItem() instanceof SplashPotionItem) {
                  this.playerPotionInfos.put(name, new AutoDodgeFeature.PlayerPotionInfo(potionColor(offHand), pos, look, now));
               }
            }
         }
      }
   }

   private void prunePotionColors() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         Set<Integer> present = new HashSet<>();

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ThrownSplashPotion) {
               present.add(entity.getId());
            }
         }

         this.potionEntityColors.keySet().removeIf(id -> !present.contains(id));
      }
   }

   private Integer matchThrower(Vec3 spawnPos, Vec3 velocity) {
      double best = Double.MAX_VALUE;
      Integer bestColor = null;
      long now = System.currentTimeMillis();

      for (Entry<String, AutoDodgeFeature.PlayerPotionInfo> entry : this.playerPotionInfos.entrySet()) {
         AutoDodgeFeature.PlayerPotionInfo info = entry.getValue();
         long age = now - info.time;
         if (age <= 1500L) {
            double dist = spawnPos.distanceTo(info.pos);
            if (!(dist > 25.0)) {
               if (info.pos.y - spawnPos.y > 2.0) {
                  Vec3 horizontal = new Vec3(spawnPos.x - info.pos.x, 0.0, spawnPos.z - info.pos.z);
                  if (horizontal.length() < 15.0 && age < 1000L) {
                     double score = dist + age / 200.0;
                     if (score < best) {
                        best = score;
                        bestColor = info.color;
                     }
                  }
               } else if (velocity.lengthSqr() > 0.0) {
                  Vec3 velocityNormalized = velocity.normalize();
                  Vec3 lookNormalized = info.look.normalize();
                  if (velocityNormalized.dot(lookNormalized) > 0.1) {
                     double score = dist + age / 200.0;
                     if (score < best) {
                        best = score;
                        bestColor = info.color;
                     }
                  }
               } else {
                  double score = dist + age / 200.0;
                  if (score < best) {
                     best = score;
                     bestColor = info.color;
                  }
               }
            }
         }
      }

      return bestColor;
   }

   private String findPlayerByPotionColor(Integer color) {
      long now = System.currentTimeMillis();

      for (Entry<String, AutoDodgeFeature.PlayerPotionInfo> entry : this.playerPotionInfos.entrySet()) {
         AutoDodgeFeature.PlayerPotionInfo info = entry.getValue();
         if (now - info.time <= 2000L && info.color == color) {
            return entry.getKey();
         }
      }

      return null;
   }

   private void useDriedKelp() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && !player.isUsingItem() && !InventorySwap.isBusy()) {
         long now = System.currentTimeMillis();
         if (now - this.lastKelpUseMs >= 500L) {
            int slot = this.findDriedKelpSlot();
            if (slot >= 0) {
               InventorySwap.useFromSlot(slot, false, false);
               this.lastKelpUseMs = now;
            }
         }
      }
   }

   private int findDriedKelpSlot() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player == null ? -1 : InventoryUtil.findPlayerMenuSlot(player, stack -> stack.is(Items.DRIED_KELP));
   }

   private void scanArrows() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null) {
         List<Vec3> predictedPath = this.predictPlayerPath(5);
         int best = 50;
         AbstractArrow threat = null;

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof AbstractArrow arrow) {
               int id = entity.getId();
               AutoDodgeFeature.ArrowState state = this.arrowStates.computeIfAbsent(id, ignored -> new AutoDodgeFeature.ArrowState(entity.position(), 0));
               if (!this.isStuck(arrow, state)
                  && !this.ignoredArrows.contains(id)
                  && !(arrow instanceof ThrownTrident trident && trident.clientSideReturnTridentTickCount > 0)
                  && !(arrow.getDeltaMovement().lengthSqr() < 1.0E-4)
                  && this.timeToImpact(arrow) <= 15) {
                  int impact = this.impactTick(arrow, predictedPath);
                  if (impact >= 0 && impact < best) {
                     best = impact;
                     threat = arrow;
                  }
               }
            }
         }

         if (threat == null) {
            this.safeZone = null;
         } else {
            this.computeSafeZone(threat, best);
         }
      }
   }

   private void computeSafeZone(AbstractArrow arrow, int ticks) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         double[] steps = new double[ticks];

         for (int i = 0; i < ticks; i++) {
            double value = i == 0 ? 1.0 : (i == 1 ? 3.0 : 5.0);
            steps[i] = value / 20.0;
         }

         Vec3 playerPos = player.position();
         Vec3 arrowPos = arrow.position();
         Vec3 arrowMotion = arrow.getDeltaMovement();
         double velocity = Math.hypot(arrowMotion.x, arrowMotion.z);
         if (velocity <= 0.0) {
            this.safeZone = null;
         } else {
            Vec3 sideA = new Vec3(arrowMotion.z / velocity, 0.0, -arrowMotion.x / velocity);
            Vec3 sideB = new Vec3(-arrowMotion.z / velocity, 0.0, arrowMotion.x / velocity);
            Vec3 dodgeA = playerPos;
            Vec3 dodgeB = playerPos;
            boolean canA = true;
            boolean canB = true;
            double width = player.getBbWidth();
            double height = player.getBbHeight();
            double minDistA = Double.MAX_VALUE;
            double minDistB = Double.MAX_VALUE;
            Vec3 arrowSimPos = arrowPos;
            Vec3 arrowSimMotion = arrowMotion;

            for (int t = 0; t < ticks; t++) {
               if (canA) {
                  Vec3 next = dodgeA.add(sideA.scale(steps[t]));
                  AABB box = playerBox(next, width, height);
                  if (!mc.level.getBlockCollisions(player, box).iterator().hasNext()) {
                     dodgeA = next;
                     double distance = arrowSimPos.distanceTo(dodgeA);
                     minDistA = Math.min(minDistA, distance);
                     if (distance <= width / 2.0) {
                        canA = false;
                     }
                  } else {
                     canA = false;
                  }
               }

               if (canB) {
                  Vec3 next = dodgeB.add(sideB.scale(steps[t]));
                  AABB box = playerBox(next, width, height);
                  if (!mc.level.getBlockCollisions(player, box).iterator().hasNext()) {
                     dodgeB = next;
                     double distance = arrowSimPos.distanceTo(dodgeB);
                     minDistB = Math.min(minDistB, distance);
                     if (distance <= width / 2.0) {
                        canB = false;
                     }
                  } else {
                     canB = false;
                  }
               }

               if (!canA && !canB) {
                  break;
               }

               arrowSimPos = arrowSimPos.add(arrowSimMotion);
               arrowSimMotion = arrowSimMotion.scale(arrow.isInWater() ? 0.6 : 0.99).add(0.0, arrow.isNoGravity() ? 0.0 : -0.05, 0.0);
            }

            if (!canA && !canB) {
               this.safeZone = null;
            } else {
               boolean goA;
               if (canA && !canB) {
                  goA = true;
               } else if (!canA && canB) {
                  goA = false;
               } else {
                  double difference = minDistA - minDistB;
                  if (Math.abs(difference) > 0.1) {
                     goA = difference > 0.0;
                  } else {
                     goA = dodgeA.distanceTo(playerPos) >= dodgeB.distanceTo(playerPos);
                  }
               }

               Vec3 target = goA ? dodgeA : dodgeB;
               Vec3 offset = target.subtract(playerPos);
               Vec3 destination = playerPos.add(offset.scale(1.5));
               this.safeZone = playerBox(destination, width, height);
               this.safeZoneExpireMs = System.currentTimeMillis();
            }
         }
      }
   }

   private static AABB playerBox(Vec3 center, double width, double height) {
      return new AABB(center.x - width / 2.0, center.y, center.z - width / 2.0, center.x + width / 2.0, center.y + height, center.z + width / 2.0);
   }

   private List<Vec3> predictPlayerPath(int ticks) {
      LocalPlayer player = Minecraft.getInstance().player;
      List<Vec3> points = new ArrayList<>();
      if (player == null) {
         points.add(Vec3.ZERO);
         return points;
      }

      Vec3 pos = player.position();
      Vec3 motion = player.getDeltaMovement();
      points.add(pos);

      for (int i = 0; i < ticks; i++) {
         pos = pos.add(motion);
         motion = motion.scale(player.onGround() ? 0.546 : 0.91);
         points.add(pos);
      }

      return points;
   }

   private boolean isStuck(AbstractArrow arrow, AutoDodgeFeature.ArrowState state) {
      Vec3 pos = arrow.position();
      if (pos.equals(state.pos)) {
         if (++state.stuckTicks > 5) {
            this.ignoredArrows.add(arrow.getId());
            return true;
         }
      } else {
         state.pos = pos;
         state.stuckTicks = 0;
      }

      return false;
   }

   private int timeToImpact(AbstractArrow arrow) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return Integer.MAX_VALUE;
      }

      double distance = arrow.position().distanceTo(player.position());
      double speed = arrow.getDeltaMovement().length();
      return speed > 0.0 ? (int)(distance / speed) : Integer.MAX_VALUE;
   }

   private int impactTick(AbstractArrow arrow, List<Vec3> predictedPath) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null) {
         return -1;
      }

      Vec3 pos = arrow.position();
      double width = player.getBbWidth();
      double height = player.getBbHeight();

      for (int tick = 1; tick <= 30; tick++) {
         Vec3 motion = arrow.getDeltaMovement();

         for (int sub = 0; sub < 4; sub++) {
            Vec3 start = pos;
            Vec3 end = pos.add(motion.scale(0.25));
            BlockHitResult blockHit = mc.level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, arrow));
            if (blockHit != null && blockHit.getType() == Type.BLOCK) {
               return -1;
            }

            int limit = Math.min(predictedPath.size(), 6);

            for (int i = 0; i < limit; i++) {
               Vec3 point = predictedPath.get(i);
               double distanceToPoint = start.distanceTo(point);
               BlockHitResult blockToPoint = mc.level.clip(new ClipContext(start, point, Block.COLLIDER, Fluid.NONE, arrow));
               if (blockToPoint == null || blockToPoint.getType() != Type.BLOCK || !(start.distanceTo(blockToPoint.getLocation()) < distanceToPoint)) {
                  AABB playerBox = playerBox(point, width, height);
                  EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(mc.level, arrow, start, end, playerBox, entity -> entity == player);
                  if (entityHit != null) {
                     return tick;
                  }
               }
            }

            pos = end;
         }
      }

      return -1;
   }

   private void clearTransientState() {
      this.safeZone = null;
      this.dodgeYaw = null;
      this.dodgeTicks = 0;
   }

   private static int potionColor(ItemStack stack) {
      return ((PotionContents)stack.getComponents().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)).getColor();
   }

   private void moveToward(PlayerInputEvent event, Vec3 playerPos, Vec3 targetPos, float yaw) {
      Vec3 targetFlat = new Vec3(targetPos.x, playerPos.y, targetPos.z);
      Vec3 direction = targetFlat.subtract(playerPos);
      if (direction.lengthSqr() < 1.0E-7) {
         event.setDirectionalLow(false, false, false, false);
      } else {
         float moveAngle = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
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

   private static final class ArrowState {
      Vec3 pos;
      int stuckTicks;

      ArrowState(Vec3 pos, int stuckTicks) {
         this.pos = pos;
         this.stuckTicks = stuckTicks;
      }
   }

   private static final class PlayerPotionInfo {
      final int color;
      final Vec3 pos;
      final Vec3 look;
      final long time;

      PlayerPotionInfo(int color, Vec3 pos, Vec3 look, long time) {
         this.color = color;
         this.pos = pos;
         this.look = look;
         this.time = time;
      }
   }
}

