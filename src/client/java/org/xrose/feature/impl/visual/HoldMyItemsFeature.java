package org.xrose.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import org.xrose.event.EventManager;
import org.xrose.event.EventTarget;
import org.xrose.event.events.lifecycle.ClientStartEvent;
import org.xrose.event.events.lifecycle.ResourceReloadEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.menu.core.MenuOverlay;
import sdk.api.optimize.optimize;

@optimize
public final class HoldMyItemsFeature extends Feature {
   private static final String RESOURCE_PACK_ID = "holdmyitems:pack_test";
   private static final HoldMyItemsFeature.ReloadCompletionListener RELOAD_COMPLETION = new HoldMyItemsFeature.ReloadCompletionListener();

   public HoldMyItemsFeature() {
      super("HoldMyItems", "Animated hands and held items in first person", FeatureCategory.VISUAL, -1);
   }

   public static boolean isActive() {
      return FeatureManager.INSTANCE.getEnabled(HoldMyItemsFeature.class) != null;
   }

   @Override
   protected void onEnable() {
      syncResourcePack(true);
   }

   @Override
   protected void onDisable() {
      syncResourcePack(false);
   }

   @EventTarget
   public void onClientStart(ClientStartEvent event) {
      syncResourcePack(true);
   }

   private static void syncResourcePack(boolean enabled) {
      Minecraft client = Minecraft.getInstance();
      if (client != null) {
         client.execute(() -> {
            PackRepository repository = client.getResourcePackRepository();
            repository.reload();
            boolean selected = repository.getSelectedIds().contains("holdmyitems:pack_test");
            boolean changed = enabled ? !selected && repository.addPack("holdmyitems:pack_test") : selected && repository.removePack("holdmyitems:pack_test");
            if (changed) {
               boolean restoreMenu = MenuOverlay.suspendForReload(client);
               if (restoreMenu) {
                  RELOAD_COMPLETION.arm();
               }

               try {
                  client.options.updateResourcePacks(repository);
               } catch (RuntimeException exception) {
                  if (restoreMenu) {
                     RELOAD_COMPLETION.complete(client);
                  }

                  throw exception;
               }
            }
         });
      }
   }

   private static final class ReloadCompletionListener {
      private boolean armed;

      private void arm() {
         if (!this.armed) {
            this.armed = true;
            EventManager.subscribe(this);
         }
      }

      private void complete(Minecraft client) {
         if (this.armed) {
            this.armed = false;
            EventManager.unsubscribe(this);
            MenuOverlay.resumeAfterReload(client);
         }
      }

      @EventTarget
      public void onResourceReload(ResourceReloadEvent event) {
         this.complete(event.getClient());
      }
   }
}

