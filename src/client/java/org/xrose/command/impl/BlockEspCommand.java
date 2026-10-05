package org.xrose.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.xrose.command.ClientCommand;
import org.xrose.command.CommandManager;
import org.xrose.context.MinecraftContext;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.impl.visual.BlockEspFeature;
import org.xrose.utils.blockesp.BlockEspConfig;
import org.xrose.utils.text.ChatUtil;

public final class BlockEspCommand extends ClientCommand implements MinecraftContext {
   private static final int PAGE_SIZE = 8;
   private static final int ALL_BLOCKS_PAGE_SIZE = 15;
   private static final String DIVIDER = "------------------------------------------------";

   public BlockEspCommand() {
      super("blockesp", "Manage the Block ESP block list", ":bricks:");
   }

   @Override
   public List<String> aliases() {
      return List.of("besp");
   }

   @Override
   public void build(LiteralArgumentBuilder<Object> builder) {
      builder.executes(context -> this.showHelp());
      builder.then(
         LiteralArgumentBuilder.literal("add")
            .then(
               RequiredArgumentBuilder.argument("block", StringArgumentType.greedyString())
                  .suggests((context, suggestions) -> this.suggestBlocks(suggestions))
                  .executes(this::addBlock)
            )
      );
      builder.then(this.removal("remove"));
      builder.then(this.removal("del"));
      builder.then(this.removal("delete"));
      builder.then(LiteralArgumentBuilder.literal("clear").executes(context -> this.clearBlocks()));
      builder.then(
         ((LiteralArgumentBuilder)LiteralArgumentBuilder.literal("list").executes(context -> this.listSavedBlocks(1)))
            .then(RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1)).executes(context -> this.listSavedBlocks(this.page(context))))
      );
      builder.then(
         ((LiteralArgumentBuilder)LiteralArgumentBuilder.literal("blocks").executes(context -> this.listAllBlocks(1)))
            .then(RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1)).executes(context -> this.listAllBlocks(this.page(context))))
      );
      builder.then(
         ((LiteralArgumentBuilder)LiteralArgumentBuilder.literal("allblocks").executes(context -> this.listAllBlocks(1)))
            .then(RequiredArgumentBuilder.argument("page", IntegerArgumentType.integer(1)).executes(context -> this.listAllBlocks(this.page(context))))
      );
   }

   private int showHelp() {
      ChatUtil.usage("blockesp add <block>  •  remove <block>  •  list [page]  •  clear  •  blocks [page]");
      return 1;
   }

   private LiteralArgumentBuilder<Object> removal(String literal) {
      return (LiteralArgumentBuilder<Object>)LiteralArgumentBuilder.literal(literal)
         .then(
            RequiredArgumentBuilder.argument("block", StringArgumentType.greedyString())
               .suggests((context, suggestions) -> this.suggestSavedBlocks(suggestions))
               .executes(this::removeBlock)
         );
   }

   private int addBlock(CommandContext<Object> context) {
      String blockId = this.normalizeBlockId(StringArgumentType.getString(context, "block"));
      Identifier identifier = Identifier.tryParse(blockId);
      if (identifier == null) {
         ChatUtil.error("Invalid block id  •  " + blockId);
         return 0;
      }

      Block block = (Block)BuiltInRegistries.BLOCK.getValue(identifier);
      if (block != null && block != Blocks.AIR) {
         String registryName = BuiltInRegistries.BLOCK.getKey(block).toString();
         if (BlockEspConfig.INSTANCE.hasBlock(registryName)) {
            ChatUtil.error("Block already in the list  •  " + registryName);
            return 0;
         } else {
            BlockEspConfig.INSTANCE.addBlock(registryName);
            this.syncModule();
            ChatUtil.success("Block added  •  " + registryName);
            return 1;
         }
      } else {
         ChatUtil.error("Block not found  •  " + blockId);
         return 0;
      }
   }

   private int removeBlock(CommandContext<Object> context) {
      String blockId = this.normalizeBlockId(StringArgumentType.getString(context, "block"));
      String registryName = this.resolveRegistryName(blockId);
      if (!BlockEspConfig.INSTANCE.hasBlock(registryName)) {
         ChatUtil.error("Block not found in list  •  " + registryName);
         return 0;
      } else {
         BlockEspConfig.INSTANCE.removeBlock(registryName);
         this.syncModule();
         ChatUtil.success("Block removed  •  " + registryName);
         return 1;
      }
   }

   private int clearBlocks() {
      if (!BlockEspConfig.INSTANCE.clear()) {
         ChatUtil.info("BlockESP list is already empty");
         return 1;
      } else {
         this.syncModule();
         ChatUtil.success("BlockESP list cleared");
         return 1;
      }
   }

   private int listSavedBlocks(int page) {
      List<String> blocks = BlockEspConfig.INSTANCE.getBlockList();
      if (blocks.isEmpty()) {
         ChatUtil.info("BlockESP list is empty");
         return 1;
      } else {
         this.displayRows(blocks, page, 8, "Block ESP list (" + blocks.size() + ")", blockName -> this.savedBlockRow(blockName));
         return 1;
      }
   }

   private int listAllBlocks(int page) {
      List<String> allBlocks = BuiltInRegistries.BLOCK.keySet().stream().<String>map(Identifier::toString).sorted().toList();
      this.displayRows(allBlocks, page, 15, "All blocks (" + allBlocks.size() + ")", this::allBlocksRow);
      return 1;
   }

   private void displayRows(List<String> items, int page, int pageSize, String title, Function<String, MutableComponent> rowFactory) {
      this.sendDivider();
      ChatUtil.header(title);
      this.sendDivider();
      int pages = Math.max(1, (int)Math.ceil((double)items.size() / pageSize));
      int safePage = Math.min(page, pages);
      int start = (safePage - 1) * pageSize;
      int end = Math.min(items.size(), start + pageSize);

      for (int i = start; i < end; i++) {
         this.sendRaw((Component)rowFactory.apply(items.get(i)));
      }

      if (pages > 1) {
         ChatUtil.info("Page " + safePage + "/" + pages + "  •  " + CommandManager.INSTANCE.getPrefix() + this.name() + " list <page>");
      }
   }

   private MutableComponent savedBlockRow(String blockName) {
      String shortName = blockName.replace("minecraft:", "");
      MutableComponent row = Component.literal("  * " + shortName).append(Component.literal(" (" + blockName + ")"));
      MutableComponent hoverText = Component.literal("Click to remove " + shortName);
      row.setStyle(
         row.getStyle()
            .withHoverEvent(new ShowText(hoverText))
            .withClickEvent(new RunCommand(CommandManager.INSTANCE.getPrefix() + "blockesp remove " + blockName))
      );
      return row;
   }

   private MutableComponent allBlocksRow(String blockName) {
      boolean inList = BlockEspConfig.INSTANCE.hasBlock(blockName);
      String prefix = inList ? "[x]" : "[ ]";
      MutableComponent row = Component.literal("  " + prefix + " " + blockName.replace("minecraft:", ""));
      String command = inList
         ? CommandManager.INSTANCE.getPrefix() + "blockesp remove " + blockName
         : CommandManager.INSTANCE.getPrefix() + "blockesp add " + blockName;
      MutableComponent hoverText = Component.literal(inList ? "Click to remove" : "Click to add");
      row.setStyle(row.getStyle().withHoverEvent(new ShowText(hoverText)).withClickEvent(new RunCommand(command)));
      return row;
   }

   private void sendDivider() {
      this.sendRaw(Component.literal("------------------------------------------------"));
   }

   private void sendRaw(Component component) {
      if (mc.gui != null) {
         mc.gui.hud.getChat().addClientSystemMessage(component);
      }
   }

   private void syncModule() {
      BlockEspFeature feature = FeatureManager.INSTANCE.getFeature(BlockEspFeature.class);
      if (feature != null) {
         feature.getBlocksToHighlight().clear();
         feature.getBlocksToHighlight().addAll(BlockEspConfig.INSTANCE.getBlocks());
      }
   }

   private int page(CommandContext<Object> context) {
      return IntegerArgumentType.getInteger(context, "page");
   }

   private String normalizeBlockId(String raw) {
      String blockId = raw.toLowerCase(Locale.ROOT);
      if (!blockId.contains(":")) {
         blockId = "minecraft:" + blockId;
      }

      return blockId;
   }

   private String resolveRegistryName(String blockId) {
      Identifier identifier = Identifier.tryParse(blockId);
      if (identifier == null) {
         return blockId;
      }

      Block block = (Block)BuiltInRegistries.BLOCK.getValue(identifier);
      return block == null ? blockId : BuiltInRegistries.BLOCK.getKey(block).toString();
   }

   private CompletableFuture<Suggestions> suggestBlocks(SuggestionsBuilder builder) {
      String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
      Stream<String> blocks = BuiltInRegistries.BLOCK
         .keySet()
         .stream()
         .<String>map(Identifier::toString)
         .filter(name -> name.toLowerCase(Locale.ROOT).contains(remaining))
         .limit(50L);
      blocks.forEach(builder::suggest);
      return builder.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestSavedBlocks(SuggestionsBuilder builder) {
      String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
      BlockEspConfig.INSTANCE.getBlockList().stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining)).forEach(builder::suggest);
      return builder.buildFuture();
   }
}

