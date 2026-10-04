package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Базовый элемент кастомного GUI.
 *
 * <p>Позицию задаёт контейнер ({@link UiSection}) через {@link #place(int, int, int)}.
 * Каждый виджет сам хранит состояние анимаций ({@link #appear}, {@link #hoverT}, {@link #pressT}) —
 * их обновляет {@link #tick(float, boolean)} в начале {@code render(...)}, поэтому виджеты
 * плавно «въезжают» при открытии вкладки, плавно подсвечиваются под курсором и «прожимаются»
 * при клике. Анимации можно выключить целиком ({@link #ANIMATIONS} = false) — тогда всё
 * применяется мгновенно.
 */
public abstract class UiWidget {
    /** Глобальный тумблер анимаций (ставит экран из настроек). */
    public static boolean ANIMATIONS = true;

    public int x;
    public int y;
    public int w;
    public int h;
    public boolean hovered;
    public boolean enabled = true;
    public boolean visible = true;
    /** Подсказка при наведении (может быть null). */
    public String tooltip;
    /** Границы, за которые не должен вылезать всплывающий список (задаёт экран). */
    public int popupLimitTop = 0;
    public int popupLimitBottom = Integer.MAX_VALUE;
    /** Акцент текущей темы — проставляется экраном перед отрисовкой. */
    public int accent = UiTheme.ACCENTS[0];

    /** 0…1 — прогресс появления виджета (анимация входа). */
    public float appear;
    /** 0…1 — сглаженное состояние наведения. */
    public float hoverT;
    /** 1…0 — затухание «нажатия». */
    public float pressT;
    /** Задержка перед стартом анимации появления, сек (для «каскада» строк). */
    protected float delay;

    protected UiWidget(int height) {
        this.h = height;
    }

    /** Ставит элемент в точку и растягивает по ширине. */
    public UiWidget place(int x, int y, int w) {
        this.x = x;
        this.y = y;
        this.w = w;
        return this;
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Перезапускает анимацию появления (вызывается при смене вкладки). */
    public void replay() {
        replay(0f);
    }

    /** Перезапускает анимацию появления с задержкой (каскад строк). */
    public void replay(float delaySeconds) {
        this.delay = ANIMATIONS ? Math.max(0f, delaySeconds) : 0f;
        this.appear = ANIMATIONS ? 0f : 1f;
        this.hoverT = 0f;
        this.pressT = 0f;
    }

    /** Обновляет состояние анимаций. Вызывается из {@code render(...)}. */
    protected void tick(float dt, boolean isHovered) {
        hovered = isHovered;
        float kh = ANIMATIONS ? Math.min(1f, dt * 11f) : 1f;
        hoverT += ((isHovered ? 1f : 0f) - hoverT) * kh;
        if (delay > 0f) {
            delay -= dt;
            return;
        }
        float ka = ANIMATIONS ? Math.min(1f, dt * 7f) : 1f;
        appear += (1f - appear) * ka;
        if (appear > 0.999f) {
            appear = 1f;
        }
        pressT = Math.max(0f, pressT - (ANIMATIONS ? dt * 3.2f : 1f));
    }

    /** Смещение виджета вниз при анимации появления. */
    protected int enterOffset() {
        return ANIMATIONS ? Math.round(Math.min(1f, 1f - appear) * 8f) : 0;
    }

    /** Цвет с учётом прозрачности появления. */
    protected int fadeIn(int color) {
        return UiTheme.withAlpha(color, Math.max(0.05f, appear));
    }

    /** Вызывать в начале render: обновляет состояние и сдвигает виджет при появлении. */
    protected void beginFrame(int mouseX, int mouseY, float dt) {
        boolean over = enabled && visible && contains(mouseX, mouseY);
        tick(dt, over);
        y += enterOffset();
    }

    /** Вызывать в конце render: возвращает исходную позицию. */
    protected void endFrame() {
        y -= enterOffset();
    }

    public abstract void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        return false;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        return false;
    }

    public boolean charTyped(char c) {
        return false;
    }

    /** Мигает ли у элемента текстовый курсор (для полей ввода). */
    public boolean wantsCursor() {
        return false;
    }

    /** Открыт ли у элемента «всплывающий» список (у выпадающих — да). */
    public boolean hasPopup() {
        return false;
    }

    /** Попадает ли точка во всплывающий список (проверяется раньше обычных кликов). */
    public boolean popupContains(double mx, double my) {
        return false;
    }

    public void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float dt) {
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Обрезает строку по ширине и добавляет «…». */
    protected static String trim(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (font.width(sb.toString() + c + ellipsis) > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb + ellipsis;
    }
}
