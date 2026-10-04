package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record SoftShadowRenderState(
        Matrix3x2fc pose,
        float x,
        float y,
        float width,
        float height,
        float radius,
        float spread,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final int CORNER_SEGMENTS = 12;
    private static final float[] STOPS = {0f, .16f, .38f, .68f, 1f};
    private static final float[] ALPHA = {.52f, .34f, .16f, .045f, 0f};

    public SoftShadowRenderState(Matrix3x2fc pose, float x, float y, float width, float height,
                                 float radius, float spread, int color, ScreenRectangle scissorArea) {
        this(pose, x, y, width, height, radius, spread, color, scissorArea,
                computeBounds(pose, x, y, width, height, spread, scissorArea));
    }

    private static ScreenRectangle computeBounds(Matrix3x2fc pose, float x, float y, float width, float height,
                                                 float spread, ScreenRectangle scissorArea) {
        ScreenRectangle rectangle = new ScreenRectangle(
                (int) Math.floor(x - spread) - 1,
                (int) Math.floor(y - spread) - 1,
                (int) Math.ceil(width + spread * 2) + 2,
                (int) Math.ceil(height + spread * 2) + 2
        ).transformMaxBounds(pose);
        return scissorArea == null ? rectangle : scissorArea.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float r = Math.clamp(radius, 0, Math.min(width, height) / 2f);
        for (int band = 0; band < STOPS.length - 1; band++) {
            float d0 = spread * STOPS[band];
            float d1 = spread * STOPS[band + 1];
            int inner = alpha(ALPHA[band]);
            int outer = alpha(ALPHA[band + 1]);

            quad(consumer, x + r, y - d1, outer, x + r, y - d0, inner,
                    x + width - r, y - d0, inner, x + width - r, y - d1, outer);
            quad(consumer, x + r, y + height + d0, inner, x + r, y + height + d1, outer,
                    x + width - r, y + height + d1, outer, x + width - r, y + height + d0, inner);
            quad(consumer, x - d1, y + r, outer, x - d1, y + height - r, outer,
                    x - d0, y + height - r, inner, x - d0, y + r, inner);
            quad(consumer, x + width + d0, y + r, inner, x + width + d0, y + height - r, inner,
                    x + width + d1, y + height - r, outer, x + width + d1, y + r, outer);

            corner(consumer, x + r, y + r, r, d0, d1, 180, inner, outer);
            corner(consumer, x + width - r, y + r, r, d0, d1, 270, inner, outer);
            corner(consumer, x + width - r, y + height - r, r, d0, d1, 0, inner, outer);
            corner(consumer, x + r, y + height - r, r, d0, d1, 90, inner, outer);
        }
    }

    private void corner(VertexConsumer consumer, float cx, float cy, float radius,
                        float d0, float d1, int startAngle, int inner, int outer) {
        for (int segment = 0; segment < CORNER_SEGMENTS; segment++) {
            double a0 = Math.toRadians(startAngle + 90.0 * segment / CORNER_SEGMENTS);
            double a1 = Math.toRadians(startAngle + 90.0 * (segment + 1) / CORNER_SEGMENTS);
            float ix0 = cx + (float) Math.cos(a0) * (radius + d0);
            float iy0 = cy + (float) Math.sin(a0) * (radius + d0);
            float ox0 = cx + (float) Math.cos(a0) * (radius + d1);
            float oy0 = cy + (float) Math.sin(a0) * (radius + d1);
            float ox1 = cx + (float) Math.cos(a1) * (radius + d1);
            float oy1 = cy + (float) Math.sin(a1) * (radius + d1);
            float ix1 = cx + (float) Math.cos(a1) * (radius + d0);
            float iy1 = cy + (float) Math.sin(a1) * (radius + d0);
            quad(consumer, ox0, oy0, outer, ix0, iy0, inner,
                    ix1, iy1, inner, ox1, oy1, outer);
        }
    }

    private int alpha(float multiplier) {
        return RenderUtil.withAlpha(color, Math.round(((color >>> 24) & 0xFF) * multiplier));
    }

    private void quad(VertexConsumer consumer,
                      float x0, float y0, int c0, float x1, float y1, int c1,
                      float x2, float y2, int c2, float x3, float y3, int c3) {
        consumer.addVertexWith2DPose(pose, x0, y0).setColor(c0);
        consumer.addVertexWith2DPose(pose, x1, y1).setColor(c1);
        consumer.addVertexWith2DPose(pose, x2, y2).setColor(c2);
        consumer.addVertexWith2DPose(pose, x3, y3).setColor(c3);
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
