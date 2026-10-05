package org.xrose.utils.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.xrose.utils.ColorUtil;
import sdk.api.optimize.optimize;

@optimize
public final class HurtUtil {
   private static final int HURT_COLOR = -44459;
   private static final float HURT_TICKS = 10.0F;
   private static final float DROP_EPSILON = 0.01F;
   private static final long DROP_DECAY_MS = 400L;
   private static final Map<Integer, HurtUtil.EntityHurtState> STATES = new HashMap<>();

   private HurtUtil() {
   }

   public static float factor(Entity entity) {
      if (entity instanceof LivingEntity living) {
         float fromHurtTime = living.hurtTime > 0 ? Mth.clamp(living.hurtTime / 10.0F, 0.0F, 1.0F) : 0.0F;
         return Math.max(fromHurtTime, dropFactor(living));
      } else {
         return 0.0F;
      }
   }

   private static float dropFactor(LivingEntity living) {
      int id = living.getId();
      float health = living.getHealth();
      long now = System.currentTimeMillis();
      HurtUtil.EntityHurtState state = STATES.get(id);
      float factor = 0.0F;
      if (state == null) {
         STATES.put(id, new HurtUtil.EntityHurtState(health, 0L));
      } else if (health < state.lastHealth() - 0.01F) {
         factor = 1.0F;
         STATES.put(id, new HurtUtil.EntityHurtState(health, now));
      } else {
         long elapsed = now - state.lastDropTime();
         if (elapsed < 400L) {
            factor = 1.0F - (float)elapsed / 400.0F;
         }

         STATES.put(id, new HurtUtil.EntityHurtState(health, state.lastDropTime()));
      }

      if (STATES.size() > 1024) {
         STATES.entrySet().removeIf(entry -> now - entry.getValue().lastDropTime() > 2000L);
      }

      return factor;
   }

   public static float easedFactor(Entity entity) {
      float factor = factor(entity);
      return 1.0F - (1.0F - factor) * (1.0F - factor);
   }

   public static int blend(int baseColor, Entity entity, float alpha) {
      return blend(baseColor, easedFactor(entity), alpha);
   }

   public static int blend(int baseColor, float factor, float alpha) {
      factor = Mth.clamp(factor, 0.0F, 1.0F);
      int normal = ColorUtil.multiplyAlpha(baseColor, alpha);
      if (factor <= 0.0F) {
         return normal;
      }

      int hurt = ColorUtil.multiplyAlpha(-44459, alpha);
      return ColorUtil.lerp(normal, hurt, factor);
   }

   public static float scale(Entity entity, float intensity) {
      return 1.0F + easedFactor(entity) * Math.max(0.0F, intensity);
   }

   private record EntityHurtState(float lastHealth, long lastDropTime) {
   }
}

