package ru.rooyzee.elytrixclient.client.bots.own;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр блоков СЕРВЕРА из config-фазы (пакет registry data, кодек minecraft:block).
 * Зачем: бот говорит с сервером 26.x, а клиент 1.18.2 — свои state-id у каждой
 * версии, поэтому Block.stateById клиента врёт. Сервер сам присылает имена блоков
 * и количество состояний — отсюда строим карту id -> (твёрдый, класс цвета).
 */
public final class BlockRegistry {

    private static volatile boolean ready;
    private static boolean[] solid;
    private static byte[] cls;

    private BlockRegistry() {
    }

    public static boolean ready() {
        return ready;
    }

    public static boolean solid(int id) {
        boolean[] a = solid;
        return a != null && id >= 0 && id < a.length && a[id];
    }

    public static int colorClass(int id) {
        byte[] a = cls;
        if (a == null || id < 0 || id >= a.length) {
            return 0;
        }
        return a[id];
    }

    /** Пытается распознать пакет реестра блоков среди config-пакетов. */
    public static synchronized void sniff(byte[] d, int off) {
        if (ready || d == null || off < 0 || off >= d.length) {
            return;
        }
        int p = indexOf(d, off, "minecraft:block");
        if (p < 0 || p - off > 512) {
            return;
        }
        int[] h = {p + 15};
        try {
            Object root = read(d, h, 10);
            List<?> entries = null;
            if (root instanceof Map<?, ?> m) {
                Object v = m.get("value");
                if (v instanceof List<?> l) {
                    entries = l;
                }
            }
            if (entries == null && root instanceof List<?> l) {
                entries = l;
            }
            if (entries == null || entries.isEmpty()) {
                return;
            }
            List<String> names = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (Object e : entries) {
                if (!(e instanceof Map<?, ?> em)) {
                    continue;
                }
                Object nm = em.get("name");
                if (!(nm instanceof String name)) {
                    continue;
                }
                int product = 1;
                if (em.get("properties") instanceof List<?> props) {
                    for (Object pr : props) {
                        if (pr instanceof Map<?, ?> pm && pm.get("values") instanceof List<?> vals
                                && !vals.isEmpty()) {
                            product *= vals.size();
                        }
                    }
                }
                names.add(name);
                counts.add(Math.min(product, 4096));
            }
            if (names.isEmpty()) {
                return;
            }
            int total = 0;
            for (int c : counts) {
                total += c;
            }
            boolean[] ns = new boolean[total];
            byte[] nc = new byte[total];
            int id = 0;
            for (int i = 0; i < names.size(); i++) {
                boolean s = isSolidName(names.get(i));
                byte c = (byte) classOf(names.get(i));
                for (int k = 0; k < counts.get(i); k++) {
                    ns[id] = s;
                    nc[id] = c;
                    id++;
                }
            }
            solid = ns;
            cls = nc;
            ready = true;
        } catch (Exception ignored) {
            // не тот пакет — ждём следующий
        }
    }

