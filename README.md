# ElytrixClient

Fabric-мод для Minecraft **26.2** — панель управления нагрузочным тестом **своего** сервера.
Связка: **BotMark** (массовый поток соединений) + **SoulFire** (умные боты с капчей и регистрацией),
а ElytrixClient — GUI и единая кнопка «пуск».

> ⚠️ Только свой сервер / сервер с письменным разрешением. BotMark (MIT) и SoulFire (AGPL-3.0) —
> отдельные внешние программы: мод **не линкует их код**, а запускает процесс/дёргает API.

---

## 0. Что изменилось в 26.x (иначе проект не соберётся)

Мир моддинга после 1.21.11 другой — все три пункта обязательны:

| Что | Как надо | Почему |
|---|---|---|
| JDK | **25** | MC 26.2 и Loom 1.18 |
| Gradle | **9.7+** | Loom 1.18 объявлен под Gradle 9.7 |
| **ID плагина Loom** | **`net.fabricmc.fabric-loom`** | это плагин-маркер «без ремапа»; старый `fabric-loom` включает ремаппинг и требует `mappings` |
| **Маппинги** | **никаких** — строки `mappings` быть не должно | Yarn закрыт на 1.21.11, а official Mojang mappings для 26.x не публикуются, потому что **игра больше не обфусцирована** |

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version '1.18-SNAPSHOT'   // ← НЕ 'fabric-loom'
}
...
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    // mappings ...            ← строки НЕТ вообще
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

Три id у Loom: `fabric-loom` (базовый, ремапит), `net.fabricmc.fabric-loom-remap` (то же явно)
и `net.fabricmc.fabric-loom` (маркер `disableObfuscation` — то, что нужно для 26.x).

Обрати внимание: `implementation`, а не `modImplementation` — ремапить больше нечего.
Так же сделано в официальном reference-проекте `FabricMC/fabric-docs` (ветка latest = 26.2)
и в Wurst7 под 26.2.

---

## 1. Что ввести в IntelliJ (MinecraftDev-визард)

| Поле | Значение |
|---|---|
| Name / Location | `ElytrixClient`, путь без кириллицы и OneDrive |
| Groups / Templates | **Mod** / **Fabric** |
| Language | Java |
| Minecraft Version | **26.2** |
| Loom Version | `1.18-SNAPSHOT` |
| Loader Version | `0.19.5` |
| **Yarn Version** | игнорируем — в 26.x его не существует |
| Fabric API Version | `0.161.0+26.2` |
| **Environment** | **Client** |
| Use Mixins | ✅ (задел) |
| **Use Datagen** | ⬜ снять |
| Split Sources | ✅ |
| Mod ID / Name | `elytrixclient` / `ElytrixClient` |
| Main Class | `ru.rooyzee.elytrixclient.Elytrixclient` |
| Group ID | `ru.rooyzee` |
| License | для публикации `MIT` |

**После генерации визардом** (его шаблон ещё старый): удалить строку `mappings ...`,
заменить `modImplementation` → `implementation`, удалить `yarn_mappings` из `gradle.properties`
и снести сгенерированные классы-заглушки.

---

## 2. Как поднять проект

Проект лежит прямо в корне репозитория: `build.gradle`, `gradle.properties`, `settings.gradle`,
`src/` — это и есть мод. **File → Open** → корень репозитория.

```bash
.\gradlew.bat build        # Windows/PowerShell
.\gradlew.bat runClient
```

(`./gradlew` в PowerShell не работает — это bash-скрипт. Нужен `.\gradlew.bat`.)

### Запустить Minecraft из IDEA за 2 клика

1. **Gradle JVM = 25** (иначе сборка падает с `requires at least JVM runtime version 25`):
   `File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM` → выбрать/скачать **Temurin 25**
   (там же `File → Project Structure → SDK: 25`).
2. В панели **Gradle** справа: `Tasks → fabric → runClient` — двойной клик, Minecraft стартует сам.
3. Либо выбери в списке конфигураций сверху готовый **Minecraft Client (runClient)** (лежит в `.run/`) и нажми ▶ (**Shift+F10**).

