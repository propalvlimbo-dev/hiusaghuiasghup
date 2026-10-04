package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record LineRenderState(
        Matrix3x2fc pose, float x1, float y1, float x2, float y2, float width,
        int startColor, int endColor, ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    public LineRenderState(Matrix3x2fc pose, float x1, float y1, float x2, float y2, float width,
                           int startColor, int endColor, ScreenRectangle scissorArea) {
        this(pose, x1, y1, x2, y2, width, startColor, endColor, scissorArea,
                bounds(pose, x1, y1, x2, y2, width, scissorArea));
    }

    private static ScreenRectangle bounds(Matrix3x2fc pose, float x1, float y1, float x2, float y2,
                                          float width, ScreenRectangle scissor) {
        float pad = Math.max(1f, width) + 1f;
        int x = (int) Math.floor(Math.min(x1, x2) - pad);
        int y = (int) Math.floor(Math.min(y1, y2) - pad);
        int w = (int) Math.ceil(Math.abs(x2 - x1) + pad * 2f);
        int h = (int) Math.ceil(Math.abs(y2 - y1) + pad * 2f);
        ScreenRectangle rectangle = new ScreenRectangle(x, y, Math.max(1, w), Math.max(1, h))
                .transformMaxBounds(pose);
        return scissor == null ? rectangle : scissor.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001f) {
            return;
        }
        float px = -dy / length * width * 0.5f;
        float py = dx / length * width * 0.5f;
        vertex(consumer, x1 + px, y1 + py, startColor);
        vertex(consumer, x1 - px, y1 - py, startColor);
        vertex(consumer, x2 - px, y2 - py, endColor);
        vertex(consumer, x2 + px, y2 + py, endColor);
    }

    private void vertex(VertexConsumer consumer, float x, float y, int color) {
        consumer.addVertexWith2DPose(pose, x, y).setColor(color);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }
}
