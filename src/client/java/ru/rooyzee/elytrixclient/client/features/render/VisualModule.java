package ru.rooyzee.elytrixclient.client.features.render;

import net.minecraft.client.Minecraft;

/**
 * Базовый класс визуального модуля — аналог delta-26.2 render-модулей.
 * Папка {@code features/render} и есть «Визуалы»: вся визуальная часть клиента.
 * Конкретный модуль переопределяет {@link #tick(Minecraft)} (логика в тике)
 * и/или {@link #renderWorld(Object)} (отрисовка в мире; контекст —
 * fabric {@code WorldRenderContext}, передаётся как Object, чтобы база не
 * зависела от rendering-api).
 */
public abstract class VisualModule {
    private final String name;
    private boolean enabled;

    protected VisualModule(String name, boolean defaultEnabled) {
        this.name = name;
        this.enabled = defaultEnabled;
    }

    public final String name() {
        return name;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final void setEnabled(boolean value) {
        if (this.enabled == value) {
            return;
        }
        this.enabled = value;
        if (value) {
            onEnable();
        } else {
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Логика каждый клиентский тик. */
    public void tick(Minecraft mc) {
    }

    /** Отрисовка в мире (fabric WorldRenderContext как Object). */
    public void renderWorld(Object ctx) {
    }

    /** Отрисовка в HUD-пространстве (GuiGraphicsExtractor + размеры экрана). */
    public void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor g, int sw, int sh) {
    }
}
