# Разбор UI загруженного клиента (`UUII/`)

Материал: папка `UUII/` в этом репозитории — 70 файлов, 424 КБ: `UUII/ui/**` (22 файла) и `UUII/mixin/**` (48 файлов).
Пакет — `wtf.expensive.client`, имена — Mojmap, сигнатуры совпадают с MC 26.2 (`GuiGraphicsExtractor`,
`extractRenderState`, `MouseButtonEvent`, `Identifier`, `FontDescription`). Похоже на декомпилированные
исходники стороннего клиента: утилиты (`RenderUtil`, `ColorUtil`, `AnimationMath`, `Fonts`,
`StyledFontRenderer`, `Managment`) в выгрузку не попали, поэтому «снизу» видно не всё — но слой рендера
и слой интерфейса видно полностью, а это главное.

---

## 1. Как устроен их UI-стек (главное открытие)

Они **не рисуют скругления спрайтами и не собирают их из прямоугольников**. У них есть свой слой
«векторных» фигур, который кладётся в тот же список элементов кадра, что и ванильная заливка:

```
Screen.extractRenderState(GuiGraphicsExtractor, int, int, float)
        │
        ├─ graphics.fill/fillGradient/text/blit          ← ванильные примитивы
        │
        └─ ThemeRenderUtil.gradientOutline/gradientRounded/gradientTexture
                    │
                    └─ ((GuiGraphicsExtractorAccessor) graphics).expensive$guiRenderState()
                              .addGuiElement(new GradientRoundedState(...))
                                        │
                                        └─ implements GuiElementRenderState
                                             buildVertices(VertexConsumer)
                                             pipeline()  → RenderPipelines.GUI
                                             textureSetup() → TextureSetup.noTexture()
                                             scissorArea() / bounds()
```

Файл-образец: `UUII/ui/clickgui/theme/ThemeRenderUtil.java` (13 КБ) — там три состояния: кольцо с
градиентом по углам (`GradientOutlineState`), скруглённый прямоугольник с 4-цветным градиентом
(`GradientRoundedState`) и текстура с градиентом (`GradientTextureState`).

Как это работает в 26.2 (я сверил с официальным деревом `26.2-mcp`):

| Что | Сигнатура в 26.2 | Комментарий |
|---|---|---|
| Доступ к списку кадра | поле `GuiGraphicsExtractor.guiRenderState` (private) | берётся `@Accessor`-миксином — `UUII/mixin/GuiGraphicsExtractorAccessor.java` |
| Добавить элемент | `GuiRenderState.addGuiElement(GuiElementRenderState)` | публичный |
| Интерфейс элемента | `buildVertices(VertexConsumer)`, `pipeline()`, `textureSetup()`, `scissorArea()`, `bounds()` | `GuiElementRenderState extends ScreenArea` |
| Вершина | `VertexConsumer.addVertexWith2DPose(Matrix3x2fc pose, float x, float y).setColor(argb)` | `setUv(u,v)` — если текстура |
| Матрица | `new Matrix3x2f(graphics.pose())` | `Matrix3x2fStack extends Matrix3x2f` |
| Обрезка | `new ScreenRectangle(x,y,w,h).transformMaxBounds(pose)` + `intersection(...)` | передаётся в состояние |
| Формат | `RenderPipelines.GUI` → `POSITION_COLOR`, **`VertexFormat.Mode.QUADS`** | важный нюанс: геометрия обязана быть четвёрками вершин |
| Текстура | `TextureSetup.singleTexture(view, sampler)` / `noTexture()` | `AbstractTexture.getTextureView()/getSampler()` |

Что это даёт на практике:

* форма считается в `float`-координатах — скругление остаётся гладким при любом GUI Scale, без набора
  спрайтов под каждый масштаб;
