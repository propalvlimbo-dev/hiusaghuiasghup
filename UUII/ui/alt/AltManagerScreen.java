package wtf.expensive.client.ui.alt;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.User;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.mixin.MinecraftAccessor;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;

public class AltManagerScreen extends Screen {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/backmenu.png");
    private static final Identifier BLURRED_BACKGROUND =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/backmenu_blurred.png");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private static final int PANEL = ColorUtil.rgba(19, 21, 27, 84);
    private static final int OUTLINE = ColorUtil.rgba(127, 132, 150, 84);
    private static final int FIELD = ColorUtil.rgba(0, 0, 0, 56);
    private static final int LABEL = ColorUtil.rgba(161, 164, 177, 255);

    private static final float LAYOUT_WIDTH = 960;
    private static final float LAYOUT_HEIGHT = 505;

    private String altName = "";
    private boolean typing;
    private float scroll;
    private float scrollAnimated;

    private final Screen parent;

    public AltManagerScreen(Screen parent) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
        AltConfig.load();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float originX() {
        return 0;
    }

    private float originY() {
        return 0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        RenderUtil.texture(graphics, BACKGROUND, 0, 0, graphics.guiWidth(), graphics.guiHeight(), -1);

        float ox = originX();
        float oy = originY();

        renderLoginPanel(graphics, ox, oy, mouseX, mouseY);
        renderAccountList(graphics, ox, oy, mouseX, mouseY);
    }

    private void renderLoginPanel(GuiGraphicsExtractor graphics, float ox, float oy, int mouseX, int mouseY) {
        float x = ox + 110;
        float y = oy + 163.5f;
        float w = 251;
        float h = 240.5f;

        panel(graphics, x, y, w, h);

        StyledFontRenderer.draw(graphics, font, Component.literal("Менеджер аккаунтов").withStyle(Fonts.SEMIBOLD_20),
                (int) (x + 21), (int) (y + 25), -1);

        StyledFontRenderer.draw(graphics, font, Component.literal("Никнейм").withStyle(Fonts.REGULAR_14),
                (int) (x + 21), (int) (y + 47), LABEL);

        RenderUtil.roundedRect(graphics, x + 21, y + 55, 209.5f, 23, 5, FIELD);
        RenderUtil.scissor(graphics, x + 21, y + 55, 204.5f, 23);
        String shown = altName + (typing && System.currentTimeMillis() % 1000 > 500 ? "_" : "");
        StyledFontRenderer.draw(graphics, font, Component.literal(shown).withStyle(Fonts.SEMIBOLD_14),
                (int) (x + 28), (int) (y + 62), LABEL);
        RenderUtil.unscissor(graphics);

        StyledFontRenderer.draw(graphics, font, Component.literal("Пароль").withStyle(Fonts.REGULAR_14),
                (int) (x + 21), (int) (y + 86), LABEL);
        float passWidth = StyledFontRenderer.width(font,Component.literal("Пароль").withStyle(Fonts.REGULAR_14));
        StyledFontRenderer.draw(graphics, font, Component.literal("(если требуется)").withStyle(Fonts.REGULAR_14),
                (int) (x + 25 + passWidth), (int) (y + 86), ColorUtil.rgba(54, 55, 59, 255));
        RenderUtil.roundedRect(graphics, x + 21, y + 93, 209.5f, 23, 5, FIELD);

        button(graphics, ox + 172.5f, oy + 332, "Рандомный ник", mouseX, mouseY);
        button(graphics, ox + 172.5f, oy + 361.5f, "Войти", mouseX, mouseY);

        String username = minecraft == null ? "" : minecraft.getUser().getName();
        StyledFontRenderer.drawCentered(graphics, font,
                Component.literal("Вы успешно зашли под никнеймом " + username).withStyle(Fonts.REGULAR_14),
                x + 125.5f, y + h + 10, ColorUtil.rgba(142, 145, 157, 255));
    }

    private void button(GuiGraphicsExtractor graphics, float x, float y, String label, int mouseX, int mouseY) {
        boolean hovered = RenderUtil.isHovered(mouseX, mouseY, x, y, 124.5f, 23);
        RenderUtil.roundedRect(graphics, x, y, 124.5f, 23, 5, FIELD);
        StyledFontRenderer.drawCentered(graphics, font, Component.literal(label).withStyle(Fonts.REGULAR_18),
                (int) (x + 62), (int) (y + 7), hovered ? -1 : LABEL);
    }

    private void panel(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        RenderUtil.scissor(graphics, x, y, width, height);
        RenderUtil.texture(graphics, BLURRED_BACKGROUND, 0, 0,
                graphics.guiWidth(), graphics.guiHeight(), -1);
        RenderUtil.unscissor(graphics);
        RenderUtil.roundedRect(graphics, x, y, width, height, 6, PANEL);
        RenderUtil.roundedOutline(graphics, x, y, width, height, 6, 0.75f, OUTLINE);
    }

