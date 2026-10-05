package org.xrose.feature.impl.player;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketSendEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.mixin.accessor.AbstractContainerScreenAccessor;
import org.xrose.utils.combat.PlayerInteractionHelper;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ItemScrollerFeature extends Feature implements MinecraftContext {
   private long nextClickTime;
   private boolean streaming;
   public final NumberSetting scrollDelay = this.register(new NumberSetting("Scroll Delay", 50.0, 0.0, 200.0, 1.0, "ms"));

   public ItemScrollerFeature() {
      super("Item Scroller", "Quick move and quick stack items while a container is open.", FeatureCategory.PLAYER, -1);
   }

   public static ItemScrollerFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(ItemScrollerFeature.class);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (mc.player != null && mc.gameMode != null && this.throttleReady()) {
         if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            Slot var7 = ((AbstractContainerScreenAccessor)screen).getHoveredSlot();
            ContainerInput actionType = PlayerInteractionHelper.isKey(mc.options.keyDrop)
               ? ContainerInput.THROW
               : (PlayerInteractionHelper.isKey(mc.options.keyAttack) ? ContainerInput.QUICK_MOVE : null);
            if (PlayerInteractionHelper.isKey(mc.options.keyShift)
               && !PlayerInteractionHelper.isKey(mc.options.keySprint)
               && var7 != null
               && var7.hasItem()
               && actionType != null) {
               AbstractContainerMenu menu = mc.player.containerMenu;
               int slotId = menu.slots.indexOf(var7);
               if (slotId != -1) {
                  this.nextClickTime = System.currentTimeMillis() + this.throttleMs();
                  mc.gameMode.handleContainerInput(menu.containerId, slotId, actionType == ContainerInput.THROW ? 1 : 0, actionType, mc.player);
               }
            }
         }
      }
   }

   @EventTarget
   public void onPacketSend(PacketSendEvent event) {
      if (event.getPhase() == PacketSendEvent.Phase.PRE && !this.streaming && event.getPacket() instanceof ServerboundContainerClickPacket packet) {
         if (mc.player != null && mc.gameMode != null) {
            if (PlayerInteractionHelper.isKey(mc.options.keyShift) && PlayerInteractionHelper.isKey(mc.options.keySprint) && this.throttleReady()) {
               AbstractContainerMenu menu = mc.player.containerMenu;
               int clickedSlotId = packet.slotNum();
               if (menu != null && clickedSlotId >= 0 && clickedSlotId < menu.slots.size()) {
                  Slot clickedSlot = (Slot)menu.slots.get(clickedSlotId);
                  if (clickedSlot.hasItem()) {
                     Item item = clickedSlot.getItem().getItem();
                     ContainerInput actionType = packet.containerInput();
                     this.nextClickTime = System.currentTimeMillis() + this.throttleMs();
                     this.streaming = true;

                     try {
                        for (int i = 0; i < menu.slots.size(); i++) {
                           Slot candidate = (Slot)menu.slots.get(i);
                           if (candidate.hasItem() && candidate.container.equals(clickedSlot.container) && candidate.getItem().getItem().equals(item)) {
                              mc.gameMode.handleContainerInput(menu.containerId, i, 1, actionType, mc.player);
                           }
                        }
                     } finally {
                        this.streaming = false;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean throttleReady() {
      return System.currentTimeMillis() >= this.nextClickTime;
   }

   private long throttleMs() {
      return Math.max(0L, this.scrollDelay.getValue().longValue());
   }
}

