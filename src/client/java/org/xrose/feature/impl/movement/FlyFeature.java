package org.xrose.feature.impl.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.util.TaskPriority;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class FlyFeature extends Feature {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Vanilla", "Vanilla", "Dragon", "Громоотводы", "Grim", "Elytra 1.17"));
   private final ModeSetting castleMode = this.register(new ModeSetting("Castle Mode", "Safe", "Safe", "Risk"));
   public final NumberSetting speed = this.register(new NumberSetting("Speed", 0.05, 0.01, 0.25, 0.01, ""));
   public final BooleanSetting vertical = this.register(new BooleanSetting("Vertical Control", true));
   private final NumberSetting horizontal = this.register(new NumberSetting("Horizontal", 1.0, 0.1, 5.0, 0.1, ""));
   private final NumberSetting verticalSpeed = this.register(new NumberSetting("Vertical", 0.6, 0.1, 3.0, 0.1, ""));
   private final NumberSetting castleDelay = this.register(new NumberSetting("Delay", 500.0, 50.0, 1000.0, 50.0, ""));
   private final NumberSetting elytraSpeedY = this.register(new NumberSetting("Speed Y", 1.0, 0.0, 10.0, 0.1, ""));
   private boolean wasFlying;
   private boolean hadMayFly;
   private float originalFlySpeed = 0.05F;
   private boolean vanillaModified;
   private long funTimeTimerMs;
   private boolean forceOnGround;
   private boolean sendingRodPacket;
   private final List<Packet<?>> grimPackets = new ArrayList<>();
   private boolean grimBlinking;
   private BlockPos grimPlacedBlock;
   private boolean resending;
   private final Queue<FlyFeature.CachedPacket> castleQueue = new ConcurrentLinkedQueue<>();
   private int castleHitCooldown;
   private BlockPos castleTargetBlock;
   private Direction castleTargetFace;

   public FlyFeature() {
      super("Fly", "Client-side flight", FeatureCategory.MOVEMENT, -1);
      this.speed.visibleWhen(() -> this.mode.is("Vanilla"));
      this.vertical.visibleWhen(() -> this.mode.is("Vanilla"));
      this.horizontal.visibleWhen(() -> this.mode.is("Dragon") || this.mode.is("CastleFly") && this.castleMode.is("Risk"));
      this.verticalSpeed.visibleWhen(() -> this.mode.is("Dragon") || this.mode.is("CastleFly") && this.castleMode.is("Risk"));
      this.castleMode.visibleWhen(this::isCastleFly);
      this.castleDelay.visibleWhen(this::isCastleFly);
      this.elytraSpeedY.visibleWhen(() -> this.mode.is("Elytra 1.17"));
   }

   private boolean isCastleFly() {
      return this.mode.is("CastleFly");
   }

   @Override
   protected void onEnable() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      this.funTimeTimerMs = 0L;
      this.forceOnGround = false;
      this.sendingRodPacket = false;
      this.grimPackets.clear();
      this.grimBlinking = false;
      this.grimPlacedBlock = null;
      this.resending = false;
      this.castleQueue.clear();
      this.castleHitCooldown = 0;
      this.castleTargetBlock = null;
      this.castleTargetFace = null;
      this.wasFlying = false;
      this.hadMayFly = false;
      this.vanillaModified = false;
      if (player != null) {
         if (this.mode.is("Vanilla")) {
            this.wasFlying = player.getAbilities().flying;
            this.hadMayFly = player.getAbilities().mayfly;
            this.originalFlySpeed = player.getAbilities().getFlyingSpeed();
         }

         if (this.isCastleFly()) {
            player.sendSystemMessage(Component.literal("§a[CastleFly] §fДля работы держите ЛЮБОЙ блок в руке"));
         }
      }
   }

   @Override
   protected void onDisable() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (this.mode.is("CastleFly")) {
         this.flushCastleQueue();
         this.castleQueue.clear();
         this.castleTargetBlock = null;
         this.castleTargetFace = null;
      }

      if (player != null) {
         if (this.mode.is("Vanilla") || this.vanillaModified) {
            player.getAbilities().mayfly = this.hadMayFly;
            player.getAbilities().setFlyingSpeed(this.originalFlySpeed);
            if (!this.hadMayFly) {
               player.getAbilities().flying = false;
            }

            player.connection.send(new ServerboundPlayerAbilitiesPacket(player.getAbilities()));
            this.wasFlying = false;
            this.vanillaModified = false;
         }
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.gameMode != null) {
         if (this.isCastleFly()) {
            this.handleCastleFlyTick(client);
         } else if (this.mode.is("Громоотводы")) {
            this.handleFunTime(client);
         } else if (this.mode.is("Grim")) {
            this.handleGrim(client);
         } else if (this.mode.is("Elytra 1.17")) {
            this.handleElytra117(client);
         } else if (!this.mode.is("Dragon")) {
            this.handleVanilla(event, player);
         } else if (player.getAbilities().mayfly && player.getAbilities().flying) {
            this.applyDirectionalFlight(player, this.horizontal.getFloat(), this.verticalSpeed.getFloat());
         }
      }
   }

   private void handleVanilla(GameTickEvent event, LocalPlayer player) {
      player.getAbilities().mayfly = true;
      player.getAbilities().setFlyingSpeed(this.speed.getFloat());
      if (!player.getAbilities().flying && this.vertical.getValue()) {
         if (event.getClient().options.keyJump.isDown()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.5, player.getDeltaMovement().z);
         } else if (event.getClient().options.keyShift.isDown()) {
            player.setDeltaMovement(player.getDeltaMovement().x, -0.5, player.getDeltaMovement().z);
         }
      }

      player.getAbilities().flying = true;
      if (!this.wasFlying) {
         player.connection.send(new ServerboundPlayerAbilitiesPacket(player.getAbilities()));
         this.wasFlying = true;
      }

      this.vanillaModified = true;
   }

   private void applyDirectionalFlight(LocalPlayer player, double horizontalSpeed, double verticalStep) {
      double[] dir = calculateDirection(player, horizontalSpeed);
      double y = 0.0;
      Minecraft client = Minecraft.getInstance();
      if (client.options.keyJump.isDown()) {
         y += verticalStep;
      }

      if (client.options.keyShift.isDown()) {
         y -= verticalStep;
      }

      dir = clampIfIdle(dir);
      player.setDeltaMovement(new Vec3(dir[0], y, dir[1]));
   }

   private static double[] clampIfIdle(double[] dir) {
      Minecraft client = Minecraft.getInstance();
      Options options = client.options;
      boolean moving = options.keyUp.isDown() || options.keyDown.isDown() || options.keyLeft.isDown() || options.keyRight.isDown();
      if (!moving) {
         dir[0] = 0.0;
         dir[1] = 0.0;
      }

      return dir;
   }

   private static double[] calculateDirection(LocalPlayer player, double speed) {
      Options options = Minecraft.getInstance().options;
      float forward = 0.0F;
      float strafe = 0.0F;
      if (options.keyUp.isDown()) {
         forward++;
      }

      if (options.keyDown.isDown()) {
         forward--;
      }

      if (options.keyLeft.isDown()) {
         strafe--;
      }

      if (options.keyRight.isDown()) {
         strafe++;
      }

      if (forward == 0.0F && strafe == 0.0F) {
         return new double[]{0.0, 0.0};
      }

      double len = Math.sqrt(forward * forward + strafe * strafe);
      forward = (float)(forward / len);
      strafe = (float)(strafe / len);
      double rad = Math.toRadians(player.getYRot());
      double x = (-Math.sin(rad) * forward - Math.cos(rad) * strafe) * speed;
      double z = (Math.cos(rad) * forward - Math.sin(rad) * strafe) * speed;
      return new double[]{x, z};
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (!this.resending) {
         Minecraft client = Minecraft.getInstance();
         if (this.isCastleFly()) {
            Packet<?> packet = event.getPacket();
            if (!(packet instanceof ServerboundMovePlayerPacket) && !(packet instanceof ServerboundSwingPacket)) {
               event.cancel();
               this.castleQueue.add(new FlyFeature.CachedPacket(packet, System.currentTimeMillis()));
            }
         } else if (this.mode.is("Громоотводы")) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket && this.forceOnGround && !this.sendingRodPacket) {
               event.cancel();
               this.forceOnGround = false;
               this.sendingRodPacket = true;
               LocalPlayer player = client.player;
               if (player != null) {
                  player.connection
                     .send(new PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), true, player.horizontalCollision));
               }

               this.sendingRodPacket = false;
            }
         } else {
            if (this.mode.is("Grim") && this.grimBlinking) {
               event.cancel();
               this.grimPackets.add(event.getPacket());
            }
         }
      }
   }

   private void handleElytra117(Minecraft client) {
      LocalPlayer player = client.player;
      if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
         Vec3 delta = player.getDeltaMovement();
         if (delta.x == 0.0 && delta.z == 0.0 && delta.y >= 0.0) {
            if (player.onGround()) {
               client.options.keyJump.setDown(true);
            } else if (!player.isFallFlying()) {
               player.connection.send(new ServerboundPlayerCommandPacket(player, Action.START_FALL_FLYING));
            }
         }

         if (player.isFallFlying()) {
            double maxY = this.elytraSpeedY.getValue();
            Angle angle = new Angle(player.getYRot(), -4.0F);
            AngleConnection.INSTANCE.rotateTo(angle, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            double newY = Math.min(delta.y + 0.06, maxY);
            player.setDeltaMovement(0.0, newY, 0.0);
            client.options.keyJump.setDown(false);
         }
      }
   }

   private void handleFunTime(Minecraft client) {
      LocalPlayer player = client.player;
      if (player.horizontalCollision) {
         long now = System.currentTimeMillis();
         if (this.funTimeTimerMs == 0L || now - this.funTimeTimerMs >= 140L) {
            player.setOnGround(true);
            player.verticalCollision = true;
            player.horizontalCollision = true;
            player.fallDistance = 0.0;
            this.forceOnGround = true;
            player.jumpFromGround();
            this.placeLightningRods(client);
            this.funTimeTimerMs = now;
         }
      }
   }

   private void placeLightningRods(Minecraft client) {
      LocalPlayer player = client.player;
      int slot = findLightningRodSlot(player);
      if (slot != -1) {
         int prev = player.getInventory().getSelectedSlot();
         player.getInventory().setSelectedSlot(slot);
         BlockPos playerPos = player.blockPosition();

         for (int i = 1; i <= 2; i++) {
            BlockPos rodPos = playerPos.above(i);
            if (client.level.getBlockState(rodPos).isAir()) {
               client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(rodPos), Direction.UP, rodPos.below(), false));
               player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
         }

         player.getInventory().setSelectedSlot(prev);
      }
   }

   private static int findLightningRodSlot(LocalPlayer player) {
      List<Item> rods = Items.LIGHTNING_ROD.asList();

      for (int i = 0; i < 9; i++) {
         if (isOneOf(player.getInventory().getItem(i), rods)) {
            return i;
         }
      }

      for (int i = 9; i < 36; i++) {
         if (isOneOf(player.getInventory().getItem(i), rods)) {
            return i;
         }
      }

      return -1;
   }

   private static boolean isOneOf(ItemStack stack, List<Item> items) {
      if (stack.isEmpty()) {
         return false;
      }

      for (Item item : items) {
         if (stack.is(item)) {
            return true;
         }
      }

      return false;
   }

   private void handleGrim(Minecraft client) {
      LocalPlayer player = client.player;
      if (!this.grimBlinking) {
         BlockPos below = player.blockPosition().below();
         boolean supported = player.onGround()
            || !client.level.getBlockState(below).isAir() && !client.level.getBlockState(below).getCollisionShape(client.level, below).isEmpty();
         if (supported) {
            this.grimPlacedBlock = null;
         } else if (this.grimPlacedBlock == null || client.level.getBlockState(this.grimPlacedBlock).isAir()) {
            int slot = findGrimBlockSlot(player);
            if (slot != -1) {
               this.grimBlinking = true;
               this.grimPackets.clear();
               int prevSlot = player.getInventory().getSelectedSlot();
               player.getInventory().setSelectedSlot(slot);
               BlockPos placePos = player.blockPosition().below();
               BlockHitResult hitResult = findPlaceResult(client, placePos);
               if (hitResult != null) {
                  client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
                  player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                  this.grimPlacedBlock = hitResult.getBlockPos().relative(hitResult.getDirection());
               } else {
                  client.gameMode
                     .useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(placePos), Direction.UP, placePos.below(), false));
                  player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                  this.grimPlacedBlock = placePos;
               }

               player.getInventory().setSelectedSlot(prevSlot);
               this.resendCollected();
               this.grimPackets.clear();
               this.grimBlinking = false;
            }
         }
      }
   }

   private static BlockHitResult findPlaceResult(Minecraft client, BlockPos target) {
      for (Direction dir : Direction.values()) {
         BlockPos neighbor = target.relative(dir);
         if (!client.level.getBlockState(neighbor).isAir()) {
            return new BlockHitResult(Vec3.atCenterOf(neighbor), dir.getOpposite(), neighbor, false);
         }
      }

      return null;
   }

   private static int findGrimBlockSlot(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getItem(i).getItem() instanceof BlockItem) {
            return i;
         }
      }

      for (int i = 9; i < 36; i++) {
         if (player.getInventory().getItem(i).getItem() instanceof BlockItem) {
            return i;
         }
      }

      return -1;
   }

   private void resendCollected() {
      if (!this.grimPackets.isEmpty()) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            this.resending = true;

            try {
               for (Packet<?> pkt : this.grimPackets) {
                  player.connection.send(pkt);
               }
            } finally {
               this.resending = false;
            }
         }
      }
   }

   private void handleCastleFlyTick(Minecraft client) {
      if (this.castleHitCooldown > 0) {
         this.castleHitCooldown--;
      }

      this.replayCastleQueue();
      if (this.castleMode.is("Safe")) {
         this.handleCastleSafe(client);
      } else {
         this.handleCastleRisk(client);
      }
   }

   private void handleCastleSafe(Minecraft client) {
      LocalPlayer player = client.player;
      if (!player.onGround()) {
         player.setDeltaMovement(Vec3.ZERO);
         player.fallDistance = 0.0;
         this.updateCastleTarget(client);
         if (this.castleTargetBlock != null) {
            this.performCastleBlockHit(client, this.castleTargetBlock, this.castleTargetFace);
         }
      }
   }

   private void handleCastleRisk(Minecraft client) {
      LocalPlayer player = client.player;
      if (!player.onGround()) {
         this.applyDirectionalFlight(player, this.horizontal.getFloat(), this.verticalSpeed.getFloat());
         this.updateCastleTarget(client);
         if (this.castleTargetBlock != null) {
            this.performCastleBlockHit(client, this.castleTargetBlock, this.castleTargetFace);
         }
      }
   }

   private void updateCastleTarget(Minecraft client) {
      if (client.hitResult != null && client.hitResult.getType() == Type.BLOCK) {
         BlockHitResult blockHit = (BlockHitResult)client.hitResult;
         this.castleTargetBlock = blockHit.getBlockPos();
         this.castleTargetFace = blockHit.getDirection();
      } else {
         this.castleTargetBlock = null;
         this.castleTargetFace = null;
      }
   }

   private void performCastleBlockHit(Minecraft client, BlockPos pos, Direction face) {
      if (this.castleHitCooldown <= 0) {
         BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
         client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult);
         client.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         this.castleHitCooldown = 2;
      }
   }

   private void replayCastleQueue() {
      long delay = this.castleDelay.getValue().longValue();
      long now = System.currentTimeMillis();
      List<Packet<?>> due = new ArrayList<>();
      this.castleQueue.removeIf(cached -> {
         if (now - cached.timestamp() >= delay) {
            due.add(cached.packet());
            return true;
         } else {
            return false;
         }
      });
      this.sendDirect(due);
   }

   private void flushCastleQueue() {
      List<Packet<?>> due = new ArrayList<>();

      for (FlyFeature.CachedPacket cached : this.castleQueue) {
         due.add(cached.packet());
      }

      this.sendDirect(due);
   }

   private void sendDirect(List<Packet<?>> packets) {
      if (!packets.isEmpty()) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            this.resending = true;

            try {
               for (Packet<?> packet : packets) {
                  try {
                     player.connection.send(packet);
                  } catch (Exception var9) {
                  }
               }
            } finally {
               this.resending = false;
            }
         }
      }
   }

   private record CachedPacket(Packet<?> packet, long timestamp) {
   }
}

