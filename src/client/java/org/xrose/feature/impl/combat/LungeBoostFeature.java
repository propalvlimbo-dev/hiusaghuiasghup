package org.xrose.feature.impl.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.hud.NotificationsElement;
import org.xrose.mixin.invoker.MinecraftAttackInvoker;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.LINEYNAY)
public final class LungeBoostFeature extends Feature {
   private static final String ENCHANTMENT_ID = "lunge";
   public final NumberSetting boostInterval = this.register(new NumberSetting("Boost Interval", 5.0, 1.0, 20.0, 1.0, ""));
   public final NumberSetting delayBeforeHit = this.register(new NumberSetting("Hit Delay", 0.0, 0.0, 10.0, 1.0, ""));
   public final BooleanSetting onlyMoving = this.register(new BooleanSetting("Only While Moving", false));
   private LungeBoostFeature.Phase phase = LungeBoostFeature.Phase.SAFE_SLOT;
   private int phaseTimer;
   private int cachedSafeSlot = -1;
   private int cachedSpearSlot = -1;
   private boolean started;
   private int errorCooldown;

   public LungeBoostFeature() {
      super("Lunge Boost", "Swaps slots and attacks to trigger the spear Lunge dash", FeatureCategory.COMBAT, -1);
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   @Override
   protected void onDisable() {
      Minecraft client = Minecraft.getInstance();
      int spearSlot = this.cachedSpearSlot;
      int safeSlot = this.cachedSafeSlot;
      this.reset();
      if (client.player != null && client.player.getInventory().getSelectedSlot() == spearSlot) {
         select(client.player.getInventory(), safeSlot != -1 ? safeSlot : 0);
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.isEnabled()) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && client.gameMode != null) {
            if (this.errorCooldown > 0) {
               this.errorCooldown--;
            }

            if (this.phaseTimer > 0) {
               this.phaseTimer--;
            } else if (!this.onlyMoving.getValue() || isMoving(client)) {
               Inventory inventory = client.player.getInventory();
               this.cachedSpearSlot = findSpear(inventory);
               if (this.cachedSpearSlot == -1) {
                  this.notify("No spear with Lunge found");
                  this.reset();
               } else {
                  this.cachedSafeSlot = findSafeSlot(inventory, this.cachedSpearSlot);
                  if (this.cachedSafeSlot == -1) {
                     this.notify("No safe slot to swap to");
                     this.reset();
                  } else {
                     if (!this.started && this.phase == LungeBoostFeature.Phase.SAFE_SLOT) {
                        this.started = true;
                        this.phase = LungeBoostFeature.Phase.SPEAR_SLOT;
                     }

                     switch (this.phase) {
                        case SAFE_SLOT:
                           select(inventory, this.cachedSafeSlot);
                           this.phaseTimer = Math.max(1, this.boostInterval.getValue().intValue());
                           this.phase = LungeBoostFeature.Phase.SPEAR_SLOT;
                           break;
                        case SPEAR_SLOT:
                           select(inventory, this.cachedSpearSlot);
                           if (this.delayBeforeHit.getValue().intValue() <= 0) {
                              attack(client);
                              this.phase = LungeBoostFeature.Phase.SAFE_SLOT;
                           } else {
                              this.phaseTimer = this.delayBeforeHit.getValue().intValue();
                              this.phase = LungeBoostFeature.Phase.HIT;
                           }
                           break;
                        case HIT:
                           attack(client);
                           this.phase = LungeBoostFeature.Phase.SAFE_SLOT;
                     }
                  }
               }
            }
         }
      }
   }

   private void reset() {
      this.phase = LungeBoostFeature.Phase.SAFE_SLOT;
      this.phaseTimer = 0;
      this.cachedSafeSlot = -1;
      this.cachedSpearSlot = -1;
      this.started = false;
   }

   private static boolean isMoving(Minecraft client) {
      double dx = client.player.getDeltaMovement().x;
      double dz = client.player.getDeltaMovement().z;
      return Math.abs(dx) >= 0.03
         || Math.abs(dz) >= 0.03
         || client.player.input.keyPresses.forward()
         || client.player.input.keyPresses.backward()
         || client.player.input.keyPresses.left()
         || client.player.input.keyPresses.right();
   }

   private static void select(Inventory inventory, int slot) {
      inventory.setSelectedSlot(slot);
      Minecraft.getInstance().getConnection().send(new ServerboundSetCarriedItemPacket(inventory.getSelectedSlot()));
   }

   private static void attack(Minecraft client) {
      ((MinecraftAttackInvoker)client).xrose$startAttack();
   }

   private static int findSpear(Inventory inventory) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = inventory.getItem(i);
         if (!stack.isEmpty()) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId.getPath().endsWith("_spear")) {
               ItemEnchantments enchantments = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

               for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                  if (entry.getKey().unwrapKey().map(key -> key.identifier().getPath().equals("lunge")).orElse(false)) {
                     return i;
                  }
               }
            }
         }
      }

      return -1;
   }

   private static int findSafeSlot(Inventory inventory, int spearSlot) {
      int fallback = -1;

      for (int i = 0; i < 9; i++) {
         if (i != spearSlot) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
               return i;
            }

            if (!stack.has(DataComponents.FOOD) && stack.getUseAnimation() == ItemUseAnimation.NONE) {
               ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
               if (modifiers == null || !modifiers.modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.ATTACK_SPEED))) {
                  fallback = i;
               }
            }
         }
      }

      return fallback;
   }

   private void notify(String message) {
      if (this.errorCooldown <= 0) {
         this.errorCooldown = 60;
         NotificationsElement.notify(Textures.Icons.SWORDS, Theme.getAccent(), "Lunge Boost", message);
      }
   }

   private enum Phase {
      SAFE_SLOT,
      SPEAR_SLOT,
      HIT;

      // $VF: synthetic method
      private static LungeBoostFeature.Phase[] $values() {
         return new LungeBoostFeature.Phase[]{SAFE_SLOT, SPEAR_SLOT, HIT};
      }
   }
}