Первый запуск качает библиотеки и ассеты (1–3 минуты), следующие — секунды.
Игра открывается с уже загруженным модом: **панель — правый Ctrl**, в главном меню кнопка *Elytrix Client*.
Если после запуска ничего не видно — смотри окно `runClient` в IDEA: там лог мода, строки `[Elytrix]`. Во время запуска не запускай `runClient` второй раз (папка `run/` занята) — если зависло: `.\gradlew.bat --stop`.

---

## 3. Что уже написано

```
src/main/java/ru/rooyzee/elytrixclient/Elytrixclient.java        — common entrypoint
src/main/resources/fabric.mod.json, lang/, icon.png              — манифест, локализация, иконка
src/main/resources/assets/elytrixclient/window_icon_*.png        — иконки окна (16…256)
src/main/resources/resourcepacks/elytrixclient/                  — встроенный ресурспак:
    pack.mcmeta, pack.png                                            лого «ELYTRIX CLIENT», свои сплэши,
    assets/minecraft/textures/gui/title/{minecraft,edition}.png      убранная надпись «JAVA EDITION»
    assets/minecraft/texts/splashes.txt
src/client/java/ru/rooyzee/elytrixclient/client/
    ElytrixclientClient.java      — конфиг, лог, раннеры, панель по ПРАВОМУ CTRL, ресурспак, иконка
    config/ElytrixConfig.java     — config/elytrixclient.json
    util/LogBuffer.java           — общий лог BotMark + SoulFire + мода
    botmark/BotMarkRunner.java    — запуск бинарника BotMark, стрим stdout
    soulfire/SoulFireController.java — CLI-режим (stdin/stdout) и MCP-режим (HTTP API)
    soulfire/McpClient.java       — JSON-RPC вызовы MCP-инструментов SoulFire
    ui/ElytrixScreen.java         — панель на YACL: Обзор / BotMark / SoulFire / Консоль / Настройки
    ui/ElytrixTitleScreen.java    — правки главного меню: кнопка панели + строка брендинга
    ui/WindowIcon.java            — подмена иконки окна/панели задач на нашу
art/icon-master.png, art/logo-master.png — мастер-картинки (из них собираются текстуры)
```

Текстуры пересобираются одной командой (чистый Python, без PIL):

```bash
python scripts/make-textures.py      # из scripts/ в корне репозитория
```

UI — целиком на **YACL** (`dev.isxander:yet-another-config-lib:3.9.7+26.2-fabric`, Jar-in-Jar):
5 категорий YACL = 5 вкладок, группы = секции, подписи = лейблы, действия = кнопки.
Свой код отрисовки не нужен — версионно-зависимых файлов в моде больше нет.
Подробности и почему так — `UI-БИБЛИОТЕКИ.md`.

---

## 4. API 26.2: что называется иначе

Всё это уже учтено в коде каркаса (сверено по декомпилированным исходникам 26.2 и по Wurst7 26.2):

| Было (≤1.21.x, Yarn/Mojmap) | В 26.2 |
|---|---|
| `GuiGraphics` | **`GuiGraphicsExtractor`** (`net.minecraft.client.gui`) |
| `drawString(font, …)` | **`graphics.text(font, str, x, y, color, shadow)`** |
| `Screen#render(...)` | **`Screen#extractRenderState(GuiGraphicsExtractor, int, int, float)`** |
| `Screen#extractBackground(...)` | то же имя, но с `GuiGraphicsExtractor` |
| `minecraft.setScreen(...)` | **`minecraft.setScreenAndShow(...)`** (или `minecraft.gui.setScreen(...)`) |
| `InputConstants.isKeyDown(long, key)` | **`InputConstants.isKeyDown(Window, key)`** |
| `window.getWindow()` (хэндл) | **`window.handle()`** |
| `Util` из `net.minecraft` | **`net.minecraft.util.Util`**, `getPlatform().openFile(File)` |
| `ResourceLocation` | **`Identifier`** (`net.minecraft.resources.Identifier`) |
| `graphics.outline(x1,y1,x2,y2,c)` | **`outline(x, y, width, height, c)`** (другая семантика!) |

