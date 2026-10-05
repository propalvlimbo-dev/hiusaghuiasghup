package org.xrose.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.pve.AutoMineFeature;
import org.xrose.feature.impl.pve.PveManagerFeature;
import org.xrose.menu.i18n.MenuText;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class PveStatusElement extends HudElement {
   private static final float PADDING = 8.0F;
   private static final float ICON_SIZE = 14.0F;
   private static final float TITLE_SIZE = 11.0F;
   private static final float ROW_SIZE = 9.0F;
   private static final float ROW_HEIGHT = 13.0F;
   private static final float MIN_WIDTH = 168.0F;
   private static final float GLIDER_WIDTH = 2.5F;
   private static final float GLIDER_GAP = 7.0F;
   private final List<PveStatusElement.Row> rows = new ArrayList<>();

   public PveStatusElement() {
      super("pve_status", "PvE State");
   }

   @Override
   protected float defaultY(float unit) {
      return 170.0F * unit;
   }

   @Override
   protected boolean preservePositionOnContentResize() {
      return true;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.rebuildRows();
      MsdfFont font = UiFonts.sfProDisplay();
      float rowSize = 9.0F * unit;
      float spacing = rowSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float contentWidth = 168.0F * unit;

      for (PveStatusElement.Row row : this.rows) {
         float measured = font.measureWidth(row.label() + "  " + row.value(), rowSize, spacing);
         contentWidth = Math.max(contentWidth, measured + 16.0F * unit);
      }

      this.width = contentWidth + 2.5F * unit + 7.0F * unit;
      this.height = (34.0F + this.rows.size() * 13.0F) * unit;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      float alpha = this.appearAlpha();
      this.hudBackground(alpha, unit, 8.0F * unit);
      float gliderX = this.x + 8.0F * unit;
      float gliderTop = this.y + 12.0F * unit;
      float gliderHeight = this.height - 24.0F * unit;
      Render2DUtil.rect(gliderX, gliderTop, 2.5F * unit, gliderHeight)
         .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
         .radius(2.5F * unit / 2.0F)
         .shadow(ColorUtil.multiplyAlpha(Theme.getAccent(), 0.4F * alpha), Math.max(1.0F, 2.5F * unit))
         .draw();
      float contentX = this.x + 8.0F * unit + 2.5F * unit + 7.0F * unit;
      float contentRight = this.x + this.width - 8.0F * unit;
      MsdfFont font = UiFonts.sfProDisplay();
      float iconSize = 14.0F * unit;
      float titleSize = 11.0F * unit;
      float headerCenterY = this.y + 15.0F * unit;
      Render2DUtil.texture(contentX, headerCenterY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.OPTION)
         .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
         .draw();
      Render2DUtil.text(contentX + 20.0F * unit, font.centeredTextY(headerCenterY, titleSize), titleSize, MenuText.ui("PvE State"))
         .style(UiFontStyle.SEMIBOLD)
         .color(ColorUtil.multiplyAlpha(-1, alpha))
         .draw();
      float cursorY = this.y + 26.0F * unit;
      float rowSize = 9.0F * unit;

      for (PveStatusElement.Row row : this.rows) {
         float centerY = cursorY + 13.0F * unit / 2.0F;
         Render2DUtil.text(contentX, font.centeredTextY(centerY, rowSize), rowSize, row.label())
            .style(UiFontStyle.MEDIUM)
            .color(ColorUtil.multiplyAlpha(row.accent() ? Theme.getAccent() : Theme.Colors.TEXT_TEXT, alpha))
            .draw();
         float valueWidth = font.measureWidth(row.value(), rowSize, rowSize * UiFontStyle.MEDIUM.letterSpacingEm());
         Render2DUtil.text(contentRight - valueWidth, font.centeredTextY(centerY, rowSize), rowSize, row.value())
            .style(UiFontStyle.MEDIUM)
            .color(ColorUtil.multiplyAlpha(Theme.Colors.SECONDARY_DARK, alpha))
            .draw();
         cursorY += 13.0F * unit;
      }
   }

   private void rebuildRows() {
      this.rows.clear();
      AutoMineFeature autoMine = FeatureManager.INSTANCE.getFeature(AutoMineFeature.class);
      if (autoMine == null) {
         this.rows.add(new PveStatusElement.Row("AutoMine", MenuText.ui("In development"), false));
      } else {
         AutoMineFeature.Status status = autoMine.currentStatus();
         this.rows.add(new PveStatusElement.Row("AutoMine", status.active() ? displayPhase(status.phase()) : MenuText.ui("Disabled"), status.active()));
         this.rows.add(new PveStatusElement.Row(MenuText.ui("Mined ores"), Integer.toString(status.minedOres()), false));
         this.rows.add(new PveStatusElement.Row(MenuText.ui("Known targets"), Integer.toString(status.knownTargets()), false));
         this.rows.add(new PveStatusElement.Row(MenuText.ui("Elapsed"), formatDuration(status.elapsedMillis()), false));
         this.rows.add(new PveStatusElement.Row(MenuText.ui("Mine profile"), status.profile(), false));
         this.rows.add(new PveStatusElement.Row(MenuText.ui("Tool profile"), status.toolProfile(), false));
      }

      for (Feature feature : FeatureManager.INSTANCE.getFeatures(FeatureCategory.PVE)) {
         if (feature != autoMine && feature != PveManagerFeature.INSTANCE && feature.isEnabled()) {
            this.rows.add(new PveStatusElement.Row(feature.getName(), MenuText.ui("In development"), false));
         }
      }
   }

   private static String formatDuration(long millis) {
      long totalSeconds = Math.max(0L, millis / 1000L);
      long hours = totalSeconds / 3600L;
      long minutes = totalSeconds % 3600L / 60L;
      long seconds = totalSeconds % 60L;
      return hours > 0L ? String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds) : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
   }

   private static String displayPhase(String phase) {
      if (phase != null && phase.startsWith("Paused:")) {
         String reason = phase.substring("Paused:".length()).trim();
         return MenuText.ui("Paused") + ": " + MenuText.ui(reason);
      } else {
         return MenuText.ui(phase);
      }
   }

   private record Row(String label, String value, boolean accent) {
   }
}

