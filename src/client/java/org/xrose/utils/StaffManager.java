package org.xrose.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.impl.pve.ModeratorDetector;
import org.xrose.hud.NotificationsElement;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.text.ChatUtil;

public final class StaffManager {
   public static final StaffManager INSTANCE = new StaffManager();
   private static final Logger LOGGER = LoggerFactory.getLogger(StaffManager.class);
   private static final long DETECTION_INTERVAL_MS = 2000L;
   private final Path path = ConfigIO.resolve("staff.json");
   private final Map<String, String> staff = new LinkedHashMap<>();
   private final Set<String> onlineStaff = new HashSet<>();
   private boolean initialized;
   private long nextDetection;

   private StaffManager() {
   }

   public synchronized void initialize() {
      if (!this.initialized) {
         this.load();
         this.initialized = true;
      }
   }

   public synchronized boolean add(String name) {
      String displayName = sanitize(name);
      if (FriendManager.isValidName(displayName) && !this.staff.containsKey(normalize(displayName))) {
         this.staff.put(normalize(displayName), displayName);
         this.save();
         return true;
      } else {
         return false;
      }
   }

   public synchronized boolean remove(String name) {
      if (name != null && this.staff.remove(normalize(name)) != null) {
         this.save();
         return true;
      } else {
         return false;
      }
   }

   public synchronized boolean isStaff(String name) {
      return name != null && this.staff.containsKey(normalize(name));
   }

   public synchronized Collection<String> getStaff() {
      return new ArrayList<>(this.staff.values());
   }

   public synchronized boolean clear() {
      if (this.staff.isEmpty()) {
         return false;
      }

      this.staff.clear();
      this.save();
      return true;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft client = event.getClient();
      if (client != null && client.player != null && client.getConnection() != null) {
         long now = System.currentTimeMillis();
         if (now >= this.nextDetection) {
            this.nextDetection = now + 2000L;
            Set<String> nowOnline = new HashSet<>();
            Set<String> autoAdded = new HashSet<>();

            for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
               String name = info.getProfile().name();
               if (name != null) {
                  String key = normalize(name);
                  if (ModeratorDetector.containsRole(info) && !this.isStaff(name)) {
                     this.add(name);
                     autoAdded.add(key);
                     this.announceJoined(name);
                  }

                  if (this.isStaff(name)) {
                     nowOnline.add(key);
                  }
               }
            }

            for (String key : nowOnline) {
               if (!autoAdded.contains(key) && !this.onlineStaff.contains(key)) {
                  this.announceJoined(this.staff.get(key));
               }
            }

            this.onlineStaff.clear();
            this.onlineStaff.addAll(nowOnline);
         }
      } else {
         this.onlineStaff.clear();
      }
   }

   private void announceJoined(String name) {
      if (name != null) {
         ChatUtil.info("Staff joined  •  " + name);
         NotificationsElement.notify(Textures.Icons.USER_ROUND, Theme.getAccent(), "Staff joined", name);
      }
   }

   private void load() {
      JsonObject root = ConfigIO.read(this.path);
      if (root != null) {
         try {
            JsonElement entries = root.get("staff");
            if (entries == null || !entries.isJsonArray()) {
               return;
            }

            for (JsonElement element : entries.getAsJsonArray()) {
               String name = this.readName(element);
               if (name != null) {
                  this.staff.putIfAbsent(normalize(name), name);
               }
            }
         } catch (Exception exception) {
            LOGGER.error("Failed to load staff from {}", this.path, exception);
         }
      }
   }

   private String readName(JsonElement element) {
      String name = null;
      if (element.isJsonPrimitive()) {
         name = sanitize(element.getAsString());
      } else if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
         name = sanitize(element.getAsJsonObject().get("name").getAsString());
      }

      return FriendManager.isValidName(name) ? name : null;
   }

   private void save() {
      JsonArray entries = new JsonArray();

      for (String name : this.staff.values()) {
         entries.add(name);
      }

      JsonObject root = new JsonObject();
      root.add("staff", entries);
      ConfigIO.write(this.path, root);
   }

   private static String sanitize(String value) {
      if (value == null) {
         return null;
      }

      String name = value.trim();
      return name.isEmpty() ? null : name;
   }

   private static String normalize(String value) {
      return value.trim().toLowerCase(Locale.ROOT);
   }
}