* 4-цветный градиент (билинейный) из коробки — то, из чего складываются «стеклянные» панели;
* всё, что без текстуры и с одним пайплайном, попадает в **один меш** (`GuiRenderer.addElementToMesh`
  группирует по `pipeline()` + `textureSetup()`), то есть десятки скруглений = один draw call. Дешевле,
  чем наборы спрайтов;
* ту же технику можно применить к кольцам, теням и «свечениям», а не только к скруглениям.

## 2. ClickGUI: раскладка и виджеты

`UUII/ui/clickgui/ClickGui.java` (45 КБ) — фиксированная панель `450×350`, сайдбар `190`… точнее
`WIDTH - BAR = 350` под контент и `BAR = 100` под навигацию:

* **Шапка сайдбара**: логотип-иконка шрифтом иконок + поиск (`EditBox`-подобный ручной ввод: строка +
  мигающий `_`, обрезка через `scissor`).
* **Категории**: иконка + название, у активной — подложка с градиентом акцента и `shadow` при включённом
  `glow`; анимация перехода `anim` на элемент (`AnimationMath.fast(anim, active ? 1 : 0, 10f)`).
* **Профиль** внизу сайдбара: `PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), x, y, size)`
  — голова игрока прямо из скина, без картинок.
* **Контент**: две колонки карточек (`column(0)` / `column(1)` — чётность индекса модуля), каждая карточка
  шириной 160, шаг `height + 5`, обрезка `scissor(панель)`, вертикальный скролл статическим полем
  `scrolling` + отдельная анимированная `scrollingOut` (плавная прокрутка).
* **Спец-вкладки**: `Theme` (сетка карточек-превью тем + кастомный HSV-пикер), `Scripts` (список файлов),
  `Configs` (сохранение/загрузка/удаление) — отдельными рендерами.

Виджеты (`ui/clickgui/object/`): `ModuleObject` — карточка модуля (шапка `HEADER = 22`, тумблер по ЛКМ,
биндинг по СКМ, «привязка клавиши»), внутри — `List<SettingObject>`, где `SettingObject` абстрактный
(позиция, `height`, `render`, `mouseClicked/Released`, `keyPressed`, `charTyped`, `isCapturingInput`), а
конкретики: `BooleanObject`, `SliderObject`, `ModeObject`, `MultiObject`, `BindObject`, `ColorObject`,
`TextObject`. То есть обычная объектная модель «модуль → настройки → виджет», где виджет сам знает, как
себя рисовать и как реагировать на ввод.

Мелочи, которые стоит перенять:

* у каждого виджета есть `isCapturingInput()` — пока кто-то ловит ввод, экран не обрабатывает Esc/клавиши;
* прокрутка анимируется отдельным значением, а не «прыгает»;
* скроллбар и содержимое обрезаются одним `scissor`-прямоугольником;
* `ClickGui.isInGameUi() = true` и `extractTransparentBackground(...)` + `graphics.blurBeforeThisStratum()`
  — панель поверх размытой игры, а не поверх чёрного скрима.

## 3. Главное меню

`UUII/mixin/TitleScreenMixin.java` + `UUII/ui/menu/MainMenuOverlay.java`:

* `init()` → `clearWidgets()`: ванильные кнопки удаляются целиком;
* `extractRenderState` → `ci.cancel()` и отрисовка своего оверлея: фоновая картинка на весь экран,
  свой логотип, 5 кнопок `176×34` с шагом 39, ховер-анимация `AnimationMath.lerp(anim, hovered?1:0, 10)`:
  подложка `rgba(19,21,27, 90..150)`, при ховере `shadow`, рамка `roundedOutline` цветом между
  `#353851` и `#93A4C4`, текст между серым и белым;
* `extractBackground` → `ci.cancel()` (свой фон вместо панорамы);
* `mouseClicked` → свой обработчик возвращает `true`, открывая `SelectWorldScreen`, `JoinMultiplayerScreen`,
  `AltManagerScreen`, `OptionsScreen(parent, options, false)`, `mc.stop()`;
