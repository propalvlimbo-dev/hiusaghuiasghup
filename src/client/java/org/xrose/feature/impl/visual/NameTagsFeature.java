package org.xrose.feature.impl.visual;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2fStack;
import org.xrose.event.EventTarget;
import org.xrose.event.events.render.Render2DEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Render3DUtil;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;
import sdk.api.optimize.optimize;

@optimize
public final class NameTagsFeature extends Feature {
   private static final int DIVIDER_COLOR = ColorUtil.rgba(255, 255, 255, 20);
   private static final int GLASS_BG = ColorUtil.rgba(12, 15, 23, 130);
   private static final int GLASS_BORDER = ColorUtil.rgba(255, 255, 255, 26);
   private static final int GLASS_SHADOW = ColorUtil.rgba(0, 0, 0, 135);
   private static final float SCALE = 1.1F;
   private static final float PILL_HEIGHT = 40.0F;
   private static final float RADIUS = 8.0F;
   private static final float PANEL_BLUR = 8.0F;
   private static final float PANEL_GLOW = 14.0F;
   private static final float PADDING = 10.0F;
   private static final float GAP = 8.0F;
   private static final float HEAD_SIZE = 20.0F;
   private static final float ICON_SIZE = 16.0F;
   private static final float ITEM_SIZE = 16.0F;
   private static final float DIVIDER_HEIGHT = 12.0F;
   private static final float TEXT_SIZE = 12.0F;
   private static final float HEAD_OFFSET = 0.25F;
   private static final float ITEM_HIGHLIGHT_HEIGHT = 26.0F;
   private static final float ITEM_HIGHLIGHT_PADDING = 9.0F;
   private static final float ITEM_HIGHLIGHT_ICON = 15.0F;
   private static final float ITEM_HIGHLIGHT_TEXT = 10.0F;
   private static final EquipmentSlot[] EQUIPMENT_ORDER = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
   };
   public final NumberSetting scale = this.register(new NumberSetting("Scale", 1.0, 0.65, 1.2, 0.05, "x"));
   public final BooleanSetting privilege = this.register(new BooleanSetting("Privilege", true));
   public final BooleanSetting health = this.register(new BooleanSetting("Health", true));
   public final BooleanSetting items = this.register(new BooleanSetting("Items", true));
   public final BooleanSetting itemHighlight = this.register(new BooleanSetting("Item Highlight", true));
   public final BooleanSetting groundItems = this.register(new BooleanSetting("Ground Items", true));
   public final NumberSetting distance = this.register(new NumberSetting("Distance", 64.0, 16.0, 128.0, 8.0, " blocks"));

   public NameTagsFeature() {
      super("NameTags", "Draws styled nametags above players", FeatureCategory.VISUAL, -1);
   }

   public static boolean shouldHideVanillaTag() {
      return FeatureManager.INSTANCE.getEnabled(NameTagsFeature.class) != null;
   }

   @EventTarget
   public void onRender2D(Render2DEvent event) {
      Minecraft mc = event.getClient();
      if (mc != null && mc.level != null && mc.player != null) {
         float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         float unit = (float)(1.1F * this.scale.getValue() / mc.getWindow().getGuiScale());
         double maxDistanceSqr = this.distance.getValue() * this.distance.getValue();

         for (AbstractClientPlayer player : mc.level.players()) {
            if ((player != mc.player || !mc.options.getCameraType().isFirstPerson())
               && !player.isRemoved()
               && player.isAlive()
               && !player.isSpectator()
               && !(mc.player.distanceToSqr(player) > maxDistanceSqr)) {
               this.drawTag(event, player, tickDelta, unit);
            }
         }

         if (this.groundItems.getValue()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
               if (entity instanceof ItemEntity item && item.isAlive() && !item.isRemoved() && !(mc.player.distanceToSqr(item) > maxDistanceSqr)) {
                  drawItemLabel(event, item, tickDelta, unit);
               }
            }
         }
      }
   }

   private void drawTag(Render2DEvent event, AbstractClientPlayer player, float tickDelta, float unit) {
      Minecraft mc = event.getClient();
      Vec3 position = Render3DUtil.interpolatedPosition(player, tickDelta).add(0.0, player.getBbHeight() + 0.25F, 0.0);
      Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, position);
      if (anchor != null) {
         MsdfFont font = UiFonts.sfProDisplay();
         float textSize = 12.0F * unit;
         float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
         float headSize = 20.0F * unit;
         float iconSize = 16.0F * unit;
         float itemSize = 16.0F * unit;
         float gap = 8.0F * unit;
         float dividerWidth = Math.max(0.5F, 0.5F * unit);
         String name = player.getGameProfile().name();
         NameTagsFeature.Privilege donat = this.privilege.getValue() ? resolvePrivilege(player) : null;
         String healthText = this.health.getValue() ? (int)Math.ceil(getEntityHealth(player)) + "HP" : null;
         List<ItemStack> equipment = this.items.getValue() ? equipment(player) : List.of();
         float nameW = font.measureWidth(name, textSize, letterSpacing);
         float donatW = donat != null ? font.measureWidth(donat.name(), textSize, letterSpacing) : 0.0F;
         float healthW = healthText != null ? font.measureWidth(healthText, textSize, letterSpacing) : 0.0F;
         float width = 10.0F * unit + headSize + gap + nameW;
         if (donat != null) {
            width += gap + dividerWidth + gap + iconSize + gap + donatW;
         }

         if (healthText != null) {
            width += gap + dividerWidth + gap + iconSize + gap + healthW;
         }

         if (!equipment.isEmpty()) {
            width += gap + dividerWidth + gap + equipment.size() * itemSize + (equipment.size() - 1) * gap;
         }

         width += 10.0F * unit;
         float pillHeight = 40.0F * unit;
         float pillX = anchor.x() - width / 2.0F;
         float pillY = anchor.y() - pillHeight;
         float centerY = pillY + pillHeight / 2.0F;
         float textY = font.centeredTextY(centerY, textSize);
         glassPill(pillX, pillY, width, pillHeight, unit);
         float cursor = pillX + 10.0F * unit;
         drawHead(player, cursor, centerY - headSize / 2.0F, headSize);
         cursor += headSize + gap;
         Render2DUtil.text(cursor, textY, textSize, name).style(UiFontStyle.MEDIUM).color(Theme.getAccent()).draw();
         cursor += nameW;
         if (donat != null) {
            cursor = this.drawDivider(cursor, centerY, dividerWidth, gap, unit);
            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.SPARKLES).color(Theme.getAccent()).draw();
            cursor += iconSize + gap;
            Render2DUtil.text(cursor, textY, textSize, donat.name()).style(UiFontStyle.MEDIUM).color(donat.color()).draw();
            cursor += donatW;
         }

         if (healthText != null) {
            cursor = this.drawDivider(cursor, centerY, dividerWidth, gap, unit);
            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.SCAN_HEART).color(Theme.getAccent()).draw();
            cursor += iconSize + gap;
            Render2DUtil.text(cursor, textY, textSize, healthText).style(UiFontStyle.MEDIUM).color(-1).draw();
            cursor += healthW;
         }

         if (!equipment.isEmpty()) {
            cursor = this.drawDivider(cursor, centerY, dividerWidth, gap, unit);
            Render2DUtil.flush();
            Matrix3x2fStack pose = event.getGuiGraphicsExtractor().pose();
            float guiScale = mc.getWindow().getGuiScale();
            float scale = itemSize / 16.0F;
            float itemY = Math.round((centerY - itemSize / 2.0F) * guiScale) / guiScale;

            for (ItemStack stack : equipment) {
               float itemX = Math.round(cursor * guiScale) / guiScale;
               pose.pushMatrix();
               pose.translate(itemX, itemY);
               pose.scale(scale);
               event.getGuiGraphicsExtractor().item(stack, 0, 0);
               pose.popMatrix();
               cursor += itemSize + gap;
            }
         }

         if (this.itemHighlight.getValue() && !equipment.isEmpty()) {
            List<ItemStack> held = heldItems(player);
            if (!held.isEmpty()) {
               drawItemHighlight(event, held, anchor.x(), pillY, pillHeight, unit);
            }
         }
      }
   }

   private static void glassPill(float x, float y, float width, float height, float unit) {
      Render2DUtil.rect(x, y, width, height)
         .color(GLASS_BG)
         .radius(8.0F * unit)
         .border(Math.max(0.5F, 1.0F * unit), GLASS_BORDER)
         .blur(8.0F * unit, 1.0F)
         .shadow(GLASS_SHADOW, 14.0F * unit)
         .draw();
   }

   private static void drawHead(AbstractClientPlayer player, float x, float y, float size) {
      Identifier skin = player.getSkin().body().texturePath();
      float radius = size * 0.2F;
      Render2DUtil.rect(x, y, size, size).color(ColorUtil.multiplyAlpha(Theme.getAccent(), 0.22F)).radius(radius).draw();
      Render2DUtil.texture(x, y, size, size, skin).managed().uv(0.125F, 0.125F, 0.25F, 0.25F).radius(radius).draw();
      Render2DUtil.texture(x, y, size, size, skin).managed().uv(0.625F, 0.125F, 0.75F, 0.25F).radius(radius).draw();
   }

   private float drawDivider(float cursor, float centerY, float dividerWidth, float gap, float unit) {
      cursor += gap;
      Render2DUtil.rect(cursor, centerY - 12.0F * unit / 2.0F, dividerWidth, 12.0F * unit).color(DIVIDER_COLOR).draw();
      return cursor + dividerWidth + gap;
   }

   private static List<ItemStack> equipment(AbstractClientPlayer player) {
      List<ItemStack> stacks = new ArrayList<>(EQUIPMENT_ORDER.length);

      for (EquipmentSlot slot : EQUIPMENT_ORDER) {
         ItemStack stack = player.getItemBySlot(slot);
         if (!stack.isEmpty()) {
            stacks.add(stack);
         }
      }

      return stacks;
   }

   private static List<ItemStack> heldItems(AbstractClientPlayer player) {
      List<ItemStack> held = new ArrayList<>(2);
      ItemStack mainHand = player.getMainHandItem();
      ItemStack offhand = player.getOffhandItem();
      if (!mainHand.isEmpty()) {
         held.add(mainHand);
      }

      if (!offhand.isEmpty()) {
         held.add(offhand);
      }

      return held;
   }

   private static void drawItemHighlight(Render2DEvent event, List<ItemStack> held, float anchorX, float pillY, float pillHeight, float unit) {
      Minecraft mc = event.getClient();
      MsdfFont font = UiFonts.sfProDisplay();
      float textSize = 10.0F * unit;
      float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float iconSize = 15.0F * unit;
      float padding = 9.0F * unit;
      float gap = 8.0F * unit;
      float height = 26.0F * unit;
      float radius = 8.0F * unit;
      float[] widths = new float[held.size()];
      float total = 0.0F;

      for (int i = 0; i < held.size(); i++) {
         float textWidth = font.measureWidth(held.get(i).getHoverName().getString(), textSize, letterSpacing);
         widths[i] = padding + iconSize + gap + textWidth + padding;
         total += widths[i];
      }

      if (held.size() > 1) {
         total += gap * (held.size() - 1);
      }

      float y = pillY + pillHeight + gap;
      float centerY = y + height / 2.0F;
      float textY = font.centeredTextY(centerY, textSize);
      float cursor = anchorX - total / 2.0F;

      for (int i = 0; i < held.size(); i++) {
         Render2DUtil.rect(cursor, y, widths[i], height).color(GLASS_BG).radius(radius).border(Math.max(0.5F, 1.0F * unit), GLASS_BORDER).draw();
         cursor += widths[i] + gap;
      }

      Render2DUtil.flush();
      float guiScale = mc.getWindow().getGuiScale();
      float iconY = Math.round((centerY - iconSize / 2.0F) * guiScale) / guiScale;
      cursor = anchorX - total / 2.0F;

      for (int i = 0; i < held.size(); i++) {
         ItemStack stack = held.get(i);
         float scale = iconSize / 16.0F;
         float itemX = Math.round((cursor + padding) * guiScale) / guiScale;
         Matrix3x2fStack pose = event.getGuiGraphicsExtractor().pose();
         pose.pushMatrix();
         pose.translate(itemX, iconY);
         pose.scale(scale);
         event.getGuiGraphicsExtractor().item(stack, 0, 0);
         pose.popMatrix();
         float textX = cursor + padding + iconSize + gap;
         Render2DUtil.text(textX, textY, textSize, stack.getHoverName().getString()).style(UiFontStyle.MEDIUM).color(Theme.getAccent()).draw();
         cursor += widths[i] + gap;
      }
   }

   private static void drawItemLabel(Render2DEvent event, ItemEntity item, float tickDelta, float unit) {
      Minecraft mc = event.getClient();
      Vec3 position = Render3DUtil.interpolatedPosition(item, tickDelta).add(0.0, item.getBbHeight() + 0.35, 0.0);
      Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, position);
      if (anchor != null) {
         ItemStack stack = item.getItem();
         String label = stack.getHoverName().getString();
         MsdfFont font = UiFonts.sfProDisplay();
         float textSize = 10.0F * unit;
         float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
         float iconSize = 15.0F * unit;
         float padding = 9.0F * unit;
         float gap = 8.0F * unit;
         float height = 26.0F * unit;
         float radius = 8.0F * unit;
         float textWidth = font.measureWidth(label, textSize, letterSpacing);
         float width = padding + iconSize + gap + textWidth + padding;
         float x = anchor.x() - width / 2.0F;
         float y = anchor.y() - height - gap;
         float centerY = y + height / 2.0F;
         float textY = font.centeredTextY(centerY, textSize);
         Render2DUtil.rect(x, y, width, height)
            .color(GLASS_BG)
            .radius(radius)
            .border(Math.max(0.5F, 1.0F * unit), GLASS_BORDER)
            .blur(8.0F * unit, 1.0F)
            .shadow(GLASS_SHADOW, 14.0F * unit)
            .draw();
         Render2DUtil.flush();
         float guiScale = mc.getWindow().getGuiScale();
         float scale = iconSize / 16.0F;
         float iconX = Math.round((x + padding) * guiScale) / guiScale;
         float iconY = Math.round((centerY - iconSize / 2.0F) * guiScale) / guiScale;
         Matrix3x2fStack pose = event.getGuiGraphicsExtractor().pose();
         pose.pushMatrix();
         pose.translate(iconX, iconY);
         pose.scale(scale);
         event.getGuiGraphicsExtractor().item(stack, 0, 0);
         pose.popMatrix();
         Render2DUtil.text(x + padding + iconSize + gap, textY, textSize, label).style(UiFontStyle.MEDIUM).color(Theme.getAccent()).draw();
      }
   }

   private static float getEntityHealth(AbstractClientPlayer player) {
      float hp = player.getHealth() + player.getAbsorptionAmount();
      if (player.level() != null) {
         Scoreboard scoreboard = player.level().getScoreboard();
         if (scoreboard != null) {
            try {
               Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
               if (objective != null) {
                  ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(player, objective);
                  if (score != null) {
                     String formatted = score.formatValue(objective.numberFormatOrDefault(BlankFormat.INSTANCE)).getString();
                     String digits = formatted.replaceAll("\\D", "");
                     if (!digits.isEmpty()) {
                        hp = Float.parseFloat(digits);
                     }
                  }
               }
            } catch (Exception var7) {
            }
         }
      }

      return hp;
   }

   private static NameTagsFeature.Privilege resolvePrivilege(AbstractClientPlayer player) {
      PlayerTeam team = player.getTeam();
      if (team == null) {
         return null;
      }

      Component prefix = team.getPlayerPrefix();
      if (prefix == null) {
         return null;
      }

      String raw = prefix.getString();
      String name = cleanName(raw);
      if (name.isEmpty()) {
         return null;
      }

      Integer color = extractColor(prefix);
      if (color == null) {
         color = legacyColor(raw);
      }

      int rgb = color != null ? ColorUtil.rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255) : Theme.getAccent();
      return new NameTagsFeature.Privilege(name, rgb);
   }

   private static String cleanName(String text) {
      String stripped = text.replaceAll("§.", "");
      StringBuilder clean = new StringBuilder(stripped.length());

      for (int i = 0; i < stripped.length(); i++) {
         char character = stripped.charAt(i);
         if (isBasicLetterOrDigit(character) || character == ' ') {
            clean.append(character);
         }
      }

      return clean.toString().replaceAll("\\s+", " ").trim();
   }

   private static boolean isBasicLetterOrDigit(char character) {
      return character >= 'a' && character <= 'z'
         || character >= 'A' && character <= 'Z'
         || character >= '0' && character <= '9'
         || character >= 1072 && character <= 1103
         || character >= 1040 && character <= 1071
         || character == 1105
         || character == 1025;
   }

   private static Integer legacyColor(String text) {
      for (int i = 0; i < text.length() - 1; i++) {
         if (text.charAt(i) == 167) {
            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting != null) {
               TextColor color = TextColor.fromLegacyFormat(formatting);
               if (color != null) {
                  return color.getValue();
               }
            }
         }
      }

      return null;
   }

   private static Integer extractColor(Component component) {
      TextColor color = component.getStyle().getColor();
      if (color != null) {
         return color.getValue();
      }

      for (Component sibling : component.getSiblings()) {
         Integer siblingColor = extractColor(sibling);
         if (siblingColor != null) {
            return siblingColor;
         }
      }

      return null;
   }

   private record Privilege(String name, int color) {
   }
}

