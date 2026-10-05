package org.xrose.feature.impl.visual;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.event.events.render.Render2DEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.pve.MineHelperFeature;
import org.xrose.feature.impl.pve.PveManagerFeature;
import org.xrose.feature.setting.MultiSelectSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.hud.ArmorHudElement;
import org.xrose.hud.CoordsElement;
import org.xrose.hud.HotbarElement;
import org.xrose.hud.HudElement;
import org.xrose.hud.KeybindsElement;
import org.xrose.hud.MineTimerElement;
import org.xrose.hud.NotificationsElement;
import org.xrose.hud.PotionsElement;
import org.xrose.hud.PveStatusElement;
import org.xrose.hud.QuickUseElement;
import org.xrose.hud.StaffListElement;
import org.xrose.hud.TargetElement;
import org.xrose.hud.WatermarkElement;
import org.xrose.menu.core.MenuConfigStore;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;
import sdk.api.optimize.optimize;

@optimize
public final class HudFeature extends Feature {
   private static final float HUD_SCALE = 1.4F;
   private static final String SIZE_MIGRATION_KEY = "hudScale100Migrated";
   private static final double LEGACY_DEFAULT_SIZE = 70.0;
   private static final Identifier RESET_ICON = Identifier.parse("xrose:textures/menu/icons/refresh_ccw.svg");
   private static final int RESET_DIVIDER_COLOR = ColorUtil.rgba(255, 255, 255, 25);
   private static final String RESET_LABEL = "Сбросить расположение";
   private static final float SNAP_MARGIN = 12.0F;
   private static final float SNAP_GAP = 6.0F;
   private static final float SNAP_THRESHOLD = 8.0F;
   private static final String WATERMARK = "Watermark";
   private static final String COORDINATES = "Coordinates";
   private static final String KEYBINDS = "Keybinds";
   private static final String POTIONS = "Potions";
   private static final String TARGET = "Target";
   private static final String NOTIFICATIONS = "Notifications";
   private static final String HOTBAR = "Hotbar";
   private static final String ARMOR_HUD = "ArmorHud";
   private static final String QUICK_USE = "QuickUse";
   private static final String STAFF_LIST = "StaffList";
   public final MultiSelectSetting elements = this.register(
      new MultiSelectSetting(
         "Elements",
         List.of("Watermark", "Coordinates", "Keybinds", "Potions", "Target", "Notifications", "Hotbar", "ArmorHud", "QuickUse", "StaffList"),
         "Watermark",
         "Coordinates",
         "Keybinds",
         "Potions",
         "Target",
         "Notifications",
         "Hotbar",
         "ArmorHud",
         "QuickUse",
         "StaffList"
      )
   );
   public final NumberSetting size = this.register(new NumberSetting("Size", 100.0, 50.0, 200.0, 1.0, "%"));
   public final NumberSetting glassBlur = this.register(new NumberSetting("Glass blur", 50.0, 0.0, 150.0, 5.0, "%"));
   public final NumberSetting chatSize = this.register(new NumberSetting("Chat Size", 100.0, 50.0, 200.0, 1.0, "%"));
   private static HudFeature instance;
   private final WatermarkElement watermarkElement = new WatermarkElement();
   private final CoordsElement coordsElement = new CoordsElement();
   private final KeybindsElement keybindsElement = new KeybindsElement();
   private final PotionsElement potionsElement = new PotionsElement();
   private final TargetElement targetElement = new TargetElement();
   private final NotificationsElement notificationsElement = new NotificationsElement();
   private final HotbarElement hotbarElement = new HotbarElement();
   private final ArmorHudElement armorHudElement = new ArmorHudElement();
   private final QuickUseElement quickUseElement = new QuickUseElement();
   private final StaffListElement staffListElement = new StaffListElement();
   private final PveStatusElement pveStatusElement = new PveStatusElement();
   private final MineTimerElement mineTimerElement = new MineTimerElement();
   private final List<HudElement> allElements = List.of(
      this.watermarkElement,
      this.coordsElement,
      this.keybindsElement,
      this.potionsElement,
      this.targetElement,
      this.notificationsElement,
      this.hotbarElement,
      this.armorHudElement,
      this.quickUseElement,
      this.staffListElement,
      this.pveStatusElement,
      this.mineTimerElement
   );
   private HudElement draggedElement;
   private boolean sizeMigrationChecked;
   private float resetButtonX;
   private float resetButtonY;
   private float resetButtonWidth;
   private float resetButtonHeight;
   private float activeGuideX = Float.NaN;
   private float activeGuideY = Float.NaN;

