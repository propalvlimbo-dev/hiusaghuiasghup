package org.xrose.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.TextAlign;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class PotionsElement extends HudElement {
   private static final int BAR_TRACK_COLOR = ColorUtil.rgba(255, 255, 255, 20);
   private static final int BAR_GREEN = Theme.Colors.TRAFFIC_MAXIMIZE;
   private static final int BAR_YELLOW = Theme.Colors.TRAFFIC_MINIMIZE;
   private static final float TEXT_SIZE = 10.0F;
   private static final float BAR_HEIGHT = 3.0F;
   private static final int COLUMNS = 2;
   private static final float ROW_GAP = 4.0F;
   private static final float COLUMN_GAP = 4.0F;
   private static final PotionsElement.Style MINI = new PotionsElement.Style(8.0F, 8.0F, 31.0F, 32.0F, 14.0F, 2.0F, 11.0F, 2.0F, 16.0F);
   private final List<PotionsElement.Card> cards = new ArrayList<>();
   private final Map<Identifier, Integer> maxDurations = new HashMap<>();
   private final PotionsElement.Style style = MINI;

   public PotionsElement() {
      super("potions", "Potions");
   }

   @Override
   protected float defaultY(float unit) {
      return 260.0F * unit;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.collectCards(mc);
      if (this.cards.isEmpty()) {
         this.width = 0.0F;
         this.height = 0.0F;
      } else {
         int columns = Math.min(2, this.cards.size());
         int rows = (this.cards.size() + 2 - 1) / 2;
         this.width = (this.style.padding() * 2.0F + columns * this.style.cardWidth() + Math.max(0, columns - 1) * 4.0F) * unit;
         this.height = (this.style.padding() * 2.0F + rows * this.style.cardHeight() + Math.max(0, rows - 1) * 4.0F) * unit;
      }
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      if (!this.cards.isEmpty()) {
         float alpha = this.appearAlpha();
         this.hudBackground(alpha, unit, this.style.radius() * unit);
         MsdfFont font = UiFonts.sfProDisplay();
         float textSize = 10.0F * unit;
         float cardWidth = this.style.cardWidth() * unit;
         float cardHeight = this.style.cardHeight() * unit;
         float gridX = this.x + this.style.padding() * unit;
         float gridY = this.y + this.style.padding() * unit;
         float columnGap = 4.0F * unit;
         float rowGap = 4.0F * unit;

         for (int i = 0; i < this.cards.size(); i++) {
            PotionsElement.Card card = this.cards.get(i);
            int column = i % 2;
            int row = i / 2;
            float cardX = gridX + column * (cardWidth + columnGap);
            float cardTop = gridY + row * (cardHeight + rowGap);
            float cardCenterX = cardX + cardWidth / 2.0F;
            float iconSize = this.style.iconSize() * unit;
            this.glassChip(cardCenterX - iconSize / 2.0F, cardTop, iconSize, iconSize, 5.0F * unit, alpha, unit);
            Render2DUtil.texture(cardCenterX - iconSize / 2.0F, cardTop, iconSize, iconSize, card.icon())
               .managed()
               .color(ColorUtil.multiplyAlpha(-1, alpha))
               .draw();
            float textCenterY = cardTop + (this.style.iconSize() + this.style.iconTextGap() + this.style.textHeight() / 2.0F) * unit;
            Render2DUtil.text(cardCenterX, font.centeredTextY(textCenterY, textSize), textSize, card.time())
               .style(UiFontStyle.MEDIUM)
               .color(ColorUtil.multiplyAlpha(card.harmful() ? Theme.Colors.SYSTEM_RED : -1, alpha))
               .align(TextAlign.CENTER)
               .draw();
            float barY = cardTop + (this.style.cardHeight() - 3.0F) * unit;
            float barX = cardCenterX - this.style.barWidth() * unit / 2.0F;
            float barRadius = 3.0F * unit / 2.0F;
            Render2DUtil.rect(barX, barY, this.style.barWidth() * unit, 3.0F * unit)
               .color(ColorUtil.multiplyAlpha(BAR_TRACK_COLOR, alpha))
               .radius(barRadius)
               .draw();
            float fillWidth = this.style.barWidth() * unit * card.fraction();
            if (fillWidth >= 3.0F * unit) {
               Render2DUtil.rect(barX, barY, fillWidth, 3.0F * unit).color(ColorUtil.multiplyAlpha(barColor(card), alpha)).radius(barRadius).draw();
            }
         }
      }
   }

   private static int barColor(PotionsElement.Card card) {
      if (card.harmful()) {
         return Theme.Colors.SYSTEM_RED;
      } else if (card.fraction() > 0.5F) {
         return BAR_GREEN;
      } else {
         return card.fraction() > 0.25F ? BAR_YELLOW : Theme.Colors.SYSTEM_RED;
      }
   }

   private void collectCards(Minecraft mc) {
      this.cards.clear();
      Map<Identifier, Integer> seen = new HashMap<>();

      for (MobEffectInstance effect : mc.player.getActiveEffects()) {
         Identifier effectId = ((ResourceKey)effect.getEffect().unwrapKey().orElseThrow()).identifier();
         Identifier icon = effectId.withPath(path -> "textures/mob_effect/" + path + ".png");
         boolean harmful = ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL;
         float fraction;
         String time;
         if (effect.isInfiniteDuration()) {
            fraction = 1.0F;
            time = "∞";
         } else {
            int duration = effect.getDuration();
            int max = Math.max(duration, this.maxDurations.getOrDefault(effectId, 0));
            seen.put(effectId, max);
            fraction = max > 0 ? (float)duration / max : 0.0F;
            int seconds = duration / 20;
            time = seconds / 60 + ":" + String.format("%02d", seconds % 60);
         }

         this.cards.add(new PotionsElement.Card(icon, time, harmful, fraction));
      }

      this.maxDurations.clear();
      this.maxDurations.putAll(seen);
      if (this.cards.isEmpty() && showcase(mc)) {
         this.cards.add(new PotionsElement.Card(effectIcon("fire_resistance"), "1:52", false, 0.9F));
         this.cards.add(new PotionsElement.Card(effectIcon("speed"), "0:47", false, 0.4F));
         this.cards.add(new PotionsElement.Card(effectIcon("hunger"), "0:30", true, 1.0F));
      }
   }

   private static Identifier effectIcon(String name) {
      return Identifier.withDefaultNamespace("textures/mob_effect/" + name + ".png");
   }

   private record Card(Identifier icon, String time, boolean harmful, float fraction) {
   }

   private record Style(
      float radius, float padding, float cardWidth, float cardHeight, float iconSize, float iconTextGap, float textHeight, float textBarGap, float barWidth
   ) {
   }
}

