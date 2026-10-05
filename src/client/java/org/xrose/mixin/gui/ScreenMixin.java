package org.xrose.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.command.CommandManager;
import org.xrose.event.EventManager;
import org.xrose.event.Events;
import org.xrose.event.events.screen.ScreenCloseEvent;
import org.xrose.event.events.screen.ScreenKeyEvent;
import org.xrose.event.events.screen.ScreenRenderEvent;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.text.ChatUtil;

@Mixin(Screen.class)
public abstract class ScreenMixin {
   @Shadow
   @Final
   protected Minecraft minecraft;

   @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
   private void onClose(CallbackInfo ci) {
      if (EventManager.hasListeners(ScreenCloseEvent.class)) {
         if (EventManager.call(Events.SCREEN_CLOSE.set(this.minecraft, (Screen)(Object)this)).isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void onExtractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (EventManager.hasListeners(ScreenRenderEvent.class)) {
         if (EventManager.call(Events.SCREEN_RENDER.set((Screen)(Object)this, guiGraphicsExtractor, mouseX, mouseY, partialTick)).isCancelled()) {
            ci.cancel();
         }
      }
   }

   @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private int maskMouseXWhenOverlayOpen(int mouseX) {
      return MenuOverlay.isOpen() ? -536870912 : mouseX;
   }

   @ModifyVariable(method = "extractRenderState", at = @At("HEAD"), ordinal = 1, argsOnly = true)
   private int maskMouseYWhenOverlayOpen(int mouseY) {
      return MenuOverlay.isOpen() ? -536870912 : mouseY;
   }

   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
      if (EventManager.hasListeners(ScreenKeyEvent.class)) {
         if (EventManager.call(Events.SCREEN_KEY.set((Screen)(Object)this, keyEvent, ScreenKeyEvent.Action.PRESS)).isCancelled()) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
   private static void onDefaultHandleGameClickEvent(ClickEvent clickEvent, Minecraft client, Screen screen, CallbackInfo ci) {
      if (clickEvent instanceof RunCommand runCommand) {
         String command = runCommand.command();
         if (command != null && command.startsWith(CommandManager.INSTANCE.getPrefix())) {
            CommandManager.INSTANCE.handleChat(command);
            ci.cancel();
         }
      }
   }

   @Inject(method = "defaultHandleClickEvent", at = @At("HEAD"), cancellable = true)
   private static void onDefaultHandleClickEvent(ClickEvent clickEvent, Minecraft client, Screen screen, CallbackInfo ci) {
      if (clickEvent instanceof CopyToClipboard copyToClipboard) {
         String value = copyToClipboard.value();
         if (value != null && !value.isEmpty() && client != null && client.keyboardHandler != null) {
            client.keyboardHandler.setClipboard(value);
            ChatUtil.info("Copied to clipboard");
            ci.cancel();
         }
      }
   }
}

