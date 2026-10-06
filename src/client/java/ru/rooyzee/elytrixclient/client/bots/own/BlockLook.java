package ru.rooyzee.elytrixclient.client.bots.own;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Справочник блоков через СОБСТВЕННЫЕ классы игры (как SoulFire, который сидит
 * внутри настоящего клиента): state-id из палитры чанка -> BlockState клиента.
 * Никаких своих NBT-парсеров реестра: игра и так знает все блоки своей версии.
 */
public final class BlockLook {

    private BlockLook() {
    }

    /** Твёрдый ли блок (воздух/трава/факел/вода — нет). */
    public static boolean solid(int stateId) {
        try {
            BlockState st = Block.stateById(stateId);
            return st != null && !st.isAir() && st.canOcclude();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Цвет блока — настоящая карта цветов Minecraft (MapColor). */
    public static int color(int stateId) {
        try {
            BlockState st = Block.stateById(stateId);
            if (st == null || st.isAir()) {
                return 0;
            }
            return 0xFF000000 | (st.getBlock().defaultMapColor().col & 0xFFFFFF);
        } catch (Throwable t) {
            return 0xFF7E858D;
        }
    }
}
