package org.xrose.menu.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.xrose.utils.ConfigIO;
import org.xrose.utils.math.MathUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class MenuConfigStore {
   private static final Path PATH = ConfigIO.resolve("menu.json");
   private static JsonObject root;

   private MenuConfigStore() {
   }

   public static float getFloat(String key, float defaultValue) {
      JsonElement element = data().get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsFloat() : defaultValue;
   }

   public static int getInt(String key, int defaultValue) {
      JsonElement element = data().get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsInt() : defaultValue;
   }

   public static boolean getBoolean(String key, boolean defaultValue) {
      JsonElement element = data().get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : defaultValue;
   }

   public static String getString(String key, String defaultValue) {
      JsonElement element = data().get(key);
      return element != null && element.isJsonPrimitive() ? element.getAsString() : defaultValue;
   }

   public static JsonArray getArray(String key) {
      JsonElement element = data().get(key);
      return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
   }

   public static void save(Consumer<JsonObject> mutator) {
      JsonObject data = data();
      mutator.accept(data);
      ConfigIO.write(PATH, data);
   }

   static float loadUiScaleValue() {
      float scale = getFloat("uiScale", Float.NaN);
      return Float.isNaN(scale) ? 0.28F : MathUtil.clamp01((scale - 0.525F) / 1.5749999F);
   }

   static void saveUiScaleValue(float uiScaleValue) {
      float scale = MathUtil.lerp(0.525F, 2.1F, uiScaleValue);
      save(data -> data.addProperty("uiScale", scale));
   }

   public static void saveAccentIndex(int accentIndex) {
      save(data -> data.addProperty("accentIndex", accentIndex));
   }

   public static void resetHudPositions() {
      save(data -> data.entrySet().removeIf(entry -> {
         String key = (String)entry.getKey();
         return key.startsWith("hud.") && (key.endsWith(".x") || key.endsWith(".y"));
      }));
   }

   private static JsonObject data() {
      if (root != null) {
         return root;
      }

      JsonObject loaded = ConfigIO.read(PATH);
      root = loaded != null ? loaded : new JsonObject();
      return root;
   }
}

