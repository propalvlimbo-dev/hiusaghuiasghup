package org.xrose.menu.core;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.event.events.screen.ScreenMouseButtonEvent;
import org.xrose.feature.impl.player.AutoBuyScreen;

public final class MenuOverlay {
   private static final MenuOverlayState STATE = new MenuOverlayState();
   private static final MenuDragController DRAG = new MenuDragController();
   private static final MenuOverlayRenderer RENDERER = new MenuOverlayRenderer();

   private MenuOverlay() {
   }

   public static boolean toggle(Minecraft minecraft) {
      if (STATE.isInteractive()) {
         close(minecraft);
         return true;
      } else {
         open(minecraft);
         return true;
      }
   }

   public static void close(Minecraft minecraft) {
      if (STATE.isOpen() && !STATE.isClosing()) {
         boolean restoreMouse = shouldReturnMouseToGame(minecraft);
         STATE.beginClose();
         DRAG.reset();
         RENDERER.releasePointer();
         if (restoreMouse) {
            minecraft.mouseHandler.grabMouse();
         }
      }
   }

   public static boolean suspendForReload(Minecraft minecraft) {
      if (!STATE.isInteractive()) {
         return false;
      }

      boolean restoreMouse = STATE.grabbedMouseBeforeOpen() && shouldReturnMouseToGame(minecraft);
      STATE.suspend();
      DRAG.reset();
      RENDERER.releasePointer();
      if (restoreMouse) {
         minecraft.mouseHandler.grabMouse();
      }

      return true;
   }

   public static void resumeAfterReload(Minecraft minecraft) {
      if (!STATE.isOpen()) {
         STATE.resume();
         DRAG.reset();
         KeyMapping.releaseAll();
         if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
         }
      }
   }

   public static boolean isOpen() {
      return STATE.isInteractive();
   }

   public static boolean isVisible() {
      return STATE.isOpen();
   }

   public static boolean blocksInput() {
      return STATE.isInteractive();
   }

   public static void focusNextHeaderAction(int direction) {
      STATE.focusNextHeaderAction(direction);
   }

   public static void activateFocusedHeaderAction() {
      STATE.activateFocusedHeaderAction();
   }

   public static void closePageOrOverlay(Minecraft minecraft) {
      if (STATE.page() != MenuPage.NONE) {
         STATE.openPage(MenuPage.NONE);
      } else {
         close(minecraft);
      }
   }

   public static void openSearch() {
      if (STATE.isInteractive()) {
         if (STATE.page() != MenuPage.SEARCH) {
            STATE.openPage(MenuPage.SEARCH);
         }

         RENDERER.focusSearch();
      }
   }

   public static boolean isSearchOpen() {
      return STATE.isInteractive() && RENDERER.isSearchOpen();
   }

   public static boolean isSearchFocused() {
      return STATE.isInteractive() && RENDERER.isSearchFocused();
   }

   public static boolean isCapturingBind() {
      return STATE.isInteractive() && RENDERER.isCapturingBind();
   }

   public static void backspaceSearch() {
      RENDERER.backspaceSearch();
   }

   public static boolean handleKey(int key) {
      return STATE.isInteractive() && RENDERER.handleKey(key);
   }

   public static boolean handleCharacter(int codePoint) {
      if (!STATE.isInteractive()) {
         return false;
      }

      if (RENDERER.handleCharacter(codePoint)) {
         return true;
      }

      if (!isSearchFocused()) {
         return false;
      }

      RENDERER.appendSearchCodePoint(codePoint);
      return true;
   }

   public static void handleScroll(double vertical) {
      if (STATE.isInteractive()) {
         RENDERER.handleScroll(mouseX(Minecraft.getInstance()), mouseY(Minecraft.getInstance()), vertical);
      }
   }

   public static boolean handleMouseButton(Minecraft minecraft, int button, int action) {
      if (!STATE.isInteractive()) {
         return false;
      }

      int screenWidth = minecraft.getWindow().getGuiScaledWidth();
      int screenHeight = minecraft.getWindow().getGuiScaledHeight();
      int mouseX = mouseX(minecraft);
      int mouseY = mouseY(minecraft);
      RENDERER.layout(minecraft, STATE, screenWidth, screenHeight, mouseX, mouseY);
      if (action == 1) {
         if (RENDERER.handleMouseButton(mouseX, mouseY, button, STATE)) {
            DRAG.onRelease();
            return true;
         }

         if (button == 0) {
            DRAG.onPress(mouseX, mouseY, STATE, RENDERER);
         }
      } else if (action == 0) {
         RENDERER.releasePointer();
         if (button == 0) {
            DRAG.onRelease();
         }
      }

      return true;
   }

   public static boolean handleScreenMouseButton(Minecraft minecraft, int button, ScreenMouseButtonEvent.Action action) {
      return action == ScreenMouseButtonEvent.Action.DRAG
         ? STATE.isInteractive()
         : handleMouseButton(minecraft, button, action == ScreenMouseButtonEvent.Action.RELEASE ? 0 : 1);
   }

   public static void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor, int screenWidth, int screenHeight) {
      if (!(minecraft.gui.screen() instanceof AutoBuyScreen)) {
         if (STATE.isOpen()) {
            if (STATE.isClosing() && STATE.openProgress() <= 0.001F) {
               STATE.close();
            } else {
               MenuDimensions dimensions = MenuDimensions.resolve(minecraft, STATE);
               int mouseX = mouseX(minecraft);
               int mouseY = mouseY(minecraft);
               DRAG.update(mouseX, mouseY, STATE, dimensions, screenWidth, screenHeight);
               RENDERER.layout(minecraft, STATE, screenWidth, screenHeight, mouseX, mouseY);
               RENDERER.drag(mouseX, mouseY);
               RENDERER.render(minecraft, guiGraphicsExtractor);
            }
         }
      }
   }

   private static void open(Minecraft minecraft) {
      boolean grabbedMouseBeforeOpen = minecraft.mouseHandler.isMouseGrabbed();
      STATE.open(grabbedMouseBeforeOpen);
      DRAG.reset();
      KeyMapping.releaseAll();
      if (grabbedMouseBeforeOpen) {
         minecraft.mouseHandler.releaseMouse();
      }
   }

   private static int mouseX(Minecraft minecraft) {
      return (int)Math.round(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()));
   }

   private static int mouseY(Minecraft minecraft) {
      return (int)Math.round(minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()));
   }

   private static boolean shouldReturnMouseToGame(Minecraft minecraft) {
      return minecraft.level != null && minecraft.player != null && minecraft.gui.screen() == null;
   }
}