* есть флаг «legit-режим», при котором клиент вообще не вмешивается (полезно, чтобы скрыть следы).

## 4. Экран загрузки

`UUII/mixin/LoadingOverlayMixin.java` + `LoadingOverlayAccessor`:

* `@Inject(method = "extractRenderState", at = HEAD, cancellable = true)` — ванильный лоадер заменяется
  полностью, `ci.cancel()`;
* свою картинку они **регистрируют вручную** через `minecraft.getTextureManager().registerAndLoad(id, new
  ReloadableTexture(id) { loadContents(...) → NativeImage.read(stream) })` — потому что обычная загрузка
  ресурсов на этом этапе ещё не готова. Полезный приём, если свой фон загрузки не находится;
* текст-фразы со сменой по таймеру (`BetterText` c интервалом 800 мс);
* аккуратный выход: считают `fadeOutStart` и по окончании делают `minecraft.gui.setOverlay(null)` — то есть
  ванильный красный прогрессбар не успевает появиться;
* у нас это уже сделано иначе (свой прогресс + `gui.setOverlay(null)`), но идею «своя картинка + фраза»
  можно взять, если захочется оживить экран.

## 5. HUD

`UUII/ui/hud/HudRenderer.java` реализует их `HudElement` и рисует: watermark (свой текст со сменой),
`KeyBinds`, `StaffList`, `TargetHUD`, `Timer`. Каждый блок — «перетаскиваемый»:
`DragManager.create("TargetHUD-new", 10, 300)` — у каждого элемента свой id, позиция сохраняется между
запусками. Внутри — `updateFunctions()` (список включённых модулей) и `updateStaff()` (по никам на сервере,
регуляркой по префиксам). Внешний HUD-слой — целиком их код, нам сейчас не нужен, но `DragManager`
пригодится, если будем делать настройку панели мышью.

`UUII/ui/visual/VisualRenderer.java` — ESP/стрелки/маркеры блоков. Это уже читерская часть, к UI меню
отношения не имеет.

## 6. Остальные миксины (что из списка полезно нам)

| Миксин | Что делает | Нам |
|---|---|---|
| `GuiGraphicsExtractorAccessor` | доступ к `guiRenderState` | **берём** — основа векторного рендера |
| `BlurStrengthMixin` | `@Inject(method = "getMenuBackgroundBlurriness", HEAD, cancellable)` в `Options`: пока открыт их GUI, подменяет силу размытия на значение из настроек | **берём** — у нас блюр зависит от ванильной настройки |
| `TitleScreenMixin` | своё главное меню | уже есть у нас |
| `LoadingOverlayMixin` + `LoadingOverlayAccessor` | свой экран загрузки + доступ к `fadeIn/fadeOut` | уже есть (без accessor) |
| `JoinMultiplayerScreenMixin` | доп. кнопки на экране серверов | пригодится для прокси-профилей |
| `ChatScreenMixin` / `ChatComponentMixin` | правки чата | не сейчас |
| `HudVisualMixin` | отключение ванильных `extractBossOverlay/ScoreboardSidebar/Title/TabList` | если будет свой HUD |
| `ComponentMixin` / `GuiGraphicsTextMixin` | подмена текста (NameProtect) | нам не нужно |
| `ConnectionProxyMixin` / `ConnectionPacketMixin` / `ClientPacketListenerMixin` | прокси и пакеты | у нас прокси-инфраструктура своя (боты), смотреть позже |
| Боевые (`SilentRotationRender`, `KeepSprint`, `NoPush`, `MovementEvent`, `Strafe`, `Jump`…) | читы | **не берём** |

## 7. Что из этого уже есть у нас

