package org.xrose.hud;

import net.minecraft.client.Minecraft;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class CoordsElement extends HudElement {
   private static final float PADDING = 8.0F;
   private static final float TEXT_SIZE = 10.0F;
   private static final float CHIP_GAP = 2.5F;
   private static final float CHIP_RADIUS = 6.0F;
   private static final float CHIP_PAD_Y = 3.5F;
   private static final float LABEL_VALUE_GAP = 4.0F;
   private static final String[] AXES = new String[]{"X", "Y", "Z"};
   private final String[] values = new String[3];
   private final float[] chipWidths = new float[3];

   public CoordsElement() {
      super("coords", "Coordinates");
   }

   @Override
   protected float defaultY(float unit) {
      return 72.0F * unit;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.values[0] = coordinate(mc.player.getX());
      this.values[1] = coordinate(mc.player.getY());
      this.values[2] = coordinate(mc.player.getZ());
      MsdfFont font = UiFonts.sfProDisplay();
      float textSize = 10.0F * unit;
      float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float total = 0.0F;

      for (int i = 0; i < AXES.length; i++) {
         this.chipWidths[i] = font.measureWidth(AXES[i], textSize, letterSpacing) + 4.0F * unit + font.measureWidth(this.values[i], textSize, letterSpacing);
         total += this.chipWidths[i];
      }

      this.width = 16.0F * unit + total + (AXES.length - 1) * 2.5F * unit;
      this.height = 33.0F * unit;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      float alpha = this.appearAlpha();
      this.hudBackground(alpha, unit, 9.0F * unit);
      MsdfFont font = UiFonts.sfProDisplay();
      float textSize = 10.0F * unit;
      float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float centerY = this.y + this.height / 2.0F;
      float textY = font.centeredTextY(centerY, textSize);
      float chipX = this.x + 8.0F * unit;
      float chipHeight = 17.0F * unit;
      float chipY = centerY - chipHeight / 2.0F;

      for (int i = 0; i < AXES.length; i++) {
         float chipWidth = this.chipWidths[i];
         Render2DUtil.rect(chipX, chipY, chipWidth, chipHeight)
            .color(ColorUtil.multiplyAlpha(ColorUtil.withAlpha(-16777216, 60), alpha))
            .radius(6.0F * unit)
            .draw();
         float cursor = chipX;
         Render2DUtil.text(cursor, textY, textSize, AXES[i]).style(UiFontStyle.MEDIUM).color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha)).draw();
         cursor += font.measureWidth(AXES[i], textSize, letterSpacing) + 4.0F * unit;
         Render2DUtil.text(cursor, textY, textSize, this.values[i]).style(UiFontStyle.MEDIUM).color(ColorUtil.multiplyAlpha(-1, alpha)).draw();
         chipX += chipWidth + 2.5F * unit;
      }
   }

   private static String coordinate(double value) {
      return Integer.toString((int)Math.floor(value));
   }
}

