package org.xrose.utils.combat.aura.attack;

import net.minecraft.client.Minecraft;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.utils.combat.aura.util.ServerType;

public class Pressing {
   private final int[] funTimeTicks = new int[]{10, 11, 10, 13};
   private final int[] spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12};
   private final int[] defaultTicks = new int[]{10, 11};
   private long lastClickTime = System.currentTimeMillis();

   public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
      if (Minecraft.getInstance().player == null) {
         return false;
      }

      boolean dynamic = this.hasTicksElapsedSinceLastClick(this.tickCount() - ticks) || !dynamicCooldown;
      return dynamic && Minecraft.getInstance().player.getAttackStrengthScale(ticks) > 0.9F;
   }

   public boolean hasTicksElapsedSinceLastClick(int ticks) {
      float tps = Math.max(1.0F, AuraFeature.activeTps());
      return (float)this.lastClickPassed() >= (float)(ticks * 50L) * (20.0F / tps);
   }

   public long lastClickPassed() {
      return System.currentTimeMillis() - this.lastClickTime;
   }

   public void recalculate() {
      this.lastClickTime = System.currentTimeMillis();
   }

   int tickCount() {
      AuraFeature aura = AuraFeature.getInstance();
      int count = aura.getAttackPerpetrator().getAttackHandler().getCount();

      return switch (ServerType.getServerName()) {
         case "FunTime" -> this.funTimeTicks[count % this.funTimeTicks.length];
         case "SpookyTime" -> this.spookyTicks[count % this.spookyTicks.length];
         default -> this.defaultTicks[count % this.defaultTicks.length];
      };
   }
}

