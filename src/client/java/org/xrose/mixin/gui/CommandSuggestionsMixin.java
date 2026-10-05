package org.xrose.mixin.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xrose.command.CommandManager;
import org.xrose.utils.text.SensitiveChatMask;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
   private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
   private static final CommandSuggestionsMixin.PriceCommand[] PRICE_COMMANDS = CommandSuggestionsMixin.PriceCommand.values();
   @Shadow
   @Final
   private Screen screen;
   @Shadow
   @Final
   private EditBox input;
   @Shadow
   @Final
   private List<FormattedCharSequence> commandUsage;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private boolean keepSuggestions;
   @Shadow
   private boolean allowSuggestions;

   private static String formatPriceHint(String text) {
      if (text != null && !text.isBlank()) {
         String trimmed = text.trim();

         for (CommandSuggestionsMixin.PriceCommand command : PRICE_COMMANDS) {
            if (trimmed.startsWith(command.prefix)) {
               String remainder = trimmed.substring(command.prefix.length()).trim();
               String token = remainder.substring(remainder.lastIndexOf(32) + 1);
               if (!token.isEmpty() && token.length() <= 18 && token.chars().allMatch(Character::isDigit)) {
                  return command.label + PRICE_FORMAT.format(Long.parseUnsignedLong(token));
               }

               return null;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Shadow
   public abstract void showSuggestions(boolean var1);

   @Shadow
   public abstract void hide();

   @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
   private void suggestClientCommands(CallbackInfo ci) {
      if (this.screen instanceof ChatScreen) {
         String text = this.input.getValue();
         if (SensitiveChatMask.hasSecret(text)) {
            ci.cancel();
            this.input.setSuggestion(null);
            this.hide();
            this.commandUsage.clear();
            this.pendingSuggestions = null;
         } else {
            String priceHint = formatPriceHint(text);
            if (priceHint != null && !text.trim().endsWith(" ")) {
               ci.cancel();
               if (!this.keepSuggestions) {
                  this.input.setSuggestion(null);
                  this.hide();
                  this.commandUsage.clear();
                  int start = Math.max(text.lastIndexOf(32) + 1, 0);
                  this.pendingSuggestions = CompletableFuture.completedFuture(new SuggestionsBuilder(text, start).suggest(priceHint).build());
                  this.showSuggestions(false);
               }
            } else {
               String prefix = CommandManager.INSTANCE.getPrefix();
               if (!prefix.equals("/") && text.startsWith(prefix)) {
                  ci.cancel();
                  if (!this.keepSuggestions) {
                     this.input.setSuggestion(null);
                     this.hide();
                     this.commandUsage.clear();
                     StringReader reader = new StringReader(text);
                     reader.setCursor(prefix.length());
                     CommandDispatcher<Object> dispatcher = CommandManager.INSTANCE.getDispatcher();
                     ParseResults<Object> parse = dispatcher.parse(reader, new Object());
                     int cursor = Math.max(this.input.getCursorPosition(), prefix.length());
                     CompletableFuture<Suggestions> future = dispatcher.getCompletionSuggestions(parse, cursor);
                     this.pendingSuggestions = future;
                     future.thenRun(() -> {
                        if (this.pendingSuggestions == future && future.isDone() && !future.join().isEmpty() && this.allowSuggestions) {
                           this.showSuggestions(false);
                        }
                     });
                  }
               }
            }
         }
      }
   }

   private enum PriceCommand {
      AH_SELL("/ah sell ", "Цена: "),
      PAY("/pay ", "Сумма: "),
      CLAN_INVEST("/clan invest ", "Сумма: "),
      CLAN_WITHDRAW("/clan withdraw ", "Сумма: ");

      final String prefix;
      final String label;

      PriceCommand(String prefix, String label) {
         this.prefix = prefix;
         this.label = label;
      }

      // $VF: synthetic method
      private static CommandSuggestionsMixin.PriceCommand[] $values() {
         return new CommandSuggestionsMixin.PriceCommand[]{AH_SELL, PAY, CLAN_INVEST, CLAN_WITHDRAW};
      }
   }
}

