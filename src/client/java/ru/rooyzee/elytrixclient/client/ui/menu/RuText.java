package ru.rooyzee.elytrixclient.client.ui.menu;

import java.util.Map;

/** Перевод подписей xrose-настроек на русский. */
public final class RuText {

    private static final Map<String, String> RU = Map.ofEntries(
            // Chams
            Map.entry("Targets", "Цели"), Map.entry("Players", "Игроки"), Map.entry("Hostile", "Враждебные"),
            Map.entry("Passive", "Мирные"), Map.entry("Items", "Предметы"), Map.entry("Distance", "Дистанция"),
            Map.entry("Effect", "Эффект"), Map.entry("Shader Fill", "Шейдер-заливка"), Map.entry("Glow", "Свечение"),
            Map.entry("Solid", "Сплошной"), Map.entry("Glass", "Стекло"), Map.entry("Outline", "Контур"),
            Map.entry("Mode", "Режим"), Map.entry("External", "Внешний"), Map.entry("Internal", "Внутренний"),
            Map.entry("Both", "Оба"), Map.entry("Original Texture", "Ориг. текстура"), Map.entry("Shader", "Шейдер"),
            Map.entry("Plasma", "Плазма"), Map.entry("Nebula", "Туманность"),
            Map.entry("Shader Speed", "Скорость шейдера"), Map.entry("Visible Color", "Цвет"),
            Map.entry("Color Friends", "Красить друзей"), Map.entry("Friend Color", "Цвет друзей"),
            Map.entry("Opacity", "Прозрачность"), Map.entry("Outline Thickness", "Толщина контура"),
            Map.entry("Glow Radius", "Радиус свечения"), Map.entry("Glow Strength", "Сила свечения"),
            Map.entry("Additive Glow", "Аддитив. свечение"), Map.entry("Glass Blur", "Блюр стекла"),
            Map.entry("Mirror", "Зеркало"),
            // BlockOutline
            Map.entry("Aurora Color", "Цвет авроры"), Map.entry("Secondary Color", "Второй цвет"),
            Map.entry("Star Color", "Цвет звёзд"), Map.entry("Ignore Depth", "Игнор глубины"),
            Map.entry("Animation Speed", "Скорость анимации"), Map.entry("Transition Speed", "Скорость перехода"),
            Map.entry("Shader Intensity", "Интенсивность"),
            // Removals
            Map.entry("Overlay", "Оверлей"), Map.entry("Fire", "Огонь"), Map.entry("Bad Effects", "Плохие эффекты"),
            Map.entry("Shaking", "Тряска"), Map.entry("Totem Overlay", "Оверлей тотема"),
            Map.entry("Scoreboard", "Скорборд"), Map.entry("Boss Bar", "Босс-бар"), Map.entry("World", "Мир"),
            Map.entry("Weather", "Погода"), Map.entry("Totem Particles", "Частицы тотема"),
            Map.entry("Effects", "Эффекты"), Map.entry("Sounds", "Звуки"), Map.entry("Hit", "Удар"),
            Map.entry("Hurt", "Урон"), Map.entry("Totem", "Тотем"), Map.entry("Step", "Шаги"),
            Map.entry("Eat", "Еда"), Map.entry("Firework", "Фейерверк"), Map.entry("Warden", "Варден"),
            // AtmoDawnFog
            Map.entry("Dusk", "Сумерки"), Map.entry("Night", "Ночь"), Map.entry("Theme", "Тема"),
            Map.entry("Density", "Плотность"), Map.entry("Scatter Height", "Высота рассеивания"),
            Map.entry("God Rays", "Лучи света"), Map.entry("Softness", "Мягкость"),
            Map.entry("Sun Glow", "Свечение солнца"), Map.entry("Rainbow", "Радуга"),
            Map.entry("Rainbow Brightness", "Яркость радуги"), Map.entry("Rainbow Size", "Размер радуги"),
            Map.entry("Dawn Color", "Цвет рассвета"), Map.entry("Stars", "Звёзды"), Map.entry("Aurora", "Аврора"),
            // NameProtect
            Map.entry("Name", "Ник"), Map.entry("Replace", "Замена"), Map.entry("Blur", "Блюр"),
            // описания фич
            Map.entry("Replace selected entity models with shader silhouettes",
                    "Заменяет модели выбранных сущностей шейдерными силуэтами"),
            Map.entry("Night aurora shader over the selected block",
                    "Ночная аврора-подсветка выделенного блока"),
            Map.entry("Hides distracting overlays, world effects, particles, and sounds.",
                    "Скрывает отвлекающие оверлеи, эффекты мира, частицы и звуки"),
            Map.entry("Cinematic atmosphere: fog, god rays, glowing sun and starry nights",
                    "Кино-атмосфера: туман, лучи света, солнце и звёздные ночи"),
            Map.entry("Protects your nickname in client UI", "Защищает твой ник в интерфейсе клиента"));

    private RuText() {
    }

    public static String ru(String s) {
        if (s == null) {
            return "";
        }
        return RU.getOrDefault(s, s);
    }
}
