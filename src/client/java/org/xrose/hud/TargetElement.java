package org.xrose.hud;

import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.geom.ModelPart.Polygon;
import net.minecraft.client.model.geom.ModelPart.Vertex;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2fStack;
import org.xrose.context.RenderContext;
import org.xrose.feature.impl.visual.TargetESPFeature;
import org.xrose.mixin.accessor.ModelPartAccessor;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.MsdfFont;
import org.xrose.utils.render.gui.PlayerHead;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;

public final class TargetElement extends HudElement {
   private static final float S = 1.4F;
   private static final float W = 141.4F;
   private static final float H = 49.0F;
   private static final float RADIUS = 7.0F;
   private static final int TXT_COL = ColorUtil.rgba(255, 255, 255, 245);
   private static final int MUTED = ColorUtil.rgba(183, 190, 202, 215);
   private static final int SEP_COL = ColorUtil.rgba(150, 150, 158, 220);
   private static final int HP_BG = ColorUtil.rgba(40, 42, 50, 200);
   private static final int DUR_BG = ColorUtil.rgba(20, 20, 25, 200);
   private static final float HEAD_X = 8.4F;
   private static final float HEAD_Y = 5.6F;
   private static final float HEAD_SIZE = 26.6F;
   private static final float NAME_X = 40.6F;
   private static final float NAME_Y = 9.8F;
   private static final float NAME_SIZE = 8.4F;
   private static final float MAX_NAME_WIDTH = 43.399998F;
   private static final float HP_Y = 22.4F;
   private static final float DOT_SIZE = 4.9F;
   private static final float BAR_X = 8.4F;
   private static final float BAR_Y = 37.1F;
   private static final float BAR_H = 4.9F;
   private static final float ARMOR_SIZE = 11.2F;
   private static final float DUR_BAR_W = 8.4F;
   private static final float DUR_BAR_H = 1.6800001F;
   private static final float PAD = 16.8F;
   private static final float GAP = 5.6F;
   private static final float SEP_GAP = 2.8F;
   private static final float SEP_W = 1.4F;
   private static final float SEP_H = 8.4F;
   private static final float ARMOR_GAP = 11.2F;
   private static final float ARMOR_Y = 5.6F;
   private static final float ARMOR_OFF_X = 11.2F;
   private static final float DUR_OFF_X = -1.4F;
   private static final float DUR_OFF_Y = 12.599999F;
   private static final float DUR_R = 0.14F;
   private static final float BAR_R = 1.4F;
   private static final float SHADOW = 1.5F;
   private static final EquipmentSlot[] ARMOR_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private LivingEntity target;
   private LivingEntity fadeEntity;
   private float fadeAlpha;
   private long lastFadeTime;
   private LivingEntity smoothedFor;
   private float smoothedFraction;
   private long lastFrameTime;

   public TargetElement() {
      super("target", "Target");
   }

   @Override
   protected float defaultY(float unit) {
      return 340.0F * unit;
   }

   @Override
   protected void layout(Minecraft mc, float unit) {
      this.target = this.resolveTarget(mc);
      if (this.target != null) {
         this.fadeEntity = this.target;
      }

      this.updateFade();
      if (this.fadeEntity != null && (this.target != null || !(this.fadeAlpha <= 0.02F))) {
         this.width = 141.4F * unit;
         this.height = 49.0F * unit;
      } else {
         this.width = 0.0F;
         this.height = 0.0F;
      }
   }

   private void updateFade() {
      long now = System.currentTimeMillis();
      float delta = this.lastFadeTime == 0L ? 0.016F : (float)Math.min(100L, now - this.lastFadeTime) / 1000.0F;
      this.lastFadeTime = now;
      float goal = this.target != null ? 1.0F : 0.0F;
      float step = Math.clamp(delta * 12.0F, 0.0F, 1.0F);
      this.fadeAlpha = this.fadeAlpha + (goal - this.fadeAlpha) * step;
   }

