package org.xrose.mixin.gui;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.CommandSuggestions$SuggestionsList")
public abstract class CommandSuggestionsSuggestionsListMixin {
   @Shadow
   private List<Suggestion> suggestionList;
   @Shadow
   private int current;

   @Inject(method = "useSuggestion", at = @At("HEAD"), cancellable = true)
   private void preventPriceHintApplication(CallbackInfo ci) {
      if (this.current >= 0 && this.current < this.suggestionList.size()) {
         String text = this.suggestionList.get(this.current).getText();
         if (text.startsWith("Цена: ") || text.startsWith("Сумма: ")) {
            ci.cancel();
         }
      }
   }
}

