package ru.rooyzee.elytrixclient.client.features.render.modules;

import net.minecraft.client.Minecraft;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * FullBright (порт delta-26.2): выкручивает гамму на максимум, пока включён,
 * и возвращает исходное значение при выключении. Доступ к опции гаммы — через
 * рефлексию, чтобы не зависеть от точных маппингов GameOptions.
 */
public final class FullBright extends VisualModule {
    private static final double BRIGHT = 16.0;
    private Double original;

    public FullBright() {
        super("FullBright", false);
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc == null || mc.options == null) {
            return;
        }
        try {
            Object gamma = gammaOption(mc.options);
            if (gamma == null) {
                return;
            }
            Method get = gamma.getClass().getMethod("getValue");
            Method set = gamma.getClass().getMethod("setValue", Object.class);
            double cur = ((Number) get.invoke(gamma)).doubleValue();
            if (original == null) {
                original = cur;
            }
            if (cur < BRIGHT) {
                set.invoke(gamma, BRIGHT);
            }
        } catch (Throwable ignored) {
            // опция не найдена / недоступна — молча пропускаем
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || original == null) {
            return;
        }
        try {
            Object gamma = gammaOption(mc.options);
            if (gamma == null) {
                return;
            }
            gamma.getClass().getMethod("setValue", Object.class).invoke(gamma, original);
            original = null;
        } catch (Throwable ignored) {
        }
    }

    private static Object gammaOption(Object options) {
        for (Field f : options.getClass().getFields()) {
            if (f.getName().equals("gamma")) {
                try {
                    return f.get(options);
                } catch (Throwable ignored) {
                }
            }
        }
        for (Method m : options.getClass().getMethods()) {
            if (m.getName().equals("gamma") && m.getParameterCount() == 0) {
                try {
                    return m.invoke(options);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }
}
