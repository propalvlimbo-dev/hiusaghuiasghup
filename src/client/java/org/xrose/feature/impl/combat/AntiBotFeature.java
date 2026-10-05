package org.xrose.feature.impl.combat;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.ModeSetting;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AntiBotFeature extends Feature {
   private static final Minecraft MC = Minecraft.getInstance();
   private static final Set<UUID> BOT_SET = new HashSet<>();
   private static AntiBotFeature instance;
   private final Set<UUID> suspectSet = new HashSet<>();
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "ReallyWorld", "ReallyWorld", "Matrix"));

   public AntiBotFeature() {
      super("Anti Bot", "Filters fake server-side entities", FeatureCategory.COMBAT, -1);
      instance = this;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (MC.player != null && MC.level != null) {
         if (this.mode.is("Matrix")) {
            this.matrixMode();
         } else {
            this.reallyWorldMode();
         }
      } else {
         this.reset();
      }
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE) {
         if (event.getPacket() instanceof ClientboundPlayerInfoUpdatePacket packet) {
            packet.newEntries().forEach(this::checkPlayerAfterSpawn);
         } else if (event.getPacket() instanceof ClientboundPlayerInfoRemovePacket packet) {
            packet.profileIds().forEach(uuid -> {
               this.suspectSet.remove(uuid);
               BOT_SET.remove(uuid);
            });
         }
      }
   }

   private void checkPlayerAfterSpawn(Entry entry) {
      GameProfile profile = entry.profile();
      if (profile != null && entry.latency() >= 2 && (profile.properties() == null || profile.properties().isEmpty())) {
         if (this.isDuplicateProfile(profile)) {
            BOT_SET.add(profile.id());
         } else {
            this.suspectSet.add(profile.id());
         }
      }
   }

   private void matrixMode() {
      if (MC.level != null) {
         for (UUID uuid : Set.copyOf(this.suspectSet)) {
            Player player = MC.level.getPlayerByUUID(uuid);
            if (player != null && (this.isFullyEquipped(player) || hasForeignUuid(player) || this.isNameBot(player))) {
               BOT_SET.add(uuid);
            }

            this.suspectSet.remove(uuid);
         }

         if (MC.player != null && MC.player.tickCount % 100 == 0) {
            BOT_SET.removeIf(uuidx -> MC.level.getPlayerByUUID(uuidx) == null);
         }
      }
   }

   private void reallyWorldMode() {
      if (MC.level != null) {
         for (Player player : MC.level.players()) {
            if (player != MC.player) {
               boolean isBot = false;
               boolean hasValidArmor = true;

               for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                  ItemStack armor = player.getItemBySlot(slot);
                  if (armor.getItem() == Items.AIR || !armor.isEnchantable() || armor.isDamaged()) {
                     hasValidArmor = false;
                     break;
                  }
               }

               boolean hasValidEquipment = false;
               if (player.getOffhandItem().getItem() == Items.AIR) {
                  for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                     ItemStack armor = player.getItemBySlot(slot);
                     if (armor.getItem() == Items.LEATHER_HELMET
                        || armor.getItem() == Items.LEATHER_CHESTPLATE
                        || armor.getItem() == Items.LEATHER_LEGGINGS
                        || armor.getItem() == Items.LEATHER_BOOTS
                        || armor.getItem() == Items.IRON_HELMET
                        || armor.getItem() == Items.IRON_CHESTPLATE
                        || armor.getItem() == Items.IRON_LEGGINGS
                        || armor.getItem() == Items.IRON_BOOTS) {
                        hasValidEquipment = true;
                        break;
                     }
                  }
               }

               boolean hasFullFood = player.getFoodData().getFoodLevel() == 20;
               isBot = hasValidArmor && hasValidEquipment && hasFullFood;
               if (isBot) {
                  BOT_SET.add(player.getUUID());
               } else {
                  BOT_SET.remove(player.getUUID());
               }
            }
         }
      }
   }

   private boolean isDuplicateProfile(GameProfile profile) {
      return MC.getConnection() != null
         && MC.getConnection()
               .getOnlinePlayers()
               .stream()
               .filter(p -> p.getProfile().name().equals(profile.name()) && !p.getProfile().id().equals(profile.id()))
               .count()
            == 1L;
   }

   private boolean isFullyEquipped(Player player) {
      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack stack = player.getItemBySlot(slot);
         Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
         if (stack.isEmpty() || equippable == null || stack.isEnchanted()) {
            return false;
         }
      }

      return true;
   }

   public boolean isBot(Player player) {
      return player != null && (BOT_SET.contains(player.getUUID()) || this.isNameBot(player) || this.isBotU(player) || hasForeignUuid(player));
   }

   public static boolean shouldIgnore(Player player) {
      return instance != null && instance.isEnabled() && instance.isBot(player);
   }

   public static boolean isKnownBot(UUID uuid) {
      return BOT_SET.contains(uuid);
   }

   public static boolean hasForeignUuid(Player player) {
      if (player == null) {
         return false;
      }

      String name = player.getName().getString();
      return !name.contains("NPC") && !name.startsWith("[ZNPC]")
         ? !player.getUUID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)))
         : false;
   }

   private boolean isNameBot(Player player) {
      String name = player.getName().getString();
      return name.startsWith("CIT-") && !name.contains("NPC") && !name.startsWith("[ZNPC]");
   }

   private boolean isBotU(Entity entity) {
      String name = entity.getName().getString();
      return entity.isInvisible()
         && !name.contains("NPC")
         && !name.startsWith("[ZNPC]")
         && !entity.getUUID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
   }

   private void reset() {
      this.suspectSet.clear();
      BOT_SET.clear();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }
}

