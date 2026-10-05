package org.xrose.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.combat.ServerTickSync;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class WatermarkElement extends HudElement {
   private static final float MINI_SCALE_DIVISOR = 1.175F;
   private static final float ROW_HEIGHT = 26.0F;
   private static final float ROW_GAP = 4.0F;
   private static final float LOGO_SIZE = 17.0F;
   private static final float TEXT_SIZE = 10.0F;
   private static final float ICON_SIZE = 12.0F;
   private static final float RADIUS = 8.0F;
   private static final float GAP = 8.0F;
   private static final float ICON_GAP = 5.0F;
   private static final float TEXT_GAP = 4.0F;
   private static final float PADDING_X = 12.0F;
   private static final float LEFT_PANEL_PAD = 4.0F;
   private static final float BRAND_GAP = 2.0F;
   private static final float BRAND_TEXT_SIZE = 12.0F;
   private static final String BRAND = "xrose";
   private static final String BRAND_ACCENT = ".fun";
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
   private float leftPanelW;
   private float rightSideStart;
   private float topTotal;
   private float botX;
   private float botTotal;
   private String userText;
   private String fpsText;
   private String pingText;
   private String timeText;
   private String tpsText;
   private static final long SESSION_REFRESH_MS = 5000L;
   private static String cachedSessionName;
   private static long cachedSessionNameAt;

   public WatermarkElement() {
      super("watermark", "Watermark");
   }

   private static String sessionName(Minecraft mc) {
      long now = System.currentTimeMillis();
      if (cachedSessionName != null && now - cachedSessionNameAt < 5000L) {
         return cachedSessionName;
      }

      String resolved = resolveSessionName(mc);
      if (!resolved.equals(cachedSessionName)) {
         cachedSessionName = resolved;
      }

      cachedSessionNameAt = now;
      return resolved;
   }

   private static String resolveSessionName(Minecraft mc) {
      try {
         String name = Class.forName("ru.xd.Session").getMethod("getName").invoke(null) instanceof String s ? s : null;
         if (name != null && !name.isBlank()) {
            return name;
         }
      } catch (Throwable var4) {
      }

      return mc.getUser() != null ? mc.getUser().getName() : "Player";
   }

   @Override
   protected boolean preservePositionOnContentResize() {
      return true;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      unit = this.scaledUnit(unit);
      MsdfFont brandFont = UiFonts.sfPro(700);
      MsdfFont contentFont = UiFonts.sfPro(600);
      float textSize = 10.0F * unit;
      float letterSpacing = textSize * UiFontStyle.SEMIBOLD.letterSpacingEm();
      float brandSize = 12.0F * unit;
      float brandLetter = brandSize * UiFontStyle.MEDIUM.letterSpacingEm();
      String user = this.userText = sessionName(mc);
      String fpsNum = this.fpsText = String.valueOf(mc.getFps());
      String pingStr = this.pingText = String.valueOf(latency(mc));
      String timeStr = this.timeText = LocalTime.now().format(TIME_FORMATTER);
      String tpsStr = this.tpsText = String.valueOf(tps());
      float brandW = brandFont.measureWidth("xrose", brandSize, brandLetter);
      float accentW = brandFont.measureWidth(".fun", brandSize, brandLetter);
      this.leftPanelW = 25.0F * unit + brandW + 2.0F * unit + accentW + 4.0F * unit;
      this.rightSideStart = this.leftPanelW + 8.0F * unit;
      float userW = contentFont.measureWidth(user, textSize, letterSpacing);
      float fpsW = contentFont.measureWidth(fpsNum, textSize, letterSpacing);
      float fpsLblW = contentFont.measureWidth("Fps", textSize, letterSpacing);
      float pingW = contentFont.measureWidth(pingStr, textSize, letterSpacing);
      float pingLblW = contentFont.measureWidth("Ticks", textSize, letterSpacing);
      float rightW = 17.0F * unit + userW + 8.0F * unit;
      rightW += 17.0F * unit + fpsW + 4.0F * unit + fpsLblW + 8.0F * unit;
      rightW += 17.0F * unit + pingW + 4.0F * unit + pingLblW;
      this.topTotal = this.rightSideStart + rightW + 12.0F * unit;
      float timeW = contentFont.measureWidth(timeStr, textSize, letterSpacing);
      float tpsW = contentFont.measureWidth(tpsStr, textSize, letterSpacing);
      float tpsLblW = contentFont.measureWidth("Tps", textSize, letterSpacing);
      float botContentW = 17.0F * unit + timeW + 8.0F * unit + 0.5F + 17.0F * unit + tpsW + 4.0F * unit + tpsLblW + 12.0F * unit;
      this.botTotal = Math.min(botContentW, this.topTotal);
      this.botX = (this.topTotal - this.botTotal) / 2.0F;
      this.width = this.topTotal;
      this.height = 56.0F * unit;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      unit = this.scaledUnit(unit);
      float alpha = this.appearAlpha();
      float rowH = 26.0F * unit;
      float rowGap = 4.0F * unit;
      int accent = Theme.getAccent();
      this.drawRowCard(this.x, this.y, this.topTotal, rowH, unit, alpha);
      this.drawRowCard(this.x + this.botX, this.y + rowH + rowGap, this.botTotal, rowH, unit, alpha);
      this.drawLeftPanel(alpha, unit);
      this.drawLogoAndBrand(alpha, unit, accent);
      MsdfFont contentFont = UiFonts.sfPro(600);
      float textSize = 10.0F * unit;
      float letterSpacing = textSize * UiFontStyle.SEMIBOLD.letterSpacingEm();
      float topCY = this.y + rowH / 2.0F;
      float ox = this.x + this.rightSideStart;
      String user = this.userText != null ? this.userText : sessionName(mc);
      ox = this.drawValueItem(ox, topCY, textSize, letterSpacing, Textures.Icons.USER_ROUND, user, null, accent, alpha, contentFont, unit);
      ox += 8.0F * unit;
      ox = this.drawValueItem(
         ox,
         topCY,
         textSize,
         letterSpacing,
         Textures.Icons.ZAP,
         this.fpsText != null ? this.fpsText : String.valueOf(mc.getFps()),
         "Fps",
         accent,
         alpha,
         contentFont,
         unit
      );
      ox += 8.0F * unit;
      this.drawValueItem(
         ox,
         topCY,
         textSize,
         letterSpacing,
         Textures.Icons.SIGNAL,
         this.pingText != null ? this.pingText : String.valueOf(latency(mc)),
         "Ticks",
         accent,
         alpha,
         contentFont,
         unit
      );
      float botY = this.y + rowH + rowGap;
      float botCY = botY + rowH / 2.0F;
      float bo = this.x + this.botX + 12.0F * unit * 0.5F;
      bo = this.drawValueItem(
         bo - 0.5F,
         botCY,
         textSize,
         letterSpacing,
         Textures.Icons.CLOCK,
         this.timeText != null ? this.timeText : LocalTime.now().format(TIME_FORMATTER),
         null,
         accent,
         alpha,
         contentFont,
         unit
      );
      bo += 8.0F * unit;
      this.drawValueItem(
         bo - 0.5F,
         botCY,
         textSize,
         letterSpacing,
         Textures.Icons.ACTIVITY,
         this.tpsText != null ? this.tpsText : String.valueOf(tps()),
         "Tps",
         accent,
         alpha,
         contentFont,
         unit
      );
   }

   private void drawRowCard(float x, float y, float w, float h, float unit, float alpha) {
      Render2DUtil.rect(x, y, w, h)
         .color(ColorUtil.multiplyAlpha(GLASS_BG, alpha))
         .radius(8.0F * unit)
         .border(Math.max(0.5F, 1.0F * unit), ColorUtil.multiplyAlpha(GLASS_BORDER, alpha))
         .blur(8.0F * unit, alpha)
         .shadow(ColorUtil.multiplyAlpha(GLASS_SHADOW, alpha), 14.0F * unit)
         .draw();
   }

   private void drawLeftPanel(float alpha, float unit) {
      float rowH = 26.0F * unit;
      Render2DUtil.rect(this.x + this.leftPanelW, this.y + 2.0F * unit, Math.max(0.5F, 0.5F * unit), rowH - 4.0F * unit)
         .color(ColorUtil.multiplyAlpha(DIVIDER_COLOR, alpha))
         .draw();
   }

   private void drawLogoAndBrand(float alpha, float unit, int accent) {
      float rowH = 26.0F * unit;
      float size = 17.0F * unit;
      float logoX = this.x + 4.0F * unit;
      float logoY = this.y + (rowH - size) / 2.0F;
      float logoPad = 3.0F * unit;
      float chipSize = size + logoPad * 2.0F;
      Render2DUtil.rect(logoX - logoPad, logoY - logoPad, chipSize, chipSize)
         .color(0)
         .radius(chipSize / 2.0F)
         .border(Math.max(0.5F, 0.75F * unit), ColorUtil.multiplyAlpha(DIVIDER_COLOR, alpha))
         .draw();
      Render2DUtil.texture(logoX, logoY, size, size, Textures.Logos.WATERMARK).color(ColorUtil.multiplyAlpha(-1, alpha)).draw();
      MsdfFont brandFont = UiFonts.sfPro(700);
      float brandSize = 12.0F * unit;
      float brandLetter = brandSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float topCY = this.y + rowH / 2.0F;
      float brandY = brandFont.centeredTextY(topCY, brandSize);
      float txtX = logoX + size + 4.0F * unit;
      Render2DUtil.text(txtX, brandY, brandSize, "xrose").font(brandFont).style(UiFontStyle.MEDIUM).color(ColorUtil.multiplyAlpha(-1, alpha)).draw();
      float accentX = txtX + brandFont.measureWidth("xrose", brandSize, brandLetter) + 2.0F * unit;
      Render2DUtil.text(accentX, brandY, brandSize, ".fun").font(brandFont).style(UiFontStyle.MEDIUM).color(ColorUtil.multiplyAlpha(accent, alpha)).draw();
   }

   private float drawValueItem(
      float ox,
      float centerY,
      float textSize,
      float letterSpacing,
      Identifier icon,
      String value,
      String label,
      int accent,
      float alpha,
      MsdfFont font,
      float unit
   ) {
      float iconSize = 12.0F * unit;
      float iconY = centerY - iconSize / 2.0F;
      Render2DUtil.texture(ox, iconY, iconSize, iconSize, icon).color(ColorUtil.multiplyAlpha(accent, alpha)).draw();
      ox += iconSize + 5.0F * unit;
      float textY = font.centeredTextY(centerY, textSize);
      Render2DUtil.text(ox, textY, textSize, value).font(font).style(UiFontStyle.SEMIBOLD).color(ColorUtil.multiplyAlpha(-1, alpha)).draw();
      ox += font.measureWidth(value, textSize, letterSpacing) + 4.0F * unit;
      if (label != null) {
         Render2DUtil.text(ox, textY, textSize, label).font(font).style(UiFontStyle.SEMIBOLD).color(ColorUtil.multiplyAlpha(accent, alpha)).draw();
         ox += font.measureWidth(label, textSize, letterSpacing);
      }

      return ox;
   }

   private static int tps() {
      float value = ServerTickSync.INSTANCE.effectiveTps();
      return Math.round(value);
   }

   private static int latency(Minecraft mc) {
      if (mc.getConnection() != null && mc.player != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         return info != null ? info.getLatency() : 0;
      } else {
         return 0;
      }
   }

   private float scaledUnit(float unit) {
      return unit / 1.175F;
   }
}

