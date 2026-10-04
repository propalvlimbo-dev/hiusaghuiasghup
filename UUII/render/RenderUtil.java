package wtf.expensive.client.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.mixin.GuiGraphicsExtractorAccessor;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RenderUtil {
    private static final int MAX_DEFERRED_BLOCK_BOXES = 4096;
    private static final ConcurrentLinkedQueue<BlockBoxRequest> DEFERRED_BLOCK_BOXES = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger DEFERRED_BLOCK_BOX_COUNT = new AtomicInteger();

    public record BlockBoxRequest(BlockPos position, int color) {
    }

    private RenderUtil() {
    }

    public static void drawBlockBox(BlockPos pos, int color) {
        if (pos == null) {
            return;
        }
        while (DEFERRED_BLOCK_BOX_COUNT.get() >= MAX_DEFERRED_BLOCK_BOXES) {
            if (DEFERRED_BLOCK_BOXES.poll() == null) {
                DEFERRED_BLOCK_BOX_COUNT.set(0);
                break;
            }
            DEFERRED_BLOCK_BOX_COUNT.decrementAndGet();
        }
        DEFERRED_BLOCK_BOXES.offer(new BlockBoxRequest(pos.immutable(), color));
        DEFERRED_BLOCK_BOX_COUNT.incrementAndGet();
    }

    public static void drainBlockBoxes(Consumer<BlockBoxRequest> consumer) {
        BlockBoxRequest request;
        while ((request = DEFERRED_BLOCK_BOXES.poll()) != null) {
            DEFERRED_BLOCK_BOX_COUNT.decrementAndGet();
            consumer.accept(request);
        }
    }

    public static void roundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                   float radius, int color) {
        roundedRect(graphics, x, y, width, height, radius, radius, radius, radius, color, color);
    }

    public static void roundedRectGradient(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                           float radius, int colorTop, int colorBottom) {
        roundedRect(graphics, x, y, width, height, radius, radius, radius, radius, colorTop, colorBottom);
    }

    public static void roundedRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                   float rTopLeft, float rTopRight, float rBottomRight, float rBottomLeft,
                                   int colorTop, int colorBottom) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = currentScissor(graphics);
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();

        float maxRadius = Math.min(width, height) / 2f;
        float tl = Math.clamp(rTopLeft, 0, maxRadius);
        float tr = Math.clamp(rTopRight, 0, maxRadius);
        float br = Math.clamp(rBottomRight, 0, maxRadius);
        float bl = Math.clamp(rBottomLeft, 0, maxRadius);

        float topInset = Math.max(tl, tr);
        float bottomInset = Math.max(bl, br);

        float middleHeight = height - topInset - bottomInset;
        if (middleHeight > 0) {
            rect(state, pose, x, y + topInset, width, middleHeight, colorTop, colorBottom, y, height, scissor);
        }

        if (topInset > 0) {
            rect(state, pose, x + tl, y, width - tl - tr, topInset, colorTop, colorBottom, y, height, scissor);
            if (tl < topInset) {
                rect(state, pose, x, y + tl, tl, topInset - tl, colorTop, colorBottom, y, height, scissor);
            }
            if (tr < topInset) {
                rect(state, pose, x + width - tr, y + tr, tr, topInset - tr, colorTop, colorBottom, y, height, scissor);
            }
        }

        if (bottomInset > 0) {
            float bandY = y + height - bottomInset;
            rect(state, pose, x + bl, bandY, width - bl - br, bottomInset, colorTop, colorBottom, y, height, scissor);
            if (bl < bottomInset) {
                rect(state, pose, x, bandY, bl, bottomInset - bl, colorTop, colorBottom, y, height, scissor);
            }
            if (br < bottomInset) {
                rect(state, pose, x + width - br, bandY, br, bottomInset - br, colorTop, colorBottom, y, height, scissor);
            }
        }

        corner(state, pose, x, y, tl, CornerRenderState.Corner.TOP_LEFT, colorTop, colorBottom, y, height, scissor);
        corner(state, pose, x + width - tr, y, tr, CornerRenderState.Corner.TOP_RIGHT, colorTop, colorBottom, y, height, scissor);
        corner(state, pose, x + width - br, y + height - br, br, CornerRenderState.Corner.BOTTOM_RIGHT,
                colorTop, colorBottom, y, height, scissor);
        corner(state, pose, x, y + height - bl, bl, CornerRenderState.Corner.BOTTOM_LEFT,
                colorTop, colorBottom, y, height, scissor);
    }

    private static void rect(GuiRenderState state, Matrix3x2f pose, float x, float y, float width, float height,
                             int colorTop, int colorBottom, float gradientY, float gradientHeight,
                             ScreenRectangle scissor) {
        if (width <= 0 || height <= 0) {
            return;
        }
        state.addGuiElement(new RectRenderState(pose, x, y, width, height,
                colorTop, colorBottom, gradientY, gradientHeight, scissor));
    }

    private static void corner(GuiRenderState state, Matrix3x2f pose, float x, float y, float size,
                               CornerRenderState.Corner corner, int colorTop, int colorBottom,
                               float gradientY, float gradientHeight, ScreenRectangle scissor) {
        if (size <= 0) {
            return;
        }
        state.addGuiElement(new CornerRenderState(pose, x, y, size, corner,
                colorTop, colorBottom, gradientY, gradientHeight, scissor));
    }

    public static void shadow(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                              float radius, int color) {
        if (width <= 0 || height <= 0 || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        if (Managment.FUNCTION_MANAGER != null && Managment.FUNCTION_MANAGER.optimization.isState()
                && Managment.FUNCTION_MANAGER.optimization.options.get(2)) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new SoftShadowRenderState(pose, x, y, width, height,
                radius, 8f, color, currentScissor(graphics)));
    }

    public static void roundedOutline(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                      float radius, float thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new RoundedOutlineRenderState(pose, x, y, width, height,
                radius, thickness, color, currentScissor(graphics)));
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    public static void diamondOutline(GuiGraphicsExtractor graphics, float centerX, float centerY,
                                      float radius, float thickness, int color) {
        if (radius <= 0 || thickness <= 0 || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new DiamondOutlineRenderState(pose, centerX, centerY, radius, thickness,
                color, currentScissor(graphics)));
    }

    public static void colorQuads(GuiGraphicsExtractor graphics, float[] positions, int[] colors) {
        if (positions.length < 8 || colors.length < 4) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new ColorQuadsRenderState(pose, positions, colors, currentScissor(graphics)));
    }

    public static void line(GuiGraphicsExtractor graphics, float x1, float y1, float x2, float y2,
                            float width, int color) {
        line(graphics, x1, y1, x2, y2, width, color, color);
    }

    public static void line(GuiGraphicsExtractor graphics, float x1, float y1, float x2, float y2,
                            float width, int startColor, int endColor) {
        if (width <= 0 || (((startColor | endColor) >>> 24) & 0xFF) == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new LineRenderState(pose, x1, y1, x2, y2, width,
                startColor, endColor, currentScissor(graphics)));
    }

    public static void triangle(GuiGraphicsExtractor graphics, float x1, float y1, float x2, float y2,
                                float x3, float y3, int color) {
        if (((color >>> 24) & 0xFF) == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new TriangleRenderState(pose, x1, y1, x2, y2, x3, y3,
                color, currentScissor(graphics)));
    }

    public static void texturedQuad(GuiGraphicsExtractor graphics, Identifier id, float[] corners, int color) {
        if (corners.length < 8 || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new TexturedQuadRenderState(pose,
                TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()),
                corners, color, currentScissor(graphics)));
    }

    public static void polyline(GuiGraphicsExtractor graphics, float[] points, float width,
                                int color, boolean closed) {
        int count = points.length / 2;
        if (count < 2 || width <= 0 || ((color >>> 24) & 0xFF) == 0) {
            return;
        }
        float half = width * 0.5f;
        int segments = closed ? count : count - 1;
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % count;
            float x1 = points[i * 2];
            float y1 = points[i * 2 + 1];
            float x2 = points[next * 2];
            float y2 = points[next * 2 + 1];
            float dx = x2 - x1;
            float dy = y2 - y1;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.0001f) {
                continue;
            }
            float ex = dx / length * half;
            float ey = dy / length * half;
            line(graphics, x1 - ex, y1 - ey, x2 + ex, y2 + ey, width, color);
        }
    }

    public static void ring(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius,
                            float width, float startDegrees, float endDegrees, int color) {
        float sweep = Math.abs(endDegrees - startDegrees);
        if (sweep < 0.01f) {
            return;
        }
        int segments = Math.max(12, (int) (sweep / 6f));
        boolean closed = sweep >= 359.9f;
        int count = closed ? segments : segments + 1;
        float[] points = new float[count * 2];
        for (int i = 0; i < count; i++) {
            float angle = startDegrees + (endDegrees - startDegrees) * i / segments;
            points[i * 2] = centerX + (float) Math.cos(Math.toRadians(angle)) * radius;
            points[i * 2 + 1] = centerY + (float) Math.sin(Math.toRadians(angle)) * radius;
        }
        polyline(graphics, points, width, color, closed);
    }

    public static void circle(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        roundedRect(graphics, centerX - radius, centerY - radius, radius * 2, radius * 2, radius, color);
    }

    public static void gradientRect(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                                    int topLeft, int bottomLeft, int bottomRight, int topRight) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        GuiRenderState state = ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState();
        state.addGuiElement(new GradientRectRenderState(pose, x, y, width, height,
                topLeft, bottomLeft, bottomRight, topRight, currentScissor(graphics)));
    }

    public static void texture(GuiGraphicsExtractor graphics, Identifier texture,
                               float x, float y, float width, float height, int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                Math.round(x), Math.round(y), 0f, 0f,
                Math.round(width), Math.round(height), Math.round(width), Math.round(height), color);
    }

    public static void textureRegion(GuiGraphicsExtractor graphics, Identifier texture,
                                     float x, float y, float u, float v,
                                     float width, float height, float sourceWidth, float sourceHeight,
                                     float textureWidth, float textureHeight,
                                     int color) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                Math.round(x), Math.round(y), u, v,
                Math.round(width), Math.round(height),
                Math.round(sourceWidth), Math.round(sourceHeight),
                Math.round(textureWidth), Math.round(textureHeight), color);
    }

    public static void scissor(GuiGraphicsExtractor graphics, float x, float y, float width, float height) {
        graphics.enableScissor((int) x, (int) y, (int) (x + width), (int) (y + height));
        ScreenRectangle rect = new ScreenRectangle((int) x, (int) y, (int) width, (int) height);
        ScreenRectangle current = scissorStack.peek();
        scissorStack.push(current == null ? rect : current.intersection(rect));
    }

    public static void unscissor(GuiGraphicsExtractor graphics) {
        graphics.disableScissor();
        if (!scissorStack.isEmpty()) {
            scissorStack.pop();
        }
    }

    public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static final java.util.ArrayDeque<ScreenRectangle> scissorStack = new java.util.ArrayDeque<>();

    public static void resetScissorStack() {
        scissorStack.clear();
    }

    private static ScreenRectangle currentScissor(GuiGraphicsExtractor graphics) {
        ScreenRectangle current = scissorStack.peek();
        return current != null ? current
                : new ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight());
    }
}
