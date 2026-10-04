package wtf.expensive.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.util.animation.AnimationMath;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.RenderUtil;

public final class UnHookScreen extends Screen {
    private static final long TEXT_DELAY = 1500L;
    private static final long TOTAL = 6000L;
    private static final float LOGO_SCALE = 65f / 30f;

    private final long start = System.currentTimeMillis();
    private float fade;
    private float textProgress;
    private boolean finished;

    public UnHookScreen() {
        super(Component.literal("UNHOOK"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        long elapsed = System.currentTimeMillis() - start;
        boolean closing = elapsed >= TOTAL;

        fade = AnimationMath.fast(fade, closing ? 0f : 1f, 12f);
        textProgress = AnimationMath.fast(textProgress, elapsed >= TEXT_DELAY && !closing ? 1f : 0f, 8f);

        RenderUtil.roundedRect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0,
                ColorUtil.rgba(0, 0, 0, (int) (190 * fade)));

        float centerX = graphics.guiWidth() / 2f;
        float centerY = graphics.guiHeight() / 2f - 50 * textProgress;
        float scale = (2f - Mth.clamp(fade, 0f, 1f)) * LOGO_SCALE;
        int iconAlpha = (int) (255 * Mth.clamp(fade, 0f, 1f));

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().rotate((float) Math.toRadians(360 * (1f - Mth.clamp(fade, 0f, 1f))));
        graphics.pose().translate(-centerX, -centerY);
        StyledFontRenderer.drawCentered(graphics, font, Component.literal("H").withStyle(Fonts.ICONS_130),
                centerX, centerY - 45, RenderUtil.withAlpha(-1, iconAlpha));
        graphics.pose().popMatrix();

        if (textProgress > 0.01f) {
            long remaining = Mth.clamp((TOTAL - 1000 - elapsed) / 1000, 0, 5);
            String suffix = remaining > 1 ? "секунды" : remaining == 1 ? "секунда" : "секунд";
            StyledFontRenderer.drawCentered(graphics, font,
                    Component.literal("Самоуничтожение через: " + remaining + " " + suffix)
                            .withStyle(Fonts.SEMIBOLD_20),
                    centerX, graphics.guiHeight() / 2f + 20,
                    RenderUtil.withAlpha(-1, (int) (255 * textProgress)));
        }

        if (closing && !finished) {
            finished = true;
            if (Managment.FUNCTION_MANAGER != null && Managment.FUNCTION_MANAGER.unhook != null) {
                Managment.FUNCTION_MANAGER.unhook.unhook();
            }
            onClose();
        }
    }
}