   @Override
   protected void draw(Minecraft mc, float unit) {
      LivingEntity entity = this.target != null ? this.target : this.fadeEntity;
      if (entity != null) {
         float a = this.fadeAlpha;
         this.hudBackground(a, unit, 7.0F * unit);
         float x = this.x;
         float y = this.y;
         int accent = Theme.getAccent();
         float hx = x + 8.4F * unit;
         float hy = y + 5.6F * unit;
         float hs = 26.6F * unit;
         if (entity instanceof AbstractClientPlayer player) {
            Render2DUtil.rect(hx, hy, hs, hs).color(0).shadow(ColorUtil.multiplyAlpha(accent, 0.3F * a), hs * 0.6F).draw();
            PlayerHead.draw(hx, hy, hs, player.getSkin().body().texturePath(), 4.0F * unit, ColorUtil.multiplyAlpha(-1, a));
         } else {
            this.drawFaceIcon(entity, hx, hy, hs, a);
         }

         MsdfFont font = UiFonts.sfProDisplay();
         float nameSize = 8.4F * unit;
         float spacing = nameSize * UiFontStyle.SEMIBOLD.letterSpacingEm();
         String name = entity.getName().getString();
         float nx = x + 40.6F * unit;
         float ny = y + 9.8F * unit;
         String visibleName = font.ellipsize(name, nameSize, spacing, 43.399998F * unit);
         Render2DUtil.text(nx, ny, nameSize, visibleName).style(UiFontStyle.SEMIBOLD).color(ColorUtil.multiplyAlpha(TXT_COL, a)).draw();
         float hp = getEntityHealth(entity);
         float hpPct = Math.min(hp, 20.0F) / 20.0F;
         float fraction = this.smoothFraction(entity, hpPct);
         String hpTxt = (int)hp + "hp";
         float hpX = x + 40.6F * unit;
         float hpY = y + 22.4F * unit;
         Render2DUtil.text(hpX, hpY, nameSize, hpTxt).style(UiFontStyle.SEMIBOLD).color(ColorUtil.multiplyAlpha(accent, a)).draw();
         float hpW = font.measureWidth(hpTxt, nameSize, spacing);
         float sepX = hpX + hpW + 2.8F * unit;
         float sepY = hpY + 1.0F * unit;
         Render2DUtil.rect(sepX, sepY, 1.4F * unit, 8.4F * unit).color(ColorUtil.multiplyAlpha(SEP_COL, a)).radius(0.5F * unit).draw();
         float dist = mc.player != null ? mc.player.distanceTo(entity) : 0.0F;
         String distTxt = String.format(Locale.ROOT, "%.1f", dist);
         float dX = sepX + 1.4F * unit + 2.8F * unit;
         float dLblW = font.measureWidth("Distance:", nameSize, spacing);
         Render2DUtil.text(dX, hpY, nameSize, "Distance:").style(UiFontStyle.SEMIBOLD).color(ColorUtil.multiplyAlpha(MUTED, a)).draw();
         Render2DUtil.text(dX + dLblW + SMALL_GAP(unit), hpY, nameSize, distTxt).style(UiFontStyle.SEMIBOLD).color(ColorUtil.multiplyAlpha(accent, a)).draw();
         float bx = x + 8.4F * unit;
         float by = y + 37.1F * unit;
         float bw = 124.59999F * unit;
         float bh = 4.9F * unit;
         Render2DUtil.rect(bx, by, bw, bh).color(ColorUtil.multiplyAlpha(HP_BG, a)).radius(1.4F * unit).draw();
         float fw = bw * fraction;
         if (fw > 0.5F * unit) {
            Render2DUtil.rect(bx, by, fw, bh)
               .color(ColorUtil.multiplyAlpha(accent, a))
               .radius(1.4F * unit)
               .shadow(ColorUtil.multiplyAlpha(accent, 0.25F * a), bh * 1.5F)
               .draw();
         }

         if (entity instanceof Player player) {
            this.drawArmor(mc, player, x, y, unit, a);
         }
      }
   }

   private static float SMALL_GAP(float unit) {
      return 3.0F * unit;
   }

   private void drawFaceIcon(LivingEntity entity, float x, float y, float size, float alpha) {
      float radius = size * 0.2F;
      int tint = ColorUtil.multiplyAlpha(-1, alpha);
      Render2DUtil.rect(x, y, size, size).color(ColorUtil.multiplyAlpha(Theme.Colors.OUTLINES_MEDIUM, alpha)).radius(radius).draw();
      TargetElement.FaceIcon icon = this.resolveFaceIcon(entity);
      if (icon != null) {
         Render2DUtil.texture(x, y, size, size, icon.texture()).managed().uv(icon.u0(), icon.v0(), icon.u1(), icon.v1()).radius(radius).color(tint).draw();
      }
   }

