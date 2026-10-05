package org.xrose.menu.core;

import net.minecraft.client.Minecraft;
import org.xrose.utils.math.MathUtil;
import org.xrose.utils.render.gui.ScaleUtil;

public record MenuDimensions(float panelWidth, float panelHeight, float panelRadius, float panelBorder) {
   private static final float DESIGN_WIDTH = 1024.0F;
   private static final float DESIGN_HEIGHT = 640.0F;
   private static final float DESIGN_BORDER = 0.5F;
   private static final float SCREEN_MARGIN = 12.0F;
   public static final float SIDEBAR_WIDTH = 180.0F;

   public MenuDimensions(float panelWidth, float panelHeight, float panelRadius, float panelBorder) {
      if (!(panelWidth <= 0.0F) && !(panelHeight <= 0.0F) && !(panelRadius < 0.0F) && !(panelBorder < 0.0F)) {
         this.panelWidth = panelWidth;
         this.panelHeight = panelHeight;
         this.panelRadius = panelRadius;
         this.panelBorder = panelBorder;
      } else {
         throw new IllegalArgumentException("Invalid menu dimensions");
      }
   }

   public static MenuDimensions resolve(Minecraft minecraft, MenuOverlayState state) {
      double guiScale = minecraft.getWindow().getGuiScale();
      float menuScale = MathUtil.lerp(0.525F, 2.1F, state.uiScaleValue());
      float naturalWidth = Math.max(1.0F, ScaleUtil.toGuiPixels(1024.0F * menuScale, guiScale));
      float naturalHeight = Math.max(1.0F, ScaleUtil.toGuiPixels(640.0F * menuScale, guiScale));
      float availableWidth = Math.max(1.0F, minecraft.getWindow().getGuiScaledWidth() - 24.0F);
      float availableHeight = Math.max(1.0F, minecraft.getWindow().getGuiScaledHeight() - 24.0F);
      float fit = Math.min(1.0F, Math.min(availableWidth / naturalWidth, availableHeight / naturalHeight));
      return new MenuDimensions(
         Math.max(1.0F, naturalWidth * fit),
         Math.max(1.0F, naturalHeight * fit),
         Math.max(0.0F, ScaleUtil.toGuiPixels(state.panelRadius(), guiScale) * fit),
         Math.max(0.1F, ScaleUtil.toGuiPixels(0.5F * menuScale, guiScale) * fit)
      );
   }
}

