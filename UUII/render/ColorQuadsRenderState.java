package wtf.expensive.client.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public record ColorQuadsRenderState(
        Matrix3x2fc pose, float[] positions, int[] colors,
        ScreenRectangle scissorArea, ScreenRectangle bounds
) implements GuiElementRenderState {
    public ColorQuadsRenderState(Matrix3x2fc pose, float[] positions, int[] colors,
                                 ScreenRectangle scissorArea) {
        this(pose, positions, colors, scissorArea, computeBounds(pose, positions, scissorArea));
    }

    private static ScreenRectangle computeBounds(Matrix3x2fc pose, float[] positions,
                                                 ScreenRectangle scissor) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i + 1 < positions.length; i += 2) {
            minX = Math.min(minX, positions[i]);
            maxX = Math.max(maxX, positions[i]);
            minY = Math.min(minY, positions[i + 1]);
            maxY = Math.max(maxY, positions[i + 1]);
        }
        ScreenRectangle rectangle = new ScreenRectangle(
                (int) Math.floor(minX) - 1, (int) Math.floor(minY) - 1,
                Math.max(1, (int) Math.ceil(maxX - minX) + 2),
                Math.max(1, (int) Math.ceil(maxY - minY) + 2)).transformMaxBounds(pose);
        return scissor == null ? rectangle : scissor.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        int vertices = Math.min(colors.length, positions.length / 2) / 4 * 4;
        for (int i = 0; i < vertices; i++) {
            consumer.addVertexWith2DPose(pose, positions[i * 2], positions[i * 2 + 1])
                    .setColor(colors[i]);
        }
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
