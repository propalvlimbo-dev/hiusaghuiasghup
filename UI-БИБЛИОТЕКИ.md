# UI-библиотеки для ElytrixClient (MC 26.2) — что берём и почему

Дата проверки: **04.10.2026**. Версии сверены по Modrinth API, GitHub-веткам и реальным
`build.gradle` мод-проектов, которые уже собираются под 26.2.

---

## 0. Решение (обновлено 04.10.2026: свой рендер, бело-розовая тема, свои иконки)

Изначально выбрали «весь UI на YACL», но на практике это дало **ванильный вид** (YACL рисует
стандартные контролы Minecraft). Задача — «красивая панель» в духе современных клиентов:
светлые карточки со скруглениями, сайдбар с иконками, тумблеры-«пилюли», слайдеры с акцентом.

Итоговое решение: **полностью свой рендер** (пакет `ui/kit`), без сторонних UI-библиотек
и без ванильных виджетов. Под 26.2 «красивых» UI-библиотек просто нет: `owo-lib` стоит на
1.21.11, `LibGui` — на 1.21.5, `ModernUI-MC` — на 1.21.x, `Vigilance/Elementa` — 1.21.x,
`Dandelion` — alpha. Реально живые под 26.2 только конфиг-либы (YACL `3.9.7+26.2`,
Cloth Config `v26.2`, MoulConfig из Skyblocker), но они дают «экран настроек», а не панель.

| Слой | Что используем |
|---|---|
| Примитивы | `GuiGraphicsExtractor`: `fill` / `fillGradient` / `blit` (+ тонировка цветом) / `enableScissor` / `blurBeforeThisStratum` / `pose()` |
| Скругления, тени, градиенты, свечение | свой `ui/kit/UiDraw` |
| Иконки | свои PNG 16×16 (`UiIcon`, 27 штук) — нарисованы SDF-скриптом `scripts/generate-icons.py` и тонируются цветом темы |
| Виджеты | `UiButton`, `UiToggle`, `UiSlider`, `UiDropdown`, `UiSection` (карточка), `UiInfo`, `UiEmpty` — у всех анимации наведения/нажатия/появления |
| Тема | `UiTheme` — две темы («Бело-розовая», «Тёмная»), 6 акцентов, радиусы 6/9/14 |
| Экран | `ElytrixScreen` — сайдбар, шапка, скролл с полосой, анимация появления и переключения вкладок, масштаб = GUI Scale игры (или ручной %) |
| Экран загрузки | `ui/ElytrixLoader` + `LoadingOverlayMixin` — свой лоадер **на всё время загрузки** вместо ванильного красного |
| Главное меню | `ScreenMixin` рисует `ui/ElytrixBackground` (сетка + дождь символов) вместо панорамы; `TitleScreenMixin` убирает логотип MC и сплэш, вместо них `ui/ElytrixBrand` |
| Иконка окна | `ui/WindowIcon` — установка «как в ванили» (`NativeImage.getPixelsABGR()` → `glfwSetWindowIcon`), повторяется раз в 4 с, т.к. игра ставит свою при создании окна |

YACL из `build.gradle` убран: он больше не используется (конфиг по-прежнему наш,
`config/ElytrixConfig.java`, JSON через Gson). Если когда-нибудь захочется отдельный
«экран настроек в стиле YACL» — вернуть можно одной строкой
`include implementation("dev.isxander:yet-another-config-lib:3.9.7+26.2-fabric")`
(скобки обязательны, репозиторий `https://maven.isxander.dev/releases`).

Плюсы: полный контроль над видом, ноль внешних зависимостей, ничего не ломается при
обновлении MC кроме имён самого `GuiGraphicsExtractor`. Минус: код отрисовки наш —
при мажорных обновлениях игры правим `ui/kit`, а не ждём апдейт библиотеки.

---

## 1. Что реально доступно под 26.2 (проверено 04.10.2026)

