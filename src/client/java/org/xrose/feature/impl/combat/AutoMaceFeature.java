package org.xrose.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.FriendManager;
import org.xrose.utils.network.PacketUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoMaceFeature extends Feature {
   private final NumberSetting radius = this.register(new NumberSetting("Радиус", 4.0, 1.0, 8.0, 0.5, ""));
   private final NumberSetting minHeight = this.register(new NumberSetting("Мин. высота", 2.0, 0.5, 8.0, 0.5, ""));
   private final NumberSetting maxDrop = this.register(new NumberSetting("Макс. высота", 8.0, 1.0, 24.0, 1.0, ""));
   private final BooleanSetting restore = this.register(new BooleanSetting("Возвращать предмет", true));
   private int maceSlot = -1;
   private int slotToRestore = -1;

   public AutoMaceFeature() {
      super("AutoMace", "Берёт булаву в руку, когда под вами игрок", FeatureCategory.COMBAT, -1);
   }

   @Override
   protected void onDisable() {
      if (this.slotToRestore != -1) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            selectSlot(player, this.slotToRestore);
         }
      }

      this.resetState();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player == null || client.level == null || client.gameMode == null) {
         this.resetState();
      } else if (!player.isUsingItem()) {
         int selected = player.getInventory().getSelectedSlot();
         if (this.maceSlot != -1 && selected != this.maceSlot) {
            this.resetState();
         }

         if (this.hasPlayerBelow(client, player, this.maceSlot != -1)) {
            if (this.maceSlot != -1) {
               return;
            }

            int found = findMaceSlot(player);
            if (found == -1 || found == selected) {
               return;
            }

            selectSlot(player, found);
            this.slotToRestore = this.restore.getValue() ? selected : -1;
            this.maceSlot = found;
         } else if (this.maceSlot != -1) {
            if (this.slotToRestore != -1) {
               selectSlot(player, this.slotToRestore);
            }

            this.resetState();
         }
      }
   }

   private boolean hasPlayerBelow(Minecraft client, LocalPlayer player, boolean holding) {
      if (player.onGround()) {
         return false;
      }

      if (!holding && player.getDeltaMovement().y >= -0.01) {
         return false;
      }

      double reach = this.radius.getValue();
      double minBelow = holding ? 0.0 : this.minHeight.getValue();
      double maxBelow = Math.max(this.maxDrop.getValue(), this.minHeight.getValue());

      for (AbstractClientPlayer other : client.level.players()) {
         if (other != player
            && other.isAlive()
            && !other.isSpectator()
            && !other.isCreative()
            && !FriendManager.INSTANCE.isFriend(other.getGameProfile().name())) {
            double below = player.getY() - other.getY();
            if (!(below <= 0.0) && !(below < minBelow) && !(below > maxBelow)) {
               double dx = other.getX() - player.getX();
               double dz = other.getZ() - player.getZ();
               if (dx * dx + dz * dz <= reach * reach) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static int findMaceSlot(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         if (player.getInventory().getItem(i).is(Items.MACE)) {
            return i;
         }
      }

      return -1;
   }

   private static void selectSlot(LocalPlayer player, int slot) {
      player.getInventory().setSelectedSlot(slot);
      PacketUtil.sendHeldItemChange(slot);
   }

   private void resetState() {
      this.maceSlot = -1;
      this.slotToRestore = -1;
   }
}

