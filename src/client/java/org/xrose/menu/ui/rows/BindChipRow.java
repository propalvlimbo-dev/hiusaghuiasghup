package org.xrose.menu.ui.rows;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.misc.ServerHelperFeature;
import org.xrose.feature.setting.InputBindSetting;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.controls.InputBindComponent;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;

public final class BindChipRow extends SettingRow {
   private static final int ICON_SIZE = 16;
   private static final int ICON_GAP = 2;
   private final InputBindSetting bindSetting;
   private final InputBindComponent chip;

   public BindChipRow(InputBindSetting setting) {
      super(setting);
      this.bindSetting = setting;
      this.chip = new InputBindComponent(setting);
   }

   @Override
   protected void placeControl(Component owner) {
      this.chip.place(owner, this.controlX(), this.controlY(), this.controlWidth(), this.controlHeight()).alpha(this.rowAlpha);
   }

   @Override
   public boolean click(int mouseX, int mouseY) {
      this.host.closeOtherRows(this);
      this.chip.handleClick(mouseX, mouseY);
      return true;
   }

   @Override
   public boolean key(int key) {
      return this.chip.captureKey(key);
   }

   @Override
   public boolean captureMouse(int button) {
      return this.chip.captureMouse(button);
   }

   @Override
   public boolean isCapturingBind() {
      return this.chip.isListening();
   }

   @Override
   public void closeTransient() {
      this.chip.stopListening();
   }

   @Override
   protected void renderControl(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      this.chip.render(minecraft, guiGraphicsExtractor);
   }

   @Override
   protected void renderExtras(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      ItemStack icon = this.icon();
      if (!icon.isEmpty()) {
         this.renderItemIcon(minecraft, guiGraphicsExtractor, this.iconScreenX(), this.iconScreenY(), icon);
      }
   }

   @Override
   protected int labelColor() {
      return Theme.Colors.TEXT_TEXT;
   }

   @Override
   protected int labelOffsetX() {
      return this.icon().isEmpty() ? 0 : 18;
   }

   private float iconScreenX() {
      return this.sx(this.rowX + 16);
   }

   private float iconScreenY() {
      return this.sy(this.rowY + 12.0F);
   }

   @Override
   protected int controlWidth() {
      return InputBindComponent.pillWidth(MenuText.bind(this.bindSetting.getDisplayValue()));
   }

   @Override
   protected int controlHeight() {
      return 20;
   }

   private ItemStack icon() {
      if (!"ServerHelper".equals(this.featureName)) {
         return ItemStack.EMPTY;
      }

      ServerHelperFeature helper = FeatureManager.INSTANCE.getFeature(ServerHelperFeature.class);
      return helper == null ? ItemStack.EMPTY : helper.iconForBind(this.bindSetting);
   }

   private void renderItemIcon(Minecraft minecraft, GuiGraphicsExtractor extractor, float x, float y, ItemStack stack) {
      LocalPlayer player = minecraft.player;
      if (player != null) {
         float size = this.px(16.0F);
         int left = Math.round(x);
         int top = Math.round(y);
         int right = Math.round(x + size);
         int bottom = Math.round(y + size);
         ScreenRectangle clip = new ScreenRectangle(left, top, right - left, bottom - top);
         ScreenRectangle pageClip = Render2DUtil.currentScissor();
         if (pageClip != null) {
            clip = clip.intersection(pageClip);
            if (clip == null) {
               return;
            }
         }

         ScreenRectangle extractorClip = extractor.scissorStack.peek();
         if (extractorClip != null) {
            ScreenRectangle next = clip.intersection(extractorClip);
            if (next == null) {
               return;
            }

            clip = next;
         }

         if (clip.width() > 0 && clip.height() > 0) {
            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();
            float guiScale = minecraft.getWindow().getGuiScale();
            float scale = size / 16.0F;
            float itemX = Math.round(x * guiScale) / guiScale;
            float itemY = Math.round(y * guiScale) / guiScale;
            extractor.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
            pose.pushMatrix();

            try {
               pose.translate(itemX, itemY);
               pose.scale(scale);
               extractor.item(player, stack, 0, 0, 0);
            } finally {
               pose.popMatrix();
               extractor.disableScissor();
            }
         }
      }
   }
}

