package org.xrose.feature.impl.player;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.event.events.render.Render2DEvent;
import org.xrose.event.events.screen.ScreenCloseEvent;
import org.xrose.event.events.screen.ScreenMouseButtonEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.menu.core.MenuConfigStore;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.mixin.accessor.AbstractContainerScreenAccessor;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.inventory.InventorySwap;
import org.xrose.utils.inventory.InventoryUtil;
import org.xrose.utils.inventory.swap.SwapExecutor;
import org.xrose.utils.inventory.swap.SwapSettings;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoSwapFeature extends Feature implements MinecraftContext {
   private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
   private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
   private static final int HOTBAR_TEXTURE_WIDTH = 182;
   private static final int HOTBAR_TEXTURE_HEIGHT = 22;
   private static final int SLOT_SIZE = 22;
   private static final int SLOT_GAP = 10;
   private static final int INVENTORY_SLOTS_START = 9;
   private static final int INVENTORY_SLOTS_END = 45;
   private static final float SECTOR_START_ANGLE = -90.0F;
   private static final int MAX_SLOTS = 8;
   private static final String CONFIG_KEY_PREFIX = "autoswap.item.";
   public final InputBindSetting key = this.register(new InputBindSetting("Key", 82));
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Wheel", "Wheel", "Normal"));
   public final NumberSetting slots = this.register(new NumberSetting("Slots", 4.0, 2.0, 8.0, 1.0, ""));
   public final BooleanSetting strict = this.register(new BooleanSetting("Strict", false));
   public final ModeSetting firstItem = this.register(new ModeSetting("First Item", "Totem", "Totem", "Head", "GApple", "Shield"));
   public final ModeSetting secondItem = this.register(new ModeSetting("Second Item", "Head", "Totem", "Head", "GApple", "Shield"));
   public final BooleanSetting autoDamage = this.register(new BooleanSetting("Auto Damage", false));
   public final MultiSelectSetting autoDamageConditions = this.register(
      new MultiSelectSetting("Auto Damage Conditions", Set.of("Target Low HP"), "Target Low HP", "Target No Sword")
   );
   public final NumberSetting lowHpThreshold = this.register(new NumberSetting("Low HP Threshold", 10.0, 1.0, 20.0, 1.0, ""));
   private static final int OFFHAND_BUTTON = 40;
   private final Item[] assignedItems = new Item[8];
   private final SwapExecutor swapExecutor = new SwapExecutor();
   private boolean assignedLoaded;
   private boolean spaceOpen;
   private int hoveredSlot = -1;
   private int pickTargetSlot = -1;
   private int pendingSwapSlot = -1;
   private ItemStack savedOffhandItem = ItemStack.EMPTY;
   private boolean autoDamageActive;

   public AutoSwapFeature() {
      super("AutoSwap", "Pick a hotbar item on screen while holding a key", FeatureCategory.PLAYER, -1);
      this.slots.visibleWhen(() -> this.mode.is("Wheel"));
      this.strict.visibleWhen(() -> this.mode.is("Wheel"));
      this.firstItem.visibleWhen(() -> this.mode.is("Normal"));
      this.secondItem.visibleWhen(() -> this.mode.is("Normal"));
      this.autoDamageConditions.visibleWhen(() -> this.autoDamage.getValue());
      this.lowHpThreshold.visibleWhen(() -> this.autoDamage.getValue() && this.autoDamageConditions.isSelected("Target Low HP"));
   }

   @Override
   protected void onDisable() {
      this.swapExecutor.cancel();
      this.pendingSwapSlot = -1;
      this.closeSpace();
      this.pickTargetSlot = -1;
      this.autoDamageActive = false;
      this.savedOffhandItem = ItemStack.EMPTY;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      this.swapExecutor.tick();
      if (this.autoDamage.getValue() && !this.spaceOpen && !this.swapExecutor.isRunning() && !InventorySwap.isBusy()) {
         LocalPlayer player = this.player();
         if (player != null && this.screen() == null && !MenuOverlay.isOpen()) {
            this.handleAutoDamage(player);
         }
      }
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (this.key.matches(event.getKey()) && this.handleBindAction(event.getAction())) {
         event.cancel();
      } else {
         if (this.spaceOpen && event.getKey() == 256 && event.getAction() == 1) {
            this.closeSpace();
            event.cancel();
         }
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      if (this.key.matchesMouse(event.getButton()) && this.handleBindAction(event.getAction())) {
         event.cancel();
      } else if (this.spaceOpen) {
         if (event.getAction() == 1) {
            int hovered = this.hoveredSlot;
            if (event.getButton() == 1) {
               if (hovered >= 0) {
                  this.beginAssign(hovered);
               } else {
                  this.closeSpace();
               }
            } else if (event.getButton() == 0) {
               if (hovered >= 0) {
                  this.applyHoveredSlot(hovered);
               }

               this.closeSpace();
            }
         }

         event.cancel();
      }
   }

   private boolean handleBindAction(int action) {
      if (action == 1) {
         if (this.spaceOpen) {
            this.closeSpace();
            return true;
         } else if (this.mode.is("Normal")) {
            this.useSwap();
            return true;
         } else {
            return this.openSpace();
         }
      } else {
         return action != 1 && this.spaceOpen;
      }
   }

   @EventTarget
   public void onScreenMouseButton(ScreenMouseButtonEvent event) {
      if (this.pickTargetSlot >= 0 && event.getAction() == ScreenMouseButtonEvent.Action.CLICK && event.getScreen() instanceof InventoryScreen screen) {
         Slot hovered = ((AbstractContainerScreenAccessor)screen).getHoveredSlot();
         if (hovered != null && hovered.hasItem() && hovered.index >= 9 && hovered.index < 45) {
            this.setAssignedItem(this.pickTargetSlot, hovered.getItem().getItem());
            this.pickTargetSlot = -1;
            event.cancel();
            mc.gui.setScreen(null);
         }
      }
   }

   @EventTarget
   public void onScreenClose(ScreenCloseEvent event) {
      if (event.getScreen() instanceof InventoryScreen) {
         this.pickTargetSlot = -1;
      }
   }

   @EventTarget
   public void onRender2D(Render2DEvent event) {
      if (this.spaceOpen) {
         Minecraft mc = event.getClient();
         LocalPlayer player = mc.player;
         if (player != null && mc.gui.screen() == null && !MenuOverlay.isOpen()) {
            GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
            int count = this.slotCount();
            float mouseX = (float)mc.mouseHandler.getScaledXPos(mc.getWindow());
            float mouseY = (float)mc.mouseHandler.getScaledYPos(mc.getWindow());
            int[][] offsets = slotOffsets(count);
            int centerX = extractor.guiWidth() / 2;
            int centerY = extractor.guiHeight() / 2;
            this.hoveredSlot = this.findHoveredSlot(count, centerX, centerY, mouseX, mouseY);

            for (int index = 0; index < count; index++) {
               int x = centerX + offsets[index][0] - 11;
               int y = centerY + offsets[index][1] - 11;
               this.drawSlot(extractor, player, index, x, y, this.hoveredSlot == index);
            }
         } else {
            this.closeSpace();
         }
      }
   }

   private int findHoveredSlot(int count, int centerX, int centerY, float mouseX, float mouseY) {
      float dx = mouseX - centerX;
      float dy = mouseY - centerY;
      float distanceSq = dx * dx + dy * dy;
      float minRadius = 8.8F;
      float maxRadius = ringRadius(count) + 30.8F;
      if (!(distanceSq < minRadius * minRadius) && !(distanceSq > maxRadius * maxRadius)) {
         float angle = normalizeAngle((float)Math.toDegrees(Math.atan2(dy, dx)));
         float sectorSize = 360.0F / count;

         for (int index = 0; index < count; index++) {
            float center = normalizeAngle(-90.0F + index * sectorSize);
            float start = normalizeAngle(center - sectorSize / 2.0F);
            float end = normalizeAngle(center + sectorSize / 2.0F);
            if (isAngleInside(angle, start, end)) {
               return index;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private static boolean isAngleInside(float angle, float startAngle, float endAngle) {
      return startAngle <= endAngle ? angle >= startAngle && angle <= endAngle : angle >= startAngle || angle <= endAngle;
   }

   private static float normalizeAngle(float angle) {
      float normalized = angle % 360.0F;
      return normalized < 0.0F ? normalized + 360.0F : normalized;
   }

   private static int[][] slotOffsets(int count) {
      return ring(count, Math.round(ringRadius(count)), -90.0F);
   }

   private static float ringRadius(int count) {
      float step = 32.0F;

      return switch (count) {
         case 2 -> step * 1.0F;
         case 3 -> step * 1.3F;
         case 4 -> step * 1.35F;
         case 5, 6 -> step * 1.55F;
         case 7 -> step * 1.85F;
         default -> step * 1.9F;
      };
   }

   private static int[][] ring(int count, int radius, float startAngleDeg) {
      int[][] offsets = new int[count][2];

      for (int index = 0; index < count; index++) {
         double angle = Math.toRadians(startAngleDeg + index * 360.0 / count);
         offsets[index][0] = (int)Math.round(Math.cos(angle) * radius);
         offsets[index][1] = (int)Math.round(Math.sin(angle) * radius);
      }

      return offsets;
   }

   private void drawSlot(GuiGraphicsExtractor extractor, LocalPlayer player, int slot, int x, int y, boolean hovered) {
      int half = 11;
      extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, x, y, half, 22);
      extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 182 - half, 0, x + half, y, half, 22);
      if (hovered) {
         int accent = Theme.getAccent();
         Render2DUtil.rect(x + 1, y + 1, 20.0F, 20.0F).color(ColorUtil.withAlpha(accent, 90)).draw();
         Render2DUtil.flush();
         extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, x - 1, y - 1, 24, 23, accent);
      }

      Item item = this.assignedItem(slot);
      if (item != null) {
         ItemStack stack = new ItemStack(item);
         extractor.item(stack, x + 3, y + 3);
         extractor.itemDecorations(mc.font, stack, x + 3, y + 3);
      }
   }

   private boolean openSpace() {
      if (!this.spaceOpen && this.inGame() && this.screen() == null && !MenuOverlay.isOpen() && mc.mouseHandler.isMouseGrabbed()) {
         mc.mouseHandler.releaseMouse();
         this.spaceOpen = true;
         this.hoveredSlot = -1;
         this.pickTargetSlot = -1;
         return true;
      } else {
         return false;
      }
   }

   private void useSwap() {
      LocalPlayer player = this.player();
      if (player != null
         && this.screen() == null
         && !MenuOverlay.isOpen()
         && !PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)
         && !this.swapExecutor.isRunning()) {
         Item first = getItemByType(this.firstItem.getValue());
         Item second = getItemByType(this.secondItem.getValue());
         int validSlot = -1;
         if (first != Items.AIR && !player.getOffhandItem().is(first)) {
            validSlot = findItemSlot(player, first);
         }

         if (validSlot == -1) {
            validSlot = findItemSlot(player, second);
         }

         if (validSlot != -1 && !player.getOffhandItem().is(player.inventoryMenu.getSlot(validSlot).getItem().getItem())) {
            this.swapToOffhand(validSlot);
         }
      }
   }

   private void swapToOffhand(int containerSlot) {
      LocalPlayer player = this.player();
      if (player != null
         && player.containerMenu == player.inventoryMenu
         && player.inventoryMenu.isValidSlotIndex(containerSlot)
         && !this.swapExecutor.isRunning()) {
         this.pendingSwapSlot = containerSlot;
         this.swapExecutor.execute(this::doSwapClick, SwapSettings.legit().closeInventory(false), () -> this.pendingSwapSlot = -1);
      }
   }

   private void doSwapClick() {
      LocalPlayer player = this.player();
      if (player != null
         && player.containerMenu == player.inventoryMenu
         && player.inventoryMenu.isValidSlotIndex(this.pendingSwapSlot)
         && !player.inventoryMenu.getSlot(this.pendingSwapSlot).getItem().isEmpty()) {
         mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, this.pendingSwapSlot, 40, ContainerInput.SWAP, player);
      }
   }

   private static Item getItemByType(String itemType) {
      return switch (itemType) {
         case "Totem" -> Items.TOTEM_OF_UNDYING;
         case "Head" -> Items.PLAYER_HEAD;
         case "GApple" -> Items.GOLDEN_APPLE;
         case "Shield" -> Items.SHIELD;
         default -> Items.AIR;
      };
   }

   private void applyHoveredSlot(int hovered) {
      LocalPlayer player = this.player();
      if (player != null
         && hovered >= 0
         && hovered < this.slotCount()
         && !this.swapExecutor.isRunning()
         && !PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
         Item item = this.assignedItem(hovered);
         if (item == null) {
            this.beginAssign(hovered);
         } else {
            int containerSlot = findItemSlot(player, item);
            if (containerSlot >= 0) {
               this.swapToOffhand(containerSlot);
            }
         }
      }
   }

   private static int findItemSlot(LocalPlayer player, Item item) {
      return InventoryUtil.findPlayerMenuSlot(player, stack -> stack.is(item));
   }

   private void beginAssign(int slot) {
      LocalPlayer player = this.player();
      if (player != null) {
         this.pickTargetSlot = slot;
         this.closeSpace();
         mc.gui.setScreen(new InventoryScreen(player));
      }
   }

   private void closeSpace() {
      if (this.spaceOpen) {
         this.spaceOpen = false;
         this.hoveredSlot = -1;
         if (this.inGame() && this.screen() == null && !MenuOverlay.isOpen()) {
            mc.mouseHandler.grabMouse();
         }
      }
   }

   private int slotCount() {
      return (int)Math.round(this.slots.getValue());
   }

   private Item assignedItem(int slot) {
      this.ensureAssignedLoaded();
      return slot >= 0 && slot < 8 ? this.assignedItems[slot] : null;
   }

   private void setAssignedItem(int slot, Item item) {
      this.ensureAssignedLoaded();
      if (slot >= 0 && slot < 8) {
         this.assignedItems[slot] = item;
         String id = item == null ? "" : BuiltInRegistries.ITEM.getKey(item).toString();
         MenuConfigStore.save(data -> data.addProperty("autoswap.item." + slot, id));
      }
   }

   private void ensureAssignedLoaded() {
      if (!this.assignedLoaded) {
         this.assignedLoaded = true;

         for (int slot = 0; slot < 8; slot++) {
            String id = MenuConfigStore.getString("autoswap.item." + slot, "");
            if (!id.isEmpty()) {
               Item item = (Item)BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
               this.assignedItems[slot] = item == Items.AIR ? null : item;
            }
         }
      }
   }

   private void handleAutoDamage(LocalPlayer player) {
      LivingEntity target = AuraFeature.getInstance().getTarget();
      boolean shouldSwapToDamage = false;
      if (target != null && target.isAlive()) {
         boolean lowHpCondition = this.autoDamageConditions.isSelected("Target Low HP")
            && target.getHealth() <= this.lowHpThreshold.getValue()
            && target.getHealth() < player.getHealth();
         boolean noSwordCondition = this.autoDamageConditions.isSelected("Target No Sword")
            && target instanceof Player targetPlayer
            && getAttackDamage(targetPlayer.getMainHandItem()) <= 0.0;
         shouldSwapToDamage = this.autoDamageConditions.getValue().isEmpty() || lowHpCondition || noSwordCondition;
      }

      if (shouldSwapToDamage && !this.autoDamageActive) {
         this.savedOffhandItem = player.getOffhandItem().copy();
         int containerSlot = this.findBestDamageSlot(player);
         if (containerSlot != -1 && !player.getOffhandItem().is(player.inventoryMenu.getSlot(containerSlot).getItem().getItem())) {
            this.swapToOffhand(containerSlot);
            this.autoDamageActive = true;
         }
      } else {
         if (!shouldSwapToDamage && this.autoDamageActive) {
            if (!this.savedOffhandItem.isEmpty()) {
               int containerSlot = findItemSlot(player, this.savedOffhandItem.getItem());
               if (containerSlot != -1 && !player.getOffhandItem().is(this.savedOffhandItem.getItem())) {
                  this.swapToOffhand(containerSlot);
               }
            }

            this.autoDamageActive = false;
            this.savedOffhandItem = ItemStack.EMPTY;
         }
      }
   }

   private int findBestDamageSlot(LocalPlayer player) {
      int bestSlot = -1;
      double bestDamage = 0.0;

      for (int index = 0; index < player.inventoryMenu.slots.size(); index++) {
         Slot slot = player.inventoryMenu.getSlot(index);
         if (slot != null && slot.hasItem() && slot.container == player.getInventory()) {
            ItemStack stack = slot.getItem();
            if (stack.getItem() == Items.PLAYER_HEAD || stack.getItem() == Items.TOTEM_OF_UNDYING) {
               double damage = getAttackDamage(stack);
               if (damage > bestDamage) {
                  bestDamage = damage;
                  bestSlot = index;
               }
            }
         }
      }

      return bestSlot;
   }

   private static double getAttackDamage(ItemStack stack) {
      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0;
      }

      double totalDamage = 0.0;

      for (Entry entry : modifiers.modifiers()) {
         if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
            totalDamage += entry.modifier().amount();
         }
      }

      return totalDamage;
   }
}

