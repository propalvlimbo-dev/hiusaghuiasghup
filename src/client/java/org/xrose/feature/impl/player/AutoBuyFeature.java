package org.xrose.feature.impl.player;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import org.slf4j.LoggerFactory;
import org.xrose.context.MinecraftContext;
import org.xrose.context.RenderContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.event.events.packet.PacketReceiveEvent;
import org.xrose.event.events.render.FinalGuiRenderEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ButtonSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.feature.setting.TextSetting;
import org.xrose.mixin.accessor.AbstractContainerScreenAccessor;
import org.xrose.pve.economy.EconomyChat;
import org.xrose.pve.economy.ScoreboardBalance;
import org.xrose.utils.ColorUtil;
import org.xrose.utils.inventory.InventoryUtil;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFonts;
import org.xrose.utils.text.ChatUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AutoBuyFeature extends Feature implements MinecraftContext {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   public final TextSetting maxPrice = this.register(new TextSetting("Max Price", "100000"));
   public final NumberSetting parserDiscount = this.register(new NumberSetting("Parser Discount", 5.0, 0.0, 80.0, 1.0, "%"));
   public final NumberSetting delay = this.register(new NumberSetting("Delay", 250.0, 100.0, 1500.0, 50.0, " ms"));
   public final BooleanSetting autoRefresh = this.register(new BooleanSetting("Auto Refresh", true));
   public final ButtonSetting openSelector = this.register(new ButtonSetting("Item Selector", "Open", () -> AutoBuyScreen.open()));
   public final ButtonSetting parseSelected = this.register(new ButtonSetting("Parse Selected", "Parse", this::startParser));
   public final ButtonSetting addHeld = this.register(new ButtonSetting("Add Held Item", "Add", this::addHeldItem));
   public final ButtonSetting removeHeld = this.register(new ButtonSetting("Remove Held Item", "Remove", this::removeHeldItem));
   public final ButtonSetting showList = this.register(new ButtonSetting("Show List", "Show", this::printList));
   public final ButtonSetting clearList = this.register(new ButtonSetting("Clear List", "Clear", this::clearEntries));
   private final List<AutoBuyFeature.Entry> entries = new ArrayList<>();
   private final Set<String> selectedItems = new LinkedHashSet<>();
   private final List<AutoBuyFeature.Catalog> parserQueue = new ArrayList<>();
   private long nextActionAt;
   private AutoBuyFeature.Entry parserTarget;
   private long parserDeadline;
   private int parserWaitTicks;
   private int parserCompleted;
   private int parserTotal;
   private AutoBuyFeature.Entry lastAttemptedEntry;
   private ItemStack lastAttemptedStack = ItemStack.EMPTY;
   private final Map<String, Long> insufficientCooldown = new LinkedHashMap<>();
   private final List<AutoBuyFeature.HistoryEntry> purchaseHistory = new ArrayList<>();
   private long confirmationEnterTime = 0L;
   private long lastAntiAfkTime = System.currentTimeMillis();
   private float antiAfkDirection = 1.0F;
   private boolean antiAfkActive = false;
   private long antiAfkEndTime = 0L;
   private long lastAntiAfkActionTime = 0L;
   private int antiAfkStep = 0;
   private long expectAuctionReopenUntil = 0L;
   private long lastAhSendTime = 0L;
   private int antiAfkMoveDir = 0;
   private long antiAfkMoveEndTime = 0L;
   private float antiAfkBaseYaw = 0.0F;
   private float antiAfkBasePitch = 0.0F;
   private static final Type ENTRY_LIST_TYPE = (new TypeToken<List<AutoBuyFeature.Entry>>() {}).getType();

   public AutoBuyFeature() {
      super("AutoBuy", "Automatically buys selected items below the configured price", FeatureCategory.PLAYER, -1);
      this.load();
   }

   boolean isSelected(String name) {
      return this.selectedItems.contains(name);
   }

   void toggle(String name) {
      boolean wasSelected = this.selectedItems.contains(name);
      if (wasSelected) {
         this.selectedItems.remove(name);

         for (AutoBuyFeature.Catalog c : AutoBuyFeature.Catalog.values()) {
            if (c.displayName.equals(name)) {
               AutoBuyFeature.Entry tmp = AutoBuyFeature.Entry.from(c, 0L);
               this.entries.removeIf(e -> e.sameIdentity(tmp));
               break;
            }
         }
      } else {
         this.selectedItems.add(name);
      }

      this.save();
   }

   private boolean isSelectedEntry(AutoBuyFeature.Entry e) {
      boolean isCatalog = false;

      for (AutoBuyFeature.Catalog c : AutoBuyFeature.Catalog.values()) {
         AutoBuyFeature.Entry ce = AutoBuyFeature.Entry.from(c, 0L);
         if (e.sameIdentity(ce)) {
            isCatalog = true;
            if (this.selectedItems.contains(c.displayName)) {
               return true;
            }
         }
      }

      return !isCatalog;
   }

   int selectedCount() {
      return this.selectedItems.size();
   }

   Set<String> getSelectedNames() {
      return new LinkedHashSet<>(this.selectedItems);
   }

   public List<AutoBuyFeature.HistoryEntry> getPurchaseHistory() {
      return new ArrayList<>(this.purchaseHistory);
   }

   public void clearPurchaseHistory() {
      this.purchaseHistory.clear();
   }

   long getParsedPrice(String displayName) {
      String n = normalize(displayName);

      for (AutoBuyFeature.Entry e : this.entries) {
         if (e.name.equals(n)) {
            return e.maxPrice;
         }

         for (AutoBuyFeature.Catalog c : AutoBuyFeature.Catalog.values()) {
            if (c.displayName.equals(displayName)) {
               AutoBuyFeature.Entry ce = AutoBuyFeature.Entry.from(c, 0L);
               if (e.sameIdentity(ce)) {
                  return e.maxPrice;
               }
            }
         }
      }

      return 0L;
   }

   void setParsedPrice(String displayName, long price) {
      if (price > 0L) {
         for (AutoBuyFeature.Catalog c : AutoBuyFeature.Catalog.values()) {
            if (c.displayName.equals(displayName)) {
               AutoBuyFeature.Entry e = AutoBuyFeature.Entry.from(c, price);
               this.entries.removeIf(en -> en.sameIdentity(e));
               this.entries.add(e);
               this.save();
               ChatUtil.success("AutoBuy: цена для " + displayName + " -> $" + format(price));
               return;
            }
         }

         for (AutoBuyFeature.Entry e : this.entries) {
            if (e.name.equals(normalize(displayName))) {
               e.maxPrice = price;
               this.save();
               return;
            }
         }
      }
   }

   String maxPriceValue() {
      return this.maxPrice.getValue();
   }

   @Override
   protected void onDisable() {
      this.parserQueue.clear();
      this.parserTarget = null;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      boolean parsing = this.parserTarget != null;
      boolean parserPending = !this.parserQueue.isEmpty();
      if (parsing || parserPending || this.isEnabled() || this.antiAfkActive) {
         Minecraft client = mc;
         if (client.player != null && client.gameMode != null) {
            if (this.isEnabled() && this.isInventoryFull(client.player)) {
               this.setEnabled(false);
               ChatUtil.error("AutoBuy: инвентарь полон — выключаю");
            } else if (this.antiAfkActive) {
               if (System.currentTimeMillis() >= this.antiAfkEndTime) {
                  this.antiAfkActive = false;
                  this.lastAntiAfkTime = System.currentTimeMillis();
                  this.nextActionAt = System.currentTimeMillis() + 500L;
                  this.expectAuctionReopenUntil = System.currentTimeMillis() + 4000L;
                  this.lastAhSendTime = System.currentTimeMillis();

                  try {
                     if (client.player != null) {
                        client.player.connection.sendCommand("ah");
                     }
                  } catch (Exception var25) {
                  }

                  ChatUtil.info("AutoBuy Anti-AFK завершён — возобновляю");
               } else {
                  if (System.currentTimeMillis() > this.antiAfkMoveEndTime) {
                     try {
                        client.options.keyUp.setDown(false);
                        client.options.keyDown.setDown(false);
                        client.options.keyLeft.setDown(false);
                        client.options.keyRight.setDown(false);
                     } catch (Exception var28) {
                     }
                  }

                  if (System.currentTimeMillis() - this.lastAntiAfkActionTime >= 50L) {
                     this.lastAntiAfkActionTime = System.currentTimeMillis();

                     try {
                        if (client.player != null) {
                           this.antiAfkStep++;
                           client.player.swing(InteractionHand.MAIN_HAND);

                           try {
                              client.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                           } catch (Exception var26) {
                           }
                        }
                     } catch (Exception var27) {
                     }
                  }
               }
            } else if (System.currentTimeMillis() >= this.nextActionAt) {
               if (this.isEnabled() && !parsing && !parserPending && System.currentTimeMillis() - this.lastAntiAfkTime > 50000L) {
                  this.antiAfkActive = true;
                  this.antiAfkEndTime = System.currentTimeMillis() + 3500L;
                  this.lastAntiAfkActionTime = System.currentTimeMillis();
                  this.antiAfkStep = 0;

                  try {
                     if (client.gui.screen() instanceof AbstractContainerScreen) {
                        client.player.closeContainer();
                     }
                  } catch (Exception var30) {
                  }

                  ChatUtil.info("AutoBuy Anti-AFK 3.5с — блокирую...");
                  this.antiAfkMoveDir = (this.antiAfkMoveDir + 1) % 4;
                  this.antiAfkMoveEndTime = System.currentTimeMillis() + 700L;
                  this.antiAfkBaseYaw = client.player.getYRot();
                  this.antiAfkBasePitch = client.player.getXRot();

                  try {
                     client.options.keyUp.setDown(this.antiAfkMoveDir == 0);
                     client.options.keyDown.setDown(this.antiAfkMoveDir == 1);
                     client.options.keyLeft.setDown(this.antiAfkMoveDir == 2);
                     client.options.keyRight.setDown(this.antiAfkMoveDir == 3);
                  } catch (Exception var29) {
                  }

                  this.nextActionAt = System.currentTimeMillis() + 3600L;
               } else {
                  if (this.expectAuctionReopenUntil != 0L) {
                     if (System.currentTimeMillis() > this.expectAuctionReopenUntil) {
                        this.expectAuctionReopenUntil = 0L;
                     } else {
                        boolean inAuction = false;
                        if (client.gui.screen() instanceof AbstractContainerScreen<?> s) {
                           String t = normalize(s.getTitle().getString());
                           if (isAuction(t) || isConfirmation(t)) {
                              inAuction = true;
                           }
                        }

                        if (!inAuction) {
                           if (System.currentTimeMillis() - this.lastAhSendTime > 1500L) {
                              try {
                                 client.player.connection.sendCommand("ah");
                                 this.lastAhSendTime = System.currentTimeMillis();
                              } catch (Exception var31) {
                              }

                              this.nextActionAt = System.currentTimeMillis() + 600L;
                              return;
                           }

                           return;
                        }

                        this.expectAuctionReopenUntil = 0L;
                     }
                  }

                  if (client.gui.screen() instanceof AbstractContainerScreen<?> confScr) {
                     String cT = normalize(confScr.getTitle().getString());
                     if (isConfirmation(cT)) {
                        if (this.confirmationEnterTime == 0L) {
                           this.confirmationEnterTime = System.currentTimeMillis();
                        } else if (System.currentTimeMillis() - this.confirmationEnterTime > 2500L) {
                           this.confirmationEnterTime = 0L;
                           ChatUtil.info("AutoBuy: покупка зависла — перезапуск /ah");

                           try {
                              client.player.closeContainer();
                           } catch (Exception var33) {
                           }

                           if (this.isEnabled()) {
                              try {
                                 client.player.connection.sendCommand("ah");
                                 this.lastAhSendTime = System.currentTimeMillis();
                                 this.expectAuctionReopenUntil = System.currentTimeMillis() + 4000L;
                              } catch (Exception var32) {
                              }
                           }

                           this.nextActionAt = System.currentTimeMillis() + 900L;
                           return;
                        }
                     } else {
                        this.confirmationEnterTime = 0L;
                     }
                  } else {
                     this.confirmationEnterTime = 0L;
                  }

                  if (!parsing && parserPending) {
                     if (client.gui.screen() instanceof AbstractContainerScreen<?> cScreen) {
                        String cTitle = normalize(cScreen.getTitle().getString());
                        if (isConfirmation(cTitle)) {
                           if (cScreen.getMenu().slots.size() > 1) {
                              InventoryUtil.leftClickSlot(1);
                              this.nextActionAt = this.confirmThrottle();
                           }

                           return;
                        }
                     }

                     this.startNextParserItem();
                  } else if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
                     String var40 = normalize(screen.getTitle().getString());
                     AbstractContainerMenu var44 = screen.getMenu();
                     if (isConfirmation(var40)) {
                        if (var44.slots.size() > 1) {
                           InventoryUtil.leftClickSlot(1);
                           this.nextActionAt = this.confirmThrottle();
                        }
                     } else if (parsing) {
                        this.tickParser(var44, var40);
                     } else if (isAuction(var40) && !this.entries.isEmpty()) {
                        this.insufficientCooldown.entrySet().removeIf(e -> System.currentTimeMillis() >= e.getValue());
                        int containerSlots = var44.slots.size() - 36;
                        int saleSlots = Math.min(45, containerSlots);

                        for (int index = 0; index < saleSlots; index++) {
                           ItemStack stack = ((Slot)var44.slots.get(index)).getItem();
                           if (!stack.isEmpty() && !isUnavailable(stack)) {
                              long totalPrice = getTotalPrice(stack);
                              if (totalPrice > 0L) {
                                 long unitPrice = totalPrice / Math.max(1, stack.getCount());
                                 OptionalLong bal = ScoreboardBalance.read(client.player);
                                 if (!bal.isPresent() || totalPrice <= bal.getAsLong()) {
                                    for (AutoBuyFeature.Catalog cat : AutoBuyFeature.Catalog.values()) {
                                       if (this.isSelected(cat.displayName)) {
                                          AutoBuyFeature.Entry catEntry = null;

                                          for (AutoBuyFeature.Entry e : this.entries) {
                                             AutoBuyFeature.Entry ce = AutoBuyFeature.Entry.from(cat, 0L);
                                             if (e.sameIdentity(ce)) {
                                                catEntry = e;
                                                break;
                                             }
                                          }

                                          if (catEntry != null) {
                                             String ek = this.entryKey(catEntry);
                                             Long cd = this.insufficientCooldown.get(ek);
                                             if (cd == null || System.currentTimeMillis() >= cd) {
                                                if (cd != null && System.currentTimeMillis() >= cd) {
                                                   this.insufficientCooldown.remove(ek);
                                                }

                                                if (unitPrice <= catEntry.maxPrice && catEntry.matches(stack)) {
                                                   this.lastAttemptedEntry = catEntry;
                                                   this.lastAttemptedStack = stack.copy();
                                                   InventoryUtil.leftClickSlot(index);
                                                   this.nextActionAt = this.throttle();
                                                   ChatUtil.success("AutoBuy bought " + catEntry.name + " for $" + format(unitPrice));
                                                   return;
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        if (this.autoRefresh.getValue() && containerSlots > 49) {
                           InventoryUtil.leftClickSlot(49);
                           this.nextActionAt = this.throttle();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private long throttle() {
      return System.currentTimeMillis() + Math.max(380L, this.delay.getValue().longValue());
   }

   private long confirmThrottle() {
      return System.currentTimeMillis() + Math.max(280L, this.delay.getValue().longValue() / 2L);
   }

   private long parserDelay() {
      return Math.max(50L, this.delay.getValue().longValue() + 200L);
   }

   private String entryKey(AutoBuyFeature.Entry e) {
      return e.itemId + "|" + e.name + "|" + String.join(";", e.lore) + "|" + (e.headTexture == null ? "" : e.headTexture.hashCode()) + "|" + e.potionColor;
   }

   @EventTarget
   public void onPacketReceive(PacketReceiveEvent event) {
      if (event.getPhase() == PacketReceiveEvent.Phase.PRE) {
         String text = EconomyChat.incomingText(event.getPacket());
         if (text != null) {
            String norm = normalize(text);
            if (norm.contains("не хватает монет")
               || norm.contains("не хватает денег")
               || norm.contains("недостаточно монет")
               || norm.contains("недостаточно средств")
               || norm.contains("not enough money")
               || norm.contains("не хватает средств")) {
               ChatUtil.info("AutoBuy: недостаточно монет — пропускаю предмет");
               this.nextActionAt = System.currentTimeMillis() + 2500L;
               if (this.lastAttemptedEntry != null) {
                  this.insufficientCooldown.put(this.entryKey(this.lastAttemptedEntry), System.currentTimeMillis() + 15000L);
               }

               if (this.parserTarget != null) {
                  this.parserTarget = null;
                  this.parserWaitTicks = 0;
                  if (!this.parserQueue.isEmpty()) {
                     this.nextActionAt = System.currentTimeMillis() + this.parserDelay();
                  }
               }
            } else if (!norm.contains("успешно купили") && !norm.contains("вы успешно купили")) {
               if (norm.contains("товар уже купили")
                  || norm.contains("этот товар уже")
                  || norm.contains("уже купили")
                  || norm.contains("already bought")
                  || norm.contains("уже продан")
                  || norm.contains("лот уже продан")
                  || norm.contains("товар уже продан")) {
                  ChatUtil.info("AutoBuy: товар уже купили — отменяю и продолжаю");
                  this.nextActionAt = System.currentTimeMillis() + 1000L;
                  if (this.lastAttemptedEntry != null) {
                     this.insufficientCooldown.put(this.entryKey(this.lastAttemptedEntry), System.currentTimeMillis() + 7000L);
                     this.lastAttemptedEntry = null;
                  }

                  try {
                     if (mc.gui.screen() instanceof AbstractContainerScreen<?> cs) {
                        String ct = normalize(cs.getTitle().getString());
                        if (isConfirmation(ct)) {
                           mc.player.closeContainer();
                        } else if (isAuction(ct)) {
                           mc.player.closeContainer();
                        }
                     }
                  } catch (Exception var12) {
                  }

                  this.expectAuctionReopenUntil = System.currentTimeMillis() + 4000L;
                  this.lastAhSendTime = System.currentTimeMillis();
                  this.nextActionAt = System.currentTimeMillis() + 650L;
                  if (this.parserTarget != null) {
                     this.parserTarget = null;
                     this.parserWaitTicks = 0;
                     if (this.parserQueue.isEmpty()) {
                        ChatUtil.success("AutoBuy parser finished, updated " + this.parserCompleted + "/" + this.parserTotal);
                        this.closeAuctionIfOpen();
                     } else {
                        this.nextActionAt = System.currentTimeMillis() + this.parserDelay();
                     }
                  }
               }
            } else {
               if (this.lastAttemptedEntry != null) {
                  try {
                     ItemStack histStack = this.lastAttemptedStack != null && !this.lastAttemptedStack.isEmpty() ? this.lastAttemptedStack.copy() : null;
                     long histPrice = 0L;
                     if (histStack != null) {
                        histPrice = getTotalPrice(histStack);
                     }

                     if (histPrice == 0L && this.lastAttemptedEntry != null) {
                        histPrice = this.lastAttemptedEntry.maxPrice;
                     }

                     if (histStack == null) {
                        for (AutoBuyFeature.Catalog c : AutoBuyFeature.Catalog.values()) {
                           AutoBuyFeature.Entry ce = AutoBuyFeature.Entry.from(c, 0L);
                           if (ce.sameIdentity(this.lastAttemptedEntry)) {
                              histStack = c.stack().copy();
                              if (histPrice == 0L) {
                                 histPrice = ce.maxPrice;
                              }
                              break;
                           }
                        }
                     }

                     if (histStack == null && this.lastAttemptedEntry != null) {
                        try {
                           histStack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.getValue(Identifier.parse(this.lastAttemptedEntry.itemId)));
                        } catch (Exception var13) {
                        }
                     }

                     if (histStack != null && !histStack.isEmpty()) {
                        this.purchaseHistory.add(0, new AutoBuyFeature.HistoryEntry(histStack.copy(), histPrice));
                        if (this.purchaseHistory.size() > 12) {
                           this.purchaseHistory.remove(this.purchaseHistory.size() - 1);
                        }
                     }
                  } catch (Exception var14) {
                  }

                  this.lastAttemptedEntry = null;
                  this.lastAttemptedStack = ItemStack.EMPTY;
               }

               this.nextActionAt = System.currentTimeMillis() + 400L;
            }
         }
      }
   }

   @EventTarget
   public void onFinalGuiRender(FinalGuiRenderEvent event) {
      if (!this.purchaseHistory.isEmpty() && event.isRenderScreen()) {
         if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            String title = normalize(screen.getTitle().getString());
            if (isAuction(title)) {
               try {
                  AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor)screen;
                  int bgH = accessor.getImageHeight();
                  int sx = accessor.getLeftPos() - 22;
                  int sy = accessor.getTopPos() + 3;
                  int count = Math.min(this.purchaseHistory.size(), bgH / 18);
                  RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
                  Render2DUtil.beginFrame();
                  Render2DUtil.rect(sx - 2, sy, 20.0F, count * 18)
                     .color(Theme.Colors.BACKGROUND_SURFACE_S)
                     .radius(6.0F)
                     .shadow(ColorUtil.rgba(0, 0, 0, 40), 6.0F)
                     .draw();
                  Render2DUtil.rect(sx - 2, sy, 20.0F, count * 18).color(0).radius(6.0F).border(1.0F, Theme.Colors.OUTLINES_SMALL).draw();
                  Render2DUtil.flush();
                  GuiGraphicsExtractor ext = RenderContext.currentGuiGraphicsExtractor();

                  for (int i = 0; i < count; i++) {
                     int slotY = sy + i * 18;
                     AutoBuyFeature.HistoryEntry he = this.purchaseHistory.get(i);
                     ItemStack hs = he.stack;
                     if (he.price > 0L) {
                        String ps = "$" + String.format(Locale.US, "%,d", he.price).replace(',', '.');

                        try {
                           Render2DUtil.text(sx + 10 - mc.font.width(ps) / 4.0F, slotY - 5, 4.0F, ps).font(UiFonts.sfPro(600)).color(-10496).draw();
                        } catch (Exception var17) {
                        }
                     }

                     ext.item(hs, sx + 1, slotY + 1);
                     if (event.getMouseX() >= sx + 1 && event.getMouseX() < sx + 17 && event.getMouseY() >= slotY + 1 && event.getMouseY() < slotY + 17) {
                        try {
                           List<Component> lines = hs.getTooltipLines(TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL);
                           if (he.price > 0L) {
                              List<Component> copy = new ArrayList<>(lines);
                              copy.add(Component.literal("$" + String.format(Locale.US, "%,d", he.price).replace(',', '.') + " /шт"));
                              ext.setTooltipForNextFrame(mc.font, copy, Optional.empty(), event.getMouseX(), event.getMouseY());
                           } else {
                              ext.setTooltipForNextFrame(mc.font, lines, Optional.empty(), event.getMouseX(), event.getMouseY());
                           }
                        } catch (Exception ignored2) {
                           ext.setTooltipForNextFrame(mc.font, hs, event.getMouseX(), event.getMouseY());
                        }
                     }
                  }

                  RenderContext.exit2D();
               } catch (Exception var18) {
               }
            }
         }
      }
   }

   private boolean isInventoryFull(LocalPlayer p) {
      if (p.getInventory().getFreeSlot() != -1) {
         return false;
      }

      for (int i = 0; i < 36; i++) {
         if (p.getInventory().getItem(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void closeAuctionIfOpen() {
      try {
         if (mc.gui.screen() instanceof AbstractContainerScreen<?> s) {
            String t = normalize(s.getTitle().getString());
            if (isAuction(t) || isConfirmation(t)) {
               mc.player.closeContainer();
            }
         }
      } catch (Exception var3) {
      }
   }

   void startParser() {
      if (mc.player == null) {
         ChatUtil.error("AutoBuy: join a server first");
      } else {
         if (!this.isEnabled()) {
            this.setEnabled(true);
         }

         this.parserQueue.clear();

         for (AutoBuyFeature.Catalog item : AutoBuyFeature.Catalog.values()) {
            if (this.selectedItems.contains(item.displayName)) {
               this.parserQueue.add(item);
            }
         }

         if (this.parserQueue.isEmpty()) {
            ChatUtil.error("AutoBuy: select at least one item in the selector");
         } else {
            this.parserCompleted = 0;
            this.parserTotal = this.parserQueue.size();
            ChatUtil.info("AutoBuy parser: analyzing " + this.parserTotal + " items...");
            this.startNextParserItem();
         }
      }
   }

   private String parserSearchTerm(AutoBuyFeature.Catalog c) {
      if (c == AutoBuyFeature.Catalog.ENCHANTED_GOLDEN_APPLE) {
         return "зачарованное";
      } else {
         return c == AutoBuyFeature.Catalog.GOLDEN_APPLE ? "золотое яблоко" : searchName(c.displayName);
      }
   }

   private void startNextParserItem() {
      this.parserWaitTicks = 0;
      if (this.parserQueue.isEmpty()) {
         this.parserTarget = null;
         ChatUtil.success("AutoBuy parser finished, updated " + this.parserCompleted + "/" + this.parserTotal);
         this.closeAuctionIfOpen();
      } else {
         AutoBuyFeature.Catalog selected = this.parserQueue.remove(0);
         this.parserTarget = AutoBuyFeature.Entry.from(selected, 0L);
         this.parserDeadline = System.currentTimeMillis() + 6000L;
         mc.player.connection.sendCommand("ah search " + this.parserSearchTerm(selected));
         ChatUtil.info("AutoBuy [" + (this.parserCompleted + 1) + "/" + this.parserTotal + "] parsing " + selected.displayName + "...");
         this.nextActionAt = System.currentTimeMillis() + 350L;
      }
   }

   private void tickParser(AbstractContainerMenu menu, String title) {
      if (mc.player == null) {
         this.parserTarget = null;
         this.parserQueue.clear();
         ChatUtil.error("AutoBuy parser stopped");
      } else if (System.currentTimeMillis() > this.parserDeadline) {
         ChatUtil.error("AutoBuy: no prices found, next item...");
         this.parserTarget = null;
         this.parserWaitTicks = 0;
         if (this.parserQueue.isEmpty()) {
            ChatUtil.success("AutoBuy parser finished, updated " + this.parserCompleted + "/" + this.parserTotal);
            this.closeAuctionIfOpen();
         } else {
            this.nextActionAt = System.currentTimeMillis() + this.parserDelay();
         }
      } else if (isAuction(title)) {
         if (++this.parserWaitTicks >= 10) {
            int saleSlots = Math.min(45, menu.slots.size() - 36);
            List<Long> prices = new ArrayList<>();

            for (int index = 0; index < saleSlots; index++) {
               ItemStack stack = ((Slot)menu.slots.get(index)).getItem();
               if (!stack.isEmpty() && this.parserMatches(stack) && !isUnavailable(stack)) {
                  long total = getTotalPrice(stack);
                  if (total > 0L) {
                     prices.add(total / Math.max(1, stack.getCount()));
                  }
               }
            }

            if (!prices.isEmpty()) {
               prices.sort(Long::compareTo);
               long reference = prices.get(Math.min(2, prices.size() - 1));
               long minimumReliable = Math.max(1L, Math.round(reference * 0.75));
               long result = reference;

               for (long price : prices) {
                  if (price >= minimumReliable) {
                     result = price;
                     break;
                  }
               }

               double discount = 1.0 - this.parserDiscount.getValue() / 100.0;
               long discounted = Math.max(1L, Math.round(result * discount));
               this.parserTarget.maxPrice = discounted;
               this.entries.removeIf(entry -> entry.sameIdentity(this.parserTarget));
               this.entries.add(this.parserTarget);
               this.save();
               this.parserCompleted++;
               ChatUtil.success(
                  "AutoBuy market: $" + format(result) + "/pc, limit $" + format(discounted) + "/pc (-" + this.parserDiscount.getValue().intValue() + "%)"
               );
               this.parserTarget = null;
               this.parserWaitTicks = 0;
               if (this.parserQueue.isEmpty()) {
                  ChatUtil.success("AutoBuy parser finished, updated " + this.parserCompleted + "/" + this.parserTotal);
                  this.closeAuctionIfOpen();
               } else {
                  this.nextActionAt = System.currentTimeMillis() + this.parserDelay();
               }
            }
         }
      }
   }

   private boolean parserMatches(ItemStack stack) {
      if (this.parserTarget != null && !stack.isEmpty()) {
         if (!this.parserTarget.itemId.equals(itemId(stack))) {
            return false;
         }

         String expected = stripName(this.parserTarget.name);
         String actual = stripName(normalize(stack.getHoverName().getString()));
         boolean nameMatches = expected.isBlank() || actual.contains(expected) || expected.contains(actual);
         if (!nameMatches && expected.equals("чарка") && (actual.contains("зачарованное") || actual.contains("золотое") || actual.contains("чарка"))) {
            nameMatches = true;
         }

         if (!nameMatches && expected.equals("спавнер") && actual.contains("спавнер")) {
            nameMatches = true;
         }

         if (!nameMatches && expected.contains("крушителя") && actual.contains("крушителя")) {
            String[] pieces = new String[]{"шлем", "нагрудник", "поножи", "ботинки", "меч", "булава", "трезубец", "кирка", "арбалет"};

            for (String p : pieces) {
               if (expected.contains(p) && actual.contains(p)) {
                  nameMatches = true;
                  break;
               }
            }

            if (!nameMatches) {
               nameMatches = true;
            }
         }

         if (!nameMatches && this.parserTarget.lore.isEmpty()) {
            return false;
         }

         if (!this.parserTarget.enchantments.isEmpty() && !this.parserTarget.enchantments.equals(enchantments(stack))) {
            return false;
         }

         List<String> haveLore = relevantLore(stack);

         for (String need : this.parserTarget.lore) {
            String nNeed = normalize(need);
            if (!nNeed.isBlank()) {
               boolean found = false;

               for (String have : haveLore) {
                  if (have.contains(nNeed) || nNeed.contains(have) || stripName(have).contains(stripName(nNeed)) || stripName(nNeed).contains(stripName(have))) {
                     found = true;
                     break;
                  }
               }

               if (!found) {
                  String needSize = extractSize(nNeed);
                  if (needSize != null) {
                     for (String have : haveLore) {
                        if (have.contains(needSize) || normalize(have).contains(needSize)) {
                           found = true;
                           break;
                        }
                     }

                     if (!found && actual.contains(needSize)) {
                        found = true;
                     }
                  }

                  if (!found) {
                     return false;
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static String extractSize(String s) {
      Matcher m = Pattern.compile("\\d+\\s*x\\s*\\d+").matcher(s);
      return m.find() ? m.group().replaceAll("\\s+", "") : null;
   }

   void applySelection(String priceText) {
      long price = parsePrice(priceText);
      if (price <= 0L) {
         ChatUtil.error("AutoBuy: enter a valid max price");
      } else {
         this.maxPrice.setValue(String.valueOf(price));
         int addedCount = 0;

         for (AutoBuyFeature.Catalog item : AutoBuyFeature.Catalog.values()) {
            if (this.selectedItems.contains(item.displayName)) {
               AutoBuyFeature.Entry added = AutoBuyFeature.Entry.from(item, price);
               this.entries.removeIf(entry -> entry.sameIdentity(added));
               this.entries.add(added);
               addedCount++;
            }
         }

         if (addedCount == 0) {
            ChatUtil.error("AutoBuy: no items selected");
         } else {
            this.save();
            ChatUtil.success("AutoBuy applied " + addedCount + " items, limit $" + format(price) + "/pc");
         }
      }
   }

   private void addHeldItem() {
      if (mc.player != null) {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.isEmpty()) {
            ChatUtil.error("AutoBuy: hold an item in your main hand");
         } else {
            long price = this.parsePriceSetting();
            if (price <= 0L) {
               ChatUtil.error("AutoBuy: enter a valid max price");
            } else {
               AutoBuyFeature.Entry added = AutoBuyFeature.Entry.from(stack, price);
               this.entries.removeIf(entry -> entry.sameIdentity(added));
               this.entries.add(added);
               this.save();
               ChatUtil.success("AutoBuy added " + added.name + ", limit $" + format(price) + "/pc");
            }
         }
      }
   }

   private void removeHeldItem() {
      if (mc.player != null) {
         ItemStack stack = mc.player.getMainHandItem();
         if (stack.isEmpty()) {
            ChatUtil.error("AutoBuy: hold an item in your main hand");
         } else {
            AutoBuyFeature.Entry held = AutoBuyFeature.Entry.from(stack, 0L);
            boolean removed = this.entries.removeIf(entry -> entry.sameIdentity(held));
            if (removed) {
               this.save();
               ChatUtil.success("AutoBuy item removed");
            } else {
               ChatUtil.error("AutoBuy: item is not in the list");
            }
         }
      }
   }

   private void printList() {
      if (this.entries.isEmpty()) {
         ChatUtil.error("AutoBuy: list is empty");
      } else {
         ChatUtil.info("AutoBuy purchase list:");

         for (AutoBuyFeature.Entry entry : this.entries) {
            ChatUtil.entry(":link:", entry.name, "$" + format(entry.maxPrice) + "/pc");
         }
      }
   }

   private void clearEntries() {
      this.entries.clear();
      this.save();
      ChatUtil.success("AutoBuy list cleared");
   }

   private long parsePriceSetting() {
      return parsePrice(this.maxPrice.getValue());
   }

   private static long parsePrice(String value) {
      try {
         return Long.parseLong(value.replaceAll("[^0-9]", ""));
      } catch (NumberFormatException ignored) {
         return 0L;
      }
   }

   private static String itemId(ItemStack stack) {
      Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? stack.getItem().toString() : id.toString();
   }

   private static String normalize(String value) {
      return value.replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
   }

   private static String stripName(String value) {
      return value.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
   }

   static String stripDecoration(String value) {
      return normalize(value)
         .replace("[★]", " ")
         .replace("[⚡]", " ")
         .replace("xxx", " ")
         .replace("★", " ")
         .replace("⚡", " ")
         .replace("[", " ")
         .replace("]", " ")
         .replaceAll("\\s+", " ")
         .trim();
   }

   static String searchName(String name) {
      return normalize(name).replaceAll("(?i)xxx", " ").replaceAll("[^\\p{L}\\p{N}\\s\\-]+", " ").replaceAll("\\s+", " ").trim();
   }

   private static boolean isAuction(String title) {
      return title.contains("аукцион") || title.contains("auction") || title.contains("маркет") || title.contains("поиск:") || title.contains("search:");
   }

   private static boolean isConfirmation(String title) {
      return title.contains("подтверждение покупки") || title.contains("подозрительная цена") || title.contains("confirm purchase");
   }

   private static List<String> relevantLore(ItemStack stack) {
      List<String> result = new ArrayList<>();
      ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
      if (lore == null) {
         return result;
      }

      for (Component line : lore.lines()) {
         String value = normalize(line.getString());
         if (!value.isBlank()
            && !value.contains("$")
            && !value.contains("цена")
            && !value.contains("price")
            && !value.contains("продавец")
            && !value.contains("seller")
            && !value.contains("истекает")
            && !value.contains("нажмите")
            && !value.contains("купить")) {
            result.add(value);
         }
      }

      return result;
   }

   private static Map<String, Integer> enchantments(ItemStack stack) {
      Map<String, Integer> result = new LinkedHashMap<>();
      ItemEnchantments component = (ItemEnchantments)stack.get(DataComponents.ENCHANTMENTS);
      if (component == null) {
         return result;
      }

      for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Holder<Enchantment>> entry : component.entrySet()) {
         Holder<Enchantment> holder = (Holder<Enchantment>)entry.getKey();
         String key = holder.unwrapKey().map(key2 -> key2.identifier().getPath()).orElse(holder.toString());
         result.put(key, entry.getIntValue());
      }

      return result;
   }

   private static boolean isUnavailable(ItemStack stack) {
      ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
      if (lore == null) {
         return false;
      }

      for (Component line : lore.lines()) {
         String text = normalize(line.getString());
         if (text.contains("нажмите, чтобы забрать") || text.contains("товар не актуален")) {
            return true;
         }
      }

      return false;
   }

   private static long getTotalPrice(ItemStack stack) {
      ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
      if (lore == null) {
         return 0L;
      }

      for (Component line : lore.lines()) {
         String text = normalize(line.getString());
         if (text.contains("$")
            || text.contains("цена")
            || text.contains("price")
            || text.contains("стоимость")
            || text.contains("монет")
            || text.contains("coins")
            || text.contains("cost")) {
            int colon = text.lastIndexOf(58);
            int dollar = text.lastIndexOf(36);
            int start = colon >= 0 ? colon + 1 : (dollar >= 0 ? dollar + 1 : 0);
            String digits = text.substring(start).replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
               digits = text.replaceAll("[^0-9]", "");
               if (digits.isEmpty()) {
                  continue;
               }
            }

            if (!digits.isEmpty()) {
               try {
                  return Long.parseLong(digits);
               } catch (NumberFormatException ignored) {
                  return 0L;
               }
            }
         }
      }

      long fallback = 0L;

      for (Component line : lore.lines()) {
         String digits = line.getString().replaceAll("[^0-9]", "");
         if (!digits.isEmpty()) {
            try {
               long v = Long.parseLong(digits);
               if (v > 1000L && v < 1000000000L && v > fallback) {
                  fallback = v;
               }
            } catch (Exception var11) {
            }
         }
      }

      return fallback;
   }

   private static String format(long value) {
      return String.format(Locale.US, "%,d", value).replace(',', '.');
   }

   private Path configPath() {
      return FabricLoader.getInstance().getConfigDir().resolve("xrose-autobuy.json");
   }

   private void load() {
      this.entries.clear();
      this.selectedItems.clear();
      Path path = this.configPath();
      if (Files.exists(path)) {
         try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonObject()) {
               JsonObject root = element.getAsJsonObject();
               if (root.has("selected") && root.get("selected").isJsonArray()) {
                  for (JsonElement e : root.getAsJsonArray("selected")) {
                     this.selectedItems.add(e.getAsString());
                  }
               }

               if (root.has("entries") && root.get("entries").isJsonArray()) {
                  List<AutoBuyFeature.Entry> loaded = (List<AutoBuyFeature.Entry>)GSON.fromJson(root.getAsJsonArray("entries"), ENTRY_LIST_TYPE);
                  if (loaded != null) {
                     this.entries.addAll(loaded);
                     return;
                  }
               }
            }
         } catch (Exception exception) {
            LoggerFactory.getLogger("AutoBuy").error("Failed to load AutoBuy config", exception);
         }
      }
   }

   private void save() {
      try {
         Path path = this.configPath();
         Files.createDirectories(path.getParent());
         JsonObject root = new JsonObject();
         JsonArray selected = new JsonArray();

         for (String name : this.selectedItems) {
            selected.add(name);
         }

         root.add("selected", selected);
         root.add("entries", GSON.toJsonTree(this.entries));

         try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
         }
      } catch (Exception exception) {
         LoggerFactory.getLogger("AutoBuy").error("Failed to save AutoBuy config", exception);
      }
   }

   enum Catalog {
      POPPER_POTION("[★] Хлопушка", Items.SPLASH_POTION),
      HOLY_WATER("[★] Святая вода", Items.SPLASH_POTION),
      RAGE_POTION("[★] Зелье Гнева", Items.SPLASH_POTION),
      PALLADIN_POTION("[★] Зелье Палладина", Items.SPLASH_POTION),
      ASSASSIN_POTION("[★] Зелье Ассасина", Items.SPLASH_POTION),
      RADIATION_POTION("[★] Зелье Радиации", Items.SPLASH_POTION),
      SLEEPING_PILL("[★] Снотворное", Items.SPLASH_POTION),
      TALISMAN_CRUSHER("[★] Талисман Крушителя", Items.TOTEM_OF_UNDYING),
      TALISMAN_DISCORD("[★] Талисман Раздора", Items.TOTEM_OF_UNDYING),
      TALISMAN_TYRANT("[★] Талисман Тирана", Items.TOTEM_OF_UNDYING),
      TALISMAN_RAGE("[★] Талисман Ярости", Items.TOTEM_OF_UNDYING),
      TALISMAN_WHIRLWIND("[★] Талисман Вихря", Items.TOTEM_OF_UNDYING),
      TALISMAN_GLOOM("[★] Талисман Мрака", Items.TOTEM_OF_UNDYING),
      TALISMAN_DEMON("[★] Талисман Демона", Items.TOTEM_OF_UNDYING),
      TALISMAN_PUNISHER("[★] Талисман Карателя xxx", Items.TOTEM_OF_UNDYING),
      SPHERE_CHAOS("[★] Сфера Хаоса", Items.PLAYER_HEAD),
      SPHERE_SATYR("[★] Сфера Сатира", Items.PLAYER_HEAD),
      SPHERE_BEAST("[★] Сфера Бестии", Items.PLAYER_HEAD),
      SPHERE_ARES("[★] Сфера Ареса", Items.PLAYER_HEAD),
      SPHERE_HYDRA("[★] Сфера Гидры", Items.PLAYER_HEAD),
      SPHERE_ICARUS("[★] Сфера Икара", Items.PLAYER_HEAD),
      SPHERE_ERIS("[★] Сфера Эрида", Items.PLAYER_HEAD),
      SPHERE_TITAN("[★] Сфера Титана", Items.PLAYER_HEAD),
      TRAP("[★] Трапка", Items.NETHERITE_SCRAP, "каст: нерушимая клетка"),
      CLEAR_DUST("[★] Явная пыль", Items.SUGAR, "каст: световая вспышка"),
      SNOWBALL("[★] Снежок заморозка", Items.SNOWBALL, "каст: ледяная сфера"),
      GOD_AURA("[★] Божья аура", Items.PHANTOM_MEMBRANE, "каст: божественная аура"),
      DISORIENTATION("[★] Дезориентация", Items.ENDER_EYE, "каст: звуковая волна"),
      STRATUM("[★] Пласт", Items.DRIED_KELP, "каст: нерушимая стена"),
      FIERY_BALL("[★] Огненный шар", Items.FIRE_CHARGE, "каст: огненный шар"),
      CRUSHER_SWORD("xxx Меч Крушителя xxx", Items.NETHERITE_SWORD, "[★] оригинальный предмет"),
      CRUSHER_MACE("xxx Булава Крушителя xxx", Items.MACE, "[★] оригинальный предмет"),
      CRUSHER_TRIDENT("xxx Трезубец Крушителя xxx", Items.TRIDENT, "[★] оригинальный предмет"),
      CRUSHER_PICKAXE("xxx Кирка Крушителя xxx", Items.NETHERITE_PICKAXE, "[★] оригинальный предмет"),
      CRUSHER_CROSSBOW("xxx Арбалет Крушителя xxx", Items.CROSSBOW, "[★] оригинальный предмет"),
      CRUSHER_HELMET("xxx Шлем Крушителя xxx", Items.NETHERITE_HELMET),
      CRUSHER_CHESTPLATE("xxx Нагрудник Крушителя xxx", Items.NETHERITE_CHESTPLATE),
      CRUSHER_LEGGINGS("xxx Поножи Крушителя xxx", Items.NETHERITE_LEGGINGS),
      CRUSHER_BOOTS("xxx Ботинки Крушителя xxx", Items.NETHERITE_BOOTS),
      PICK_SPHERES("[★] Отмычка к Сферам", Items.TRIPWIRE_HOOK, "с сферами"),
      GOD_TOUCH("[★] Божье касание", Items.GOLDEN_PICKAXE, "может добыть спавнер"),
      POWER_STRIKE("[★] Мощный удар", Items.GOLDEN_PICKAXE, "может разрушить бедрок"),
      MIST_COMMON("Обычный мист", Items.CAMPFIRE, "уровень лута: обычный"),
      MIST_RICH("Богатый мист", Items.CAMPFIRE, "уровень лута: богатый"),
      MIST_LEGENDARY("Легендарный мист", Items.SOUL_CAMPFIRE, "уровень лута: легендарный"),
      SKIN_INEVITABLE("[★] Неизбежный скин", Items.PAPER, "получаете неизбежный скин"),
      SKIN_DRAGON("[★] Драконий скин", Items.PAPER, "получаете драконий скин"),
      CHUNK_LOADER_1("[★] Прогрузчик чанков [1x1]", Items.STRUCTURE_BLOCK, "прогружаемой области (1x1)"),
      CHUNK_LOADER_3("[★] Прогрузчик чанков [3x3]", Items.STRUCTURE_BLOCK, "прогружаемой области (3x3)"),
      CHUNK_LOADER_5("[★] Прогрузчик чанков [5x5]", Items.STRUCTURE_BLOCK, "прогружаемой области (5x5)"),
      REGION_25("[★] Регион 25x25", Items.CHAIN_COMMAND_BLOCK, "размер: 25x25 блоков"),
      AIRDROP("[★] Аирдроп", Items.REDSTONE_TORCH, "призыва аирдропа"),
      BLOCK_DAMAGER("[★] Блок дамагер", Items.JIGSAW, "каст: нанесение урона"),
      FLY_MODIFIER("[⚡] Модификатор полёта", Items.FEATHER, "доступ к /fly"),
      FIX_MODIFIER("[⚡] Модификатор починки", Items.AMETHYST_SHARD, "доступ к /fix"),
      PRIVILEGE_KEY("[★] Ключ от кейса с Привилегиями", Items.TRIAL_KEY, "открывает: кейс с привилегиями"),
      TOKEN_KEY("[★] Ключ от кейса с Токенами", Items.TRIAL_KEY, "открывает: кейс с токенами"),
      WHITE_TNT("[★] Вайт", Items.TNT, "в 10 раз сильнее обычного"),
      BLACK_TNT("[★] Блэк", Items.TNT, "способен взорвать обсидиан"),
      BLOOD_ARROW("Кровавая стрела", Items.TIPPED_ARROW),
      FROST_ARROW("Стрела обледенения", Items.TIPPED_ARROW),
      AGONY_ARROW("Мучительная стрела", Items.TIPPED_ARROW),
      GOLDEN_APPLE("Золотое яблоко", Items.GOLDEN_APPLE),
      ENCHANTED_GOLDEN_APPLE("Чарка", Items.ENCHANTED_GOLDEN_APPLE),
      SPAWNER("Рассадник монстров", Items.SPAWNER),
      EMERALD_ORE("Изумрудная руда", Items.EMERALD_ORE),
      DRAGON_HEAD("Голова дракона", Items.DRAGON_HEAD),
      DRAGON_EGG("Яйцо дракона", Items.DRAGON_EGG),
      ELYTRA("Элитры", Items.ELYTRA),
      BEACON("Маяк", Items.BEACON),
      VILLAGER_SPAWN_EGG("Яйцо призыва крестьянина", Items.VILLAGER_SPAWN_EGG),
      ENDERMAN_SPAWN_EGG("Яйцо призыва эндермена", Items.ENDERMAN_SPAWN_EGG),
      NETHERITE_UPGRADE("Отделка незеритовая", Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
      DIAMOND("Алмаз", Items.DIAMOND),
      NETHER_STAR("Звезда Незера", Items.NETHER_STAR),
      ANCIENT_DEBRIS("Древние обломки", Items.ANCIENT_DEBRIS),
      NETHERITE_INGOT("Незеритовый слиток", Items.NETHERITE_INGOT);

      final String displayName;
      private final Item base;
      private ItemStack item;
      private final String[] requiredLore;

      Catalog(String displayName, Item item, String... requiredLore) {
         this.displayName = displayName;
         this.base = item;
         this.requiredLore = requiredLore;
      }

      ItemStack stack() {
         if (this.item != null) {
            return this.item;
         }

         ItemStack s = new ItemStack(this.base);
         Integer col = this.potionColor();
         if (col != null && (s.is(Items.SPLASH_POTION) || s.is(Items.TIPPED_ARROW))) {
            s.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(col), List.of(), Optional.empty()));
         }

         String skin = this.headTexture();
         if (skin != null && s.is(Items.PLAYER_HEAD)) {
            try {
               ImmutableMultimap<String, Property> props = ImmutableMultimap.of("textures", new Property("textures", skin));
               PropertyMap map = new PropertyMap(props);
               GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(skin.getBytes(StandardCharsets.UTF_8)), this.name().toLowerCase(Locale.ROOT), map);
               s.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            } catch (Exception var7) {
            }
         }

         this.item = s;
         return this.item;
      }

      Integer potionColor() {
         return switch (this) {
            case POPPER_POTION -> 16711680;
            case HOLY_WATER -> 16777215;
            case RAGE_POTION -> 10040115;
            case PALLADIN_POTION -> 65535;
            case ASSASSIN_POTION -> 3355443;
            case RADIATION_POTION -> 3329330;
            case SLEEPING_PILL -> 4737096;
            case BLOOD_ARROW -> 9109504;
            case FROST_ARROW -> 11393254;
            case AGONY_ARROW -> 8019194;
            default -> null;
         };
      }

      String headTexture() {
         return switch (this) {
            case SPHERE_CHAOS -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
            case SPHERE_SATYR -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
            case SPHERE_BEAST -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
            case SPHERE_ARES -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSI6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
            case SPHERE_HYDRA -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
            case SPHERE_ICARUS -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODU4MjQ5MSwKICAicHJvZmlsZUlkIiA6ICJhZWNkODIxZTQyYzE0ZDJlOThmNTA1OTg1MWI5OWMzNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSb2RyaVgyMDc1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M2ODAzZTZkNTY2N2EyZDYxMDYyOGJjM2IzMmY4NjNjZGE0OTVjNDY1NjE2ZGU2NTVjYjMyOTkzM2I2MWFmNzciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
            case SPHERE_ERIS -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
            case SPHERE_TITAN -> "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM1NDQ1NTE5MiwKICAicHJvZmlsZUlkIiA6ICJkOTcwYzEzZTM4YWI0NzlhOTY1OGM1ZDQ1MjZkMTM0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmltcHlMYWNlODUxMjciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
            default -> null;
         };
      }

      // $VF: synthetic method
      private static AutoBuyFeature.Catalog[] $values() {
         return new AutoBuyFeature.Catalog[]{
            POPPER_POTION,
            HOLY_WATER,
            RAGE_POTION,
            PALLADIN_POTION,
            ASSASSIN_POTION,
            RADIATION_POTION,
            SLEEPING_PILL,
            TALISMAN_CRUSHER,
            TALISMAN_DISCORD,
            TALISMAN_TYRANT,
            TALISMAN_RAGE,
            TALISMAN_WHIRLWIND,
            TALISMAN_GLOOM,
            TALISMAN_DEMON,
            TALISMAN_PUNISHER,
            SPHERE_CHAOS,
            SPHERE_SATYR,
            SPHERE_BEAST,
            SPHERE_ARES,
            SPHERE_HYDRA,
            SPHERE_ICARUS,
            SPHERE_ERIS,
            SPHERE_TITAN,
            TRAP,
            CLEAR_DUST,
            SNOWBALL,
            GOD_AURA,
            DISORIENTATION,
            STRATUM,
            FIERY_BALL,
            CRUSHER_SWORD,
            CRUSHER_MACE,
            CRUSHER_TRIDENT,
            CRUSHER_PICKAXE,
            CRUSHER_CROSSBOW,
            CRUSHER_HELMET,
            CRUSHER_CHESTPLATE,
            CRUSHER_LEGGINGS,
            CRUSHER_BOOTS,
            PICK_SPHERES,
            GOD_TOUCH,
            POWER_STRIKE,
            MIST_COMMON,
            MIST_RICH,
            MIST_LEGENDARY,
            SKIN_INEVITABLE,
            SKIN_DRAGON,
            CHUNK_LOADER_1,
            CHUNK_LOADER_3,
            CHUNK_LOADER_5,
            REGION_25,
            AIRDROP,
            BLOCK_DAMAGER,
            FLY_MODIFIER,
            FIX_MODIFIER,
            PRIVILEGE_KEY,
            TOKEN_KEY,
            WHITE_TNT,
            BLACK_TNT,
            BLOOD_ARROW,
            FROST_ARROW,
            AGONY_ARROW,
            GOLDEN_APPLE,
            ENCHANTED_GOLDEN_APPLE,
            SPAWNER,
            EMERALD_ORE,
            DRAGON_HEAD,
            DRAGON_EGG,
            ELYTRA,
            BEACON,
            VILLAGER_SPAWN_EGG,
            ENDERMAN_SPAWN_EGG,
            NETHERITE_UPGRADE,
            DIAMOND,
            NETHER_STAR,
            ANCIENT_DEBRIS,
            NETHERITE_INGOT
         };
      }
   }

   private static final class Entry {
      String itemId;
      String name;
      List<String> lore = new ArrayList<>();
      Map<String, Integer> enchantments = new LinkedHashMap<>();
      long maxPrice;
      String headTexture;
      Integer potionColor;

      static AutoBuyFeature.Entry from(ItemStack stack, long maxPrice) {
         AutoBuyFeature.Entry entry = new AutoBuyFeature.Entry();
         entry.itemId = AutoBuyFeature.itemId(stack);
         entry.name = AutoBuyFeature.normalize(stack.getHoverName().getString());
         entry.lore = AutoBuyFeature.relevantLore(stack);
         entry.enchantments = AutoBuyFeature.enchantments(stack);
         entry.maxPrice = maxPrice;

         try {
            ResolvableProfile profile = (ResolvableProfile)stack.get(DataComponents.PROFILE);
            if (profile != null && profile.partialProfile() != null) {
               Collection<Property> props = profile.partialProfile().properties().get("textures");
               if (!props.isEmpty()) {
                  entry.headTexture = props.iterator().next().value();
               }
            }
         } catch (Exception var7) {
         }

         try {
            PotionContents pc = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
            if (pc != null) {
               entry.potionColor = (Integer)pc.customColor().orElse(null);
            }
         } catch (Exception var6) {
         }

         return entry;
      }

      static AutoBuyFeature.Entry from(AutoBuyFeature.Catalog catalog, long maxPrice) {
         AutoBuyFeature.Entry entry = new AutoBuyFeature.Entry();
         entry.itemId = AutoBuyFeature.itemId(catalog.stack());
         entry.name = AutoBuyFeature.normalize(catalog.displayName);
         List<String> normLore = new ArrayList<>();

         for (String s : catalog.requiredLore) {
            String n = AutoBuyFeature.normalize(s);
            if (!n.isBlank()) {
               normLore.add(n);
            }
         }

         entry.lore = normLore;
         entry.maxPrice = maxPrice;
         entry.headTexture = catalog.headTexture();
         entry.potionColor = catalog.potionColor();
         return entry;
      }

      boolean matches(ItemStack stack) {
         if (!this.itemId.equals(AutoBuyFeature.itemId(stack))) {
            return false;
         }

         String expected = AutoBuyFeature.stripDecoration(this.name);
         String actual = AutoBuyFeature.stripDecoration(AutoBuyFeature.normalize(stack.getHoverName().getString()));
         boolean nameOk = !expected.isBlank() && !actual.isBlank() && (actual.contains(expected) || expected.contains(actual));
         if (!nameOk) {
            if (!expected.equals("чарка") || !actual.contains("зачарованное") && !actual.contains("золотое") && !actual.contains("чарка")) {
               if (expected.equals("спавнер") && actual.contains("спавнер")) {
                  nameOk = true;
               } else if (expected.contains("крушителя") && actual.contains("крушителя")) {
                  nameOk = true;
               }
            } else {
               nameOk = true;
            }
         }

         if (!nameOk) {
            return false;
         }

         if (!this.enchantments.isEmpty() && !this.enchantments.equals(AutoBuyFeature.enchantments(stack))) {
            return false;
         }

         if (this.headTexture != null) {
            String actualHead = null;

            try {
               ResolvableProfile profile = (ResolvableProfile)stack.get(DataComponents.PROFILE);
               if (profile != null && profile.partialProfile() != null) {
                  Collection<Property> props = profile.partialProfile().properties().get("textures");
                  if (!props.isEmpty()) {
                     actualHead = props.iterator().next().value();
                  }
               }
            } catch (Exception var14) {
            }

            if (actualHead == null || !actualHead.equals(this.headTexture)) {
               return false;
            }
         }

         if (this.potionColor != null) {
            Integer actualColor = null;

            try {
               PotionContents pc = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
               if (pc != null) {
                  actualColor = (Integer)pc.customColor().orElse(null);
               }
            } catch (Exception var13) {
            }

            if (!this.potionColor.equals(actualColor)) {
               return false;
            }
         }

         List<String> haveLore = AutoBuyFeature.relevantLore(stack);

         for (String need : this.lore) {
            String nNeed = AutoBuyFeature.normalize(need);
            if (!nNeed.isBlank()) {
               boolean found = false;

               for (String have : haveLore) {
                  if (have.contains(nNeed)
                     || nNeed.contains(have)
                     || AutoBuyFeature.stripName(have).contains(AutoBuyFeature.stripName(nNeed))
                     || AutoBuyFeature.stripName(nNeed).contains(AutoBuyFeature.stripName(have))) {
                     found = true;
                     break;
                  }
               }

               if (!found) {
                  String needSize = AutoBuyFeature.extractSize(nNeed);
                  if (needSize != null) {
                     for (String have : haveLore) {
                        if (have.contains(needSize)) {
                           found = true;
                           break;
                        }
                     }

                     if (!found && actual.contains(needSize)) {
                        found = true;
                     }
                  }

                  if (!found) {
                     return false;
                  }
               }
            }
         }

         return true;
      }

      boolean sameIdentity(AutoBuyFeature.Entry other) {
         return this.itemId.equals(other.itemId)
            && this.name.equals(other.name)
            && this.lore.equals(other.lore)
            && this.enchantments.equals(other.enchantments)
            && Objects.equals(this.headTexture, other.headTexture)
            && Objects.equals(this.potionColor, other.potionColor);
      }
   }

   static final class HistoryEntry {
      final ItemStack stack;
      final long price;

      HistoryEntry(ItemStack stack, long price) {
         this.stack = stack;
         this.price = price;
      }
   }
}

