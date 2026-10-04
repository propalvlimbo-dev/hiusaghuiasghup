package wtf.expensive.client.util.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class AnimationMath {
    private AnimationMath() {
    }

    public static double deltaTime() {
        int fps = Minecraft.getInstance().getFps();
        return fps > 0 ? 1.0 / fps : 1.0;
    }

    public static float fast(float current, float target, float multiple) {
        float factor = Mth.clamp((float) (deltaTime() * multiple), 0f, 1f);
        return current * (1 - factor) + target * factor;
    }

    public static float lerp(float current, float target, float multiple) {
        return (float) (current + (target - current) * Mth.clamp(deltaTime() * multiple, 0, 1));
    }

    public static double lerp(double current, double target, double multiple) {
        return current + (target - current) * Mth.clamp(deltaTime() * multiple, 0, 1);
    }
}
