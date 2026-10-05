package org.xrose.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.xrose.context.RenderContext;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;

public final class ArmorHudElement extends HudElement {
   private static final float PADDING = 5.0F;
   private static final float CELL_SIZE = 27.5F;
   private static final float CELL_GAP = 2.5F;
   private static final float ITEM_SIZE = 20.0F;
   private static final float ITEM_TOP = 2.0F;
   private static final float BAR_WIDTH = 14.0F;
   private static final float BAR_HEIGHT = 2.5F;
   private static final float BAR_BOTTOM = 2.5F;
   private static final int BAR_TRACK_COLOR = ColorUtil.rgba(255, 255, 255, 20);
   private static final float PANEL_RADIUS = 10.0F;
   private static final float CELL_RADIUS = 5.0F;
   private static final float BOTTOM_MARGIN = 82.0F;
   private static final EquipmentSlot[] ARMOR_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private float screenHeight;

   public ArmorHudElement() {
      super("armor_hud", "ArmorHud");
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.screenHeight = mc.getWindow().getGuiScaledHeight();
      this.width = 37.5F * unit;
      this.height = (10.0F + 27.5F * ARMOR_ORDER.length + 2.5F * (ARMOR_ORDER.length - 1)) * unit;
   }

   @Override
   protected float defaultY(float unit) {
      return this.screenHeight - this.height - 82.0F * unit;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      LocalPlayer player = mc.player;
      if (player != null) {
         float alpha = this.appearAlpha();
         this.hudBackground(alpha, unit, 10.0F * unit);
         float cellSize = 27.5F * unit;
         float cellGap = 2.5F * unit;
         float cellsX = this.x + 5.0F * unit;
         float cellsY = this.y + 5.0F * unit;
         int cellTrack = ColorUtil.multiplyAlpha(ColorUtil.withAlpha(-16777216, 70), alpha);

         for (int index = 0; index < ARMOR_ORDER.length; index++) {
            float cellY = cellsY + index * (cellSize + cellGap);
            Render2DUtil.rect(cellsX, cellY, cellSize, cellSize).color(cellTrack).radius(5.0F * unit).draw();
            ItemStack stack = player.getItemBySlot(ARMOR_ORDER[index]);
            drawDurability(stack, cellsX, cellY, cellSize, unit, alpha);
         }

         if (!(alpha < 0.75F)) {
            GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
            if (extractor != null) {
               Render2DUtil.flush();
               Matrix3x2fStack pose = extractor.pose();
               float guiScale = mc.getWindow().getGuiScale();
               float itemSize = 20.0F * unit;
               float itemScale = itemSize / 16.0F;

               for (int index = 0; index < ARMOR_ORDER.length; index++) {
                  ItemStack stack = player.getItemBySlot(ARMOR_ORDER[index]);
                  if (!stack.isEmpty()) {
                     float cellY = cellsY + index * (cellSize + cellGap);
                     float itemX = cellsX + (cellSize - itemSize) / 2.0F;
                     float itemY = cellY + 2.0F * unit;
                     itemX = Math.round(itemX * guiScale) / guiScale;
                     itemY = Math.round(itemY * guiScale) / guiScale;
                     pose.pushMatrix();
                     pose.translate(itemX, itemY);
                     pose.scale(itemScale);
                     extractor.item(stack, 0, 0);
                     pose.popMatrix();
                  }
               }
            }
         }
      }
   }

   private static void drawDurability(ItemStack stack, float cellX, float cellY, float cellSize, float unit, float alpha) {
      if (!stack.isEmpty() && stack.isDamageableItem()) {
         float remaining = Math.clamp((float)(stack.getMaxDamage() - stack.getDamageValue()) / Math.max(1, stack.getMaxDamage()), 0.0F, 1.0F);
         float barWidth = 14.0F * unit;
         float barHeight = 2.5F * unit;
         float barX = cellX + (cellSize - barWidth) / 2.0F;
         float barY = cellY + cellSize - 2.5F * unit - barHeight;
         float barRadius = barHeight / 2.0F;
         Render2DUtil.rect(barX, barY, barWidth, barHeight).color(ColorUtil.multiplyAlpha(BAR_TRACK_COLOR, alpha)).radius(barRadius).draw();
         float fillWidth = barWidth * remaining;
         if (!(fillWidth <= 0.0F)) {
            int color = remaining > 0.5F ? Theme.Colors.TRAFFIC_MAXIMIZE : (remaining > 0.25F ? Theme.Colors.TRAFFIC_MINIMIZE : Theme.Colors.TRAFFIC_CLOSE);
            Render2DUtil.rect(barX, barY, Math.max(barRadius, fillWidth), barHeight)
               .color(ColorUtil.multiplyAlpha(color, alpha))
               .radius(barRadius)
               .shadow(ColorUtil.multiplyAlpha(color, 0.35F * alpha), Math.max(1.0F, 2.0F * unit))
               .draw();
         }
      }
   }
}

