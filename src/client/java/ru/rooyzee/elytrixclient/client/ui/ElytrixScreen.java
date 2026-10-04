package ru.rooyzee.elytrixclient.client.ui;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;
import ru.rooyzee.elytrixclient.client.util.LogBuffer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Панель ElytrixClient целиком на YACL (YetAnotherConfigLib 3.9.7+26.2).
 *
 * 5 категорий YACL = 5 вкладок прежней самописной панели:
 * Обзор / BotMark / SoulFire / Консоль / Настройки.
 *
 * Особенности YACL, о которых надо помнить:
 *  • значения опций применяются к конфигу не сразу, а при applyValue()
 *    (кнопка «Сохранить» внизу экрана либо наши кнопки действий — они
 *    вызывают applyAll() + cfg.save() перед запуском процессов);
 *  • категории рисуются как таб-бар сверху, группы — как секции,
 *    подписи к опциям показываются в тултипе и в подзаголовке группы.
 */
public final class ElytrixScreen {

    private static final String[] ACCENTS = {"#8B5CF6", "#22D3EE", "#4ADE80", "#F472B6", "#FBBF24"};
    private static final int CONSOLE_LINES = 22;
    private static final int CONSOLE_LINE_WIDTH = 110;

    private final ElytrixConfig cfg = ElytrixclientClient.CONFIG;
    private final List<Option<?>> options = new ArrayList<>();

    private ElytrixScreen() {
    }

    /** Открыть панель. parent == null — вернёмся в игру, а не в предыдущий экран. */
    public static Screen create(Screen parent) {
        return new ElytrixScreen().build(parent);
    }

    private Screen build(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(c("ElytrixClient — панель нагрузочного теста"))
                .category(overview())
                .category(botMark())
                .category(soulFire())
                .category(consoleCategory())
                .category(settings())
                .save(cfg::save)
                .build()
                .generateScreen(parent);
    }

    // ------------------------------------------------------------------ Обзор

