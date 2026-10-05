package org.xrose.pve.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.lifecycle.DisconnectEvent;
import org.xrose.event.events.lifecycle.WorldLeaveEvent;
import org.xrose.pve.server.ServerAdapter;
import org.xrose.pve.server.ServerAdapters;
import org.xrose.utils.inventory.ContainerLootService;
import org.xrose.utils.inventory.InventoryUtil;
import org.xrose.utils.text.ChatUtil;

public final class AhSellFlow {
   private static final long OPEN_TIMEOUT_TICKS = 160L;
   private static final long PAGE_SETTLE_TICKS_MIN = 4L;
   private static final long PAGE_SETTLE_TICKS_MAX = 9L;
   private static final long LIST_DELAY_TICKS_MIN = 8L;
   private static final long LIST_DELAY_TICKS_MAX = 16L;
   private static final long SELL_COMMAND_COOLDOWN_MIN = 30L;
   private static final long SELL_COMMAND_COOLDOWN_MAX = 50L;
   private static final int DEFAULT_MAX_PAGES = 3;
   private static final Pattern QUERY_CHARS = Pattern.compile("[^\\p{L}\\p{N}_ .,'\\-’]");
   public static final AhSellFlow INSTANCE = new AhSellFlow();
   private final CommandCooldown commandCooldown = new CommandCooldown();
   private AhSellFlow.State state = AhSellFlow.State.IDLE;
   private long elapsedTicks;
   private long stateStartTick;
   private long pageActionTick;
   private int discountPercent;
   private int saleCount;
   private int maxPages;
   private int pagesScanned;
   private int sellSourceSlot = -1;
   private Item targetItem;
   private String query;
   private String[] queries;
   private final ArrayList<List<Long>> pageExactUnitPrices = new ArrayList<>();
   private final ArrayList<List<Long>> pageFuzzyUnitPrices = new ArrayList<>();
   private long salePrice;

   private AhSellFlow() {
   }

   public boolean isActive() {
      return this.state != AhSellFlow.State.IDLE;
   }

