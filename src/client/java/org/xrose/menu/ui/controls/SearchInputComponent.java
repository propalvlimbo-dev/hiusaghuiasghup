package org.xrose.menu.ui.controls;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.menu.ui.Component;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class SearchInputComponent extends Component {
   private static final String PLACEHOLDER = "Start typing to search...";
   private static final int PADDING_X = 16;
   private static final int ICON_SIZE = 16;
   private static final int GAP = 10;
   private static final int TEXT_SIZE = 12;
   private final Supplier<String> querySupplier;
   private final Supplier<String> suggestionSupplier;
   private int mouseX;
   private int mouseY;
   private boolean focused;
   private boolean selected;
   private boolean bare;
   private float cornerRadius = -1.0F;
   private float alpha = 1.0F;
   private float smoothCaretX = -1.0F;
   private float smoothDrawX = -1.0F;
   private float placeholderAlpha = 1.0F;
   private long lastFrameNanos = System.nanoTime();

   public SearchInputComponent(Supplier<String> querySupplier, Supplier<String> suggestionSupplier) {
      this.querySupplier = querySupplier;
      this.suggestionSupplier = suggestionSupplier;
   }

   public SearchInputComponent place(Component owner, int x, int y, int width, int height, int mouseX, int mouseY) {
      this.attach(owner, owner.sx(x), owner.sy(y), owner.px(width), owner.px(height));
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      return this;
   }

   public SearchInputComponent alpha(float alpha) {
      this.alpha = alpha;
      return this;
   }

   public SearchInputComponent focused(boolean focused) {
      this.focused = focused;
      return this;
   }

   public SearchInputComponent selected(boolean selected) {
      this.selected = selected;
      return this;
   }

   public SearchInputComponent bare(boolean bare) {
      this.bare = bare;
      return this;
   }

   public SearchInputComponent cornerRadius(float radius) {
      this.cornerRadius = radius;
      return this;
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      long nowNanos = System.nanoTime();
      float seconds = Math.min(0.05F, (float)(nowNanos - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = nowNanos;
      float dtFactor = 1.0F - (float)Math.exp(-seconds * 24.0F);
      String query = this.querySupplier.get();
      if (query == null) {
         query = "";
      }

      String suggestion = this.suggestionSupplier.get();
      if (suggestion == null || query.isEmpty()) {
         suggestion = "";
      }

      boolean hasInput = !query.isEmpty();
      boolean active = this.focused || !hasInput && this.contains(this.mouseX, this.mouseY);
      int background = active ? Theme.Colors.OUTLINES_LARGE : Theme.Colors.OUTLINES_MEDIUM;
      int border = active ? Theme.Colors.OUTLINES_LARGE : Theme.Colors.OUTLINES_SMALL;
      UiFontStyle textStyle = hasInput ? UiFontStyle.MEDIUM : UiFontStyle.REGULAR;
      float textSize = this.px(12.0F);
      float letterSpacing = textStyle.letterSpacingEm() * textSize;
      float textX = this.x() + this.px(42.0F);
      float maxTextWidth = Math.max(1.0F, this.width() - this.px(58.0F));
      float targetPlaceholderAlpha = !hasInput && !this.focused ? 1.0F : 0.0F;
      this.placeholderAlpha = this.placeholderAlpha + (targetPlaceholderAlpha - this.placeholderAlpha) * dtFactor;
      float queryWidth = hasInput ? UiFonts.sfProDisplay().measureWidth(query, textSize, letterSpacing) : 0.0F;
      float suggestionWidth = suggestion.isEmpty() ? 0.0F : UiFonts.sfProDisplay().measureWidth(suggestion, textSize, letterSpacing);
      float joinSpacing = hasInput && !suggestion.isEmpty() ? letterSpacing : 0.0F;
      float visualTextWidth = queryWidth + joinSpacing + suggestionWidth;
      float targetDrawX = visualTextWidth > maxTextWidth ? textX - (visualTextWidth - maxTextWidth) : textX;
      if (this.smoothDrawX < 0.0F) {
         this.smoothDrawX = targetDrawX;
      } else {
         this.smoothDrawX = this.smoothDrawX + (targetDrawX - this.smoothDrawX) * dtFactor;
      }

      float drawX = this.smoothDrawX;
      float iconY = this.y() + (this.height() - this.px(16.0F)) / 2.0F;
      float textY = UiFonts.sfProDisplay().centeredTextY(this.y() + this.height() / 2.0F, textSize);
      if (!this.bare) {
         float radius = this.cornerRadius >= 0.0F ? this.px(this.cornerRadius) : Math.max(1.0F, this.height() / 2.0F);
         Render2DUtil.rect(this.x(), this.y(), this.width(), this.height())
            .color(ColorUtil.multiplyAlpha(background, this.alpha))
            .radius(radius)
            .border(Math.max(0.5F, this.px(0.5F)), ColorUtil.multiplyAlpha(border, this.alpha))
            .draw();
      }

      Render2DUtil.texture(this.x() + this.px(16.0F), iconY, this.px(16.0F), this.px(16.0F), Textures.Header.SEARCH)
         .color(ColorUtil.multiplyAlpha(!active && !hasInput ? Theme.Colors.ICON : -1, this.alpha))
         .draw();
      Render2DUtil.pushScissor(textX, this.y() + this.px(12.0F), maxTextWidth, Math.max(1.0F, this.height() - this.px(24.0F)));
      if (this.placeholderAlpha > 0.01F) {
         float placeholderOffset = (1.0F - this.placeholderAlpha) * this.px(-6.0F);
         Render2DUtil.text(textX + placeholderOffset, textY, textSize, "Start typing to search...")
            .style(UiFontStyle.REGULAR)
            .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, this.alpha * this.placeholderAlpha))
            .draw();
      }

      if (this.focused && this.selected && hasInput) {
         Render2DUtil.rect(drawX, this.y() + this.px(12.0F), queryWidth, Math.max(1.0F, this.height() - this.px(24.0F)))
            .color(ColorUtil.multiplyAlpha(Theme.getAccent(), this.alpha * 0.55F))
            .radius(this.px(2.0F))
            .draw();
      }

      if (hasInput) {
         Render2DUtil.text(drawX, textY, textSize, query).style(textStyle).color(ColorUtil.multiplyAlpha(-1, this.alpha)).draw();
      }

      if (!suggestion.isEmpty()) {
         Render2DUtil.text(drawX + queryWidth + joinSpacing, textY, textSize, suggestion)
            .style(textStyle)
            .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, this.alpha * 0.7F))
            .draw();
      }

      if (this.focused && !this.selected) {
         float targetCaretX = drawX + queryWidth + this.px(2.0F);
         if (this.smoothCaretX < 0.0F) {
            this.smoothCaretX = targetCaretX;
         } else {
            this.smoothCaretX = this.smoothCaretX + (targetCaretX - this.smoothCaretX) * dtFactor;
         }

         float blinkPhase = (float)(System.currentTimeMillis() % 1000L) / 1000.0F;
         float caretAlphaMult = 0.5F + 0.5F * (float)Math.cos(blinkPhase * Math.PI * 2.0);
         if (caretAlphaMult > 0.05F) {
            Render2DUtil.rect(this.smoothCaretX, this.y() + this.px(16.0F), Math.max(1.0F, this.px(1.5F)), this.px(16.0F))
               .color(ColorUtil.multiplyAlpha(-1, this.alpha * caretAlphaMult))
               .draw();
         }
      } else {
         this.smoothCaretX = -1.0F;
      }

      Render2DUtil.popScissor();
   }
}

