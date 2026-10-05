package ru.rooyzee.elytrixclient.client.render.font;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;
import ru.rooyzee.elytrixclient.mixin.client.GuiGraphicsExtractorAccessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилита для очереди и отправки render states.
 * Полный порт из delta-26.2 DeltaRenderUtil.
 */
public final class ElytrixRenderUtil {
    private static final List<GuiElementRenderState> QUEUED = new ArrayList<>(128);

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
                distanceRange, fontTexture, glyphs, totalWidth, totalHeight, scissor
        );
        QUEUED.add(state);
    }
}