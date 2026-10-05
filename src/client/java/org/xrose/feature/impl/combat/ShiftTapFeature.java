package org.xrose.feature.impl.combat;

import net.minecraft.client.Minecraft;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.AttackEvent;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class ShiftTapFeature extends Feature {
   public final NumberSetting duration = this.register(new NumberSetting("Duration", 120.0, 40.0, 400.0, 10.0, " ms"));
   private long tapUntil;
   private boolean forcing;
   private boolean originalShift;

   public ShiftTapFeature() {
      super("ShiftTap", "Taps sneak on every attack instead of W-tap", FeatureCategory.COMBAT, -1);
   }

   @EventTarget
   public void onAttack(AttackEvent event) {
      this.tapUntil = System.currentTimeMillis() + (long)this.duration.getFloat();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null && client.options != null) {
         boolean shouldForce = System.currentTimeMillis() < this.tapUntil && !client.player.onGround();
         if (shouldForce && !this.forcing) {
            this.originalShift = client.options.keyShift.isDown();
         }

         if (shouldForce) {
            client.options.keyShift.setDown(true);
            this.forcing = true;
         } else if (this.forcing) {
            client.options.keyShift.setDown(this.originalShift);
            this.forcing = false;
         }
      }
   }

   @Override
   protected void onDisable() {
      if (this.forcing) {
         Minecraft client = Minecraft.getInstance();
         if (client.options != null) {
            client.options.keyShift.setDown(this.originalShift);
         }

         this.forcing = false;
      }

      this.tapUntil = 0L;
   }
}

