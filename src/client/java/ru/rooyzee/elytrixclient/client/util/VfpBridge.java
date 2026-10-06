package ru.rooyzee.elytrixclient.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Мост к ViaFabricPlus через рефлексию — без compile-зависимости.
 * Если VFP не установлен, все методы безопасны и возвращают null/false.
 * API (5.x): ViaFabricPlus.api().setTargetVersion(ProtocolVersion) / targetVersion().
 */
public final class VfpBridge {

    private static Boolean present;

    private VfpBridge() {
    }

    public static boolean installed() {
        if (present == null) {
            try {
                Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
                present = true;
            } catch (Throwable t) {
                present = false;
            }
        }
        return present;
    }

    /** Короткий статус для UI. */
    public static String status() {
        if (!installed()) {
            return "VFP не установлен — версии не меняются";
        }
        return "VFP: текущая " + current();
    }

    public static String current() {
        try {
            return name(api().getClass().getMethod("targetVersion").invoke(api()));
        } catch (Throwable t) {
            return "—";
        }
    }

    /** Имена всех версий, которые знает VFP. */
    public static List<String> versions() {
        List<String> out = new ArrayList<>();
        for (Object pv : versionObjects()) {
            out.add(name(pv));
        }
        return out;
    }

    /** Применить целевую версию по имени («1.16.5», «26.2»…). true если нашли и поставили. */
    public static boolean setVersion(String wanted) {
        if (!installed() || wanted == null || wanted.isBlank()) {
            return false;
        }
        try {
            Object target = null;
            String w = wanted.trim();
            for (Object pv : versionObjects()) {
                String n = name(pv);
                if (w.equalsIgnoreCase(n)) {
                    target = pv;
                    break;
                }
            }
            if (target == null) {
                // допускаем «1.16» как префикс
                for (Object pv : versionObjects()) {
                    if (name(pv).startsWith(w)) {
                        target = pv;
                    }
                }
            }
            if (target == null) {
                return false;
            }
            api().getClass().getMethod("setTargetVersion",
                    Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion"))
                    .invoke(api(), target);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object api() throws Exception {
        return Class.forName("com.viaversion.viafabricplus.ViaFabricPlus")
                .getMethod("api").invoke(null);
    }

    private static List<Object> versionObjects() {
        List<Object> out = new ArrayList<>();
        try {
            Class<?> pv = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            Object coll = null;
            for (String m : new String[]{"getRegisteredVersions", "getVersions", "getProtocols"}) {
                try {
                    coll = pv.getMethod(m).invoke(null);
                    break;
                } catch (Throwable ignored) {
                }
            }
            if (coll instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o != null) {
                        out.add(o);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String name(Object pv) {
        try {
            return String.valueOf(pv.getClass().getMethod("getName").invoke(pv));
        } catch (Throwable t) {
            return String.valueOf(pv);
        }
    }
}
