package org.xrose.feature.impl.pve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.pve.AutomationPriority;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.pve.PveFeature;
import org.xrose.utils.combat.aura.Angle;
import org.xrose.utils.combat.aura.MathAngle;
import org.xrose.utils.combat.aura.util.StopWatch;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoPottBotFeature extends PveFeature implements MinecraftContext {
   private static final long TIMESTAMP = 20000L;
   private static final long TIMESTAMP_4 = 1600L;
   private static final long LOOK_SETTLE_MS = 250L;
   public static volatile boolean flag;
   public static volatile String text = "—";
   public static volatile int intValue;
   public static volatile int intValue2;
   public static volatile int intValue3;
   public static volatile int intValue4;
   public static volatile int intValue5;
   public static volatile int intValue6;
   public static volatile int[] ints = new int[7];
   public static volatile List<AutoPottBotFeature.AutoPottBotDisplayEntry> items = List.of();
   public static volatile List<String> items2 = List.of();
   private final ModeSetting zele = this.register(new ModeSetting("Potion", "Strength", "Strength", "Speed", "Fire Resistance", "Invisibility"));
   private final NumberSetting zaderzhkaKlikov = this.register(new NumberSetting("Click Delay", 120.0, 30.0, 600.0, 10.0, ""));
   private final NumberSetting radiusVarok = this.register(new NumberSetting("Brewing Stand Radius", 4.5, 2.0, 6.0, 0.5, ""));
   private final BooleanSetting napolnyatButylki = this.register(new BooleanSetting("Fill Bottles", true));
   private final NumberSetting buferVody = this.register(new NumberSetting("Water Buffer", 12.0, 3.0, 24.0, 1.0, ""));
   private final BooleanSetting skladyvatVSunduk = this.register(new BooleanSetting("Deposit to Chest", true));
   private final NumberSetting transitionCooldown = this.register(new NumberSetting("Transition Delay", 500.0, 100.0, 2000.0, 50.0, ""));
   private final NumberSetting rotationRandom = this.register(new NumberSetting("Rotation Random", 1.5, 0.0, 5.0, 0.1, ""));
   private final NumberSetting aimThreshold = this.register(new NumberSetting("Aim Threshold", 1.5, 0.5, 5.0, 0.1, ""));
   private final StopWatch dualTimer = new StopWatch();
   private final StopWatch dualTimer2 = new StopWatch();
   private final StopWatch fullChestsTimer = new StopWatch();
   private final StopWatch transitionTimer = new StopWatch();
   private final Set<BlockPos> fullChests = new HashSet<>();
   private final Map<BlockPos, AutoPottBotFeature.AutoPottBotState5> valuesByKey = new LinkedHashMap<>();
   private final Map<String, Long> valuesByKey2 = new HashMap<>();
   private AutoPottBotFeature.AutoPottBotState2 autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
   private BlockPos blockPos;
   private BlockPos blockPos2;
   private BlockPos blockPos3;
   private int lastDepositSlot = -1;
   private int depositFailCount;
   private final List<int[]> hotbarQueue = new ArrayList<>();
   private boolean hotbarSwapping;
   private int intValue7;
   private int intValue8;
   private int intValue9;
   private long timestamp;
   private int intValue10;
   private Angle cachedLookTarget;
   private Angle cachedChestTarget;
   private long aimSettleStart;

   public AutoPottBotFeature() {
      super(
         "AutoPottBot",
         "Automatically brews potions at brewing stands",
         -1,
         AutomationPriority.BOT,
         AutomationResource.ROTATION,
         AutomationResource.INVENTORY,
         AutomationResource.SCREEN
      );
   }

   @Override
   protected void onPveEnable() {
      this.invoke();
      flag = true;
   }

   @Override
   protected void onPveDisable() {
      flag = false;
      if (this.player() != null && (this.player().containerMenu instanceof BrewingStandMenu || this.player().containerMenu instanceof ChestMenu)) {
         this.player().closeContainer();
      }

      this.invoke();
   }

   @Override
   protected void onPvePreempted(PveAutomationCoordinator.RevocationReason reason) {
      flag = false;
      if (this.player() != null && (this.player().containerMenu instanceof BrewingStandMenu || this.player().containerMenu instanceof ChestMenu)) {
         this.player().closeContainer();
      }

      this.invoke();
   }

   private void invoke() {
      this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      this.blockPos = null;
      this.blockPos2 = null;
      this.blockPos3 = null;
      this.cachedLookTarget = null;
      this.cachedChestTarget = null;
      this.aimSettleStart = 0L;
      this.timestamp = 0L;
      this.intValue8 = 0;
      this.intValue10 = 0;
      this.valuesByKey.clear();
      this.valuesByKey2.clear();
      this.fullChests.clear();
      this.lastDepositSlot = -1;
      this.depositFailCount = 0;
      this.hotbarSwapping = false;
      this.hotbarQueue.clear();
      this.dualTimer.reset();
      this.dualTimer2.reset();
      this.transitionTimer.reset();
      items = List.of();
      items2 = List.of();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null && client.level != null && client.gameMode != null) {
         if (!this.player().isUsingItem()) {
            this.invoke2();
            switch (this.autoPottBotState2) {
               case SCAN:
                  this.invoke3();
                  break;
               case OPENING:
                  this.invoke4();
                  break;
               case SERVICING:
                  this.invoke5();
                  break;
               case CLOSING:
                  this.invoke6();
                  break;
               case FILL_WATER:
                  this.invoke7();
                  break;
               case DEPOSIT_OPEN:
                  this.invoke10();
                  break;
               case DEPOSIT_MOVE:
                  this.invoke11();
                  break;
               case HOTBAR_MOVE:
                  this.invokeHotbarMove();
            }

            this.invoke15();
         }
      }
   }

   private void invoke2() {
      if (this.fullChestsTimer.every(30000.0)) {
         this.fullChests.clear();
      }

      long longValue = System.currentTimeMillis();
      int intValue = (int)Math.ceil(this.radiusVarok.getValue() + 4.0);
      BlockPos blockPos2 = this.player().blockPosition();

      for (int intValue2 = -intValue; intValue2 <= intValue; intValue2++) {
         for (int intValue3 = -intValue; intValue3 <= intValue; intValue3++) {
            for (int intValue4 = -intValue; intValue4 <= intValue; intValue4++) {
               BlockPos blockPos3 = blockPos2.offset(intValue2, intValue3, intValue4);
               if (this.level().getBlockEntity(blockPos3) instanceof BrewingStandBlockEntity) {
                  AutoPottBotFeature.AutoPottBotState5 autoPottBotState5 = this.valuesByKey.get(blockPos3);
                  if (autoPottBotState5 == null) {
                     this.valuesByKey.put(blockPos3.immutable(), new AutoPottBotFeature.AutoPottBotState5(blockPos3.immutable()));
                  } else {
                     autoPottBotState5.timestamp4 = longValue;
                  }
               }
            }
         }
      }

      Iterator<Entry<BlockPos, AutoPottBotFeature.AutoPottBotState5>> iterator = this.valuesByKey.entrySet().iterator();

      while (iterator.hasNext()) {
         AutoPottBotFeature.AutoPottBotState5 autoPottBotState52 = iterator.next().getValue();
         if (this.level().getBlockEntity(autoPottBotState52.blockPos) instanceof BrewingStandBlockEntity) {
            autoPottBotState52.timestamp4 = longValue;
         } else if (longValue - autoPottBotState52.timestamp4 > 8000L) {
            iterator.remove();
         }
      }
   }

   private void invoke3() {
      if (this.transitionTimer.finished(this.transitionCooldown.getValue().longValue())) {
         if (!(this.player().containerMenu instanceof BrewingStandMenu) && !(this.player().containerMenu instanceof ChestMenu)) {
            if (this.skladyvatVSunduk.getValue() && (this.compute6() <= 3 || this.compute12()) && this.compute7() > 0) {
               BlockPos blockPos4 = this.resolve10();
               if (blockPos4 != null) {
                  this.blockPos2 = blockPos4;
                  this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.DEPOSIT_OPEN;
                  this.dualTimer2.reset();
                  this.dualTimer.reset();
                  return;
               }
            }

            if (this.napolnyatButylki.getValue()
               && this.compute8(Items.GLASS_BOTTLE) > 0
               && this.compute9() < 3
               && System.currentTimeMillis() >= this.timestamp) {
               int intValue5 = this.compute2();
               int intValue6 = this.compute9();
               int intValue7 = Math.min((int)this.buferVody.getValue().doubleValue(), Math.max(3, intValue5));
               int intValue8 = Math.max(0, this.compute6() - 5);
               int intValue9 = Math.min(intValue7, intValue6 + intValue8);
               if (intValue5 > 0 && intValue9 > intValue6) {
                  BlockPos blockPos5 = this.resolve3();
                  if (blockPos5 != null) {
                     if (this.check2()) {
                        this.blockPos3 = blockPos5;
                        this.intValue7 = intValue9;
                        this.intValue8 = 0;
                        this.intValue9 = intValue6;
                        this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.FILL_WATER;
                        this.dualTimer.reset();
                        return;
                     }

                     this.invoke9("Бутылочки в хотбар");
                  }
               }
            }

            if (this.prepareHotbarQueue()) {
               this.hotbarSwapping = true;
               this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.HOTBAR_MOVE;
               this.dualTimer.reset();
               return;
            }

            AutoPottBotFeature.AutoPottBotState5 autoPottBotState53 = this.resolve();
            if (autoPottBotState53 != null) {
               this.blockPos = autoPottBotState53.blockPos;
               this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.OPENING;
               this.dualTimer2.reset();
               this.dualTimer.reset();
            }
         } else {
            this.player().closeContainer();
         }
      }
   }

   private AutoPottBotFeature.AutoPottBotState5 resolve() {
      long longValue2 = System.currentTimeMillis();
      Vec3 vec3d = this.player().getEyePosition();
      double doubleValue = this.radiusVarok.getValue() * this.radiusVarok.getValue();
      boolean bl = this.check4();
      AutoPottBotFeature.AutoPottBotState5 autoPottBotState54 = null;
      int intValue10 = -1;
      double doubleValue2 = Double.MAX_VALUE;

      for (AutoPottBotFeature.AutoPottBotState5 autoPottBotState55 : this.valuesByKey.values()) {
         double doubleValue3 = Vec3.atCenterOf(autoPottBotState55.blockPos).distanceToSqr(vec3d);
         if (!(doubleValue3 > doubleValue)
            && longValue2 >= autoPottBotState55.timestamp3
            && (!autoPottBotState55.flag2 || longValue2 >= autoPottBotState55.timestamp2)) {
            int intValue11 = this.compute(autoPottBotState55, bl);
            if (intValue11 > 0 && (intValue11 > intValue10 || intValue11 == intValue10 && doubleValue3 < doubleValue2)) {
               autoPottBotState54 = autoPottBotState55;
               intValue10 = intValue11;
               doubleValue2 = doubleValue3;
            }
         }
      }

      return autoPottBotState54;
   }

   private int compute(AutoPottBotFeature.AutoPottBotState5 autoPottBotState56, boolean bl) {
      return switch (autoPottBotState56.autoPottBotState4) {
         case UNKNOWN -> 2;
         case EMPTY -> bl ? 1 : 0;
         case WATER, AWKWARD, BASE -> 3;
         case FINAL -> 4;
         default -> 0;
      };
   }

   private void invoke4() {
      if (this.player().containerMenu instanceof BrewingStandMenu) {
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SERVICING;
         this.dualTimer.reset();
      } else if (this.blockPos == null || !(this.level().getBlockEntity(this.blockPos) instanceof BrewingStandBlockEntity)) {
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      } else if (this.dualTimer2.finished(1600.0)) {
         AutoPottBotFeature.AutoPottBotState5 autoPottBotState57 = this.valuesByKey.get(this.blockPos);
         if (autoPottBotState57 != null) {
            autoPottBotState57.timestamp3 = System.currentTimeMillis() + 4500L;
         }

         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      } else {
         if (this.cachedLookTarget == null) {
            Vec3 target = Vec3.atCenterOf(this.blockPos);
            Angle baseAngle = MathAngle.fromTo(this.player().getEyePosition(), target);
            float rand = this.rotationRandom.getFloat();
            this.cachedLookTarget = rand > 0.0F ? baseAngle.random(rand) : baseAngle;
         }

         Angle targetAngle = this.cachedLookTarget;
         float currentYaw = this.player().getYRot();
         float currentPitch = this.player().getXRot();
         float deltaYaw = Mth.wrapDegrees(targetAngle.getYaw() - currentYaw);
         float deltaPitch = targetAngle.getPitch() - currentPitch;
         float diff = (float)Math.hypot(deltaYaw, deltaPitch);
         if (diff < this.aimThreshold.getValue()) {
            if (this.aimSettleStart == 0L) {
               this.aimSettleStart = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - this.aimSettleStart >= 250L) {
               this.invoke12(this.blockPos);
               this.aimSettleStart = 0L;
            }
         } else {
            this.aimSettleStart = 0L;
            float speed = 0.35F + (float)(Math.random() * 0.1 - 0.05);
            if (diff < 5.0F) {
               speed = Math.max(0.6F, speed * 2.0F);
            }

            float yawJitter = (float)(Math.random() * 0.3 - 0.15) * Math.min(1.0F, diff / 10.0F);
            float pitchJitter = (float)(Math.random() * 0.2 - 0.1) * Math.min(1.0F, diff / 10.0F);
            this.player().setYRot(currentYaw + deltaYaw * speed + yawJitter);
            this.player().setXRot(Mth.clamp(currentPitch + deltaPitch * speed + pitchJitter, -90.0F, 90.0F));
         }
      }
   }

   private void invoke5() {
      if (this.player().containerMenu instanceof BrewingStandMenu brewingStandScreenHandler2) {
         AutoPottBotFeature.AutoPottBotState5 autoPottBotState58 = this.valuesByKey.get(this.blockPos);
         if (autoPottBotState58 == null) {
            this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.CLOSING;
         } else if (this.dualTimer.finished(this.zaderzhkaKlikov.getValue())) {
            this.dualTimer.reset();
            AutoPottBotFeature.AutoPottBotState autoPottBotState = this.resolve2(brewingStandScreenHandler2, autoPottBotState58);
            switch (autoPottBotState) {
               case CONTINUE:
               default:
                  break;
               case BREW_STARTED:
                  long longValue3 = System.currentTimeMillis();
                  autoPottBotState58.flag2 = true;
                  autoPottBotState58.timestamp = longValue3;
                  autoPottBotState58.timestamp2 = longValue3 + 20000L;
                  autoPottBotState58.timestamp3 = autoPottBotState58.timestamp2;
                  this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.CLOSING;
                  break;
               case DONE:
                  long longValue4 = autoPottBotState58.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.EMPTY ? 4000L : 1500L;
                  autoPottBotState58.timestamp3 = System.currentTimeMillis() + longValue4;
                  this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.CLOSING;
            }
         }
      } else {
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      }
   }

   private AutoPottBotFeature.AutoPottBotState resolve2(BrewingStandMenu brewingStandScreenHandler, AutoPottBotFeature.AutoPottBotState5 autoPottBotState59) {
      boolean flag2 = !brewingStandScreenHandler.getSlot(3).getItem().isEmpty();
      int intValue12 = this.compute3(brewingStandScreenHandler, autoPottBotState59);
      boolean flag3 = intValue12 >= 0;
      autoPottBotState59.flag = flag3;
      autoPottBotState59.intValue = flag3 ? Math.min(3, intValue12 + (flag2 ? 1 : 0)) : 0;
      autoPottBotState59.autoPottBotState4 = this.resolve8(intValue12, flag2);
      if (flag2) {
         autoPottBotState59.flag2 = true;
         if (autoPottBotState59.timestamp2 == 0L) {
            autoPottBotState59.timestamp = System.currentTimeMillis();
            autoPottBotState59.timestamp2 = autoPottBotState59.timestamp + 20000L;
         }

         return AutoPottBotFeature.AutoPottBotState.DONE;
      } else {
         autoPottBotState59.flag2 = false;
         if (autoPottBotState59.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.OTHER) {
            return AutoPottBotFeature.AutoPottBotState.DONE;
         }

         if (autoPottBotState59.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.FINAL) {
            if (this.compute6() <= 0) {
               return AutoPottBotFeature.AutoPottBotState.DONE;
            }

            for (int intValue13 = 0; intValue13 < 3; intValue13++) {
               if (!brewingStandScreenHandler.getSlot(intValue13).getItem().isEmpty()) {
                  this.invoke13(intValue13);
               }
            }

            autoPottBotState59.flag = false;
            autoPottBotState59.intValue = 0;
            autoPottBotState59.autoPottBotState4 = AutoPottBotFeature.AutoPottBotState4.EMPTY;
            return AutoPottBotFeature.AutoPottBotState.CONTINUE;
         } else {
            if (!flag3) {
               AutoPottBotFeature.AutoPottBotState3 autoPottBotState3 = this.resolve7();
               if (autoPottBotState3 == null) {
                  return AutoPottBotFeature.AutoPottBotState.DONE;
               }

               autoPottBotState59.autoPottBotState3 = autoPottBotState3;
            }

            if (brewingStandScreenHandler.getFuel() <= 0) {
               if (!brewingStandScreenHandler.getSlot(4).getItem().isEmpty()) {
                  this.invoke13(4);
                  return AutoPottBotFeature.AutoPottBotState.CONTINUE;
               } else {
                  return this.check6(Items.BLAZE_POWDER, 4) ? AutoPottBotFeature.AutoPottBotState.CONTINUE : AutoPottBotFeature.AutoPottBotState.DONE;
               }
            } else {
               if (this.check5(brewingStandScreenHandler)) {
                  int intValue14 = this.compute5(itemStack -> this.check8(itemStack, Potions.WATER));
                  if (intValue14 != -1) {
                     this.invoke13(intValue14);
                     return AutoPottBotFeature.AutoPottBotState.CONTINUE;
                  }

                  if (this.compute4(brewingStandScreenHandler) == 0) {
                     return AutoPottBotFeature.AutoPottBotState.DONE;
                  }
               }

               AutoPottBotFeature.AutoPottBotState3 autoPottBotState32 = autoPottBotState59.autoPottBotState3 != null
                  ? autoPottBotState59.autoPottBotState3
                  : this.resolve7();
               if (autoPottBotState32 == null) {
                  return AutoPottBotFeature.AutoPottBotState.DONE;
               }

               autoPottBotState59.autoPottBotState3 = autoPottBotState32;

               Item item4 = switch (autoPottBotState59.autoPottBotState4) {
                  case WATER -> Items.NETHER_WART;
                  case AWKWARD -> autoPottBotState32.item;
                  case BASE -> {
                     if (autoPottBotState32.item3 != null) {
                        Holder<Potion> standPotion = this.getPotionInStand();
                        yield standPotion != null && this.check10(standPotion, autoPottBotState32.registryEntry)
                           ? autoPottBotState32.item2
                           : autoPottBotState32.item3;
                     } else {
                        yield autoPottBotState32.item2;
                     }
                  }
                  default -> null;
               };
               if (item4 == null) {
                  return AutoPottBotFeature.AutoPottBotState.DONE;
               } else if (!this.check7(item4)) {
                  return AutoPottBotFeature.AutoPottBotState.DONE;
               } else {
                  return this.check6(item4, 3) ? AutoPottBotFeature.AutoPottBotState.BREW_STARTED : AutoPottBotFeature.AutoPottBotState.DONE;
               }
            }
         }
      }
   }

   private void invoke6() {
      this.player().closeContainer();
      this.blockPos = null;
      this.cachedLookTarget = null;
      this.aimSettleStart = 0L;
      this.transitionTimer.reset();
      this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
   }

   private void invoke7() {
      if (!(this.player().containerMenu instanceof BrewingStandMenu) && !(this.player().containerMenu instanceof ChestMenu)) {
         if (this.compute9() < this.intValue7 && this.compute8(Items.GLASS_BOTTLE) > 0) {
            if (this.intValue8 > this.intValue7 * 2 + 20) {
               if (this.compute9() <= this.intValue9) {
                  this.timestamp = System.currentTimeMillis() + 6000L;
                  this.invoke9("Источник воды недостигаем");
               }

               this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
            } else {
               if (this.blockPos3 == null || !this.check(this.blockPos3)) {
                  this.blockPos3 = this.resolve3();
                  if (this.blockPos3 == null) {
                     this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
                     return;
                  }
               }

               if (!this.check3()) {
                  this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
               } else if (this.dualTimer.finished(this.zaderzhkaKlikov.getValue())) {
                  this.dualTimer.reset();
                  this.invoke8(this.blockPos3);
                  this.gameMode().useItem(this.player(), InteractionHand.MAIN_HAND);
                  this.intValue8++;
               }
            }
         } else {
            this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
         }
      } else {
         this.player().closeContainer();
      }
   }

   private int compute2() {
      int intValue15 = 0;

      for (AutoPottBotFeature.AutoPottBotState5 autoPottBotState510 : this.valuesByKey.values()) {
         if (autoPottBotState510.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.EMPTY
            || autoPottBotState510.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.UNKNOWN) {
            intValue15++;
         }
      }

      return intValue15 * 3;
   }

   private BlockPos resolve3() {
      BlockPos blockPos6 = this.player().blockPosition();
      int intValue16 = (int)Math.ceil(this.radiusVarok.getValue());
      double doubleValue4 = this.radiusVarok.getValue() * this.radiusVarok.getValue();
      Vec3 vec3d2 = this.player().getEyePosition();
      BlockPos blockPos7 = null;
      double doubleValue5 = Double.MAX_VALUE;

      for (int intValue17 = -intValue16; intValue17 <= intValue16; intValue17++) {
         for (int intValue18 = -intValue16; intValue18 <= intValue16; intValue18++) {
            for (int intValue19 = -intValue16; intValue19 <= intValue16; intValue19++) {
               BlockPos blockPos8 = blockPos6.offset(intValue17, intValue18, intValue19);
               if (this.check(blockPos8)) {
                  double doubleValue6 = Vec3.atCenterOf(blockPos8).distanceToSqr(vec3d2);
                  if (doubleValue6 <= doubleValue4 && doubleValue6 < doubleValue5) {
                     doubleValue5 = doubleValue6;
                     blockPos7 = blockPos8.immutable();
                  }
               }
            }
         }
      }

      return blockPos7;
   }

   private boolean check(BlockPos blockPos) {
      FluidState fluidState = this.level().getFluidState(blockPos);
      return fluidState.isSource() && fluidState.getType().is(FluidTags.WATER);
   }

   private boolean check2() {
      for (int intValue20 = 0; intValue20 < 9; intValue20++) {
         if (this.player().getInventory().getItem(intValue20).getItem() == Items.GLASS_BOTTLE) {
            return true;
         }
      }

      return false;
   }

   private boolean check3() {
      for (int intValue21 = 0; intValue21 < 9; intValue21++) {
         if (this.player().getInventory().getItem(intValue21).getItem() == Items.GLASS_BOTTLE) {
            if (this.player().getInventory().getSelectedSlot() != intValue21) {
               this.player().getInventory().setSelectedSlot(intValue21);
            }

            return true;
         }
      }

      return false;
   }

   private void invoke8(BlockPos blockPos) {
      Vec3 vec3d3 = this.player().getEyePosition();
      double doubleValue7 = blockPos.getX() + 0.5 - vec3d3.x;
      double doubleValue8 = blockPos.getY() + 0.5 - vec3d3.y;
      double doubleValue9 = blockPos.getZ() + 0.5 - vec3d3.z;
      double doubleValue10 = Math.sqrt(doubleValue7 * doubleValue7 + doubleValue9 * doubleValue9);
      float floatValue = (float)(Math.toDegrees(Math.atan2(doubleValue9, doubleValue7)) - 90.0);
      float floatValue2 = (float)(-Math.toDegrees(Math.atan2(doubleValue8, doubleValue10)));
      this.player().setYRot(floatValue);
      this.player().setXRot(Math.max(-90.0F, Math.min(90.0F, floatValue2)));
   }

   private void invoke9(String string) {
      long longValue5 = System.currentTimeMillis();
      Long longValue6 = this.valuesByKey2.get(string);
      if (longValue6 == null || longValue5 - longValue6 > 15000L) {
         this.valuesByKey2.put(string, longValue5);
         this.player().sendSystemMessage(Component.literal("§8[AutoPottBot] §c" + string));
      }
   }

   private void invoke10() {
      if (this.player().containerMenu instanceof ChestMenu) {
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.DEPOSIT_MOVE;
         this.dualTimer.reset();
      } else if (this.blockPos2 == null || !(this.level().getBlockEntity(this.blockPos2) instanceof ChestBlockEntity)) {
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      } else if (this.dualTimer2.finished(1600.0)) {
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      } else {
         if (this.cachedChestTarget == null) {
            Vec3 target = Vec3.atCenterOf(this.blockPos2);
            Angle baseAngle = MathAngle.fromTo(this.player().getEyePosition(), target);
            float rand = this.rotationRandom.getFloat();
            this.cachedChestTarget = rand > 0.0F ? baseAngle.random(rand) : baseAngle;
         }

         Angle targetAngle = this.cachedChestTarget;
         float currentYaw = this.player().getYRot();
         float currentPitch = this.player().getXRot();
         float deltaYaw = Mth.wrapDegrees(targetAngle.getYaw() - currentYaw);
         float deltaPitch = targetAngle.getPitch() - currentPitch;
         float diff = (float)Math.hypot(deltaYaw, deltaPitch);
         if (diff < this.aimThreshold.getValue()) {
            if (this.aimSettleStart == 0L) {
               this.aimSettleStart = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - this.aimSettleStart >= 250L) {
               this.invoke12(this.blockPos2);
               this.aimSettleStart = 0L;
            }
         } else {
            this.aimSettleStart = 0L;
            float speed = 0.35F + (float)(Math.random() * 0.1 - 0.05);
            if (diff < 5.0F) {
               speed = Math.max(0.6F, speed * 2.0F);
            }

            float yawJitter = (float)(Math.random() * 0.3 - 0.15) * Math.min(1.0F, diff / 10.0F);
            float pitchJitter = (float)(Math.random() * 0.2 - 0.1) * Math.min(1.0F, diff / 10.0F);
            this.player().setYRot(currentYaw + deltaYaw * speed + yawJitter);
            this.player().setXRot(Mth.clamp(currentPitch + deltaPitch * speed + pitchJitter, -90.0F, 90.0F));
         }
      }
   }

   private void invoke11() {
      if (this.player().containerMenu instanceof ChestMenu genericContainerScreenHandler) {
         if (this.dualTimer.finished(this.zaderzhkaKlikov.getValue())) {
            this.dualTimer.reset();
            int intValue22 = genericContainerScreenHandler.getRowCount() * 9;
            int movedSlot = -1;

            for (int intValue23 = intValue22; intValue23 < genericContainerScreenHandler.slots.size(); intValue23++) {
               if (this.check11(((Slot)genericContainerScreenHandler.slots.get(intValue23)).getItem())) {
                  movedSlot = intValue23;
                  this.invoke14(intValue23, 0, ContainerInput.QUICK_MOVE);
                  break;
               }
            }

            if (movedSlot == -1) {
               this.player().closeContainer();
               this.blockPos2 = null;
               this.lastDepositSlot = -1;
               this.depositFailCount = 0;
               this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
            } else {
               if (movedSlot == this.lastDepositSlot) {
                  this.depositFailCount++;
               } else {
                  this.lastDepositSlot = movedSlot;
                  this.depositFailCount = 1;
               }

               if (this.depositFailCount >= 3) {
                  this.fullChests.add(this.blockPos2);
                  this.player().closeContainer();
                  this.blockPos2 = null;
                  this.lastDepositSlot = -1;
                  this.depositFailCount = 0;
                  this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
               }
            }
         }
      } else {
         this.lastDepositSlot = -1;
         this.depositFailCount = 0;
         this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
      }
   }

   private boolean prepareHotbarQueue() {
      this.hotbarQueue.clear();
      List<AutoPottBotFeature.AutoPottBotState3> recipes = this.resolve4();
      Set<Item> needed = new LinkedHashSet<>();
      if (!recipes.isEmpty()) {
         needed.add(Items.BLAZE_POWDER);
         needed.add(Items.NETHER_WART);

         for (AutoPottBotFeature.AutoPottBotState3 r : recipes) {
            needed.add(r.item);
            needed.add(r.item2);
            if (r.item3 != null) {
               needed.add(r.item3);
            }
         }
      }

      if (this.shouldMoveBottlesToHotbar()) {
         needed.add(Items.GLASS_BOTTLE);
      }

      if (needed.isEmpty()) {
         return false;
      }

      for (Item item : needed) {
         if (item != null) {
            boolean inHotbar = false;

            for (int h = 0; h < 9; h++) {
               if (this.player().getInventory().getItem(h).getItem() == item) {
                  inHotbar = true;
                  break;
               }
            }

            if (!inHotbar) {
               for (int s = 9; s < 36; s++) {
                  if (this.player().getInventory().getItem(s).getItem() == item) {
                     int target = -1;

                     for (int h = 0; h < 9; h++) {
                        if (this.player().getInventory().getItem(h).isEmpty()) {
                           target = h;
                           break;
                        }
                     }

                     if (target == -1) {
                        for (int h = 0; h < 9; h++) {
                           if (this.player().getInventory().getItem(h).getItem() != item) {
                              target = h;
                              break;
                           }
                        }
                     }

                     if (target != -1) {
                        this.hotbarQueue.add(new int[]{s, target});
                     }
                     break;
                  }
               }
            }
         }
      }

      return !this.hotbarQueue.isEmpty();
   }

   private boolean shouldMoveBottlesToHotbar() {
      if (!this.napolnyatButylki.getValue()) {
         return false;
      }

      if (this.compute8(Items.GLASS_BOTTLE) <= 0) {
         return false;
      }

      if (this.compute9() >= 3) {
         return false;
      }

      if (this.compute2() <= 0) {
         return false;
      }

      if (this.resolve3() == null) {
         return false;
      }

      for (int h = 0; h < 9; h++) {
         if (this.player().getInventory().getItem(h).getItem() == Items.GLASS_BOTTLE) {
            return false;
         }
      }

      for (int s = 9; s < 36; s++) {
         if (this.player().getInventory().getItem(s).getItem() == Items.GLASS_BOTTLE) {
            return true;
         }
      }

      return false;
   }

   private void invokeHotbarMove() {
      if (this.hotbarSwapping) {
         if (!(this.player().containerMenu instanceof BrewingStandMenu) && !(this.player().containerMenu instanceof ChestMenu)) {
            if (this.dualTimer.finished(this.zaderzhkaKlikov.getValue())) {
               this.dualTimer.reset();
               if (!this.hotbarQueue.isEmpty()) {
                  int[] swap = this.hotbarQueue.remove(0);
                  int srcSlot = swap[0];
                  int dstSlot = swap[1];
                  if (srcSlot >= 9 && srcSlot < 36 && dstSlot >= 0 && dstSlot < 9 && !this.player().getInventory().getItem(srcSlot).isEmpty()) {
                     this.gameMode().handleContainerInput(this.player().containerMenu.containerId, srcSlot, dstSlot, ContainerInput.SWAP, this.player());
                     return;
                  }
               }

               this.hotbarSwapping = false;
               this.hotbarQueue.clear();
               this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
            }
         } else {
            this.hotbarSwapping = false;
            this.hotbarQueue.clear();
            this.autoPottBotState2 = AutoPottBotFeature.AutoPottBotState2.SCAN;
         }
      }
   }

   private List<AutoPottBotFeature.AutoPottBotState3> resolve4() {
      ArrayList<AutoPottBotFeature.AutoPottBotState3> arrayList = new ArrayList<>(1);

      arrayList.add(switch ((String)this.zele.getValue()) {
         case "Speed" -> AutoPottBotFeature.AutoPottBotState3.SWIFTNESS;
         case "Fire Resistance" -> AutoPottBotFeature.AutoPottBotState3.FIRE_RESISTANCE;
         case "Invisibility" -> AutoPottBotFeature.AutoPottBotState3.INVISIBILITY;
         default -> AutoPottBotFeature.AutoPottBotState3.STRENGTH;
      });
      return arrayList;
   }

   private Map<Item, Integer> resolve5() {
      HashMap<Item, Integer> hashMap = new HashMap<>();

      for (AutoPottBotFeature.AutoPottBotState5 autoPottBotState511 : this.valuesByKey.values()) {
         if (autoPottBotState511.flag && autoPottBotState511.autoPottBotState3 != null) {
            if (autoPottBotState511.intValue < 1) {
               invoke17(hashMap, Items.NETHER_WART, 1);
            }

            if (autoPottBotState511.intValue < 2) {
               invoke17(hashMap, autoPottBotState511.autoPottBotState3.item, 1);
            }

            if (autoPottBotState511.intValue < 3) {
               invoke17(hashMap, autoPottBotState511.autoPottBotState3.item2, 1);
            }
         }
      }

      return hashMap;
   }

   private List<AutoPottBotFeature.AutoPottBotState3> resolve6() {
      Map<Item, Integer> valuesByKey = this.resolve5();
      int intValue24 = this.compute9();
      ArrayList<AutoPottBotFeature.AutoPottBotState3> arrayList2 = new ArrayList<>(3);

      for (AutoPottBotFeature.AutoPottBotState3 autoPottBotState33 : this.resolve4()) {
         int intValue25 = this.compute8(Items.NETHER_WART) - valuesByKey.getOrDefault(Items.NETHER_WART, 0);
         int intValue26 = this.compute8(autoPottBotState33.item) - valuesByKey.getOrDefault(autoPottBotState33.item, 0);
         int intValue27 = this.compute8(autoPottBotState33.item2) - valuesByKey.getOrDefault(autoPottBotState33.item2, 0);
         if (intValue24 >= 1 && intValue25 >= 1 && intValue26 >= 1 && intValue27 >= 1) {
            arrayList2.add(autoPottBotState33);
         }
      }

      return arrayList2;
   }

   private boolean check4() {
      return !this.resolve6().isEmpty();
   }

   private AutoPottBotFeature.AutoPottBotState3 resolve7() {
      List<AutoPottBotFeature.AutoPottBotState3> items = this.resolve6();
      if (items.isEmpty()) {
         return null;
      }

      AutoPottBotFeature.AutoPottBotState3 autoPottBotState34 = items.get(Math.floorMod(this.intValue10, items.size()));
      this.intValue10++;
      return autoPottBotState34;
   }

   private int compute3(BrewingStandMenu brewingStandScreenHandler, AutoPottBotFeature.AutoPottBotState5 autoPottBotState512) {
      Holder<Potion> registryEntry3 = null;
      int intValue28 = 0;

      for (int intValue29 = 0; intValue29 < 3; intValue29++) {
         ItemStack itemStack2 = brewingStandScreenHandler.getSlot(intValue29).getItem();
         if (itemStack2.getItem() == Items.POTION) {
            intValue28++;
            if (registryEntry3 == null) {
               registryEntry3 = this.resolve9(itemStack2);
            }
         }
      }

      if (intValue28 == 0 || registryEntry3 == null) {
         return -1;
      }

      if (this.check10(registryEntry3, Potions.WATER)) {
         return 0;
      }

      if (this.check10(registryEntry3, Potions.AWKWARD)) {
         return 1;
      }

      for (AutoPottBotFeature.AutoPottBotState3 autoPottBotState35 : AutoPottBotFeature.AutoPottBotState3.values()) {
         if (this.check10(registryEntry3, autoPottBotState35.registryEntry2)) {
            autoPottBotState512.autoPottBotState3 = autoPottBotState35;
            return 3;
         }

         if (autoPottBotState35.registryEntry3 != null && this.check10(registryEntry3, autoPottBotState35.registryEntry3)) {
            autoPottBotState512.autoPottBotState3 = autoPottBotState35;
            return 2;
         }

         if (this.check10(registryEntry3, autoPottBotState35.registryEntry)) {
            autoPottBotState512.autoPottBotState3 = autoPottBotState35;
            return 2;
         }
      }

      return -2;
   }

   private AutoPottBotFeature.AutoPottBotState4 resolve8(int i, boolean bl) {
      return switch (i) {
         case -1 -> AutoPottBotFeature.AutoPottBotState4.EMPTY;
         case 0 -> AutoPottBotFeature.AutoPottBotState4.WATER;
         case 1 -> AutoPottBotFeature.AutoPottBotState4.AWKWARD;
         case 2 -> AutoPottBotFeature.AutoPottBotState4.BASE;
         case 3 -> AutoPottBotFeature.AutoPottBotState4.FINAL;
         default -> AutoPottBotFeature.AutoPottBotState4.OTHER;
      };
   }

   private Holder<Potion> getPotionInStand() {
      if (this.player().containerMenu instanceof BrewingStandMenu brewingStand) {
         for (int var4 = 0; var4 < 3; var4++) {
            ItemStack stack = brewingStand.getSlot(var4).getItem();
            if (stack.getItem() == Items.POTION) {
               return this.resolve9(stack);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean check5(BrewingStandMenu brewingStandScreenHandler) {
      for (int intValue30 = 0; intValue30 < 3; intValue30++) {
         if (brewingStandScreenHandler.getSlot(intValue30).getItem().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private int compute4(BrewingStandMenu brewingStandScreenHandler) {
      int intValue31 = 0;

      for (int intValue32 = 0; intValue32 < 3; intValue32++) {
         if (!brewingStandScreenHandler.getSlot(intValue32).getItem().isEmpty()) {
            intValue31++;
         }
      }

      return intValue31;
   }

   private boolean check6(Item item, int i) {
      int intValue33 = this.compute5(itemStack -> itemStack.getItem() == item);
      if (intValue33 == -1) {
         return false;
      }

      this.invoke14(intValue33, 0, ContainerInput.PICKUP);
      this.invoke14(i, 1, ContainerInput.PICKUP);
      this.invoke14(intValue33, 0, ContainerInput.PICKUP);
      return true;
   }

   private int compute5(Predicate<ItemStack> predicate) {
      AbstractContainerMenu screenHandler = this.player().containerMenu;

      for (int intValue34 = 5; intValue34 < screenHandler.slots.size(); intValue34++) {
         ItemStack itemStack3 = ((Slot)screenHandler.slots.get(intValue34)).getItem();
         if (!itemStack3.isEmpty() && predicate.test(itemStack3)) {
            return intValue34;
         }
      }

      return -1;
   }

   private boolean check7(Item item) {
      return this.compute5(itemStack -> itemStack.getItem() == item) != -1;
   }

   private boolean check8(ItemStack itemStack, Holder<Potion> registryEntry) {
      if (itemStack.getItem() != Items.POTION) {
         return false;
      }

      Holder<Potion> registryEntry4 = this.resolve9(itemStack);
      return registryEntry4 != null && this.check10(registryEntry4, registryEntry);
   }

   private boolean check9(ItemStack itemStack) {
      return itemStack.getItem() == Items.POTION || itemStack.getItem() == Items.SPLASH_POTION || itemStack.getItem() == Items.LINGERING_POTION;
   }

   private Holder<Potion> resolve9(ItemStack itemStack) {
      PotionContents potionContentsComponent = (PotionContents)itemStack.get(DataComponents.POTION_CONTENTS);
      if (potionContentsComponent == null) {
         return null;
      }

      Optional<Holder<Potion>> potion = potionContentsComponent.potion();
      return potion.orElse(null);
   }

   private boolean check10(Holder<Potion> registryEntry, Holder<Potion> registryEntry2) {
      if (registryEntry == registryEntry2) {
         return true;
      }

      Optional<ResourceKey<Potion>> key1 = registryEntry.unwrapKey();
      Optional<ResourceKey<Potion>> key2 = registryEntry2.unwrapKey();
      return key1.isPresent() && key2.isPresent() && key1.get().equals(key2.get());
   }

   private int compute6() {
      int intValue35 = 0;

      for (int intValue36 = 0; intValue36 < this.player().getInventory().getContainerSize(); intValue36++) {
         if (this.player().getInventory().getItem(intValue36).isEmpty()) {
            intValue35++;
         }
      }

      return intValue35;
   }

   private boolean check11(ItemStack itemStack) {
      if (!this.check9(itemStack)) {
         return false;
      }

      Holder<Potion> registryEntry5 = this.resolve9(itemStack);
      return registryEntry5 != null && !this.check10(registryEntry5, Potions.WATER) && !this.check10(registryEntry5, Potions.AWKWARD);
   }

   private int compute7() {
      int intValue37 = 0;

      for (int intValue38 = 0; intValue38 < this.player().getInventory().getContainerSize(); intValue38++) {
         if (this.check11(this.player().getInventory().getItem(intValue38))) {
            intValue37++;
         }
      }

      return intValue37;
   }

   private int compute8(Item item) {
      int intValue39 = 0;

      for (int intValue40 = 0; intValue40 < this.player().getInventory().getContainerSize(); intValue40++) {
         ItemStack itemStack4 = this.player().getInventory().getItem(intValue40);
         if (itemStack4.getItem() == item) {
            intValue39 += itemStack4.getCount();
         }
      }

      return intValue39;
   }

   private int compute9() {
      int intValue41 = 0;

      for (int intValue42 = 0; intValue42 < this.player().getInventory().getContainerSize(); intValue42++) {
         ItemStack itemStack5 = this.player().getInventory().getItem(intValue42);
         if (itemStack5.getItem() == Items.POTION) {
            Holder<Potion> registryEntry6 = this.resolve9(itemStack5);
            if (registryEntry6 != null && this.check10(registryEntry6, Potions.WATER)) {
               intValue41 += itemStack5.getCount();
            }
         }
      }

      return intValue41;
   }

   private boolean compute12() {
      for (int i = 0; i < this.player().getInventory().getContainerSize(); i++) {
         ItemStack stack = this.player().getInventory().getItem(i);
         if (this.check11(stack) && stack.getCount() >= 64) {
            return true;
         }
      }

      return false;
   }

   private BlockPos resolve10() {
      BlockPos blockPos9 = this.player().blockPosition();
      int intValue43 = (int)Math.ceil(this.radiusVarok.getValue() + 1.0);
      BlockPos blockPos10 = null;
      double doubleValue11 = Double.MAX_VALUE;
      Vec3 vec3d4 = this.player().getEyePosition();

      for (int intValue44 = -intValue43; intValue44 <= intValue43; intValue44++) {
         for (int intValue45 = -intValue43; intValue45 <= intValue43; intValue45++) {
            for (int intValue46 = -intValue43; intValue46 <= intValue43; intValue46++) {
               BlockPos blockPos11 = blockPos9.offset(intValue44, intValue45, intValue46);
               if (this.level().getBlockEntity(blockPos11) instanceof ChestBlockEntity chest
                  && !this.fullChests.contains(blockPos11)
                  && !this.isChestFull(chest)) {
                  double doubleValue12 = Vec3.atCenterOf(blockPos11).distanceToSqr(vec3d4);
                  if (doubleValue12 < doubleValue11) {
                     doubleValue11 = doubleValue12;
                     blockPos10 = blockPos11.immutable();
                  }
               }
            }
         }
      }

      return blockPos10;
   }

   private boolean isChestFull(ChestBlockEntity chest) {
      for (int i = 0; i < chest.getContainerSize(); i++) {
         if (chest.getItem(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void invoke12(BlockPos blockPos) {
      Vec3 vec3d5 = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
      BlockHitResult blockHitResult = new BlockHitResult(vec3d5, Direction.UP, blockPos, false);
      this.gameMode().useItemOn(this.player(), InteractionHand.MAIN_HAND, blockHitResult);
   }

   private void invoke13(int i) {
      this.invoke14(i, 0, ContainerInput.QUICK_MOVE);
   }

   private void invoke14(int i, int j, ContainerInput clickType) {
      this.gameMode().handleContainerInput(this.player().containerMenu.containerId, i, j, clickType, this.player());
   }

   private void invoke15() {
      long longValue7 = System.currentTimeMillis();
      int intValue47 = 0;
      int intValue48 = 0;
      int intValue49 = 0;
      ArrayList<AutoPottBotFeature.AutoPottBotDisplayEntry> arrayList3 = new ArrayList<>(this.valuesByKey.size());

      for (AutoPottBotFeature.AutoPottBotState5 autoPottBotState513 : this.valuesByKey.values()) {
         if (autoPottBotState513.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.FINAL) {
            intValue49++;
         } else if (autoPottBotState513.flag2 && longValue7 < autoPottBotState513.timestamp2) {
            intValue47++;
         } else if (autoPottBotState513.autoPottBotState4 != AutoPottBotFeature.AutoPottBotState4.OTHER) {
            intValue48++;
         }

         arrayList3.add(
            new AutoPottBotFeature.AutoPottBotDisplayEntry(
               autoPottBotState513.resolve(), autoPottBotState513.compute(), autoPottBotState513.measure(longValue7), autoPottBotState513.resolve2(longValue7)
            )
         );
      }

      arrayList3.sort((o1, o2) -> Float.compare(o2.progress(), o1.progress()));
      ints = new int[]{
         this.compute9(),
         this.compute8(Items.NETHER_WART),
         this.compute8(Items.BLAZE_POWDER),
         this.compute8(Items.GLOWSTONE_DUST),
         this.compute8(Items.SUGAR),
         this.compute8(Items.MAGMA_CREAM),
         this.compute8(Items.REDSTONE)
      };
      intValue6 = this.compute8(Items.GLASS_BOTTLE);
      intValue = this.valuesByKey.size();
      intValue2 = intValue47;
      intValue3 = intValue48;
      intValue4 = intValue49;
      intValue5 = this.compute10();
      items = arrayList3;
      text = this.autoPottBotState2.text;
      items2 = this.resolve11();
      this.invoke16();
   }

   private int compute10() {
      List<AutoPottBotFeature.AutoPottBotState3> items2 = this.resolve4();
      if (items2.isEmpty()) {
         return 0;
      }

      Map<Item, Integer> valuesByKey2 = this.resolve5();
      int intValue50 = this.compute9();
      int intValue51 = Math.max(0, this.compute8(Items.NETHER_WART) - valuesByKey2.getOrDefault(Items.NETHER_WART, 0));
      int intValue52 = 0;

      for (AutoPottBotFeature.AutoPottBotState3 autoPottBotState36 : items2) {
         int intValue53 = this.compute8(autoPottBotState36.item) - valuesByKey2.getOrDefault(autoPottBotState36.item, 0);
         int intValue54 = this.compute8(autoPottBotState36.item2) - valuesByKey2.getOrDefault(autoPottBotState36.item2, 0);
         intValue52 += Math.max(0, Math.min(intValue53, intValue54));
      }

      intValue52 = Math.min(intValue52, intValue51);
      return Math.max(0, Math.min(intValue50, intValue52 * 3));
   }

   private List<String> resolve11() {
      List<AutoPottBotFeature.AutoPottBotState3> items3 = this.resolve4();
      if (items3.isEmpty()) {
         return List.of("Не выбрано зелье");
      }

      Map<Item, Integer> valuesByKey3 = this.resolve5();
      ArrayList<String> arrayList4 = new ArrayList<>();
      if (this.compute9() < 1) {
         boolean flag4 = this.napolnyatButylki.getValue() && this.compute8(Items.GLASS_BOTTLE) > 0;
         if (!flag4) {
            arrayList4.add(this.compute8(Items.GLASS_BOTTLE) > 0 ? "Источник воды" : "Вода / Бутылочки");
         }
      }

      if (this.compute8(Items.NETHER_WART) - valuesByKey3.getOrDefault(Items.NETHER_WART, 0) < 1) {
         arrayList4.add("Адский нарост");
      }

      for (AutoPottBotFeature.AutoPottBotState3 autoPottBotState37 : items3) {
         if (this.compute8(autoPottBotState37.item) - valuesByKey3.getOrDefault(autoPottBotState37.item, 0) < 1) {
            invoke18(arrayList4, autoPottBotState37.item.getName(ItemStack.EMPTY).getString());
         }

         if (this.compute8(autoPottBotState37.item2) - valuesByKey3.getOrDefault(autoPottBotState37.item2, 0) < 1) {
            invoke18(arrayList4, autoPottBotState37.item2.getName(ItemStack.EMPTY).getString());
         }
      }

      if (this.compute8(Items.BLAZE_POWDER) <= 0) {
         invoke18(arrayList4, "Огненный порошок (топливо)");
      }

      return arrayList4;
   }

   private void invoke16() {
      boolean flag5 = false;

      for (AutoPottBotFeature.AutoPottBotState5 autoPottBotState514 : this.valuesByKey.values()) {
         if (autoPottBotState514.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.EMPTY
            || autoPottBotState514.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.UNKNOWN) {
            flag5 = true;
            break;
         }
      }

      if (flag5 && !items2.isEmpty()) {
         long longValue8 = System.currentTimeMillis();

         for (String text : items2) {
            Long longValue9 = this.valuesByKey2.get(text);
            if (longValue9 == null || longValue8 - longValue9 > 15000L) {
               this.valuesByKey2.put(text, longValue8);
               this.player().sendSystemMessage(Component.literal("§8[AutoPottBot] §cНе хватает: §f" + text));
            }
         }
      }
   }

   private static void invoke17(Map<Item, Integer> map, Item item, int i) {
      map.merge(item, i, Integer::sum);
   }

   private static void invoke18(List<String> list, String string) {
      if (!list.contains(string)) {
         list.add(string);
      }
   }

   public record AutoPottBotDisplayEntry(String name, int color, float progress, String label) {
   }

   enum AutoPottBotState {
      CONTINUE,
      BREW_STARTED,
      DONE;

      // $VF: synthetic method
      private static AutoPottBotFeature.AutoPottBotState[] $values() {
         return new AutoPottBotFeature.AutoPottBotState[]{CONTINUE, BREW_STARTED, DONE};
      }
   }

   enum AutoPottBotState2 {
      SCAN("Поиск"),
      OPENING("Открытие"),
      SERVICING("Загрузка"),
      CLOSING("Закрытие"),
      FILL_WATER("Налив воды"),
      DEPOSIT_OPEN("Сундук"),
      DEPOSIT_MOVE("Разгрузка"),
      HOTBAR_MOVE("Перенос в хотбар");

      final String text;

      AutoPottBotState2(String string2) {
         this.text = string2;
      }

      // $VF: synthetic method
      private static AutoPottBotFeature.AutoPottBotState2[] $values() {
         return new AutoPottBotFeature.AutoPottBotState2[]{SCAN, OPENING, SERVICING, CLOSING, FILL_WATER, DEPOSIT_OPEN, DEPOSIT_MOVE, HOTBAR_MOVE};
      }
   }

   enum AutoPottBotState3 {
      STRENGTH("Сила", Items.BLAZE_POWDER, Items.GLOWSTONE_DUST, Potions.STRENGTH, Potions.STRONG_STRENGTH, null, null, 14042437),
      SWIFTNESS("Скорость", Items.SUGAR, Items.GLOWSTONE_DUST, Potions.SWIFTNESS, Potions.STRONG_SWIFTNESS, null, null, 5227511),
      FIRE_RESISTANCE("Огнестойкость", Items.MAGMA_CREAM, Items.REDSTONE, Potions.FIRE_RESISTANCE, Potions.LONG_FIRE_RESISTANCE, null, null, 16750592),
      INVISIBILITY(
         "Невидимость",
         Items.GOLDEN_CARROT,
         Items.REDSTONE,
         Potions.INVISIBILITY,
         Potions.LONG_INVISIBILITY,
         Items.FERMENTED_SPIDER_EYE,
         Potions.NIGHT_VISION,
         9919936
      );

      final String text;
      final Item item;
      final Item item2;
      final Holder<Potion> registryEntry;
      final Holder<Potion> registryEntry2;
      final Item item3;
      final Holder<Potion> registryEntry3;
      final int intValue;

      AutoPottBotState3(
         String string2, Item item, Item item2, Holder<Potion> registryEntry, Holder<Potion> registryEntry2, Item item3, Holder<Potion> registryEntry3, int j
      ) {
         this.text = string2;
         this.item = item;
         this.item2 = item2;
         this.registryEntry = registryEntry;
         this.registryEntry2 = registryEntry2;
         this.item3 = item3;
         this.registryEntry3 = registryEntry3;
         this.intValue = j;
      }

      // $VF: synthetic method
      private static AutoPottBotFeature.AutoPottBotState3[] $values() {
         return new AutoPottBotFeature.AutoPottBotState3[]{STRENGTH, SWIFTNESS, FIRE_RESISTANCE, INVISIBILITY};
      }
   }

   enum AutoPottBotState4 {
      UNKNOWN,
      EMPTY,
      WATER,
      AWKWARD,
      BASE,
      FINAL,
      OTHER;

      // $VF: synthetic method
      private static AutoPottBotFeature.AutoPottBotState4[] $values() {
         return new AutoPottBotFeature.AutoPottBotState4[]{UNKNOWN, EMPTY, WATER, AWKWARD, BASE, FINAL, OTHER};
      }
   }

   static final class AutoPottBotState5 {
      final BlockPos blockPos;
      AutoPottBotFeature.AutoPottBotState3 autoPottBotState3;
      AutoPottBotFeature.AutoPottBotState4 autoPottBotState4 = AutoPottBotFeature.AutoPottBotState4.UNKNOWN;
      boolean flag;
      int intValue;
      boolean flag2;
      long timestamp;
      long timestamp2;
      long timestamp3;
      long timestamp4 = System.currentTimeMillis();

      AutoPottBotState5(BlockPos blockPos) {
         this.blockPos = blockPos;
      }

      float measure(long l) {
         if (this.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.FINAL) {
            return 1.0F;
         } else if (this.flag2 && this.timestamp2 > this.timestamp) {
            float floatValue3 = (float)(l - this.timestamp) / (float)(this.timestamp2 - this.timestamp);
            return floatValue3 < 0.0F ? 0.0F : Math.min(floatValue3, 1.0F);
         } else {
            return 0.0F;
         }
      }

      int compute() {
         if (this.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.FINAL) {
            return 5954680;
         } else {
            return this.autoPottBotState3 != null ? this.autoPottBotState3.intValue : 9868960;
         }
      }

      String resolve() {
         return this.autoPottBotState3 != null ? this.autoPottBotState3.text : "—";
      }

      String resolve2(long l) {
         if (this.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.FINAL) {
            return this.resolve() + " ✓";
         } else if (this.flag2 && l < this.timestamp2) {
            return this.resolve() + " " + (int)(this.measure(l) * 100.0F) + "%";
         } else if (this.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.EMPTY) {
            return "Свободна";
         } else {
            return this.autoPottBotState4 == AutoPottBotFeature.AutoPottBotState4.UNKNOWN ? "…" : this.resolve() + " готова";
         }
      }
   }
}

