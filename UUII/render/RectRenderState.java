package wtf.expensive.client.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.joml.Matrix3x2fc;

public record RectRenderState(
        Matrix3x2fc pose,
        float x,
        float y,
        float width,
        float height,
        int colorTop,
        int colorBottom,
        float gradientOriginY,
        float gradientHeight,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {
    public RectRenderState(Matrix3x2fc pose, float x, float y, float width, float height,
                           int colorTop, int colorBottom, float gradientOriginY, float gradientHeight,
                           ScreenRectangle scissorArea) {
        this(pose, x, y, width, height, colorTop, colorBottom, gradientOriginY, gradientHeight, scissorArea,
                computeBounds(pose, x, y, width, height, scissorArea));
    }

    private static ScreenRectangle computeBounds(Matrix3x2fc pose, float x, float y, float width, float height,
                                                 ScreenRectangle scissorArea) {
        ScreenRectangle rect = new ScreenRectangle(
                (int) Math.floor(x) - 1, (int) Math.floor(y) - 1,
                (int) Math.ceil(width) + 2, (int) Math.ceil(height) + 2
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        vertex(consumer, x, y);
        vertex(consumer, x, y + height);
        vertex(consumer, x + width, y + height);
        vertex(consumer, x + width, y);
    }

    private void vertex(VertexConsumer consumer, float vx, float vy) {
        consumer.addVertexWith2DPose(pose, vx, vy).setColor(colorAt(vy));
    }

    private int colorAt(float vy) {
        if (colorTop == colorBottom || gradientHeight <= 0) {
            return colorTop;
        }
        float t = Math.clamp((vy - gradientOriginY) / gradientHeight, 0f, 1f);
        return ColorUtil.interpolate(colorTop, colorBottom, t);
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
