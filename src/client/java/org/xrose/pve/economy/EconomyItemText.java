package org.xrose.pve.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public final class EconomyItemText {
   private static final Pattern NAME_QUANTITY = Pattern.compile(
      "(?<![\\p{L}\\p{N}])[x×*]\\s*(\\d{1,6})(?!\\p{L})|(\\d{1,6})\\s*(?:шт|pcs|pieces)(?!\\p{L})", 66
   );

   private EconomyItemText() {
   }

   public static List<String> lines(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ArrayList<String> lines = new ArrayList<>();
         lines.add(stack.getHoverName().getString());
         ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
         if (lore != null) {
            for (Component line : lore.lines()) {
               lines.add(line.getString());
            }
         }

         return List.copyOf(lines);
      } else {
         return List.of();
      }
   }

   public static String combined(ItemStack stack) {
      return String.join("\n", lines(stack));
   }

   public static boolean containsAny(ItemStack stack, String... markers) {
      return EconomyTextParser.containsAny(combined(stack), markers);
   }

   public static OptionalLong listingPrice(ItemStack stack) {
      long largest = -1L;

      for (String line : lines(stack)) {
         String normalized = EconomyTextParser.normalize(line);
         if (EconomyTextParser.containsAny(normalized, "price", "cost", "цена", "стоимость", "за все", "за штуку", "монет", "$")) {
            OptionalLong amount = EconomyTextParser.largestAmount(normalized);
            if (amount.isPresent()) {
               largest = Math.max(largest, amount.getAsLong());
            }
         }
      }

      return largest < 0L ? OptionalLong.empty() : OptionalLong.of(largest);
   }

   public static OptionalLong listingUnitPrice(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         List<String> allLines = lines(stack);
         int divisor = lotQuantity(allLines, stack);
         long largest = -1L;

         for (String line : allLines) {
            String normalized = EconomyTextParser.normalize(line);
            if (EconomyTextParser.containsAny(normalized, "price", "cost", "цена", "стоимость", "за все", "за штуку", "per item", "each", "монет", "$")) {
               OptionalLong parsed = EconomyTextParser.largestAmount(normalized);
               if (!parsed.isEmpty()) {
                  boolean explicitlyPerItem = EconomyTextParser.containsAny(normalized, "за штуку", "per item", "each", "1 шт");
                  long unit = explicitlyPerItem ? parsed.getAsLong() : Math.max(1L, parsed.getAsLong() / divisor);
                  largest = Math.max(largest, unit);
               }
            }
         }

         return largest < 0L ? OptionalLong.empty() : OptionalLong.of(largest);
      } else {
         return OptionalLong.empty();
      }
   }

   private static int lotQuantity(List<String> textLines, ItemStack stack) {
      int loreQuantity = listedQuantity(textLines);
      if (loreQuantity > 0) {
         return loreQuantity;
      }

      int nameQuantity = nameQuantity(textLines.isEmpty() ? "" : textLines.get(0));
      return nameQuantity > 0 ? nameQuantity : Math.max(1, stack.getCount());
   }

   public static int nameQuantity(String name) {
      if (name != null && !name.isBlank()) {
         String normalized = EconomyTextParser.normalize(name);
         int largest = 0;
         Matcher matcher = NAME_QUANTITY.matcher(normalized);

         while (matcher.find()) {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (group != null) {
               try {
                  int value = Integer.parseInt(group);
                  if (value >= 2 && value <= 100000) {
                     largest = Math.max(largest, value);
                  }
               } catch (NumberFormatException var6) {
               }
            }
         }

         return largest;
      } else {
         return 0;
      }
   }

   public static int listedQuantity(List<String> textLines) {
      if (textLines == null) {
         return 0;
      }

      for (String line : textLines) {
         String normalized = EconomyTextParser.normalize(line);
         if (EconomyTextParser.containsAny(normalized, "количество", "кол-во", "quantity")) {
            OptionalLong parsed = EconomyTextParser.amountNearAnyLabel(line, "количество", "кол-во", "quantity");
            if (parsed.isPresent()) {
               long value = parsed.getAsLong();
               if (value >= 1L && value <= 100000L) {
                  return (int)value;
               }
            }
         }
      }

      return 0;
   }
}

