package ru.rooyzee.elytrixclient.client.render.font;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;
import ru.rooyzee.elytrixclient.mixin.client.GuiGraphicsExtractorAccessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Утилита для очереди и отправки render states.
 * Полный порт из delta-26.2 DeltaRenderUtil.
 */
public final class ElytrixRenderUtil {
    private static final List<GuiElementRenderState> QUEUED = new ArrayList<>(128);

    // ── текущий scissor ─────────────────────────────────────────────────
    //
    // Ваниль хранит активные прямоугольники обрезки в GuiGraphicsExtractor
    // (поле scissorStack со стеком внутри) и применяет их только к своим
    // элементам. Наш текст — свой GuiElementRenderState, поэтому обрезку
    // нужно прочитать и передать ему явно: иначе прокручивающийся заголовок
    // в плеере и списки с enableScissor(...) рисуются поверх всей панели.
    // Тот же приём, что в DeltaRenderUtil (delta-26.2).
    private static final Field SCISSOR_STACK_FIELD;
    private static final Field SCISSOR_DEQUE_FIELD;

    static {
        Field scissorStack = null;
        Field deque = null;
        try {
            scissorStack = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            scissorStack.setAccessible(true);
            deque = scissorStack.getType().getDeclaredField("stack");
            deque.setAccessible(true);
        } catch (Exception ignored) {
            // поля нет/переименовано — просто рисуем без обрезки
        }
        SCISSOR_STACK_FIELD = scissorStack;
        SCISSOR_DEQUE_FIELD = deque;
    }

    /** Пересечение всех активных scissor-прямоугольников (в единицах GUI) или null. */
    public static ScreenRectangle currentScissor(GuiGraphicsExtractor extractor) {
        if (extractor == null || SCISSOR_STACK_FIELD == null || SCISSOR_DEQUE_FIELD == null) {
            return null;
        }
        try {
            Object stack = SCISSOR_STACK_FIELD.get(extractor);
            if (stack == null) {
                return null;
            }
            Deque<?> entries = (Deque<?>) SCISSOR_DEQUE_FIELD.get(stack);
            if (entries == null || entries.isEmpty()) {
                return null;
            }
            ScreenRectangle result = null;
            for (Object entry : entries) {
                ScreenRectangle rect = (ScreenRectangle) entry;
                if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
                    continue;
                }
                result = result == null ? rect : result.intersection(rect);
                if (result == null || result.width() <= 0 || result.height() <= 0) {
                    return null;
                }
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ElytrixRenderUtil() {}

    public static void beginFrame() {
        QUEUED.clear();
    }

    public static void flush(GuiGraphicsExtractor extractor) {
        if (extractor == null) {
            QUEUED.clear();
            return;
        }
        GuiGraphicsExtractorAccessor accessor = (GuiGraphicsExtractorAccessor) extractor;
        for (GuiElementRenderState renderState : QUEUED) {
            accessor.elytrix$guiRenderState().addGuiElement(renderState);
        }
        QUEUED.clear();
    }

    public static void queueText(
            GuiGraphicsExtractor extractor,
            float x, float y,
            float size,
            int color,
            int outlineColor,
            float outlineThickness,
            float fontWeight,
            float distanceRange,
            Identifier fontTexture,
            List<MtsdfTextRenderState.GlyphData> glyphs,
            float totalWidth,
            float totalHeight,
            ScreenRectangle scissor
    ) {
        Matrix3x2fc pose = extractor.pose();
        MtsdfTextRenderState state = new MtsdfTextRenderState(
                pose, x, y, size, color, outlineColor, outlineThickness, fontWeight,
                distanceRange, fontTexture, glyphs, totalWidth, totalHeight,
                scissor != null ? scissor : currentScissor(extractor)
        );
        QUEUED.add(state);
    }

    /**
     * То же, что {@link #queueText}, но элемент сразу уходит в кадр.
     *
     * <p>Нужно для текста, который рисуется вперемешку с остальным GUI: порядок
     * элементов в {@code GuiRenderState} и есть порядок отрисовки, поэтому
     * отложенный flush до конца кадра выкинул бы весь текст поверх панелей,
     * всплывающих списков и тултипов.
     */
    public static void queueTextNow(
            GuiGraphicsExtractor extractor,
            float x, float y,
            float size,
            int color,
            int outlineColor,
            float outlineThickness,
            float fontWeight,
            float distanceRange,
            Identifier fontTexture,
            List<MtsdfTextRenderState.GlyphData> glyphs,
            float totalWidth,
            float totalHeight,
            ScreenRectangle scissor
    ) {
        queueText(extractor, x, y, size, color, outlineColor, outlineThickness, fontWeight,
                distanceRange, fontTexture, glyphs, totalWidth, totalHeight, scissor);
        flush(extractor);
    }
}
