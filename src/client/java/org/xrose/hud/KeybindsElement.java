package org.xrose.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import net.minecraft.client.Minecraft;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BindSetting;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.TextAlign;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class KeybindsElement extends HudElement {
   private static final float HEADER_HEIGHT = 18.0F;
   private static final float ROW_STEP = 15.0F;
   private static final float BODY_PADDING_Y = 7.0F;
   private static final float NAME_SIZE = 9.0F;
   private static final float BIND_SIZE = 8.0F;
   private static final float NAME_X = 10.0F;
   private static final float BADGE_MARGIN = 6.0F;
   private static final float BADGE_PAD_X = 5.0F;
   private static final float BADGE_HEIGHT = 13.0F;
   private static final float BADGE_RADIUS = 3.0F;
   private static final float MIN_WIDTH = 112.0F;
   private static final float HEADER_PAD_X = 8.0F;
   private static final float HEADER_TEXT_SIZE = 11.0F;
   private static final float ROW_ANIM_MS = 220.0F;
   private static final float ROW_FADE_MS = 330.0F;
   private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 245);
   private static final int MUTED_COLOR = ColorUtil.rgba(183, 190, 202, 215);
   private static final Comparator<KeybindsElement.RowAnim> ROW_ORDER = Comparator.comparingDouble(anim -> anim.offset);
   private final List<KeybindsElement.RowAnim> rowAnims = new ArrayList<>();

   public KeybindsElement() {
      super("keybinds", "Keybinds");
   }

   @Override
   protected float defaultY(float unit) {
      return 120.0F * unit;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.refreshRows(mc);
      if (this.rowAnims.isEmpty()) {
         this.width = 0.0F;
         this.height = 0.0F;
      } else {
         float width = 112.0F * unit;
         int rowCount = 0;
         MsdfFont font = UiFonts.sfProDisplay();

         for (KeybindsElement.RowAnim anim : this.rowAnims) {
            if (!(anim.alpha <= 0.01F) || anim.active) {
               rowCount++;
               float nameSize = 9.0F * unit;
               float nameWidth = font.measureWidth(anim.name, nameSize, nameSize * UiFontStyle.MEDIUM.letterSpacingEm());
               float bindSize = 8.0F * unit;
               float bindWidth = font.measureWidth(anim.bind, bindSize, bindSize * UiFontStyle.REGULAR.letterSpacingEm());
               width = Math.max(width, (10.0F + nameWidth + 6.0F + bindWidth + 10.0F + 6.0F) * unit);
            }
         }

         this.width = width;
         this.height = 18.0F * unit + rowCount * 15.0F * unit + 14.0F * unit;
      }
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      if (!this.rowAnims.isEmpty()) {
         float alpha = this.appearAlpha();
         float headerHeight = 18.0F * unit;
         float nameSize = 9.0F * unit;
         float bindSize = 8.0F * unit;
         this.hudBackground(alpha, unit);
         MsdfFont font = UiFonts.sfProDisplay();
         float headerCenterY = this.y + headerHeight / 2.0F;
         float headerTextY = font.centeredTextY(headerCenterY, 11.0F * unit);
         Render2DUtil.text(this.x + 8.0F * unit, headerTextY, 11.0F * unit, this.displayName())
            .style(UiFontStyle.SEMIBOLD)
            .color(ColorUtil.multiplyAlpha(TEXT_COLOR, alpha))
            .draw();
         Render2DUtil.rect(this.x + 8.0F * unit, this.y + headerHeight - 0.5F * unit, this.width - 16.0F * unit, Math.max(0.5F, 0.5F * unit))
            .color(ColorUtil.multiplyAlpha(DIVIDER_COLOR, alpha))
            .draw();
         float bodyY = this.y + headerHeight + 7.0F * unit;
         float nameX = this.x + 10.0F * unit;
         List<KeybindsElement.RowAnim> visible = new ArrayList<>(this.rowAnims.size());

         for (KeybindsElement.RowAnim anim : this.rowAnims) {
            if (anim.alpha > 0.01F || anim.active) {
               visible.add(anim);
            }
         }

         visible.sort(ROW_ORDER);

         for (KeybindsElement.RowAnim anim : visible) {
            float rowAlpha = alpha * anim.alpha;
            float rowY = bodyY + anim.offset * unit;
            float rowCenterY = rowY + 15.0F * unit / 2.0F;
            float badgeHeight = 13.0F * unit;
            float badgeRadius = 3.0F * unit;
            float bindWidth = font.measureWidth(anim.bind, bindSize, bindSize * UiFontStyle.REGULAR.letterSpacingEm());
            float badgeWidth = bindWidth + 10.0F * unit;
            float badgeX = this.x + this.width - 6.0F * unit - badgeWidth;
            float badgeY = rowY + (15.0F * unit - badgeHeight) / 2.0F;
            float nameEndX = badgeX - 6.0F * unit;
            String visibleName = this.trimToWidth(font, anim.name, nameSize, nameEndX - nameX);
            Render2DUtil.text(nameX, font.centeredTextY(rowCenterY, nameSize), nameSize, visibleName)
               .style(UiFontStyle.MEDIUM)
               .color(ColorUtil.multiplyAlpha(TEXT_COLOR, rowAlpha))
               .draw();
            this.drawBadge(badgeX, badgeY, badgeWidth, badgeHeight, badgeRadius, unit, rowAlpha);
            int bindColor = anim.bindEnabled ? Theme.getAccent() : MUTED_COLOR;
            Render2DUtil.text(badgeX + badgeWidth / 2.0F, font.centeredTextY(badgeY + badgeHeight / 2.0F, bindSize), bindSize, anim.bind)
               .style(UiFontStyle.REGULAR)
               .align(TextAlign.CENTER)
               .color(ColorUtil.multiplyAlpha(bindColor, rowAlpha))
               .draw();
         }
      }
   }

   private void drawBadge(float x, float y, float width, float height, float radius, float unit, float alpha) {
      this.glassChip(x, y, width, height, radius, alpha, unit);
   }

   private void refreshRows(Minecraft mc) {
      long now = System.currentTimeMillis();

      for (KeybindsElement.RowAnim anim : this.rowAnims) {
         anim.active = false;
      }

      int index = 0;

      for (KeybindsElement.RowSpec spec : this.collectRows(mc)) {
         String key = spec.name() + "|" + spec.bind();
         KeybindsElement.RowAnim anim = this.row(key, index * 15.0F, now);
         anim.name = spec.name();
         anim.bind = spec.bind();
         anim.bindEnabled = spec.enabled();
         anim.active = true;
         anim.run(now, 1.0F, 220.0F, true, index * 15.0F);
         index++;
      }

      for (KeybindsElement.RowAnim anim : this.rowAnims) {
         if (!anim.active) {
            anim.run(now, 0.0F, 330.0F, false, anim.offsetTarget);
         }
      }

      this.rowAnims.removeIf(animx -> !animx.active && animx.alpha <= 0.01F);

      for (KeybindsElement.RowAnim anim : this.rowAnims) {
         anim.update(now);
      }
   }

   private List<KeybindsElement.RowSpec> collectRows(Minecraft mc) {
      List<KeybindsElement.RowSpec> specs = new ArrayList<>();

      for (Feature feature : FeatureManager.INSTANCE.getFeatures()) {
         StringJoiner binds = new StringJoiner(" + ");
         List<Integer> bindCodes = feature.getBinds();

         for (int i = 0; i < bindCodes.size(); i++) {
            int bindCode = bindCodes.get(i);
            if (bindCode != -1 && feature.isBindVisibleAt(i)) {
               binds.add(BindSetting.describe(bindCode));
            }
         }

         if (binds.length() > 0) {
            specs.add(new KeybindsElement.RowSpec(feature.getName(), binds.toString(), feature.isEnabled()));
         }
      }

      if (specs.isEmpty() && showcase(mc)) {
         specs.add(new KeybindsElement.RowSpec("TriggerBot", "F", false));
         specs.add(new KeybindsElement.RowSpec("ESP", "Space", true));
         specs.add(new KeybindsElement.RowSpec("FullBright", "Z + X", false));
      }

      return specs;
   }

   private KeybindsElement.RowAnim row(String key, float targetOffset, long now) {
      for (KeybindsElement.RowAnim anim : this.rowAnims) {
         if (anim.key.equals(key)) {
            return anim;
         }
      }

      KeybindsElement.RowAnim anim = new KeybindsElement.RowAnim(key);
      anim.alphaFrom = 0.0F;
      anim.alphaStart = now;
      anim.offset = anim.offsetFrom = targetOffset + 4.0F;
      anim.offsetTarget = Float.NaN;
      this.rowAnims.add(anim);
      return anim;
   }

   private String trimToWidth(MsdfFont font, String name, float size, float maxWidth) {
      if (name.length() <= 1) {
         return name;
      }

      float letterSpacing = size * UiFontStyle.MEDIUM.letterSpacingEm();
      if (font.measureWidth(name, size, letterSpacing) <= maxWidth) {
         return name;
      }

      for (int length = name.length() - 1; length > 0; length--) {
         String truncated = name.substring(0, length) + "...";
         if (font.measureWidth(truncated, size, letterSpacing) <= maxWidth) {
            return truncated;
         }
      }

      return name.substring(0, 1) + "...";
   }

   private static final class RowAnim {
      private final String key;
      private String name = "";
      private String bind = "";
      private boolean bindEnabled;
      private boolean active;
      private float alpha;
      private float alphaFrom;
      private float alphaTarget;
      private float alphaSpeed = 220.0F;
      private boolean alphaExpo = true;
      private long alphaStart;
      private float offset;
      private float offsetFrom;
      private float offsetTarget = Float.NaN;
      private float offsetSpeed = 220.0F;
      private boolean offsetExpo = true;
      private long offsetStart;

      private RowAnim(String key) {
         this.key = key;
      }

      private void run(long now, float alphaTarget, float alphaSpeed, boolean alphaExpo, float offsetTarget) {
         if (alphaTarget != this.alphaTarget || alphaExpo != this.alphaExpo) {
            this.alphaFrom = this.alpha;
            this.alphaStart = now;
            this.alphaTarget = alphaTarget;
            this.alphaSpeed = alphaSpeed;
            this.alphaExpo = alphaExpo;
         }

         if (offsetTarget != this.offsetTarget) {
            this.offsetFrom = this.offset;
            this.offsetStart = now;
            this.offsetTarget = offsetTarget;
            this.offsetSpeed = 220.0F;
         }
      }

      private void update(long now) {
         this.alpha = ease(this.alphaFrom, this.alphaTarget, this.alphaStart, this.alphaSpeed, this.alphaExpo, now);
         this.offset = ease(this.offsetFrom, this.offsetTarget, this.offsetStart, this.offsetSpeed, this.offsetExpo, now);
      }

      private static float ease(float from, float target, long start, float durationMs, boolean expo, long now) {
         float t = Math.min(1.0F, Math.max(0.0F, (float)(now - start) / durationMs));
         float eased = expo ? 1.0F - (float)Math.pow(2.0, -10.0 * t) : (float)Math.sin((Math.PI / 2) * t);
         return from + (target - from) * eased;
      }
   }

   private record RowSpec(String name, String bind, boolean enabled) {
   }
}

