package wtf.expensive.client.util.render;

import java.util.ArrayList;
import java.util.List;

public final class BloomHelper {
    private static final List<Runnable> renderCalls = new ArrayList<>();
    private static final List<Runnable> handRenderCalls = new ArrayList<>();

    private BloomHelper() {
    }

    public static void registerRenderCall(Runnable runnable) {
        if (runnable != null) {
            renderCalls.add(runnable);
        }
    }

    public static void registerRenderCallHand(Runnable runnable) {
        if (runnable != null) {
            handRenderCalls.add(runnable);
        }
    }

    public static void draw() {
        for (Runnable runnable : new ArrayList<>(renderCalls)) {
            runnable.run();
        }
        renderCalls.clear();
    }

    public static void drawHand() {
        for (Runnable runnable : new ArrayList<>(handRenderCalls)) {
            runnable.run();
        }
        handRenderCalls.clear();
    }

    public static void clear() {
        renderCalls.clear();
        handRenderCalls.clear();
    }
}