   private void drawArmor(Minecraft mc, Player player, float x, float y, float unit, float alpha) {
      GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
      if (extractor != null) {
         Render2DUtil.flush();
         Matrix3x2fStack pose = extractor.pose();
         float guiScale = mc.getWindow().getGuiScale();
         float itemSize = 11.2F * unit;
         float scale = itemSize / 16.0F;
         float ay = y + 5.6F * unit;
         float ax = x + 141.4F * unit - 11.2F * unit;

         for (int i = 3; i >= 0; i--) {
            EquipmentSlot slot = ARMOR_ORDER[i];
            ItemStack stack = player.getItemBySlot(slot);
            ax -= 11.2F * unit;
            float itemX = Math.round(ax * guiScale) / guiScale;
            float itemY = Math.round(ay * guiScale) / guiScale;
            pose.pushMatrix();
            pose.translate(itemX, itemY);
            pose.scale(scale);
            extractor.item(stack, 0, 0);
            pose.popMatrix();
            if (!stack.isEmpty() && stack.isDamageableItem()) {
               float maxDur = stack.getMaxDamage();
               float curDur = maxDur - stack.getDamageValue();
               float durPct = Math.clamp(curDur / maxDur, 0.0F, 1.0F);
               float bW = 8.4F * unit;
               float bH = 1.6800001F * unit;
               float bX = ax - -1.4F * unit;
               float bY = ay + 12.599999F * unit;
               Render2DUtil.rect(bX, bY, bW, bH).color(ColorUtil.multiplyAlpha(DUR_BG, alpha)).radius(0.14F * unit).draw();
               if (durPct > 0.01F) {
                  float t = 1.0F - durPct;
                  int r = (int)(80.0F + t * 140.0F);
                  int g = (int)(220.0F - t * 140.0F);
                  Render2DUtil.rect(bX, bY, bW * durPct, bH).color(ColorUtil.multiplyAlpha(ColorUtil.rgba(r, g, 60, 255), alpha)).radius(0.14F * unit).draw();
               }
            }
         }
      }
   }

   private float smoothFraction(LivingEntity entity, float fraction) {
      long now = System.currentTimeMillis();
      float delta = this.lastFrameTime == 0L ? 0.016F : (float)Math.min(100L, now - this.lastFrameTime) / 1000.0F;
      this.lastFrameTime = now;
      if (this.smoothedFor != entity) {
         this.smoothedFor = entity;
         this.smoothedFraction = fraction;
      }

      float step = Math.clamp(delta * 10.0F, 0.0F, 1.0F);
      this.smoothedFraction = this.smoothedFraction + (fraction - this.smoothedFraction) * step;
      return this.smoothedFraction;
   }

   private static float getEntityHealth(LivingEntity entity) {
      float hp = entity.getHealth() + entity.getAbsorptionAmount();
      if (entity instanceof Player player && entity.level() != null) {
         Scoreboard scoreboard = entity.level().getScoreboard();
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
            } catch (Exception var8) {
            }
         }
      }

      return hp;
   }

   private TargetElement.FaceIcon resolveFaceIcon(LivingEntity entity) {
      EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
      EntityRenderState state = renderer.createRenderState(entity, 1.0F);
      if (renderer instanceof LivingEntityRenderer livingRenderer && state instanceof LivingEntityRenderState livingState) {
         Identifier texture = livingRenderer.getTextureLocation(livingState);
         Model<?> model = livingRenderer.getModel();
         ModelPart root = model.root();
         Function<String, ModelPart> lookup = root.createPartLookup();
         ModelPart iconPart = lookup.apply("head");
         if (iconPart == null) {
            iconPart = lookup.apply("body");
         }

         if (iconPart == null) {
            iconPart = root;
         }

         TargetElement.FaceIcon icon = findLargestFrontFace(texture, iconPart);
         return icon == null && iconPart != root ? findLargestFrontFace(texture, root) : icon;
      } else {
         return null;
      }
   }

   private static TargetElement.FaceIcon findLargestFrontFace(Identifier texture, ModelPart part) {
      TargetElement.FaceIcon result = null;
      float largestArea = 0.0F;

      for (ModelPart candidate : part.getAllParts()) {
         for (Cube cube : ((ModelPartAccessor)(Object)candidate).xrose$getCubes()) {
            for (Polygon polygon : cube.polygons) {
               if (!(polygon.normal().z() > -0.9F)) {
                  float minU = Float.POSITIVE_INFINITY;
                  float minV = Float.POSITIVE_INFINITY;
                  float maxU = Float.NEGATIVE_INFINITY;
                  float maxV = Float.NEGATIVE_INFINITY;

                  for (Vertex vertex : polygon.vertices()) {
                     minU = Math.min(minU, vertex.u());
                     minV = Math.min(minV, vertex.v());
                     maxU = Math.max(maxU, vertex.u());
                     maxV = Math.max(maxV, vertex.v());
                  }

                  float area = (maxU - minU) * (maxV - minV);
                  if (area > largestArea) {
                     largestArea = area;
                     result = new TargetElement.FaceIcon(texture, minU, minV, maxU, maxV);
                  }
               }
            }
         }
      }

      return result;
   }

   private LivingEntity resolveTarget(Minecraft mc) {
      LivingEntity current = TargetESPFeature.getCurrentTargetSafe();
      if (current == null && showcase(mc)) {
         current = mc.player;
      }

      return current != null && current.isAlive() ? current : null;
   }

   private record FaceIcon(Identifier texture, float u0, float v0, float u1, float v1) {
   }
}

