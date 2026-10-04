package wtf.expensive.client.util;

import java.util.List;

public class BetterText {
    private static final long TYPE_DELAY = 100;
    private static final long ERASE_DELAY = 60;
    private static final long GAP = 400;

    private final List<String> texts;
    private final long holdDelay;
    private long startTime = -1L;

    public BetterText(List<String> texts, long holdDelay) {
        this.texts = texts;
        this.holdDelay = holdDelay;
    }

    public String get() {
        if (texts.isEmpty()) {
            return "";
        }

        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
        }

        long elapsed = System.currentTimeMillis() - startTime;
        long total = 0;
        for (String text : texts) {
            total += cycleLength(text);
        }
        long position = elapsed % total;

        for (String text : texts) {
            long cycle = cycleLength(text);
            if (position < cycle) {
                return frame(text, position);
            }
            position -= cycle;
        }
        return texts.getFirst();
    }

    private long cycleLength(String text) {
        return text.length() * TYPE_DELAY + holdDelay + (text.length() + 1) * ERASE_DELAY + GAP;
    }

    private String frame(String text, long position) {
        if (text.isEmpty()) {
            return "";
        }

        long typing = text.length() * TYPE_DELAY;
        if (position < typing) {
            return text.substring(0, (int) (position / TYPE_DELAY) + 1);
        }
        position -= typing;

        if (position < holdDelay) {
            return text;
        }
        position -= holdDelay;

        long erasing = (text.length() + 1) * ERASE_DELAY;
        if (position < erasing) {
            int left = text.length() - (int) (position / ERASE_DELAY);
            return text.substring(0, Math.max(0, left));
        }
        return "";
    }
}
