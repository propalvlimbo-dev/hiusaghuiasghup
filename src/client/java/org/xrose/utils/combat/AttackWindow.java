package org.xrose.utils.combat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.PiercingWeapon;
import org.xrose.context.PlayerContext;
import org.xrose.feature.impl.visual.HoldMyItemsCompat;

public class AttackWindow implements PlayerContext {
   public static void attack(Entity target) {
      if (mc.gameMode != null && mc.player != null) {
         HoldMyItemsCompat.beginMainHandAttack(mc.player);
         PiercingWeapon piercing = (PiercingWeapon)mc.player.getWeaponItem().get(DataComponents.PIERCING_WEAPON);
         if (piercing != null) {
            mc.gameMode.piercingAttack(piercing);
         } else {
            mc.gameMode.attack(mc.player, target);
         }

         mc.player.swing(InteractionHand.MAIN_HAND);
         SprintManager.onAttack();
      }
   }
}

