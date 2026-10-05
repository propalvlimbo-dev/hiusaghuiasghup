package org.xrose.feature.impl.player;

import java.util.stream.StreamSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.mixin.accessor.KeyMappingAccessor;
import org.xrose.utils.network.PacketUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class FarmHelperFeature extends Feature {
   private static final long CLICK_DELAY = 50L;
   private static final long COMMAND_DELAY = 1000L;
   private final BooleanSetting autoInvisibility = this.register(new BooleanSetting("AutoInvisibility", false));
   private final BooleanSetting autoEat = this.register(new BooleanSetting("AutoEat", false));
   private final BooleanSetting autoClicker = this.register(new BooleanSetting("AutoClicker", false));
   private final BooleanSetting expBottleFill = this.register(new BooleanSetting("ExpBottleFill", false));
   private final NumberSetting minDuration = this.register(new NumberSetting("Min Duration", 10.0, 0.0, 60.0, 1.0, ""));
   private final NumberSetting hunger = this.register(new NumberSetting("Hunger", 8.0, 0.0, 20.0, 1.0, ""));
   private final NumberSetting cooldown = this.register(new NumberSetting("Cooldown (ms)", 100.0, 50.0, 1000.0, 10.0, ""));
   private final NumberSetting fillingLevel = this.register(new NumberSetting("ExpBottleFill Level", 15.0, 15.0, 50.0, 1.0, ""));
   private boolean drinking;
   private boolean eating;
   private int prevSlotInvis;
   private int prevSlotEat;
   private long lastAttack;
   private long lastClickTime;
   private long lastCommandTime;

   public FarmHelperFeature() {
      super("FarmHelper", "Farm automation helper", FeatureCategory.PLAYER, -1);
      this.minDuration.visibleWhen(this.autoInvisibility::getValue);
      this.hunger.visibleWhen(this.autoEat::getValue);
      this.cooldown.visibleWhen(this.autoClicker::getValue);
      this.fillingLevel.visibleWhen(this.expBottleFill::getValue);
   }

   public boolean isDrinking() {
      return this.drinking;
   }

   public boolean isEating() {
      return this.eating;
   }

   @Override
   protected void onEnable() {
      this.lastClickTime = 0L;
      this.lastCommandTime = 0L;
      LocalPlayer player = FarmHelperFeature.MinecraftHolder.mc.player;
      if (player != null && this.expBottleFill.getValue()) {
         player.sendSystemMessage(
            Component.literal("Данная функция предназначена для сервера ")
               .append(Component.literal("FunTime").withStyle(ChatFormatting.GOLD))
               .append(Component.literal(". С уважением "))
               .append(Component.literal("XRose Client").withStyle(ChatFormatting.AQUA))
         );
      }
   }

   @Override
   protected void onDisable() {
      LocalPlayer player = FarmHelperFeature.MinecraftHolder.mc.player;
      if (player != null) {
         if (this.drinking) {
            this.stopDrinking(player);
         }

         if (this.eating) {
            this.stopEating(player);
         }
      }

      this.lastClickTime = 0L;
      this.lastCommandTime = 0L;
      this.lastAttack = 0L;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      LocalPlayer player = event.getClient().player;
      if (player != null && event.getClient().gameMode != null) {
         if (this.autoInvisibility.getValue()) {
            this.handleAutoInvisibility(event.getClient());
         }

         if (this.autoEat.getValue()) {
            this.handleAutoEat(event.getClient());
         }

         if (!this.drinking && !this.eating) {
            if (this.autoClicker.getValue()) {
               this.handleAutoClicker();
            }

            if (this.expBottleFill.getValue()) {
               this.handleExpBottleFill(event.getClient());
            }
         }
      }
   }

   private void handleAutoInvisibility(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player != null) {
         if (hasInvisibilityEffect(player) && !(getInvisibilityDuration(player) <= this.minDuration.getValue() * 20.0)) {
            if (this.drinking) {
               this.stopDrinking(player);
            }
         } else {
            boolean found;
            if (!this.isHandGoodInvisibility(player, InteractionHand.MAIN_HAND) && !this.isHandGoodInvisibility(player, InteractionHand.OFF_HAND)) {
               found = this.switchToInvisibilityPotion(player);
            } else {
               found = true;
            }

            if (!found) {
               if (this.drinking) {
                  this.stopDrinking(player);
               }

               return;
            }

            this.startDrinking(mc);
         }
      }
   }

   private void handleAutoEat(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player != null) {
         int foodLevel = player.getFoodData().getFoodLevel();
         boolean isHungry = foodLevel < 20 || foodLevel <= this.hunger.getValue();
         if (isHungry) {
            boolean found;
            if (!this.isHandGoodEat(player, InteractionHand.MAIN_HAND) && !this.isHandGoodEat(player, InteractionHand.OFF_HAND)) {
               found = this.switchToFood(player);
            } else {
               found = true;
            }

            if (!found) {
               if (this.eating) {
                  this.stopEating(player);
               }

               return;
            }

            this.startEating(mc);
         } else if (this.eating) {
            this.stopEating(player);
         }
      }
   }

   private void handleAutoClicker() {
      LocalPlayer player = FarmHelperFeature.MinecraftHolder.mc.player;
      if (player != null) {
         long currentTime = System.currentTimeMillis();
         if (player.isUsingItem()) {
            ItemUseAnimation anim = player.getUseItem().getUseAnimation();
            if (anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK) {
               return;
            }
         }

         if (currentTime - this.lastAttack >= this.cooldown.getValue().longValue()) {
            this.performLeftClick();
            this.lastAttack = currentTime;
         }
      }
   }

   private void handleExpBottleFill(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null) {
         int requiredLevel = this.fillingLevel.getValue().intValue();
         if (player.experienceLevel < requiredLevel) {
            if (mc.gui.screen() instanceof ContainerScreen) {
               player.closeContainer();
            }
         } else {
            if (mc.gui.screen() instanceof ContainerScreen chestScreen) {
               this.handleChestScreen(chestScreen, requiredLevel);
            } else if (System.currentTimeMillis() - this.lastCommandTime >= 1000L) {
               player.connection.sendCommand("exp");
               this.lastCommandTime = System.currentTimeMillis();
            }
         }
      }
   }

   private void startDrinking(Minecraft mc) {
      this.drinking = true;
      if (mc.gui.screen() != null && !mc.player.isUsingItem()) {
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      } else {
         mc.options.keyUse.setDown(true);
      }
   }

   private void stopDrinking(LocalPlayer player) {
      this.drinking = false;
      FarmHelperFeature.MinecraftHolder.mc.options.keyUse.setDown(false);
      syncSelectedHotbarSlot(player, this.prevSlotInvis);
   }

   private boolean switchToInvisibilityPotion(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (isInvisibilityPotion(stack)) {
            this.prevSlotInvis = player.getInventory().getSelectedSlot();
            syncSelectedHotbarSlot(player, i);
            return true;
         }
      }

      return false;
   }

   private boolean isHandGoodInvisibility(LocalPlayer player, InteractionHand hand) {
      ItemStack stack = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
      return isInvisibilityPotion(stack);
   }

   private static boolean isInvisibilityPotion(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
         return false;
      }

      PotionContents potionContents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
      if (potionContents != null) {
         return StreamSupport.<MobEffectInstance>stream(potionContents.getAllEffects().spliterator(), false)
            .anyMatch(effect -> effect.is(MobEffects.INVISIBILITY));
      }

      String itemName = stack.getHoverName().getString().toLowerCase();
      return itemName.contains("invisibility") || itemName.contains("невидимост");
   }

   private static boolean hasInvisibilityEffect(LocalPlayer player) {
      return player.hasEffect(MobEffects.INVISIBILITY);
   }

   private static int getInvisibilityDuration(LocalPlayer player) {
      if (!hasInvisibilityEffect(player)) {
         return 0;
      }

      MobEffectInstance effect = player.getEffect(MobEffects.INVISIBILITY);
      return effect != null ? effect.getDuration() : 0;
   }

   private void startEating(Minecraft mc) {
      this.eating = true;
      if (mc.gui.screen() != null && !mc.player.isUsingItem()) {
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
      } else {
         mc.options.keyUse.setDown(true);
      }
   }

   private void stopEating(LocalPlayer player) {
      this.eating = false;
      FarmHelperFeature.MinecraftHolder.mc.options.keyUse.setDown(false);
      syncSelectedHotbarSlot(player, this.prevSlotEat);
   }

   private boolean switchToFood(LocalPlayer player) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (stack.get(DataComponents.FOOD) != null) {
            this.prevSlotEat = player.getInventory().getSelectedSlot();
            syncSelectedHotbarSlot(player, i);
            return true;
         }
      }

      return false;
   }

   private boolean isHandGoodEat(LocalPlayer player, InteractionHand hand) {
      ItemStack stack = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
      return stack.get(DataComponents.FOOD) != null;
   }

   private void performLeftClick() {
      Minecraft mc = FarmHelperFeature.MinecraftHolder.mc;
      if (mc.gui.screen() == null) {
         click(mc.options.keyAttack);
      }
   }

   private void handleChestScreen(ContainerScreen screen, int requiredLevel) {
      try {
         ChestMenu handler = (ChestMenu)screen.getMenu();
         String searchPattern = requiredLevel + " Уров";

         for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = (Slot)handler.slots.get(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
               String itemName = stack.getHoverName().getString();
               if (itemName.contains(searchPattern) || stack.is(Items.DRAGON_BREATH)) {
                  long currentTime = System.currentTimeMillis();
                  if (currentTime - this.lastClickTime >= 50L) {
                     this.clickSlot(handler.containerId, i);
                     this.lastClickTime = currentTime;
                  }

                  return;
               }
            }
         }
      } catch (Exception var11) {
      }
   }

   private void clickSlot(int containerId, int slotIndex) {
      Minecraft mc = FarmHelperFeature.MinecraftHolder.mc;
      if (mc.player != null && mc.gameMode != null) {
         mc.gameMode.handleContainerInput(containerId, slotIndex, 0, ContainerInput.PICKUP, mc.player);
      }
   }

   private static void syncSelectedHotbarSlot(LocalPlayer player, int slot) {
      player.getInventory().setSelectedSlot(slot);
      PacketUtil.sendHeldItemChange(slot);
   }

   private static void click(KeyMapping mapping) {
      KeyMapping.click(((KeyMappingAccessor)mapping).getKey());
   }

   private static final class MinecraftHolder {
      static final Minecraft mc = Minecraft.getInstance();
   }
}