    private void renderAccountList(GuiGraphicsExtractor graphics, float ox, float oy, int mouseX, int mouseY) {
        float x = ox + 389;
        float y = oy + 149;
        float w = 461.5f;
        float h = 269.5f;

        panel(graphics, x, y, w, h);

        StyledFontRenderer.draw(graphics, font, Component.literal("Список аккаунтов").withStyle(Fonts.SEMIBOLD_20),
                (int) (x + 15), (int) (y + 15), -1);

        scrollAnimated = AnimationMath.fast(scrollAnimated, scroll, 5f);

        RenderUtil.scissor(graphics, x, y + 35, w, h - 40);
        float cardWidth = 98.5f;
        float cardHeight = 130.5f;
        float cardY = y + 60;

        for (int i = 0; i < Account.ACCOUNTS.size(); i++) {
            Account account = Account.ACCOUNTS.get(i);
            float cardX = Math.round(x + 15 + (i + scrollAnimated) * (cardWidth + 10));
            if (cardX + cardWidth < x || cardX > x + w) {
                continue;
            }

            boolean active = minecraft != null && account.name.equalsIgnoreCase(minecraft.getUser().getName());
            RenderUtil.roundedRect(graphics, cardX, cardY, cardWidth, cardHeight, 8,
                    active ? ColorUtil.rgba(40, 44, 56, 140) : FIELD);

            RenderUtil.roundedRect(graphics, cardX + 12.5f, cardY + 10, 74, 74, 7,
                    ColorUtil.rgba(0, 0, 0, 90));

            PlayerFaceExtractor.extractRenderState(graphics, account.skin,
                    Math.round(cardX + 12.5f), Math.round(cardY + 10), 74, -1);

            StyledFontRenderer.drawCentered(graphics, font, Component.literal(account.name).withStyle(Fonts.SEMIBOLD_14),
                    (int) (cardX + cardWidth / 2), (int) (cardY + 93), active ? -1 : LABEL);
            StyledFontRenderer.drawCentered(graphics, font,
                    Component.literal(DATE.format(Instant.ofEpochMilli(account.added))).withStyle(Fonts.REGULAR_14),
                    (int) (cardX + cardWidth / 2), (int) (cardY + 106), ColorUtil.rgba(100, 100, 100, 255));
        }
        RenderUtil.unscissor(graphics);

        int size = Account.ACCOUNTS.size();
        int visible = (int) ((w - 30) / (cardWidth + 10));
        scroll = Mth.clamp(scroll, size > visible ? -(size - visible) : 0, 0);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll += (float) scrollY;
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        float ox = originX();
        float oy = originY();
        double mx = event.x();
        double my = event.y();

        if (RenderUtil.isHovered(mx, my, ox + 172.5f, oy + 332, 124.5f, 23)) {
            addAccount(randomName());
            return true;
        }
        if (RenderUtil.isHovered(mx, my, ox + 172.5f, oy + 361.5f, 124.5f, 23)) {
            addAccount(altName);
            return true;
        }
        if (RenderUtil.isHovered(mx, my, ox + 131, oy + 218.5f, 209.5f, 23)) {
            typing = !typing;
            return true;
        }

        float x = ox + 389;
        float y = oy + 149;
        float cardWidth = 98.5f;
        float cardHeight = 130.5f;
        float cardY = y + 60;

        for (int i = 0; i < Account.ACCOUNTS.size(); i++) {
            float cardX = Math.round(x + 15 + (i + scrollAnimated) * (cardWidth + 10));
            if (cardX < x || cardX + cardWidth > x + 461.5f) {
                continue;
            }
            if (RenderUtil.isHovered(mx, my, cardX, cardY, cardWidth, cardHeight)) {
                if (event.button() == 0) {
                    login(Account.ACCOUNTS.get(i));
                } else {
                    Account.ACCOUNTS.remove(i);
                    AltConfig.save();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void login(Account account) {
        if (minecraft == null) {
            return;
        }
        altName = account.name;
        UUID uuid = Account.resolveUUIDOffline(account.name);
        ((MinecraftAccessor) minecraft).expensive$setUser(
                new User(account.name, uuid, "", Optional.empty(), Optional.empty()));
    }

    private void addAccount(String name) {
        if (name == null || name.isBlank() || name.length() >= 20) {
            return;
        }
        Account.ACCOUNTS.add(new Account(name, System.currentTimeMillis()));
        AltConfig.save();
        altName = "";
    }

    private static String randomName() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            builder.append((char) ThreadLocalRandom.current().nextInt('a', 'z' + 1));
        }
        return builder.toString();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (typing) {
            if (event.key() == GLFW_KEY_BACKSPACE) {
                if (!altName.isEmpty()) {
                    altName = altName.substring(0, altName.length() - 1);
                }
                return true;
            }
            if (event.key() == GLFW_KEY_ENTER) {
                addAccount(altName);
                return true;
            }
            if (event.isEscape()) {
                typing = false;
                return true;
            }
        }
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (typing && event.isAllowedChatCharacter() && altName.length() < 20) {
            altName += event.codepointAsString();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        AltConfig.save();
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}
