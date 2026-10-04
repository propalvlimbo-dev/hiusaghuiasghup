package wtf.expensive.client.ui.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import wtf.expensive.client.util.render.ColorUtil;

public record HudRoundedGradientRenderState(
        Matrix3x2fc pose,
        float x,
        float y,
        float width,
        float height,
        float topLeftRadius,
        float topRightRadius,
        float bottomRightRadius,
        float bottomLeftRadius,
        int topLeft,
        int topRight,
        int bottomRight,
        int bottomLeft,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final int SEGMENTS = 12;

    public HudRoundedGradientRenderState(Matrix3x2fc pose, float x, float y, float width, float height,
                                         float topLeftRadius, float topRightRadius,
                                         float bottomRightRadius, float bottomLeftRadius,
                                         int topLeft, int topRight, int bottomRight, int bottomLeft,
                                         ScreenRectangle scissorArea) {
        this(pose, x, y, width, height,
                topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius,
                topLeft, topRight, bottomRight, bottomLeft, scissorArea,
                bounds(pose, x, y, width, height, scissorArea));
    }

    private static ScreenRectangle bounds(Matrix3x2fc pose, float x, float y, float width, float height,
                                           ScreenRectangle scissorArea) {
        ScreenRectangle rectangle = new ScreenRectangle(
                (int) Math.floor(x) - 1,
                (int) Math.floor(y) - 1,
                (int) Math.ceil(width) + 2,
                (int) Math.ceil(height) + 2
        ).transformMaxBounds(pose);
        return scissorArea == null ? rectangle : scissorArea.intersection(rectangle);
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float maxRadius = Math.min(width, height) / 2f;
        float tl = Math.clamp(topLeftRadius, 0, maxRadius);
        float tr = Math.clamp(topRightRadius, 0, maxRadius);
        float br = Math.clamp(bottomRightRadius, 0, maxRadius);
        float bl = Math.clamp(bottomLeftRadius, 0, maxRadius);

        float topInset = Math.max(tl, tr);
        float bottomInset = Math.max(bl, br);
        float middleHeight = height - topInset - bottomInset;
        if (middleHeight > 0) {
            quad(consumer, x, y + topInset, width, middleHeight);
        }

        if (topInset > 0) {
            quad(consumer, x + tl, y, width - tl - tr, topInset);
            if (tl < topInset) {
                quad(consumer, x, y + tl, tl, topInset - tl);
            }
            if (tr < topInset) {
                quad(consumer, x + width - tr, y + tr, tr, topInset - tr);
            }
        }

        if (bottomInset > 0) {
            float bandY = y + height - bottomInset;
            quad(consumer, x + bl, bandY, width - bl - br, bottomInset);
            if (bl < bottomInset) {
                quad(consumer, x, bandY, bl, bottomInset - bl);
            }
            if (br < bottomInset) {
                quad(consumer, x + width - br, bandY, br, bottomInset - br);
            }
        }

        corner(consumer, x, y, tl, Corner.TOP_LEFT);
        corner(consumer, x + width - tr, y, tr, Corner.TOP_RIGHT);
        corner(consumer, x + width - br, y + height - br, br, Corner.BOTTOM_RIGHT);
        corner(consumer, x, y + height - bl, bl, Corner.BOTTOM_LEFT);
    }

    private enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

    private void quad(VertexConsumer consumer, float qx, float qy, float qw, float qh) {
        if (qw <= 0 || qh <= 0) {
            return;
        }
        vertex(consumer, qx, qy);
        vertex(consumer, qx, qy + qh);
        vertex(consumer, qx + qw, qy + qh);
        vertex(consumer, qx + qw, qy);
    }

    private void corner(VertexConsumer consumer, float qx, float qy, float radius, Corner corner) {
        if (radius <= 0) {
            return;
        }
        float centerX = switch (corner) {
            case TOP_LEFT, BOTTOM_LEFT -> qx + radius;
            case TOP_RIGHT, BOTTOM_RIGHT -> qx;
        };
        float centerY = switch (corner) {
            case TOP_LEFT, TOP_RIGHT -> qy + radius;
            case BOTTOM_RIGHT, BOTTOM_LEFT -> qy;
        };
        int startAngle = switch (corner) {
            case TOP_LEFT -> 180;
            case TOP_RIGHT -> 270;
            case BOTTOM_RIGHT -> 0;
            case BOTTOM_LEFT -> 90;
        };

        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a0 = Math.toRadians(startAngle + 90d * segment / SEGMENTS);
            double a1 = Math.toRadians(startAngle + 90d * (segment + 1) / SEGMENTS);
            double am = (a0 + a1) * 0.5;
            vertex(consumer, centerX, centerY);
            vertex(consumer, centerX + (float) Math.cos(a1) * radius,
                    centerY + (float) Math.sin(a1) * radius);
            vertex(consumer, centerX + (float) Math.cos(am) * radius,
                    centerY + (float) Math.sin(am) * radius);
            vertex(consumer, centerX + (float) Math.cos(a0) * radius,
                    centerY + (float) Math.sin(a0) * radius);
        }
    }

    private void vertex(VertexConsumer consumer, float vx, float vy) {
        consumer.addVertexWith2DPose(pose, vx, vy).setColor(colorAt(vx, vy));
    }

    private int colorAt(float vx, float vy) {
        float horizontal = width <= 0 ? 0f : Math.clamp((vx - x) / width, 0f, 1f);
        float vertical = height <= 0 ? 0f : Math.clamp((vy - y) / height, 0f, 1f);
        int top = ColorUtil.interpolate(topLeft, topRight, horizontal);
        int bottom = ColorUtil.interpolate(bottomLeft, bottomRight, horizontal);
        return ColorUtil.interpolate(top, bottom, vertical);
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
