package wtf.expensive.client.util.render;

import java.util.ArrayList;
import java.util.List;

public final class OutlineUtils {
    private static final List<Runnable> renderCalls = new ArrayList<>();

    private OutlineUtils() {
    }

    public static void registerRenderCall(Runnable runnable) {
        if (runnable != null) {
            renderCalls.add(runnable);
        }
    }

    public static void draw(float radius, int color) {
        for (Runnable runnable : new ArrayList<>(renderCalls)) {
            runnable.run();
        }
        renderCalls.clear();
    }

    public static void clear() {
        renderCalls.clear();
    }
}
