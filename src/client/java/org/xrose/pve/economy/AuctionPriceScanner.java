package org.xrose.pve.economy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.xrose.utils.inventory.ContainerLootService;

public final class AuctionPriceScanner {
   private AuctionPriceScanner() {
   }

   public static void collectUnitPrices(Collection<Long> into, AbstractContainerMenu menu, Item target, String... queries) {
      collectUnitPrices(into, null, menu, target, null, queries);
   }

   public static void collectUnitPrices(
      Collection<Long> exactInto, Collection<Long> fuzzyInto, AbstractContainerMenu menu, Item target, String ownSellerName, String... queries
   ) {
      if ((exactInto != null || fuzzyInto != null) && menu != null && target != null) {
         String ownSeller = ownSellerName == null ? "" : EconomyTextParser.normalize(ownSellerName);
         int containerSlots = ContainerLootService.containerSlotCount(menu);
         List<long[]> exactEntries = new ArrayList<>();
         List<long[]> fuzzyEntries = new ArrayList<>();

         for (int slotId = 0; slotId < containerSlots; slotId++) {
            if (menu.isValidSlotIndex(slotId)) {
               ItemStack stack = menu.getSlot(slotId).getItem();
               if (!stack.isEmpty() && !isShulkerStack(stack) && (ownSeller.isEmpty() || !EconomyTextParser.containsAny(sellerName(stack), ownSeller))) {
                  List<long[]> entries;
                  if (stack.is(target)) {
                     entries = exactEntries;
                  } else {
                     if (fuzzyInto == null || !EconomyItemText.containsAny(stack, queries)) {
                        continue;
                     }

                     entries = fuzzyEntries;
                  }

                  int count = Math.max(1, stack.getCount());
                  EconomyItemText.listingUnitPrice(stack).ifPresent(unit -> entries.add(new long[]{count, unit}));
               }
            }
         }

         collectNonOverpriced(exactInto, exactEntries);
         collectNonOverpriced(fuzzyInto, fuzzyEntries);
      }
   }

   private static void collectNonOverpriced(Collection<Long> into, List<long[]> entries) {
      if (into != null) {
         for (long[] entry : entries) {
            if (!isOverpriced(entry, entries)) {
               into.add(entry[1]);
            }
         }
      }
   }

   private static boolean isOverpriced(long[] entry, List<long[]> all) {
      long count = entry[0];
      long unit = entry[1];

      for (long[] other : all) {
         if (other != entry && other[0] > count && other[1] < unit) {
            return true;
         }
      }

      return false;
   }

   private static String sellerName(ItemStack stack) {
      for (String line : EconomyItemText.lines(stack)) {
         String normalized = EconomyTextParser.normalize(line);
         int index = normalized.indexOf("продавец:");
         if (index < 0) {
            index = normalized.indexOf("seller:");
         }

         if (index >= 0) {
            return normalized.substring(index).replaceFirst("^(продавец|seller):\\s*", "").trim();
         }
      }

      return "";
   }

   private static boolean isShulkerStack(ItemStack stack) {
      return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   public static OptionalLong medianUnitPrice(AbstractContainerMenu menu, Item target, String... queries) {
      if (menu != null && target != null) {
         ArrayList<Long> unitPrices = new ArrayList<>();
         collectUnitPrices(unitPrices, menu, target, queries);
         if (unitPrices.isEmpty()) {
            return OptionalLong.empty();
         }

         Collections.sort(unitPrices);
         return OptionalLong.of(unitPrices.get(unitPrices.size() / 2));
      } else {
         return OptionalLong.empty();
      }
   }

   public static OptionalLong persistentUnitPrice(List<Set<Long>> perPageUnitPrices) {
      if (perPageUnitPrices != null && !perPageUnitPrices.isEmpty()) {
         Set<Long> intersection = null;

         for (Set<Long> page : perPageUnitPrices) {
            if (page == null || page.isEmpty()) {
               return OptionalLong.empty();
            }

            if (intersection == null) {
               intersection = new HashSet<>(page);
            } else {
               intersection.retainAll(page);
            }
         }

         return intersection != null && !intersection.isEmpty() ? OptionalLong.of(Collections.min(intersection)) : OptionalLong.empty();
      } else {
         return OptionalLong.empty();
      }
   }

   public static OptionalLong minimumUnitPrice(List<? extends Collection<Long>> perPageUnitPrices) {
      if (perPageUnitPrices != null && !perPageUnitPrices.isEmpty()) {
         long minimum = -1L;

         for (Collection<Long> page : perPageUnitPrices) {
            if (page != null && !page.isEmpty()) {
               long pageMin = Collections.min(page);
               minimum = minimum < 0L ? pageMin : Math.min(minimum, pageMin);
            }
         }

         return minimum < 0L ? OptionalLong.empty() : OptionalLong.of(minimum);
      } else {
         return OptionalLong.empty();
      }
   }

   public static OptionalLong averageUnitPrice(List<? extends Collection<Long>> perPageUnitPrices) {
      if (perPageUnitPrices != null && !perPageUnitPrices.isEmpty()) {
         long sum = 0L;
         long count = 0L;

         for (Collection<Long> page : perPageUnitPrices) {
            if (page != null && !page.isEmpty()) {
               for (Long value : page) {
                  sum += value;
                  count++;
               }
            }
         }

         return count == 0L ? OptionalLong.empty() : OptionalLong.of(Math.max(1L, sum / count));
      } else {
         return OptionalLong.empty();
      }
   }

   public static OptionalLong medianUnitPrice(List<? extends Collection<Long>> perPageUnitPrices) {
      if (perPageUnitPrices != null && !perPageUnitPrices.isEmpty()) {
         ArrayList<Long> all = new ArrayList<>();

         for (Collection<Long> page : perPageUnitPrices) {
            if (page != null) {
               all.addAll(page);
            }
         }

         if (all.isEmpty()) {
            return OptionalLong.empty();
         }

         Collections.sort(all);
         int middle = all.size() / 2;
         long median = all.size() % 2 == 1 ? all.get(middle) : Math.max(1L, (all.get(middle - 1) + all.get(middle)) / 2L);
         return OptionalLong.of(median);
      } else {
         return OptionalLong.empty();
      }
   }

   public static long marketTotal(long unitPrice, int saleCount) {
      if (saleCount <= 0) {
         return 1L;
      }

      long total;
      try {
         total = Math.multiplyExact(unitPrice, saleCount);
      } catch (ArithmeticException ignored) {
         total = 2147483647L;
      }

      return Math.min(2147483647L, Math.max(1L, total));
   }

   public static OptionalLong marketPrice(AbstractContainerMenu menu, Item target, int saleCount, String... queries) {
      if (saleCount <= 0) {
         return OptionalLong.empty();
      }

      OptionalLong unitMedian = medianUnitPrice(menu, target, queries);
      return unitMedian.isPresent() ? OptionalLong.of(marketTotal(unitMedian.getAsLong(), saleCount)) : OptionalLong.empty();
   }

   public static OptionalLong competitivePrice(AbstractContainerMenu menu, Item target, String query, int saleCount) {
      OptionalLong market = marketPrice(menu, target, saleCount, query);
      return market.isPresent() ? OptionalLong.of(Math.max(1L, market.getAsLong() - 1L)) : OptionalLong.empty();
   }
}

