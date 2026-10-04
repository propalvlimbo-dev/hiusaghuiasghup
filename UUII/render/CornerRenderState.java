package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record CornerRenderState(
        Matrix3x2fc pose,
        float x,
        float y,
        float size,
        Corner corner,
        int colorTop,
        int colorBottom,
        float gradientOriginY,
        float gradientHeight,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final int SEGMENTS = 12;

    public enum Corner {
        TOP_LEFT(180), TOP_RIGHT(270), BOTTOM_RIGHT(0), BOTTOM_LEFT(90);

        final int startAngle;

        Corner(int startAngle) {
            this.startAngle = startAngle;
        }
    }

    public CornerRenderState(Matrix3x2fc pose, float x, float y, float size, Corner corner,
                             int colorTop, int colorBottom, float gradientOriginY, float gradientHeight,
                             ScreenRectangle scissorArea) {
        this(pose, x, y, size, corner, colorTop, colorBottom, gradientOriginY, gradientHeight,
                scissorArea, computeBounds(pose, x, y, size, scissorArea));
    }

    private static ScreenRectangle computeBounds(Matrix3x2fc pose, float x, float y, float size,
                                                 ScreenRectangle scissorArea) {
        ScreenRectangle rectangle = new ScreenRectangle(
                (int) Math.floor(x) - 1,
                (int) Math.floor(y) - 1,
                (int) Math.ceil(size) + 2,
                (int) Math.ceil(size) + 2
        ).transformMaxBounds(pose);
        return scissorArea == null ? rectangle : scissorArea.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float centerX = switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> x + size;
            case TOP_RIGHT, BOTTOM_RIGHT -> x;
        };
        float centerY = switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> y + size;
            case BOTTOM_RIGHT, BOTTOM_LEFT -> y;
        };

        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a0 = Math.toRadians(corner.startAngle + 90.0 * segment / SEGMENTS);
            double a1 = Math.toRadians(corner.startAngle + 90.0 * (segment + 1) / SEGMENTS);
            float x0 = centerX + (float) Math.cos(a0) * size;
            float y0 = centerY + (float) Math.sin(a0) * size;
            float x1 = centerX + (float) Math.cos(a1) * size;
            float y1 = centerY + (float) Math.sin(a1) * size;
            double am = (a0 + a1) * 0.5;
            float xm = centerX + (float) Math.cos(am) * size;
            float ym = centerY + (float) Math.sin(am) * size;

            vertex(consumer, centerX, centerY);
            vertex(consumer, x1, y1);
            vertex(consumer, xm, ym);
            vertex(consumer, x0, y0);
        }
    }

    private void vertex(VertexConsumer consumer, float vx, float vy) {
        float progress = gradientHeight <= 0 ? 0
                : Math.clamp((vy - gradientOriginY) / gradientHeight, 0f, 1f);
        consumer.addVertexWith2DPose(pose, vx, vy)
                .setColor(ColorUtil.interpolate(colorTop, colorBottom, progress));
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
