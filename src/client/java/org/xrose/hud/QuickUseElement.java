package org.xrose.hud;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.xrose.context.RenderContext;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.misc.ServerHelperFeature;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.TextAlign;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class QuickUseElement extends HudElement {
   private static final int MAX_COLUMNS = 6;
   private static final float PADDING = 5.0F;
   private static final float CELL_SIZE = 31.0F;
   private static final float CELL_GAP = 2.5F;
   private static final float ITEM_SIZE = 20.0F;
   private static final float PANEL_RADIUS = 10.0F;
   private static final float CELL_RADIUS = 5.0F;
   private static final float KEY_TEXT_SIZE = 7.5F;
   private static final float COUNT_TEXT_SIZE = 8.0F;
   private List<ServerHelperFeature.QuickUseEntry> entries = List.of();
   private int columns;

   public QuickUseElement() {
      super("quick_use", "QuickUse");
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      ServerHelperFeature helper = FeatureManager.INSTANCE.getEnabled(ServerHelperFeature.class);
      LocalPlayer player = mc.player;
      this.entries = helper != null && player != null ? helper.quickUseEntries(player) : List.of();
      if (this.entries.isEmpty()) {
         this.columns = 0;
         this.width = 0.0F;
         this.height = 0.0F;
      } else {
         this.columns = Math.min(6, this.entries.size());
         int rows = (this.entries.size() + this.columns - 1) / this.columns;
         this.width = (10.0F + this.columns * 31.0F + Math.max(0, this.columns - 1) * 2.5F) * unit;
         this.height = (10.0F + rows * 31.0F + Math.max(0, rows - 1) * 2.5F) * unit;
      }
   }

   @Override
   protected float defaultY(float unit) {
      return 220.0F * unit;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      if (!this.entries.isEmpty() && this.columns > 0 && mc.player != null) {
         float alpha = this.appearAlpha();
         this.hudBackground(alpha, unit, 10.0F * unit);
         float cellSize = 31.0F * unit;
         float cellGap = 2.5F * unit;
         float cellsX = this.x + 5.0F * unit;
         float cellsY = this.y + 5.0F * unit;

         for (int index = 0; index < this.entries.size(); index++) {
            int column = index % this.columns;
            int row = index / this.columns;
            float cellX = cellsX + column * (cellSize + cellGap);
            float cellY = cellsY + row * (cellSize + cellGap);
            this.glassChip(cellX, cellY, cellSize, cellSize, 5.0F * unit, alpha, unit);
         }

         if (!(alpha < 0.75F)) {
            GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
            if (extractor != null) {
               Render2DUtil.flush();
               Matrix3x2fStack pose = extractor.pose();
               float guiScale = mc.getWindow().getGuiScale();
               float itemSize = 20.0F * unit;
               float itemScale = itemSize / 16.0F;
               MsdfFont keyFont = UiFonts.sfPro(UiFontStyle.SEMIBOLD.weight());
               MsdfFont countFont = UiFonts.sfPro(UiFontStyle.SEMIBOLD.weight());
               float keySize = 7.5F * unit;
               float countSize = 8.0F * unit;
               float keySpacing = keySize * UiFontStyle.SEMIBOLD.letterSpacingEm();

               for (int index = 0; index < this.entries.size(); index++) {
                  ServerHelperFeature.QuickUseEntry entry = this.entries.get(index);
                  int column = index % this.columns;
                  int row = index / this.columns;
                  float cellX = cellsX + column * (cellSize + cellGap);
                  float cellY = cellsY + row * (cellSize + cellGap);
                  ItemStack icon = entry.icon();
                  if (!icon.isEmpty()) {
                     float itemX = cellX + (cellSize - itemSize) / 2.0F;
                     float itemY = cellY + (cellSize - itemSize) / 2.0F;
                     itemX = Math.round(itemX * guiScale) / guiScale;
                     itemY = Math.round(itemY * guiScale) / guiScale;
                     pose.pushMatrix();
                     pose.translate(itemX, itemY);
                     pose.scale(itemScale);
                     extractor.item(mc.player, icon, 0, 0, index + 1);
                     extractor.itemDecorations(mc.font, icon, 0, 0, "");
                     pose.popMatrix();
                  }

                  String bind = compactBind(entry.bindLabel());
                  bind = keyFont.ellipsize(bind, keySize, keySpacing, cellSize - 6.0F * unit);
                  Render2DUtil.text(cellX + 3.0F * unit, cellY + 2.0F * unit, keySize, bind).style(UiFontStyle.SEMIBOLD).color(-1).draw();
                  if (entry.count() != 1) {
                     String count = Integer.toString(entry.count());
                     float countRight = cellX + cellSize - 2.0F * unit;
                     float countBottom = cellY + cellSize - 2.0F * unit;
                     Render2DUtil.text(countRight, countBottom - countFont.textHeight(countSize), countSize, count)
                        .style(UiFontStyle.SEMIBOLD)
                        .color(entry.count() > 0 ? -1 : Theme.Colors.TEXT_GHOST)
                        .align(TextAlign.RIGHT)
                        .draw();
                  }
               }
            }
         }
      }
   }

   private static String compactBind(String label) {
      return switch (label) {
         case "Mouse Left" -> "M1";
         case "Mouse Right" -> "M2";
         case "Mouse Middle" -> "M3";
         default -> label != null && label.startsWith("Mouse ") ? "M" + label.substring("Mouse ".length()) : label;
      };
   }
}

