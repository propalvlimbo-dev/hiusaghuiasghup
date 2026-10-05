package org.xrose.feature.impl.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.impl.movement.InventoryMoveFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.utils.inventory.InventorySwap;
import org.xrose.utils.text.ChatUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class ServerHelperFeature extends Feature {
   public final ModeSetting server = this.register(new ModeSetting("Server", "FunTime", "FunTime", "HolyWorld", "ReallyWorld"));
   public final InputBindSetting ftDisorientation = this.register(this.funTimeBind("Дезориентация"));
   public final InputBindSetting ftSheerDust = this.register(this.funTimeBind("Явная пыль"));
   public final InputBindSetting ftGodsAura = this.register(this.funTimeBind("Божья аура"));
   public final InputBindSetting ftFreezeSnowball = this.register(this.funTimeBind("Снежок заморозка"));
   public final InputBindSetting ftFieryTornado = this.register(this.funTimeBind("Огненный смерч"));
   public final InputBindSetting ftStratum = this.register(this.funTimeBind("Пласт"));
   public final InputBindSetting ftTrap = this.register(this.funTimeBind("Трапка"));
   public final InputBindSetting ftStrengthPotion = this.register(this.funTimeBind("Зелье силы"));
   public final InputBindSetting ftInvisibilityPotion = this.register(this.funTimeBind("Зелье невидимости"));
   public final InputBindSetting ftSpeedPotion = this.register(this.funTimeBind("Зелье скорости"));
   public final InputBindSetting ftLeapingPotion = this.register(this.funTimeBind("Зелье прыгучести"));
   public final InputBindSetting ftRegenerationPotion = this.register(this.funTimeBind("Зелье регенерации"));
   public final InputBindSetting ftNightVisionPotion = this.register(this.funTimeBind("Зелье ночного зрения"));
   public final InputBindSetting ftFireResistancePotion = this.register(this.funTimeBind("Зелье огнестойкости"));
   public final InputBindSetting ftWaterBreathingPotion = this.register(this.funTimeBind("Зелье водного дыхания"));
   public final InputBindSetting ftPopper = this.register(this.funTimeBind("Хлопушка"));
   public final InputBindSetting ftHolyWater = this.register(this.funTimeBind("Святая вода"));
   public final InputBindSetting ftRagePotion = this.register(this.funTimeBind("Зелье Гнева"));
   public final InputBindSetting ftPaladinPotion = this.register(this.funTimeBind("Зелье Палладина"));
   public final InputBindSetting ftAssassinPotion = this.register(this.funTimeBind("Зелье Ассасина"));
   public final InputBindSetting ftRadiationPotion = this.register(this.funTimeBind("Зелье Радиации"));
   public final InputBindSetting ftDrowsinessPotion = this.register(this.funTimeBind("Снотворное"));
   public final InputBindSetting hwWinnerPotion = this.register(this.holyWorldBind("Зелье победителя"));
   public final InputBindSetting hwStrengthPotion = this.register(this.holyWorldBind("Улучшенное зелье силы"));
   public final InputBindSetting hwSpeedPotion = this.register(this.holyWorldBind("Улучшенное зелье скорости"));
   public final InputBindSetting hwStun = this.register(this.holyWorldBind("Стан"));
   public final InputBindSetting hwExplosiveTrap = this.register(this.holyWorldBind("Взрывная трапка"));
   public final InputBindSetting hwTrap = this.register(this.holyWorldBind("Трапка"));
   public final InputBindSetting hwTntCannon = this.register(this.holyWorldBind("Тнт-Пушка"));
   public final InputBindSetting hwDynamite = this.register(this.holyWorldBind("Динамит"));
   public final InputBindSetting hwDynamiteA = this.register(this.holyWorldBind("Динамит A"));
   public final InputBindSetting hwDynamiteB = this.register(this.holyWorldBind("Динамит B"));
   public final InputBindSetting hwC4 = this.register(this.holyWorldBind("C4"));
   public final InputBindSetting hwBlastWave = this.register(this.holyWorldBind("Разрывная волна"));
   public final InputBindSetting hwDynamiteB2 = this.register(this.holyWorldBind("Динамит Б2"));
   public final InputBindSetting hwStealer = this.register(this.holyWorldBind("Стиллер"));
   public final InputBindSetting hwReliableStealer = this.register(this.holyWorldBind("Надёжный стиллер"));
   public final InputBindSetting hwIceWave = this.register(this.holyWorldBind("Ледяная волна"));
   public final InputBindSetting hwSpecialCompass = this.register(this.holyWorldBind("Особый компас"));
   public final InputBindSetting hwEnchantedApple = this.register(this.holyWorldBind("Зачарованное яблоко"));
   public final InputBindSetting hwUniversalKey = this.register(this.holyWorldBind("Универсальный ключ"));
   public final InputBindSetting hwVexEgg = this.register(this.holyWorldBind("Vex Spawn Egg"));
   public final InputBindSetting hwGoldenSpawner = this.register(this.holyWorldBind("Золотой спавнер"));
   public final InputBindSetting hwUniqueClaim = this.register(this.holyWorldBind("Уникальный приват"));
   public final InputBindSetting hwOpenBackpack = this.register(this.holyWorldBind("Открыть рюкзак"));
   public final InputBindSetting rwAntiflight = this.register(this.reallyWorldBind("Анти полет"));
   public final InputBindSetting rwExpScroll = this.register(this.reallyWorldBind("Свиток опыта"));
   public final InputBindSetting rwGrinchPotion = this.register(this.reallyWorldBind("Зелье Гринча"));
   public final InputBindSetting rwAutoShift = this.register(this.reallyWorldBind("AutoShift (Шифт для шара)"));
   public final InputBindSetting rwNewYearHorror = this.register(this.reallyWorldBind("Новогодний ужас"));
   public final InputBindSetting rwDarkEssence = this.register(this.reallyWorldBind("Эссенция кромешника"));
   public final InputBindSetting rwSnowball = this.register(this.reallyWorldBind("Снежок"));
   public final InputBindSetting rwTrap = this.register(this.reallyWorldBind("Трапка"));
   public final BooleanSetting rwAutoFixAll = this.register(
      new BooleanSetting("Auto /fix all", false).configKey("reallyworld.autofixall").visibleWhen(this::showReallyWorldSettings)
   );
   private static final List<DonItems.DonItem> BACKPACK_PRIORITY = List.of(
      DonItems.HolyWorld.INFINITY_BACKPACK,
      DonItems.HolyWorld.BACKPACK_IV,
      DonItems.HolyWorld.BACKPACK_III,
      DonItems.HolyWorld.BACKPACK_II,
      DonItems.HolyWorld.BACKPACK_I
   );
   private final List<ServerHelperFeature.QuickAction> funTimeActions = List.of(
      this.action(this.ftDisorientation, DonItems.FunTime.DISORIENTATION),
      this.action(this.ftSheerDust, DonItems.FunTime.SHEER_DUST),
      this.action(this.ftGodsAura, DonItems.FunTime.GODS_AURA),
      this.action(this.ftFreezeSnowball, DonItems.FunTime.FREEZE_SNOWBALL),
      this.action(this.ftFieryTornado, DonItems.FunTime.FIERY_TORNADO),
      this.action(this.ftStratum, DonItems.FunTime.STRATUM),
      this.action(this.ftTrap, DonItems.FunTime.TRAP),
      this.action(this.ftStrengthPotion, DonItems.FunTime.ENHANCED_STRENGTH_POTION),
      this.action(this.ftInvisibilityPotion, DonItems.FunTime.ENHANCED_INVISIBILITY_POTION),
      this.action(this.ftSpeedPotion, DonItems.FunTime.ENHANCED_SPEED_POTION),
      this.action(this.ftLeapingPotion, DonItems.FunTime.ENHANCED_LEAPING_POTION),
      this.action(this.ftRegenerationPotion, DonItems.FunTime.ENHANCED_REGENERATION_POTION),
      this.action(this.ftNightVisionPotion, DonItems.FunTime.ENHANCED_NIGHT_VISION_POTION),
      this.action(this.ftFireResistancePotion, DonItems.FunTime.ENHANCED_FIRE_RESISTANCE_POTION),
      this.action(this.ftWaterBreathingPotion, DonItems.FunTime.ENHANCED_WATER_BREATHING_POTION),
      this.action(this.ftPopper, DonItems.FunTime.POPPER),
      this.action(this.ftHolyWater, DonItems.FunTime.HOLY_WATER),
      this.action(this.ftRagePotion, DonItems.FunTime.RAGE_POTION),
      this.action(this.ftPaladinPotion, DonItems.FunTime.PALADIN_POTION),
      this.action(this.ftAssassinPotion, DonItems.FunTime.ASSASSIN_POTION),
      this.action(this.ftRadiationPotion, DonItems.FunTime.RADIATION_POTION),
      this.action(this.ftDrowsinessPotion, DonItems.FunTime.DROWSINESS_POTION)
   );
   private final List<ServerHelperFeature.QuickAction> holyWorldActions = List.of(
      this.action(this.hwWinnerPotion, DonItems.HolyWorld.WINNER_POTION),
      this.action(this.hwStrengthPotion, DonItems.HolyWorld.ENHANCED_STRENGTH_POTION),
      this.action(this.hwSpeedPotion, DonItems.HolyWorld.ENHANCED_SPEED_POTION),
      this.action(this.hwStun, DonItems.HolyWorld.STAN),
      this.action(this.hwExplosiveTrap, DonItems.HolyWorld.EXPLOSIVE_TRAP),
      this.action(this.hwTrap, DonItems.HolyWorld.TRAP),
      this.action(this.hwTntCannon, DonItems.HolyWorld.TNT_CANNON),
      this.action(this.hwDynamite, DonItems.HolyWorld.DYNAMITE),
      this.action(this.hwDynamiteA, DonItems.HolyWorld.DYNAMITE_A),
      this.action(this.hwDynamiteB, DonItems.HolyWorld.DYNAMITE_B),
      this.action(this.hwC4, DonItems.HolyWorld.C4),
      this.action(this.hwBlastWave, DonItems.HolyWorld.BLAST_WAVE),
      this.action(this.hwDynamiteB2, DonItems.HolyWorld.DYNAMITE_B2),
      this.action(this.hwStealer, DonItems.HolyWorld.STEALER),
      this.action(this.hwReliableStealer, DonItems.HolyWorld.RELIABLE_STEALER),
      this.action(this.hwIceWave, DonItems.HolyWorld.ICE_WAVE),
      this.action(this.hwSpecialCompass, DonItems.HolyWorld.SPECIAL_COMPASS),
      this.action(this.hwEnchantedApple, DonItems.HolyWorld.ENCHANTED_APPLE),
      this.action(this.hwUniversalKey, DonItems.HolyWorld.UNIVERSAL_KEY),
      this.action(this.hwVexEgg, DonItems.HolyWorld.VEX_SPAWN_EGG),
      this.action(this.hwGoldenSpawner, DonItems.HolyWorld.GOLDEN_SPAWNER),
      this.action(this.hwUniqueClaim, DonItems.HolyWorld.UNIQUE_CLAIM)
   );
   private final List<ServerHelperFeature.QuickAction> reallyWorldActions = List.of(
      this.action(this.rwAntiflight, DonItems.ReallyWorld.ANTIFLIGHT), this.action(this.rwExpScroll, DonItems.ReallyWorld.EXP_SCROLL)
   );
   private final List<ServerHelperFeature.RwQuickAction> reallyWorldCustomActions = List.of(
      this.rwAction(this.rwGrinchPotion, ServerHelperFeature.RwActionKind.GRINCH_POTION),
      this.rwAction(this.rwAutoShift, ServerHelperFeature.RwActionKind.AUTO_SHIFT),
      this.rwAction(this.rwNewYearHorror, ServerHelperFeature.RwActionKind.NEW_YEAR_HORROR),
      this.rwAction(this.rwDarkEssence, ServerHelperFeature.RwActionKind.DARK_ESSENCE),
      this.rwAction(this.rwSnowball, ServerHelperFeature.RwActionKind.SNOWBALL),
      this.rwAction(this.rwTrap, ServerHelperFeature.RwActionKind.TRAP)
   );
   private ServerHelperFeature.RwActionKind pendingRwAction;
   private int shiftPulseTicks;
   private boolean wasInPvp;
   private int nextFixTick;
   private final Map<DonItems.DonItem, ItemStack> quickUseIconCache = new HashMap<>();
   private ServerHelperFeature.PendingAction pendingAction;
   private static final long BUNDLE_SELECT_DELAY_MS = 60L;
   private static final long BUNDLE_EXTRACT_DELAY_MS = 40L;
   private ServerHelperFeature.BundleExtractState bundleExtractState = ServerHelperFeature.BundleExtractState.IDLE;
   private long bundleExtractTimer;
   private int bundleMenuSlot = -1;
   private int bundleTargetIndex = -1;
   private int bundleEmptySlot = -1;

   public ServerHelperFeature() {
      super("ServerHelper", "Quick FunTime/HolyWorld DonItems actions", FeatureCategory.MISC, -1);
   }

   @Override
   protected void onDisable() {
      this.pendingAction = null;
      this.pendingRwAction = null;
      this.shiftPulseTicks = 0;
      Minecraft client = Minecraft.getInstance();
      if (client.options != null) {
         client.options.keyShift.setDown(false);
      }

      this.resetBundleExtractState();
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (event.getAction() == 1 && this.queueMatching(bind -> bind.matches(event.getKey()))) {
         event.cancel();
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      if (event.getAction() == 1 && this.queueMatching(bind -> bind.matchesMouse(event.getButton()))) {
         event.cancel();
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      this.handleShiftPulse(event);
      this.handleAutoFixAll(event);
      if (this.bundleExtractState != ServerHelperFeature.BundleExtractState.IDLE) {
         this.processBundleExtraction(event);
      } else {
         if (this.pendingRwAction != null) {
            this.handleRwAction(event);
         }

         if (this.pendingAction != null && !InventorySwap.isBusy() && !PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
            ServerHelperFeature.PendingAction action = this.pendingAction;
            this.pendingAction = null;
            Minecraft client = event.getClient();
            LocalPlayer player = client.player;
            if (player != null && client.gameMode != null && client.gui.screen() == null) {
               ServerHelperFeature.FoundItem found = this.findItem(player, action.candidates);
               if (found == null) {
                  ServerHelperFeature.BundleTarget bundleTarget = this.findItemInBundles(player, action.candidates);
                  if (bundleTarget == null) {
                     ChatUtil.error("Не найдено: " + action.label);
                  } else {
                     this.startBundleExtract(player, bundleTarget, action);
                  }
               } else {
                  boolean waitForUse = this.requiresHeldUse(found.stack);
                  if (found.selected) {
                     InventorySwap.useSelected(waitForUse, true);
                  } else {
                     InventorySwap.useFromSlot(found.containerSlot, waitForUse, true, true);
                  }
               }
            }
         }
      }
   }

   private void startBundleExtract(LocalPlayer player, ServerHelperFeature.BundleTarget target, ServerHelperFeature.PendingAction action) {
      if (player != null && player.connection != null) {
         this.bundleMenuSlot = target.bundleMenuSlot;
         this.bundleTargetIndex = target.targetIndex;
         this.bundleExtractTimer = System.currentTimeMillis();
         player.connection.send(new ServerboundSelectBundleItemPacket(this.bundleMenuSlot, this.bundleTargetIndex));
         this.bundleExtractState = ServerHelperFeature.BundleExtractState.SELECT_SENT;
         ChatUtil.info("Извлекаю из мешочка: " + action.label);
      }
   }

   private void processBundleExtraction(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null && client.gameMode != null && player.containerMenu == player.inventoryMenu) {
         long now = System.currentTimeMillis();
         switch (this.bundleExtractState) {
            case SELECT_SENT:
               if (now - this.bundleExtractTimer < 60L) {
                  return;
               }

               int emptySlot = this.findEmptyInventorySlot(player);
               if (emptySlot < 0) {
                  ChatUtil.error("Нет свободного слота для извлечения из мешочка");
                  this.resetBundleExtractState();
                  return;
               }

               this.bundleEmptySlot = emptySlot;
               if (this.isLegitInventoryMove()) {
                  Vec3 velocity = player.getDeltaMovement();
                  if (Math.abs(velocity.x) < 0.001 && Math.abs(velocity.z) < 0.001) {
                     player.connection.send(new Pos(player.getX(), player.getY(), player.getZ(), player.onGround(), player.horizontalCollision));
                  }
               }

               client.gameMode.handleContainerInput(player.inventoryMenu.containerId, this.bundleMenuSlot, 1, ContainerInput.PICKUP, player);
               client.gameMode.handleContainerInput(player.inventoryMenu.containerId, emptySlot, 0, ContainerInput.PICKUP, player);
               this.bundleExtractTimer = now;
               this.bundleExtractState = ServerHelperFeature.BundleExtractState.EXTRACTED;
               break;
            case EXTRACTED:
               if (now - this.bundleExtractTimer < 40L) {
                  return;
               }

               ItemStack extracted = player.inventoryMenu.getSlot(this.bundleEmptySlot).getItem();
               if (!extracted.isEmpty()) {
                  boolean waitForUse = this.requiresHeldUse(extracted);
                  InventorySwap.useFromSlot(this.bundleEmptySlot, waitForUse, true, true);
               } else {
                  ChatUtil.error("Не удалось извлечь предмет из мешочка");
               }

               this.resetBundleExtractState();
               break;
            default:
               this.resetBundleExtractState();
         }
      } else {
         this.resetBundleExtractState();
      }
   }

   private void handleShiftPulse(GameTickEvent event) {
      if (this.shiftPulseTicks > 0) {
         this.shiftPulseTicks--;
         if (this.shiftPulseTicks <= 0) {
            Minecraft client = event.getClient();
            if (client.options != null) {
               client.options.keyShift.setDown(false);
            }
         }
      }
   }

   private void handleAutoFixAll(GameTickEvent event) {
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player == null || client.level == null) {
         this.wasInPvp = false;
      } else if (this.server.is("ReallyWorld") && this.rwAutoFixAll.getValue()) {
         boolean inPvp = this.isPvp(client);
         if (this.wasInPvp && !inPvp && player.tickCount >= this.nextFixTick) {
            player.connection.sendCommand("fix all");
            this.nextFixTick = player.tickCount + 40;
         }

         this.wasInPvp = inPvp;
      } else {
         this.wasInPvp = this.isPvp(client);
      }
   }

   private boolean isPvp(Minecraft client) {
      return client.level.players().stream().filter(p -> p != client.player).anyMatch(p -> p.distanceToSqr(client.player) < 100.0);
   }

   private void handleRwAction(GameTickEvent event) {
      ServerHelperFeature.RwActionKind kind = this.pendingRwAction;
      this.pendingRwAction = null;
      Minecraft client = event.getClient();
      LocalPlayer player = client.player;
      if (player != null
         && client.gameMode != null
         && client.gui.screen() == null
         && !InventorySwap.isBusy()
         && !PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
         switch (kind) {
            case GRINCH_POTION:
               this.usePredicateItem(player, stack -> this.isNamedSplashPotion(stack, "гринч"), true);
               break;
            case AUTO_SHIFT:
               client.options.keyShift.setDown(true);
               this.shiftPulseTicks = 9;
               break;
            case NEW_YEAR_HORROR:
               this.usePredicateItem(player, stack -> this.isNamedSplashPotion(stack, "новогодний ужас"), true);
               break;
            case DARK_ESSENCE:
               this.usePredicateItem(player, stack -> this.isNamedSplashPotion(stack, "эссенция кромешника"), true);
               break;
            case SNOWBALL:
               this.usePredicateItem(player, stack -> this.isNamedSplashPotion(stack, "снежок"), true);
               break;
            case TRAP:
               this.usePredicateItem(player, stack -> stack.is(Items.HEART_OF_THE_SEA), false);
         }
      }
   }

   private boolean isNamedSplashPotion(ItemStack stack, String namePart) {
      return stack.is(Items.SPLASH_POTION) && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(namePart);
   }

   private void usePredicateItem(LocalPlayer player, Predicate<ItemStack> predicate, boolean waitForUse) {
      Minecraft client = Minecraft.getInstance();
      if (client.gameMode != null) {
         if (predicate.test(player.getOffhandItem())) {
            client.gameMode.useItem(player, InteractionHand.OFF_HAND);
         } else if (predicate.test(player.getMainHandItem())) {
            InventorySwap.useSelected(waitForUse);
         } else {
            for (int slot = 36; slot < 45; slot++) {
               if (slot - 36 != player.getInventory().getSelectedSlot() && predicate.test(player.inventoryMenu.getSlot(slot).getItem())) {
                  InventorySwap.useFromSlot(slot, waitForUse, true);
                  return;
               }
            }

            for (int slot = 9; slot < 36; slot++) {
               if (predicate.test(player.inventoryMenu.getSlot(slot).getItem())) {
                  InventorySwap.useFromSlot(slot, waitForUse, true);
                  return;
               }
            }

            ChatUtil.error("Не найдено: предмет не в инвентаре");
         }
      }
   }

   private int findEmptyInventorySlot(LocalPlayer player) {
      for (int slot = 9; slot < 45; slot++) {
         if (player.inventoryMenu.getSlot(slot).getItem().isEmpty()) {
            return slot;
         }
      }

      return -1;
   }

   private boolean isLegitInventoryMove() {
      InventoryMoveFeature inventoryMove = InventoryMoveFeature.getEnabled();
      return inventoryMove != null && "Legit".equals(inventoryMove.getSelectedMode());
   }

   private void resetBundleExtractState() {
      this.bundleExtractState = ServerHelperFeature.BundleExtractState.IDLE;
      this.bundleMenuSlot = -1;
      this.bundleTargetIndex = -1;
      this.bundleEmptySlot = -1;
      this.bundleExtractTimer = 0L;
   }

   private ServerHelperFeature.BundleTarget findItemInBundles(LocalPlayer player, List<DonItems.DonItem> candidates) {
      for (int slot = 9; slot < 45; slot++) {
         ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
         if (!stack.isEmpty() && stack.is(Items.BUNDLE)) {
            List<ItemStack> innerItems = new ArrayList<>();
            BundleContents bundleContents = (BundleContents)stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundleContents != null) {
               bundleContents.itemCopyStream().forEach(innerItems::add);
            }

            ItemContainerContents containerContents = (ItemContainerContents)stack.get(DataComponents.CONTAINER);
            if (containerContents != null) {
               containerContents.allItemsCopyStream().forEach(innerItems::add);
            }

            for (int index = 0; index < innerItems.size(); index++) {
               ItemStack inner = innerItems.get(index);
               if (!inner.isEmpty()) {
                  for (DonItems.DonItem candidate : candidates) {
                     if (candidate.matches(inner)) {
                        return new ServerHelperFeature.BundleTarget(slot, index);
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public List<ServerHelperFeature.QuickUseEntry> quickUseEntries(LocalPlayer player) {
      if (this.isEnabled() && player != null) {
         DonItems.Server selected = this.selectedServer().orElse(DonItems.Server.FUNTIME);
         Map<DonItems.DonItem, ServerHelperFeature.ItemSummary> inventory = this.indexQuickUseInventory(player, selected);
         List<ServerHelperFeature.QuickUseEntry> entries = new ArrayList<>();

         for (ServerHelperFeature.QuickAction action : switch (selected) {
            case FUNTIME -> this.funTimeActions;
            case HOLYWORLD -> this.holyWorldActions;
            case REALLYWORLD -> this.reallyWorldActions;
         }) {
            if (action.bind.isBound()) {
               entries.add(this.quickUseEntry(action.bind, List.of(action.item), inventory));
            }
         }

         if (selected == DonItems.Server.REALLYWORLD) {
            for (ServerHelperFeature.RwQuickAction action : this.reallyWorldCustomActions) {
               if (action.bind.isBound()) {
                  entries.add(this.quickUseEntry(action.bind, this.rwDisplayStack(action.kind), this.countRwItem(player, action.kind)));
               }
            }
         }

         if (selected == DonItems.Server.HOLYWORLD && this.hwOpenBackpack.isBound()) {
            entries.add(this.quickUseEntry(this.hwOpenBackpack, BACKPACK_PRIORITY, inventory));
         }

         return List.copyOf(entries);
      } else {
         return List.of();
      }
   }

   private Map<DonItems.DonItem, ServerHelperFeature.ItemSummary> indexQuickUseInventory(LocalPlayer player, DonItems.Server selected) {
      Map<DonItems.DonItem, ServerHelperFeature.ItemSummary> inventory = new HashMap<>();

      for (int slot = 9; slot < 45; slot++) {
         ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
         if (!stack.isEmpty()) {
            DonItems.find(stack, selected).ifPresent(item -> {
               ServerHelperFeature.ItemSummary summary = inventory.computeIfAbsent(item, ignored -> new ServerHelperFeature.ItemSummary());
               summary.add(stack);
               ItemStack icon = stack.copy();
               icon.setCount(1);
               this.quickUseIconCache.put(item, icon);
            });
         }
      }

      return inventory;
   }

   public ItemStack iconForBind(InputBindSetting bind) {
      if (bind == null) {
         return ItemStack.EMPTY;
      }

      for (ServerHelperFeature.QuickAction action : this.funTimeActions) {
         if (action.bind == bind) {
            return DonItems.displayStack(action.item);
         }
      }

      for (ServerHelperFeature.QuickAction action : this.holyWorldActions) {
         if (action.bind == bind) {
            return DonItems.displayStack(action.item);
         }
      }

      for (ServerHelperFeature.QuickAction action : this.reallyWorldActions) {
         if (action.bind == bind) {
            return DonItems.displayStack(action.item);
         }
      }

      for (ServerHelperFeature.RwQuickAction action : this.reallyWorldCustomActions) {
         if (action.bind == bind) {
            return this.rwDisplayStack(action.kind);
         }
      }

      return this.hwOpenBackpack == bind ? DonItems.displayStack(DonItems.HolyWorld.INFINITY_BACKPACK) : ItemStack.EMPTY;
   }

   private ServerHelperFeature.QuickUseEntry quickUseEntry(InputBindSetting bind, ItemStack icon, int count) {
      return new ServerHelperFeature.QuickUseEntry(icon, count, bind.getDisplayValue());
   }

   private ServerHelperFeature.QuickUseEntry quickUseEntry(
      InputBindSetting bind, List<DonItems.DonItem> candidates, Map<DonItems.DonItem, ServerHelperFeature.ItemSummary> inventory
   ) {
      int count = 0;
      ItemStack icon = ItemStack.EMPTY;

      for (DonItems.DonItem candidate : candidates) {
         ServerHelperFeature.ItemSummary summary = inventory.get(candidate);
         if (summary != null) {
            count += summary.count;
            if (icon.isEmpty()) {
               icon = summary.icon;
            }
         }
      }

      if (icon.isEmpty()) {
         for (DonItems.DonItem candidate : candidates) {
            ItemStack cached = this.quickUseIconCache.get(candidate);
            if (cached != null && !cached.isEmpty()) {
               icon = cached;
               break;
            }
         }
      }

      if (icon.isEmpty() && !candidates.isEmpty()) {
         icon = DonItems.displayStack(candidates.getFirst());
      }

      return new ServerHelperFeature.QuickUseEntry(icon, count, bind.getDisplayValue());
   }

   private ItemStack rwDisplayStack(ServerHelperFeature.RwActionKind kind) {
      Item item = switch (kind) {
         case GRINCH_POTION, NEW_YEAR_HORROR, DARK_ESSENCE, SNOWBALL -> Items.SPLASH_POTION;
         case AUTO_SHIFT -> Items.PLAYER_HEAD;
         case TRAP -> Items.HEART_OF_THE_SEA;
      };
      return new ItemStack(item);
   }

   private Predicate<ItemStack> rwPredicate(ServerHelperFeature.RwActionKind kind) {
      return switch (kind) {
         case GRINCH_POTION -> stack -> this.isNamedSplashPotion(stack, "гринч");
         case AUTO_SHIFT -> stack -> stack.is(Items.PLAYER_HEAD);
         case NEW_YEAR_HORROR -> stack -> this.isNamedSplashPotion(stack, "новогодний ужас");
         case DARK_ESSENCE -> stack -> this.isNamedSplashPotion(stack, "эссенция кромешника");
         case SNOWBALL -> stack -> this.isNamedSplashPotion(stack, "снежок");
         case TRAP -> stack -> stack.is(Items.HEART_OF_THE_SEA);
      };
   }

   private int countRwItem(LocalPlayer player, ServerHelperFeature.RwActionKind kind) {
      Predicate<ItemStack> predicate = this.rwPredicate(kind);
      int count = 0;

      for (int slot = 9; slot < 45; slot++) {
         ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
         if (!stack.isEmpty() && predicate.test(stack)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private boolean queueMatching(Predicate<InputBindSetting> matcher) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null
         && client.gameMode != null
         && client.gui.screen() == null
         && this.pendingAction == null
         && !InventorySwap.isBusy()
         && !PveAutomationCoordinator.INSTANCE.isClaimed(AutomationResource.INVENTORY)) {
         Optional<DonItems.Server> selected = this.selectedServer();
         if (selected.isEmpty()) {
            return false;
         }

         for (ServerHelperFeature.QuickAction action : switch ((DonItems.Server)selected.get()) {
            case FUNTIME -> this.funTimeActions;
            case HOLYWORLD -> this.holyWorldActions;
            case REALLYWORLD -> this.reallyWorldActions;
         }) {
            if (matcher.test(action.bind)) {
               this.pendingAction = new ServerHelperFeature.PendingAction(List.of(action.item), action.item.displayName());
               return true;
            }
         }

         if (selected.get() == DonItems.Server.HOLYWORLD && matcher.test(this.hwOpenBackpack)) {
            this.pendingAction = new ServerHelperFeature.PendingAction(BACKPACK_PRIORITY, "рюкзак");
            return true;
         }

         if (selected.get() == DonItems.Server.REALLYWORLD) {
            for (ServerHelperFeature.RwQuickAction action : this.reallyWorldCustomActions) {
               if (matcher.test(action.bind)) {
                  this.pendingRwAction = action.kind;
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private ServerHelperFeature.FoundItem findItem(LocalPlayer player, List<DonItems.DonItem> candidates) {
      int selectedHotbar = player.getInventory().getSelectedSlot();

      for (DonItems.DonItem candidate : candidates) {
         ItemStack selected = player.getMainHandItem();
         if (candidate.matches(selected)) {
            return new ServerHelperFeature.FoundItem(-1, selected.copy(), true);
         }

         for (int slot = 36; slot < 45; slot++) {
            if (slot - 36 != selectedHotbar) {
               ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
               if (candidate.matches(stack)) {
                  return new ServerHelperFeature.FoundItem(slot, stack.copy(), false);
               }
            }
         }

         for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
            if (candidate.matches(stack)) {
               return new ServerHelperFeature.FoundItem(slot, stack.copy(), false);
            }
         }
      }

      return null;
   }

   private boolean requiresHeldUse(ItemStack stack) {
      ItemUseAnimation animation = stack.getUseAnimation();
      return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
   }

   private Optional<DonItems.Server> selectedServer() {
      return this.server.is("ReallyWorld")
         ? Optional.of(DonItems.Server.REALLYWORLD)
         : Optional.of(this.server.is("HolyWorld") ? DonItems.Server.HOLYWORLD : DonItems.Server.FUNTIME);
   }

   private boolean showFunTimeSettings() {
      return this.server.is("FunTime");
   }

   private boolean showHolyWorldSettings() {
      return this.server.is("HolyWorld");
   }

   private boolean showReallyWorldSettings() {
      return this.server.is("ReallyWorld");
   }

   private InputBindSetting funTimeBind(String item) {
      return new InputBindSetting(item, -1).configKey("funtime." + item).visibleWhen(this::showFunTimeSettings);
   }

   private InputBindSetting holyWorldBind(String item) {
      return new InputBindSetting(item, -1).configKey("holyworld." + item).visibleWhen(this::showHolyWorldSettings);
   }

   private InputBindSetting reallyWorldBind(String item) {
      return new InputBindSetting(item, -1).configKey("reallyworld." + item).visibleWhen(this::showReallyWorldSettings);
   }

   private ServerHelperFeature.QuickAction action(InputBindSetting bind, DonItems.DonItem item) {
      return new ServerHelperFeature.QuickAction(bind, item);
   }

   private ServerHelperFeature.RwQuickAction rwAction(InputBindSetting bind, ServerHelperFeature.RwActionKind kind) {
      return new ServerHelperFeature.RwQuickAction(bind, kind);
   }

   private enum BundleExtractState {
      IDLE,
      SELECT_SENT,
      EXTRACTED;

      // $VF: synthetic method
      private static ServerHelperFeature.BundleExtractState[] $values() {
         return new ServerHelperFeature.BundleExtractState[]{IDLE, SELECT_SENT, EXTRACTED};
      }
   }

   private record BundleTarget(int bundleMenuSlot, int targetIndex) {
   }

   private record FoundItem(int containerSlot, ItemStack stack, boolean selected) {
   }

   private static final class ItemSummary {
      private ItemStack icon = ItemStack.EMPTY;
      private int count;

      private void add(ItemStack stack) {
         this.count = this.count + stack.getCount();
         if (this.icon.isEmpty()) {
            this.icon = stack.copy();
            this.icon.setCount(1);
         }
      }
   }

   private record PendingAction(List<DonItems.DonItem> candidates, String label) {
      private PendingAction {
         candidates = List.copyOf(candidates);
      }
   }

   private record QuickAction(InputBindSetting bind, DonItems.DonItem item) {
   }

   public record QuickUseEntry(ItemStack icon, int count, String bindLabel) {
      public QuickUseEntry {
         icon = icon == null ? ItemStack.EMPTY : icon.copy();
         count = Math.max(0, count);
         bindLabel = bindLabel == null ? "" : bindLabel;
      }

      public ItemStack icon() {
         return this.icon.copy();
      }
   }

   private enum RwActionKind {
      GRINCH_POTION,
      AUTO_SHIFT,
      NEW_YEAR_HORROR,
      DARK_ESSENCE,
      SNOWBALL,
      TRAP;

      // $VF: synthetic method
      private static ServerHelperFeature.RwActionKind[] $values() {
         return new ServerHelperFeature.RwActionKind[]{GRINCH_POTION, AUTO_SHIFT, NEW_YEAR_HORROR, DARK_ESSENCE, SNOWBALL, TRAP};
      }
   }

   private record RwQuickAction(InputBindSetting bind, ServerHelperFeature.RwActionKind kind) {
   }
}

