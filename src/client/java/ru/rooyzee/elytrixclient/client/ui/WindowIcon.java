package ru.rooyzee.elytrixclient.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import ru.rooyzee.elytrixclient.Elytrixclient;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Подменяет иконку окна Minecraft (и значок в панели задач) на иконку ElytrixClient.
 *
 * <p>Почему «периодически»: ваниль ставит свою иконку при создании окна
 * ({@code Window.setIcon(vanillaPackResources, IconSet.RELEASE)}), а моды грузятся
 * позже — поэтому одного раза мало. Мы повторяем установку, пока окно живо
 * (раз в несколько секунд), и делаем это ровно так же, как сама игра:
 * PNG → {@link NativeImage} → {@code getPixelsABGR()} → {@code glfwSetWindowIcon}.
 *
 * <p>Картинки лежат в нашем jar: {@code assets/elytrixclient/window_icon_*.png}
 * (генерируются скриптом {@code scripts/make-textures.py} из {@code art/icon-master.png}).
 */
public final class WindowIcon {
    private static final String[] SIZES = {"16", "32", "48", "64", "128", "256"};
    /** Как часто переставлять иконку (мс): перекрывает и ваниль, и смену окна. */
    private static final long RETRY_MS = 4000L;

    private static int[][] pixels;
    private static int[] dims;
    private static boolean loaded;
    private static boolean warned;
    private static boolean loggedOnce;
    private static long lastHandle = -1L;
    private static long lastApplied;

    private WindowIcon() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().handle();
        if (handle == 0L) {
            return;
        }
        long now = Util.getMillis();
        if (handle == lastHandle && now - lastApplied < RETRY_MS) {
            return;
        }
        boolean ok = apply(handle);
        lastApplied = now;
        if (ok) {
            lastHandle = handle;
        }
    }

    private static boolean apply(long handle) {
        if (!load()) {
            return false;
        }
        int count = dims.length;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(count, stack);
            List<ByteBuffer> allocated = new ArrayList<>(count);
            try {
                for (int i = 0; i < count; i++) {
                    ByteBuffer buffer = MemoryUtil.memAlloc(pixels[i].length * 4);
                    allocated.add(buffer);
                    buffer.asIntBuffer().put(pixels[i]);
                    icons.position(i);
                    icons.width(dims[i]);
                    icons.height(dims[i]);
                    icons.pixels(buffer);
                }
                icons.position(0);
                GLFW.glfwSetWindowIcon(handle, icons);
                if (!loggedOnce) {
                    loggedOnce = true;
                    Elytrixclient.LOG.info("[Elytrix] Иконка окна заменена ({} размеров)", count);
                }
                return true;
            } finally {
                for (ByteBuffer buffer : allocated) {
                    MemoryUtil.memFree(buffer);
                }
            }
        } catch (Throwable t) {
            warnOnce("Не удалось поставить иконку окна: " + t);
            return false;
        }
    }

    /** Однократное чтение и разбор PNG (дальше только дешёвая установка). */
    private static boolean load() {
        if (loaded) {
            return dims.length > 0;
        }
        loaded = true;
        List<int[]> images = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        for (String size : SIZES) {
            String path = "/assets/elytrixclient/window_icon_" + size + ".png";
            try (InputStream in = WindowIcon.class.getResourceAsStream(path)) {
                if (in == null) {
                    continue;
                }
                try (NativeImage image = NativeImage.read(in)) {
                    IntBuffer source = image.getPixelsABGR();
                    int[] copy = new int[source.remaining()];
                    source.get(copy);
                    images.add(copy);
                    sizes.add(image.getWidth());
                }
            } catch (Throwable t) {
                warnOnce("Не удалось прочитать " + path + ": " + t);
            }
        }
        if (images.isEmpty()) {
            warnOnce("window_icon_*.png не найдены в jar — иконка останется ванильной");
        }
        pixels = images.toArray(new int[0][]);
        dims = new int[sizes.size()];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = sizes.get(i);
        }
        return dims.length > 0;
    }

    private static void warnOnce(String message) {
        if (!warned) {
            warned = true;
            Elytrixclient.LOG.warn("[Elytrix] {}", message);
        }
    }
}
