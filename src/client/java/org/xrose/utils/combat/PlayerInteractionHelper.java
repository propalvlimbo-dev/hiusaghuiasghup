package org.xrose.utils.combat;

import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class PlayerInteractionHelper {
   private PlayerInteractionHelper() {
   }

   public static void sendSequencedPacket(PredictiveAction packetCreator) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.getConnection() != null) {
         mc.getConnection().send(packetCreator.predict(0));
      }
   }

   public static void sendPacketWithOutEvent(Packet<?> packet) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() != null) {
         mc.getConnection().getConnection().send(packet, null);
      }
   }

   public static void jump() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (mc.player.isSprinting()) {
            float yaw = mc.player.getYRot() * (float) (Math.PI / 180.0);
            mc.player.addDeltaMovement(new Vec3(-Mth.sin(yaw) * 0.2F, 0.0, Mth.cos(yaw) * 0.2F));
         }

         mc.player.hurtMarked = true;
      }
   }

   public static List<BlockPos> getCube(BlockPos center, float radius) {
      return getCube(center, radius, radius, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
      return getCube(center, radiusXZ, radiusY, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
      List<BlockPos> positions = new ArrayList<>();
      int centerX = center.getX();
      int centerY = center.getY();
      int centerZ = center.getZ();
      int posY = down ? centerY - (int)radiusY : centerY;

      for (int x = centerX - (int)radiusXZ; x <= centerX + radiusXZ; x++) {
         for (int z = centerZ - (int)radiusXZ; z <= centerZ + radiusXZ; z++) {
            for (int y = posY; y <= centerY + radiusY; y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
      List<BlockPos> positions = new ArrayList<>();

      for (int x = start.getX(); x <= end.getX(); x++) {
         for (int z = start.getZ(); z <= end.getZ(); z++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static Type getKeyType(int key) {
      return key < 8 ? Type.MOUSE : Type.KEYSYM;
   }

   public static boolean canChangeIntoPose(Pose pose, Vec3 pos) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.level != null
         ? mc.level.noCollision(mc.player, mc.player.getDimensions(pose).makeBoundingBox(pos).deflate(1.0E-7))
         : false;
   }

   public static boolean isPlayerInBlock(Block block) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && isBoxInBlock(mc.player.getBoundingBox().inflate(-0.001), block);
   }

   public static boolean isBoxInBlock(AABB box, Block block) {
      return isBox(box, pos -> {
         Minecraft mc = Minecraft.getInstance();
         return mc.level != null && mc.level.getBlockState(pos).getBlock().equals(block);
      });
   }

   public static boolean isBoxInBlocks(AABB box, List<Block> blocks) {
      return isBox(box, pos -> {
         Minecraft mc = Minecraft.getInstance();
         return mc.level != null && blocks.contains(mc.level.getBlockState(pos).getBlock());
      });
   }

   public static boolean isBox(AABB box, Predicate<BlockPos> pos) {
      return BlockPos.betweenClosedStream(box).anyMatch(pos);
   }

   public static boolean isKey(KeyMapping key) {
      return isKey(key.getDefaultKey().getType(), key.getDefaultKey().getValue());
   }

   public static boolean isKey(Type type, int keyCode) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getWindow() == null) {
         return false;
      }

      if (keyCode != -1) {
         switch (type) {
            case KEYSYM:
               return GLFW.glfwGetKey(mc.getWindow().handle(), keyCode) == 1;
            case MOUSE:
               return GLFW.glfwGetMouseButton(mc.getWindow().handle(), keyCode) == 1;
         }
      }

      return false;
   }

   public static boolean isAir(BlockPos blockPos) {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null && isAir(mc.level.getBlockState(blockPos));
   }

   public static boolean isAir(BlockState state) {
      return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
   }

   public static boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public static boolean nullCheck() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player == null || mc.level == null;
   }
}