| Библиотека | 26.2? | Вес | Что даёт | Вердикт |
|---|---|---|---|---|
| **Ваниль** `Screen`, `GuiGraphicsExtractor` | ✅ это и есть 26.2 | 0 | `fill`, `fillGradient`, `fill(RenderPipeline,…)`, `blit`, `blitSprite` (в т.ч. nine-slice), `text`, scissor, `blurBeforeThisStratum`, готовые `Button`/`EditBox`/`AbstractSlider`/`Checkbox` | **база** |
| **YACL** (isXander) `3.9.7+26.2-fabric` | ✅ релиз 20.09.2026 (Modrinth `yacl`); в Skyblocker для 26.2 стоит 3.9.4 | 1.1 МБ | конфиг-UI: категории, группы, поиск, слайдеры, цветовые пикеры, reset, переопределение контролов | **не берём** (вид ванильный; держим как опцию для отдельного экрана настроек) |
| **Cloth Config** (shedaniel) ветка `v26.2` | ✅ (base_version=26.2, maven.shedaniel.me) | ~1 МБ | `ConfigBuilder`, AutoConfig; классический вид | альтернатива YACL, но вид старее, API многословнее |
| **MoulConfig** (NotEnoughUpdates) `modern/26.2` | ✅ (NEU/Skyblocker/Dandelion) | ~ | динамическая конфиг-UI, бинды/цвета из коробки; публикуется на repo.nea.moe | нужен **Fabric Language Kotlin** → лишний вес |
| **Dandelion** (AzureAaron) `1.0.0-alpha.22+26.2` | ✅, но **alpha** | шэдит MoulConfig | абстракция «YACL или MoulConfig» одним API | не сейчас |
| **ModernUI / ModernUI-MC** (BloCamLimb) | ⚠️ ядро 3.13.0, `ModernUI-MC` master = **MC 26.1.2**, сборка `3.13.0.7-SNAPSHOT` | тяжёлая: Arc3D, LWJGL 3.3.6, ICU4J, свои нативы | nanoVG: настоящие скругления, свои шрифты, тени, блюр | ждём порт под 26.2; пока не тянем |
| **owo-lib / owo-ui** (wisp-forest) | ❌ MC 1.21.11, последний пуш 19.08.2026 | средняя | богатый UI-фреймворк, data-driven экраны | мимо |
| **LibGui** (CottonMC) | ❌ максимум ветка `port/1.21.5` | средняя | widget toolkit | мимо |
| **imgui-java** (SpaiR) | ⚠️ не MC-либа: свой GL-контекст / второе окно | тяжёлая | мгновенные панели и графики | интеграция с blaze3d 26.2 = отдельный проект, не берём |
| **Elementa / Vigilance** (Essential) | ❌ пуш 08.04.2026, MC до 1.21.x | средняя | UI-фреймворк Essential | нет |
| **Fabric API** | ✅ `0.161.0+26.2` | — | хуки/события, но **не** UI | уже подключён |
| **RenderChest / HM API** (AzureAaron) `1.0.3+26.2` | ✅ | малая | рендер-хелперы (предметы/модели), не виджеты | по необходимости |

Итог: под 26.2 живут **только конфиг-библиотеки** (YACL, Cloth, MoulConfig) — и все они
про настройки, а не про «красивую панель с логом и списком ботов». Панель пишем сами.

---

## 2. Свой кит: что именно пишем

`src/client/java/…/ui/widget/`

| Класс | Для чего |
|---|---|
| `ElyWidget` | база: bounds, hover/active/focus, tooltip, `extractRenderState(GuiGraphicsExtractor,…)` |
| `ElyButton` | 3 стиля: primary (accent), ghost (прозрачная), danger (красная) |
| `ElyIconButton` | квадратная кнопка под иконку (16×16) |
| `ElyTabBar` + `ElyTab` | левый сайдбар, активная вкладка с accent-полосой и подсветкой |
| `ElyToggle` | тумблер с анимацией (вместо `Button "ВКЛ/выкл"`) |
| `ElySlider` | 0…N, drag + колесо |
| `ElyDropdown` | выбор из списка (пресеты 10/50/100 ботов) |
| `ElyTextField` | стилизованная обёртка `EditBox` (фон, рамка, плейсхолдер) |
| `ElyPanel` | панель/карточка: заголовок, 9-slice фон, скругления, тень |
| `ElyScroll` + `ElyScrollbar` | прокрутка контента, инерция |
| `ElyList<T>` | виртуализированные списки (боты, прокси, логи) |
| `ElyStatusPill` | «BotMark: RUN 50» — цвет по состоянию |
| `ElyProgress` | прогресс запуска ботов |
| `ElyToast` | всплывашки снизу справа (замена нынешнего самодельного toast) |

