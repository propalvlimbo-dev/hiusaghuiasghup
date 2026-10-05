package org.xrose.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import org.xrose.command.ClientCommand;
import org.xrose.pve.economy.AhSellFlow;
import org.xrose.utils.text.ChatUtil;

public final class AhCommand extends ClientCommand {
   public AhCommand() {
      super("ah", "Auction House helpers", ":hammer_and_pick:");
   }

   @Override
   public void build(LiteralArgumentBuilder<Object> builder) {
      builder.executes(context -> this.showUsage());
      builder.then(
         LiteralArgumentBuilder.literal("sell")
            .then(RequiredArgumentBuilder.argument("percent", IntegerArgumentType.integer(1, 99)).executes(this::sellWithDiscount))
      );
   }

   private int showUsage() {
      ChatUtil.usage("ah sell <скидка%>  —  продать предмет в руке со скидкой от рыночной цены");
      return 1;
   }

   private int sellWithDiscount(CommandContext<Object> context) {
      int percent = IntegerArgumentType.getInteger(context, "percent");
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null || minecraft.getConnection() == null) {
         ChatUtil.error("Вы не подключены к серверу");
         return 0;
      } else if (AhSellFlow.INSTANCE.isActive()) {
         ChatUtil.error("Анализ цены уже выполняется");
         return 0;
      } else {
         return AhSellFlow.INSTANCE.start(minecraft.player, percent) ? 1 : 0;
      }
   }
}

