package ru.rooyzee.elytrixclient.client.ui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import ru.rooyzee.elytrixclient.Elytrixclient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Подменяет иконку окна Minecraft (и значок в панели задач) на иконку ElytrixClient,
 * заодно меняет заголовок окна.
 *
 * Текстуры лежат в нашем jar: assets/elytrixclient/window_icon_*.png (генерируются
 * скриптом scripts/make-textures.py из art/icon-master.png).
 * Ваниль ставит свою иконку при создании окна, поэтому нашу ставим один раз
 * на первом клиентском тике — после неё.
 */
public final class WindowIcon {
    private static final String[] SIZES = {"16", "32", "64", "128", "256"};
    private static boolean applied;

    private WindowIcon() {
    }

    public static void tick(Minecraft client) {
        if (applied) {
            return;
        }
        applied = true;
        try {
            apply(client.getWindow().handle());
        } catch (Throwable t) {
            Elytrixclient.LOG.warn("[Elytrix] Иконку окна поставить не удалось: {}", t.toString());
        }
    }

    private static void apply(long windowHandle) throws Exception {
        List<BufferedImage> images = new ArrayList<>();
        for (String size : SIZES) {
            BufferedImage image = read("/assets/elytrixclient/window_icon_" + size + ".png");
            if (image != null) {
                images.add(image);
            }
        }
        if (images.isEmpty()) {
            Elytrixclient.LOG.warn("[Elytrix] window_icon_*.png не найдены в jar");
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer buffer = GLFWImage.malloc(images.size(), stack);
            for (int i = 0; i < images.size(); i++) {
                BufferedImage image = images.get(i);
                int width = image.getWidth();
                int height = image.getHeight();
                ByteBuffer pixels = stack.malloc(width * height * 4);
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int argb = image.getRGB(x, y);
                        pixels.put((byte) (argb >> 16))
                                .put((byte) (argb >> 8))
                                .put((byte) argb)
                                .put((byte) (argb >>> 24));
                    }
                }
                pixels.flip();
                buffer.get(i).width(width).height(height).pixels(pixels);
            }
            GLFW.glfwSetWindowIcon(windowHandle, buffer);
            GLFW.glfwSetWindowTitle(windowHandle, "ElytrixClient — Minecraft 26.2");
            Elytrixclient.LOG.info("[Elytrix] Иконка окна заменена ({} размеров)", images.size());
        }
    }

    private static BufferedImage read(String resource) {
        try (InputStream in = WindowIcon.class.getResourceAsStream(resource)) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }
}
