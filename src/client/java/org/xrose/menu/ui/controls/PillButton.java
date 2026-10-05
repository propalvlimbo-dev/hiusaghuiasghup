package org.xrose.menu.ui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.ui.Component;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;

public final class PillButton extends Component {
   private final Runnable action;
   private String label;
   private float textSize = 11.0F;
   private int idleBackground = Theme.Colors.CONTROL;
   private int hoverBackground = Theme.Colors.SURFACE_HOVER;
   private int textColor = Theme.Colors.SECONDARY;
   private Integer borderColor;
   private float borderThickness = 0.5F;
   private float alpha = 1.0F;
   private int mouseX;
   private int mouseY;

   public PillButton(String label, Runnable action) {
      this.label = label;
      this.action = action == null ? () -> {} : action;
   }

   public PillButton place(Component owner, float x, float y, float width, float height, int mouseX, int mouseY) {
      this.attach(owner, owner.sx(x), owner.sy(y), owner.px(width), owner.px(height));
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      return this;
   }

   public PillButton label(String label) {
      this.label = label;
      return this;
   }

   public PillButton textSize(float textSize) {
      this.textSize = textSize;
      return this;
   }

   public PillButton colors(int idleBackground, int hoverBackground, int textColor) {
      this.idleBackground = idleBackground;
      this.hoverBackground = hoverBackground;
      this.textColor = textColor;
      return this;
   }

   public PillButton border(int color, float thickness) {
      this.borderColor = color;
      this.borderThickness = thickness;
      return this;
   }

   public PillButton alpha(float alpha) {
      this.alpha = alpha;
      return this;
   }

   @Override
   public boolean handleClick(int mouseX, int mouseY) {
      if (!this.contains(mouseX, mouseY)) {
         return false;
      }

      this.action.run();
      return true;
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      boolean hovered = this.contains(this.mouseX, this.mouseY);
      float designX = this.designX(this.x());
      float designY = this.designY(this.y());
      float designWidth = this.designX(this.x() + this.width()) - designX;
      float designHeight = this.designY(this.y() + this.height()) - designY;
      Render2DUtil.rect(this.sx(designX), this.sy(designY), this.px(designWidth), this.px(designHeight))
         .color(this.alpha(hovered ? this.hoverBackground : this.idleBackground, this.alpha))
         .radius(this.px(999.0F))
         .border(
            this.borderColor == null ? Math.max(0.5F, this.px(0.5F)) : Math.max(0.5F, this.px(this.borderThickness)),
            this.borderColor == null ? 0 : this.alpha(this.borderColor, this.alpha)
         )
         .draw();
      this.textCentered(
         designX + designWidth / 2.0F,
         this.centeredTextY(designY + designHeight / 2.0F, this.textSize),
         this.textSize,
         MenuText.ui(this.label),
         this.textColor,
         this.alpha,
         UiFontStyle.MEDIUM
      );
   }
}

