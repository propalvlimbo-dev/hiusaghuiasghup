package org.xrose.hud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.lifecycle.FeatureToggleEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class NotificationsElement extends HudElement {
   private static final long LIFETIME_MS = 4000L;
   private static final long APPEAR_MS = 200L;
   private static final long FADE_MS = 300L;
   private static final int MAX_NOTES = 5;
   private static final int EXPIRING_TICKS = 400;
   private static final float STACK_GAP = 8.0F;
   private static final float SLIDE_PX = 8.0F;
   private static final float PADDING_X = 9.0F;
   private static final float PADDING_Y = 7.0F;
   private static final float ICON_SIZE = 18.0F;
   private static final float ICON_RADIUS = 6.0F;
   private static final float ICON_TEXT_GAP = 8.0F;
   private static final float TITLE_SIZE = 10.0F;
   private static final float DETAIL_SIZE = 8.5F;
   private static final float DETAIL_GAP = 2.0F;
   private static final String[] ROMAN = new String[]{"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
   private static final Identifier[] CATEGORY_ICONS = new Identifier[]{
      Textures.Icons.SWORDS, Textures.Icons.PERSON_STANDING, Textures.Icons.EYE, Textures.Icons.USER_ROUND, Textures.Icons.BOXES, Textures.Icons.BRAIN
   };
   private static final NotificationsElement.Style MINI = new NotificationsElement.Style(34.0F, 9.0F);
   private static final Deque<NotificationsElement.Note> NOTES = new ArrayDeque<>();
   private final Map<Identifier, NotificationsElement.EffectState> trackedEffects = new HashMap<>();
   private final List<NotificationsElement.Note> visible = new ArrayList<>();
   private final NotificationsElement.Style style = MINI;

   public NotificationsElement() {
      super("notifications", "Notifications");
   }

   @Override
   protected boolean centerHorizontally() {
      return true;
   }

   @Override
   protected float defaultY(float unit) {
      return 420.0F * unit;
   }

   private static void featureToggled(Feature feature, boolean enabled) {
      Minecraft mc = MinecraftContext.mc;
      if (mc != null && mc.level != null) {
         push(
            new NotificationsElement.Note(
               categoryIcon(feature.getCategory()),
               enabled ? Theme.getAccent() : Theme.Colors.TEXT_GHOST,
               false,
               List.of(
                  new NotificationsElement.Segment(feature.getName(), enabled ? Theme.getAccent() : Theme.Colors.TEXT_GHOST),
                  new NotificationsElement.Segment(enabled ? "feature has been enabled" : "feature has been disabled", Theme.Colors.TEXT_TEXT)
               ),
               System.currentTimeMillis()
            )
         );
      }
   }

   public static void notify(Identifier icon, int tint, String title, String detail) {
      List<NotificationsElement.Segment> segments = new ArrayList<>(2);
      segments.add(new NotificationsElement.Segment(title, -1));
      if (detail != null && !detail.isBlank()) {
         segments.add(new NotificationsElement.Segment(detail, Theme.Colors.TEXT_TEXT));
      }

      push(new NotificationsElement.Note(icon, tint, false, segments, System.currentTimeMillis()));
   }

   private static void push(NotificationsElement.Note note) {
      NOTES.addLast(note);

      while (NOTES.size() > 5) {
         NOTES.removeFirst();
      }
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.trackEffects(mc);
      long now = System.currentTimeMillis();
      NOTES.removeIf(notex -> now - notex.createdAt() >= 4000L);
      this.visible.clear();
      this.visible.addAll(NOTES);
      if (this.visible.isEmpty() && showcase(mc)) {
         this.visible.add(this.exampleNote(now));
      }

      if (this.visible.isEmpty()) {
         this.width = 0.0F;
         this.height = 0.0F;
      } else {
         MsdfFont font = UiFonts.sfPro(UiFontStyle.MEDIUM.weight());
         float maxWidth = 0.0F;
         float stackHeight = 0.0F;

         for (NotificationsElement.Note note : this.visible) {
            maxWidth = Math.max(maxWidth, this.pillWidth(font, note, unit));
            stackHeight += (this.style.height() + 8.0F) * unit * envelope(note, now);
         }

         this.width = maxWidth;
         this.height = Math.max(1.0F, stackHeight - 8.0F * unit);
      }
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      MsdfFont font = UiFonts.sfPro(UiFontStyle.MEDIUM.weight());
      long now = System.currentTimeMillis();
      float guiWidth = mc.getWindow().getGuiScaledWidth();
      float pillY = this.y;

      for (NotificationsElement.Note note : this.visible) {
         float envelope = envelope(note, now);
         if (envelope > 0.01F) {
            this.drawPill(font, note, guiWidth, pillY, unit, now, envelope);
         }

         pillY += (this.style.height() + 8.0F) * unit * envelope;
      }
   }

   private static float envelope(NotificationsElement.Note note, long now) {
      long age = now - note.createdAt();
      float appear = Math.clamp((float)age / 200.0F, 0.0F, 1.0F);
      float disappear = Math.clamp((float)(4000L - age) / 300.0F, 0.0F, 1.0F);
      float value = Math.min(appear, disappear);
      return value * value * (3.0F - 2.0F * value);
   }

   private void drawPill(MsdfFont font, NotificationsElement.Note note, float guiWidth, float pillY, float unit, long now, float alpha) {
      float pillHeight = this.style.height() * unit;
      float pillWidth = this.pillWidth(font, note, unit);
      float pillX = (guiWidth - pillWidth) / 2.0F;
      long age = now - note.createdAt();
      float appear = Math.clamp((float)age / 200.0F, 0.0F, 1.0F);
      pillY -= (1.0F - appear * appear * (3.0F - 2.0F * appear)) * 8.0F * unit;
      this.hudBackground(pillX, pillY, pillWidth, pillHeight, alpha, unit, this.style.radius() * unit);
      float iconSize = 18.0F * unit;
      float iconX = pillX + 9.0F * unit;
      float iconY = pillY + (pillHeight - iconSize) / 2.0F;
      this.glassChip(iconX, iconY, iconSize, iconSize, 6.0F * unit, alpha, unit);
      Render2DUtil.TextureBuilder icon = Render2DUtil.texture(iconX, iconY, iconSize, iconSize, note.icon());
      if (note.managedIcon()) {
         icon.managed();
         icon.color(ColorUtil.multiplyAlpha(-1, alpha));
      } else {
         icon.color(ColorUtil.multiplyAlpha(note.iconTint(), alpha));
      }

      icon.draw();
      float textX = iconX + iconSize + 8.0F * unit;
      float titleSize = 10.0F * unit;
      float detailSize = 8.5F * unit;
      float firstCenterY = pillY + 7.0F * unit + titleSize / 2.0F;
      float secondCenterY = pillY + pillHeight - 7.0F * unit - detailSize / 2.0F;
      NotificationsElement.Segment titleSegment = note.segments().get(0);
      Render2DUtil.text(textX, font.centeredTextY(firstCenterY, titleSize), titleSize, titleSegment.text())
         .style(UiFontStyle.SEMIBOLD)
         .color(ColorUtil.multiplyAlpha(titleSegment.color(), alpha))
         .draw();
      if (note.segments().size() > 1) {
         StringBuilder detail = new StringBuilder();

         for (int index = 1; index < note.segments().size(); index++) {
            if (index > 1) {
               detail.append(' ');
            }

            detail.append(note.segments().get(index).text());
         }

         Render2DUtil.text(textX, font.centeredTextY(secondCenterY, detailSize), detailSize, detail.toString())
            .style(UiFontStyle.MEDIUM)
            .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, alpha))
            .draw();
      }
   }

   private float pillWidth(MsdfFont font, NotificationsElement.Note note, float unit) {
      float titleSize = 10.0F * unit;
      float detailSize = 8.5F * unit;
      float titleSpacing = titleSize * UiFontStyle.SEMIBOLD.letterSpacingEm();
      float detailSpacing = detailSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float titleWidth = font.measureWidth(note.segments().get(0).text(), titleSize, titleSpacing);
      float detailWidth = 0.0F;

      for (int index = 1; index < note.segments().size(); index++) {
         detailWidth += font.measureWidth(note.segments().get(index).text(), detailSize, detailSpacing);
         if (index > 1) {
            detailWidth += font.measureWidth(" ", detailSize, detailSpacing);
         }
      }

      return 18.0F * unit + 18.0F * unit + 8.0F * unit + Math.max(titleWidth, detailWidth);
   }

   private void trackEffects(Minecraft mc) {
      if (mc.player == null) {
         this.trackedEffects.clear();
      } else {
         Map<Identifier, NotificationsElement.EffectState> current = new HashMap<>();

         for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            Identifier effectId = ((ResourceKey)effect.getEffect().unwrapKey().orElseThrow()).identifier();
            int duration = effect.getDuration();
            int amplifier = effect.getAmplifier();
            NotificationsElement.EffectState previous = this.trackedEffects.get(effectId);
            boolean applied = previous == null || amplifier != previous.amplifier() || !effect.isInfiniteDuration() && duration > previous.duration() + 200;
            boolean expiryNotified = previous != null && previous.expiryNotified();
            if (applied) {
               pushEffect(effect, "effect applied for", duration);
               expiryNotified = false;
            } else if (!expiryNotified && !effect.isInfiniteDuration() && duration <= 400) {
               pushEffect(effect, "effect expires in", duration);
               expiryNotified = true;
            }

            current.put(effectId, new NotificationsElement.EffectState(duration, amplifier, expiryNotified));
         }

         this.trackedEffects.clear();
         this.trackedEffects.putAll(current);
      }
   }

   private static void pushEffect(MobEffectInstance effect, String middle, int durationTicks) {
      Identifier effectId = ((ResourceKey)effect.getEffect().unwrapKey().orElseThrow()).identifier();
      Identifier icon = effectId.withPath(path -> "textures/mob_effect/" + path + ".png");
      boolean harmful = ((MobEffect)effect.getEffect().value()).getCategory() == MobEffectCategory.HARMFUL;
      String name = ((MobEffect)effect.getEffect().value()).getDisplayName().getString() + roman(effect.getAmplifier());
      String duration = effect.isInfiniteDuration() ? "∞" : formatTicks(durationTicks);
      push(
         new NotificationsElement.Note(
            icon,
            0,
            true,
            List.of(
               new NotificationsElement.Segment(name, harmful ? Theme.Colors.SYSTEM_RED : Theme.Colors.TRAFFIC_MAXIMIZE),
               new NotificationsElement.Segment(middle, Theme.Colors.TEXT_TEXT),
               new NotificationsElement.Segment(duration, -1)
            ),
            System.currentTimeMillis()
         )
      );
   }

   private static String roman(int amplifier) {
      return amplifier > 0 && amplifier < ROMAN.length ? ROMAN[amplifier] : (amplifier >= ROMAN.length ? " " + (amplifier + 1) : "");
   }

   private static String formatTicks(int ticks) {
      int seconds = ticks / 20;
      return seconds / 60 + ":" + String.format("%02d", seconds % 60);
   }

   private NotificationsElement.Note exampleNote(long now) {
      return new NotificationsElement.Note(
         categoryIcon(FeatureCategory.VISUAL),
         Theme.getAccent(),
         false,
         List.of(
            new NotificationsElement.Segment("ExampleFeature", Theme.getAccent()),
            new NotificationsElement.Segment("feature has been enabled", Theme.Colors.TEXT_TEXT)
         ),
         now - 2000L
      );
   }

   private static Identifier categoryIcon(FeatureCategory category) {
      int index = category.ordinal();
      return CATEGORY_ICONS[index >= 0 && index < CATEGORY_ICONS.length ? index : 0];
   }

   private record EffectState(int duration, int amplifier, boolean expiryNotified) {
   }

   private record Note(Identifier icon, int iconTint, boolean managedIcon, List<NotificationsElement.Segment> segments, long createdAt) {
   }

   private record Segment(String text, int color) {
   }

   private record Style(float height, float radius) {
   }

   public static final class ToggleListener {
      @EventTarget
      public void onFeatureToggle(FeatureToggleEvent event) {
         NotificationsElement.featureToggled(event.getFeature(), event.isEnabled());
      }
   }
}