`ui/Theme.java` расширяем: палитра (dark/light), радиусы, отступы, шрифтовые стили.
`ui/Render.java` расширяем: `roundedRect`, `roundedOutline`, `gradient`, `shadowRadial`,
`blurBackground`, `divider`, `accentBar`, `nineSlice`.

Всё это — только публичные методы `GuiGraphicsExtractor`, поэтому при обновлении MC
ломается максимум один файл (`Render`), а не весь UI.

---

## 3. «Красиво» без тяжёлых зависимостей

1. **Блюр фона** — в 26.2 есть `GuiGraphicsExtractor.blurBeforeThisStratum()`
   (ванилька использует его в `Screen.extractBackground` при `menuBackgroundBlurriness >= 1`).
   Порядок отрисовки: `blur` → тёмный слой `ARGB ~0xC0101018` → панель.
2. **Скруглённые панели** — `blitSprite` умеет nine-slice (`blitNineSlicedSprite`):
   одна PNG 24×24 в `assets/elytrixclient/textures/gui/panel.png` даёт скругления,
   рамку и «стеклянный» вид без шейдеров. Так же делаем кнопки/поля.
3. **Свой шейдер (позже)** — `GuiGraphicsExtractor.fill(RenderPipeline, x0,y0,x1,y1,color)`
   публичный, значит можно зарегистрировать свой пайплайн и шейдер
   `assets/elytrixclient/shaders/core/rounded_rect.{json,vsh,fsh}` (SDF): идеальные
   скругления любого радиуса, glow вокруг активного бота, градиент по accent-цвету.
4. **Мелочи, которые дают 80% «дорогого» вида**: единая сетка отступов (4/8/12/16),
   accent-полоса у активного элемента, анимация hover 120 мс, тень под панелью,
   моноширинный шрифт в консоли, приглушённые подписи (`Theme.MUTED`).

---

## 4. Как подключить YACL (если берём его для «Настроек»)

`build.gradle`:

```gradle
repositories {
    mavenCentral()
    // ...существующие репозитории loom/fabric
    maven { name = 'isxander'; url = 'https://maven.isxander.dev/releases' }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

    // include = зашить в наш jar (Jar-in-Jar), игроку ставить YACL отдельно не надо.
    include implementation 'dev.isxander:yet-another-config-lib:3.9.7+26.2-fabric'
}
```

### 4.1 Шпаргалка по API YACL 3.9 (сверено по исходникам ветки main, 04.10.2026)

```java
// экран
Screen screen = YetAnotherConfigLib.createBuilder()
        .title(Component.literal("ElytrixClient"))
        .category(category)                 // .categories(...)
        .save(cfg::save)                    // что вызвать при «Сохранить»
        .build()
        .generateScreen(parent /* @Nullable */);

// категория / группа / опция
ConfigCategory.createBuilder().name(...).group(group).option(option).build();
OptionGroup.createBuilder().name(...).option(...).collapsed(false).build();
Option.<Integer>createBuilder()
        .name(...).description(OptionDescription.of(...))
        .binding(defaultValue, getter, setter)
        .controller(IntegerSliderControllerBuilder::create)   // .range(min,max).step(1)
        .build();

// контроллеры: StringControllerBuilder, DropdownStringControllerBuilder(.values(List<String>)),
// IntegerSliderControllerBuilder, TickBoxControllerBuilder, EnumControllerBuilder,
// IntegerFieldControllerBuilder, ColorControllerBuilder, ItemControllerBuilder …

// не-опции
LabelOption.create(Component.literal("строка лога"));
ButtonOption.createBuilder().name(...).text(на_кнопке).action((YACLScreen s, ButtonOption o) -> …).build();

// ВАЖНО: значения опций применяются к конфигу не сразу, а по applyValue().
// Кнопки действий должны сами «протолкнуть» введённое:
for (Option<?> o : options) o.applyValue();
cfg.save();
```

