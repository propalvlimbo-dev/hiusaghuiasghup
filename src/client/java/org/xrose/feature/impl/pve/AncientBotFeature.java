package org.xrose.feature.impl.pve;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.xrose.command.impl.RctCommand;
import org.xrose.context.MinecraftContext;
import org.xrose.context.RotationContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.pve.AutomationPriority;
import org.xrose.pve.AutomationResource;
import org.xrose.pve.PveFeature;
import org.xrose.pve.server.ServerProfile;
import org.xrose.utils.text.ChatUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AncientBotFeature extends PveFeature implements MinecraftContext {
   private static final Pattern ANARCHY_PATTERN = Pattern.compile("анархия-(\\d+)");
   private static final Pattern GRIEF_PATTERN = Pattern.compile("гриф-(\\d+)");
   private static final float ROTATION_STEP = 5.0F;
   private static final float AIM_TOLERANCE = 1.0F;
   private final List<BlockPos> ores = Collections.synchronizedList(new ArrayList<>());
   private AncientBotFeature.State state = AncientBotFeature.State.NONE;
   private volatile BlockPos target;
   private int potionSlot = -1;
   private int foodSlot = -1;
   private boolean tntPlaced;
   private Thread scanThread;
   private AncientBotFeature.BaritoneSettingsSnapshot baritoneSnapshot;

   public AncientBotFeature() {
      super(
         "AncientBot",
         "Automatically mines ancient debris in the nether anarchy",
         -1,
         AutomationPriority.BOT,
         AutomationResource.MOVEMENT,
         AutomationResource.ROTATION,
         AutomationResource.NAVIGATION,
         AutomationResource.INVENTORY,
         AutomationResource.CHAT
      );
   }

   @Override
   protected void onPveEnable() {
      if (baritone() == null) {
         throw new IllegalStateException("Baritone is unavailable");
      }

      Settings settings = BaritoneAPI.getSettings();
      this.baritoneSnapshot = AncientBotFeature.BaritoneSettingsSnapshot.capture(settings);
      settings.allowBreak.value = true;
      settings.allowPlace.value = true;
      settings.assumeWalkOnLava.value = true;
      settings.blockReachDistance.value = 4.0F;
      settings.avoidance.value = true;
      settings.mobAvoidanceCoefficient.value = 2.0;
      settings.mobAvoidanceRadius.value = 12;
      settings.assumeSafeWalk.value = false;
      settings.allowParkour.value = true;
      settings.allowParkourPlace.value = true;
      baritone().getCommandManager().execute("resume");
      this.reset();
      StringBuilder missing = new StringBuilder();
      this.checkRequiredItems(missing);
      if (missing.length() > 0) {
         ChatUtil.info("Рекомендуемые предметы для AncientBot:\n" + missing);
      }
   }

   @Override
   protected void onPveDisable() {
      mc.options.keyUse.setDown(false);
      if (baritone() != null) {
         if (this.potionSlot != -1 || this.foodSlot != -1) {
            baritone().getCommandManager().execute("resume");
            if (this.potionSlot != -1) {
               this.player().getInventory().setSelectedSlot(this.potionSlot);
            }

            if (this.foodSlot != -1) {
               this.player().getInventory().setSelectedSlot(this.foodSlot);
            }
         }

         this.reset();
         if (this.baritoneSnapshot != null) {
            this.baritoneSnapshot.restore(BaritoneAPI.getSettings());
            this.baritoneSnapshot = null;
         }
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.player() != null && this.level() != null) {
         this.pruneOres();
         if (this.handleCheckItems()) {
            if (this.target == null) {
               if (this.scanThread == null || !this.scanThread.isAlive()) {
                  this.scanThread = new Thread(this::scanForTarget, "AncientBot-Scanner");
                  this.scanThread.setDaemon(true);
                  this.scanThread.start();
               }
            } else if (this.state == AncientBotFeature.State.NONE) {
               this.startPathing();
            } else if (this.state == AncientBotFeature.State.PATHING) {
               this.tickPathing();
            } else if (this.state == AncientBotFeature.State.PLACE_TNT) {
               this.tickPlaceTnt();
            } else if (this.state == AncientBotFeature.State.ACTIVATE) {
               this.tickActivate();
            } else if (this.state == AncientBotFeature.State.FARM) {
               this.tickFarm();
            }
         }
      }
   }

   @EventTarget
   public void onPacket(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.POST) {
         if (event.getPacket() instanceof ClientboundSectionBlocksUpdatePacket packet) {
            if (this.level() != null && this.level().dimension() == Level.NETHER) {
               packet.runUpdates((pos, blockState) -> {
                  if (blockState.is(Blocks.ANCIENT_DEBRIS)) {
                     BlockPos immutable = pos.immutable();
                     if (!this.ores.contains(immutable)) {
                        this.ores.add(immutable);
                     }
                  }
               });
            }
         }
      }
   }

   private void pruneOres() {
      if (this.getAnarchy() != -1) {
         synchronized (this.ores) {
            this.ores
               .removeIf(
                  pos -> this.level().getBlockState(pos).is(Blocks.AIR)
                     || pos.distSqr(this.player().blockPosition()) >= 6400.0
                     || !this.level().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
               );
         }
      }
   }

   private boolean handleCheckItems() {
      StringBuilder missing = new StringBuilder();
      boolean ok = this.checkRequiredItems(missing);
      if (!ok && this.getAnarchy() != -1 && this.state != AncientBotFeature.State.FARM) {
         ChatUtil.info("Для работы модуля необходимы предметы:\n" + missing);
      }

      MobEffectInstance fireResistance = this.player().getEffect(MobEffects.FIRE_RESISTANCE);
      if (fireResistance == null || fireResistance.getDuration() <= 0) {
         int slot = this.findPotionEffectHotbarSlot(MobEffects.FIRE_RESISTANCE);
         if (slot != -1) {
            if (this.potionSlot == -1) {
               baritone().getCommandManager().execute("pause");
               this.potionSlot = this.player().getInventory().getSelectedSlot();
            }

            this.player().getInventory().setSelectedSlot(slot);
            mc.options.keyUse.setDown(true);
            return false;
         }
      } else if (mc.options.keyUse.isDown() && this.potionSlot != -1) {
         baritone().getCommandManager().execute("resume");
         mc.options.keyUse.setDown(false);
         this.player().getInventory().setSelectedSlot(this.potionSlot);
         this.potionSlot = -1;
      }

      boolean needFood = this.player().getFoodData().getFoodLevel() <= 14
         || this.player().getFoodData().getFoodLevel() <= 18 && this.player().getHealth() <= this.player().getMaxHealth() / 1.5F;
      if (needFood) {
         if (this.foodSlot == -1) {
            baritone().getCommandManager().execute("pause");
            this.foodSlot = this.player().getInventory().getSelectedSlot();
         }

         this.player().getInventory().setSelectedSlot(this.getBestFoodHotbarSlot());
         mc.options.keyUse.setDown(true);
         return false;
      } else {
         if (mc.options.keyUse.isDown() && this.foodSlot != -1) {
            baritone().getCommandManager().execute("resume");
            mc.options.keyUse.setDown(false);
            this.player().getInventory().setSelectedSlot(this.foodSlot);
            this.foodSlot = -1;
         }

         return true;
      }
   }

   private boolean checkRequiredItems(StringBuilder missing) {
      boolean ok = true;
      if (this.getAnarchy() != -1) {
         if (!this.hasAncientBot()) {
            missing.append("- Кирка с зачарованием Магнит в хотбаре\n");
            ok = false;
         }

         if (!this.hasPotionEffectInHotbar(MobEffects.FIRE_RESISTANCE)) {
            missing.append("- Зелье Огнестойкости в хотбаре\n");
            ok = false;
         }

         if (!this.hasAnyFoodInHotbar()) {
            missing.append("- Любая еда в хотбаре\n");
            ok = false;
         }

         if (!this.hasItemInHotbar(Items.FLINT_AND_STEEL)) {
            missing.append("- Огниво в хотбаре\n");
            ok = false;
         }

         if (!this.hasItemInHotbar(Items.TNT) && !this.tntPlaced) {
            missing.append("- Динамит (вайты) в хотбаре\n");
            ok = false;
         }

         ItemStack boots = this.player().getItemBySlot(EquipmentSlot.FEET);
         boolean hasFeatherFalling = !boots.isEmpty() && boots.getEnchantments().getLevel(this.featherFallingHolder()) > 0;
         if (!hasFeatherFalling) {
            missing.append("- Одеты ботинки с зачарованием Невесомость\n");
            ok = false;
         }

         if (!this.hasGoldArmor()) {
            missing.append("- Любой элемент золотой брони\n");
            ok = false;
         }
      }

      return ok;
   }

   private boolean hasAncientBot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.player().getInventory().getItem(i);
         if (!stack.isEmpty() && stack.is(ItemTags.PICKAXES) && this.tooltipContains(stack, "магнит")) {
            return true;
         }
      }

      return false;
   }

   private boolean tooltipContains(ItemStack stack, String requirement) {
      List<Component> lines = stack.getTooltipLines(TooltipContext.of(this.level()), this.player(), TooltipFlag.NORMAL);
      StringBuilder joined = new StringBuilder();

      for (int i = 1; i < lines.size(); i++) {
         joined.append(normalizeTooltip(lines.get(i).getString())).append(' ');
      }

      String wanted = normalizeTooltip(requirement);
      return joined.toString().contains(wanted);
   }

   private static String normalizeTooltip(String text) {
      return text.replaceAll("§.", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
   }

   private boolean hasAnyFoodInHotbar() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.player().getInventory().getItem(i);
         if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
            return true;
         }
      }

      return false;
   }

   private int getBestFoodHotbarSlot() {
      int best = -1;
      int bestCount = 0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.player().getInventory().getItem(i);
         if (!stack.isEmpty() && stack.has(DataComponents.FOOD) && stack.getItem() != Items.CHORUS_FRUIT && stack.getCount() > bestCount) {
            best = i;
            bestCount = stack.getCount();
         }
      }

      return best;
   }

   private boolean hasItemInHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.player().getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return true;
         }
      }

      return false;
   }

   private boolean hasPotionEffectInHotbar(Holder<MobEffect> effect) {
      return this.findPotionEffectHotbarSlot(effect) != -1;
   }

   private int findPotionEffectHotbarSlot(Holder<MobEffect> effect) {
      for (int i = 0; i < 9; i++) {
         if (this.isPotionWithEffect(this.player().getInventory().getItem(i), effect)) {
            return i;
         }
      }

      return -1;
   }

   private boolean isPotionWithEffect(ItemStack stack, Holder<MobEffect> effect) {
      if (stack.isEmpty()) {
         return false;
      }

      Item item = stack.getItem();
      if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) {
         return false;
      }

      PotionContents contents = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
      if (contents == null) {
         return false;
      }

      for (MobEffectInstance instance : contents.getAllEffects()) {
         if (instance.getEffect() == effect) {
            return true;
         }
      }

      return false;
   }

   private boolean hasGoldArmor() {
      for (EquipmentSlot slot : List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.BODY)) {
         Item item = this.player().getItemBySlot(slot).getItem();
         if (item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS) {
            return true;
         }
      }

      return false;
   }

   private Holder<Enchantment> featherFallingHolder() {
      return this.player().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FEATHER_FALLING);
   }

   private int getItemSlot(Item item, boolean hotbar) {
      int max = hotbar ? 9 : 45;

      for (int i = 0; i < max; i++) {
         if (this.player().getInventory().getItem(i).getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private void startPathing() {
      if (this.target != null) {
         baritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(this.target));
         this.state = AncientBotFeature.State.PATHING;
      }
   }

   private void tickPathing() {
      if (this.arrived()) {
         this.logDebug("Пришли к региону, устанавливаем динамит.");
         baritone().getPathingBehavior().cancelEverything();
         this.state = AncientBotFeature.State.PLACE_TNT;
      } else {
         if (!this.ores.isEmpty()) {
            this.pruneOres();
            List<BlockPos> validOres = this.filterValidDebris(this.ores);
            if (!validOres.isEmpty() && !baritone().getMineProcess().isActive()) {
               BlockPos closest = this.findClosestValidDebris(validOres);
               if (closest != null) {
                  baritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(closest));
               }
            }
         } else if (!baritone().getCustomGoalProcess().isActive()) {
            this.startPathing();
         }
      }
   }

   private boolean arrived() {
      return this.target == null ? false : this.player().blockPosition().distToCenterSqr(Vec3.atCenterOf(this.target)) <= 2.25;
   }

   private void tickPlaceTnt() {
      if (!this.tntPlaced) {
         this.placeTnt();
      }

      for (Entity entity : this.level().entitiesForRendering()) {
         if (entity instanceof PrimedTnt tnt) {
            float fuse = tnt.getFuse() / 20.0F;
            if (this.tntPlaced && fuse <= 2.7F) {
               RctCommand.requestRejoin(mc);
               this.logDebug("Выполняем переподключение на анархию.");
               this.ores.clear();
               this.player().tickCount = 0;
               this.state = AncientBotFeature.State.ACTIVATE;
               break;
            }
         }
      }
   }

   private void tickActivate() {
      if (this.level().dimension() == Level.NETHER && this.getAnarchy() != -1) {
         boolean hasTnt = false;

         for (Entity entity : this.level().entitiesForRendering()) {
            if (entity instanceof PrimedTnt) {
               hasTnt = true;
               break;
            }
         }

         if (!hasTnt) {
            this.scanSurroundingDebris();
            this.state = AncientBotFeature.State.FARM;
         }
      }
   }

   private void scanSurroundingDebris() {
      if (this.level() != null && this.level().dimension() == Level.NETHER) {
         BlockPos center = this.player().blockPosition();
         int radius = 24;
         int foundCount = 0;

         for (int dy = -10; dy <= 6; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  BlockPos pos = center.offset(dx, dy, dz);
                  if (this.level().isLoaded(pos) && this.level().getBlockState(pos).is(Blocks.ANCIENT_DEBRIS)) {
                     BlockPos immutable = pos.immutable();
                     synchronized (this.ores) {
                        if (!this.ores.contains(immutable)) {
                           this.ores.add(immutable);
                           foundCount++;
                        }
                     }
                  }
               }
            }
         }

         if (!this.ores.isEmpty()) {
            this.pruneOres();
            List<BlockPos> validOres = this.filterValidDebris(this.ores);
            this.logDebug("Найдено " + this.ores.size() + " обломков рядом после переподключения.");
            ChatUtil.info("Обнаружено древних обломков: " + validOres.size() + " валидных из " + this.ores.size() + " всего");
         }
      }
   }

   private void tickFarm() {
      if (!this.ores.isEmpty()) {
         this.pruneOres();
         List<BlockPos> validOres = this.filterValidDebris(this.ores);
         if (!validOres.isEmpty() && this.player().onGround()) {
            BlockPos closest = this.findClosestValidDebris(validOres);
            if (closest != null && !baritone().getCustomGoalProcess().isActive()) {
               baritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(closest));
            }
         } else if (validOres.isEmpty()) {
            this.ores.clear();
            if (this.getAnarchy() != -1) {
               this.logDebug("Все обломки выкопаны, начинаем поиск новой территории.");
               this.reset();
            }
         }
      } else if (this.getAnarchy() != -1) {
         this.logDebug("Копание завершено, начинаем поиск новой территории.");
         this.reset();
      }
   }

   private void placeTnt() {
      BlockPos placePos = this.findPlacePos(this.target);
      if (placePos == null) {
         this.logDebug("Не удалось найти подходящее место для TNT");
      } else {
         int tntSlot = this.getItemSlot(Items.TNT, true);
         int flintSlot = this.getItemSlot(Items.FLINT_AND_STEEL, true);
         if (tntSlot == -1) {
            this.logDebug("TNT не найден в хотбаре");
         } else if (flintSlot == -1) {
            this.logDebug("Огниво не найдено в хотбаре");
         } else if (!this.canPlaceTntAt(placePos)) {
            this.logDebug("Позиция невалидна для размещения TNT: " + placePos);
         } else {
            BlockHitResult placeHit = this.getRotationPlaceBlock(placePos);
            if (placeHit == null) {
               this.logDebug("Не удалось найти поверхность для размещения TNT");
            } else {
               Vec3 eye = this.player().getEyePosition(1.0F);
               Vec3 vec = placeHit.getLocation().subtract(eye);
               float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
               float pitch = (float)Mth.wrapDegrees(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
               if (PveManagerFeature.INSTANCE.rotate.getValue()) {
                  float curYaw = this.currentYaw();
                  float curPitch = this.currentPitch();
                  float dYaw = Mth.wrapDegrees(yaw - curYaw);
                  float dPitch = pitch - curPitch;
                  float diff = (float)Math.hypot(dYaw, dPitch);
                  if (diff > 1.0F) {
                     this.smoothRotateTo(yaw, pitch);
                     return;
                  }

                  BlockHitResult rayHit = this.rayTraceTowards(this.player(), this.currentYaw(), this.currentPitch(), this.player().blockInteractionRange());
                  if (rayHit.getType() != Type.BLOCK
                     || !rayHit.getBlockPos().equals(placeHit.getBlockPos())
                     || rayHit.getDirection() != placeHit.getDirection()) {
                     this.logDebug("Raycast не совпадает с ожидаемой позицией");
                     return;
                  }
               } else {
                  RotationContext.clear();
               }

               int prevSlot = this.player().getInventory().getSelectedSlot();
               if (prevSlot != tntSlot) {
                  this.player().getInventory().setSelectedSlot(tntSlot);
                  this.logDebug("Выбран TNT слот: " + tntSlot);

                  try {
                     Thread.sleep(50L);
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                  }
               }

               InteractionResult result = this.gameMode().useItemOn(this.player(), InteractionHand.MAIN_HAND, placeHit);
               if (result != InteractionResult.SUCCESS) {
                  this.logDebug("Не удалось разместить TNT, результат: " + result);
               } else {
                  this.player().swing(InteractionHand.MAIN_HAND);
                  this.logDebug("TNT размещен на " + placePos);

                  try {
                     Thread.sleep(150L);
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                  }

                  if (!this.level().getBlockState(placePos).is(Blocks.TNT)) {
                     this.logDebug("TNT не появился после размещения");
                  } else {
                     if (this.player().getInventory().getSelectedSlot() != flintSlot) {
                        this.player().getInventory().setSelectedSlot(flintSlot);
                        this.logDebug("Выбрано огниво слот: " + flintSlot);

                        try {
                           Thread.sleep(50L);
                        } catch (InterruptedException e) {
                           Thread.currentThread().interrupt();
                        }
                     }

                     Vec3 tntCenter = Vec3.atCenterOf(placePos);
                     BlockHitResult igniteHit = this.rayTraceTowards(
                        this.player(),
                        (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(tntCenter.z - eye.z, tntCenter.x - eye.x)) - 90.0),
                        (float)Mth.wrapDegrees(-Math.toDegrees(Math.atan2(tntCenter.y - eye.y, Math.hypot(tntCenter.x - eye.x, tntCenter.z - eye.z)))),
                        this.player().blockInteractionRange()
                     );
                     if (igniteHit.getType() != Type.BLOCK || !igniteHit.getBlockPos().equals(placePos)) {
                        igniteHit = new BlockHitResult(tntCenter, Direction.UP, placePos, false);
                     }

                     InteractionResult igniteResult = this.gameMode().useItemOn(this.player(), InteractionHand.MAIN_HAND, igniteHit);
                     if (igniteResult == InteractionResult.SUCCESS) {
                        this.player().swing(InteractionHand.MAIN_HAND);
                        this.logDebug("TNT подожжен");
                        this.tntPlaced = true;
                     } else {
                        this.logDebug("Не удалось поджечь TNT, результат: " + igniteResult);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canPlaceTntAt(BlockPos pos) {
      BlockPos below = pos.below();
      if (this.level().isLoaded(below) && this.level().isLoaded(pos) && this.level().isLoaded(pos.above())) {
         BlockState belowState = this.level().getBlockState(below);
         BlockState currentState = this.level().getBlockState(pos);
         BlockState aboveState = this.level().getBlockState(pos.above());
         boolean solidBelow = !belowState.canBeReplaced() && belowState.getFluidState().isEmpty();
         boolean currentOk = currentState.canBeReplaced() && currentState.getFluidState().isEmpty();
         boolean aboveOk = aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty();
         return solidBelow && currentOk && aboveOk;
      } else {
         return false;
      }
   }

   private BlockPos findPlacePos(BlockPos origin) {
      BlockPos best = this.findPlacePos(origin, false);
      if (best == null) {
         best = this.findPlacePos(origin, true);
      }

      return best;
   }

   private BlockPos findPlacePos(BlockPos origin, boolean allowPlayerIntersection) {
      BlockPos best = null;
      double bestDistSqr = Double.MAX_VALUE;
      BlockPos playerPos = this.player().blockPosition();

      for (int dy = -1; dy <= 2; dy++) {
         for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
               BlockPos candidate = playerPos.offset(dx, dy, dz);
               if (this.level().isLoaded(candidate)
                  && this.isPlaceable(candidate, allowPlayerIntersection)
                  && this.hasDenseGroundBelow(candidate)
                  && this.getRotationPlaceBlock(candidate) != null) {
                  double distSqr = candidate.distSqr(playerPos);
                  if (distSqr < bestDistSqr) {
                     bestDistSqr = distSqr;
                     best = candidate;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean isPlaceable(BlockPos pos, boolean allowPlayerIntersection) {
      if (!this.level().isLoaded(pos)) {
         return false;
      }

      BlockPos below = pos.below();
      BlockPos above = pos.above();
      if (this.level().isLoaded(below) && this.level().isLoaded(above)) {
         BlockState belowState = this.level().getBlockState(below);
         boolean solidBelow = !belowState.canBeReplaced() && belowState.getFluidState().isEmpty();
         if (!solidBelow) {
            return false;
         }

         BlockState currentState = this.level().getBlockState(pos);
         BlockState aboveState = this.level().getBlockState(above);
         boolean currentOk = currentState.canBeReplaced() && currentState.getFluidState().isEmpty();
         boolean aboveOk = aboveState.canBeReplaced() && aboveState.getFluidState().isEmpty();
         return currentOk && aboveOk ? allowPlayerIntersection || !this.player().getBoundingBox().intersects(new AABB(pos)) : false;
      } else {
         return false;
      }
   }

   private boolean hasDenseGroundBelow(BlockPos pos) {
      int solidCount = 0;
      int total = 0;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            BlockPos below = pos.offset(dx, -1, dz);
            if (this.level().isLoaded(below)) {
               total++;
               BlockState state = this.level().getBlockState(below);
               boolean nonSolid = state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty() || state.is(Blocks.CAVE_AIR);
               if (!nonSolid) {
                  solidCount++;
               }
            }
         }
      }

      return total > 0 && solidCount >= 5;
   }

   private BlockHitResult getRotationPlaceBlock(BlockPos pos) {
      if (pos != null && this.player() != null && this.level() != null) {
         Direction[] directions = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};
         double reach = this.player().blockInteractionRange();
         Vec3 eye = this.player().getEyePosition(1.0F);

         for (Direction dir : directions) {
            BlockPos neighbor = pos.relative(dir.getOpposite());
            if (this.level().isLoaded(neighbor) && !this.level().getBlockState(neighbor).isAir() && !this.level().getBlockState(neighbor).canBeReplaced()) {
               Vec3 neighborCenter = Vec3.atCenterOf(neighbor);

               Vec3 offset = switch (dir) {
                  case UP -> new Vec3(0.0, 0.5, 0.0);
                  case DOWN -> new Vec3(0.0, -0.5, 0.0);
                  case NORTH -> new Vec3(0.0, 0.0, -0.5);
                  case SOUTH -> new Vec3(0.0, 0.0, 0.5);
                  case WEST -> new Vec3(-0.5, 0.0, 0.0);
                  case EAST -> new Vec3(0.5, 0.0, 0.0);
                  default -> throw new MatchException(null, null);
               };
               Vec3 aim = neighborCenter.add(offset);
               Vec3 toAim = aim.subtract(eye);
               float yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(toAim.z, toAim.x)) - 90.0);
               float pitch = (float)Mth.wrapDegrees(-Math.toDegrees(Math.atan2(toAim.y, Math.hypot(toAim.x, toAim.z))));
               BlockHitResult ray = this.rayTraceTowards(this.player(), yaw, pitch, reach);
               if (ray.getType() == Type.BLOCK && neighbor.equals(ray.getBlockPos()) && ray.getDirection() == dir) {
                  return new BlockHitResult(aim, dir, neighbor, false);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private BlockHitResult rayTraceTowards(Entity entity, float yaw, float pitch, double range) {
      Vec3 eye = entity.getEyePosition(1.0F);
      Vec3 dir = getVectorForRotation(pitch, yaw);
      Vec3 end = eye.add(dir.x * range, dir.y * range, dir.z * range);
      return this.level().clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, entity));
   }

   private float currentYaw() {
      return RotationContext.isActive() ? RotationContext.getServerYaw() : this.player().getYRot();
   }

   private float currentPitch() {
      return RotationContext.isActive() ? RotationContext.getServerPitch() : this.player().getXRot();
   }

   private float getAngularDistance(float yaw1, float pitch1, float yaw2, float pitch2) {
      float dYaw = Mth.wrapDegrees(yaw2 - yaw1);
      float dPitch = pitch2 - pitch1;
      return (float)Math.hypot(dYaw, dPitch);
   }

   private void smoothRotateTo(float targetYaw, float targetPitch) {
      float curYaw = this.currentYaw();
      float curPitch = this.currentPitch();
      float dYaw = Mth.wrapDegrees(targetYaw - curYaw);
      float dPitch = targetPitch - curPitch;
      float angleDiff = (float)Math.hypot(dYaw, dPitch);
      if (!(angleDiff <= 1.0F)) {
         float rotationSpeed = Math.max(34.0F, Math.min(140.0F, angleDiff * 1.35F));
         float factor = Math.min(1.0F, rotationSpeed / angleDiff);
         float actualYawDelta = dYaw * factor;
         float actualPitchDelta = dPitch * factor;
         RotationContext.setRotation(curYaw + actualYawDelta, Mth.clamp(curPitch + actualPitchDelta, -90.0F, 90.0F));
      }
   }

   private static Vec3 getVectorForRotation(float pitch, float yaw) {
      float f = -pitch * (float) (Math.PI / 180.0) - (float) Math.PI;
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = Mth.cos(f);
      float i = Mth.sin(f);
      float j = -Mth.cos(g);
      float k = Mth.sin(g);
      return new Vec3(i * j, k, h * j);
   }

   private List<BlockPos> filterValidDebris(List<BlockPos> debris) {
      List<BlockPos> valid = new ArrayList<>();
      synchronized (debris) {
         for (BlockPos pos : debris) {
            if (this.isValidDebris(pos)) {
               valid.add(pos);
            }
         }

         return valid;
      }
   }

   private BlockPos findClosestValidDebris(List<BlockPos> validDebris) {
      if (validDebris.isEmpty()) {
         return null;
      }

      BlockPos playerPos = this.player().blockPosition();
      BlockPos closest = null;
      double minDistSq = Double.MAX_VALUE;

      for (BlockPos pos : validDebris) {
         double distSq = playerPos.distSqr(pos);
         if (distSq < minDistSq) {
            minDistSq = distSq;
            closest = pos;
         }
      }

      return closest;
   }

   private boolean isValidDebris(BlockPos pos) {
      return this.level() != null && this.level().getBlockState(pos).is(Blocks.ANCIENT_DEBRIS)
         ? this.hasAirAround(pos) && !this.hasTooMuchQuartzGold(pos) && this.hasEnoughAirInCube(pos) && !this.hasTooManyDebrisNearby(pos)
         : false;
   }

   private boolean hasAirAround(BlockPos pos) {
      int count = 0;

      for (Direction dir : Direction.values()) {
         BlockState state = this.level().getBlockState(pos.relative(dir));
         if (state.isAir() || state.is(Blocks.LAVA) || state.is(Blocks.CAVE_AIR)) {
            if (++count >= 2) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean hasTooMuchQuartzGold(BlockPos pos) {
      int count = 0;

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               BlockState state = this.level().getBlockState(pos.offset(x, y, z));
               if (state.is(Blocks.NETHER_QUARTZ_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) {
                  if (++count >= 4) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean hasEnoughAirInCube(BlockPos pos) {
      int count = 0;

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               BlockState state = this.level().getBlockState(pos.offset(x, y, z));
               if (state.isAir() || state.is(Blocks.LAVA) || state.is(Blocks.CAVE_AIR)) {
                  count++;
               }
            }
         }
      }

      return count >= 4;
   }

   private boolean hasTooManyDebrisNearby(BlockPos pos) {
      int count = 0;

      for (int x = -3; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 3; z++) {
               if (this.level().getBlockState(pos.offset(x, y, z)).is(Blocks.ANCIENT_DEBRIS)) {
                  if (++count > 6) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private void scanForTarget() {
      BlockPos playerPos = this.player().blockPosition();
      int bestDepth = 0;
      BlockPos bestPos = null;
      double bestDist = Double.MAX_VALUE;

      for (int y = 15; y <= 60; y++) {
         for (int x = playerPos.getX() - 200; x <= playerPos.getX() + 200; x += 8) {
            for (int z = playerPos.getZ() - 200; z <= playerPos.getZ() + 200; z += 8) {
               if (x != playerPos.getX() || z != playerPos.getZ()) {
                  BlockPos column = new BlockPos(x, y, z);
                  if (this.level().isLoaded(column)
                     && this.level().getWorldBorder().isWithinBounds(column)
                     && !this.level().getBiome(column).is(Biomes.BASALT_DELTAS)) {
                     int blastCheckRadius = 20;
                     int totalBlocks = 0;
                     int airBlocks = 0;
                     int solidBlocks = 0;

                     for (int dx = -blastCheckRadius; dx <= blastCheckRadius; dx++) {
                        for (int dy = -10; dy <= 10; dy++) {
                           for (int dz = -blastCheckRadius; dz <= blastCheckRadius; dz++) {
                              BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                              if (this.level().isLoaded(p)) {
                                 totalBlocks++;
                                 BlockState blockState = this.level().getBlockState(p);
                                 if (blockState.isAir() || blockState.is(Blocks.CAVE_AIR)) {
                                    airBlocks++;
                                 } else if (blockState.getFluidState().isEmpty() && !blockState.canBeReplaced()) {
                                    solidBlocks++;
                                 }
                              }
                           }
                        }
                     }

                     if (totalBlocks > 0) {
                        double airRatio = (double)airBlocks / totalBlocks;
                        double solidRatio = (double)solidBlocks / totalBlocks;
                        if (airRatio > 0.3 || solidRatio < 0.6) {
                           continue;
                        }
                     }

                     int counted = 0;
                     int solid = 0;
                     int[][][] air = new int[20][20][4];

                     for (int dx = -10; dx <= 9; dx++) {
                        for (int dy = -2; dy <= 1; dy++) {
                           for (int dz = -10; dz <= 9; dz++) {
                              if (dx != 0 || dy != 0 || dz != 0) {
                                 BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                                 if (this.level().isLoaded(p)) {
                                    counted++;
                                    BlockState blockState = this.level().getBlockState(p);
                                    boolean nonSolid = blockState.isAir()
                                       || blockState.canBeReplaced()
                                       || !blockState.getFluidState().isEmpty()
                                       || blockState.is(Blocks.CAVE_AIR);
                                    if (!nonSolid) {
                                       solid++;
                                    }

                                    int ax = dx + 10;
                                    int az = dz + 10;
                                    int ay = dy + 2;
                                    if (ax >= 0 && ax < 20 && az >= 0 && az < 20 && ay >= 0 && ay < 4 && nonSolid) {
                                       air[ax][az][ay] = 1;
                                    }
                                 }
                              }
                           }
                        }
                     }

                     double density = counted == 0 ? 0.0 : (double)solid / counted;
                     if (!(density < 0.85)) {
                        boolean hasAirPocket = false;

                        label238:
                        for (int ay = 0; ay < 4; ay++) {
                           for (int ax = 0; ax <= 12; ax++) {
                              for (int az = 0; az <= 12; az++) {
                                 int airCount = 0;

                                 for (int wx = ax; wx < ax + 7 && wx < 20; wx++) {
                                    for (int wz = az; wz < az + 7 && wz < 20; wz++) {
                                       if (air[wx][wz][ay] == 1) {
                                          airCount++;
                                       }
                                    }
                                 }

                                 if (airCount >= 35) {
                                    hasAirPocket = true;
                                    break label238;
                                 }
                              }
                           }
                        }

                        if (!hasAirPocket) {
                           int depth = 0;

                           for (int d = 1; d <= 30; d++) {
                              BlockPos below = new BlockPos(x, y - d, z);
                              if (!this.level().isLoaded(below)) {
                                 break;
                              }

                              BlockState belowState = this.level().getBlockState(below);
                              if (belowState.isAir() || belowState.canBeReplaced() || !belowState.getFluidState().isEmpty() || belowState.is(Blocks.CAVE_AIR)) {
                                 break;
                              }

                              depth++;
                           }

                           if (depth >= 22) {
                              double dist = Math.sqrt(Math.pow(x - playerPos.getX(), 2.0) + Math.pow(z - playerPos.getZ(), 2.0));
                              if (depth > bestDepth || depth == bestDepth && dist < bestDist) {
                                 bestDepth = depth;
                                 bestPos = column;
                                 bestDist = dist;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (bestPos == null) {
         synchronized (this) {
            this.target = null;
         }

         ChatUtil.error("Не найдено подходящей территории, пропишите /rtp");
      } else {
         synchronized (this) {
            this.target = bestPos;
         }

         this.logDebug("Найдена территория для взрыва: " + bestPos.toShortString() + " (глубина: " + bestDepth + ")");
      }
   }

   private void reset() {
      if (baritone() != null) {
         baritone().getPathingBehavior().cancelEverything();
      }

      this.state = AncientBotFeature.State.NONE;
      this.potionSlot = -1;
      this.foodSlot = -1;
      this.tntPlaced = false;
      this.target = null;
      if (this.scanThread != null && this.scanThread.isAlive()) {
         this.scanThread.interrupt();
      }

      this.scanThread = null;
   }

   private void logDebug(String message) {
      if (PveManagerFeature.INSTANCE.debugLogging.getValue()) {
         ChatUtil.info(message);
      }
   }

   private int getAnarchy() {
      ServerProfile profile = PveManagerFeature.INSTANCE.resolveServerProfile(mc);
      if (profile != ServerProfile.FUNTIME && profile != ServerProfile.REALLYWORLD) {
         String header = RctCommand.tabHeader(mc);
         if (header.isEmpty()) {
            return -1;
         }

         Matcher anarchy = ANARCHY_PATTERN.matcher(header);
         if (anarchy.find()) {
            return Integer.parseInt(anarchy.group(1));
         }

         Matcher grief = GRIEF_PATTERN.matcher(header);
         return grief.find() ? Integer.parseInt(grief.group(1)) : -1;
      } else {
         return PveManagerFeature.INSTANCE.resolvedAnarchy();
      }
   }

   private static IBaritone baritone() {
      return BaritoneAPI.getProvider().getPrimaryBaritone();
   }

   private static final class BaritoneSettingsSnapshot {
      private final boolean allowBreak;
      private final boolean allowPlace;
      private final boolean assumeWalkOnLava;
      private final boolean avoidance;
      private final float blockReachDistance;
      private final double mobAvoidanceCoefficient;
      private final int mobAvoidanceRadius;

      private BaritoneSettingsSnapshot(Settings settings) {
         this.allowBreak = (Boolean)settings.allowBreak.value;
         this.allowPlace = (Boolean)settings.allowPlace.value;
         this.assumeWalkOnLava = (Boolean)settings.assumeWalkOnLava.value;
         this.avoidance = (Boolean)settings.avoidance.value;
         this.blockReachDistance = (Float)settings.blockReachDistance.value;
         this.mobAvoidanceCoefficient = (Double)settings.mobAvoidanceCoefficient.value;
         this.mobAvoidanceRadius = (Integer)settings.mobAvoidanceRadius.value;
      }

      private static AncientBotFeature.BaritoneSettingsSnapshot capture(Settings settings) {
         return new AncientBotFeature.BaritoneSettingsSnapshot(settings);
      }

      private void restore(Settings settings) {
         settings.allowBreak.value = this.allowBreak;
         settings.allowPlace.value = this.allowPlace;
         settings.assumeWalkOnLava.value = this.assumeWalkOnLava;
         settings.avoidance.value = this.avoidance;
         settings.blockReachDistance.value = this.blockReachDistance;
         settings.mobAvoidanceCoefficient.value = this.mobAvoidanceCoefficient;
         settings.mobAvoidanceRadius.value = this.mobAvoidanceRadius;
      }
   }

   private enum State {
      NONE,
      PATHING,
      PLACE_TNT,
      ACTIVATE,
      FARM;

      // $VF: synthetic method
      private static AncientBotFeature.State[] $values() {
         return new AncientBotFeature.State[]{NONE, PATHING, PLACE_TNT, ACTIVATE, FARM};
      }
   }
}