   public boolean start(LocalPlayer player, int discountPercent) {
      Minecraft client = Minecraft.getInstance();
      if (this.isActive()) {
         ChatUtil.error("Анализ цены уже выполняется");
         return false;
      }

      if (player == null) {
         ChatUtil.error("Вы не подключены к серверу");
         return false;
      }

      if (player.containerMenu == player.inventoryMenu && (client.gui.screen() == null || client.gui.screen() instanceof ChatScreen)) {
         if (discountPercent >= 1 && discountPercent <= 99) {
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
               ChatUtil.error("Возьмите предмет в руку");
               return false;
            } else {
               String safeQuery = sanitizeQuery(stack.getHoverName().getString());
               if (safeQuery.isEmpty()) {
                  ChatUtil.error("Не удалось построить запрос для предмета");
                  return false;
               } else {
                  ServerAdapter adapter = ServerAdapters.current();
                  Optional<String> command = adapter.auctionCommand().flatMap(root -> EconomyCommands.auctionSearch(root, safeQuery));
                  if (command.isEmpty()) {
                     ChatUtil.error("Аукцион не поддерживается на этом сервере");
                     return false;
                  } else {
                     this.discountPercent = discountPercent;
                     this.saleCount = stack.getCount();
                     this.maxPages = 3;
                     this.targetItem = stack.getItem();
                     this.query = safeQuery;
                     this.queries = new String[]{safeQuery, BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()};
                     this.pageExactUnitPrices.clear();
                     this.pageFuzzyUnitPrices.clear();
                     this.salePrice = -1L;
                     this.pagesScanned = 0;
                     this.sellSourceSlot = -1;
                     this.elapsedTicks = 0L;
                     this.stateStartTick = 0L;
                     this.pageActionTick = 0L;
                     this.commandCooldown.reset();
                     this.state = AhSellFlow.State.OPEN_SEARCH;
                     ChatUtil.info("Анализ цены  •  " + stack.getHoverName().getString() + " x" + stack.getCount());
                     return true;
                  }
               }
            }
         } else {
            ChatUtil.error("Скидка должна быть от 1 до 99 процентов");
            return false;
         }
      } else {
         ChatUtil.error("Закройте экран и попробуйте снова");
         return false;
      }
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.state != AhSellFlow.State.IDLE) {
         Minecraft client = event.getClient();
         if (client != null && client.player != null && client.getConnection() != null) {
            this.elapsedTicks++;
            switch (this.state) {
               case OPEN_SEARCH:
                  this.openSearch(client, client.player);
                  break;
               case WAIT_SEARCH:
                  this.waitSearch(client, client.player);
                  break;
               case LIST:
                  this.list(client, client.player);
            }
         } else {
            this.abort(client, null);
         }
      }
   }

   @EventTarget
   public void onDisconnect(DisconnectEvent event) {
      this.abort(Minecraft.getInstance(), null);
   }

   @EventTarget
   public void onWorldLeave(WorldLeaveEvent event) {
      this.abort(Minecraft.getInstance(), null);
   }

   private void openSearch(Minecraft client, LocalPlayer player) {
      if (this.elapsedTicks - this.stateStartTick > 160L) {
         this.abort(client, "Не удалось открыть аукцион");
      } else if (client.gui.screen() == null && this.commandCooldown.ready(this.elapsedTicks)) {
         ServerAdapter adapter = ServerAdapters.current();
         adapter.auctionCommand().flatMap(root -> EconomyCommands.auctionSearch(root, this.query)).ifPresentOrElse(command -> {
            this.commandCooldown.tryAcquire(this.elapsedTicks, randomDelay(30L, 50L));
            adapter.sendCommand(player, command);
            this.state = AhSellFlow.State.WAIT_SEARCH;
            this.stateStartTick = this.elapsedTicks;
         }, () -> this.abort(client, "Аукцион не поддерживается на этом сервере"));
      }
   }

   private void waitSearch(Minecraft client, LocalPlayer player) {
      if (this.elapsedTicks - this.stateStartTick > 160L) {
         this.abort(client, "Не удалось найти цену предмета на аукционе");
      } else {
         AbstractContainerMenu menu = currentForeignMenu(player);
         if (menu == null) {
            this.pageActionTick = this.elapsedTicks;
         } else if (this.elapsedTicks - this.pageActionTick >= randomDelay(4L, 9L)) {
            ArrayList<Long> exactPrices = new ArrayList<>();
            ArrayList<Long> fuzzyPrices = new ArrayList<>();
            AuctionPriceScanner.collectUnitPrices(exactPrices, fuzzyPrices, menu, this.targetItem, client.player.getName().getString(), this.queries);
            addPage(this.pageExactUnitPrices, exactPrices);
            addPage(this.pageFuzzyUnitPrices, fuzzyPrices);
            this.pagesScanned++;
            if (this.pagesScanned < this.maxPages) {
               int nextPage = ContainerLootService.findFirst(
                  menu, stack -> EconomyItemText.containsAny(stack, "next page", "следующая страница", "вперед", "далее")
               );
               if (nextPage >= 0) {
                  InventoryUtil.clickSlot(nextPage, 0, ContainerInput.PICKUP);
                  this.pageActionTick = this.elapsedTicks;
                  return;
               }
            }

            this.resolveSalePrice(client);
         }
      }
   }

   private static void addPage(ArrayList<List<Long>> pages, ArrayList<Long> prices) {
      if (!prices.isEmpty()) {
         if (pages.isEmpty() || !pages.get(pages.size() - 1).equals(prices)) {
            pages.add(prices);
         }
      }
   }

   private void resolveSalePrice(Minecraft client) {
      List<List<Long>> source = this.pageExactUnitPrices.isEmpty() ? this.pageFuzzyUnitPrices : this.pageExactUnitPrices;
      OptionalLong medianUnit = AuctionPriceScanner.medianUnitPrice(source);
      OptionalLong minimumUnit = AuctionPriceScanner.minimumUnitPrice(source);
      OptionalLong marketUnit;
      if (medianUnit.isPresent() && minimumUnit.isPresent()) {
         marketUnit = OptionalLong.of(Math.max(1L, (medianUnit.getAsLong() + minimumUnit.getAsLong()) / 2L));
      } else if (medianUnit.isPresent()) {
         marketUnit = medianUnit;
      } else {
         marketUnit = minimumUnit;
      }

      if (marketUnit.isEmpty()) {
         this.abort(client, "Предмет не найден на аукционе");
      } else {
         long marketTotal = AuctionPriceScanner.marketTotal(marketUnit.getAsLong(), this.saleCount);
         long discounted = Math.round(marketTotal * (100.0 - this.discountPercent) / 100.0);
         this.salePrice = Math.min(2147483647L, Math.max(1L, discounted));
         ChatUtil.info(
            "Лоу "
               + formatPrice(minimumUnit.isPresent() ? AuctionPriceScanner.marketTotal(minimumUnit.getAsLong(), this.saleCount) : marketTotal)
               + "  •  средняя "
               + formatPrice(medianUnit.isPresent() ? AuctionPriceScanner.marketTotal(medianUnit.getAsLong(), this.saleCount) : marketTotal)
               + "  •  выставляю за "
               + formatPrice(this.salePrice)
               + " ("
               + this.saleCount
               + " шт)  •  скидка "
               + this.discountPercent
               + "%"
         );
         this.closeOwnedContainer(client);
         this.sellSourceSlot = InventoryUtil.findPlayerMenuSlot(client.player, stack -> stack.is(this.targetItem) && stack.getCount() == this.saleCount);
         this.state = AhSellFlow.State.LIST;
         this.stateStartTick = this.elapsedTicks;
      }
   }

   private void list(Minecraft client, LocalPlayer player) {
      if (this.elapsedTicks - this.stateStartTick > 160L) {
         this.abort(client, "Не удалось выставить предмет на аукцион");
      } else if (this.elapsedTicks - this.stateStartTick >= randomDelay(8L, 16L)) {
         this.selectSellStack(player);
         ItemStack held = player.getMainHandItem();
         if (!held.isEmpty() && held.is(this.targetItem)) {
            ServerAdapter adapter = ServerAdapters.current();
            adapter.auctionCommand()
               .flatMap(root -> EconomyCommands.auctionSell(root, this.salePrice))
               .ifPresentOrElse(
                  command -> {
                     this.commandCooldown.tryAcquire(this.elapsedTicks, randomDelay(30L, 50L));
                     adapter.sendCommand(player, command);
                     ChatUtil.success(
                        "Выставлено " + this.saleCount + " шт за " + formatPrice(this.salePrice) + " монет  •  скидка " + this.discountPercent + "% от рынка"
                     );
                     this.reset();
                  },
                  () -> this.abort(client, "Не удалось отправить команду продажи")
               );
         }
      }
   }

   private void selectSellStack(LocalPlayer player) {
      if (this.sellSourceSlot >= 0) {
         int selected = player.getInventory().getSelectedSlot();
         int selectedMenu = 36 + selected;
         if (this.sellSourceSlot >= 36 && this.sellSourceSlot <= 44) {
            player.getInventory().setSelectedSlot(this.sellSourceSlot - 36);
         } else if (this.sellSourceSlot != selectedMenu) {
            this.swapInventorySlotToHotbar(player, this.sellSourceSlot, selected);
         }
      }
   }

   private void swapInventorySlotToHotbar(LocalPlayer player, int slotId, int hotbarSlot) {
      Minecraft client = Minecraft.getInstance();
      if (client.gameMode != null
         && player != null
         && player.containerMenu == player.inventoryMenu
         && player.inventoryMenu.isValidSlotIndex(slotId)
         && hotbarSlot >= 0
         && hotbarSlot <= 8) {
         client.gameMode.handleContainerInput(player.inventoryMenu.containerId, slotId, hotbarSlot, ContainerInput.SWAP, player);
      }
   }

   private static AbstractContainerMenu currentForeignMenu(LocalPlayer player) {
      return player != null && player.containerMenu != null && player.containerMenu != player.inventoryMenu ? player.containerMenu : null;
   }

   private void closeOwnedContainer(Minecraft client) {
      if (client != null && client.player != null) {
         LocalPlayer player = client.player;
         if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
         } else if (client.gui.screen() instanceof AbstractContainerScreen) {
            client.gui.setScreen(null);
         }
      }
   }

   private void abort(Minecraft client, String message) {
      if (message != null && client != null) {
         ChatUtil.error(message);
      }

      this.closeOwnedContainer(client);
      this.reset();
   }

   private void reset() {
      this.state = AhSellFlow.State.IDLE;
      this.elapsedTicks = 0L;
      this.stateStartTick = 0L;
      this.pageActionTick = 0L;
      this.discountPercent = 0;
      this.saleCount = 0;
      this.maxPages = 3;
      this.pagesScanned = 0;
      this.sellSourceSlot = -1;
      this.targetItem = null;
      this.query = null;
      this.queries = null;
      this.pageExactUnitPrices.clear();
      this.pageFuzzyUnitPrices.clear();
      this.salePrice = -1L;
      this.commandCooldown.reset();
   }

   private static String sanitizeQuery(String name) {
      if (name != null && !name.isBlank()) {
         String cleaned = QUERY_CHARS.matcher(name.replace(' ', ' ')).replaceAll("").replaceAll("\\s+", " ").trim();
         if (cleaned.length() > 40) {
            cleaned = cleaned.substring(0, 40).trim();
         }

         return cleaned;
      } else {
         return "";
      }
   }

   private static String formatPrice(long value) {
      return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
   }

   private static long randomDelay(long min, long max) {
      return ThreadLocalRandom.current().nextLong(min, max + 1L);
   }

   private enum State {
      IDLE,
      OPEN_SEARCH,
      WAIT_SEARCH,
      LIST;

      // $VF: synthetic method
      private static AhSellFlow.State[] $values() {
         return new AhSellFlow.State[]{IDLE, OPEN_SEARCH, WAIT_SEARCH, LIST};
      }
   }
}

