package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.rotations.SnapAngle;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.network.PacketUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class WallClimbFeature extends Feature {
   private static final long BLOCK_PLACE_DELAY_MS = 75L;
   private static final long BUTTON_PLACE_DELAY_MS = 110L;
   public final NumberSetting speed = this.register(new NumberSetting("Speed", 0.6, 0.1, 1.0, 0.05, ""));
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Motion", "Motion", "Blocks", "GrimWater_Bucket"));
   private final NumberSetting grimDelay = this.register(new NumberSetting("Grim Delay", 0.05, 0.05, 1.0, 0.05, ""));
   private long lastPlaceMs;
   private long lastButtonMs;
   private long lastGrimUseMs;
   private boolean grimCanUse = true;

   public WallClimbFeature() {
      super("WallClimb", "Climb walls like a ladder", FeatureCategory.MOVEMENT, -1);
      this.speed.visibleWhen(() -> this.mode.is("Motion"));
      this.grimDelay.visibleWhen(() -> this.mode.is("GrimWater_Bucket"));
   }

   @Override
   protected void onDisable() {
      Minecraft client = Minecraft.getInstance();
      if (client.options != null) {
         client.options.keyJump.setDown(false);
      }

      this.grimCanUse = true;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.level != null && client.gameMode != null) {
         if (this.mode.is("Motion")) {
            this.handleMotion(player);
         } else if (this.mode.is("GrimWater_Bucket")) {
            this.onGrimWaterBucket(client);
         } else if (this.mode.is("FunTimeButtons")) {
            this.onFunTimeButtons(client);
         } else {
            if (this.mode.is("Blocks")) {
               this.onBlocks(client);
            }
         }
      }
   }

   private void handleMotion(LocalPlayer player) {
      if (player.horizontalCollision) {
         Vec3 movement = player.getDeltaMovement();
         player.setDeltaMovement(movement.x, this.speed.getValue(), movement.z);
      }
   }

   private void onBlocks(Minecraft client) {
      LocalPlayer player = client.player;
      boolean offHand = player.getOffhandItem().getItem() instanceof BlockItem;
      int slot = this.findHotbarBlockSlot(player);
      BlockPos blockPos = this.findPos(client);
      if ((offHand || slot != -1) && !blockPos.equals(BlockPos.ZERO)) {
         ItemStack stack = offHand ? player.getOffhandItem() : player.getInventory().getItem(slot);
         if (stack.getItem() instanceof BlockItem) {
            Vec3 vec = Vec3.atCenterOf(blockPos);
            Direction direction = nearestDirection(vec.x - player.getX(), vec.y - player.getY(), vec.z - player.getZ());
            Angle angle = MathAngle.calculateAngle(vec.subtract(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(0.1F)));
            AngleConnection.INSTANCE
               .rotateTo(
                  new Angle.VecRotation(angle, angle.toVector()), player, 1, new AngleConfig(new SnapAngle(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this
               );
            if (System.currentTimeMillis() - this.lastPlaceMs >= 75L && this.canPlace(client, (BlockItem)stack.getItem())) {
               InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
               int previous = player.getInventory().getSelectedSlot();
               if (!offHand) {
                  selectSlot(player, slot);
               }

               client.gameMode.useItemOn(player, hand, new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
               player.connection.send(new ServerboundSwingPacket(hand));
               if (!offHand) {
                  selectSlot(player, previous);
               }

               this.lastPlaceMs = System.currentTimeMillis();
            }
         }
      }
   }

   private int findHotbarBlockSlot(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getItem(i).getItem() instanceof BlockItem) {
            return i;
         }
      }

      return -1;
   }

   private BlockPos findPos(Minecraft client) {
      BlockPos blockPos = this.getBlockPos(client);
      if (client.level.getBlockState(blockPos).isCollisionShapeFullBlock(client.level, blockPos)) {
         return BlockPos.ZERO;
      }

      for (BlockPos pos : new BlockPos[]{blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north()}) {
         if (client.level.getBlockState(pos).isCollisionShapeFullBlock(client.level, pos)) {
            return pos;
         }
      }

      return BlockPos.ZERO;
   }

   private BlockPos getBlockPos(Minecraft client) {
      Vec3 predicted = client.player.position().add(client.player.getDeltaMovement()).add(0.0, -0.001, 0.0);
      return BlockPos.containing(predicted);
   }

   private boolean canPlace(Minecraft client, BlockItem blockItem) {
      LocalPlayer player = client.player;
      BlockPos blockPos = this.getBlockPos(client);
      if (blockPos.getY() >= BlockPos.containing(player.getX(), player.getY(), player.getZ()).getY()) {
         return false;
      }

      VoxelShape shape = blockItem.getBlock().defaultBlockState().getCollisionShape(client.level, blockPos);
      if (shape.isEmpty()) {
         return false;
      }

      AABB box = shape.bounds().move(blockPos);
      return !box.intersects(player.getBoundingBox());
   }

   private static Direction nearestDirection(double x, double y, double z) {
      double absX = Math.abs(x);
      double absY = Math.abs(y);
      double absZ = Math.abs(z);
      if (absX >= absY && absX >= absZ) {
         return x >= 0.0 ? Direction.EAST : Direction.WEST;
      } else if (absY >= absX && absY >= absZ) {
         return y >= 0.0 ? Direction.UP : Direction.DOWN;
      } else {
         return z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private static void selectSlot(LocalPlayer player, int slot) {
      player.getInventory().setSelectedSlot(slot);
      PacketUtil.sendHeldItemChange(slot);
   }

   private void onGrimWaterBucket(Minecraft client) {
      LocalPlayer player = client.player;
      if (player != null) {
         if (player.isInWater()) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.3, player.getDeltaMovement().z);
         } else {
            int waterSlot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack stack = player.getInventory().getItem(i);
               if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                  waterSlot = i;
                  break;
               }
            }

            boolean hasWaterBucket = waterSlot != -1 || !player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() == Items.WATER_BUCKET;
            if (hasWaterBucket) {
               if (!this.grimCanUse) {
                  if (System.currentTimeMillis() - this.lastGrimUseMs >= (long)(this.grimDelay.getValue() * 1000.0)) {
                     this.grimCanUse = true;
                  }
               } else {
                  if (client.hitResult != null && client.hitResult.getType() == Type.BLOCK) {
                     BlockHitResult blockHit = (BlockHitResult)client.hitResult;
                     Direction side = blockHit.getDirection();
                     if (side != Direction.UP && side != Direction.DOWN) {
                        if (player.onGround()) {
                           player.jumpFromGround();
                           return;
                        }

                        int prev = player.getInventory().getSelectedSlot();
                        if (waterSlot != -1) {
                           selectSlot(player, waterSlot);
                        }

                        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, blockHit);
                        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                        player.connection.send(new ServerboundContainerClosePacket(0));
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.3, player.getDeltaMovement().z);
                        if (waterSlot != -1) {
                           selectSlot(player, prev);
                        }

                        this.grimCanUse = false;
                        this.lastGrimUseMs = System.currentTimeMillis();
                     } else if (side == Direction.DOWN && !player.onGround()) {
                        player.jumpFromGround();
                     }
                  }
               }
            }
         }
      }
   }

   private void onFunTimeButtons(Minecraft client) {
      LocalPlayer player = client.player;
      if (player.horizontalCollision) {
         client.options.keyJump.setDown(true);
         player.setJumping(true);
         player.setOnGround(true);
         player.fallDistance = 0.0;
         int buttonSlot = findItemAnywhere(Items.OAK_BUTTON, player);
         if (buttonSlot == -1) {
            this.setEnabled(false);
            return;
         }

         if (buttonSlot > 8) {
            return;
         }

         if (System.currentTimeMillis() - this.lastButtonMs >= 110L) {
            this.placeButton(client, buttonSlot);
            this.lastButtonMs = System.currentTimeMillis();
         }
      } else {
         client.options.keyJump.setDown(false);
      }
   }

   private void placeButton(Minecraft client, int slot) {
      LocalPlayer player = client.player;
      int prev = player.getInventory().getSelectedSlot();
      selectSlot(player, slot);
      float yaw = this.getWallYaw(client, this.getWallDirection(client));
      float pitch = 90.0F;
      player.setYRot(yaw);
      player.setXRot(pitch);
      BlockHitResult hit = this.getTargetedBlock(client, yaw, pitch, 4.0);
      if (hit.getType() == Type.BLOCK) {
         player.swing(InteractionHand.MAIN_HAND);
         client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
      }

      selectSlot(player, prev);
      player.fallDistance = 0.0;
   }

   private Direction getWallDirection(Minecraft client) {
      Direction forward = client.player.getDirection();
      BlockPos playerPos = client.player.blockPosition();
      if (!client.level.getBlockState(playerPos.relative(forward)).isAir()) {
         return forward;
      }

      for (Direction direction : Plane.HORIZONTAL) {
         if (!client.level.getBlockState(playerPos.relative(direction)).isAir()) {
            return direction;
         }
      }

      return forward;
   }

   private BlockHitResult getTargetedBlock(Minecraft client, float yaw, float pitch, double distance) {
      Vec3 eye = client.player.getEyePosition();
      Vec3 dir = Vec3.directionFromRotation(pitch, yaw).normalize();
      Vec3 end = eye.add(dir.scale(distance));
      ClipContext context = new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, client.player);
      return client.level.clip(context);
   }

   private float getWallYaw(Minecraft client, Direction direction) {
      return switch (direction) {
         case NORTH -> 180.0F;
         case SOUTH -> 0.0F;
         case WEST -> 90.0F;
         case EAST -> -90.0F;
         default -> client.player.getYRot();
      };
   }

   private static int findItemAnywhere(Item item, LocalPlayer player) {
      for (int i = 0; i < 36; i++) {
         if (player.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }
}

