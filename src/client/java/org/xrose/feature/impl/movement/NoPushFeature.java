package org.xrose.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class NoPushFeature extends Feature {
   public final BooleanSetting entity = this.register(new BooleanSetting("Entity", true));
   public final BooleanSetting blocks = this.register(new BooleanSetting("Blocks", true));
   public final BooleanSetting water = this.register(new BooleanSetting("Water", true));
   public final BooleanSetting fishingHook = this.register(new BooleanSetting("Fishing Hook", true));

   public NoPushFeature() {
      super("NoPush", "Prevents the player from being pushed", FeatureCategory.MOVEMENT, -1);
   }

   public static NoPushFeature getInstance() {
      return FeatureManager.INSTANCE.getFeature(NoPushFeature.class);
   }

   public static NoPushFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(NoPushFeature.class);
   }

   public static boolean shouldCancelEntityPush(Entity self, Entity other) {
      NoPushFeature feature = getEnabled();
      if (feature != null && feature.entity.getValue()) {
         Player player = localPlayer();
         return player != null && self == player && other != player;
      } else {
         return false;
      }
   }

   public static boolean shouldCancelBlockPush(Player player, BlockState state) {
      NoPushFeature feature = getEnabled();
      if (feature == null || !feature.blocks.getValue()) {
         return false;
      }

      if (state != null && state.is(Blocks.COBWEB)) {
         return false;
      }

      Player local = localPlayer();
      return local != null && player == local && state != null && !state.isAir();
   }

   public static boolean shouldCancelClosestSpacePush(LocalPlayer player) {
      NoPushFeature feature = getEnabled();
      if (feature != null && feature.blocks.getValue()) {
         Player local = localPlayer();
         return local != null && player == local;
      } else {
         return false;
      }
   }

   public static boolean shouldCancelFluidPush(Player player) {
      NoPushFeature feature = getEnabled();
      if (feature != null && feature.water.getValue()) {
         Player local = localPlayer();
         return local != null && player == local;
      } else {
         return false;
      }
   }

   public static boolean shouldCancelFishingHookPull(FishingHook hook, Entity target) {
      NoPushFeature feature = getEnabled();
      if (feature != null && feature.fishingHook.getValue()) {
         Player local = localPlayer();
         return local != null && target == local && hook != null && hook.getOwner() != local;
      } else {
         return false;
      }
   }

   private static Player localPlayer() {
      return Minecraft.getInstance().player;
   }
}

