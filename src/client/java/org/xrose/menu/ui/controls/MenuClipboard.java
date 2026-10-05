package org.xrose.menu.ui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OS;
import org.lwjgl.glfw.GLFW;

public final class MenuClipboard {
   private MenuClipboard() {
   }

   public static boolean shortcutDown() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.getWindow() != null) {
         long window = client.getWindow().handle();
         return Util.getPlatform() == OS.OSX ? keyDown(window, 343) || keyDown(window, 347) : keyDown(window, 341) || keyDown(window, 345);
      } else {
         return false;
      }
   }

   public static String get() {
      Minecraft client = Minecraft.getInstance();
      return client == null ? "" : client.keyboardHandler.getClipboard();
   }

   public static void set(String value) {
      Minecraft client = Minecraft.getInstance();
      if (client != null) {
         client.keyboardHandler.setClipboard(value == null ? "" : value);
      }
   }

   private static boolean keyDown(long window, int key) {
      return GLFW.glfwGetKey(window, key) == 1;
   }
}

