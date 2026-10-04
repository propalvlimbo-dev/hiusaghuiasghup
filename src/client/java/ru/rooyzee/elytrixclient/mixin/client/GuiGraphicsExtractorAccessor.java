package ru.rooyzee.elytrixclient.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Доступ к списку элементов кадра у {@link GuiGraphicsExtractor}.
 *
 * <p>Ваниль умеет класть туда только свои формы (заливка, спрайты, текст).
 * Кастомному GUI нужен {@code GuiRenderState.addGuiElement} — так можно отдать
 * собственную векторную геометрию (скругления, градиенты, кольца) и получить
 * её в том же батче, что и остальной интерфейс.
 */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {

    @Accessor("guiRenderState")
    GuiRenderState elytrix$guiRenderState();
}
