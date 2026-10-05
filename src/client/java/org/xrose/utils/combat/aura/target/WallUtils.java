package org.xrose.utils.combat.aura.target;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WallUtils {
   private WallUtils() {
   }

   public static boolean isPassthrough(BlockHitResult result) {
      if (result == null) {
         return true;
      }

      Minecraft client = Minecraft.getInstance();
      if (client.level == null) {
         return true;
      }

      BlockState state = client.level.getBlockState(result.getBlockPos());
      return state.isAir()
         ? true
         : state.getBlock() instanceof StairBlock
            || state.getBlock() instanceof DoorBlock
            || state.getBlock() instanceof TrapDoorBlock
            || state.getBlock() instanceof GrassBlock
            || state.getBlock() == Blocks.KELP
            || state.getBlock() == Blocks.TALL_SEAGRASS
            || state.getBlock() == Blocks.TALL_GRASS
            || state.getBlock() == Blocks.COBWEB;
   }
}

