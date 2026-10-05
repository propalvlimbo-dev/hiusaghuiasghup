package org.xrose.feature.impl.combat;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.impl.movement.AirStuckFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.pve.AutomationOwner;
import org.xrose.pve.AutomationPriority;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.utils.inventory.InventorySwap;
import org.xrose.utils.inventory.InventoryUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoTotemFeature extends Feature implements MinecraftContext, AutomationOwner {
   private static final double DANGER_RADIUS = 6.0;
   private static final int ANCHOR_RADIUS_XZ = 4;
   private static final int ANCHOR_RADIUS_Y = 2;
   private static final double FALL_CLIP_DEPTH = 128.0;
   private static final double SAFE_FALL_DISTANCE = 3.0;
   private static final long EQUIP_ATTEMPT_COOLDOWN_MS = 250L;
   public final NumberSetting health = this.register(new NumberSetting("Health", 16.0, 1.0, 20.0, 0.5, ""));
   public final NumberSetting elytraHealth = this.register(new NumberSetting("Elytra Health", 8.5, 1.0, 20.0, 0.5, ""));
   public final BooleanSetting skipTalismans = this.register(new BooleanSetting("Skip Talismans", true));
   public final BooleanSetting notWhileEating = this.register(new BooleanSetting("Not While Eating", true));
   public final BooleanSetting notWithHead = this.register(new BooleanSetting("Not With Head", true));
   public final BooleanSetting returnItem = this.register(new BooleanSetting("Return Item", true));
   public final NumberSetting crystalDistance = this.register(new NumberSetting("Crystal Distance", 4.0, 1.0, 6.0, 0.5, ""));
   public final NumberSetting crystalHeight = this.register(new NumberSetting("Crystal Height", 2.5, 0.5, 8.0, 0.5, ""));
   public final NumberSetting tntDistance = this.register(new NumberSetting("TNT Distance", 8.0, 1.0, 50.0, 1.0, ""));
   public final MultiSelectSetting dangers = this.register(
      new MultiSelectSetting(
         "Dangers",
         Set.of("Fall", "Crystal", "Explosion", "Elytra", "Obsidian", "Anchor", "Mace", "Spear"),
         "Fall",
         "Crystal",
         "Explosion",
         "Elytra",
         "Obsidian",
         "Anchor",
         "Mace",
         "Spear"
      )
   );
   private boolean releasePending;
   private boolean returnPending;
   private ItemStack previousOffhandStack = ItemStack.EMPTY;
   private int previousOffhandSourceSlot = -1;
   private boolean needsReturn;
   private long lastEquipAttemptMs;

   public AutoTotemFeature() {
      super("AutoTotem", "Keeps a totem of undying in your offhand", FeatureCategory.COMBAT, -1);
      this.elytraHealth.visibleWhen(() -> this.dangers.isSelected("Elytra"));
      this.crystalDistance.visibleWhen(() -> this.dangers.isSelected("Crystal"));
      this.crystalHeight.visibleWhen(() -> this.dangers.isSelected("Crystal"));
      this.tntDistance.visibleWhen(() -> this.dangers.isSelected("Explosion"));
   }

   @Override
   protected void onDisable() {
      this.releasePending = false;
      this.returnPending = false;
      this.clearReturnState();
      PveAutomationCoordinator.INSTANCE.release(this);
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.releasePending && !InventorySwap.isBusy()) {
         this.releasePending = false;
         PveAutomationCoordinator.INSTANCE.release(this);
      }

      if (this.returnPending && !InventorySwap.isBusy()) {
         this.returnPending = false;
         this.clearReturnState();
      }

      LocalPlayer player = this.player();
      if (player == null) {
         this.clearReturnState();
      } else if (!this.notWhileEating.getValue() || !isEating(player)) {
         if (!this.notWithHead.getValue() || !player.getMainHandItem().is(Items.PLAYER_HEAD)) {
            if (this.shouldEquipTotem(player)) {
               long now = System.currentTimeMillis();
               if (now - this.lastEquipAttemptMs >= 250L) {
                  this.tryEquipTotem(player);
               }
            } else {
               if (this.needsReturn && this.returnItem.getValue() && !this.previousOffhandStack.isEmpty()) {
                  ItemStack offhand = player.getOffhandItem();
                  if (!offhand.is(Items.TOTEM_OF_UNDYING) && (offhand.isEmpty() || !offhand.is(this.previousOffhandStack.getItem()))) {
                     this.startReturn(player);
                  }
               }
            }
         }
      }
   }

   private boolean shouldEquipTotem(LocalPlayer player) {
      if (player.getHealth() <= this.health.getValue()) {
         return true;
      } else {
         return this.dangers.isSelected("Elytra") && player.isFallFlying() && player.getHealth() <= this.elytraHealth.getValue()
            ? true
            : this.dangerNearby(player);
      }
   }

   private void tryEquipTotem(LocalPlayer player) {
      if (!InventorySwap.isBusy() && !this.isPreferredTotemAlreadyInOffhand(player)) {
         this.lastEquipAttemptMs = System.currentTimeMillis();
         int containerSlot = this.findTotemSlot(player);
         if (containerSlot != -1 && PveAutomationCoordinator.INSTANCE.acquire(this, AutomationPriority.EMERGENCY, EnumSet.of(AutomationResource.INVENTORY))) {
            this.previousOffhandStack = player.getOffhandItem().copy();
            this.previousOffhandSourceSlot = containerSlot;
            this.needsReturn = !this.previousOffhandStack.isEmpty();
            InventorySwap.equip(containerSlot);
            this.releasePending = true;
         }
      }
   }

   private void startReturn(LocalPlayer player) {
      if (!InventorySwap.isBusy() && !this.previousOffhandStack.isEmpty()) {
         int slot = this.previousOffhandSourceSlot;
         if (slot < 0 || !this.isValidMenuSlot(slot) || !this.stackMatchesForReturn(player.inventoryMenu.getSlot(slot).getItem())) {
            slot = this.findExactSlot(player);
         }

         if (slot == -1) {
            slot = this.findSameItemSlot(player);
         }

         if (slot != -1) {
            InventorySwap.equip(slot);
            this.returnPending = true;
            this.needsReturn = false;
         } else {
            this.clearReturnState();
         }
      }
   }

   private int findExactSlot(LocalPlayer player) {
      return InventoryUtil.findPlayerMenuSlot(player, this::stackMatchesForReturn);
   }

   private int findSameItemSlot(LocalPlayer player) {
      return InventoryUtil.findPlayerMenuSlot(player, stack -> !stack.isEmpty() && stack.is(this.previousOffhandStack.getItem()));
   }

   private boolean stackMatchesForReturn(ItemStack stack) {
      return stack != null && !stack.isEmpty() && !this.previousOffhandStack.isEmpty()
         ? stack.is(this.previousOffhandStack.getItem())
            && stack.isEnchanted() == this.previousOffhandStack.isEnchanted()
            && stack.getHoverName().getString().equals(this.previousOffhandStack.getHoverName().getString())
         : false;
   }

   private boolean isValidMenuSlot(int slotId) {
      LocalPlayer player = this.player();
      return player != null && slotId >= 0 && slotId < player.inventoryMenu.slots.size();
   }

   private void clearReturnState() {
      this.previousOffhandStack = ItemStack.EMPTY;
      this.previousOffhandSourceSlot = -1;
      this.needsReturn = false;
   }

   private boolean isPreferredTotemAlreadyInOffhand(LocalPlayer player) {
      ItemStack offhand = player.getOffhandItem();
      if (!offhand.is(Items.TOTEM_OF_UNDYING)) {
         return false;
      } else {
         return !this.skipTalismans.getValue() ? true : !offhand.isEnchanted();
      }
   }

   private static boolean isEating(LocalPlayer player) {
      return player.isUsingItem() && player.getUseItem().has(DataComponents.FOOD);
   }

   private boolean dangerNearby(LocalPlayer player) {
      AABB box = player.getBoundingBox().inflate(6.0);
      return this.dangers.isSelected("Fall") && this.lethalFall(player)
         || this.dangers.isSelected("Crystal") && this.crystalNearby(player)
         || this.dangers.isSelected("Explosion") && this.explosionNearby(player)
         || this.dangers.isSelected("Anchor") && this.anchorNearby(player)
         || this.armedPlayerNearby(player, box);
   }

   private boolean crystalNearby(LocalPlayer player) {
      double maxHorizontal = this.crystalDistance.getValue();
      double maxVertical = this.crystalHeight.getValue();
      AABB box = player.getBoundingBox().inflate(maxHorizontal, maxVertical, maxHorizontal);

      for (EndCrystal crystal : mc.level.getEntitiesOfClass(EndCrystal.class, box)) {
         double vertical = Math.abs(crystal.getY() - player.getY());
         if (!(vertical > maxVertical)) {
            double horizontal = Math.hypot(crystal.getX() - player.getX(), crystal.getZ() - player.getZ());
            if (horizontal <= maxHorizontal) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean lethalFall(LocalPlayer player) {
      boolean airStuckHold = isAirStuckHolding(player);
      if (player.onGround()) {
         return false;
      }

      if (airStuckHold
         || !player.isInWater()
            && !player.getAbilities().flying
            && !player.isFallFlying()
            && !player.hasEffect(MobEffects.SLOW_FALLING)
            && !(player.getDeltaMovement().y >= 0.0)) {
         Vec3 from = player.position();
         BlockHitResult hit = mc.level.clip(new ClipContext(from, from.add(0.0, -128.0, 0.0), Block.COLLIDER, Fluid.ANY, player));
         if (hit.getType() != Type.MISS && !mc.level.getFluidState(hit.getBlockPos()).isEmpty()) {
            return false;
         }

         double groundY = hit.getType() == Type.MISS ? from.y - 128.0 : hit.getLocation().y;
         double accumulatedFall = airStuckHold ? 0.0 : player.fallDistance;
         double predictedDamage = accumulatedFall + (from.y - groundY) - 3.0;
         return predictedDamage >= player.getHealth();
      } else {
         return false;
      }
   }

   private static boolean isAirStuckHolding(LocalPlayer player) {
      AirStuckFeature airStuck = AirStuckFeature.getInstance();
      return airStuck != null && airStuck.isEnabled();
   }

   private boolean explosionNearby(LocalPlayer player) {
      double maxDistance = this.tntDistance.getValue();
      AABB box = player.getBoundingBox().inflate(maxDistance);
      return !mc.level.getEntitiesOfClass(PrimedTnt.class, box).isEmpty()
         ? true
         : !mc.level.getEntitiesOfClass(Creeper.class, box, creeper -> creeper.isIgnited() || creeper.getSwellDir() > 0).isEmpty();
   }

   private boolean anchorNearby(LocalPlayer player) {
      BlockPos center = player.blockPosition();

      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
         if (mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
            return true;
         }
      }

      return false;
   }

   private boolean armedPlayerNearby(LocalPlayer player, AABB box) {
      boolean obsidian = this.dangers.isSelected("Obsidian");
      boolean mace = this.dangers.isSelected("Mace");
      boolean spear = this.dangers.isSelected("Spear");
      if (!obsidian && !mace && !spear) {
         return false;
      }

      for (Player other : mc.level.getEntitiesOfClass(Player.class, box, p -> p != player)) {
         if (isThreatItem(other.getMainHandItem(), obsidian, mace, spear) || isThreatItem(other.getOffhandItem(), obsidian, mace, spear)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isThreatItem(ItemStack held, boolean obsidian, boolean mace, boolean spear) {
      return obsidian && (held.is(Items.OBSIDIAN) || held.is(Items.CRYING_OBSIDIAN)) || mace && held.is(Items.MACE) || spear && held.is(ItemTags.SPEARS);
   }

   private int findTotemSlot(LocalPlayer player) {
      if (this.skipTalismans.getValue()) {
         return InventoryUtil.findPlayerMenuSlot(player, this::isUsableTotem);
      }

      int plain = InventoryUtil.findPlayerMenuSlot(player, this::isPlainTotem);
      return plain != -1 ? plain : InventoryUtil.findPlayerMenuSlot(player, this::isUsableTotem);
   }

   private boolean isPlainTotem(ItemStack stack) {
      return stack.is(Items.TOTEM_OF_UNDYING) && !stack.isEnchanted();
   }

   private boolean isUsableTotem(ItemStack stack) {
      return stack.is(Items.TOTEM_OF_UNDYING);
   }
}

