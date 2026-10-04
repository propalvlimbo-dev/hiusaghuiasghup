package ru.rooyzee.elytrixclient.client.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Кольцевой буфер логов: сюда пишут BotMark, SoulFire и сам мод; консоль в GUI читает отсюда. */
public class LogBuffer {
    private final Deque<String> lines = new ArrayDeque<>();
    private final int max;

    public LogBuffer() {
        this(2000);
    }

    public LogBuffer(int max) {
        this.max = max;
    }

    public synchronized void add(String line) {
        lines.addLast(line);
        while (lines.size() > max) {
            lines.removeFirst();
        }
    }

    public synchronized List<String> snapshot() {
        return new ArrayList<>(lines);
    }

    public synchronized void clear() {
        lines.clear();
    }

    public synchronized int size() {
        return lines.size();
    }
}
