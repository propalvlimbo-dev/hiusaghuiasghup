package ru.rooyzee.elytrixclient.client.ui.kit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Базовый элемент кастомного GUI. Позицию задаёт контейнер ({@link UiSection}) через
 * {@link #place(int, int, int)}, рисуется в абсолютных координатах экрана.
 */
public abstract class UiWidget {
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
}
