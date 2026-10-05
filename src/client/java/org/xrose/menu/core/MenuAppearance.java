package org.xrose.menu.core;

public final class MenuAppearance {
   private static final MenuBackground BACKGROUND = MenuBackground.NONE;
   private static float backgroundDim = 0.35F;

   private MenuAppearance() {
   }

   public static MenuBackground background() {
      return BACKGROUND;
   }

   public static void setBackground(MenuBackground next) {
   }

   public static float backgroundDim() {
      return backgroundDim;
   }

   public static void setBackgroundDim(float dim) {
      backgroundDim = Math.max(0.0F, Math.min(1.0F, dim));
   }
}

