package ru.rooyzee.elytrixclient.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.xrose.command.ClientCommand;
import org.xrose.utils.text.ChatUtil;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;

/**
 * Команды управления ботами и их физикой прямо из чата:
 * .bots, .randommove, .follow, .stay, .jump, .rejoin.
 * .help покажет их все вместе с остальными.
 */
public final class ElytrixBotCommands {

    private static final String E = ":small_blue_diamond:";

    private static ElytrixConfig cfg() {
        return ElytrixclientClient.CONFIG;
    }

    private static boolean toggle(boolean v, String on, String off) {
        ChatUtil.success(v ? on : off);
        return v;
    }

    /** Памятка всех команд управления ботами (для .bots и .help). */
    static void list() {
        ChatUtil.header("Управление ботами");
        ChatUtil.entry(E, ".bots start | stop", "запустить / остановить ботов");
        ChatUtil.entry(E, ".randommove", "физика: случайные прогулки (вкл/выкл)");
        ChatUtil.entry(E, ".stay", "физика: боты стоят на месте");
        ChatUtil.entry(E, ".jump", "автопрыжки ботов (вкл/выкл)");
        ChatUtil.entry(E, ".follow ник", "следовать за конкретным игроком");
        ChatUtil.entry(E, ".ffserver", "FF-режим: боты прыгают, приседают, крутят камерой");
    }

    static void start() {
        var rd = ElytrixclientClient.RUST_BOTS;
        var ob = ElytrixclientClient.OWN_BOTS;
        if (rd.isRunning() || ob.isRunning()) {
            ChatUtil.error("Боты уже запущены — .bots stop");
            return;
        }
        ElytrixConfig c = cfg();
        String addr = c.botAddress.trim();
        String host = addr;
        int port = 25565;
        int colon = addr.lastIndexOf(':');
        if (colon > 0) {
            host = addr.substring(0, colon);
            try {
                port = Integer.parseInt(addr.substring(colon + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        if (host.isEmpty()) {
            ChatUtil.error("Укажи адрес: меню → Боты → Адрес сервера");
            return;
        }
        ru.rooyzee.elytrixclient.client.bots.own.OwnBotSettings st =
                new ru.rooyzee.elytrixclient.client.bots.own.OwnBotSettings();
        st.count = c.botmarkCount;
        st.delayMs = c.botmarkDelay;
        st.timeoutMs = c.botmarkTimeout;
        st.prefix = c.ownBotPrefix;
        st.autoReg = c.botAutoReg;
        st.autoLogin = c.botAutoLogin;
        st.password = c.botPassword;
        st.spam = c.bmSpam;
        st.spamMessage = c.bmSpamMessage;
        st.spamDelayMin = c.botSpamMin;
        st.spamDelayMax = c.botSpamMax;
        st.rotation = c.bmRotation;
        st.swing = c.bmSwing;
        st.mode = c.botMode;
        st.autoJump = c.botAutoJump;
        st.captcha = c.botCaptcha;
        st.antiAfk = c.botAntiAfk;
        st.useProxy = c.botUseProxy;
        st.rejoin = c.botRejoin;
        st.rejoinDelayMs = c.botRejoinDelay;
        int proto = ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.probeProtocol(host, port, 3000);
        proto = proto < 0 ? -proto : proto;
        if (ru.rooyzee.elytrixclient.client.bots.rust.RustBotDaemon.available(c)) {
            rd.start(c);
        } else {
            ob.start(host, port, proto, st);
        }
        ChatUtil.success("Запускаем " + c.botmarkCount + " ботов на " + host + ":" + port);
    }

    static void stop() {
        ElytrixclientClient.RUST_BOTS.stop();
        ElytrixclientClient.OWN_BOTS.stop();
        ChatUtil.success("Боты остановлены");
    }

    public static final class Bots extends ClientCommand {
        public Bots() {
            super("bots", "Боты: список команд | start | stop", E);
        }

        @Override
        public void build(LiteralArgumentBuilder<Object> b) {
            b.executes(ctx -> {
                list();
                return 1;
            });
            b.then(LiteralArgumentBuilder.<Object>literal("start").executes(ctx -> {
                start();
                return 1;
            }));
            b.then(LiteralArgumentBuilder.<Object>literal("stop").executes(ctx -> {
                stop();
                return 1;
            }));
        }
    }

    public static final class RandomMove extends ClientCommand {
        public RandomMove() {
            super("randommove", "Физика ботов: случайные прогулки (вкл/выкл)", E);
        }

        @Override
        public void build(LiteralArgumentBuilder<Object> b) {
            b.executes(ctx -> {
                cfg().botMode = cfg().botMode == 2 ? 0 : 2;
                toggle(cfg().botMode == 2, "randommove: боты гуляют", "randommove: выкл (стоят)");
                return 1;
            });
        }
    }

    public static final class Follow extends ClientCommand {
        public Follow() {
            super("follow", "Физика ботов: за тобой или за игроком: .follow {ник}", E);
        }

        @Override
        public void build(LiteralArgumentBuilder<Object> b) {
            b.executes(ctx -> {
                cfg().botFollowTarget = "";
                cfg().botMode = cfg().botMode == 1 ? 0 : 1;
                toggle(cfg().botMode == 1, "follow: боты идут за тобой", "follow: выкл (стоят)");
                return 1;
            });
            b.then(LiteralArgumentBuilder.<Object>literal("off").executes(ctx -> {
                cfg().botMode = 0;
                ChatUtil.success("follow: выкл (стоят)");
                return 1;
            }));
            b.then(com.mojang.brigadier.builder.RequiredArgumentBuilder
                    .<Object, String>argument("nick", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> {
                        String nick = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "nick");
                        cfg().botFollowTarget = nick;
                        cfg().botMode = 1;
                        ChatUtil.success("follow: боты идут за " + nick + " (если он в твоей видимости)");
                        return 1;
                    }));
        }
    }

    public static final class Stay extends ClientCommand {
        public Stay() {
            super("stay", "Физика ботов: стоят на месте", E);
        }

        @Override
        public void build(LiteralArgumentBuilder<Object> b) {
            b.executes(ctx -> {
                cfg().botMode = 0;
                ChatUtil.success("stay: боты стоят");
                return 1;
            });
        }
    }

    public static final class FfServer extends ClientCommand {
        public FfServer() {
            super("ffserver", "FF-режим ботов: быстрые прыжки + приседания + камера во все стороны", E);
        }

        @Override
        public void build(LiteralArgumentBuilder<Object> b) {
            b.executes(ctx -> {
                ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.liveFfServer =
                        !ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.liveFfServer;
                ChatUtil.success(ru.rooyzee.elytrixclient.client.bots.own.OwnBotEngine.liveFfServer
                        ? "ffserver: ВКЛ — боты беснуются" : "ffserver: выкл");
                return 1;
            });
        }
    }

    public static final class Jump extends ClientCommand {
        public Jump() {
            super("jump", "Автопрыжки ботов (вкл/выкл)", E);
        }

        @Override
        public void build(LiteralArgumentBuilder<Object> b) {
            b.executes(ctx -> {
                cfg().botAutoJump = !cfg().botAutoJump;
                toggle(cfg().botAutoJump, "jump: автопрыжки вкл", "jump: автопрыжки выкл");
                return 1;
            });
        }
    }

}
