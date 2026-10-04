# drafts/ui-vector — черновик векторного слоя рендера (в сборку не входит)

Лежит вне `src/`, чтобы Gradle его не компилировал: сначала согласуем подход, потом переносим в
`src/client/java/ru/rooyzee/elytrixclient/`.

* `UiVector.java` → должен лежать в `client/ui/kit/gfx/UiVector.java` (пакет поменять на
  `ru.rooyzee.elytrixclient.client.ui.kit.gfx`).
* `GuiGraphicsExtractorAccessor.java` → в `mixin/client/` + добавить строку
  `"GuiGraphicsExtractorAccessor"` в `src/client/resources/elytrixclient.client.mixins.json`.

Что даёт: скругления, кольца, тени/свечения и 4-цветные градиенты как настоящая геометрия
(`GuiElementRenderState` + `GuiRenderState.addGuiElement`) вместо спрайтов под каждый GUI Scale.
Координаты — `float`, поэтому форма гладкая на любом масштабе и любом размере панели.

Проверено по официальным исходникам 26.2: `GuiGraphicsExtractor.guiRenderState` (private, берём
`@Accessor`), `GuiRenderState.addGuiElement`, `GuiElementRenderState` (`buildVertices/pipeline/
textureSetup/scissorArea/bounds`), `VertexConsumer.addVertexWith2DPose(...).setColor(...)`,
`ScreenRectangle.transformMaxBounds/intersection/empty`, `TextureSetup.noTexture/singleTexture`,
`RenderPipelines.GUI` (важно: `VertexFormat.Mode.QUADS` — геометрия четвёрками вершин).
