package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record GradientRectRenderState(
        Matrix3x2fc pose, float x, float y, float width, float height,
        int topLeft, int bottomLeft, int bottomRight, int topRight,
        ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    public GradientRectRenderState(Matrix3x2fc pose, float x, float y, float width, float height,
                                   int topLeft, int bottomLeft, int bottomRight, int topRight,
                                   ScreenRectangle scissorArea) {
        this(pose, x, y, width, height, topLeft, bottomLeft, bottomRight, topRight,
                scissorArea, bounds(pose, x, y, width, height, scissorArea));
    }

    private static ScreenRectangle bounds(Matrix3x2fc pose, float x, float y, float width, float height,
                                          ScreenRectangle scissorArea) {
        ScreenRectangle rectangle = new ScreenRectangle((int) Math.floor(x) - 1, (int) Math.floor(y) - 1,
                (int) Math.ceil(width) + 2, (int) Math.ceil(height) + 2).transformMaxBounds(pose);
        return scissorArea == null ? rectangle : scissorArea.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        vertex(consumer, x, y, topLeft);
        vertex(consumer, x, y + height, bottomLeft);
        vertex(consumer, x + width, y + height, bottomRight);
        vertex(consumer, x + width, y, topRight);
    }

    private void vertex(VertexConsumer consumer, float vx, float vy, int color) {
        consumer.addVertexWith2DPose(pose, vx, vy).setColor(color);
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
