package org.xrose.utils.combat.aura.util;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerType {
   private ServerType() {
   }

   public static String getServerName() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() != null && mc.getConnection().getServerData() != null) {
         ServerData data = mc.getConnection().getServerData();
         String address = safeLower(data.ip);
         String brand = safeLower(mc.getConnection().serverBrand());
         String normalizedBrand = normalizeServerToken(mc.getConnection().serverBrand());
         if (brand.contains("botfilter") || normalizedBrand.contains("botfilter")) {
            return "FunTime";
         } else if (address.contains("spooky")
            || address.contains("pookie")
            || brand.contains("spooky")
            || brand.contains("pookie")
            || normalizedBrand.contains("spookycore")
            || normalizedBrand.contains("spookytime")
            || normalizedBrand.contains("pookietime")) {
            return "SpookyTime";
         } else if (address.contains("funtime") || address.contains("skytime") || address.contains("space-times") || address.contains("funsky")) {
            return "CopyTime";
         } else if (brand.contains("holyworld") || normalizedBrand.contains("holyworld") || brand.contains("vk.com/idwok")) {
            return "HolyWorld";
         } else if (address.contains("reallyworld")) {
            return "ReallyWorld";
         } else {
            return address.contains("gulpvp") ? "GulPvP" : "Vanilla";
         }
      } else {
         return "Vanilla";
      }
   }

   private static String normalizeServerToken(String value) {
      if (value != null && !value.isEmpty()) {
         StringBuilder out = new StringBuilder(value.length());
         boolean skip = false;

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (skip) {
               skip = false;
            } else if (c == 167) {
               skip = true;
            } else if (c != 194 && !Character.isWhitespace(c)) {
               out.append(Character.toLowerCase(c));
            }
         }

         return out.toString();
      } else {
         return "";
      }
   }

   private static String safeLower(String value) {
      return value == null ? "" : value.toLowerCase(Locale.ROOT);
   }
}

