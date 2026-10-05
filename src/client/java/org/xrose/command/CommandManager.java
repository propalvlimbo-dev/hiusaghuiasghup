package org.xrose.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.xrose.command.impl.AhCommand;
import org.xrose.command.impl.BindCommand;
import org.xrose.command.impl.BlockEspCommand;
import org.xrose.command.impl.ConfigCommand;
import org.xrose.command.impl.FriendCommand;
import org.xrose.command.impl.GpsCommand;
import org.xrose.command.impl.HelpCommand;
import org.xrose.command.impl.MacroCommand;
import org.xrose.command.impl.NbtParserCommand;
import org.xrose.command.impl.PrefixCommand;
import org.xrose.command.impl.RctCommand;
import org.xrose.command.impl.StaffCommand;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventManager;
import org.xrose.event.EventTarget;
import org.xrose.event.events.input.KeyboardInputEvent;
import org.xrose.event.events.input.MouseInputEvent;
import org.xrose.feature.setting.BindSetting;
import org.xrose.utils.text.ChatUtil;

public final class CommandManager {
   public static final CommandManager INSTANCE = new CommandManager();
   public static final String DEFAULT_PREFIX = ".";
   private final CommandDispatcher<Object> dispatcher = new CommandDispatcher();
   private final List<ClientCommand> commands = new ArrayList<>();
   private final Map<String, Macro> macros = new LinkedHashMap<>();
   private final CommandStore store = new CommandStore();
   private String prefix = ".";
   private boolean initialized;

   private CommandManager() {
   }

   public void initialize() {
      if (!this.initialized) {
         List.of(
               new HelpCommand(this),
               new PrefixCommand(this),
               new BindCommand(),
               new FriendCommand(),
               new AhCommand(),
               new MacroCommand(this),
               new GpsCommand(),
               new BlockEspCommand(),
               new ConfigCommand(),
               new NbtParserCommand(),
               new RctCommand(),
               new StaffCommand()
            )
            .forEach(this::register);
         this.store.load(this);
         this.initialized = true;
      }
   }

   public List<ClientCommand> getCommands() {
      return Collections.unmodifiableList(this.commands);
   }

   public CommandDispatcher<Object> getDispatcher() {
      return this.dispatcher;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix != null && !prefix.isBlank() ? prefix.trim() : ".";
      this.store.save(this);
   }

   public boolean handleChat(String message) {
      if (this.initialized && message != null && message.startsWith(this.prefix)) {
         String input = message.substring(this.prefix.length()).trim();
         if (input.isEmpty()) {
            return false;
         }

         try {
            this.dispatcher.execute(input, new Object());
         } catch (CommandSyntaxException exception) {
            String detail = exception.getRawMessage() == null ? null : exception.getRawMessage().getString();
            ChatUtil.error(
               detail != null && !detail.isBlank() ? detail + "  •  Try " + this.prefix + "help" : "Unknown command or syntax  •  Try " + this.prefix + "help"
            );
         } catch (Exception exception) {
            ChatUtil.error("Command failed  •  " + exception.getMessage());
         }

         return true;
      } else {
         return false;
      }
   }

   public Collection<Macro> getMacros() {
      return Collections.unmodifiableCollection(this.macros.values());
   }

   public Macro getMacro(String name) {
      return this.macros.get(this.normalize(name));
   }

   public void putMacro(Macro macro) {
      this.macros.put(this.normalize(macro.name()), macro);
      this.store.save(this);
   }

   public boolean removeMacro(String name) {
      boolean removed = this.macros.remove(this.normalize(name)) != null;
      if (removed) {
         this.store.save(this);
      }

      return removed;
   }

   void loadState(String prefix, List<Macro> loadedMacros) {
      this.prefix = prefix != null && !prefix.isBlank() ? prefix : ".";
      this.macros.clear();

      for (Macro macro : loadedMacros) {
         this.macros.put(this.normalize(macro.name()), macro);
      }
   }

   @EventTarget
   public void onKeyboardInput(KeyboardInputEvent event) {
      if (event.getAction() == 1) {
         this.dispatchMacros(BindSetting.key(event.getKey()));
      }
   }

   @EventTarget
   public void onMouseInput(MouseInputEvent event) {
      if (event.getAction() == 1) {
         this.dispatchMacros(BindSetting.mouse(event.getButton()));
      }
   }

   private void dispatchMacros(int bindCode) {
      Minecraft mc = MinecraftContext.mc;
      if (mc.player != null && mc.gui.screen() == null) {
         for (Macro macro : this.macros.values()) {
            if (macro.bindCode() == bindCode) {
               this.send(mc, macro.text());
            }
         }
      }
   }

   private void send(Minecraft mc, String text) {
      if (!this.handleChat(text)) {
         if (text.startsWith("/")) {
            mc.player.connection.sendCommand(text.substring(1));
         } else {
            mc.player.connection.sendChat(text);
         }
      }
   }

   private void register(ClientCommand command) {
      this.registerLiteral(command.name(), command);

      for (String alias : command.aliases()) {
         this.registerLiteral(alias, command);
      }

      this.commands.add(command);
      EventManager.subscribe(command);
   }

   private void registerLiteral(String literal, ClientCommand command) {
      LiteralArgumentBuilder<Object> builder = LiteralArgumentBuilder.literal(literal);
      command.build(builder);
      this.dispatcher.register(builder);
   }

   private String normalize(String value) {
      return value.toLowerCase(Locale.ROOT);
   }
}

