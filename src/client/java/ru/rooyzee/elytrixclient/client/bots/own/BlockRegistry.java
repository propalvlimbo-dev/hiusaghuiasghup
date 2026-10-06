package ru.rooyzee.elytrixclient.client.bots.own;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Реестр блоков из config-фазы (сервер сам шлёт NBT-кодек «minecraft:block»).
 * Даёт для глобального state-id: твёрдость и класс цвета — как настоящий клиент
 * знает, что воздух/трава/факел — не стены, а камень/дерево — стены.
 */
public final class BlockRegistry {

    /** 0 камень, 1 трава-земля, 2 листва, 3 дерево, 4 песок, 5 снег, 6 вода/лава, 7 воздух. */
    private static volatile byte[] stateClass = null;
    private static volatile boolean[] stateSolid = null;
    private static volatile int stateCount;

    private BlockRegistry() {
    }

    public static boolean ready() {
        return stateSolid != null;
    }

    public static boolean solid(int stateId) {
        boolean[] s = stateSolid;
        return s != null && stateId >= 0 && stateId < s.length && s[stateId];
    }

    public static int colorClass(int stateId) {
        byte[] c = stateClass;
        if (c == null || stateId < 0 || stateId >= c.length) {
            return 0;
        }
        return c[stateId];
    }

    /** true, если пакет — реестр блоков; тогда распарсен. Данные: строка-id + NBT. */
    public static boolean tryParse(byte[] data, int off) {
        try {
            int[] h = {off};
            String reg = readString(data, h);
            if (!"minecraft:block".equals(reg)) {
                return false;
            }
            parseNbt(data, h);
            return stateCount > 0;
        } catch (Exception e) {
            return stateSolid != null;
        }
    }

    // ── разбор NBT реестра: entries[] -> name + properties -> диапазон state-id ──

    private static int nextStateId;
    private static java.util.List<Boolean> solidList;
    private static java.util.List<Byte> classList;

    private static void parseNbt(byte[] d, int[] h) {
        nextStateId = 0;
        solidList = new java.util.ArrayList<>();
        classList = new java.util.ArrayList<>();
        if ((d[h[0]++] & 0xFF) != 10) {
            return;
        }
        walkCompound(d, h, 0, null);
        boolean[] sol = new boolean[nextStateId];
        byte[] cls = new byte[nextStateId];
        for (int i = 0; i < nextStateId && i < solidList.size(); i++) {
            sol[i] = solidList.get(i);
            cls[i] = classList.get(i);
        }
        stateSolid = sol;
        stateClass = cls;
        stateCount = nextStateId;
    }

    /** Ищет список "entries" и для каждого блока считает число состояний. */
    private static void walkCompound(byte[] d, int[] h, int depth, String name) {
        while (true) {
            int type = d[h[0]++] & 0xFF;
            if (type == 0) {
                return;
            }
            String key = readName(d, h);
            if (type == 9 && "entries".equals(key) && depth <= 3) {
                int et = d[h[0]++] & 0xFF;
                int n = readI32(d, h);
                if (et == 10) {
                    for (int i = 0; i < n; i++) {
                        readEntry(d, h);
                    }
                } else {
                    for (int i = 0; i < n; i++) {
                        skipPayload(d, h, et);
                    }
                }
            } else {
                skipPayload(d, h, type);
            }
        }
    }

    private static void readEntry(byte[] d, int[] h) {
        String blockName = null;
        int states = 1;
        int[] save = {h[0]};
        // первый проход: имя + произведение вариантов properties
        Map<String, Integer> props = new HashMap<>();
        while (true) {
            int type = d[h[0]++] & 0xFF;
            if (type == 0) {
                break;
            }
            String key = readName(d, h);
            if (type == 8 && "name".equals(key)) {
                blockName = readString(d, h);
            } else if (type == 9 && "properties".equals(key)) {
                int et = d[h[0]++] & 0xFF;
                int n = readI32(d, h);
                for (int i = 0; i < n; i++) {
                    if (et == 10) {
                        while (true) {
                            int t2 = d[h[0]++] & 0xFF;
                            if (t2 == 0) {
                                break;
                            }
                            String pk = readName(d, h);
                            if (t2 == 9) {
                                int vt = d[h[0]++] & 0xFF;
                                int vn = readI32(d, h);
                                props.put(pk, vn);
                                for (int v = 0; v < vn; v++) {
                                    skipPayload(d, h, vt);
                                }
                            } else {
                                skipPayload(d, h, t2);
                            }
                        }
                    } else {
                        skipPayload(d, h, et);
                    }
                }
            } else {
                skipPayload(d, h, type);
            }
        }
        for (int v : props.values()) {
            states *= Math.max(1, v);
        }
        byte cls = classOf(blockName);
        boolean sol = isSolid(blockName);
        for (int i = 0; i < states; i++) {
            solidList.add(sol);
            classList.add(cls);
        }
        nextStateId += states;
    }

