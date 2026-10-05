package org.xrose.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.AttackEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class HitSoundFeature extends Feature {
   public final ModeSetting sound = this.register(new ModeSetting("Sound", "Pop", "Pop", "Level Up", "Arrow", "Anvil"));
   public final NumberSetting volume = this.register(new NumberSetting("Volume", 1.0, 0.1, 2.0, 0.05, ""));
   public final NumberSetting pitch = this.register(new NumberSetting("Pitch", 1.2, 0.5, 2.0, 0.05, ""));

   public HitSoundFeature() {
      super("HitSound", "Plays a sound on every attack", FeatureCategory.COMBAT, -1);
   }

   @EventTarget
   public void onAttack(AttackEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null && client.crosshairPickEntity instanceof LivingEntity) {
         SoundEvent selected = this.resolveSound();
         if (selected != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(selected, this.pitch.getFloat(), this.volume.getFloat()));
         }
      }
   }

   private SoundEvent resolveSound() {
      return switch ((String)this.sound.getValue()) {
         case "Level Up" -> SoundEvents.PLAYER_LEVELUP;
         case "Arrow" -> SoundEvents.ARROW_HIT_PLAYER;
         case "Anvil" -> SoundEvents.ANVIL_LAND;
         default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
      };
   }
}