| Их слой | Наш аналог | Состояние |
|---|---|---|
| `RenderUtil.roundedRect/gradientOutline/shadow/scissor/isHovered` | `ui/kit/UiDraw` | есть, но на спрайтах |
| `StyledFontRenderer` + `Fonts.SEMIBOLD_15` и т.п. | `ui/kit/UiText` + свои TTF (`inter.ttf`, `mono.ttf`) | есть, даже ближе к «Apple» |
| `AnimationMath.fast/lerp` | `UiWidget` (appear/hoverT/pressT, ease) | есть |
| `Style`/`StyleManager` (30 тем с градиентами) | `UiTheme` (2 пресета + акценты) | есть, но проще |
| `ModuleObject`/`SettingObject` + 7 виджетов | `UiSection/UiWidget/UiButton/UiDropdown/UiSlider/UiToggle` | **нет дерева «модуль → настройки»** |
| `ThemeRenderUtil` (вектор) | — | **нет** (черновик в `drafts/ui-vector/`) |
| `ModuleObject.isCapturingInput`, `ClickGui.isInGameUi`/`extractTransparentBackground` | частично | нет |

Наш проект — не чит-клиент: у нас вкладки «Главная / Боты / Прокси / Консоль / Настройки», и
перетаскивать их объектную модель модулей целиком смысла нет. Но **каркас** оттуда подходит один в один:
карточка = шапка + список виджетов, виджет сам рисуется и сам обрабатывает ввод, панель обрезается
scissor'ом, прокрутка анимируется, ввод «кто-то поймал» блокирует горячие клавиши.

## 8. Что я предлагаю забрать (по приоритету)

1. **Векторное ядро рендера** (`drafts/ui-vector/UiVector.java` + `GuiGraphicsExtractorAccessor`).
   Это прямое лечение «пиксельности»: никаких наборов спрайтов под GUI Scale, гладкие скругления/кольца/
   градиенты, один draw call на все фигуры. Уже написано (не подключено к сборке, чтобы не рисковать).
2. **Размытие под панелью** (`BlurStrengthMixin`-приём + `extractTransparentBackground` + `isInGameUi`) —
   «стеклянная» панель как на референсе.
3. **Виджет-каркас** для вкладок: `UiWidget` уже есть, добавить `isCapturingInput()` и каскадные
   `visible()`-условия, чтобы строить контент «модуль → настройки».
4. **Прокрутка с анимацией** (их `scrolling` + `scrollingOut`) — вместо мгновенной.
5. **Голова игрока** (`PlayerFaceExtractor`) — для вкладки ботов/аккаунтов: живая голова без картинок.
6. **Прокси-экран с `EditBox`** (`UUII/ui/proxy/ProxyScreen.java`) — как образец формы ввода для нашей
   вкладки «Прокси» (у них поля-`EditBox` рисуются поверх своего фонa).

## 9. Правовая заметка

`UUII/` — код стороннего клиента (декомпилированный), не наш. В публичном репозитории ему делать нечего:
если автор кода захочет — он легко найдёт его здесь. Предлагаю после разбора папку удалить (или держать
локально `.gitignore`-нутой). Переносить оттуда код строками я не буду: берём **идеи и архитектуру**,
реализацию пишем свою под `ru.rooyzee.elytrixclient`.

## 10. План

| Этап | Что делаем | Риск сборки |
|---|---|---|
| 1 | Подключить `drafts/ui-vector` → `UiVector` + accessor-миксин; перевести `UiDraw.roundRect/disc/ring/shadow/glow/progress/line` на вектор | средний (новый API, но всё сверено по 26.2) |
| 2 | Размытие под панелью + `extractTransparentBackground`/`isInGameUi` | низкий |
| 3 | Прокрутка с анимацией + `isCapturingInput` в `UiWidget` | низкий |
| 4 | Переверстать вкладки под карточки «модуль → настройки» (Боты/Прокси/Настройки) | средний |
| 5 | Вкладка «Консоль» аккуратным окном на `UiText.MONO` + голова игрока во вкладке ботов | низкий |
