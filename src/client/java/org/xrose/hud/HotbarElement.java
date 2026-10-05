package org.xrose.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.xrose.context.RenderContext;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.math.MathUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.TextAlign;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class HotbarElement extends HudElement {
   private static final int SLOT_COUNT = 9;
   private static final float PADDING = 5.0F;
   private static final float CELL = 27.5F;
   private static final float CELL_GAP = 2.5F;
   private static final float ITEM_SIZE = 20.0F;
   private static final float OFFHAND_GAP = 6.0F;
   private static final float OFFHAND_SIZE = 32.0F;
   private static final float OFFHAND_RADIUS = 8.0F;
   private static final float BOTTOM_MARGIN = 12.0F;
   private static final float NAME_TEXT_SIZE = 10.0F;
   private static final float ITEM_COUNT_TEXT_SIZE = 8.0F;
   private static final float ITEM_COUNT_RIGHT = 17.0F;
   private static final float ITEM_COUNT_BOTTOM = 17.0F;
   private static final long NAME_FADE_MS = 500L;
   private static final float XP_TEXT_SIZE = 10.0F;
   private static final int XP_COLOR = ColorUtil.rgb(128, 255, 32);
   private static final float VANILLA_HOTBAR_HEIGHT = 22.0F;
   private static final float VANILLA_STATS_ROW_CENTER = 12.5F;
   private static final float VANILLA_NAME_OFFSET = 49.0F;
   private static final float VANILLA_NAME_OFFSET_CREATIVE = 23.0F;
   private static final float STATS_ROW_DROP = 5.0F;
   private static final HotbarElement.Style DEFAULT = new HotbarElement.Style(14.0F);
   private static final HotbarElement.Style MINI = new HotbarElement.Style(10.0F);
   private HotbarElement.Style style = MINI;
   private float screenHeight;
   private float smoothedSlot;
   private boolean slotInitialized;
   private long lastFrameTime;
   private ItemStack lastHighlight = ItemStack.EMPTY;
   private long highlightEnd;
   private float renderedTop = Float.NaN;
   private float statsHalfWidth;

   public HotbarElement() {
      super("hotbar", "Hotbar");
   }

   public void setMini(boolean mini) {
      this.style = mini ? MINI : DEFAULT;
   }

   @Override
   protected boolean centerHorizontally() {
      return true;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.screenHeight = mc.getWindow().getGuiScaledHeight();
      LocalPlayer player = mc.player;
      if (player != null && !player.isSpectator()) {
         this.width = 277.5F * unit;
         this.height = 37.5F * unit;
         this.statsHalfWidth = this.width / 2.0F - 5.0F * unit;
      } else {
         this.width = 0.0F;
         this.height = 0.0F;
         this.statsHalfWidth = 0.0F;
      }
   }

   @Override
   protected float defaultY(float unit) {
      return this.screenHeight - this.height - 12.0F * unit;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      LocalPlayer player = mc.player;
      if (player != null) {
         this.renderedTop = this.y;
         float alpha = this.appearAlpha();
         this.hudBackground(alpha, unit, this.style.radius() * unit);
         float cell = 27.5F * unit;
         float gap = 2.5F * unit;
         float cellsX = this.x + 5.0F * unit;
         float cellsY = this.y + 5.0F * unit;
         float trackRadius = 9.0F * unit;
         Render2DUtil.rect(cellsX, cellsY, 9.0F * cell + 8.0F * gap, cell)
            .color(ColorUtil.multiplyAlpha(ColorUtil.withAlpha(-16777216, 70), alpha))
            .radius(trackRadius)
            .draw();
         float dividerHeight = cell * 0.5F;
         int dividerColor = ColorUtil.multiplyAlpha(DIVIDER_COLOR, alpha);

         for (int slot = 1; slot < 9; slot++) {
            float dividerX = cellsX + slot * (cell + gap) - gap / 2.0F - 0.5F * unit;
            Render2DUtil.rect(dividerX, cellsY + (cell - dividerHeight) / 2.0F, 1.0F * unit, dividerHeight).color(dividerColor).draw();
         }

         float selection = this.smoothSelection(player.getInventory().getSelectedSlot());
         float selectionX = cellsX + selection * (cell + gap);
         float border = Math.max(0.5F, 1.25F * unit);
         Render2DUtil.rect(selectionX + border / 2.0F, cellsY + border / 2.0F, cell - border, cell - border)
            .color(ColorUtil.multiplyAlpha(ColorUtil.withAlpha(Theme.getAccent(), 22), alpha))
            .radius(trackRadius)
            .border(border, ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
            .shadow(ColorUtil.multiplyAlpha(Theme.getAccent(), 0.22F * alpha), Math.max(1.0F, 3.0F * unit))
            .draw();
         this.drawXpLevel(mc, player, unit, alpha);
         this.drawSelectedItemName(mc, player, unit, alpha);
         ItemStack offhand = player.getOffhandItem();
         float offhandX = Float.NaN;
         if (!offhand.isEmpty()) {
            float capsuleSize = 32.0F * unit;
            float capsuleY = this.y + (this.height - capsuleSize) / 2.0F;
            boolean left = player.getMainArm().getOpposite() == HumanoidArm.LEFT;
            float capsuleX = left ? this.x - 6.0F * unit - capsuleSize : this.x + this.width + 6.0F * unit;
            this.hudBackground(capsuleX, capsuleY, capsuleSize, capsuleSize, alpha, unit, 8.0F * unit);
            offhandX = capsuleX + (capsuleSize - 20.0F * unit) / 2.0F;
         }

         if (!(alpha < 0.75F)) {
            GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
            if (extractor != null) {
               Render2DUtil.flush();
               float itemInset = (cell - 20.0F * unit) / 2.0F;
               float itemY = cellsY + itemInset;
               int seed = 1;

               for (int slot = 0; slot < 9; slot++) {
                  this.drawItem(mc, extractor, player, player.getInventory().getItem(slot), cellsX + slot * (cell + gap) + itemInset, itemY, unit, seed++);
               }

               if (!offhand.isEmpty()) {
                  this.drawItem(mc, extractor, player, offhand, offhandX, this.y + (this.height - 20.0F * unit) / 2.0F, unit, seed);
               }
            }
         }
      }
   }

   public float decorationOffset(Minecraft mc) {
      return Float.isNaN(this.renderedTop) ? 0.0F : this.renderedTop - (mc.getWindow().getGuiScaledHeight() - 22.0F) + bandDrop(mc.player);
   }

   private static float bandDrop(LocalPlayer player) {
      boolean bandFree = player != null && player.jumpableVehicle() == null && !player.connection.getWaypointManager().hasWaypoints();
      return bandFree ? 5.0F : 0.0F;
   }

   public float statsHalfWidth() {
      return this.statsHalfWidth;
   }

   private void drawXpLevel(Minecraft mc, LocalPlayer player, float unit, float alpha) {
      if (mc.gameMode.hasExperience() && player.experienceLevel > 0) {
         MsdfFont font = UiFonts.sfProDisplay();
         String level = Integer.toString(player.experienceLevel);
         float textSize = 10.0F * unit;
         float centerX = this.x + this.width / 2.0F;
         float centerY = this.y - 12.5F + bandDrop(player);
         Render2DUtil.text(centerX, font.centeredTextY(centerY, textSize), textSize, level)
            .style(UiFontStyle.MEDIUM)
            .color(ColorUtil.multiplyAlpha(XP_COLOR, alpha))
            .align(TextAlign.CENTER)
            .draw();
      }
   }

   private void drawSelectedItemName(Minecraft mc, LocalPlayer player, float unit, float panelAlpha) {
      float alpha = this.highlightAlpha(mc, player) * panelAlpha;
      if (!(alpha <= 0.01F) && !this.lastHighlight.isEmpty()) {
         String name = this.lastHighlight.getHoverName().getString();
         MsdfFont font = UiFonts.sfProDisplay();
         float textSize = 10.0F * unit;
         float centerX = this.x + this.width / 2.0F;
         float nameOffset = mc.gameMode.canHurtPlayer() ? 49.0F : 23.0F;
         float centerY = this.y - nameOffset + bandDrop(player) + textSize / 2.0F;
         TextColor rarityColor = TextColor.fromLegacyFormat(this.lastHighlight.getRarity().color());
         int color = rarityColor != null ? 0xFF000000 | rarityColor.getValue() : -1;
         Render2DUtil.text(centerX, font.centeredTextY(centerY, textSize), textSize, name)
            .style(UiFontStyle.MEDIUM)
            .color(ColorUtil.multiplyAlpha(color, alpha))
            .align(TextAlign.CENTER)
            .draw();
      }
   }

   private float highlightAlpha(Minecraft mc, LocalPlayer player) {
      ItemStack selected = player.getInventory().getSelectedItem();
      long now = System.currentTimeMillis();
      if (selected.isEmpty()) {
         this.highlightEnd = 0L;
      } else if (this.lastHighlight.isEmpty()
         || !selected.is(this.lastHighlight.getItem())
         || !selected.getHoverName().equals(this.lastHighlight.getHoverName())) {
         this.highlightEnd = now + (long)(2000.0 * (Double)mc.options.notificationDisplayTime().get());
      }

      this.lastHighlight = selected;
      return MathUtil.clamp01((float)(this.highlightEnd - now) / 500.0F);
   }

   private void drawItem(Minecraft mc, GuiGraphicsExtractor extractor, LocalPlayer player, ItemStack stack, float x, float y, float unit, int seed) {
      if (!stack.isEmpty()) {
         Matrix3x2fStack pose = extractor.pose();
         float guiScale = mc.getWindow().getGuiScale();
         float intendedSize = 20.0F * unit;
         float snappedSize;
         if (intendedSize < 15.99F) {
            snappedSize = intendedSize;
         } else {
            float texelPixels = Math.max(1.0F, Math.round(intendedSize * guiScale / 16.0F));
            snappedSize = texelPixels * 16.0F / guiScale;
         }

         float scale = snappedSize / 16.0F;
         float centerOffset = (intendedSize - snappedSize) / 2.0F;
         float itemX = Math.round((x + centerOffset) * guiScale) / guiScale;
         float itemY = Math.round((y + centerOffset) * guiScale) / guiScale;
         pose.pushMatrix();
         pose.translate(itemX, itemY);
         pose.scale(scale);
         float pop = popProgress(stack);
         if (pop > 0.0F) {
            float squeeze = 1.0F + pop / 5.0F;
            pose.pushMatrix();
            pose.translate(8.0F, 12.0F);
            pose.scale(1.0F / squeeze, (squeeze + 1.0F) / 2.0F);
            pose.translate(-8.0F, -12.0F);
         }

         extractor.item(player, stack, 0, 0, seed);
         if (pop > 0.0F) {
            pose.popMatrix();
         }

         extractor.itemDecorations(mc.font, stack, 0, 0, "");
         drawItemCount(stack);
         pose.popMatrix();
      }
   }

   private static void drawItemCount(ItemStack stack) {
      if (stack.getCount() != 1) {
         String amount = Integer.toString(stack.getCount());
         MsdfFont font = UiFonts.sfProDisplay();
         float textY = 17.0F - font.textHeight(8.0F);
         Render2DUtil.text(17.0F, textY, 8.0F, amount).font(font).style(UiFontStyle.SEMIBOLD).color(-1).align(TextAlign.RIGHT).draw();
      }
   }

   private static float popProgress(ItemStack stack) {
      if (stack.getPopTime() <= 0) {
         return 0.0F;
      }

      DeltaTracker deltaTracker = RenderContext.currentDeltaTracker();
      float partial = deltaTracker != null ? deltaTracker.getGameTimeDeltaPartialTick(false) : 0.0F;
      return stack.getPopTime() - partial;
   }

   private float smoothSelection(int selectedSlot) {
      long now = System.currentTimeMillis();
      float delta = this.lastFrameTime == 0L ? 0.016F : (float)Math.min(100L, now - this.lastFrameTime) / 1000.0F;
      this.lastFrameTime = now;
      if (!this.slotInitialized) {
         this.slotInitialized = true;
         this.smoothedSlot = selectedSlot;
      }

      float step = MathUtil.clamp01(delta * 20.0F);
      this.smoothedSlot = this.smoothedSlot + (selectedSlot - this.smoothedSlot) * step;
      if (Math.abs(this.smoothedSlot - selectedSlot) < 0.005F) {
         this.smoothedSlot = selectedSlot;
      }

      return this.smoothedSlot;
   }

   private record Style(float radius) {
   }
}

