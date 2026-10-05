package platform.client.ui.screen;

import platform.client.utils.render.DeltaBlurProcessor;
import platform.client.utils.render.ScaleUtil;
import platform.client.utils.render.pipeline.DeltaRenderUtil;
import platform.api.system.interfaces.NativeMethodLookup;
import static platform.api.module.Interface.aM_;
import platform.client.Delta;
import platform.api.module.Interface;
import platform.api.module.Category;
import platform.api.module.Module;
import platform.client.utils.render.EasingList;
import platform.client.utils.render.Fonts;
import platform.client.utils.render.ColorUtil;
import platform.client.utils.math.MathUtil;

import platform.client.ui.screen.GUIPanel;

import platform.client.utils.render.AnimationUtil;
import platform.client.ui.element.TextField;
import platform.api.annotation.Compile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import lombok.Generated;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class GUIScreen extends Screen {
    private final TextField a;
    private final AnimationUtil b;
    private final List<GUIPanel> c;
    private String d;

    @Compile
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        DeltaBlurProcessor.setStrength(1.0f);
        DeltaRenderUtil.setShadowStrength(1.0f);
        DeltaRenderUtil.beginFrame();
        super.extractRenderState(context, mouseX, mouseY, delta);
        double dA = MathUtil.scale(mouseX, 2);
        double dA2 = MathUtil.scale(mouseY, 2);
        ScaleUtil.a(context, 2);
        double dSum = this.c.stream().mapToDouble(new ToDoubleFunction() {
            @Override
            public double applyAsDouble(Object obj) {
                return ((GUIPanel) obj).f().z;
            }
        }).sum();
        float size = (this.c.size() - 1) * 8.0f;
        Minecraft class_310Var = Interface.aM_;
        int iMethod_4486 = class_310Var.getWindow().getGuiScaledWidth();
        float f = size + ((float) dSum);
        float f2 = (iMethod_4486 - f) * 0.5f;
        float f3 = f2;
        float f4 = 0.0f;
        for (final GUIPanel gUIPanel : this.c) {
            Vector4f vector4fF = gUIPanel.f();
            gUIPanel.a(Arrays.stream(Delta.h().d().t().e()).filter(obj -> this.a(gUIPanel, (Module) obj)).sorted(Comparator.comparing(new Function() {
                @Override
                public Object apply(Object obj) {
                    return ((Module) obj).j();
                }
            }, String.CASE_INSENSITIVE_ORDER)).toList());
            vector4fF.x = f3;
            vector4fF.y = (class_310Var.getWindow().getGuiScaledHeight() - vector4fF.w) * 0.5f;
            f4 = vector4fF.y;
            gUIPanel.a(context, (int) dA, (int) dA2, delta);
            f3 += vector4fF.z + 8.0f;
        }
        Iterator<GUIPanel> it = this.c.iterator();
        while (it.hasNext()) {
            it.next().a(context, dA, dA2, delta);
        }
        float f5 = ((GUIPanel) this.c.getFirst()).f().w;
        float fC = ((GUIPanel) this.c.getFirst()).b().c();
        float fEase = EasingList.s.ease(fC);
        context.pose().pushMatrix();
        float fEase2 = EasingList.p.ease(fC);
        float f6 = (0.5f * f) + f2;
        float f7 = f5 + f4;
        float f8 = 12.0f + f7 + 10.0f;
        float f9 = ((1.0f - fEase2) * 14.0f) + f8;
        float f10 = (0.15f * fEase) + 0.85f;
        context.pose().translate(f6, f9);
        context.pose().scale(f10, f10);
        context.pose().translate(-f6, -f8);
        a(context, f6, f7, (int) dA, (int) dA2, delta);
        context.pose().popMatrix();
        a(context, f6, f4, delta);
        ScaleUtil.a(context);
        DeltaRenderUtil.flush(context);
    }

    @Compile
    public boolean mouseClicked(MouseButtonEvent event, boolean held) {
        double dA = MathUtil.scale(event.x(), 2);
        double dA2 = MathUtil.scale(event.y(), 2);
        int button = event.button();
        this.a.a(dA, dA2, button);
        if (this.c.stream().filter(obj -> GUIScreen.f((GUIPanel) obj)).anyMatch(obj -> ((GUIPanel) obj).a(dA, dA2, button))) {
            return true;
        }
        return super.mouseClicked(event, held);
    }

    @Compile
    public boolean mouseReleased(MouseButtonEvent event) {
        double dA = MathUtil.scale(event.x(), 2);
        double dA2 = MathUtil.scale(event.y(), 2);
        int button = event.button();
        if (this.c.stream().filter(obj -> GUIScreen.e((GUIPanel) obj)).anyMatch(obj -> ((GUIPanel) obj).b(dA, dA2, button))) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Compile
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        double dA = MathUtil.scale(event.x(), 2);
        double dA2 = MathUtil.scale(event.y(), 2);
        int button = event.button();
        this.a.b(dA, dA2, button);
        if (this.c.stream().filter(obj -> GUIScreen.d((GUIPanel) obj)).anyMatch(obj -> ((GUIPanel) obj).a(dA, dA2, button, MathUtil.scale(deltaX, 2), MathUtil.scale(deltaY, 2)))) {
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Compile
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double scaledX = MathUtil.scale(mouseX, 2);
        double scaledY = MathUtil.scale(mouseY, 2);
        for (GUIPanel panel : this.c) {
            if (panel.d() == null) {
                continue;
            }
            Vector4f bounds = panel.f();
            if (MathUtil.a(scaledX, scaledY, bounds.x, bounds.y, bounds.z, bounds.w)) {
                return panel.a(scaledX, scaledY, verticalAmount);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Compile
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        int modifiers = event.modifiers();
        if (keyCode == 70 && (modifiers & 2) != 0) {
            this.a.a(!this.a.j());
            return true;
        }
        if (this.a.j()) {
            this.a.a(keyCode, 0, modifiers);
            return true;
        }
        if (this.c.stream().filter(obj -> GUIScreen.b((GUIPanel) obj)).anyMatch(obj -> ((GUIPanel) obj).a(keyCode, 0, modifiers))) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Compile
    public boolean charTyped(CharacterEvent event) {
        char character = (char) event.codepoint();
        if (this.a.j()) {
            this.a.a(character, 0);
            return true;
        }
        if (this.c.stream().filter(obj -> GUIScreen.a((GUIPanel) obj)).anyMatch(obj -> ((GUIPanel) obj).a(character, 0))) {
            return true;
        }
        return super.charTyped(event);
    }

    static {
        NativeMethodLookup.lookup(GUIScreen.class, 5);
    }

    @Generated
    public TextField a() {
        return this.a;
    }

    @Generated
    public AnimationUtil b() {
        return this.b;
    }

    @Generated
    public List<GUIPanel> c() {
        return this.c;
    }

    @Generated
    public String d() {
        return this.d;
    }

    public GUIScreen(Component title) {
        super(title);
        this.a = new TextField(TextField.a.GUI);
        this.b = new AnimationUtil();
        this.c = new ArrayList();
        for (Category category : Category.values()) {
            this.c.add(new GUIPanel(category));
        }
        this.a.a("Поиск по модулям");
    }

    public void onClose() {
        super.onClose();
        this.c.forEach(panel -> {
            panel.b().c(0.0f);
        });
    }

    @Override
    public void removed() {
        DeltaBlurProcessor.setStrength(1.0f);
        DeltaRenderUtil.setShadowStrength(1.0f);
        super.removed();
    }

    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    public boolean a(GUIPanel panel, Module module) {
        return module.l() == panel.c() && module.j().toLowerCase().contains(this.a.g().toString().toLowerCase());
    }

    private void a(GuiGraphicsExtractor context, float centerX, float panelBottom, int mouseX, int mouseY, float delta) {
        this.a.b(new Vector2f(100.0f, 20.0f));
        this.a.a(new Vector2f(centerX - 50.0f, panelBottom + 12.0f));
        this.a.a(context, mouseX, mouseY, delta, 1.0f);
    }

    private void a(GuiGraphicsExtractor context, float centerX, float panelTop, float delta) {
        Delta.h().d().o();
        Module hovered = (Module) Arrays.stream(this.c.toArray()).map((v0) -> {
            return ((GUIPanel) v0).e();
        }).filter(module -> {
            return (module == null || module.k() == null || module.k().isEmpty()) ? false : true;
        }).findFirst().orElse(null);
        if (hovered != null && !hovered.k().equals(this.d)) {
            this.d = hovered.k();
            this.b.c(0.0f);
        }
        this.b.a(0.0f, 1.0f, 0.3f, EasingList.i, delta);
        this.b.a(hovered != null);
        float fade = EasingList.p.ease(this.b.c());
        if (fade > 0.0f && this.d != null) {
            float x = centerX - (Fonts.c.a(this.d, 10.0f) / 2.0f);
            float y = ((panelTop - Fonts.c.a(10.0f)) - 8.0f) + ((1.0f - fade) * 4.0f);
            Fonts.c.a(context, this.d, x + 0.5f, y + 0.5f, 10.0f, ColorUtil.a(ColorUtil.a(0, 0, 0, 255), 0.5f * fade));
            Fonts.c.a(context, this.d, x, y, 10.0f, ColorUtil.a(ColorUtil.a(255, 255, 255, 255), fade));
        }
    }

    public static boolean f(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean e(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean d(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean c(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean b(GUIPanel panel) {
        return panel.d() != null;
    }

    public static boolean a(GUIPanel panel) {
        return panel.d() != null;
    }
}





