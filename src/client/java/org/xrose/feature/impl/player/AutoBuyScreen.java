package org.xrose.feature.impl.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import org.xrose.context.MinecraftContext;
import org.xrose.context.RenderContext;
import org.xrose.event.EventManager;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.CharacterInputEvent;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.event.events.render.FinalGuiRenderEvent;
import org.xrose.event.events.screen.ScreenKeyEvent;
import org.xrose.event.events.screen.ScreenMouseButtonEvent;
import org.xrose.feature.FeatureManager;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFonts;

public final class AutoBuyScreen extends Screen implements MinecraftContext {
   private static final float PANEL_W = 480.0F;
   private static final float PANEL_H = 282.0F;
   private static final float CELL = 24.0F;
   private static final int COLS = 16;
   private static final int ROWS = 5;
   private static final int PAGE_SIZE = 80;
   private static final float PANEL_RADIUS = 6.0F;
   private static final int COLOR_TEXT = -855307;
   private static final int COLOR_DIM = -7434599;
   private final Screen parent;
   private final AutoBuyFeature feature = AutoBuyScreen.FeatureHolder.FEATURE;
   private final List<AutoBuyFeature.Catalog> visible = new ArrayList<>();
   private String search = "";
   private String price = "";
   private boolean editingSearch;
   private boolean editingPrice;
   private String editingPriceFor = null;
   private int page;
   private float mouseX = -1.0F;
   private float mouseY = -1.0F;
   private boolean wasMenuOpen;
   private float historyScroll = 0.0F;

   private AutoBuyScreen(Screen parent) {
      super(Component.literal("AutoBuy Selector"));
      this.parent = parent;
      this.refresh();
   }

   public static void open() {
      mc.gui.setScreen(new AutoBuyScreen(mc.gui.screen()));
   }

   private void refresh() {
      this.visible.clear();
      String query = AutoBuyFeature.stripDecoration(this.search);

      for (AutoBuyFeature.Catalog item : AutoBuyFeature.Catalog.values()) {
         if (query.isBlank() || AutoBuyFeature.stripDecoration(item.displayName).contains(query)) {
            this.visible.add(item);
         }
      }

      int maxPage = Math.max(0, (this.visible.size() - 1) / 80);
      if (this.page > maxPage) {
         this.page = maxPage;
      }
   }

   protected void init() {
      EventManager.subscribe(this);
      this.price = this.feature.maxPriceValue();
      this.wasMenuOpen = MenuOverlay.isOpen();
      if (this.wasMenuOpen) {
         MenuOverlay.suspendForReload(mc);
      }
   }

