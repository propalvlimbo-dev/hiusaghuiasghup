package org.xrose.menu.pages.configs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.MenuCard;
import org.xrose.menu.ui.controls.IconButton;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class ConfigCard extends MenuCard {
   private static final int CONTENT_GAP = 8;
   private static final int PREVIEW_RADIUS = 6;
   private static final int TEXT_X = 56;
   private static final float TEXT_GAP = 4.0F;
   private final String name;
   private final IconButton pinButton = new IconButton(Textures.Icons.PIN, 16, null);
   private final IconButton loadButton = new IconButton(Textures.Icons.REFRESH_CCW, 16, null);
   private final IconButton deleteButton = new IconButton(Textures.Icons.DELETE_LEFT, 16, null);
   private final ConfigPreviewManager previews = ConfigPreviewManager.INSTANCE;
   private boolean favorite;
   private boolean selected;
   private String dateText = "";

   public ConfigCard(String name, boolean favorite) {
      this.name = name;
      this.favorite = favorite;
   }

   public String name() {
      return this.name;
   }

   public boolean favorite() {
      return this.favorite;
   }

   public void setDateText(String dateText) {
      this.dateText = dateText;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public void toggleFavorite() {
      this.favorite = !this.favorite;
   }

   public void place(Component owner, int x, int y, int mouseX, int mouseY, float alpha) {
      this.placeBounds(owner, x, y, mouseX, mouseY, alpha);
      int deleteSlot = 0;
      int loadSlot = 1;
      int pinSlot = 2;
      float actionY = this.actionRowY();
      int white38 = ColorUtil.withAlpha(-1, 38);
      this.pinButton
         .place(owner, x + this.actionSlotX(pinSlot), actionY, 24, mouseX, mouseY)
         .active(this.favorite)
         .activeTint(this.selected ? -1 : Theme.getAccent())
         .activeBackground(
            this.selected ? white38 : ColorUtil.withAlpha(Theme.getAccent(), 30), this.selected ? white38 : ColorUtil.withAlpha(Theme.getAccent(), 48)
         )
         .hoverBackground(this.selected ? white38 : Theme.Colors.SURFACE_HOVER)
         .tint(this.selected ? -1 : Theme.Colors.ICON, -1)
         .alpha(alpha);
      this.loadButton
         .place(owner, x + this.actionSlotX(loadSlot), actionY, 24, mouseX, mouseY)
         .hoverBackground(this.selected ? white38 : Theme.Colors.SURFACE_HOVER)
         .tint(this.selected ? -1 : Theme.Colors.ICON, -1)
         .alpha(alpha);
      this.deleteButton
         .place(owner, x + this.actionSlotX(deleteSlot), actionY, 24, mouseX, mouseY)
         .hoverBackground(ColorUtil.withAlpha(Theme.Colors.SYSTEM_RED, 28))
         .tint(this.selected ? -1 : Theme.Colors.ICON, Theme.Colors.SYSTEM_RED)
         .alpha(alpha);
   }

   @Override
   public boolean handleClick(int mouseX, int mouseY) {
      return this.contains(mouseX, mouseY);
   }

   public boolean isDeleteAt(int mouseX, int mouseY) {
      return this.deleteButton.contains(mouseX, mouseY);
   }

   public boolean isPinAt(int mouseX, int mouseY) {
      return this.pinButton.contains(mouseX, mouseY);
   }

   public boolean isLoadAt(int mouseX, int mouseY) {
      return this.loadButton.contains(mouseX, mouseY);
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      if (this.selected) {
         this.rect(this.designX, this.designY, 320.0F, 57.0F, Theme.getAccent(), 8.0F, this.alpha);
      } else {
         this.rect(this.designX, this.designY, 320.0F, 57.0F, Theme.Colors.BACKGROUND_SURFACE_S, 8.0F, this.alpha);
         this.outline(this.designX, this.designY, 320.0F, 57.0F, Theme.Colors.OUTLINES_SMALL, 8.0F, 0.5F, this.alpha);
      }

      this.renderPreview(minecraft);
      int textColor = this.selected ? -1 : Theme.Colors.ICON;
      float textMaxWidth = this.actionSlotX(2) - 56 - 8;
      String shownName = UiFonts.sfProDisplay().ellipsize(this.name, 14.0F, 14.0F * UiFontStyle.MEDIUM.letterSpacingEm(), textMaxWidth);
      String shownDate = UiFonts.sfProDisplay().ellipsize(this.dateText, 12.0F, 12.0F * UiFontStyle.REGULAR.letterSpacingEm(), textMaxWidth);
      float nameHeight = UiFonts.sfProDisplay().textHeight(14.0F);
      float dateHeight = UiFonts.sfProDisplay().textHeight(12.0F);
      float blockTop = this.designY + (57.0F - (nameHeight + 4.0F + dateHeight)) / 2.0F;
      this.text(this.designX + 56, blockTop, 14.0F, shownName, textColor, this.alpha, UiFontStyle.MEDIUM);
      this.text(
         this.designX + 56, blockTop + nameHeight + 4.0F, 12.0F, shownDate, this.selected ? textColor : Theme.Colors.SECONDARY, this.alpha, UiFontStyle.REGULAR
      );
      this.pinButton.render(minecraft, guiGraphicsExtractor);
      this.loadButton.render(minecraft, guiGraphicsExtractor);
      this.deleteButton.render(minecraft, guiGraphicsExtractor);
   }

   private void renderPreview(Minecraft minecraft) {
      float x = this.designX + 16;
      float y = this.designY + 12.5F;
      ConfigPreviewManager.Preview preview = this.previews.preview(minecraft, this.name);
      if (preview == null) {
         this.rect(x, y, 32.0F, 32.0F, Theme.Colors.SURFACE_HOVER, 6.0F, this.alpha);
         int ghost = this.alpha(Theme.Colors.ICON_GHOST, this.alpha);
         Render2DUtil.texture(this.sx(x + 8.0F), this.sy(y + 8.0F), this.px(16.0F), this.px(16.0F), Textures.Icons.HARD_DRIVE).color(ghost).draw();
      } else {
         Render2DUtil.texture(this.sx(x), this.sy(y), this.px(32.0F), this.px(32.0F), preview.texture())
            .managed()
            .uv(preview.u0(), preview.v0(), preview.u1(), preview.v1())
            .radius(this.px(6.0F))
            .color(this.alpha(-1, this.alpha))
            .draw();
      }
   }
}

