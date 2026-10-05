package org.xrose.utils.inventory;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.xrose.context.MinecraftContext;
import org.xrose.utils.inventory.script.Script;

public final class InventoryTask implements MinecraftContext {
   private static final Script SWAP_AND_USE_SCRIPT = new Script();
   private static final Script HOTBAR_USE_SCRIPT = new Script();

   private InventoryTask() {
   }

   public static void updateSwapAndUseScript() {
      SWAP_AND_USE_SCRIPT.update();
      HOTBAR_USE_SCRIPT.update();
   }

   public static boolean isSwapAndUseIdle() {
      return SWAP_AND_USE_SCRIPT.isFinished() && HOTBAR_USE_SCRIPT.isFinished();
   }

   public static void closeScreen(boolean packet) {
      if (mc.player != null) {
         if (packet) {
            if (mc.getConnection() != null) {
               mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            }
         } else {
            mc.player.closeContainer();
         }
      }
   }
}

