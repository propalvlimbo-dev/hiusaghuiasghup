package org.xrose.menu.core;

public enum MenuPage {
   NONE,
   SEARCH,
   SETTINGS,
   FRIENDS,
   ACCOUNT_SWITCHER,
   CONFIGURATIONS;

   private static final MenuPage[] ACTION_PAGES = new MenuPage[]{SETTINGS, FRIENDS, ACCOUNT_SWITCHER, CONFIGURATIONS};
   private static final MenuPage[] NAV_PAGES = new MenuPage[]{NONE, SEARCH, FRIENDS, ACCOUNT_SWITCHER, SETTINGS, CONFIGURATIONS};

   public static MenuPage[] actionPages() {
      return (MenuPage[])ACTION_PAGES.clone();
   }

   public static MenuPage[] navPages() {
      return (MenuPage[])NAV_PAGES.clone();
   }

   public static int navCount() {
      return NAV_PAGES.length;
   }

   public static MenuPage navAt(int index) {
      return NAV_PAGES[Math.floorMod(index, NAV_PAGES.length)];
   }

   public int navIndex() {
      for (int index = 0; index < NAV_PAGES.length; index++) {
         if (NAV_PAGES[index] == this) {
            return index;
         }
      }

      return -1;
   }

   public static int actionCount() {
      return ACTION_PAGES.length;
   }

   public static MenuPage actionAt(int index) {
      return ACTION_PAGES[Math.floorMod(index, ACTION_PAGES.length)];
   }

   public int actionIndex() {
      for (int index = 0; index < ACTION_PAGES.length; index++) {
         if (ACTION_PAGES[index] == this) {
            return index;
         }
      }

      return -1;
   }

   // $VF: synthetic method
   private static MenuPage[] $values() {
      return new MenuPage[]{NONE, SEARCH, SETTINGS, FRIENDS, ACCOUNT_SWITCHER, CONFIGURATIONS};
   }
}
