package org.xrose.feature.impl.misc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.mixin.accessor.AbstractContainerScreenAccessor;
import org.xrose.utils.ColorUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AuctionHelperFeature extends Feature {
   private static final float ALPHA_SPEED = 35.0F;
   private static final Pattern PAGE_FRACTION = Pattern.compile("(?U)\\b\\d+\\s*/\\s*\\d+\\b");
   private static final Pattern AMOUNT = Pattern.compile("(?iu)(\\d+(?:[\\s\\u00A0,_.'’]\\d+)*)(?:\\s*([kкmмbб]))?");
   private static AuctionHelperFeature instance;
   public final BooleanSetting three = this.register(new BooleanSetting("Подсвечивать 3 слота", true));
   public final BooleanSetting pricePerOne = this.register(new BooleanSetting("Цена за штуку", true));
   private float x1 = 0.0F;
   private float y1 = 0.0F;
   private float x2 = 0.0F;
   private float y2 = 0.0F;
   private float x3 = 0.0F;
   private float y3 = 0.0F;
   private long price1 = 0L;
   private long price2 = 0L;
   private long price3 = 0L;
   private float alpha1 = 0.0F;
   private float alpha2 = 0.0F;
   private float alpha3 = 0.0F;
   private boolean increasing = true;
   private AbstractContainerScreen<?> lastDebugScreen;

   public AuctionHelperFeature() {
      super("Auction Helper", "Подсвечивает 3 дешевых предмета на FunTime", FeatureCategory.MISC, -1);
      instance = this;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      Minecraft mc = event.getClient();
      if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
         this.reset();
      } else {
         String title = screen.getTitle().getString();
         if (this.lastDebugScreen != screen) {
            this.lastDebugScreen = screen;
            if (isAuctionTitle(title)) {
               this.writeDebugInfo(screen, "rose-auction-debug.log");
            } else if (looksLikeAuctionMiss(title)) {
               this.writeDebugInfo(screen, "rose-auction-miss.log");
            }
         }

         if (!isAuctionTitle(title)) {
            this.reset();
         } else {
            Slot slot1 = null;
            Slot slot2 = null;
            Slot slot3 = null;
            long slotPrice1 = 0L;
            long slotPrice2 = 0L;
            long slotPrice3 = 0L;
            double fsPrice = Double.MAX_VALUE;
            double medPrice = Double.MAX_VALUE;
            double thPrice = Double.MAX_VALUE;

            for (Slot slot : screen.getMenu().slots) {
               if (slot.index <= 44) {
                  ItemStack stack = slot.getItem();
                  long totalPrice = extractPriceFromStack(stack);
                  int count = Math.max(1, stack.getCount());
                  if (totalPrice > 0L) {
                     double pricePerItem = (double)totalPrice / count;
                     long unitPrice = Math.max(1L, totalPrice / count);
                     if (pricePerItem < fsPrice) {
                        thPrice = medPrice;
                        slot3 = slot2;
                        slotPrice3 = slotPrice2;
                        medPrice = fsPrice;
                        slot2 = slot1;
                        slotPrice2 = slotPrice1;
                        fsPrice = pricePerItem;
                        slot1 = slot;
                        slotPrice1 = unitPrice;
                     } else if (this.three.getValue() && pricePerItem < medPrice) {
                        thPrice = medPrice;
                        slot3 = slot2;
                        slotPrice3 = slotPrice2;
                        medPrice = pricePerItem;
                        slot2 = slot;
                        slotPrice2 = unitPrice;
                     } else if (this.three.getValue() && pricePerItem < thPrice) {
                        thPrice = pricePerItem;
                        slot3 = slot;
                        slotPrice3 = unitPrice;
                     }
                  }
               }
            }

            if (slot1 != null) {
               this.x1 = slot1.x;
               this.y1 = slot1.y;
               this.price1 = slotPrice1;
            } else {
               this.x1 = 0.0F;
               this.y1 = 0.0F;
               this.price1 = 0L;
            }

            if (slot2 != null) {
               this.x2 = slot2.x;
               this.y2 = slot2.y;
               this.price2 = slotPrice2;
            } else {
               this.x2 = 0.0F;
               this.y2 = 0.0F;
               this.price2 = 0L;
            }

            if (slot3 != null) {
               this.x3 = slot3.x;
               this.y3 = slot3.y;
               this.price3 = slotPrice3;
            } else {
               this.x3 = 0.0F;
               this.y3 = 0.0F;
               this.price3 = 0L;
            }

            if (this.increasing) {
               this.alpha1 += 35.0F;
               this.alpha2 += 35.0F;
               this.alpha3 += 35.0F;
               if (this.alpha1 >= 200.0F) {
                  this.increasing = false;
               }
            } else {
               this.alpha1 -= 35.0F;
               this.alpha2 -= 35.0F;
               this.alpha3 -= 35.0F;
               if (this.alpha1 <= 0.0F) {
                  this.increasing = true;
               }
            }

            this.alpha1 = Math.max(0.0F, Math.min(200.0F, this.alpha1));
            this.alpha2 = Math.max(0.0F, Math.min(200.0F, this.alpha2));
            this.alpha3 = Math.max(0.0F, Math.min(200.0F, this.alpha3));
         }
      }
   }

   private void reset() {
      this.x1 = 0.0F;
      this.x2 = 0.0F;
      this.x3 = 0.0F;
      this.price1 = 0L;
      this.price2 = 0L;
      this.price3 = 0L;
      this.alpha1 = 0.0F;
      this.alpha2 = 0.0F;
      this.alpha3 = 0.0F;
   }

   public static void renderHighlights(GuiGraphicsExtractor graphics, String title, AbstractContainerScreen<?> screen) {
      if (instance != null && instance.isEnabled() && isAuctionTitle(title)) {
         if (instance.x1 != 0.0F) {
            AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor)screen;
            int left = accessor.getLeftPos();
            int top = accessor.getTopPos();
            int x = left + (int)instance.x1;
            int y = top + (int)instance.y1;
            graphics.fill(x, y, x + 16, y + 16, ColorUtil.rgba(64, 255, 64, (int)instance.alpha1));
            if (instance.x2 != 0.0F) {
               int x2 = left + (int)instance.x2;
               int y2 = top + (int)instance.y2;
               graphics.fill(x2, y2, x2 + 16, y2 + 16, ColorUtil.rgba(255, 255, 64, (int)instance.alpha2));
            }

            if (instance.x3 != 0.0F) {
               int x3 = left + (int)instance.x3;
               int y3 = top + (int)instance.y3;
               graphics.fill(x3, y3, x3 + 16, y3 + 16, ColorUtil.rgba(255, 64, 64, (int)instance.alpha3));
            }
         }
      }
   }

   public static void augmentTooltip(String title, ItemStack stack, List<Component> original, CallbackInfoReturnable<List<Component>> cir) {
      if (instance != null && instance.isEnabled() && instance.pricePerOne.getValue() && isAuctionTitle(title)) {
         long totalPrice = extractPriceFromStack(stack);
         if (totalPrice > 0L && stack.getCount() > 1) {
            long unit = Math.max(1L, totalPrice / stack.getCount());
            Component line = Component.literal("$ За штуку: $" + formatPrice(unit)).withStyle(ChatFormatting.GRAY);
            List<Component> tooltip = new ArrayList<>(original);

            for (int i = 0; i < tooltip.size(); i++) {
               if (isPriceLine(tooltip.get(i).getString())) {
                  tooltip.add(i + 1, line);
                  cir.setReturnValue(tooltip);
                  return;
               }
            }

            tooltip.add(line);
            cir.setReturnValue(tooltip);
         }
      }
   }

   public static String formatPrice(long value) {
      return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
   }

   public static boolean activeFor(String title) {
      return instance != null && instance.isEnabled() && isAuctionTitle(title);
   }

   public static boolean isAuctionTitle(String title) {
      if (title != null && !title.isBlank()) {
         String normalized = normalizeName(title);
         if (!normalized.contains("донат магазин")
            && !normalized.contains("все для pvp")
            && !normalized.startsWith("помощь")
            && !normalized.contains("премиум магазин")) {
            boolean explicitAuction = normalized.contains("аукцион")
               || normalized.contains("auction")
               || normalized.contains("поиск")
               || normalized.contains("search")
               || normalized.contains("биржа");
            return explicitAuction || title.contains("漢:") || title.contains(":") && PAGE_FRACTION.matcher(title).find();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean looksLikeAuctionMiss(String title) {
      return title != null && !title.isBlank() ? title.contains(":") || title.contains("漢") || PAGE_FRACTION.matcher(title).find() : false;
   }

   private static String normalizeName(String value) {
      if (value != null && !value.isBlank()) {
         String normalized = Normalizer.normalize(value, Form.NFKC);
         String stripped = ChatFormatting.stripFormatting(normalized);
         return (stripped == null ? normalized : stripped).toLowerCase(Locale.ROOT).replace('ё', 'е');
      } else {
         return "";
      }
   }

   private static boolean isPriceLine(String line) {
      if (line == null) {
         return false;
      }

      String normalized = normalizeName(line);
      return normalized.contains("цен")
         || normalized.contains("стоимост")
         || normalized.contains("price")
         || normalized.contains("cost")
         || normalized.contains("за штуку")
         || line.contains("$")
         || line.contains("⛃");
   }

   public static long extractPriceFromStack(ItemStack stack) {
      ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
      if (lore != null && !lore.lines().isEmpty()) {
         long price = -1L;

         for (Component line : lore.lines()) {
            String raw = line.getString();
            if (isPriceLine(raw)) {
               price = Math.max(price, parseLargestAmount(raw));
            }
         }

         return price;
      } else {
         return -1L;
      }
   }

   private static long parseLargestAmount(String line) {
      long largest = -1L;
      Matcher matcher = AMOUNT.matcher(line);

      while (matcher.find()) {
         long parsed = parseAmount(matcher.group(1), matcher.group(2));
         largest = Math.max(largest, parsed);
      }

      return largest;
   }

   private static long parseAmount(String token, String suffix) {
      long multiplier = switch (suffix == null ? "" : suffix.toLowerCase(Locale.ROOT)) {
         case "k", "к" -> 1000L;
         case "m", "м" -> 1000000L;
         case "b", "б" -> 1000000000L;
         default -> 1L;
      };

      try {
         if (multiplier > 1L) {
            String compact = token.replaceAll("[\\s\\u00A0_'’]", "");
            int separator = Math.max(compact.lastIndexOf(46), compact.lastIndexOf(44));
            if (separator >= 0 && compact.length() - separator - 1 <= 2) {
               double value = Double.parseDouble(compact.replace(',', '.'));
               return Math.round(value * multiplier);
            }
         }

         String digits = token.replaceAll("\\D", "");
         if (digits.isEmpty()) {
            return -1L;
         }

         long value = Long.parseLong(digits);
         return multiplier != 1L && value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
      } catch (NumberFormatException ignored) {
         return -1L;
      }
   }

   private void writeDebugInfo(AbstractContainerScreen<?> screen, String fileName) {
      try {
         StringBuilder sb = new StringBuilder();
         sb.append("Screen: ").append(screen.getClass().getName()).append('\n');
         sb.append("Title: ").append(screen.getTitle().getString()).append('\n');

         for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            sb.append("slot ")
               .append(slot.index)
               .append(" x=")
               .append(slot.x)
               .append(" y=")
               .append(slot.y)
               .append(" count=")
               .append(stack.getCount())
               .append(" name=")
               .append(stack.getHoverName().getString())
               .append('\n');
            ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
            if (lore != null) {
               for (Component line : lore.lines()) {
                  sb.append("    | ").append(line.getString()).append('\n');
               }
            }
         }

         Files.writeString(Path.of(Minecraft.getInstance().gameDirectory.getAbsolutePath(), fileName), sb.toString());
      } catch (Exception var10) {
      }
   }
}

