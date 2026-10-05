package org.xrose.menu;

import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.CharacterInputEvent;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.event.events.screen.ScreenKeyEvent;
import org.xrose.event.events.screen.ScreenMouseButtonEvent;
import org.xrose.feature.impl.player.AutoBuyScreen;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.menu.ui.controls.MenuClipboard;

public final class MenuKeyHandler implements MinecraftContext {
   private static boolean isAutoBuyScreenOpen() {
      return mc != null && mc.gui != null && mc.gui.screen() instanceof AutoBuyScreen;
   }

   @EventTarget(priority = 1000)
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (!isAutoBuyScreenOpen()) {
         if (event.getKey() != 300) {
            if (event.getAction() != 1 || event.getKey() != 344 || MenuOverlay.isOpen() && MenuOverlay.isCapturingBind()) {
               if (MenuOverlay.isOpen()) {
                  boolean pressOrRepeat = event.getAction() == 1 || event.getAction() == 2;
                  if (pressOrRepeat && MenuOverlay.handleKey(event.getKey())) {
                     event.cancel();
                  } else if (event.getAction() == 1 && event.getKey() == 344) {
                     event.cancel();
                  } else if (pressOrRepeat && event.getKey() == 256) {
                     MenuOverlay.closePageOrOverlay(mc);
                     event.cancel();
                  } else if (pressOrRepeat && event.getKey() == 259 && MenuOverlay.isSearchFocused()) {
                     MenuOverlay.backspaceSearch();
                     event.cancel();
                  } else if (event.getAction() == 1 && event.getKey() == 70 && MenuClipboard.shortcutDown()) {
                     MenuOverlay.openSearch();
                     event.cancel();
                  } else if (event.getAction() != 1 || event.getKey() != 258 && event.getKey() != 262) {
                     if (event.getAction() == 1 && event.getKey() == 263) {
                        MenuOverlay.focusNextHeaderAction(-1);
                        event.cancel();
                     } else if (event.getAction() == 1 && event.getKey() == 257) {
                        MenuOverlay.activateFocusedHeaderAction();
                        event.cancel();
                     } else {
                        event.cancel();
                     }
                  } else {
                     MenuOverlay.focusNextHeaderAction(1);
                     event.cancel();
                  }
               }
            } else {
               if (MenuOverlay.toggle(mc)) {
                  event.cancel();
               }
            }
         }
      }
   }

   @EventTarget(priority = 1000)
   public void onCharacterInput(CharacterInputEvent event) {
      if (!isAutoBuyScreenOpen()) {
         if (MenuOverlay.handleCharacter(event.getCodePoint())) {
            event.cancel();
         }
      }
   }

   @EventTarget(priority = 1000)
   public void onMouseInput(MouseInputEvent event) {
      if (!isAutoBuyScreenOpen()) {
         if (MenuOverlay.handleMouseButton(mc, event.getButton(), event.getAction())) {
            event.cancel();
         }
      }
   }

   @EventTarget(priority = 1000)
   public void onScreenKey(ScreenKeyEvent event) {
      if (!isAutoBuyScreenOpen()) {
         if (event.getKeyEvent() == null || event.getKeyEvent().key() != 300) {
            if (MenuOverlay.blocksInput()) {
               event.cancel();
            }
         }
      }
   }

   @EventTarget(priority = 1000)
   public void onScreenMouseButton(ScreenMouseButtonEvent event) {
      if (!isAutoBuyScreenOpen()) {
         if (MenuOverlay.handleScreenMouseButton(mc, event.getMouseButtonEvent().button(), event.getAction())) {
            event.cancel();
         }
      }
   }
}

