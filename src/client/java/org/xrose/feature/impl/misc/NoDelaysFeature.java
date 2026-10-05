package org.xrose.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.mixin.accessor.LivingEntityAccessor;
import org.xrose.mixin.accessor.MinecraftAccessor;
import org.xrose.mixin.accessor.MultiPlayerGameModeAccessor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NoDelaysFeature extends Feature {
   private static final int DEFAULT_JUMP_DELAY = 10;
   private static final int DEFAULT_RIGHT_CLICK_DELAY = 4;
   private static final int DEFAULT_BLOCK_BREAK_DELAY = 5;
   public final BooleanSetting jump = this.register(new BooleanSetting("Jump", true));
   public final BooleanSetting rightClick = this.register(new BooleanSetting("Right Click", false));
   public final BooleanSetting experienceBottlesOnly = this.register(
      new BooleanSetting("Experience Bottles Only", false).visibleWhen(this.rightClick::getValue)
   );
   public final BooleanSetting blockBreak = this.register(new BooleanSetting("Block Break", false));

   public NoDelaysFeature() {
      super("NoDelays", "Removes selected player action delays", FeatureCategory.MISC, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null) {
         if (this.jump.getValue()) {
            ((LivingEntityAccessor)client.player).setNoJumpDelay(0);
         }

         if (this.rightClick.getValue()
            && (
               !this.experienceBottlesOnly.getValue()
                  || client.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)
                  || client.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE)
            )) {
            ((MinecraftAccessor)client).setRightClickDelay(0);
         }

         if (this.blockBreak.getValue() && client.gameMode != null) {
            ((MultiPlayerGameModeAccessor)client.gameMode).setDestroyDelay(0);
         }
      }
   }

   @Override
   protected void onDisable() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         ((LivingEntityAccessor)client.player).setNoJumpDelay(10);
      }

      ((MinecraftAccessor)client).setRightClickDelay(4);
      if (client.gameMode != null) {
         ((MultiPlayerGameModeAccessor)client.gameMode).setDestroyDelay(5);
      }
   }
}

