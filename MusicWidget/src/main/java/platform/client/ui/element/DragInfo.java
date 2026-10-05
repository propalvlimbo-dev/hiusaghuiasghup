package platform.client.ui.element;

import static platform.api.module.Interface.aM_;
import platform.client.Xivivide;
import platform.client.utils.math.MathUtil;
import platform.api.module.Interface;
import platform.client.ui.widget.Widget;
import lombok.Generated;

public class DragInfo implements Interface {
    private Widget b;
    private float c;
    private float d;
    private float e;
    private float f;
    private final String i;
    private double g = 0.0d;
    private double h = 0.0d;
    private int j = 0;
    private final DragSpring k;
    private final DragSpring l;
    private boolean savedPosition;
    /** Render scale of the widget; e/f are the unscaled (local) size, f()/g() return the on-screen size. */
    private float scale = 1.0f;

    public boolean hasSavedPosition() { return this.savedPosition; }
    public void markPositionSaved() { this.savedPosition = true; }

    @Generated
    public void a(Widget widget) { this.b = widget; }

    public void a(float x) { this.c = x; this.k.a(x); }

    public void b(float y) { this.d = y; this.l.a(y); }

    @Generated
    public void c(float width) { this.e = width; }

    @Generated
    public void d(float height) { this.f = height; }

    @Generated
    public void a(double offsetX) { this.g = offsetX; }

    @Generated
    public void b(double offsetY) { this.h = offsetY; }

    @Generated
    public void a(int status) { this.j = status; }

    @Generated
    public Widget e() { return this.b; }

    @Generated
    public float f() { return this.e * this.scale; }

    @Generated
    public float g() { return this.f * this.scale; }

    @Generated
    public double h() { return this.g; }

    @Generated
    public double i() { return this.h; }

    @Generated
    public String j() { return this.i; }

    @Generated
    public int k() { return this.j; }

    public DragInfo(String name, float x, float y, float width, float height) {
        this.i = name;
        this.c = x;
        this.d = y;
        this.e = width;
        this.f = height;
        this.k = new DragSpring(x);
        this.l = new DragSpring(y);
        Xivivide.h().d().s().e().add(this);
    }

    public void a(long now) {
        this.k.c(now);
        this.l.c(now);
    }

    public void b(long now) {
        this.k.b(this.c);
        this.l.b(this.d);
    }

    public float a() {
        return MathUtil.b(this.k.a(), 0.0f, Math.max(0.0f, aM_.getWindow().getGuiScaledWidth() - f()));
    }

    public float b() {
        return MathUtil.b(this.l.a(), 0.0f, Math.max(0.0f, aM_.getWindow().getGuiScaledHeight() - g()));
    }

    /** Unscaled width, the size the widget lays itself out in. */
    public float lw() { return this.e; }

    /** Unscaled height. */
    public float lh() { return this.f; }

    public float scale() { return this.scale; }

    public void setScale(float scale) { this.scale = scale; }

    public float c() { return this.c; }

    public float d() { return this.d; }
}



