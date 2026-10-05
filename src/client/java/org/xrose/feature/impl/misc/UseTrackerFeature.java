package org.xrose.feature.impl.misc;

import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.impl.combat.AuraFeature;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ModeSetting;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.hud.NotificationsElement;
import org.xrose.utils.FriendManager;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class UseTrackerFeature extends Feature {
   private static final double MAX_CONSUME_TRACK_DISTANCE_SQ = 4096.0;
   private static final long TOTEM_DEDUP_MS = 500L;
   private final BooleanSetting trackTotem = this.register(new BooleanSetting("Снос тотема", true));
   private final BooleanSetting trackPotions = this.register(new BooleanSetting("Полученные зелья", true));
   private final BooleanSetting trackConsume = this.register(new BooleanSetting("Съеденный предмет", true));
   private final NumberSetting radius = this.register(new NumberSetting("Радиус зелий", 100.0, 10.0, 100.0, 1.0, ""));
   private final MultiSelectSetting outputSetting = this.register(new MultiSelectSetting("Вывод", List.of("Чат"), "Чат", "Уведомления"));
   private final ModeSetting targetSetting = this.register(new ModeSetting("Цель", "Все", "Все", "Текущая цель"));
   private final Map<Integer, UseTrackerFeature.PotionData> trackedPotions = new HashMap<>();
   private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
   private final Map<UUID, Integer> useStartTick = new HashMap<>();
   private final Map<UUID, Long> lastTotemPopTime = new HashMap<>();

   public UseTrackerFeature() {
      super("UseTracker", "Tracks player totems, potions and consumed items", FeatureCategory.MISC, -1);
      this.radius.visibleWhen(this.trackPotions::getValue);
      this.outputSetting.visibleWhen(() -> this.trackTotem.getValue() || this.trackPotions.getValue() || this.trackConsume.getValue());
      this.targetSetting.visibleWhen(() -> this.trackTotem.getValue() || this.trackPotions.getValue() || this.trackConsume.getValue());
   }

   @Override
   protected void onDisable() {
      this.trackedPotions.clear();
      this.activeUseItem.clear();
      this.useStartTick.clear();
      this.lastTotemPopTime.clear();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client.player != null && client.level != null) {
         if (this.trackConsume.getValue()) {
            this.trackConsumedItems(client);
         }

         if (this.trackPotions.getValue()) {
            this.trackThrownPotions(client);
         }
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE && mc.level != null && mc.player != null) {
         if (event.getPacket() instanceof ClientboundEntityEventPacket packet && this.trackTotem.getValue()) {
            mc.execute(() -> this.handleTotemPop(packet));
         }

         if (event.getPacket() instanceof ClientboundSetEquipmentPacket packet && this.trackConsume.getValue()) {
            mc.execute(() -> this.handleEquipmentChange(packet));
         }
      }
   }

   private void handleTotemPop(ClientboundEntityEventPacket packet) {
      Minecraft mc = Minecraft.getInstance();
      if (packet.getEventId() == 35) {
         if (packet.getEntity(mc.level) instanceof Player player && !player.getUUID().equals(mc.player.getUUID())) {
            if (!this.isFriend(player) && this.isTargetValid(player)) {
               long now = System.currentTimeMillis();
               Long lastTime = this.lastTotemPopTime.get(player.getUUID());
               if (lastTime == null || now - lastTime >= 500L) {
                  this.lastTotemPopTime.put(player.getUUID(), now);
                  ItemStack totemStack = null;
                  if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                     totemStack = player.getOffhandItem();
                  } else if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
                     totemStack = player.getMainHandItem();
                  }

                  Component totemName = (Component)(totemStack != null && !totemStack.isEmpty()
                     ? totemStack.getHoverName()
                     : Component.literal("Тотем бессмертия"));
                  boolean enchanted = totemStack != null && (player.getOffhandItem().isEnchanted() || player.getMainHandItem().isEnchanted());
                  MutableComponent message = Component.empty()
                     .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(" потерял ").withStyle(ChatFormatting.GRAY))
                     .append(totemName)
                     .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                     .append(Component.literal(enchanted ? "зачарованный" : "незачарованный").withStyle(enchanted ? ChatFormatting.GREEN : ChatFormatting.RED))
                     .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
                  String plainName = player.getName().getString();
                  if (this.outputSetting.isSelected("Уведомления")) {
                     notify("UseTracker", plainName + " потерял тотем" + (enchanted ? " (зачар)" : " (обычный)"));
                  }

                  if (this.outputSetting.isSelected("Чат")) {
                     this.sendPrefixed(message);
                  }
               }
            }
         }
      }
   }

   private void handleEquipmentChange(ClientboundSetEquipmentPacket packet) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level.getEntity(packet.getEntity()) instanceof Player player && !player.getUUID().equals(mc.player.getUUID())) {
         if (!this.isFriend(player) && this.isTargetValid(player)) {
            EquipmentSlot activeSlot = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

            for (Pair<EquipmentSlot, ItemStack> pair : packet.getSlots()) {
               if (pair.getFirst() == activeSlot) {
                  ItemStack packetStack = (ItemStack)pair.getSecond();
                  ItemStack handStack = player.getItemInHand(player.getUsedItemHand());
                  int useTime = player.getTicksUsingItem();
                  int maxUseTime = handStack.getUseDuration(player);
                  if (useTime < maxUseTime - 1) {
                     return;
                  }
                  String verb = switch (packetStack.getUseAnimation()) {
                     case EAT -> "съел";
                     case DRINK -> "выпил";
                     default -> null;
                  };
                  if (verb == null) {
                     return;
                  }

                  String itemName = stripFormatting(packetStack.getHoverName().getString()).replace("[★]", "").replace("fff", "").replace("ggg", "").trim();
                  MutableComponent message = Component.empty()
                     .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(" " + verb + " ").withStyle(ChatFormatting.GRAY))
                     .append(Component.literal(itemName).withStyle(ChatFormatting.WHITE));
                  String plainName = player.getName().getString();
                  if (this.outputSetting.isSelected("Уведомления")) {
                     notify("UseTracker", plainName + " " + verb + " " + itemName);
                  }

                  if (this.outputSetting.isSelected("Чат")) {
                     this.sendPrefixed(message);
                  }

                  return;
               }
            }
         }
      }
   }

   private void trackConsumedItems(Minecraft client) {
      for (Player player : client.level.players()) {
         if (!player.getUUID().equals(client.player.getUUID()) && !this.isFriend(player) && this.isTargetValid(player)) {
            UUID id = player.getUUID();
            if (client.player.distanceToSqr(player) > 4096.0) {
               this.activeUseItem.remove(id);
               this.useStartTick.remove(id);
            } else if (player.isUsingItem()) {
               this.activeUseItem.computeIfAbsent(id, ignored -> player.getUseItem().copy());
               this.useStartTick.putIfAbsent(id, player.tickCount);
            } else {
               ItemStack used = this.activeUseItem.remove(id);
               Integer startTick = this.useStartTick.remove(id);
               if (used != null && !used.isEmpty() && startTick != null && player.tickCount - startTick >= 31) {
                  String verb = switch (used.getUseAnimation()) {
                     case EAT -> "съел";
                     case DRINK -> "выпил";
                     default -> null;
                  };
                  if (verb != null) {
                     String itemName = stripFormatting(used.getHoverName().getString());
                     MutableComponent message = Component.empty()
                        .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" " + verb + " ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(itemName).withStyle(ChatFormatting.WHITE));
                     String plainName = player.getName().getString();
                     if (this.outputSetting.isSelected("Уведомления")) {
                        notify("UseTracker", plainName + " " + verb + " " + itemName);
                     }

                     if (this.outputSetting.isSelected("Чат")) {
                        this.sendPrefixed(message);
                     }
                  }
               }
            }
         }
      }
   }

   private void trackThrownPotions(Minecraft client) {
      Set<Integer> current = new HashSet<>();
      float maxDistance = this.radius.getFloat();

      for (Entity entity : client.level.entitiesForRendering()) {
         if (entity instanceof AbstractThrownPotion potion && !(client.player.distanceTo(potion) > maxDistance)) {
            int entityId = potion.getId();
            current.add(entityId);
            UseTrackerFeature.PotionData data = this.trackedPotions.get(entityId);
            if (data == null) {
               this.trackedPotions.put(entityId, new UseTrackerFeature.PotionData(potion.getItem().copy(), potion.getX(), potion.getY(), potion.getZ()));
            } else {
               data.lastX = potion.getX();
               data.lastY = potion.getY();
               data.lastZ = potion.getZ();
            }
         }
      }

      Set<Integer> removed = new HashSet<>(this.trackedPotions.keySet());
      removed.removeAll(current);

      for (int entityId : removed) {
         UseTrackerFeature.PotionData data = this.trackedPotions.remove(entityId);
         if (data != null) {
            this.notifyPotionHits(client, data);
         }
      }
   }

   private void notifyPotionHits(Minecraft client, UseTrackerFeature.PotionData data) {
      AABB hitBox = new AABB(data.lastX - 4.0, data.lastY - 2.0, data.lastZ - 4.0, data.lastX + 4.0, data.lastY + 2.0, data.lastZ + 4.0);

      for (LivingEntity hit : client.level.getEntitiesOfClass(LivingEntity.class, hitBox, ignored -> true)) {
         if (hit instanceof Player player && !this.isFriend(player) && this.isTargetValid(player)) {
            double dx = player.getX() - data.lastX;
            double dz = player.getZ() - data.lastZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (!(distance > 4.0)) {
               double hitChance = Math.max(0.0, 1.0 - distance / 4.0) * 100.0;
               ChatFormatting hitColor = hitChance >= 65.0 ? ChatFormatting.GREEN : (hitChance >= 35.0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
               MutableComponent message = Component.empty()
                  .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                  .append(Component.literal(" получил ").withStyle(ChatFormatting.GRAY))
                  .append(data.stack.getHoverName())
                  .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                  .append(Component.literal(String.format("%.0f%%", hitChance)).withStyle(hitColor))
                  .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
               String plain = player.getName().getString()
                  + " получил "
                  + stripFormatting(data.stack.getHoverName().getString())
                  + String.format(" (%.0f%%)", hitChance);
               if (this.outputSetting.isSelected("Уведомления")) {
                  notify("UseTracker", plain);
               }

               if (this.outputSetting.isSelected("Чат")) {
                  this.sendPrefixed(message);
               }
            }
         }
      }
   }

   private boolean isFriend(Player player) {
      return FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
   }

   private boolean isTargetValid(Player player) {
      if (this.targetSetting.is("Все")) {
         return true;
      }

      AuraFeature aura = AuraFeature.getInstance();
      LivingEntity target = aura != null ? aura.getTarget() : null;
      return target == player;
   }

   private static void notify(String title, String detail) {
      NotificationsElement.notify(Textures.Icons.EYE, Theme.getAccent(), title, sanitizeForNotification(detail));
   }

   private static String sanitizeForNotification(String text) {
      MsdfFont font = UiFonts.sfPro(UiFontStyle.MEDIUM.weight());
      StringBuilder builder = new StringBuilder(text.length());
      boolean lastWasSpace = true;
      int index = 0;

      while (index < text.length()) {
         int codePoint = text.codePointAt(index);
         index += Character.charCount(codePoint);
         if (Character.isWhitespace(codePoint)) {
            if (!lastWasSpace) {
               builder.append(' ');
               lastWasSpace = true;
            }
         } else {
            String part = new String(Character.toChars(codePoint));
            if (font.canRender(part)) {
               builder.append(part);
               lastWasSpace = false;
            }
         }
      }

      return builder.toString().trim();
   }

   private void sendPrefixed(Component message) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         MutableComponent text = Component.empty()
            .append(Component.literal("XRose Beta").withStyle(style -> style.withBold(true).withColor(8385279)))
            .append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY))
            .append(message);
         mc.player.sendSystemMessage(text);
      }
   }

   private static String stripFormatting(String text) {
      String stripped = ChatFormatting.stripFormatting(text);
      return stripped == null ? text : stripped;
   }

   private static final class PotionData {
      final ItemStack stack;
      double lastX;
      double lastY;
      double lastZ;

      PotionData(ItemStack stack, double x, double y, double z) {
         this.stack = stack;
         this.lastX = x;
         this.lastY = y;
         this.lastZ = z;
      }
   }
}

