package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record TriangleRenderState(
        Matrix3x2fc pose, float x1, float y1, float x2, float y2, float x3, float y3,
        int color, ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    public TriangleRenderState(Matrix3x2fc pose, float x1, float y1, float x2, float y2,
                               float x3, float y3, int color, ScreenRectangle scissorArea) {
        this(pose, x1, y1, x2, y2, x3, y3, color, scissorArea,
                bounds(pose, x1, y1, x2, y2, x3, y3, scissorArea));
    }

    private static ScreenRectangle bounds(Matrix3x2fc pose, float x1, float y1, float x2, float y2,
                                          float x3, float y3, ScreenRectangle scissor) {
        float minX = Math.min(x1, Math.min(x2, x3));
        float minY = Math.min(y1, Math.min(y2, y3));
        float maxX = Math.max(x1, Math.max(x2, x3));
        float maxY = Math.max(y1, Math.max(y2, y3));
        ScreenRectangle rectangle = new ScreenRectangle((int) Math.floor(minX) - 1, (int) Math.floor(minY) - 1,
                Math.max(1, (int) Math.ceil(maxX - minX) + 2),
                Math.max(1, (int) Math.ceil(maxY - minY) + 2)).transformMaxBounds(pose);
        return scissor == null ? rectangle : scissor.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        vertex(consumer, x1, y1);
        vertex(consumer, x2, y2);
        vertex(consumer, x3, y3);
        vertex(consumer, x3, y3);
    }

    private void vertex(VertexConsumer consumer, float x, float y) {
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
