package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record TexturedQuadRenderState(
        Matrix3x2fc pose, TextureSetup textureSetup, float[] corners, int color,
        ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final float[] UV = {0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f};

    public TexturedQuadRenderState(Matrix3x2fc pose, TextureSetup textureSetup, float[] corners,
                                   int color, ScreenRectangle scissorArea) {
        this(pose, textureSetup, corners, color, scissorArea, computeBounds(pose, corners, scissorArea));
    }

    private static ScreenRectangle computeBounds(Matrix3x2fc pose, float[] corners, ScreenRectangle scissor) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i + 1 < corners.length; i += 2) {
            minX = Math.min(minX, corners[i]);
            maxX = Math.max(maxX, corners[i]);
            minY = Math.min(minY, corners[i + 1]);
            maxY = Math.max(maxY, corners[i + 1]);
        }
        ScreenRectangle rectangle = new ScreenRectangle(
                (int) Math.floor(minX) - 1, (int) Math.floor(minY) - 1,
                Math.max(1, (int) Math.ceil(maxX - minX) + 2),
                Math.max(1, (int) Math.ceil(maxY - minY) + 2)).transformMaxBounds(pose);
        return scissor == null ? rectangle : scissor.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (int i = 0; i < 4; i++) {
            consumer.addVertexWith2DPose(pose, corners[i * 2], corners[i * 2 + 1])
                    .setUv(UV[i * 2], UV[i * 2 + 1])
                    .setColor(color);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }
}
