package org.xrose.mixin.render;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.xrose.utils.render.ItemEntityRenderStateAccess;

@Mixin(ItemEntityRenderState.class)
public abstract class ItemEntityRenderStateMixin implements ItemEntityRenderStateAccess {
   @Unique
   private boolean onGround;

   @Override
   public boolean isOnGround() {
      return this.onGround;
   }

   @Override
   public void setOnGround(boolean onGround) {
   }
}

