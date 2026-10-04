package wtf.expensive.client.ui.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Matrix3x2f;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import wtf.expensive.client.Expensive;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.mixin.GuiGraphicsExtractorAccessor;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.Type;
import wtf.expensive.client.modules.impl.render.Hud;
import wtf.expensive.client.util.BetterText;
import wtf.expensive.client.util.ClientUtil;
import wtf.expensive.client.util.KeyUtil;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.drag.DragManager;
import wtf.expensive.client.util.drag.Dragging;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class HudRenderer implements HudElement {
    private static final float ROUND = 3f;
    private static final float COLOR_STRIP = 4f;
    private static final int PANEL = new Color(0, 0, 0, 128).getRGB();
    private static final int TEXT = Color.WHITE.getRGB();

    private static final float SMALL_FONT_HEIGHT = 7f;
    private static final float SMALL_DUMB_OFFSET = 1.5f;

    private static final Identifier FPS_ICON = Identifier.fromNamespaceAndPath(
            Expensive.MOD_ID, "textures/images/animation.png");
    private static final Identifier PING_ICON = Identifier.fromNamespaceAndPath(
            Expensive.MOD_ID, "textures/images/wifi.png");

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern STAFF_PREFIX = Pattern.compile(
            ".*(mod|der|adm|help|wne|мод|хелп|помо|адм|владе|отри|таф|taf|curat|курато|dev|раз|supp|сапп|yt|ютуб).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static final Dragging KEY_BINDS = DragManager.create("KeyBinds-new", 10, 100);
    public static final Dragging STAFF_LIST = DragManager.create("StaffList-new", 10, 200);
    public static final Dragging TARGET_HUD = DragManager.create("TargetHUD-new", 10, 300);
    public static final Dragging TIMER_HUD = DragManager.create("TimerHUD-new", 10, 400);

    private final BetterText watermarkText = new BetterText(List.of(
            "Expensive",
            "uid: " + Managment.USER_PROFILE.getUid(),
            "user: " + Managment.USER_PROFILE.getName()), 2000);

    private final List<Function> functions = new ArrayList<>();
    private final List<StaffEntry> staffEntries = new ArrayList<>();

    private float keyBindHeight;
    private float keyBindWidth;
    private float staffHeight;
    private float staffWidth = 100;
    private float timerProgress;
    private float targetHealth;
    private float targetScale;
    private LivingEntity target;
    private boolean targetForward;
    private long targetAnimationTime;
    private long lastTargetFrame;
    private long lastStaffTick = -1;
    private float hudScale = 1f;
    private boolean glowEnabled;

    private record StaffEntry(Component prefix, String name) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (Managment.FUNCTION_MANAGER == null || Managment.STYLE_MANAGER == null) return;

        if (ClientUtil.legitMode) return;

        Hud hud = Managment.FUNCTION_MANAGER.hud;

        if (Managment.NOTIFICATION_MANAGER != null) {
            Managment.NOTIFICATION_MANAGER.render(graphics);
        }

        if (mc.gui.hud.isHidden() || !hud.isState()) return;

        int offset = hud.offset.getValue().intValue();
        boolean shadow = hud.shadow.get();
        glowEnabled = hud.glow.get();
        updateFunctions();
        updateStaff(mc);

        hudScale = hud.scale.getValue().floatValue();
        graphics.pose().pushMatrix();
        if (hudScale != 1f) {
            graphics.pose().scale(hudScale, hudScale);
        }

        if (hud.elements.get(0)) renderWatermark(graphics, mc, offset, shadow);
        if (hud.elements.get(1)) renderActiveFunctions(graphics, mc, offset);
        if (hud.elements.get(2)) renderCoordinates(graphics, mc, offset, shadow);
        if (hud.elements.get(3)) renderTarget(graphics, mc);
        if (hud.elements.get(4)) renderStaffList(graphics, mc, shadow);
        if (hud.elements.get(5)) renderKeyBinds(graphics, mc);
        if (hud.elements.get(6)) renderEffects(graphics, mc, offset);
        renderTimer(graphics, mc, shadow);
        graphics.pose().popMatrix();
    }

    private float screenWidth(GuiGraphicsExtractor graphics) {
        return graphics.guiWidth() / hudScale;
    }

    private float screenHeight(GuiGraphicsExtractor graphics) {
        return graphics.guiHeight() / hudScale;
    }

    private int color(int index) {
        return Managment.STYLE_MANAGER.getColor(index);
    }

    private void updateFunctions() {
        if (!functions.isEmpty() || Managment.FUNCTION_MANAGER.getFunctions().isEmpty()) return;
        for (Function function : Managment.FUNCTION_MANAGER.getFunctions()) {
            if (function.category != Type.Render) {
                functions.add(function);
            }
        }
        Font font = Minecraft.getInstance().font;
        functions.sort(Comparator.comparingDouble((Function f) -> width(font, f.name, Fonts.MEDIUM_14)).reversed());
    }

    private void updateStaff(Minecraft mc) {
        long tick = mc.level.getGameTime();
        if (tick == lastStaffTick) return;
        lastStaffTick = tick;
        staffEntries.clear();
        for (PlayerTeam team : mc.level.getScoreboard().getPlayerTeams().stream()
                .sorted(Comparator.comparing(PlayerTeam::getName)).toList()) {
            String members = team.getPlayers().toString();
            if (members.length() < 2) continue;
            members = members.substring(1, members.length() - 1);
            if (!NAME_PATTERN.matcher(members).matches()) continue;
            String prefix = team.getPlayerPrefix().getString().toLowerCase(Locale.ROOT);
            if (STAFF_PREFIX.matcher(prefix).matches()
                    || (Managment.STAFF_MANAGER != null && Managment.STAFF_MANAGER.isStaff(members))) {
                staffEntries.add(new StaffEntry(team.getPlayerPrefix(), members));
            }
        }
    }

    private void renderWatermark(GuiGraphicsExtractor graphics, Minecraft mc, int offset, boolean shadow) {
        Font font = mc.font;
        String text = watermarkText.get();
        float width = Math.max(80, width(font, text, Fonts.MEDIUM_16) + 40);
        float height = 16;

        if (shadow) {
            RenderUtil.shadow(graphics, offset + COLOR_STRIP, offset, width, height, ROUND,
                    RenderUtil.withAlpha(PANEL, 64));
        }
        RenderUtil.roundedRect(graphics, offset, offset, COLOR_STRIP, height,
                ROUND, 0, 0, ROUND, color(0), color(100));
        RenderUtil.roundedRect(graphics, offset + COLOR_STRIP, offset, width, height,
                0, ROUND, ROUND, 0, PANEL, PANEL);
        glow(graphics, offset, offset, COLOR_STRIP, height, ROUND, color(50));

        StyledFontRenderer.draw(graphics, font, Component.literal("H").withStyle(Fonts.ICONS_20),
                offset + COLOR_STRIP + height / 2, offset + 6, TEXT);
        StyledFontRenderer.drawCentered(graphics, font,
                Component.literal(text).withStyle(Fonts.MEDIUM_16),
                offset + COLOR_STRIP + width / 2 + 4, offset + 5.5f, TEXT);

        RenderUtil.texture(graphics, FPS_ICON, offset, offset + 21, 12, 12, color(0));
        String fps = mc.getFps() + "FPS";
        drawGradient(graphics, font, fps, Fonts.MEDIUM_16, offset + 15, offset + 25,
                color(0), color(90));

        RenderUtil.texture(graphics, PING_ICON, offset + width - 5, offset + 21, 12, 12, color(0));
        String ping = ping(mc) + "MS";
        float pingWidth = width(font, ping, Fonts.MEDIUM_16);
        drawGradient(graphics, font, ping, Fonts.MEDIUM_16,
                offset + width - 6 - pingWidth, offset + 25, color(0), color(90));
    }

    private void renderActiveFunctions(GuiGraphicsExtractor graphics, Minecraft mc, int offset) {
        Font font = mc.font;
        float padding = 4;

        float height = 11;

        List<Function> visible = new ArrayList<>();
        for (Function function : functions) {
            function.animation = AnimationMath.fast(function.animation,
                    function.isState() ? 1f : 0f, 15f);
            if (function.animation >= 0.1f) {
                visible.add(function);
            }
        }

        if (visible.isEmpty()) return;

        float index = 0;
        int visibleIndex = 0;
        for (Function function : visible) {
            float textWidth = width(font, function.name, Fonts.MEDIUM_14);

            float panelWidth = textWidth + padding * 2;
            float panelX = screenWidth(graphics) - offset - COLOR_STRIP - panelWidth;
            float y = offset + index * height;

            boolean first = visibleIndex == 0;
            boolean last = visibleIndex == visible.size() - 1;

            int railTop = color((int) (index * 10));
            int railBottom = color((int) ((index + 1) * 10));

            graphics.pose().pushMatrix();

            boolean animating = function.animation < 0.999f;
            if (animating) {
                graphics.pose().scaleAround(1f, function.animation, panelX, y);
            }

            RenderUtil.shadow(graphics, panelX, y, panelWidth, height, ROUND,
                    RenderUtil.withAlpha(PANEL, 64));

            if (glowEnabled) {
                RenderUtil.shadow(graphics, panelX + panelWidth, y, COLOR_STRIP, height, ROUND,
                        RenderUtil.withAlpha(ColorUtil.interpolate(railTop, railBottom, .5f), 150));
            }
            hudGradient(graphics, panelX + panelWidth, y, COLOR_STRIP, height,
                    0, first ? ROUND : 0, last ? ROUND : 0, 0,
                    railTop, railTop, railBottom, railBottom);

            RenderUtil.roundedRect(graphics, panelX, y, panelWidth, height,
                    first ? ROUND : 0, 0, 0, 0, PANEL, PANEL);

            StyledFontRenderer.draw(graphics, font,
                    Component.literal(function.name).withStyle(Fonts.MEDIUM_14),
                    Math.round(panelX + padding),
                    Math.round(y + (height - SMALL_FONT_HEIGHT) / 2f), TEXT);
            graphics.pose().popMatrix();
            index += function.animation;
            visibleIndex++;
        }
    }

    private void renderCoordinates(GuiGraphicsExtractor graphics, Minecraft mc, int offset, boolean shadow) {
        Font font = mc.font;
        double dx = mc.player.getX() - mc.player.xo;
        double dz = mc.player.getZ() - mc.player.zo;
        String[] texts = {
                "Coords: " + (int) mc.player.getX() + ", " + (int) mc.player.getY() + ", " + (int) mc.player.getZ(),
                "BPS: " + String.format(Locale.ROOT, "%.2f", Math.hypot(dx, dz) * 20)
        };
        float shift = 0;
        for (String text : texts) {
            float width = width(font, text, Fonts.MEDIUM_16) + 16;
            float height = 16;
            float y = screenHeight(graphics) - height - offset - shift;
            if (shadow) {
                RenderUtil.shadow(graphics, offset + COLOR_STRIP, y, width, height, ROUND,
                        RenderUtil.withAlpha(PANEL, 64));
            }
            RenderUtil.roundedRect(graphics, offset, y, COLOR_STRIP, height,
                    ROUND, 0, 0, ROUND, color(0), color(100));
            RenderUtil.roundedRect(graphics, offset + COLOR_STRIP, y, width, height,
                    0, ROUND, ROUND, 0, PANEL, PANEL);
            glow(graphics, offset, y, COLOR_STRIP, height, ROUND, color(50));
            StyledFontRenderer.drawCentered(graphics, font,
                    Component.literal(text).withStyle(Fonts.MEDIUM_16),
                    offset + COLOR_STRIP + width / 2, y + 5.5f, TEXT);
            shift += height + 3;
        }
    }

    private void renderTarget(GuiGraphicsExtractor graphics, Minecraft mc) {
        long now = System.currentTimeMillis();
        TARGET_HUD.setWidth(100);
        TARGET_HUD.setHeight(38);

        if (lastTargetFrame == 0) {
            lastTargetFrame = now;
        }
        long elapsed = Math.min(100L, Math.max(0L, now - lastTargetFrame));
        lastTargetFrame = now;

        LivingEntity requested = findTarget(mc);
        if (requested != null) {
            target = requested;
            targetForward = true;
        } else {
            targetForward = false;
        }
        targetAnimationTime = Math.clamp(targetAnimationTime + (targetForward ? elapsed : -elapsed), 0L, 400L);
        float progress = targetAnimationTime / 400f;
        targetScale = easeBack(progress);
        if (targetScale <= 0.001f) {
            target = null;
            return;
        }
        if (target == null) return;

        float x = TARGET_HUD.getX();
        float y = TARGET_HUD.getY();
        float width = 100;
        float height = 38;

        targetHealth = Mth.clamp(AnimationMath.fast(targetHealth,
                target.getHealth() / target.getMaxHealth(), 5f), 0f, 1f);
        List<ItemStack> equipment = equipmentStacks(target);

        graphics.pose().pushMatrix();
        graphics.pose().scaleAround(targetScale, targetScale, x + width / 2, y + height / 2);
        RenderUtil.shadow(graphics, x, y, width, height, ROUND,
                RenderUtil.withAlpha(PANEL, 64));
        RenderUtil.roundedRect(graphics, x, y, width, height,
                ROUND, equipment.isEmpty() ? ROUND : 0, ROUND, ROUND, PANEL, PANEL);
        if (!equipment.isEmpty()) {
            RenderUtil.roundedRect(graphics, x + 40, y - 12, 60, 12,
                    ROUND, ROUND, 0, 0, PANEL, PANEL);
        }

        if (target instanceof AbstractClientPlayer clientPlayer) {
            PlayerFaceExtractor.extractRenderState(graphics, clientPlayer.getSkin(),
                    Math.round(x + 4), Math.round(y + 4), 24, TEXT);
        } else {
            int entityScale = 34;
            int anchorY = Math.round(y + 2 + target.getBbHeight() * entityScale);
            RenderUtil.scissor(graphics, x + 4, y + 4, 24, 24);
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics,
                    Math.round(x + 4), Math.round(y + 4),
                    Math.round(x + 28), anchorY,
                    entityScale, 0.0625f, 0f, 0f, target);
            RenderUtil.unscissor(graphics);
        }

        String name = target.getName().getString();
        if (name.length() > 10) name = name.substring(0, 10);
        int health = (int) (Math.round((targetHealth * 20 + target.getAbsorptionAmount()) * 2f) / 2f);
        StyledFontRenderer.draw(graphics, mc.font, Component.literal(name).withStyle(Fonts.MEDIUM_16),
                x + 32, y + 6, TEXT);
        StyledFontRenderer.draw(graphics, mc.font,
                Component.literal("Health: " + health).withStyle(Fonts.GILROY_12),
                x + 32, y + 16, TEXT);
        StyledFontRenderer.draw(graphics, mc.font,
                Component.literal("Distance: " + String.format(Locale.ROOT, "%.1f", mc.player.distanceTo(target)))
                        .withStyle(Fonts.GILROY_12),
                x + 32, y + 22, TEXT);
        renderEquipment(graphics, equipment, x + 90, y - 11);

        float barWidth = width * targetHealth;
        if (barWidth > 0) {
            int left = RenderUtil.withAlpha(color(100), 255);
            int right = RenderUtil.withAlpha(color(0), 255);
            if (glowEnabled) {
                RenderUtil.shadow(graphics, x, y + height - 6, barWidth, 6, ROUND,
                        RenderUtil.withAlpha(color(50), 180));
            }
            hudGradient(graphics, x, y + height - 6, barWidth, 6,
                    0, targetHealth > 0.99f ? 0 : ROUND, ROUND, ROUND,
                    left, right, right, left);
        }
        graphics.pose().popMatrix();
    }

    private float easeBack(float progress) {
        if (progress >= 1f) return 1f;
        float shifted = progress - 1f;
        float easeAmount = 1.5f;
        return Math.max(0f, 1f + (easeAmount + 1f) * shifted * shifted * shifted
                + easeAmount * shifted * shifted);
    }

    private LivingEntity findTarget(Minecraft mc) {
        if (Managment.FUNCTION_MANAGER.auraFunction != null
                && Managment.FUNCTION_MANAGER.auraFunction.getTarget() != null) {
            return Managment.FUNCTION_MANAGER.auraFunction.getTarget();
        }
        return mc.gui.screen() instanceof ChatScreen ? mc.player : null;
    }

    private List<ItemStack> equipmentStacks(LivingEntity entity) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(entity instanceof Player player)) {
            return stacks;
        }
        stacks.add(player.getMainHandItem());
        stacks.add(player.getOffhandItem());
        stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
        stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
        stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
        stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
        stacks.removeIf(ItemStack::isEmpty);
        java.util.Collections.reverse(stacks);
        return stacks;
    }

    private void renderEquipment(GuiGraphicsExtractor graphics, List<ItemStack> stacks, float x, float y) {
        float itemX = x;
        for (ItemStack stack : stacks) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(itemX, y);
            graphics.pose().scale(0.6f, 0.6f);
            graphics.item(stack, 0, 0);
            graphics.pose().popMatrix();
            itemX -= 10;
        }
    }

    private void renderTimer(GuiGraphicsExtractor graphics, Minecraft mc, boolean shadow) {
        float x = TIMER_HUD.getX();
        float y = TIMER_HUD.getY();
        float width = 100;
        float height = 20;
        TIMER_HUD.setWidth(width);
        TIMER_HUD.setHeight(height);

        timerProgress = AnimationMath.lerp(timerProgress, 1f, 10f);
        String text = (int) (timerProgress * 100) + "%";
        if (shadow) {
            RenderUtil.shadow(graphics, x, y, width, height, ROUND,
                    RenderUtil.withAlpha(PANEL, 64));
        }
        RenderUtil.roundedRect(graphics, x, y, width, height, ROUND, PANEL);
        StyledFontRenderer.draw(graphics, mc.font, Component.literal("Timer").withStyle(Fonts.MEDIUM_16),
                x + 3, y + 5, TEXT);
        float textWidth = width(mc.font, text, Fonts.MEDIUM_16);
        StyledFontRenderer.draw(graphics, mc.font, Component.literal(text).withStyle(Fonts.MEDIUM_16),
                x + width - textWidth - 3, y + 5, TEXT);

        float barWidth = width * timerProgress;
        if (glowEnabled) {
            RenderUtil.shadow(graphics, x, y + height - 6, barWidth, 6, ROUND,
                    RenderUtil.withAlpha(color(50), 180));
        }
        hudGradient(graphics, x, y + height - 6, barWidth, 6,
                0, timerProgress >= 1f ? 0 : ROUND, ROUND, ROUND,
                color(100), color(0), color(0), color(100));
    }

    private void renderKeyBinds(GuiGraphicsExtractor graphics, Minecraft mc) {
        float x = KEY_BINDS.getX();
        float y = KEY_BINDS.getY();
        float bodyWidth = 100;
        float bodyHeight = 22;
        int rows = 0;
        for (Function function : Managment.FUNCTION_MANAGER.getFunctions()) {
            if (function.isState() && function.bind != 0) rows++;
        }
        float targetHeight = 22 + rows * 10;
        keyBindHeight = AnimationMath.fast(keyBindHeight, targetHeight, 10);

        float targetWidth = 100;
        for (Function function : Managment.FUNCTION_MANAGER.getFunctions()) {
            if (!function.isState() || function.bind == 0) continue;
            String key = KeyUtil.getKeyName(function.bind);
            if (key.length() > 4) key = key.substring(0, 4) + "..";
            String bindText = "[" + key.toUpperCase(Locale.ROOT) + "]";
            targetWidth = Math.max(targetWidth,
                    width(mc.font, bindText + function.bind + 5, Fonts.MEDIUM_14));
        }
        keyBindWidth = AnimationMath.fast(keyBindWidth, targetWidth, 10);
        KEY_BINDS.setWidth(targetWidth);
        KEY_BINDS.setHeight(targetHeight);

        RenderUtil.shadow(graphics, x + COLOR_STRIP, y, keyBindWidth, keyBindHeight, ROUND,
                RenderUtil.withAlpha(PANEL, 64));
        RenderUtil.roundedRect(graphics, x, y, COLOR_STRIP, keyBindHeight,
                ROUND, 0, 0, ROUND, color(0), color(100));
        RenderUtil.roundedRect(graphics, x + COLOR_STRIP, y, keyBindWidth, keyBindHeight,
                0, ROUND, ROUND, 0, PANEL, PANEL);
        glow(graphics, x, y, COLOR_STRIP, keyBindHeight, ROUND, color(50));
        StyledFontRenderer.draw(graphics, mc.font,
                Component.literal("Key Binds").withStyle(Fonts.MEDIUM_16),
                x + COLOR_STRIP + 4, y + 6, TEXT);
        RenderUtil.roundedRect(graphics, x + COLOR_STRIP, y + 16, keyBindWidth, 0.5f,
                0, color(100));

        RenderUtil.scissor(graphics, x, y, keyBindWidth, keyBindHeight);
        int index = 0;
        for (Function function : Managment.FUNCTION_MANAGER.getFunctions()) {
            if (!function.isState() || function.bind == 0) continue;
            String key = KeyUtil.getKeyName(function.bind);
            if (key.length() > 4) key = key.substring(0, 4) + "..";
            String bindText = "[" + key.toUpperCase(Locale.ROOT) + "]";
            String name = function.name;
            if (name.length() > 13) name = name.substring(0, 13) + "..";
            float bindWidth = width(mc.font, bindText, Fonts.MEDIUM_14);
            StyledFontRenderer.draw(graphics, mc.font,
                    Component.literal(name).withStyle(Fonts.MEDIUM_14),
                    x + COLOR_STRIP + 4, y + 22 + index * 10, TEXT);
            StyledFontRenderer.draw(graphics, mc.font,
                    Component.literal(bindText).withStyle(Fonts.MEDIUM_14),
                    x + COLOR_STRIP + keyBindWidth - bindWidth - 4,
                    y + 22 + index * 10, TEXT);
            index++;
        }
        RenderUtil.unscissor(graphics);
    }

    private void renderStaffList(GuiGraphicsExtractor graphics, Minecraft mc, boolean shadow) {
        float x = STAFF_LIST.getX();
        float y = STAFF_LIST.getY();
        float targetHeight = 22 + staffEntries.size() * 10;
        staffHeight = targetHeight;
        for (StaffEntry entry : staffEntries) {
            staffWidth = Math.max(staffWidth,
                    width(mc.font, entry.prefix().getString() + entry.name(), Fonts.MEDIUM_14) + COLOR_STRIP);
        }
        STAFF_LIST.setWidth(staffWidth);
        STAFF_LIST.setHeight(staffHeight);

        RenderUtil.shadow(graphics, x + COLOR_STRIP, y, staffWidth, staffHeight, ROUND,
                RenderUtil.withAlpha(PANEL, 64));
        RenderUtil.roundedRect(graphics, x, y, COLOR_STRIP, staffHeight,
                ROUND, 0, 0, ROUND, color(0), color(100));
        RenderUtil.roundedRect(graphics, x + COLOR_STRIP, y, staffWidth, staffHeight,
                0, ROUND, ROUND, 0, PANEL, PANEL);
        glow(graphics, x, y, COLOR_STRIP, staffHeight, ROUND, color(50));
        StyledFontRenderer.draw(graphics, mc.font,
                Component.literal("Staff List").withStyle(Fonts.MEDIUM_16),
                x + COLOR_STRIP + 4, y + 6, TEXT);
        RenderUtil.roundedRect(graphics, x + COLOR_STRIP, y + 16, staffWidth, 0.5f,
                0, color(100));

        int index = 0;
        for (StaffEntry entry : staffEntries) {
            float rowY = y + 22 + index * 10;
            Component prefix = entry.prefix().copy().withStyle(Fonts.MEDIUM_14);
            StyledFontRenderer.draw(graphics, mc.font, prefix, x + COLOR_STRIP + 4, rowY, TEXT);
            float prefixWidth = StyledFontRenderer.width(mc.font, prefix);
            StyledFontRenderer.draw(graphics, mc.font,
                    Component.literal(entry.name()).withStyle(Fonts.MEDIUM_14),
                    x + COLOR_STRIP + 4 + prefixWidth, rowY, TEXT);
            index++;
        }
    }

    private void renderEffects(GuiGraphicsExtractor graphics, Minecraft mc, int offset) {
        Font font = mc.font;
        float height = 12;
        int index = 0;
        List<MobEffectInstance> effects = mc.player.getActiveEffects().stream()
                .sorted(Comparator.comparingInt(MobEffectInstance::getDuration)).toList();

        for (MobEffectInstance effect : effects) {
            String text = Component.translatable(effect.getEffect().value().getDescriptionId()).getString()
                    + " " + Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)).getString()
                    + " - " + MobEffectUtil.formatDuration(effect, 1f, 20f).getString();
            float textWidth = width(font, text, Fonts.MEDIUM_14) + 12;
            float x = screenWidth(graphics) - offset - textWidth;
            float y = screenHeight(graphics) - offset - height - index * 16;

            RenderUtil.shadow(graphics, x, y, textWidth - COLOR_STRIP, height, ROUND,
                    RenderUtil.withAlpha(PANEL, 64));
            RenderUtil.roundedRect(graphics, x, y, textWidth - COLOR_STRIP, height,
                    ROUND, 0, 0, ROUND, PANEL, PANEL);
            RenderUtil.roundedRect(graphics, x + textWidth - COLOR_STRIP, y, COLOR_STRIP, height,
                    0, ROUND, ROUND, 0, color(index * 30), color(index * 30 + 30));
            glow(graphics, x + textWidth - COLOR_STRIP, y, COLOR_STRIP, height, ROUND, color(index * 30));

            StyledFontRenderer.draw(graphics, font,
                    Component.literal(text).withStyle(Fonts.MEDIUM_14),
                    x + 4, y + (height - SMALL_FONT_HEIGHT) / 2f, TEXT);
            index++;
        }
    }

    private int ping(Minecraft mc) {
        if (mc.getConnection() == null || mc.player == null) return 0;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info == null ? 0 : info.getLatency();
    }

    private float width(Font font, String text, net.minecraft.network.chat.Style style) {
        return StyledFontRenderer.width(font, Component.literal(text).withStyle(style));
    }

    private void drawGradient(GuiGraphicsExtractor graphics, Font font, String text,
                              net.minecraft.network.chat.Style style, float x, float y,
                              int first, int second) {
        StyledFontRenderer.drawGradient(graphics, font, text, style, x, y, first, second);
    }

    private void hudGradient(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                             float topLeftRadius, float topRightRadius, float bottomRightRadius,
                             float bottomLeftRadius, int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (width <= 0 || height <= 0) return;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        ScreenRectangle scissor = new ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight());
        state.addGuiElement(new HudRoundedGradientRenderState(pose, x, y, width, height,
                topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius,
                topLeft, topRight, bottomRight, bottomLeft, scissor));
    }

    private void glow(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                      float radius, int color) {
        if (!glowEnabled) return;
        RenderUtil.shadow(graphics, x, y, width, height, radius, RenderUtil.withAlpha(color, 120));
    }
}
