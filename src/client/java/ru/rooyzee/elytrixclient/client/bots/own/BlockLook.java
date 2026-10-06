package ru.rooyzee.elytrixclient.client.bots.own;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Внешность блока для 3D-вида. Основной источник — реестр СЕРВЕРА (BlockRegistry),
 * т.к. бот говорит по протоколу сервера, а не клиента. Fallback (пока реестр не
 * пришёл) — классы клиента, лучше чем ничего.
 */
public final class BlockLook {

    private BlockLook() {
    }

    public static boolean solid(int stateId) {
        if (BlockRegistry.ready()) {
            return BlockRegistry.solid(stateId);
        }
        try {
            BlockState st = Block.stateById(stateId);
            return st != null && !st.isAir() && st.canOcclude();
        } catch (Throwable t) {
            return false;
        }
    }

    public static int color(int stateId, boolean topFace) {
        if (BlockRegistry.ready()) {
            int cls = BlockRegistry.colorClass(stateId);
            int base = switch (cls) {
                case 1 -> 0xFF6B4A2F;
                case 2 -> 0xFF3E6B2A;
                case 3 -> 0xFF6E5232;
                case 4 -> 0xFFC9B47C;
                case 5 -> 0xFFE8EEF2;
                case 6 -> 0xFFB3542A;
                case 7 -> 0xFF2E5FA3;
                default -> 0xFF7E858D;
            };
            if (cls == 1 && topFace) {
                base = 0xFF58893B;
            }
            return base;
        }
        try {
            BlockState st = Block.stateById(stateId);
            if (st == null || st.isAir()) {
                return 0xFF7E858D;
            }
            return 0xFF000000 | (st.getBlock().defaultMapColor().col & 0xFFFFFF);
        } catch (Throwable t) {
            return 0xFF7E858D;
        }
    }
}
