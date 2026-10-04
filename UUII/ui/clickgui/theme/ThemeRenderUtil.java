package wtf.expensive.client.ui.clickgui.theme;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import wtf.expensive.client.mixin.GuiGraphicsExtractorAccessor;
import wtf.expensive.client.util.render.ColorUtil;

public final class ThemeRenderUtil {
    private static final int SEGMENTS = 12;

    private ThemeRenderUtil() {
    }

    public static void gradientOutline(GuiGraphicsExtractor graphics,
                                       float x, float y, float width, float height,
                                       float radius, float thickness,
                                       int topLeft, int bottomLeft,
                                       int bottomRight, int topRight) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = fullScreen(graphics);
        ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState().addGuiElement(
                new GradientOutlineState(pose, x, y, width, height, radius, thickness,
                        topLeft, bottomLeft, bottomRight, topRight, scissor));
    }

    public static void gradientRounded(GuiGraphicsExtractor graphics,
                                       float x, float y, float width, float height,
                                       float radius, int topLeft, int bottomLeft,
                                       int bottomRight, int topRight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = fullScreen(graphics);
        ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState().addGuiElement(
                new GradientRoundedState(pose, x, y, width, height, radius,
                        topLeft, bottomLeft, bottomRight, topRight, scissor));
    }

    public static void gradientTexture(GuiGraphicsExtractor graphics, Identifier id,
                                       float x, float y, float width, float height,
                                       int topLeft, int bottomLeft,
                                       int bottomRight, int topRight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = fullScreen(graphics);
        TextureSetup setup = TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState().addGuiElement(
                new GradientTextureState(pose, setup, x, y, width, height,
                        topLeft, bottomLeft, bottomRight, topRight, scissor));
    }

    private static ScreenRectangle fullScreen(GuiGraphicsExtractor graphics) {
        return new ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight());
    }

    private static int bilinear(float x, float y, float left, float top, float width, float height,
                                int topLeft, int bottomLeft, int bottomRight, int topRight) {
        float tx = width <= 0 ? 0f : Math.clamp((x - left) / width, 0f, 1f);
        float ty = height <= 0 ? 0f : Math.clamp((y - top) / height, 0f, 1f);
        int topColor = ColorUtil.interpolate(topLeft, topRight, tx);
        int bottomColor = ColorUtil.interpolate(bottomLeft, bottomRight, tx);
        return ColorUtil.interpolate(topColor, bottomColor, ty);
    }

    private static ScreenRectangle bounds(Matrix3x2f pose, float x, float y, float width, float height,
                                           ScreenRectangle scissor) {
        ScreenRectangle rectangle = new ScreenRectangle((int) Math.floor(x) - 1, (int) Math.floor(y) - 1,
                (int) Math.ceil(width) + 2, (int) Math.ceil(height) + 2).transformMaxBounds(pose);
        return scissor == null ? rectangle : scissor.intersection(rectangle);
    }

    private record GradientTextureState(
            Matrix3x2fc pose, TextureSetup textureSetup,
            float x, float y, float width, float height,
            int topLeft, int bottomLeft, int bottomRight, int topRight,
            ScreenRectangle scissorArea, ScreenRectangle bounds
    ) implements GuiElementRenderState {
        private GradientTextureState(Matrix3x2f pose, TextureSetup textureSetup,
                                     float x, float y, float width, float height,
                                     int topLeft, int bottomLeft, int bottomRight, int topRight,
                                     ScreenRectangle scissorArea) {
            this(pose, textureSetup, x, y, width, height, topLeft, bottomLeft,
                    bottomRight, topRight, scissorArea,
                    ThemeRenderUtil.bounds(pose, x, y, width, height, scissorArea));
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            vertex(consumer, x, y, 0f, 0f, topLeft);
            vertex(consumer, x, y + height, 0f, 1f, bottomLeft);
            vertex(consumer, x + width, y + height, 1f, 1f, bottomRight);
            vertex(consumer, x + width, y, 1f, 0f, topRight);
        }

        private void vertex(VertexConsumer consumer, float vx, float vy, float u, float v, int color) {
            consumer.addVertexWith2DPose(pose, vx, vy).setUv(u, v).setColor(color);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }
    }

    private record GradientOutlineState(
            Matrix3x2fc pose, float x, float y, float width, float height,
            float radius, float thickness,
            int topLeft, int bottomLeft, int bottomRight, int topRight,
            ScreenRectangle scissorArea, ScreenRectangle bounds
    ) implements GuiElementRenderState {
        private GradientOutlineState(Matrix3x2f pose, float x, float y, float width, float height,
                                     float radius, float thickness,
                                     int topLeft, int bottomLeft, int bottomRight, int topRight,
                                     ScreenRectangle scissorArea) {
            this(pose, x, y, width, height, radius, thickness, topLeft, bottomLeft,
                    bottomRight, topRight, scissorArea,
                    ThemeRenderUtil.bounds(pose, x, y, width, height, scissorArea));
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float r = Math.clamp(radius, 0f, Math.min(width, height) / 2f);
            float t = Math.clamp(thickness, 0f, Math.min(width, height) / 2f);
            float inner = Math.max(0f, r - t);

            quad(consumer, x + r, y, x + r, y + t,
                    x + width - r, y + t, x + width - r, y);
            quad(consumer, x + r, y + height - t, x + r, y + height,
                    x + width - r, y + height, x + width - r, y + height - t);
            quad(consumer, x, y + r, x, y + height - r,
                    x + t, y + height - r, x + t, y + r);
            quad(consumer, x + width - t, y + r, x + width - t, y + height - r,
                    x + width, y + height - r, x + width, y + r);

            corner(consumer, x + r, y + r, r, inner, 180);
            corner(consumer, x + width - r, y + r, r, inner, 270);
            corner(consumer, x + width - r, y + height - r, r, inner, 0);
            corner(consumer, x + r, y + height - r, r, inner, 90);
        }

        private void corner(VertexConsumer consumer, float cx, float cy,
                            float outer, float inner, int startAngle) {
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a0 = Math.toRadians(startAngle + 90.0 * segment / SEGMENTS);
                double a1 = Math.toRadians(startAngle + 90.0 * (segment + 1) / SEGMENTS);
                quad(consumer,
                        cx + (float) Math.cos(a0) * outer,
                        cy + (float) Math.sin(a0) * outer,
                        cx + (float) Math.cos(a0) * inner,
                        cy + (float) Math.sin(a0) * inner,
                        cx + (float) Math.cos(a1) * inner,
                        cy + (float) Math.sin(a1) * inner,
                        cx + (float) Math.cos(a1) * outer,
                        cy + (float) Math.sin(a1) * outer);
            }
        }

        private void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1,
                          float x2, float y2, float x3, float y3) {
            vertex(consumer, x0, y0);
            vertex(consumer, x1, y1);
            vertex(consumer, x2, y2);
            vertex(consumer, x3, y3);
        }

        private void vertex(VertexConsumer consumer, float vx, float vy) {
            consumer.addVertexWith2DPose(pose, vx, vy)
                    .setColor(bilinear(vx, vy, x, y, width, height,
                            topLeft, bottomLeft, bottomRight, topRight));
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

    private record GradientRoundedState(
            Matrix3x2fc pose, float x, float y, float width, float height,
            float radius,
            int topLeft, int bottomLeft, int bottomRight, int topRight,
            ScreenRectangle scissorArea, ScreenRectangle bounds
    ) implements GuiElementRenderState {
        private GradientRoundedState(Matrix3x2f pose, float x, float y, float width, float height,
                                     float radius, int topLeft, int bottomLeft, int bottomRight, int topRight,
                                     ScreenRectangle scissorArea) {
            this(pose, x, y, width, height, radius, topLeft, bottomLeft, bottomRight, topRight,
                    scissorArea, ThemeRenderUtil.bounds(pose, x, y, width, height, scissorArea));
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            float r = Math.clamp(radius, 0f, Math.min(width, height) / 2f);
            quad(consumer, x + r, y, x + width - r, y + r,
                    x + width - r, y + height - r, x + r, y + height);
            quad(consumer, x, y + r, x + r, y + r,
                    x + r, y + height - r, x, y + height - r);
            quad(consumer, x + width - r, y + r, x + width, y + r,
                    x + width, y + height - r, x + width - r, y + height - r);
            corner(consumer, x + r, y + r, 180);
            corner(consumer, x + width - r, y + r, 270);
            corner(consumer, x + width - r, y + height - r, 0);
            corner(consumer, x + r, y + height - r, 90);
        }

        private void corner(VertexConsumer consumer, float cx, float cy, int startAngle) {
            for (int segment = 0; segment < SEGMENTS; segment++) {
                double a0 = Math.toRadians(startAngle + 90.0 * segment / SEGMENTS);
                double a1 = Math.toRadians(startAngle + 90.0 * (segment + 1) / SEGMENTS);
                double am = (a0 + a1) / 2.0;
                quad(consumer, cx, cy,
                        cx + (float) Math.cos(a1) * radius,
                        cy + (float) Math.sin(a1) * radius,
                        cx + (float) Math.cos(am) * radius,
                        cy + (float) Math.sin(am) * radius,
                        cx + (float) Math.cos(a0) * radius,
                        cy + (float) Math.sin(a0) * radius);
            }
        }

        private void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1,
                          float x2, float y2, float x3, float y3) {
            vertex(consumer, x0, y0);
            vertex(consumer, x1, y1);
            vertex(consumer, x2, y2);
            vertex(consumer, x3, y3);
        }

        private void vertex(VertexConsumer consumer, float vx, float vy) {
            consumer.addVertexWith2DPose(pose, vx, vy)
                    .setColor(bilinear(vx, vy, x, y, width, height,
                            topLeft, bottomLeft, bottomRight, topRight));
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
}