    private ConfigCategory overview() {
        return ConfigCategory.createBuilder()
                .name(c("Обзор"))
                .group(OptionGroup.createBuilder()
                        .name(c("Цель — только свой сервер"))
                        .option(text("Хост", "Адрес сервера, который ты тестируешь.",
                                "127.0.0.1", () -> cfg.host, v -> cfg.host = v))
                        .option(slider("Порт", "Порт сервера.", 25565, 1, 65535,
                                () -> cfg.port, v -> cfg.port = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Управление"))
                        .option(button("Запустить всё", "BotMark + SoulFire", (screen, opt) -> run(() -> {
                            ElytrixclientClient.BOTMARK.start(cfg);
                            ElytrixclientClient.SOULFIRE.botsStart(cfg);
                        })))
                        .option(button("Остановить всё", "стоп", (screen, opt) -> run(() -> {
                            ElytrixclientClient.BOTMARK.stop();
                            ElytrixclientClient.SOULFIRE.botsStop(cfg);
                        })))
                        .build())
                .option(LabelOption.create(c("BotMark: " + ElytrixclientClient.BOTMARK.status())))
                .option(LabelOption.create(c("SoulFire: " + ElytrixclientClient.SOULFIRE.status(cfg))))
                .option(LabelOption.create(c("BotMark — сырой поток ботов (сервер в offline-mode).")
                        .withStyle(ChatFormatting.GRAY)))
                .option(LabelOption.create(c("SoulFire — умные боты: капча, регистрация, прокси.")
                        .withStyle(ChatFormatting.GRAY)))
                .option(button("Обновить статус", "обновить", (screen, opt) -> reopen()))
                .build();
    }

    // ------------------------------------------------------------------ BotMark

    private ConfigCategory botMark() {
        return ConfigCategory.createBuilder()
                .name(c("BotMark"))
                .group(OptionGroup.createBuilder()
                        .name(c("Процесс"))
                        .option(text("Путь к botmark", "botmark.exe (Windows) или бинарник Linux.",
                                "C:/tools/botmark.exe", () -> cfg.botmarkPath, v -> cfg.botmarkPath = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Нагрузка"))
                        .option(slider("Ботов", "Сколько клиентов поднимать.", 50, 1, 5000,
                                () -> cfg.botmarkCount, v -> cfg.botmarkCount = v))
                        .option(slider("Задержка, мс", "Пауза между запусками ботов.", 200, 0, 5000,
                                () -> cfg.botmarkDelay, v -> cfg.botmarkDelay = v))
                        .option(slider("Таймаут, мс", "Таймаут подключения.", 5000, 500, 60000,
                                () -> cfg.botmarkTimeout, v -> cfg.botmarkTimeout = v))
                        .option(text("Текст спама", "Что боты пишут в чат (--spam_message).",
                                "Please do not spam!", () -> cfg.bmSpamMessage, v -> cfg.bmSpamMessage = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Поведение"))
                        .option(tick("Спам в чат", "Флаг --enable_spam_message.", true,
                                () -> cfg.bmSpam, v -> cfg.bmSpam = v))
                        .option(tick("Повороты", "Флаг --enable_rotation.", true,
                                () -> cfg.bmRotation, v -> cfg.bmRotation = v))
                        .option(tick("Взмахи", "Флаг --enable_swing.", true,
                                () -> cfg.bmSwing, v -> cfg.bmSwing = v))
                        .option(tick("Ходьба", "Флаг --enable_movement.", true,
                                () -> cfg.bmMovement, v -> cfg.bmMovement = v))
                        .option(tick("Прыжки", "Флаг --enable_jumping.", true,
                                () -> cfg.bmJumping, v -> cfg.bmJumping = v))
                        .option(tick("Физика", "Флаг --enable_physics.", true,
                                () -> cfg.bmPhysics, v -> cfg.bmPhysics = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Действия"))
                        .option(button("Запустить BotMark", "запустить", (screen, opt) -> run(
                                () -> ElytrixclientClient.BOTMARK.start(cfg))))
                        .option(button("Остановить BotMark", "остановить", (screen, opt) ->
                                ElytrixclientClient.BOTMARK.stop()))
                        .build())
                .build();
    }

    // ------------------------------------------------------------------ SoulFire

    private ConfigCategory soulFire() {
        return ConfigCategory.createBuilder()
                .name(c("SoulFire"))
                .group(OptionGroup.createBuilder()
                        .name(c("Режим работы"))
                        .option(dropdown("Режим", "cli — локальный процесс SoulFireCLI, mcp — HTTP MCP API.",
                                "cli", List.of("cli", "mcp"), () -> cfg.soulfireMode, v -> cfg.soulfireMode = v))
                        .option(text("Инстанс (instance_id)", "Заполняется после запуска; нужен для MCP-вызовов.",
                                "", () -> cfg.soulfireInstanceId, v -> cfg.soulfireInstanceId = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Пути и доступ"))
                        .option(text("SoulFireCLI.jar", "Путь к jar-нику SoulFire CLI.",
                                "C:/soulfire/SoulFireCLI.jar", () -> cfg.soulfireJar, v -> cfg.soulfireJar = v))
                        .option(text("Память JVM", "Аргумент -Xmx для SoulFire.", "-Xmx2G",
                                () -> cfg.soulfireJavaArgs, v -> cfg.soulfireJavaArgs = v))
                        .option(text("MCP URL", "Адрес MCP-эндпоинта SoulFire.",
                                "http://127.0.0.1:38765/mcp", () -> cfg.soulfireApiUrl, v -> cfg.soulfireApiUrl = v))
                        .option(text("API-токен", "Профиль → API token в SoulFire GUI.",
                                "", () -> cfg.soulfireToken, v -> cfg.soulfireToken = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Действия"))
                        .option(button("Запустить SoulFire CLI", "старт", (screen, opt) -> run(
                                () -> ElytrixclientClient.SOULFIRE.startSoulFire(cfg))))
                        .option(button("Стоп SoulFire", "стоп", (screen, opt) ->
                                ElytrixclientClient.SOULFIRE.stopSoulFire()))
                        .option(button("Боты: старт", "bots start", (screen, opt) -> run(
                                () -> ElytrixclientClient.SOULFIRE.botsStart(cfg))))
                        .option(button("Боты: стоп", "bots stop", (screen, opt) ->
                                ElytrixclientClient.SOULFIRE.botsStop(cfg)))
                        .option(button("Кто онлайн", "online", (screen, opt) ->
                                ElytrixclientClient.SOULFIRE.online(cfg)))
                        .option(button("MCP: список ботов", "MCP", (screen, opt) ->
                                ElytrixclientClient.SOULFIRE.callMcp(cfg, "get_bot_list", null)))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Команда в консоль SoulFire"))
                        .option(text("Команда", "Например: bots start или bot <имя> say <текст>.",
                                "", () -> cfg.soulfireCommand, v -> cfg.soulfireCommand = v))
                        .option(button("Отправить команду", "отправить", (screen, opt) -> run(() -> {
                            if (!cfg.soulfireCommand.isBlank()) {
                                ElytrixclientClient.SOULFIRE.send(cfg.soulfireCommand.trim());
                            }
                        })))
                        .build())
                .build();
    }

    // ------------------------------------------------------------------ Консоль

    private ConfigCategory consoleCategory() {
        LogBuffer log = ElytrixclientClient.LOG;
        List<String> lines = log.snapshot();

        ConfigCategory.Builder builder = ConfigCategory.createBuilder().name(c("Консоль"))
                .group(OptionGroup.createBuilder()
                        .name(c("Действия"))
                        .option(button("Обновить", "обновить", (screen, opt) -> reopen()))
                        .option(button("Очистить", "очистить", (screen, opt) -> {
                            log.clear();
                            reopen();
                        }))
                        .option(button("Копировать в буфер", "копировать", (screen, opt) ->
                                Minecraft.getInstance().keyboardHandler.setClipboard(String.join("\n", log.snapshot()))))
                        .build());

        OptionGroup.Builder linesGroup = OptionGroup.createBuilder().name(c("Последние " + CONSOLE_LINES + " строк"));
        if (lines.isEmpty()) {
            linesGroup.option(LabelOption.create(c("Лог пуст.").withStyle(ChatFormatting.GRAY)));
        } else {
            int from = Math.max(0, lines.size() - CONSOLE_LINES);
            for (int i = from; i < lines.size(); i++) {
                linesGroup.option(LabelOption.create(consoleLine(lines.get(i))));
            }
        }
        return builder.group(linesGroup.build())
                .option(LabelOption.create(c("всего строк: " + lines.size()).withStyle(ChatFormatting.DARK_GRAY)))
                .build();
    }

    // ------------------------------------------------------------------ Настройки

    private ConfigCategory settings() {
        return ConfigCategory.createBuilder()
                .name(c("Настройки"))
                .group(OptionGroup.createBuilder()
                        .name(c("Внешний вид"))
                        .option(dropdown("Акцент", "Цвет акцента (пока хранится в конфиге).",
                                "#8B5CF6", List.of(ACCENTS), () -> cfg.accent, v -> cfg.accent = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(c("Конфиг"))
                        .option(button("Сохранить конфиг", "сохранить", (screen, opt) -> run(() -> {
                        })))
                        .option(button("Открыть папку конфига", "открыть", (screen, opt) -> {
                            Path dir = ElytrixConfig.file().getParent();
                            if (dir != null) Util.getPlatform().openPath(dir);
                        }))
                        .build())
                .option(LabelOption.create(c("Файл конфига: " + ElytrixConfig.file()).withStyle(ChatFormatting.GRAY)))
                .option(LabelOption.create(c("Шорткат панели — правый Ctrl.").withStyle(ChatFormatting.GRAY)))
                .option(LabelOption.create(c("Лого, сплэши и иконка — встроенный ресурспак ElytrixClient.")
                        .withStyle(ChatFormatting.GRAY)))
                .build();
    }

    // ------------------------------------------------------------------ утилиты

    private Option<String> text(String name, String description, String def,
                                Supplier<String> get, Consumer<String> set) {
        return register(Option.<String>createBuilder()
                .name(c(name))
                .description(OptionDescription.of(c(description)))
                .binding(def, get, set)
                .controller(StringControllerBuilder::create)
                .build());
    }

    private Option<String> dropdown(String name, String description, String def, List<String> values,
                                    Supplier<String> get, Consumer<String> set) {
        return register(Option.<String>createBuilder()
                .name(c(name))
                .description(OptionDescription.of(c(description)))
                .binding(def, get, set)
                .controller(opt -> DropdownStringControllerBuilder.create(opt).values(values).allowAnyValue(true))
                .build());
    }

    private Option<Integer> slider(String name, String description, int def, int min, int max,
                                   Supplier<Integer> get, Consumer<Integer> set) {
        return register(Option.<Integer>createBuilder()
                .name(c(name))
                .description(OptionDescription.of(c(description)))
                .binding(def, get, set)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1))
                .build());
    }

    private Option<Boolean> tick(String name, String description, boolean def,
                                 Supplier<Boolean> get, Consumer<Boolean> set) {
        return register(Option.<Boolean>createBuilder()
                .name(c(name))
                .description(OptionDescription.of(c(description)))
                .binding(def, get, set)
                .controller(TickBoxControllerBuilder::create)
                .build());
    }

    private ButtonOption button(String name, String text, BiConsumer<YACLScreen, ButtonOption> action) {
        return ButtonOption.createBuilder()
                .name(c(name))
                .text(c(text))
                .description(OptionDescription.of(c(name)))
                .action(action)
                .build();
    }

    private <T extends Option<?>> T register(T option) {
        options.add(option);
        return option;
    }

    /**
     * Кнопки действий: сначала применяем введённые в поля значения к конфигу
     * (в YACL они «висят» до сохранения), сохраняем на диск и только потом
     * запускаем процесс — иначе боты ушли бы со старыми настройками.
     */
    private void run(Runnable action) {
        for (Option<?> option : options) {
            option.applyValue();
        }
        cfg.save();
        action.run();
    }

    private void reopen() {
        Minecraft.getInstance().setScreenAndShow(create(null));
    }

    private static Component consoleLine(String line) {
        ChatFormatting color = ChatFormatting.GRAY;
        String lower = line.toLowerCase();
        if (lower.contains("ошиб") || lower.contains("error") || lower.contains("exception")) {
            color = ChatFormatting.RED;
        } else if (lower.contains("warn")) {
            color = ChatFormatting.YELLOW;
        } else if (line.startsWith("[BotMark]")) {
            color = ChatFormatting.LIGHT_PURPLE;
        } else if (line.startsWith("[SoulFire")) {
            color = ChatFormatting.AQUA;
        }
        return c(ellipsize(line)).withStyle(color);
    }

    private static String ellipsize(String line) {
        if (line.length() <= CONSOLE_LINE_WIDTH) {
            return line;
        }
        return line.substring(0, CONSOLE_LINE_WIDTH) + "…";
    }

    private static Component c(String value) {
        return Component.literal(value);
    }
}
