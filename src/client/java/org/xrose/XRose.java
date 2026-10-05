package org.xrose;

import net.fabricmc.api.ModInitializer;
import org.xrose.command.CommandManager;
import org.xrose.event.EventManager;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.ClientStartEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.feature.FeatureManager;
import org.xrose.hud.NotificationsElement;
import org.xrose.menu.MenuKeyHandler;
import org.xrose.menu.MenuOverlayRenderHandler;
import org.xrose.menu.i18n.UiLanguage;
import org.xrose.menu.pages.configs.ConfigPreviewManager;
import org.xrose.pve.PveAutomationCoordinator;
import org.xrose.pve.PvpStateTracker;
import org.xrose.pve.economy.AhSellFlow;
import org.xrose.utils.AccountSwitcher;
import org.xrose.utils.FriendManager;
import org.xrose.utils.ScreenNbtParser;
import org.xrose.utils.StaffManager;
import org.xrose.utils.blockesp.BlockEspConfig;
import org.xrose.utils.combat.ServerSprintTracker;
import org.xrose.utils.combat.ServerTickSync;
import org.xrose.utils.combat.aura.AngleConnection;
import org.xrose.utils.inventory.DropAllInventoryController;
import org.xrose.utils.inventory.InventoryFlowManager;
import org.xrose.utils.inventory.InventorySwap;
import org.xrose.utils.render.EntityEspStateCache;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.gui.GuiTexture;
import org.xrose.utils.render.world.WorldEffects;

public class XRose implements ModInitializer {
   private final MenuKeyHandler menuKeyHandler = new MenuKeyHandler();
   private final MenuOverlayRenderHandler menuOverlayRenderHandler = new MenuOverlayRenderHandler();

   public void onInitialize() {
      UiLanguage.loadSaved();
      FriendManager.INSTANCE.initialize();
      StaffManager.INSTANCE.initialize();
      BlockEspConfig.INSTANCE.initialize();
      FeatureManager.INSTANCE.initialize();
      CommandManager.INSTANCE.initialize();
      WorldEffects.bootstrap();
      EventManager.subscribe(FeatureManager.INSTANCE);
      EventManager.subscribe(CommandManager.INSTANCE);
      EventManager.subscribe(FriendManager.INSTANCE);
      EventManager.subscribe(StaffManager.INSTANCE);
      EventManager.subscribe(new NotificationsElement.ToggleListener());
      EventManager.subscribe(this.menuKeyHandler);
      EventManager.subscribe(this.menuOverlayRenderHandler);
      EventManager.subscribe(InventorySwap.INSTANCE);
      EventManager.subscribe(DropAllInventoryController.INSTANCE);
      EventManager.subscribe(ServerTickSync.INSTANCE);
      EventManager.subscribe(ServerSprintTracker.INSTANCE);
      EventManager.subscribe(AngleConnection.INSTANCE);
      EventManager.subscribe(PveAutomationCoordinator.INSTANCE);
      EventManager.subscribe(PvpStateTracker.INSTANCE);
      EventManager.subscribe(AhSellFlow.INSTANCE);
      EventManager.subscribe(ScreenNbtParser.INSTANCE);
      EventManager.subscribe(ConfigPreviewManager.INSTANCE);
      EventManager.subscribe(this);
   }

   @EventTarget
   public void onClientStart(ClientStartEvent event) {
      AccountSwitcher.applyStartupAccount();
      GuiTexture.prewarm(Textures.all());
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      InventoryFlowManager.update();
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      EntityEspStateCache.clear();
   }
}