   public HudFeature() {
      super("HUD", "HUD customization settings", FeatureCategory.VISUAL, -1);
      instance = this;
   }

   public static boolean customHotbarActive() {
      return instance != null && instance.isEnabled() && instance.elements.isSelected("Hotbar");
   }

   public static float glassBlurScale() {
      return instance != null ? (float)(instance.glassBlur.getValue() / 100.0) : 0.5F;
   }

   public static float chatScale() {
      return instance != null ? (float)(instance.chatSize.getValue() / 100.0) : 1.0F;
   }

   public static float hotbarDecorationOffset(Minecraft mc) {
      return customHotbarActive() && mc.player != null && !mc.player.isSpectator() ? instance.hotbarElement.decorationOffset(mc) : 0.0F;
   }

   public static float hotbarDecorationScale(Minecraft mc) {
      return customHotbarActive() && mc.player != null && !mc.player.isSpectator() ? (float)(instance.size.getValue() / 100.0) : 1.0F;
   }

   public static int hotbarStatsHalfWidth(Minecraft mc, int vanillaHalfWidth) {
      if (customHotbarActive() && mc.player != null && !mc.player.isSpectator()) {
         float halfWidth = instance.hotbarElement.statsHalfWidth();
         float scale = hotbarDecorationScale(mc);
         return halfWidth > 0.5F && scale > 0.001F ? Math.round(halfWidth / scale) : vanillaHalfWidth;
      } else {
         return vanillaHalfWidth;
      }
   }