    private static int indexOf(byte[] d, int off, String s) {
        byte[] n = s.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = off; i <= d.length - n.length; i++) {
            for (int j = 0; j < n.length; j++) {
                if (d[i + j] != n[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    // ── классы цвета для рендера ──
    static int classOf(String n) {
        if (n.contains("grass_block") || n.contains("dirt") || n.contains("podzol")
                || n.contains("mud") || n.contains("clay") || n.contains("farmland")
                || n.contains("mycelium") || n.contains("rooted")) {
            return 1;
        }
        if (n.contains("leaves") || n.contains("vine") || n.contains("moss")
                || n.contains("azalea") || n.contains("crop") || n.contains("wheat")) {
            return 2;
        }
        if (n.contains("log") || n.contains("wood") || n.contains("plank") || n.contains("stem")
                || n.contains("bookshelf") || n.contains("crafting_table")) {
            return 3;
        }
        if (n.contains("sand") || n.contains("gravel") || n.contains("terracotta")
                || n.contains("concrete") || n.contains("wool") || n.contains("sandstone")) {
            return 4;
        }
        if (n.contains("snow") || n.contains("ice") || n.contains("quartz")) {
            return 5;
        }
        if (n.contains("lava") || n.contains("magma")) {
            return 6;
        }
        if (n.contains("water") || n.contains("bubble") || n.contains("air")) {
            return 7;
        }
        return 0;
    }

    static boolean isSolidName(String n) {
        String id = n;
        int c = n.indexOf(':');
        if (c >= 0) {
            id = n.substring(c + 1);
        }
        if (id.equals("snow") || id.equals("barrier") || id.equals("structure_void")) {
            return false;
        }
        if (id.equals("grass_block") || id.equals("snow_block") || id.equals("moss_block")) {
            return true;
        }
        String[] no = {"air", "water", "bubble", "fire", "torch", "grass", "fern", "flower",
                "sapling", "vine", "wheat", "carrot", "potato", "beetroot", "cane", "kelp",
                "seagrass", "carpet", "rail", "lever", "button", "pressure", "redstone_wire",
                "tripwire", "cobweb", "crops", "bush", "poppy", "dandelion", "orchid", "tulip",
                "daisy", "allium", "lily", "cornflower"};
        for (String k : no) {
            if (id.contains(k)) {
                return false;
            }
        }
        return true;
    }

    // ── мини-NBT (сетевой формат, big-endian) ──
    private static Object read(byte[] d, int[] h, int t) {
        switch (t) {
            case 1:
                return d[h[0]++] != 0;
            case 2:
                return (int) (short) (((d[h[0]++] & 0xFF) << 8) | (d[h[0]++] & 0xFF));
            case 3: {
                int v = 0;
                for (int i = 0; i < 4; i++) {
                    v = (v << 8) | (d[h[0]++] & 0xFF);
                }
                return v;
            }
            case 4: {
                long v = 0;
                for (int i = 0; i < 8; i++) {
                    v = (v << 8) | (d[h[0]++] & 0xFF);
                }
                return v;
            }
            case 5: {
                int v = 0;
                for (int i = 0; i < 4; i++) {
                    v = (v << 8) | (d[h[0]++] & 0xFF);
                }
                return Float.intBitsToFloat(v);
            }
            case 6: {
                long v = 0;
                for (int i = 0; i < 8; i++) {
                    v = (v << 8) | (d[h[0]++] & 0xFF);
                }
                return Double.longBitsToDouble(v);
            }
            case 7: {
                int n = i32(d, h);
                byte[] r = new byte[n];
                System.arraycopy(d, h[0], r, 0, n);
                h[0] += n;
                return r;
            }
            case 8: {
                int n = ((d[h[0]++] & 0xFF) << 8) | (d[h[0]++] & 0xFF);
                String r = new String(d, h[0], n, StandardCharsets.UTF_8);
                h[0] += n;
                return r;
            }
            case 9: {
                int et = d[h[0]++] & 0xFF;
                int n = i32(d, h);
                List<Object> r = new ArrayList<>(Math.max(0, Math.min(n, 100000)));
                for (int i = 0; i < n; i++) {
                    r.add(read(d, h, et));
                }
                return r;
            }
            case 10: {
                Map<String, Object> r = new LinkedHashMap<>();
                while (true) {
                    int et = d[h[0]++] & 0xFF;
                    if (et == 0) {
                        return r;
                    }
                    int nl = ((d[h[0]++] & 0xFF) << 8) | (d[h[0]++] & 0xFF);
                    String name = new String(d, h[0], nl, StandardCharsets.UTF_8);
                    h[0] += nl;
                    r.put(name, read(d, h, et));
                }
            }
            case 11: {
                int n = i32(d, h);
                h[0] += 4L * n;
                return n;
            }
            case 12: {
                int n = i32(d, h);
                h[0] += 8L * n;
                return n;
            }
            default:
                throw new IllegalArgumentException("bad nbt type " + t);
        }
    }

    private static int i32(byte[] d, int[] h) {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            v = (v << 8) | (d[h[0]++] & 0xFF);
        }
        return v;
    }
}
