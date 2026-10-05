package org.xrose.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.xrose.menu.pages.friends.FriendSkinCache;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.StaffManager;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.PlayerHead;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class StaffListElement extends HudElement {
   private static final float HEADER_HEIGHT = 18.0F;
   private static final float ROW_STEP = 16.0F;
   private static final float BODY_PADDING_Y = 7.0F;
   private static final float NAME_SIZE = 9.0F;
   private static final float HEAD_SIZE = 11.0F;
   private static final float HEAD_X = 10.0F;
   private static final float HEAD_MARGIN = 7.0F;
   private static final float BODY_PAD_X = 8.0F;
   private static final float MIN_WIDTH = 116.0F;
   private static final float HEADER_PAD_X = 8.0F;
   private static final float HEADER_TEXT_SIZE = 11.0F;
   private static final float ROW_ANIM_MS = 220.0F;
   private static final float ROW_FADE_MS = 330.0F;
   private static final float TIMER_SIZE = 8.0F;
   private static final float TIMER_GAP = 6.0F;
   private static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 245);
   private static final int ONLINE_COLOR = ColorUtil.rgba(255, 255, 255, 245);
   private static final int OFFLINE_COLOR = ColorUtil.rgba(183, 190, 202, 150);
   private static final int TIMER_COLOR = ColorUtil.rgba(160, 170, 186, 205);
   private static final Comparator<StaffListElement.RowAnim> ROW_ORDER = Comparator.comparingDouble(anim -> anim.offset);
   private final List<StaffListElement.RowAnim> rowAnims = new ArrayList<>();

   public StaffListElement() {
      super("stafflist", "StaffList");
   }

   @Override
   protected float defaultY(float unit) {
      return 40.0F * unit;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.refreshRows(mc);
      if (!this.hasActiveModerator()) {
         this.width = 0.0F;
         this.height = 0.0F;
      } else {
         float width = 116.0F * unit;
         MsdfFont font = UiFonts.sfProDisplay();
         float timerSize = 8.0F * unit;
         float timerWidth = font.measureWidth("88:88", timerSize, timerSize * UiFontStyle.REGULAR.letterSpacingEm());
         float bodyHeight = 0.0F;

         for (StaffListElement.RowAnim anim : this.rowAnims) {
            if (!(anim.alpha <= 0.01F)) {
               float nameSize = 9.0F * unit;
               float nameWidth = font.measureWidth(anim.name, nameSize, nameSize * UiFontStyle.MEDIUM.letterSpacingEm());
               float reserve = anim.elapsedMs >= 1000L ? 6.0F * unit + timerWidth : 0.0F;
               width = Math.max(width, (28.0F + nameWidth + reserve + 8.0F) * unit);
               bodyHeight = Math.max(bodyHeight, (anim.offset + 16.0F) * unit);
            }
         }

         this.width = width;
         this.height = 18.0F * unit + 14.0F * unit + bodyHeight;
      }
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      if (!this.rowAnims.isEmpty()) {
         float alpha = this.appearAlpha();
         float headerHeight = 18.0F * unit;
         float headSize = 11.0F * unit;
         float nameSize = 9.0F * unit;
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
         float headX = this.x + 10.0F * unit;
         List<StaffListElement.RowAnim> visible = new ArrayList<>(this.rowAnims.size());

         for (StaffListElement.RowAnim anim : this.rowAnims) {
            if (anim.alpha > 0.01F || anim.active) {
               visible.add(anim);
            }
         }

         visible.sort(ROW_ORDER);

         for (StaffListElement.RowAnim anim : visible) {
            float rowAlpha = alpha * anim.alpha;
            float rowY = bodyY + anim.offset * unit;
            float rowCenterY = rowY + 16.0F * unit / 2.0F;
            float headY = rowCenterY - headSize / 2.0F;
            float nameX = headX + headSize + 7.0F * unit;
            String timer = formatElapsed(anim.elapsedMs);
            float timerWidth = 0.0F;
            if (!timer.isEmpty()) {
               timerWidth = font.measureWidth(timer, 8.0F * unit, 8.0F * unit * UiFontStyle.REGULAR.letterSpacingEm());
               Render2DUtil.text(this.x + this.width - 8.0F * unit - timerWidth, font.centeredTextY(rowCenterY, 8.0F * unit), 8.0F * unit, timer)
                  .style(UiFontStyle.REGULAR)
                  .color(ColorUtil.multiplyAlpha(TIMER_COLOR, rowAlpha))
                  .draw();
            }

            float nameMaxWidth = this.x + this.width - 8.0F * unit - nameX - (timerWidth > 0.0F ? 6.0F * unit + timerWidth : 0.0F);
            String visibleName = this.trimToWidth(font, anim.name, nameSize, nameMaxWidth);
            int nameColor = anim.online ? ONLINE_COLOR : OFFLINE_COLOR;
            PlayerHead.draw(headX, headY, headSize, FriendSkinCache.texture(mc, anim.name), 3.0F * unit, ColorUtil.multiplyAlpha(-1, rowAlpha));
            Render2DUtil.text(nameX, font.centeredTextY(rowCenterY, nameSize), nameSize, visibleName)
               .style(UiFontStyle.MEDIUM)
               .color(ColorUtil.multiplyAlpha(nameColor, rowAlpha))
               .draw();
         }
      }
   }

   private void refreshRows(Minecraft mc) {
      long now = System.currentTimeMillis();

      for (StaffListElement.RowAnim anim : this.rowAnims) {
         anim.active = false;
      }

      boolean demo = StaffManager.INSTANCE.getStaff().isEmpty() && showcase(mc);
      List<String> names = this.collectRows(mc);
      int index = 0;

      for (String name : names) {
         boolean online = demo || FriendSkinCache.isOnline(mc, name);
         StaffListElement.RowAnim anim = this.row(name, index * 16.0F, now);
         anim.name = name;
         anim.online = online;
         anim.active = true;
         if (online) {
            if (anim.lastSeenAt > 0L && now > anim.lastSeenAt && now - anim.lastSeenAt < 2000L) {
               anim.elapsedMs = anim.elapsedMs + (now - anim.lastSeenAt);
            }

            anim.lastSeenAt = now;
         } else {
            anim.lastSeenAt = 0L;
         }

         if (online) {
            anim.run(now, 1.0F, 220.0F, true, index * 16.0F);
         } else {
            anim.run(now, 0.0F, 330.0F, false, index * 16.0F);
         }

         index++;
      }

      for (StaffListElement.RowAnim anim : this.rowAnims) {
         if (!anim.active) {
            anim.run(now, 0.0F, 330.0F, false, anim.offsetTarget);
         }
      }

      this.rowAnims.removeIf(animx -> !animx.active && animx.alpha <= 0.01F);

      for (StaffListElement.RowAnim anim : this.rowAnims) {
         anim.update(now);
      }
   }

   private boolean hasActiveModerator() {
      for (StaffListElement.RowAnim anim : this.rowAnims) {
         if (anim.active && anim.online) {
            return true;
         }
      }

      return false;
   }

   private List<String> collectRows(Minecraft mc) {
      List<String> rows = new ArrayList<>(StaffManager.INSTANCE.getStaff());
      rows.sort(Comparator.<String, Boolean>comparing(name -> !FriendSkinCache.isOnline(mc, name)).thenComparing(Comparator.naturalOrder()));
      if (rows.isEmpty() && showcase(mc)) {
         rows.add("Administrator");
         rows.add("Curator");
         rows.add("Helper");
      }

      return rows;
   }

   private StaffListElement.RowAnim row(String key, float targetOffset, long now) {
      for (StaffListElement.RowAnim anim : this.rowAnims) {
         if (anim.key.equals(key)) {
            return anim;
         }
      }

      StaffListElement.RowAnim anim = new StaffListElement.RowAnim(key);
      anim.alphaFrom = 0.0F;
      anim.alphaStart = now;
      anim.offset = anim.offsetFrom = targetOffset + 4.0F;
      anim.offsetTarget = Float.NaN;
      this.rowAnims.add(anim);
      return anim;
   }

   private static String formatElapsed(long ms) {
      long totalSeconds = ms / 1000L;
      return totalSeconds <= 0L ? "" : String.format("%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
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
      private boolean online;
      private boolean active;
      private long elapsedMs;
      private long lastSeenAt;
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
}

