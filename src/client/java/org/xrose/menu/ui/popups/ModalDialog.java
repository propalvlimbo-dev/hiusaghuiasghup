package org.xrose.menu.ui.popups;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.controls.InputComponent;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.math.Animation;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;

public final class ModalDialog extends Component {
   public static final int STRIP_WIDTH = 460;
   public static final int STRIP_HEIGHT = 52;
   private static final int MODAL_X = 282;
   private static final int MODAL_Y = 294;
   private static final long OPEN_ANIMATION_MS = 170L;
   private static final int GLASS_BG = ColorUtil.rgba(10, 12, 18, 120);
   private static final int GLASS_BORDER = ColorUtil.rgba(0, 0, 0, 110);
   private static final int GLASS_SHADOW = ColorUtil.rgba(0, 0, 0, 80);
   private static final float GLASS_RADIUS = 9.0F;
   private static final float CHIP_BG = 60.0F;
   private static final float CHIP_BG_HOVER = 95.0F;
   private static final int PAD_X = 16;
   private static final int INPUT_HEIGHT = 30;
   private static final int BUTTON_HEIGHT = 30;
   private static final int SAVE_WIDTH = 66;
   private static final int CANCEL_WIDTH = 58;
   private static final int BUTTON_GAP = 10;
   private static final int INPUT_BUTTONS_GAP = 14;
   private final Identifier titleIcon;
   private final String title;
   private final InputComponent input;
   private final Runnable onSave;
   private final ModalDialog.Extras extras;
   private final Animation openAnimation = new Animation(170L, Animation.Easing.EASE_OUT_QUAD);
   private boolean open;
   private float pageAlpha = 1.0F;
   private int inputRightInset;
   private float fadeAlpha = 1.0F;
   private float riseOffset;
   private int mouseX;
   private int mouseY;

   public ModalDialog(Identifier titleIcon, String title, String[] descriptionLines, InputComponent input, Runnable onSave, ModalDialog.Extras extras) {
      this.titleIcon = titleIcon;
      this.title = title;
      this.input = input;
      this.onSave = onSave;
      this.extras = extras == null ? new ModalDialog.Extras() {} : extras;
      this.openAnimation.animate(0.0F, 0.0F, 0L, Animation.Easing.EASE_OUT_QUAD);
   }

   public ModalDialog inputRightInset(int inset) {
      this.inputRightInset = inset;
      return this;
   }