Не изменилось: `Component`, `Font.width(...)`, `Button.builder(...).bounds(...).build()`,
`addRenderableWidget`, `EditBox` (+`setResponder`/`setValue`/`getValue`/`setMaxLength`),
`rebuildWidgets()`, `onClose()`, `mouseScrolled(double,double,double,double)`,
`ClientTickEvents.END_CLIENT_TICK`, `minecraft.keyboardHandler.setClipboard(...)`.

Если захочешь нормальный кейбинд в управлении (а не прямое чтение клавиши, как сейчас) —
в 26.x это `net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(...)`
и `KeyMapping.Category.register(Identifier...)`.

---

## 5. Частые ошибки сборки

| Ошибка | Причина / фикс |
|---|---|
| `Dependency requires at least JVM runtime version 25` | Gradle JVM = JDK 25 (Settings → Build Tools → Gradle) |
| `No matching variant of fabric-loom:1.18.2 ... '9.7.0' vs '9.2.1'` | Gradle 9.7+: `distributionUrl=...gradle-9.7.1-bin.zip` в `gradle/wrapper/gradle-wrapper.properties` + Reload |
| `The mappings (net.fabricmc:yarn:…) were not built for Minecraft version 26.2` | строку `mappings` удалить (см. §0) |
| `Failed to find official mojang mappings for 26.2` | то же самое: маппинги не нужны, удалить строку `mappings` |
| `Configuration 'mappings' has no dependencies` | **id плагина**: заменить `id 'fabric-loom'` на `id 'net.fabricmc.fabric-loom'` (маркер «без ремапа») |
| `drawString` / `GuiGraphics` не найдены | см. таблицу в §4 — в 26.2 это `text(...)` и `GuiGraphicsExtractor` |

---

## 6. Брендинг: иконка и главное меню

| Что | Как сделано |
|---|---|
| Иконка окна / панели задач | `ui/WindowIcon.java` — на первом клиентском тике зовёт `GLFW.glfwSetWindowIcon` с нашими `window_icon_{16,32,64,128,256}.png` (ваниль ставит свою иконку раньше, поэтому наша побеждает) и меняет заголовок окна на «ElytrixClient — Minecraft 26.2» |
| Иконка мода (в списке модов) | `assets/elytrixclient/icon.png` (128×128), прописана в `fabric.mod.json` |
| Логотип в главном меню | встроенный ресурспак `resourcepacks/elytrixclient` подменяет `minecraft:textures/gui/title/minecraft.png` на «ELYTRIX CLIENT» |
| Надпись «JAVA EDITION» | там же `edition.png` отдан полностью прозрачным |
| Сплэши | `resourcepacks/elytrixclient/assets/minecraft/texts/splashes.txt` — свои 14 строк |
| Кнопка панели в меню | `ui/ElytrixTitleScreen.java` — `ScreenEvents.AFTER_INIT` + `Screens.getWidgets(screen).add(...)` (левый верхний угол) и подпись с версией через `ScreenEvents.afterExtract` |

Ресурспак регистрируется кодом (`ResourceLoader.registerBuiltinPack`, `PackActivationType.DEFAULT_ENABLED`),
поэтому его видно и можно выключить в «Настройки → Наборы ресурсов».

Мастер-картинки (`art/*.png`) — сгенерированные; любую можно заменить и пересобрать текстуры
скриптом `scripts/make-textures.py`.

---

## 7. Дальше по плану

Обоснование выбора UI-библиотек — в файле **`UI-БИБЛИОТЕКИ.md`**.
Коротко: **весь UI — на YACL 3.9.7+26.2** (Jar-in-Jar), 5 категорий = 5 вкладок.

- [x] Панель на YACL: Обзор / BotMark / SoulFire / Консоль / Настройки (открывается правым Ctrl)
- [x] Иконка клиента, лого «ELYTRIX CLIENT», свои сплэши, кнопка панели в главном меню
- [ ] Живая консоль прямо в экране (нужен маленький кастомный контрол YACL)
- [ ] Тултипы и шапка со сводкой (ботов подключено / отвалилось)
- [ ] График TPS/MSPT (Spark или RCON)
- [ ] Настоящий gRPC-клиент SoulFire
- [ ] Пресеты сценариев (10 / 50 / ступени 1→5→10→20)
- [ ] HUD-оверлей поверх игры
