package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record DiamondOutlineRenderState(
        Matrix3x2fc pose, float centerX, float centerY, float radius, float thickness,
        int color, ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    public DiamondOutlineRenderState(Matrix3x2fc pose, float centerX, float centerY,
                                     float radius, float thickness, int color,
                                     ScreenRectangle scissorArea) {
        this(pose, centerX, centerY, radius, thickness, color, scissorArea,
                bounds(pose, centerX, centerY, radius, scissorArea));
    }

    private static ScreenRectangle bounds(Matrix3x2fc pose, float centerX, float centerY,
                                          float radius, ScreenRectangle scissorArea) {
        ScreenRectangle rectangle = new ScreenRectangle(
                (int) Math.floor(centerX - radius) - 2, (int) Math.floor(centerY - radius) - 2,
                (int) Math.ceil(radius * 2) + 4, (int) Math.ceil(radius * 2) + 4)
                .transformMaxBounds(pose);
        return scissorArea == null ? rectangle : scissorArea.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        float inner = Math.max(0f, radius - thickness);

        int steps = 8;
        for (int edge = 0; edge < 4; edge++) {
            for (int s = 0; s < steps; s++) {
                float t0 = edge + s / (float) steps;
                float t1 = edge + (s + 1) / (float) steps;
                float[] o1 = point(radius, t0);
                float[] o2 = point(radius, t1);
                float[] i1 = point(inner, t0);
                float[] i2 = point(inner, t1);
                consumer.addVertexWith2DPose(pose, o1[0], o1[1]).setColor(color);
                consumer.addVertexWith2DPose(pose, i1[0], i1[1]).setColor(color);
                consumer.addVertexWith2DPose(pose, i2[0], i2[1]).setColor(color);
                consumer.addVertexWith2DPose(pose, o2[0], o2[1]).setColor(color);
            }
        }
    }

    private float[] point(float r, float t) {
        double angle = t * Math.PI / 2.0;
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double sum = Math.abs(sin) + Math.abs(cos);

        double bulge = 1.0 + (sum - 1.0) * 0.18;
        return new float[]{
                centerX + (float) (sin / sum * bulge * r),
                centerY - (float) (cos / sum * bulge * r)
        };
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
