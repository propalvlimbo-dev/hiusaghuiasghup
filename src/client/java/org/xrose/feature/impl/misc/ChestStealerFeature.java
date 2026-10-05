package org.xrose.feature.impl.misc;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.utils.inventory.ContainerLootService;
import org.xrose.utils.inventory.InventoryUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ChestStealerFeature extends Feature {
   public final NumberSetting delay = this.register(new NumberSetting("Delay", 80.0, 0.0, 1000.0, 10.0, "ms"));
   private long lastMoveAt;

   public ChestStealerFeature() {
      super("ChestStealer", "Automatically takes items from containers", FeatureCategory.MISC, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (!PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY) && InventoryUtil.isContainerScreenOpen()) {
         AbstractContainerMenu menu = InventoryUtil.getOpenMenu();
         if (menu != null && menu != event.getClient().player.inventoryMenu) {
            long now = System.currentTimeMillis();
            if (now - this.lastMoveAt >= this.delay.getValue().longValue()) {
               if (ContainerLootService.quickMoveFirst(menu, stack -> !stack.isEmpty())) {
                  this.lastMoveAt = now;
               }
            }
         }
      }
   }
}

