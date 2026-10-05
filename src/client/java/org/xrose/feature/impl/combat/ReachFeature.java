package org.xrose.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ReachFeature extends Feature {
   private static final Identifier ENTITY_RANGE_ID = Identifier.parse("xrose:reach_entity_range");
   private static final Identifier BLOCK_RANGE_ID = Identifier.parse("xrose:reach_block_range");
   public final NumberSetting entityRange = this.register(new NumberSetting("Entity Range", 3.5, 3.0, 6.0, 0.05, " blocks"));
   public final NumberSetting blockRange = this.register(new NumberSetting("Block Range", 5.0, 4.5, 8.0, 0.05, " blocks"));

   public ReachFeature() {
      super("Reach", "Extends attack and interaction range", FeatureCategory.COMBAT, -1);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null) {
         this.apply(player);
      }
   }

   private void apply(LocalPlayer player) {
      applyRange(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_ID, this.entityRange.getFloat() - 3.0F);
      applyRange(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_ID, this.blockRange.getFloat() - 4.5F);
   }

   private static void applyRange(AttributeInstance attribute, Identifier id, double extra) {
      if (attribute != null) {
         attribute.removeModifier(id);
         if (extra > 0.0) {
            attribute.addTransientModifier(new AttributeModifier(id, extra, Operation.ADD_VALUE));
         }
      }
   }

   @Override
   protected void onDisable() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         clearRange(player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE), ENTITY_RANGE_ID);
         clearRange(player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE), BLOCK_RANGE_ID);
      }
   }

   private static void clearRange(AttributeInstance attribute, Identifier id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }
}

