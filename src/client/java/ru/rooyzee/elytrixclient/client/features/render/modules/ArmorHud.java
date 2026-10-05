package ru.rooyzee.elytrixclient.client.features.render.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import ru.rooyzee.elytrixclient.client.features.render.VisualModule;
import ru.rooyzee.elytrixclient.client.ui.kit.gfx.UiVector;

/**
 * ArmorHud (порт delta-26.2 ArmorWidget): вертикальная полоска прочности
 * каждой вещи брони справа внизу. Чистый HUD-рендер на наших векторах.
 */
public final class ArmorHud extends VisualModule {
    private static final float BAR_W = 3f, BAR_H = 26f, GAP = 4f;

    public ArmorHud() {
        super("ArmorHud", false);
    }

    @Override
    public void renderHud(GuiGraphicsExtractor g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        float x = sw - 8f - (BAR_W + GAP) * 4f;
        float y = sh - BAR_H - 40f;
        int i = 0;
        for (ItemStack stack : mc.player.getArmorSlots()) {
            float bx = x + i * (BAR_W + GAP);
            UiVector.roundRect(g, bx, y, BAR_W, BAR_H, BAR_W / 2, 0x40FFFFFF);
            if (stack != null && !stack.isEmpty() && stack.isDamaged()) {
                float frac = 1f - (float) stack.getDamageValue() / (float) Math.max(1, stack.getMaxDamage());
                float bh = Math.max(1.5f, BAR_H * frac);
                UiVector.roundRect(g, bx, y + BAR_H - bh, BAR_W, bh, BAR_W / 2, color(frac));
            } else if (stack != null && !stack.isEmpty()) {
                UiVector.roundRect(g, bx, y, BAR_W, BAR_H, BAR_W / 2, color(1f));
            }
            i++;
            if (i >= 4) {
                break;
            }
        }
    }

    private static int color(float frac) {
        int r = (int) (255 * Math.min(1f, 2f * (1f - frac)));
        int gg = (int) (255 * Math.min(1f, 2f * frac));
        return 0xFF000000 | (r << 16) | (gg << 8) | 60;
    }
}
