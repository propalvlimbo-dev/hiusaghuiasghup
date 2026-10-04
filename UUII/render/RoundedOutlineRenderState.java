package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record RoundedOutlineRenderState(
        Matrix3x2fc pose, float x, float y, float width, float height,
        float radius, float thickness, int color,
        ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final int SEGMENTS = 12;

    public RoundedOutlineRenderState(Matrix3x2fc pose, float x, float y, float width, float height,
                                     float radius, float thickness, int color, ScreenRectangle scissorArea) {
        this(pose, x, y, width, height, radius, thickness, color, scissorArea,
                computeBounds(pose, x, y, width, height, scissorArea));
    }

    private static ScreenRectangle computeBounds(Matrix3x2fc pose, float x, float y, float width, float height,
                                                 ScreenRectangle scissorArea) {
        ScreenRectangle rectangle = new ScreenRectangle((int) Math.floor(x) - 1, (int) Math.floor(y) - 1,
                (int) Math.ceil(width) + 2, (int) Math.ceil(height) + 2).transformMaxBounds(pose);
        return scissorArea == null ? rectangle : scissorArea.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float r = Math.clamp(radius, 0, Math.min(width, height) / 2f);
        float t = Math.clamp(thickness, 0, Math.min(width, height) / 2f);
        float innerRadius = Math.max(0, r - t);

        quad(consumer, x + r, y, x + r, y + t,
                x + width - r, y + t, x + width - r, y);
        quad(consumer, x + r, y + height - t, x + r, y + height,
                x + width - r, y + height, x + width - r, y + height - t);
        quad(consumer, x, y + r, x, y + height - r,
                x + t, y + height - r, x + t, y + r);
        quad(consumer, x + width - t, y + r, x + width - t, y + height - r,
                x + width, y + height - r, x + width, y + r);

        corner(consumer, x + r, y + r, r, innerRadius, 180);
        corner(consumer, x + width - r, y + r, r, innerRadius, 270);
        corner(consumer, x + width - r, y + height - r, r, innerRadius, 0);
        corner(consumer, x + r, y + height - r, r, innerRadius, 90);
    }

    private void corner(VertexConsumer consumer, float cx, float cy,
                        float outerRadius, float innerRadius, int startAngle) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a0 = Math.toRadians(startAngle + 90.0 * segment / SEGMENTS);
            double a1 = Math.toRadians(startAngle + 90.0 * (segment + 1) / SEGMENTS);
            quad(consumer,
                    cx + (float) Math.cos(a0) * outerRadius,
                    cy + (float) Math.sin(a0) * outerRadius,
                    cx + (float) Math.cos(a0) * innerRadius,
                    cy + (float) Math.sin(a0) * innerRadius,
                    cx + (float) Math.cos(a1) * innerRadius,
                    cy + (float) Math.sin(a1) * innerRadius,
                    cx + (float) Math.cos(a1) * outerRadius,
                    cy + (float) Math.sin(a1) * outerRadius);
        }
    }

    private void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1,
                      float x2, float y2, float x3, float y3) {
        consumer.addVertexWith2DPose(pose, x0, y0).setColor(color);
        consumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
        consumer.addVertexWith2DPose(pose, x2, y2).setColor(color);
        consumer.addVertexWith2DPose(pose, x3, y3).setColor(color);
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
