package org.xrose.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.xrose.command.ClientCommand;
import org.xrose.utils.FriendManager;
import org.xrose.utils.StaffManager;
import org.xrose.utils.text.ChatUtil;

public final class StaffCommand extends ClientCommand {
   public StaffCommand() {
      super("staff", "Manages the staff list", ":shield:");
   }

   @Override
   public void build(LiteralArgumentBuilder<Object> builder) {
      builder.executes(context -> this.showUsage());
      builder.then(
         LiteralArgumentBuilder.literal("add")
            .then(
               RequiredArgumentBuilder.argument("name", StringArgumentType.word())
                  .suggests((context, suggestions) -> this.suggestOnlinePlayers(suggestions))
                  .executes(this::add)
            )
      );
      builder.then(this.staffRemoval("delete"));
      builder.then(this.staffRemoval("remove"));
      builder.then(LiteralArgumentBuilder.literal("list").executes(context -> this.list()));
      builder.then(LiteralArgumentBuilder.literal("clear").executes(context -> this.clear()));
   }

   private int showUsage() {
      ChatUtil.usage("staff add <name>  •  delete <name>  •  list  •  clear");
      return 1;
   }

   private int add(CommandContext<Object> context) {
      String name = StringArgumentType.getString(context, "name");
      if (!FriendManager.isValidName(name)) {
         ChatUtil.error("Invalid player name  •  " + name);
         return 0;
      } else if (!StaffManager.INSTANCE.add(name)) {
         ChatUtil.error(name + " is already on the staff list");
         return 0;
      } else {
         ChatUtil.success(name + " added to the staff list");
         return 1;
      }
   }

   private int remove(CommandContext<Object> context) {
      String name = StringArgumentType.getString(context, "name");
      if (!StaffManager.INSTANCE.remove(name)) {
         ChatUtil.error("Staff member not found  •  " + name);
         return 0;
      } else {
         ChatUtil.success(name + " removed from the staff list");
         return 1;
      }
   }

   private int list() {
      Collection<String> staff = StaffManager.INSTANCE.getStaff();
      if (staff.isEmpty()) {
         ChatUtil.info("Staff list is empty");
         return 1;
      } else {
         ChatUtil.header("Staff  •  " + staff.size());
         staff.forEach(name -> ChatUtil.entry(":shield:", name, null));
         return 1;
      }
   }

   private int clear() {
      if (!StaffManager.INSTANCE.clear()) {
         ChatUtil.info("Staff list is already empty");
         return 1;
      } else {
         ChatUtil.success("Staff list cleared");
         return 1;
      }
   }

   private LiteralArgumentBuilder<Object> staffRemoval(String literal) {
      return (LiteralArgumentBuilder<Object>)LiteralArgumentBuilder.literal(literal)
         .then(
            RequiredArgumentBuilder.argument("name", StringArgumentType.word())
               .suggests((context, suggestions) -> suggest(suggestions, StaffManager.INSTANCE.getStaff()))
               .executes(this::remove)
         );
   }

   private CompletableFuture<Suggestions> suggestOnlinePlayers(SuggestionsBuilder builder) {
      Minecraft minecraft = Minecraft.getInstance();
      return minecraft.getConnection() == null
         ? builder.buildFuture()
         : suggest(
            builder,
            minecraft.getConnection()
               .getOnlinePlayers()
               .stream()
               .map(info -> info.getProfile().name())
               .filter(name -> !StaffManager.INSTANCE.isStaff(name))
               .toList()
         );
   }

   private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Iterable<String> values) {
      String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

      for (String value : values) {
         if (value.toLowerCase(Locale.ROOT).startsWith(remaining)) {
            builder.suggest(value);
         }
      }

      return builder.buildFuture();
   }
}