   public void onClose() {
      if (this.editingPriceFor != null && !this.price.isBlank()) {
         try {
            long v = Long.parseLong(this.price.replaceAll("[^0-9]", ""));
            if (v > 0L) {
               this.feature.setParsedPrice(this.editingPriceFor, v);
            }
         } catch (Exception var3) {
         }
      }

      EventManager.unsubscribe(this);
      boolean resume = this.wasMenuOpen;
      mc.gui.setScreen(this.parent);
      if (resume) {
         MenuOverlay.resumeAfterReload(mc);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   @EventTarget(priority = -1000)
   public void onFinalGuiRender(FinalGuiRenderEvent event) {
      if (mc.gui.screen() == this && event.isRenderScreen()) {
         this.mouseX = event.getMouseX();
         this.mouseY = event.getMouseY();
         RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
         Render2DUtil.beginFrame();

         try {
            this.draw(event.getMouseX(), event.getMouseY());
         } finally {
            Render2DUtil.flush();
            RenderContext.exit2D();
         }
      }
   }

   private void draw(float mouseX, float mouseY) {
      float x0 = this.gWidth() / 2.0F - 240.0F;
      float y0 = this.gHeight() / 2.0F - 141.0F;
      float w = this.gWidth();
      float h = this.gHeight();
      GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
      Render2DUtil.rect(0.0F, 0.0F, w, h).color(ColorUtil.rgba(9, 11, 17, 150)).draw();
      Render2DUtil.rect(x0, y0, 480.0F, 282.0F).color(0).radius(6.0F).shadow(Theme.Colors.PANEL_SHADOW, 10.0F).draw();
      Render2DUtil.rect(x0, y0, 480.0F, 282.0F).color(Theme.Colors.PANEL).radius(6.0F).blur(14.0F).draw();
      Render2DUtil.rect(x0, y0, 480.0F, 282.0F).color(0).radius(6.0F).border(1.0F, Theme.Colors.PANEL_BORDER).draw();
      Render2DUtil.text(x0 + 14.0F, y0 + 12.0F, 9.0F, "AutoBuy").font(UiFonts.sfPro(700)).color(-855307).draw();
      String cnt = this.visible.size() + "/" + AutoBuyFeature.Catalog.values().length;
      float cntW = UiFonts.sfPro(600).measureWidth(cnt, 5.0F, 0.0F);
      Render2DUtil.rect(x0 + 480.0F - 14.0F - cntW - 10.0F, y0 + 11.0F, cntW + 10.0F, 12.0F).color(ColorUtil.rgba(255, 255, 255, 8)).radius(4.0F).draw();
      Render2DUtil.text(x0 + 480.0F - 9.0F - cntW - 5.0F, y0 + 14.0F, 5.0F, cnt).font(UiFonts.sfPro(600)).color(-7434599).draw();
      this.fieldMinimal("Search", this.search, this.editingSearch, x0 + 14.0F, y0 + 44.0F, 220.0F);
      String priceHint = this.editingPriceFor != null ? this.editingPriceFor : "Price";
      if (priceHint.length() > 18) {
         priceHint = priceHint.substring(0, 15) + "...";
      }

      this.fieldMinimal(priceHint, this.price, this.editingPrice, x0 + 244.0F, y0 + 44.0F, 220.0F);
      float gridX = x0 + 14.0F;
      float gridY = y0 + 68.0F;
      float gridW = 392.0F;
      float gridH = 128.0F;
      Render2DUtil.rect(gridX, gridY, gridW, gridH).color(Theme.Colors.BACKGROUND_SURFACE_S).radius(4.0F).draw();
      Render2DUtil.rect(gridX, gridY, gridW, gridH).color(0).radius(4.0F).border(1.0F, Theme.Colors.OUTLINES_SMALL).draw();
      Render2DUtil.flush();
      Render2DUtil.pushScissor(gridX, gridY, gridW, gridH);
      int start = this.page * 80;

      for (int row = 0; row < 5; row++) {
         for (int col = 0; col < 16; col++) {
            int index = start + row * 16 + col;
            if (index < this.visible.size()) {
               AutoBuyFeature.Catalog view = this.visible.get(index);
               float cx = gridX + 4.0F + col * 24.0F;
               float cy = gridY + 4.0F + row * 24.0F;
               boolean selected = this.feature.isSelected(view.displayName);
               boolean hover = inside(mouseX, mouseY, cx, cy, 24.0F, 24.0F);
               Render2DUtil.rect(cx, cy, 24.0F, 24.0F).color(Theme.Colors.CONTROL_IDLE).radius(4.0F).draw();
               Render2DUtil.rect(cx, cy, 24.0F, 24.0F).color(0).radius(4.0F).border(1.0F, Theme.Colors.OUTLINES_SMALL).draw();
               if (selected) {
                  int accent = Theme.getAccent();
                  Render2DUtil.rect(cx, cy, 24.0F, 24.0F).color(withAlpha(accent, 0.13F)).radius(4.0F).draw();
                  Render2DUtil.rect(cx, cy, 24.0F, 24.0F).color(0).radius(4.0F).border(1.2F, withAlpha(accent, 0.85F)).draw();
               } else if (hover) {
                  Render2DUtil.rect(cx, cy, 24.0F, 24.0F).color(Theme.Colors.SURFACE_HOVER).radius(4.0F).draw();
                  Render2DUtil.rect(cx, cy, 24.0F, 24.0F).color(0).radius(4.0F).border(1.0F, ColorUtil.rgba(255, 255, 255, 28)).draw();
               }
            }
         }
      }

      Render2DUtil.flush();
      ItemStack tooltipStack = null;
      String tooltipName = null;
      long tooltipPrice = 0L;

      for (int row = 0; row < 5; row++) {
         for (int col = 0; col < 16; col++) {
            int index = start + row * 16 + col;
            if (index < this.visible.size()) {
               AutoBuyFeature.Catalog view = this.visible.get(index);
               float cx = gridX + 4.0F + col * 24.0F;
               float cy = gridY + 4.0F + row * 24.0F;
               boolean hover = inside(mouseX, mouseY, cx, cy, 24.0F, 24.0F);
               ItemStack stack = view.stack();
               extractor.item(stack, (int)(cx + 4.0F), (int)(cy + 4.0F));
               extractor.itemDecorations(mc.font, stack, (int)(cx + 4.0F), (int)(cy + 4.0F));
               if (hover && tooltipStack == null) {
                  tooltipStack = stack;
                  tooltipName = view.displayName;
                  tooltipPrice = this.feature.getParsedPrice(view.displayName);
               }
            }
         }
      }

      Render2DUtil.popScissor();
      Render2DUtil.rect(gridX, gridY, gridW, gridH).color(0).border(1.0F, ColorUtil.rgba(255, 255, 255, 8)).radius(4.0F).draw();
      String pageText = "Page " + (this.page + 1) + "/" + (this.pages() + 1) + " · " + this.feature.selectedCount() + " selected";
      Render2DUtil.text(gridX, gridY + gridH + 7.0F, 5.0F, pageText).font(UiFonts.sfPro(500)).color(-7434599).draw();
      float infoY = gridY + gridH + 14.0F;
      int infoColor = -7434599;
      String infoText;
      if (tooltipName != null) {
         String ps = tooltipPrice > 0L ? "  $" + String.format(Locale.US, "%,d", tooltipPrice).replace(',', '.') : "  —";
         infoText = tooltipName + ps;
         infoColor = tooltipPrice > 0L ? Theme.getAccent() : -7434599;
      } else if (this.feature.selectedCount() > 0) {
         Set<String> sel = this.feature.getSelectedNames();
         String joined = String.join(", ", sel);
         String base = "Выбрано (" + this.feature.selectedCount() + "): ";
         float avail = gridW - 12.0F;

         for (String full = base + joined; !joined.isEmpty() && UiFonts.sfPro(500).measureWidth(full, 5.0F, 0.0F) > avail; full = base + joined + "...") {
            joined = joined.substring(0, joined.length() - 1);
         }

         if (joined.length() < String.join(", ", sel).length()) {
            joined = joined + "...";
         }

         infoText = base + joined;
         infoColor = -855307;
      } else {
         infoText = "Hover to preview  •  Click to select  •  Price editable";
         infoColor = -7434599;
      }

      Render2DUtil.text(gridX, infoY + 2.0F, 5.0F, infoText).font(UiFonts.sfPro(500)).color(infoColor).draw();
      Render2DUtil.rect(gridX, infoY + 11.0F, Math.min(gridW, UiFonts.sfPro(500).measureWidth(infoText, 5.0F, 0.0F)), 1.0F)
         .color(ColorUtil.rgba(255, 255, 255, 10))
         .draw();
      float buttonY = y0 + 282.0F - 22.0F;
      this.buttonMinimal("Apply", x0 + 14.0F, buttonY, 72.0F, inside(mouseX, mouseY, x0 + 14.0F, buttonY, 72.0F, 14.0F));
      this.buttonMinimal("Parse", x0 + 92.0F, buttonY, 72.0F, inside(mouseX, mouseY, x0 + 92.0F, buttonY, 72.0F, 14.0F));
      this.buttonMinimalSmall("<", x0 + 480.0F - 138.0F, buttonY, 22.0F, inside(mouseX, mouseY, x0 + 480.0F - 138.0F, buttonY, 22.0F, 14.0F), this.page <= 0);
      this.buttonMinimalSmall(
         ">", x0 + 480.0F - 110.0F, buttonY, 22.0F, inside(mouseX, mouseY, x0 + 480.0F - 110.0F, buttonY, 22.0F, 14.0F), this.page >= this.pages()
      );
      List<AutoBuyFeature.HistoryEntry> hist2 = this.feature.getPurchaseHistory();
      if (!hist2.isEmpty()) {
         float hx2 = x0 + 14.0F;
         float hy2 = y0 + 282.0F - 38.0F;
         Render2DUtil.text(hx2, hy2, 4.5F, "История:").font(UiFonts.sfPro(500)).color(-7434599).draw();
         hx2 += 30.0F;
         float histW = 386.0F;
         float itemW = 32.0F;
         float gap = 6.0F;
         int maxVisible = (int)(histW / (itemW + gap));
         float maxScroll = Math.max(0.0F, hist2.size() * (itemW + gap) - gap - histW);
         this.historyScroll = Math.max(0.0F, Math.min(this.historyScroll, maxScroll));
         Render2DUtil.pushScissor(hx2, hy2 - 8.0F, histW, 22.0F);
         float drawX = hx2 - this.historyScroll;

         for (int i = 0; i < hist2.size(); i++) {
            AutoBuyFeature.HistoryEntry he = hist2.get(i);
            ItemStack hs = he.stack;
            float ix = drawX + i * (itemW + gap);
            if (!(ix + itemW < hx2) && !(ix > hx2 + histW)) {
               Render2DUtil.rect(ix, hy2 - 6.0F, itemW, 20.0F).color(Theme.Colors.CONTROL).radius(4.0F).draw();
               Render2DUtil.rect(ix, hy2 - 6.0F, itemW, 20.0F).color(0).radius(4.0F).border(1.0F, Theme.Colors.OUTLINES_SMALL).draw();
               if (he.price > 0L) {
                  String ps = "$" + String.format(Locale.US, "%,d", he.price).replace(',', '.');
                  float pw = UiFonts.sfPro(600).measureWidth(ps, 4.0F, 0.0F);
                  float px = ix + (itemW - pw) / 2.0F;
                  Render2DUtil.text(Math.max(hx2, Math.min(px, hx2 + histW - pw)), hy2 - 7.0F, 4.0F, ps).font(UiFonts.sfPro(600)).color(-10496).draw();
               }

               extractor.item(hs, (int)(ix + 8.0F), (int)(hy2 + 1.0F));
               if (inside(mouseX, mouseY, ix, hy2 - 6.0F, itemW, 20.0F)) {
                  try {
                     List<Component> lines = hs.getTooltipLines(TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL);
                     if (he.price > 0L) {
                        ArrayList<Component> copy = new ArrayList<>(lines);
                        copy.add(Component.literal("$" + String.format(Locale.US, "%,d", he.price).replace(',', '.') + " /шт"));
                        extractor.setTooltipForNextFrame(mc.font, copy, Optional.empty(), (int)mouseX, (int)mouseY);
                     } else {
                        extractor.setTooltipForNextFrame(mc.font, lines, Optional.empty(), (int)mouseX, (int)mouseY);
                     }
                  } catch (Exception ignored2) {
                     extractor.setTooltipForNextFrame(mc.font, hs, (int)mouseX, (int)mouseY);
                  }
               }
            }
         }

         Render2DUtil.popScissor();
         float clearW = 32.0F;
         float clearX = x0 + 480.0F - 14.0F - clearW;
         boolean clearHover = inside(mouseX, mouseY, clearX, hy2 - 1.0F, clearW, 12.0F);
         Render2DUtil.rect(clearX, hy2 - 1.0F, clearW, 12.0F)
            .color(clearHover ? ColorUtil.rgba(40, 20, 20, 180) : ColorUtil.rgba(22, 24, 32, 120))
            .radius(4.0F)
            .draw();
         Render2DUtil.rect(clearX, hy2 - 1.0F, clearW, 12.0F)
            .color(0)
            .border(1.0F, clearHover ? ColorUtil.rgba(200, 60, 60, 180) : ColorUtil.rgba(255, 255, 255, 10))
            .radius(4.0F)
            .draw();
         Render2DUtil.text(clearX + (clearW - UiFonts.sfPro(600).measureWidth("Clear", 4.5F, 0.0F)) / 2.0F, hy2 + 2.0F, 4.5F, "Clear")
            .font(UiFonts.sfPro(600))
            .color(clearHover ? -32640 : -7434599)
            .draw();
      }

      this.buttonMinimal("Close", x0 + 480.0F - 84.0F, buttonY, 70.0F, inside(mouseX, mouseY, x0 + 480.0F - 84.0F, buttonY, 70.0F, 14.0F));
      if (tooltipStack != null && tooltipName != null) {
         String priceLine = tooltipPrice > 0L
            ? "$" + String.format(Locale.US, "%,d", tooltipPrice).replace(',', '.') + " /шт  • клик чтобы изменить цену"
            : "не спаршено • клик чтобы выбрать";
         float tipW = Math.max(UiFonts.sfPro(600).measureWidth(tooltipName, 6.0F, 0.0F), UiFonts.sfPro(500).measureWidth(priceLine, 5.0F, 0.0F)) + 20.0F;
         float tipH = 22.0F;
         float tx = Math.min(mouseX + 12.0F, w - tipW - 4.0F);
         float ty = Math.min(mouseY + 12.0F, h - tipH - 4.0F);
         Render2DUtil.rect(tx, ty, tipW, tipH).color(ColorUtil.rgba(14, 16, 22, 235)).radius(4.0F).shadow(ColorUtil.rgba(0, 0, 0, 60), 6.0F).draw();
         Render2DUtil.rect(tx, ty, tipW, tipH).color(0).radius(4.0F).border(1.0F, Theme.Colors.OUTLINES_SMALL).draw();
         Render2DUtil.text(tx + 8.0F, ty + 4.5F, 6.0F, tooltipName).font(UiFonts.sfPro(600)).color(-855307).draw();
         Render2DUtil.text(tx + 8.0F, ty + 12.5F, 5.0F, priceLine).font(UiFonts.sfPro(500)).color(tooltipPrice > 0L ? Theme.getAccent() : -7434599).draw();
      }
   }

   private int pages() {
      return Math.max(0, (this.visible.size() - 1) / 80);
   }

   private void fieldMinimal(String hint, String value, boolean active, float x, float y, float w) {
      Render2DUtil.rect(x, y, w, 16.0F).color(active ? ColorUtil.rgba(22, 26, 38, 170) : ColorUtil.rgba(16, 18, 26, 140)).radius(4.0F).draw();
      Render2DUtil.rect(x, y, w, 16.0F).color(0).border(1.0F, active ? Theme.getAccent() : ColorUtil.rgba(255, 255, 255, 12)).radius(4.0F).draw();
      String shown = value.isBlank() ? hint : value;
      int col = value.isBlank() ? ColorUtil.rgba(140, 140, 150, 255) : -855307;
      Render2DUtil.pushScissor(x + 6.0F, y, w - 12.0F, 16.0F);
      Render2DUtil.text(x + 8.0F, y + 4.8F, 5.2F, shown).font(UiFonts.sfPro(500)).color(col).draw();
      Render2DUtil.popScissor();
      if (active && System.currentTimeMillis() / 530L % 2L == 0L) {
         float tw = UiFonts.sfPro(500).measureWidth(shown, 5.2F, 0.0F);
         float caretX = x + 8.0F + tw + 1.0F;
         if (caretX + 1.0F < x + w - 8.0F) {
            Render2DUtil.rect(caretX, y + 4.0F, 1.0F, 8.0F).color(Theme.getAccent()).draw();
         }
      }
   }

   private void buttonMinimal(String label, float x, float y, float w, boolean hover) {
      int bg = hover ? ColorUtil.rgba(28, 32, 48, 180) : ColorUtil.rgba(22, 24, 32, 140);
      int br = hover ? withAlpha(Theme.getAccent(), 0.45F) : ColorUtil.rgba(255, 255, 255, 14);
      Render2DUtil.rect(x, y, w, 14.0F).color(bg).radius(4.0F).draw();
      Render2DUtil.rect(x, y, w, 14.0F).color(0).border(1.0F, br).radius(4.0F).draw();
      float tw = UiFonts.sfPro(600).measureWidth(label, 5.4F, 0.0F);
      Render2DUtil.text(x + (w - tw) / 2.0F, y + 4.2F, 5.4F, label).font(UiFonts.sfPro(600)).color(hover ? -855307 : ColorUtil.rgba(220, 220, 230, 255)).draw();
   }

   private void buttonMinimalSmall(String label, float x, float y, float w, boolean hover, boolean disabled) {
      int bg;
      int br;
      int col;
      if (disabled) {
         bg = ColorUtil.rgba(16, 18, 26, 100);
         br = ColorUtil.rgba(255, 255, 255, 8);
         col = ColorUtil.rgba(100, 100, 110, 255);
      } else {
         bg = hover ? ColorUtil.rgba(28, 32, 48, 180) : ColorUtil.rgba(22, 24, 32, 140);
         br = hover ? withAlpha(Theme.getAccent(), 0.4F) : ColorUtil.rgba(255, 255, 255, 12);
         col = -855307;
      }

      Render2DUtil.rect(x, y, w, 14.0F).color(bg).radius(4.0F).draw();
      Render2DUtil.rect(x, y, w, 14.0F).color(0).border(1.0F, br).radius(4.0F).draw();
      float tw = UiFonts.sfPro(600).measureWidth(label, 5.6F, 0.0F);
      Render2DUtil.text(x + (w - tw) / 2.0F, y + 4.0F, 5.6F, label).font(UiFonts.sfPro(600)).color(col).draw();
   }

   private boolean handleClick(MouseButtonEvent click) {
      float mx = (float)click.x();
      float my = (float)click.y();
      float x0 = this.gWidth() / 2.0F - 240.0F;
      float y0 = this.gHeight() / 2.0F - 141.0F;
      boolean insidePanel = inside(mx, my, x0, y0, 480.0F, 282.0F);
      if (inside(mx, my, x0 + 14.0F, y0 + 44.0F, 220.0F, 16.0F)) {
         this.editingSearch = true;
         this.editingPrice = false;
         this.editingPriceFor = null;
         return true;
      }

      if (inside(mx, my, x0 + 244.0F, y0 + 44.0F, 220.0F, 16.0F)) {
         this.editingPrice = true;
         this.editingSearch = false;
         if (this.editingPriceFor == null) {
            this.editingPriceFor = null;
         }

         return true;
      } else {
         float gridX = x0 + 14.0F;
         float gridY = y0 + 68.0F;
         float gridW = 392.0F;
         float gridH = 128.0F;
         if (inside(mx, my, gridX, gridY, gridW, gridH)) {
            float relX = mx - gridX - 4.0F;
            float relY = my - gridY - 4.0F;
            if (relX >= 0.0F && relY >= 0.0F) {
               int col = (int)(relX / 24.0F);
               int row = (int)(relY / 24.0F);
               int index = this.page * 80 + row * 16 + col;
               if (col < 16 && row < 5 && index < this.visible.size()) {
                  AutoBuyFeature.Catalog cat = this.visible.get(index);
                  boolean isSelected = this.feature.isSelected(cat.displayName);
                  if (click.button() == 1) {
                     long pp = this.feature.getParsedPrice(cat.displayName);
                     this.price = String.valueOf(
                        pp > 0L
                           ? pp
                           : Long.parseLong(
                              this.feature.maxPriceValue().replaceAll("[^0-9]", "").isEmpty()
                                 ? "100000"
                                 : this.feature.maxPriceValue().replaceAll("[^0-9]", "")
                           )
                     );
                     this.editingPriceFor = cat.displayName;
                     this.editingPrice = true;
                     this.editingSearch = false;
                     if (!isSelected) {
                        this.feature.toggle(cat.displayName);
                     }
                  } else {
                     boolean wasSelected = isSelected;
                     this.feature.toggle(cat.displayName);
                     if (!wasSelected) {
                        long pp = this.feature.getParsedPrice(cat.displayName);
                        this.price = String.valueOf(
                           pp > 0L
                              ? pp
                              : Long.parseLong(
                                 this.feature.maxPriceValue().replaceAll("[^0-9]", "").isEmpty()
                                    ? "100000"
                                    : this.feature.maxPriceValue().replaceAll("[^0-9]", "")
                              )
                        );
                        this.editingPriceFor = cat.displayName;
                        this.editingPrice = true;
                        this.editingSearch = false;
                     } else {
                        this.editingPriceFor = null;
                        this.price = this.feature.maxPriceValue();
                     }
                  }
               }
            }

            return true;
         } else {
            float buttonY = y0 + 282.0F - 22.0F;
            if (inside(mx, my, x0 + 14.0F, buttonY, 72.0F, 14.0F)) {
               if (this.editingPriceFor != null) {
                  try {
                     long v = Long.parseLong(this.price.replaceAll("[^0-9]", ""));
                     if (v > 0L) {
                        this.feature.setParsedPrice(this.editingPriceFor, v);
                     }
                  } catch (Exception var21) {
                  }
               }

               this.feature.applySelection(this.price);
               this.onClose();
               return true;
            } else if (inside(mx, my, x0 + 92.0F, buttonY, 72.0F, 14.0F)) {
               if (this.editingPriceFor != null) {
                  try {
                     long v = Long.parseLong(this.price.replaceAll("[^0-9]", ""));
                     if (v > 0L) {
                        this.feature.setParsedPrice(this.editingPriceFor, v);
                     }
                  } catch (Exception var22) {
                  }
               }

               this.feature.applySelection(this.price);
               this.feature.startParser();
               this.onClose();
               return true;
            } else if (inside(mx, my, x0 + 480.0F - 138.0F, buttonY, 22.0F, 14.0F)) {
               if (this.page > 0) {
                  this.page--;
               }

               return true;
            } else if (inside(mx, my, x0 + 480.0F - 110.0F, buttonY, 22.0F, 14.0F)) {
               if (this.page < this.pages()) {
                  this.page++;
               }

               return true;
            } else if (inside(mx, my, x0 + 480.0F - 84.0F, buttonY, 70.0F, 14.0F)) {
               this.onClose();
               return true;
            } else {
               float chX = x0 + 480.0F - 14.0F - 32.0F;
               float chY = y0 + 282.0F - 38.0F;
               if (inside(mx, my, chX, chY - 1.0F, 32.0F, 12.0F) && !this.feature.getPurchaseHistory().isEmpty()) {
                  this.feature.clearPurchaseHistory();
                  return true;
               } else if (insidePanel) {
                  this.editingSearch = false;
                  this.editingPrice = false;
                  return true;
               } else {
                  return true;
               }
            }
         }
      }
   }

   @EventTarget(priority = 2000)
   public void onScreenClick(ScreenMouseButtonEvent event) {
      if (event.getScreen() instanceof AutoBuyScreen && event.getAction() == ScreenMouseButtonEvent.Action.CLICK) {
         int btn = event.getMouseButtonEvent().button();
         if (btn == 0 || btn == 1) {
            this.handleClick(event.getMouseButtonEvent());
            event.cancel();
         }
      }
   }

   @EventTarget(priority = 2000)
   public void onScreenRelease(ScreenMouseButtonEvent event) {
      if (event.getScreen() instanceof AutoBuyScreen) {
         if (event.getAction() == ScreenMouseButtonEvent.Action.RELEASE || event.getAction() == ScreenMouseButtonEvent.Action.DRAG) {
            event.cancel();
         }
      }
   }

   @EventTarget(priority = 2000)
   public void onScreenKey(ScreenKeyEvent event) {
      if (event.getScreen() instanceof AutoBuyScreen) {
         KeyEvent key = event.getKeyEvent();
         if (key == null) {
            event.cancel();
         } else if (key.key() == 256 && event.getAction() == ScreenKeyEvent.Action.PRESS) {
            if (!this.editingSearch && !this.editingPrice) {
               this.onClose();
               event.cancel();
            } else {
               this.editingSearch = false;
               this.editingPrice = false;
               event.cancel();
            }
         } else if (!this.editingSearch && !this.editingPrice) {
            event.cancel();
         } else if (event.getAction() != ScreenKeyEvent.Action.PRESS) {
            event.cancel();
         } else {
            if (key.key() == 259) {
               if (this.editingSearch && !this.search.isEmpty()) {
                  this.search = this.search.substring(0, this.search.length() - 1);
                  this.refresh();
               } else if (this.editingPrice && !this.price.isEmpty()) {
                  this.price = this.price.substring(0, this.price.length() - 1);
               }

               event.cancel();
            } else if (key.key() == 257 || key.key() == 335) {
               event.cancel();
            }
         }
      }
   }

   @EventTarget(priority = 2000)
   public void onChar(CharacterInputEvent event) {
      if (!(mc.gui.screen() instanceof AutoBuyScreen) || !this.editingSearch && !this.editingPrice) {
         if (mc.gui.screen() instanceof AutoBuyScreen) {
            event.cancel();
         }
      } else {
         char ch = (char)event.getCodePoint();
         if (this.editingSearch) {
            if (ch >= ' ' && ch != 127) {
               this.search = this.search + ch;
               this.refresh();
            }
         } else if (this.editingPrice && Character.isDigit(ch) && this.price.length() < 12) {
            this.price = this.price + ch;
         }

         event.cancel();
      }
   }

   @EventTarget(priority = 2000)
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (mc.gui.screen() instanceof AutoBuyScreen) {
         ;
      }
   }

   @EventTarget(priority = 2000)
   public void onMouseInput(MouseInputEvent event) {
      if (mc.gui.screen() instanceof AutoBuyScreen) {
         ;
      }
   }

   public static void handleScroll(double vertical) {
      if (mc.gui.screen() instanceof AutoBuyScreen s) {
         float x0 = s.gWidth() / 2.0F - 240.0F;
         float y0 = s.gHeight() / 2.0F - 141.0F;
         float hx2 = x0 + 14.0F + 30.0F;
         float hy2 = y0 + 282.0F - 38.0F;
         float histW = 386.0F;
         if (s.mouseX >= hx2 && s.mouseX < hx2 + histW && s.mouseY >= hy2 - 8.0F && s.mouseY < hy2 + 14.0F) {
            float maxScroll = Math.max(0.0F, s.feature.getPurchaseHistory().size() * 38 - 6 - histW);
            s.historyScroll = Math.max(0.0F, Math.min(maxScroll, s.historyScroll - (float)vertical * 12.0F));
            return;
         }

         if (vertical > 0.0) {
            if (s.page > 0) {
               s.page--;
            }
         } else if (vertical < 0.0 && s.page < s.pages()) {
            s.page++;
         }
      }
   }

   private float gWidth() {
      return mc.getWindow().getGuiScaledWidth();
   }

   private float gHeight() {
      return mc.getWindow().getGuiScaledHeight();
   }

   private static boolean inside(float px, float py, float bx, float by, float bw, float bh) {
      return px >= bx && px <= bx + bw && py >= by && py <= by + bh;
   }

   private static int withAlpha(int argb, float alpha) {
      int a = Math.round((argb >>> 24 & 0xFF) * alpha);
      return a << 24 | argb & 16777215;
   }

   private static final class FeatureHolder {
      private static final AutoBuyFeature FEATURE = FeatureManager.INSTANCE.getFeature(AutoBuyFeature.class);
   }
}