   public void place(Component owner, int mouseX, int mouseY, float pageAlpha) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.pageAlpha = pageAlpha;
      float eased = Animation.Easing.EASE_OUT_QUAD.ease(this.openAnimation.getValue());
      this.fadeAlpha = pageAlpha * eased;
      this.riseOffset = (1.0F - eased) * 12.0F;
      this.attach(owner, owner.sx(282.0F), owner.sy(294.0F + this.riseOffset), owner.px(460.0F), owner.px(52.0F));
      if (this.open && !(this.fadeAlpha <= 0.01F)) {
         this.input.place(owner, this.inputX(), this.inputY(), this.placedInputWidth(), 30, mouseX, mouseY).alpha(this.fadeAlpha);
      }
   }

   public void open() {
      this.open = true;
      this.openAnimation.animate(0.0F, 1.0F, 170L, Animation.Easing.EASE_OUT_QUAD);
      this.input.focus();
   }

   public void close() {
      this.open = false;
      this.input.blur();
   }

   public boolean isOpen() {
      return this.open;
   }

   @Override
   public boolean handleClick(int mouseX, int mouseY) {
      if (!this.contains(mouseX, mouseY)) {
         this.close();
         return true;
      } else if (this.extras.click(this, mouseX, mouseY)) {
         return true;
      } else if (this.input.handleClick(mouseX, mouseY)) {
         return true;
      } else if (this.hit(mouseX, mouseY, this.saveX(), this.buttonY(), 66.0F, 30.0F)) {
         this.onSave.run();
         return true;
      } else if (this.hit(mouseX, mouseY, this.cancelX(), this.buttonY(), 58.0F, 30.0F)) {
         this.close();
         return true;
      } else {
         return true;
      }
   }

   public boolean handleKey(int key) {
      if (!this.open) {
         return false;
      } else if (key == 256) {
         this.close();
         return true;
      } else if (key != 257 && key != 335) {
         this.input.handleKey(key);
         return true;
      } else {
         this.onSave.run();
         return true;
      }
   }

   public boolean handleCharacter(int codePoint) {
      return this.open && this.input.handleCharacter(codePoint);
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      if (this.open && !(this.fadeAlpha <= 0.01F)) {
         Component owner = this.frame();
         Render2DUtil.rect(owner.x() - owner.px(180.0F), owner.y(), owner.width() + owner.px(180.0F), owner.height())
            .color(ColorUtil.withAlpha(Theme.Colors.OVERLAY, Math.round(140.0F * this.fadeAlpha)))
            .radius(0.0F, 0.0F, owner.px(12.0F), owner.px(12.0F))
            .draw();
         Render2DUtil.rect(this.x(), this.y(), this.width(), this.height())
            .color(ColorUtil.multiplyAlpha(GLASS_BG, this.fadeAlpha))
            .radius(this.px(9.0F))
            .border(Math.max(0.5F, this.px(1.0F)), ColorUtil.multiplyAlpha(GLASS_BORDER, this.fadeAlpha))
            .blur(this.px(8.0F), this.fadeAlpha)
            .shadow(ColorUtil.multiplyAlpha(GLASS_SHADOW, this.fadeAlpha), this.px(8.0F))
            .draw();
         this.input.render(minecraft, guiGraphicsExtractor);
         this.renderButtons();
         this.extras.render(this, this.fadeAlpha);
      }
   }

   private void renderButtons() {
      int buttonY = this.buttonY();
      boolean cancelHovered = this.hit(this.mouseX, this.mouseY, this.cancelX(), buttonY, 58.0F, 30.0F);
      this.rect(this.cancelX(), buttonY, 58.0F, 30.0F, this.withBlackAlpha(cancelHovered ? 95.0F : 60.0F), 8.0F, this.fadeAlpha);
      this.outline(this.cancelX(), buttonY, 58.0F, 30.0F, Theme.Colors.OUTLINES_SMALL, 8.0F, 0.5F, this.fadeAlpha * 0.8F);
      this.textCentered(
         this.cancelX() + 29.0F,
         this.centeredTextY(buttonY + 15.0F, 11.0F),
         11.0F,
         MenuText.ui("Cancel"),
         cancelHovered ? Theme.Colors.TEXT_TEXT : Theme.Colors.SECONDARY,
         this.fadeAlpha,
         UiFontStyle.MEDIUM
      );
      boolean saveHovered = this.hit(this.mouseX, this.mouseY, this.saveX(), buttonY, 66.0F, 30.0F);
      this.rect(this.saveX(), buttonY, 66.0F, 30.0F, Theme.getAccent(), 8.0F, this.fadeAlpha);
      if (saveHovered) {
         this.rect(this.saveX(), buttonY, 66.0F, 30.0F, Theme.Colors.OUTLINES_LARGE, 8.0F, this.fadeAlpha);
      }

      this.textCentered(this.saveX() + 33.0F, this.centeredTextY(buttonY + 15.0F, 11.0F), 11.0F, MenuText.ui("Save"), -1, this.fadeAlpha, UiFontStyle.MEDIUM);
   }

   private int withBlackAlpha(float alphaValue) {
      return ColorUtil.withAlpha(-16777216, Math.round(alphaValue));
   }

   private int inputX() {
      return 298;
   }

   private int inputWidth() {
      return Math.max(120, this.cancelX() - 14 - this.inputX());
   }

   private int placedInputWidth() {
      return Math.max(120, this.inputWidth() - this.inputRightInset);
   }

   public int inputY() {
      return 305;
   }

   private int buttonY() {
      return 305;
   }

   private int saveX() {
      return 660;
   }

   private int cancelX() {
      return this.saveX() - 10 - 58;
   }

   public int footerY() {
      return 353;
   }

   public int centerX() {
      return 512;
   }

   public int contentX() {
      return this.inputX();
   }

   public int contentWidth() {
      return this.placedInputWidth();
   }

   public int headerY() {
      return 294;
   }

   public int inputHeight() {
      return 30;
   }

   public interface Extras {
      default void render(ModalDialog modal, float alpha) {
      }

      default boolean click(ModalDialog modal, int mouseX, int mouseY) {
         return false;
      }
   }
}

