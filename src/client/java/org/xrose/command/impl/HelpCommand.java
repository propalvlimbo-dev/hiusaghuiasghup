package org.xrose.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.xrose.command.ClientCommand;
import org.xrose.command.CommandManager;
import org.xrose.utils.text.ChatUtil;

public final class HelpCommand extends ClientCommand {
   private final CommandManager manager;

   public HelpCommand(CommandManager manager) {
      super("help", "Shows all available commands", ":question:");
      this.manager = manager;
   }

   @Override
   public void build(LiteralArgumentBuilder<Object> builder) {
      builder.executes(context -> {
         ChatUtil.header("Elytrix commands");

         for (ClientCommand command : this.manager.getCommands()) {
            ChatUtil.entry(command.emoji(), this.manager.getPrefix() + command.name(), command.description());
         }

         return 1;
      });
   }
}

