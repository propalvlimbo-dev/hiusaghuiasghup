package org.xrose.menu;

import net.minecraft.client.Minecraft;
import org.xrose.context.RenderContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.render.FinalGuiRenderEvent;
import org.xrose.feature.impl.visual.HudFeature;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.render.gui.Render2DUtil;

public final class MenuOverlayRenderHandler {
   @EventTarget
   public void onFinalGuiRender(FinalGuiRenderEvent event) {
      Minecraft minecraft = event.getClient();
      RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());

      try {
         int screenWidth = minecraft.getWindow().getGuiScaledWidth();
         int screenHeight = minecraft.getWindow().getGuiScaledHeight();
         Render2DUtil.beginFrame();
         Render2DUtil.setBackdropBlurScale(HudFeature.glassBlurScale());
         MenuOverlay.render(minecraft, event.getGuiGraphicsExtractor(), screenWidth, screenHeight);
         Render2DUtil.flush();
      } finally {
         RenderContext.exit2D();
      }
   }
}

