package org.xrose.feature.impl.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.event.EventTarget;
import org.xrose.event.events.EventPhase;
import org.xrose.event.events.game.RotationUpdateEvent;
import org.xrose.event.events.render.Render3DEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.FriendManager;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.AngleConfig;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.combat.aura.util.MathUtils;
import org.xrose.utils.combat.aura.util.TaskPriority;
import org.xrose.utils.render.Render3DUtil;
import org.xrose.utils.render.world.WorldMeshRenderer;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class BowAimbotFeature extends Feature {
   public final NumberSetting searchDistance = this.register(new NumberSetting("Search Distance", 16.0, 5.0, 64.0, 1.0, ""));
   public final MultiSelectSetting targetType = this.register(
      new MultiSelectSetting("Target Type", List.of("Players", "Mobs", "Animals"), "Players", "Mobs", "Animals", "Armor Stands")
   );
   public final BooleanSetting prediction = this.register(new BooleanSetting("Prediction", true));
   public final BooleanSetting wallCheck = this.register(new BooleanSetting("Wall Check", true));
   public final BooleanSetting showIndicator = this.register(new BooleanSetting("Target Indicator", true));
   private LivingEntity currentTarget;

   public BowAimbotFeature() {
      super("BowAimbot", "Projectile Helper", FeatureCategory.COMBAT, -1);
   }

   @Override
   protected void onDisable() {
      this.currentTarget = null;
   }

   private LivingEntity getTarget(Iterable<Entity> entities) {
      List<LivingEntity> validTargets = new ArrayList<>();

      for (Entity entity : entities) {
         if (entity instanceof LivingEntity living && this.isValidTarget(living)) {
            validTargets.add(living);
         }
      }

      LivingEntity nearestTarget = null;
      double nearestDistance = Double.MAX_VALUE;
      Minecraft client = Minecraft.getInstance();
      Vec3 playerPos = client.player.position();

      for (LivingEntity target : validTargets) {
         double distance = target.position().distanceTo(playerPos);
         if (distance < nearestDistance && distance <= this.searchDistance.getValue()) {
            nearestDistance = distance;
            nearestTarget = target;
         }
      }

      this.currentTarget = nearestTarget;
      return this.currentTarget;
   }

   private boolean isValidTarget(LivingEntity entity) {
      if (entity == null) {
         return false;
      } else if (entity == Minecraft.getInstance().player) {
         return false;
      } else if (!entity.isAlive()) {
         return false;
      } else if (!this.targetType.isSelected("Players") && entity instanceof Player) {
         return false;
      } else if (!this.targetType.isSelected("Mobs") && entity instanceof Mob) {
         return false;
      } else {
         return !this.targetType.isSelected("Animals") && entity instanceof Animal
            ? false
            : this.targetType.isSelected("Armor Stands") || !(entity instanceof ArmorStand);
      }
   }

   private Vec3 getPredictedPosition(LivingEntity target) {
      Vec3 pos = target.position();
      double motionX = (target.getX() - target.xo) * 3.0;
      double motionZ = (target.getZ() - target.zo) * 3.0;
      double rawMotionY = target.getY() - target.yo;
      double motionY = target.onGround() ? 0.0 : rawMotionY * 1.2;
      return pos.add(motionX, motionY, motionZ).add(0.0, target.getBbHeight() / 2.0, 0.0);
   }

   private boolean isHoldingProjectile() {
      ItemStack main = Minecraft.getInstance().player.getMainHandItem();
      return main.getItem() instanceof BowItem || main.getItem() instanceof CrossbowItem || main.getItem() instanceof TridentItem;
   }

   @EventTarget
   public void onWorldRender(Render3DEvent event) {
      if (this.showIndicator.getValue() && this.currentTarget != null) {
         if (!this.currentTarget.isRemoved() && this.currentTarget.isAlive()) {
            int red = ColorUtil.rgba(255, 40, 40, 180);
            int fill = ColorUtil.rgba(255, 40, 40, 40);
            AABB box = this.currentTarget.getBoundingBox();
            float delta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 interpolated = Render3DUtil.interpolatedPosition(this.currentTarget, delta);
            box = box.move(interpolated.x - this.currentTarget.getX(), interpolated.y - this.currentTarget.getY(), interpolated.z - this.currentTarget.getZ());
            AABB inflated = box.inflate(0.1, 0.1, 0.1);
            double x1 = inflated.minX;
            double y1 = inflated.minY;
            double z1 = inflated.minZ;
            double x2 = inflated.maxX;
            double y2 = inflated.maxY;
            double z2 = inflated.maxZ;
            double cx = (x1 + x2) * 0.5;
            double cy = (y1 + y2) * 0.5;
            double cz = (z1 + z2) * 0.5;
            double hx = (x2 - x1) * 0.5;
            double hy = (y2 - y1) * 0.5;
            double hz = (z2 - z1) * 0.5;
            List<WorldMeshRenderer.PlaneRect> planeRects = new ArrayList<>(6);
            planeRects.add(new WorldMeshRenderer.PlaneRect(new Vec3(cx, y1, cz), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), hx, hz, fill));
            planeRects.add(new WorldMeshRenderer.PlaneRect(new Vec3(cx, y2, cz), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), hx, hz, fill));
            planeRects.add(new WorldMeshRenderer.PlaneRect(new Vec3(x1, cy, cz), new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0), hy, hz, fill));
            planeRects.add(new WorldMeshRenderer.PlaneRect(new Vec3(x2, cy, cz), new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0), hy, hz, fill));
            planeRects.add(new WorldMeshRenderer.PlaneRect(new Vec3(cx, cy, z1), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), hx, hy, fill));
            planeRects.add(new WorldMeshRenderer.PlaneRect(new Vec3(cx, cy, z2), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), hx, hy, fill));
            Vec3[] corners = new Vec3[]{
               new Vec3(x1, y1, z1),
               new Vec3(x2, y1, z1),
               new Vec3(x2, y1, z2),
               new Vec3(x1, y1, z2),
               new Vec3(x1, y2, z1),
               new Vec3(x2, y2, z1),
               new Vec3(x2, y2, z2),
               new Vec3(x1, y2, z2)
            };
            int[][] edges = new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {3, 7}, {0, 4}, {2, 6}, {1, 5}, {4, 5}, {5, 6}, {6, 7}, {7, 4}};
            int[][] crosses = new int[][]{{0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 5}, {1, 4}, {3, 6}, {2, 7}, {0, 7}, {3, 4}, {1, 6}, {2, 5}};
            List<WorldMeshRenderer.Line> lines = new ArrayList<>(24);

            for (int[] edge : edges) {
               lines.add(new WorldMeshRenderer.Line(corners[edge[0]], corners[edge[1]], red));
            }

            for (int[] cross : crosses) {
               lines.add(new WorldMeshRenderer.Line(corners[cross[0]], corners[cross[1]], red));
            }

            WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, List.of(), planeRects));
         }
      }
   }

   @EventTarget
   public void onRotationUpdate(RotationUpdateEvent event) {
      if (event.getPhase() == EventPhase.PRE) {
         Minecraft client = Minecraft.getInstance();
         if (this.isEnabled() && client.player != null && client.level != null) {
            ItemStack stack = client.player.getMainHandItem();
            if (!this.isValidWeaponState(stack)) {
               this.currentTarget = null;
            } else {
               this.updateTarget(client);
               if (this.currentTarget != null) {
                  this.performAim(client);
               }
            }
         }
      }
   }

   private boolean isValidWeaponState(ItemStack stack) {
      boolean holdingBow = stack.getItem() instanceof BowItem;
      boolean holdingCrossbow = stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
      boolean holdingTrident = stack.getItem() instanceof TridentItem;
      return !holdingBow && !holdingCrossbow && !holdingTrident ? false : !holdingBow || Minecraft.getInstance().player.getUseItem() == stack;
   }

   private void updateTarget(Minecraft client) {
      if (this.currentTarget != null && !this.currentTarget.isAlive()) {
         this.currentTarget = null;
      }

      if (this.currentTarget == null) {
         LivingEntity newTarget = this.getTarget(client.level.entitiesForRendering());
         if (newTarget == client.player) {
            newTarget = null;
         }

         if (this.isFriend(newTarget)) {
            newTarget = null;
         }

         if (newTarget != this.currentTarget) {
            this.currentTarget = newTarget;
         }
      } else if (this.isFriend(this.currentTarget)) {
         this.currentTarget = null;
      }
   }

   private boolean canSee(LivingEntity target) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && client.player != null) {
         Vec3 eyes = client.player.getEyePosition();
         AABB box = target.getBoundingBox();
         Vec3[] points = new Vec3[]{
            box.getCenter(),
            new Vec3(box.minX, box.minY + box.getYsize() * 0.9, box.minZ),
            new Vec3(box.maxX, box.minY + box.getYsize() * 0.9, box.maxZ),
            new Vec3(box.minX, box.minY + box.getYsize() * 0.5, box.minZ),
            new Vec3(box.maxX, box.minY + box.getYsize() * 0.5, box.maxZ),
            new Vec3(box.getCenter().x, box.minY + box.getYsize() * 0.7, box.getCenter().z)
         };

         for (Vec3 point : points) {
            if (client.level.clip(new ClipContext(eyes, point, Block.COLLIDER, Fluid.NONE, client.player)).getType() == Type.MISS) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void performAim(Minecraft client) {
      if (!this.wallCheck.getValue() || this.canSee(this.currentTarget)) {
         Vec3 shooterPos = client.player.position().add(0.0, client.player.getEyeHeight(), 0.0).add(client.player.getDeltaMovement());
         Vec3 aimPos;
         if (this.prediction.getValue()) {
            aimPos = this.getPredictedPosition(this.currentTarget);
         } else {
            aimPos = this.currentTarget.position().add(0.0, this.currentTarget.getBbHeight() * 0.5, 0.0);
         }

         double dx = aimPos.x - shooterPos.x;
         double dy = aimPos.y - shooterPos.y;
         double dz = aimPos.z - shooterPos.z;
         double distanceXZ = Math.sqrt(dx * dx + dz * dz);
         float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F + MathUtils.getRandom(-1.0F, 1.0F);
         float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distanceXZ))) + MathUtils.getRandom(-1.0F, 1.0F);
         AngleConnection.INSTANCE.rotateTo(new Angle(yaw, pitch), AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
      }
   }

   private boolean isFriend(LivingEntity entity) {
      if (entity == null) {
         return false;
      } else {
         return entity instanceof Player player ? FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) : false;
      }
   }
}

