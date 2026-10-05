package org.xrose.hud;

import net.minecraft.client.Minecraft;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.pve.MineHelperFeature;
import org.xrose.menu.i18n.MenuText;
import org.xrose.pve.mining.MineTimer;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class MineTimerElement extends HudElement {
   private static final float PADDING_X = 9.0F;
   private static final float PADDING_Y = 7.0F;
   private static final float TEXT_SIZE = 9.0F;
   private static final float LINE_HEIGHT = 12.0F;
   private static final float LABEL_SIZE = 8.0F;
   private static final float GLIDER_WIDTH = 2.5F;
   private static final float GLIDER_GAP = 7.0F;
   private String mineLabel;
   private String mineValue;
   private String timeLabel;
   private String timeValue;

   public MineTimerElement() {
      super("mine_timer", "Mine Timer");
   }

   @Override
   protected float defaultX(float unit) {
      return 280.0F * unit;
   }

   @Override
   protected float defaultY(float unit) {
      return 24.0F * unit;
   }

   @Override
   protected boolean preservePositionOnContentResize() {
      return true;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      MineHelperFeature helper = FeatureManager.INSTANCE.getFeature(MineHelperFeature.class);
      MineTimer timer = helper == null ? null : helper.getCurrentTimer().orElse(null);
      boolean configured = helper != null && helper.isMineTimerSelected();
      if ((!configured || timer == null) && !showcase(mc)) {
         this.width = 0.0F;
         this.height = 0.0F;
      } else {
         this.mineLabel = MenuText.ui("Next mine");
         this.mineValue = timer == null ? MenuText.ui("Diamond") : timer.nextType();
         this.timeLabel = MenuText.ui("Time left");
         this.timeValue = timer == null ? "01:30" : timer.formattedTime(System.currentTimeMillis());
         MsdfFont font = UiFonts.sfProDisplay();
         float textSize = 9.0F * unit;
         float labelSize = 8.0F * unit;
         float spacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
         float rowWidth = 0.0F;

         for (int i = 0; i < 2; i++) {
            float labelWidth = font.measureWidth(i == 0 ? this.mineLabel : this.timeLabel, labelSize, labelSize * UiFontStyle.MEDIUM.letterSpacingEm());
            float valueWidth = font.measureWidth(i == 0 ? this.mineValue : this.timeValue, textSize, spacing);
            rowWidth = Math.max(rowWidth, labelWidth + 5.0F * unit + valueWidth);
         }

         this.width = 2.5F * unit + 7.0F * unit + 18.0F * unit + rowWidth;
         this.height = 38.0F * unit;
      }
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      float alpha = this.appearAlpha();
      this.hudBackground(alpha, unit, 8.0F * unit);
      MsdfFont font = UiFonts.sfProDisplay();
      float textSize = 9.0F * unit;
      float labelSize = 8.0F * unit;
      float gliderX = this.x + 9.0F * unit;
      float gliderTop = this.y + 10.0F * unit;
      float gliderHeight = this.height - 20.0F * unit;
      Render2DUtil.rect(gliderX, gliderTop, 2.5F * unit, gliderHeight)
         .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
         .radius(2.5F * unit / 2.0F)
         .shadow(ColorUtil.multiplyAlpha(Theme.getAccent(), 0.4F * alpha), Math.max(1.0F, 2.5F * unit))
         .draw();
      float textX = this.x + 9.0F * unit + 2.5F * unit + 7.0F * unit;
      float firstCenterY = this.y + 13.0F * unit;
      Render2DUtil.text(textX, font.centeredTextY(firstCenterY, labelSize), labelSize, this.mineLabel)
         .style(UiFontStyle.MEDIUM)
         .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha))
         .draw();
      float mineValueWidth = font.measureWidth(this.mineValue, textSize, textSize * UiFontStyle.MEDIUM.letterSpacingEm());
      Render2DUtil.text(this.x + this.width - 9.0F * unit - mineValueWidth, font.centeredTextY(firstCenterY, textSize), textSize, this.mineValue)
         .style(UiFontStyle.MEDIUM)
         .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
         .draw();
      float secondCenterY = firstCenterY + 12.0F * unit;
      Render2DUtil.text(textX, font.centeredTextY(secondCenterY, labelSize), labelSize, this.timeLabel)
         .style(UiFontStyle.MEDIUM)
         .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha))
         .draw();
      float timeValueWidth = font.measureWidth(this.timeValue, textSize, textSize * UiFontStyle.MEDIUM.letterSpacingEm());
      Render2DUtil.text(this.x + this.width - 9.0F * unit - timeValueWidth, font.centeredTextY(secondCenterY, textSize), textSize, this.timeValue)
         .style(UiFontStyle.MEDIUM)
         .color(ColorUtil.multiplyAlpha(-1, alpha))
         .draw();
   }
}