Метод `screen.finishOrSave()` — это кнопка «Сохранить/Готово» самого YACL (при отсутствии
изменений она закрывает экран), для своих кнопок она не подходит — используем `applyValue()`.

Если позже понадобится **живая консоль** — это один класс: `Controller<Component>` +
наследник `dev.isxander.yacl3.gui.AbstractWidget`, который в `extractRenderState(GuiGraphicsExtractor,…)`
рисует строки и обрабатывает `mouseScrolled`. Даёт полностью кастомный виджет внутри YACL-списка.

Проверка, что путь верный: в Skyblocker (MC 26.2, тот же плагин `id 'net.fabricmc.fabric-loom'`,
loom 1.17-SNAPSHOT) стоит `include implementation("dev.isxander:yet-another-config-lib:3.9.4+26.2-fabric")`.

Если зашивать не хотим — добавляем в `fabric.mod.json`:
`"depends": { "yet_another_config_lib_v3": ">=3.9.4" }` и просим игрока поставить YACL.

Как использовать: `YetAnotherConfigLib.createBuilder()` → категории → `ScreenBuilder`;
открывать из нашей панели кнопкой «Настройки (YACL)». Наш `ElytrixConfig` при этом
остаётся источником данных — контролы просто читают/пишут его поля.

---

## 5. Чего не делаем

- не тянем две конфиг-библиотеки сразу (YACL + Cloth) — разные стили и +2 МБ;
- не пишем UI на Kotlin/Compose — Compose-рантайма для MC 26.2 нет;
- не возвращаем `mappings`/yarn-имена — 26.x без обфускации (см. README §0);
- не строим панель на `imgui-java`/втором окне — ломает ввод и не дружит с blaze3d.

---

## 6. Что сделано и что дальше

1. ✅ Свой мини-кит `ui/kit`: тема, скругления, тени, градиенты, иконки (рисуются кодом),
   кнопки (4 стиля), тумблеры, слайдеры, выпадающие списки, карточки-разделы, «пустые состояния».
2. ✅ Панель `ui/ElytrixScreen.java` на этом ките: сайдбар с иконками и 5 разделами
   (Главная / Боты / Прокси / Консоль / Настройки), шапка с заголовком и кнопкой закрытия,
   скролл с полосой, анимации появления/закрытия, клик вне панели, Esc и правый Ctrl.
3. ✅ Экран загрузки `ui/ElytrixLoader.java` + `mixin/client/LoadingOverlayMixin.java` — показывается **всё время загрузки** (ванильный красный лоадер отменён); в главном меню — свой фон `ElytrixBackground` и своя «шапка» `ElytrixBrand` вместо логотипа MC
   (логотип, кнопка и сплэши из ресурспака удалены).
4. ✅ Настройки интерфейса работают и сохраняются: акцент (5 пресетов), прозрачность панели,
   плавные анимации, размытие фона, закрытие кликом вне панели, горячая клавиша.
5. ⬜ Наполнение разделов: боты (BotMark + SoulFire и другие репозитории → один список),
   прокси, живая консоль — это уже логика, а не UI.
6. ⬜ Тултипы к элементам, сводка «подключено / отвалилось», HUD-оверлей, график TPS/MSPT.

### Если понадобится готовая библиотека на будущее

Из проверенного под 26.2 живёт только YACL (`3.9.7+26.2-fabric`, Jar-in-Jar); Cloth Config —
ветка `v26.2`, но это снова ванильный вид. MoulConfig (`modern/26.2`) тянет Fabric Language
Kotlin. owo-lib / LibGui / Vigilance / ModernUI под 26.2 не обновлены (см. §1).
