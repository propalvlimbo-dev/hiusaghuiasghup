package ru.rooyzee.elytrixclient.client.features;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.xrose.event.EventManager;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;

/**
 * Серверный MSPT: клиент раз в 5 секунд шлёт {@code /mspt} (команда Paper) и парсит
 * ответ из чата («Mean tick time: 4.02 ms»). Напрямую серверный MSPT с клиента
 * не измерим никак — только так. Если команды нет (не Paper) или нет прав —
 * помечаем unavailable и панель показывает прочерк, а не враньё.
 */
public final class ServerMspt {

    /** -1 = ещё не измерено / недоступно. */
    public static volatile float mspt = -1f;
    public static volatile boolean unavailable;

    private static long lastSendAt;
    private static long lastProbeAt;

    public static void init() {
        EventManager.subscribe(new Listener());
    }

    private ServerMspt() {
    }

    static final class Listener {

        @EventTarget
        public void onTick(GameTickEvent e) {
            if (!ElytrixclientClient.CONFIG.msptProbe || unavailable) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastSendAt > 5000) {
                lastSendAt = now;
                lastProbeAt = now;
                try {
                    client.player.connection.sendCommand("mspt");
                } catch (Throwable ignored) {
                }
            }
        }

        @EventTarget
        public void onPacket(PacketReceiveEvent e) {
            if (!(e.getPacket() instanceof ClientboundSystemChatPacket chat)) {
                return;
            }
            String raw = chat.content().getString();
            if (raw == null || raw.isEmpty()) {
                return;
            }
            String s = raw.replaceAll("§.", "");
            long now = System.currentTimeMillis();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)tick time[^0-9]{0,16}(\\d+(?:[.,]\\d+)?)").matcher(s);
            if (m.find()) {
                try {
                    mspt = Float.parseFloat(m.group(1).replace(',', '.'));
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
            if (now - lastProbeAt < 2500 && (s.toLowerCase().contains("unknown command")
                    || s.toLowerCase().contains("no such command")
                    || s.contains("неизвестная команда")
                    || s.toLowerCase().contains("permission")
                    || s.contains("прав"))) {
                unavailable = true;
            }
        }
    }
}