    private static boolean isSolid(String n) {
        if (n == null) {
            return true;
        }
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

    private static byte classOf(String n) {
        if (n == null) {
            return 0;
        }
        if (n.contains("air") || n.contains("water")) {
            return 7;
        }
        if (n.contains("grass_block") || n.contains("dirt") || n.contains("podzol")
                || n.contains("mud") || n.contains("farmland") || n.contains("clay")) {
            return 1;
        }
        if (n.contains("leaf") || n.contains("leaves") || n.contains("vine") || n.contains("moss")) {
            return 2;
        }
        if (n.contains("log") || n.contains("wood") || n.contains("plank") || n.contains("stem")
                || n.contains("bookshelf") || n.contains("crafting")) {
            return 3;
        }
        if (n.contains("sand") || n.contains("gravel") || n.contains("terracotta")
                || n.contains("concrete") || n.contains("wool")) {
            return 4;
        }
        if (n.contains("snow") || n.contains("ice") || n.contains("quartz")) {
            return 5;
        }
        if (n.contains("lava") || n.contains("magma")) {
            return 6;
        }
        return 0;
    }

    /** Публичный пропуск NBT-тега (для парсинга чанков). */
    public static void skipNbt(byte[] d, int[] h, int type) {
        skipPayload(d, h, type);
    }

    // ── низкоуровневое чтение NBT (big-endian, сетевой формат) ──

    private static String readName(byte[] d, int[] h) {
        int n = readU16(d, h);
        String s = new String(d, h[0], n, StandardCharsets.UTF_8);
        h[0] += n;
        return s;
    }

    private static String readString(byte[] d, int[] h) {
        // варинт-строка протокола (для id реестра) ИЛИ имя тега? здесь — варинт
        int n = readVarInt(d, h);
        String s = new String(d, h[0], n, StandardCharsets.UTF_8);
        h[0] += n;
        return s;
    }

    private static void skipPayload(byte[] d, int[] h, int type) {
        switch (type) {
            case 1 -> h[0] += 1;
            case 2 -> h[0] += 2;
            case 3 -> h[0] += 4;
            case 4 -> h[0] += 8;
            case 5 -> h[0] += 4;
            case 6 -> h[0] += 8;
            case 7 -> {
                int n = readI32(d, h);
                h[0] += n;
            }
            case 8 -> {
                int n = readU16(d, h);
                h[0] += n;
            }
            case 9 -> {
                int et = d[h[0]++] & 0xFF;
                int n = readI32(d, h);
                for (int i = 0; i < n; i++) {
                    skipPayload(d, h, et);
                }
            }
            case 10 -> {
                while (true) {
                    int t = d[h[0]++] & 0xFF;
                    if (t == 0) {
                        return;
                    }
                    readName(d, h);
                    skipPayload(d, h, t);
                }
            }
            case 11 -> h[0] += 4L * readI32(d, h);
            case 12 -> h[0] += 8L * readI32(d, h);
            default -> {
            }
        }
    }

    private static int readU16(byte[] d, int[] h) {
        int v = ((d[h[0]] & 0xFF) << 8) | (d[h[0] + 1] & 0xFF);
        h[0] += 2;
        return v;
    }

    private static int readI32(byte[] d, int[] h) {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            v = (v << 8) | (d[h[0]++] & 0xFF);
        }
        return v;
    }

    private static int readVarInt(byte[] d, int[] h) {
        int v = 0, pos = 0;
        while (pos < 32) {
            byte b = d[h[0]++];
            v |= (b & 0x7F) << pos;
            if ((b & 0x80) == 0) {
                return v;
            }
            pos += 7;
        }
        return v;
    }
}