   @EventTarget(priority = -100)
   public void onRender2D(Render2DEvent event) {
      Minecraft mc = event.getClient();
      if (mc != null && mc.player != null) {
         this.migrateLegacySize();
         Render2DUtil.setBackdropBlurScale(glassBlurScale());
         float unit = (float)(1.4F / mc.getWindow().getGuiScale() * this.size.getValue() / 100.0);
         List<HudElement> visible = this.visibleElements();
         boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
         boolean snapMode = chatOpen && controlDown(mc);
         if (this.draggedElement != null) {
            if (chatOpen) {
               this.draggedElement.dragTo(mouseX(mc), mouseY(mc));
               if (snapMode) {
                  this.applySnap(mc, visible);
               } else {
                  this.clearSnapGuides();
               }
            } else {
               this.stopDrag(mc);
            }
         } else {
            this.clearSnapGuides();
         }

         if (snapMode) {
            this.drawSnapGrid(mc);
         }

         for (HudElement element : visible) {
            element.render(mc, unit);
         }

         if (snapMode) {
            this.drawResetButton(mc, unit, visible);
         } else {
            this.resetButtonWidth = 0.0F;
            this.resetButtonHeight = 0.0F;
         }
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      Minecraft mc = MinecraftContext.mc;
      if (mc != null && event.getButton() == 0) {
         if (event.getAction() == 0) {
            if (this.draggedElement != null) {
               this.stopDrag(mc);
               event.cancel();
            }
         } else if (event.getAction() == 1 && mc.gui.screen() instanceof ChatScreen) {
            float mouseX = mouseX(mc);
            float mouseY = mouseY(mc);
            if (controlDown(mc) && inside(mouseX, mouseY, this.resetButtonX, this.resetButtonY, this.resetButtonWidth, this.resetButtonHeight)) {
               this.resetPositions();
               event.cancel();
            } else if (!inside(mouseX, mouseY, mc.getWindow().getGuiScaledWidth() - 150.0F, mc.getWindow().getGuiScaledHeight() - 40.0F, 146.0F, 20.0F)) {
               for (HudElement element : this.visibleElements()) {
                  if (element.startDrag(mouseX, mouseY)) {
                     this.draggedElement = element;
                     event.cancel();
                     return;
                  }
               }
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      if (this.draggedElement != null && MinecraftContext.mc != null) {
         this.stopDrag(MinecraftContext.mc);
      }
   }

   public void resetPositions() {
      this.draggedElement = null;
      this.clearSnapGuides();
      MenuConfigStore.resetHudPositions();
      this.allElements.forEach(HudElement::resetPosition);
   }

   private void applySnap(Minecraft mc, List<HudElement> visible) {
      HudElement dragged = this.draggedElement;
      if (dragged == null) {
         this.clearSnapGuides();
      } else {
         float screenWidth = mc.getWindow().getGuiScaledWidth();
         float screenHeight = mc.getWindow().getGuiScaledHeight();
         float maxX = Math.max(0.0F, screenWidth - dragged.width());
         float maxY = Math.max(0.0F, screenHeight - dragged.height());
         List<HudFeature.SnapCandidate> xCandidates = new ArrayList<>(20);
         List<HudFeature.SnapCandidate> yCandidates = new ArrayList<>(20);
         addCandidate(xCandidates, Math.min(12.0F, maxX), 12.0F, maxX);
         addCandidate(xCandidates, maxX / 2.0F, screenWidth / 2.0F, maxX);
         addCandidate(xCandidates, Math.max(0.0F, maxX - 12.0F), screenWidth - 12.0F, maxX);
         addCandidate(yCandidates, Math.min(12.0F, maxY), 12.0F, maxY);
         addCandidate(yCandidates, maxY / 2.0F, screenHeight / 2.0F, maxY);
         addCandidate(yCandidates, Math.max(0.0F, maxY - 12.0F), screenHeight - 12.0F, maxY);

         for (HudElement other : visible) {
            if (other != dragged && !(other.width() <= 0.5F) && !(other.height() <= 0.5F)) {
               float otherRight = other.x() + other.width();
               float otherBottom = other.y() + other.height();
               float otherCenterX = other.x() + other.width() / 2.0F;
               float otherCenterY = other.y() + other.height() / 2.0F;
               addCandidate(xCandidates, other.x(), other.x(), maxX);
               addCandidate(xCandidates, otherRight - dragged.width(), otherRight, maxX);
               addCandidate(xCandidates, otherCenterX - dragged.width() / 2.0F, otherCenterX, maxX);
               addCandidate(xCandidates, other.x() - dragged.width() - 6.0F, other.x() - 3.0F, maxX);
               addCandidate(xCandidates, otherRight + 6.0F, otherRight + 3.0F, maxX);
               addCandidate(yCandidates, other.y(), other.y(), maxY);
               addCandidate(yCandidates, otherBottom - dragged.height(), otherBottom, maxY);
               addCandidate(yCandidates, otherCenterY - dragged.height() / 2.0F, otherCenterY, maxY);
               addCandidate(yCandidates, other.y() - dragged.height() - 6.0F, other.y() - 3.0F, maxY);
               addCandidate(yCandidates, otherBottom + 6.0F, otherBottom + 3.0F, maxY);
            }
         }

         float x = Math.clamp(dragged.x(), 0.0F, maxX);
         float y = Math.clamp(dragged.y(), 0.0F, maxY);
         this.activeGuideX = Float.NaN;
         this.activeGuideY = Float.NaN;
         if (!dragged.isHorizontallyCentered()) {
            HudFeature.SnapCandidate snappedX = nearestCandidate(x, xCandidates);
            if (snappedX != null) {
               x = snappedX.position();
               this.activeGuideX = snappedX.guide();
            }
         }

         HudFeature.SnapCandidate snappedY = nearestCandidate(y, yCandidates);
         if (snappedY != null) {
            y = snappedY.position();
            this.activeGuideY = snappedY.guide();
         }

         dragged.snapTo(x, y);
      }
   }

   private static void addCandidate(List<HudFeature.SnapCandidate> candidates, float position, float guide, float maximum) {
      if (Float.isFinite(position) && position >= 0.0F && position <= maximum) {
         candidates.add(new HudFeature.SnapCandidate(position, guide));
      }
   }

   private static HudFeature.SnapCandidate nearestCandidate(float position, List<HudFeature.SnapCandidate> candidates) {
      HudFeature.SnapCandidate nearest = null;
      float nearestDistance = 8.0F;

      for (HudFeature.SnapCandidate candidate : candidates) {
         float distance = Math.abs(position - candidate.position());
         if (distance <= nearestDistance) {
            nearest = candidate;
            nearestDistance = distance;
         }
      }

      return nearest;
   }

   private void drawSnapGrid(Minecraft mc) {
      float width = mc.getWindow().getGuiScaledWidth();
      float height = mc.getWindow().getGuiScaledHeight();
      float line = (float)Math.max(0.5, 1.0 / Math.max(1.0, mc.getWindow().getGuiScale()));
      int thirdsColor = ColorUtil.rgba(190, 195, 205, 36);
      int centerColor = ColorUtil.rgba(205, 209, 216, 58);
      int cornerColor = ColorUtil.rgba(210, 214, 220, 78);
      float left = 12.0F;
      float top = 12.0F;
      float right = width - 12.0F;
      float bottom = height - 12.0F;
      float cornerLength = 10.0F;
      drawDashedVertical(width / 3.0F, top, bottom, 1.5F, 6.0F, line, thirdsColor);
      drawDashedVertical(width * 2.0F / 3.0F, top, bottom, 1.5F, 6.0F, line, thirdsColor);
      drawDashedHorizontal(height / 3.0F, left, right, 1.5F, 6.0F, line, thirdsColor);
      drawDashedHorizontal(height * 2.0F / 3.0F, left, right, 1.5F, 6.0F, line, thirdsColor);
      drawDashedVertical(width / 2.0F, top, bottom, 4.0F, 6.0F, line, centerColor);
      drawDashedHorizontal(height / 2.0F, left, right, 4.0F, 6.0F, line, centerColor);
      Render2DUtil.rect(left, top, cornerLength, line).color(cornerColor).draw();
      Render2DUtil.rect(left, top, line, cornerLength).color(cornerColor).draw();
      Render2DUtil.rect(right - cornerLength, top, cornerLength, line).color(cornerColor).draw();
      Render2DUtil.rect(right - line, top, line, cornerLength).color(cornerColor).draw();
      Render2DUtil.rect(left, bottom - line, cornerLength, line).color(cornerColor).draw();
      Render2DUtil.rect(left, bottom - cornerLength, line, cornerLength).color(cornerColor).draw();
      Render2DUtil.rect(right - cornerLength, bottom - line, cornerLength, line).color(cornerColor).draw();
      Render2DUtil.rect(right - line, bottom - cornerLength, line, cornerLength).color(cornerColor).draw();
      if (Float.isFinite(this.activeGuideX)) {
         Render2DUtil.rect(this.activeGuideX - line * 2.0F, 0.0F, line * 4.0F, height).color(ColorUtil.rgba(205, 210, 218, 30)).draw();
         Render2DUtil.rect(this.activeGuideX - line / 2.0F, 0.0F, line, height).color(ColorUtil.rgba(220, 224, 230, 175)).draw();
      }

      if (Float.isFinite(this.activeGuideY)) {
         Render2DUtil.rect(0.0F, this.activeGuideY - line * 2.0F, width, line * 4.0F).color(ColorUtil.rgba(205, 210, 218, 30)).draw();
         Render2DUtil.rect(0.0F, this.activeGuideY - line / 2.0F, width, line).color(ColorUtil.rgba(220, 224, 230, 175)).draw();
      }
   }

   private static void drawDashedVertical(float x, float top, float bottom, float dash, float gap, float width, int color) {
      float y = top;

      while (y < bottom) {
         Render2DUtil.rect(x - width / 2.0F, y, width, Math.min(dash, bottom - y)).color(color).draw();
         y += dash + gap;
      }
   }

   private static void drawDashedHorizontal(float y, float left, float right, float dash, float gap, float height, int color) {
      float x = left;

      while (x < right) {
         Render2DUtil.rect(x, y - height / 2.0F, Math.min(dash, right - x), height).color(color).draw();
         x += dash + gap;
      }
   }

   private void clearSnapGuides() {
      this.activeGuideX = Float.NaN;
      this.activeGuideY = Float.NaN;
   }

   private void drawResetButton(Minecraft mc, float unit, List<HudElement> visible) {
      float height = 28.0F * unit;
      float radius = 8.0F * unit;
      float padding = 8.0F * unit;
      float gap = 4.0F * unit;
      float iconSize = 12.0F * unit;
      float dividerWidth = Math.max(0.5F, 0.5F * unit);
      float textSize = 10.0F * unit;
      MsdfFont font = UiFonts.sfProDisplay();
      float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
      float textWidth = font.measureWidth("Сбросить расположение", textSize, letterSpacing);
      this.resetButtonWidth = padding * 2.0F + iconSize + gap * 2.0F + dividerWidth + textWidth;
      this.resetButtonHeight = height;
      this.resetButtonX = (mc.getWindow().getGuiScaledWidth() - this.resetButtonWidth) / 2.0F;
      this.resetButtonY = this.resolveResetButtonY(mc, visible);
      float mouseX = mouseX(mc);
      float mouseY = mouseY(mc);
      boolean hovered = inside(mouseX, mouseY, this.resetButtonX, this.resetButtonY, this.resetButtonWidth, this.resetButtonHeight);
      int foreground = hovered ? Theme.Colors.PRIMARY_BRIGHT : Theme.Colors.TEXT_TEXT;
      int iconColor = Theme.getAccent();
      float centerY = this.resetButtonY + height / 2.0F;
      Render2DUtil.rect(this.resetButtonX, this.resetButtonY, this.resetButtonWidth, height)
         .color(Theme.Colors.BACKGROUND_PRIMARY_50)
         .radius(radius)
         .border(Math.max(0.5F, 0.5F * unit), Theme.Colors.OUTLINES_MEDIUM)
         .blur(8.0F * unit)
         .draw();
      float cursor = this.resetButtonX + padding;
      Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, RESET_ICON).color(iconColor).draw();
      cursor += iconSize + gap;
      Render2DUtil.rect(cursor, centerY - iconSize / 2.0F, dividerWidth, iconSize).color(RESET_DIVIDER_COLOR).draw();
      cursor += dividerWidth + gap;
      Render2DUtil.text(cursor, font.centeredTextY(centerY, textSize), textSize, "Сбросить расположение").style(UiFontStyle.MEDIUM).color(foreground).draw();
   }

   private float resolveResetButtonY(Minecraft mc, List<HudElement> visible) {
      float preferredY = 18.0F;
      float minY = 4.0F;
      float maxY = Math.max(minY, mc.getWindow().getGuiScaledHeight() - this.resetButtonHeight - minY);

      for (float offset = 0.0F; offset <= maxY + preferredY; offset += 2.0F) {
         float above = preferredY - offset;
         if (above >= minY && this.resetButtonFits(visible, above)) {
            return above;
         }

         float below = preferredY + offset;
         if (offset > 0.0F && below <= maxY && this.resetButtonFits(visible, below)) {
            return below;
         }
      }

      return Math.min(preferredY, maxY);
   }

   private boolean resetButtonFits(List<HudElement> visible, float y) {
      float gap = 4.0F;

      for (HudElement element : visible) {
         if (element.overlaps(this.resetButtonX - gap, y - gap, this.resetButtonWidth + gap * 2.0F, this.resetButtonHeight + gap * 2.0F)) {
            return false;
         }
      }

      return true;
   }

   private List<HudElement> visibleElements() {
      List<HudElement> visible = new ArrayList<>(4);
      if (this.elements.isSelected("Watermark")) {
         visible.add(this.watermarkElement);
      }

      if (this.elements.isSelected("Coordinates")) {
         visible.add(this.coordsElement);
      }

      if (this.elements.isSelected("Keybinds")) {
         visible.add(this.keybindsElement);
      }

      if (this.elements.isSelected("Potions")) {
         visible.add(this.potionsElement);
      }

      if (this.elements.isSelected("Target")) {
         visible.add(this.targetElement);
      }

      if (this.elements.isSelected("Notifications")) {
         visible.add(this.notificationsElement);
      }

      if (this.elements.isSelected("Hotbar")) {
         visible.add(this.hotbarElement);
      }

      if (this.elements.isSelected("ArmorHud")) {
         visible.add(this.armorHudElement);
      }

      if (this.elements.isSelected("QuickUse")) {
         visible.add(this.quickUseElement);
      }

      if (this.elements.isSelected("StaffList")) {
         visible.add(this.staffListElement);
      }

      if (PveManagerFeature.INSTANCE.currentState.getValue()) {
         visible.add(this.pveStatusElement);
      }

      MineHelperFeature mineHelper = FeatureManager.INSTANCE.getFeature(MineHelperFeature.class);
      if (mineHelper != null && mineHelper.isMineTimerSelected()) {
         visible.add(this.mineTimerElement);
      }

      return visible;
   }

   private void stopDrag(Minecraft mc) {
      this.draggedElement.stopDrag(mc);
      this.draggedElement = null;
      this.clearSnapGuides();
   }

   private static boolean controlDown(Minecraft mc) {
      long window = mc.getWindow().handle();
      return GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
   }

   private static float mouseX(Minecraft mc) {
      return (float)mc.mouseHandler.getScaledXPos(mc.getWindow());
   }

   private static float mouseY(Minecraft mc) {
      return (float)mc.mouseHandler.getScaledYPos(mc.getWindow());
   }

   private static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private void migrateLegacySize() {
      if (!this.sizeMigrationChecked) {
         this.sizeMigrationChecked = true;
         if (!MenuConfigStore.getBoolean("hudScale100Migrated", false)) {
            if (this.size.getValue() == 70.0) {
               this.size.setValue(100.0);
            }

            MenuConfigStore.save(data -> data.addProperty("hudScale100Migrated", true));
         }
      }
   }

   private record SnapCandidate(float position, float guide) {
   }
}

