package org.xrose.menu.ui.rows;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.feature.setting.ButtonSetting;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.controls.ButtonComponent;
import org.xrose.utils.render.Theme;

public final class ButtonRow extends SettingRow {
   private final ButtonComponent button;

   public ButtonRow(ButtonSetting setting) {
      super(setting);
      this.button = new ButtonComponent(setting.getButtonLabel(), setting::press);
   }

   @Override
   protected void placeControl(Component owner) {
      this.button.place(owner, this.controlX(), this.controlY(), this.controlWidth(), this.controlHeight(), this.mouseX, this.mouseY).alpha(this.rowAlpha);
   }

   @Override
   public boolean click(int mouseX, int mouseY) {
      this.host.closeOtherRows(this);
      return this.button.handleClick(mouseX, mouseY);
   }

   @Override
   protected void renderControl(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      this.button.render(minecraft, guiGraphicsExtractor);
   }

   @Override
   protected int labelColor() {
      return Theme.Colors.TEXT_TEXT;
   }

   @Override
   protected int controlWidth() {
      return 128;
   }

   @Override
   protected int controlHeight() {
      return 24;
   }
}

