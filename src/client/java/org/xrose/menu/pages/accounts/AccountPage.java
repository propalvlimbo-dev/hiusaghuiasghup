package org.xrose.menu.pages.accounts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.xrose.menu.core.MenuConfigStore;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.menu.core.MenuPage;
import org.xrose.menu.i18n.MenuText;
import org.xrose.menu.ui.CardGrid;
import org.xrose.menu.ui.Component;
import org.xrose.menu.ui.PageComponent;
import org.xrose.menu.ui.controls.IconButton;
import org.xrose.menu.ui.controls.InputComponent;
import org.xrose.menu.ui.popups.ModalDialog;
import org.xrose.utils.AccountSwitcher;
import org.xrose.utils.render.Textures;
import org.xrose.utils.render.Theme;
import org.xrose.utils.render.gui.Render2DUtil;
import org.xrose.utils.render.gui.UiFontStyle;
import org.xrose.utils.render.gui.UiFonts;
import org.xrose.utils.text.NameProtectUtil;

public final class AccountPage extends PageComponent {
   private static final int ROW_STEP = 73;
   private static final int DROPDOWN_WIDTH = 108;
   private static final int DROPDOWN_HEIGHT = 36;
   private static final int DROPDOWN_PADDING_LEFT = 14;
   private static final int DROPDOWN_PADDING_RIGHT = 10;
   private static final int PLUS_BOX = 24;
   private static final int PLUS_ICON = 16;
   private static final int CONTROLS_RIGHT = 992;
   private static final int CONTROLS_GAP = 8;
   private static final int CONTROLS_ROW_Y = 104;
   private static final int PLUS_X = 968;
   private static final float PLUS_Y = 110.0F;
   private static final int DROPDOWN_X = 852;
   private static final String[] NICK_PREFIXES = new String[]{
      "Shadow",
      "Frost",
      "Void",
      "Pixel",
      "Aqua",
      "Night",
      "Storm",
      "Ember",
      "Ghost",
      "Nova",
      "Cyber",
      "Lunar",
      "Rapid",
      "Toxic",
      "Magma",
      "Blaze",
      "Astro",
      "Neon",
      "Grim",
      "Hyper",
      "Iron",
      "Zero",
      "Drako",
      "Mystic",
      "Silent",
      "Wicked",
      "Prime",
      "Retro",
      "Sour",
      "Vex"
   };
   private static final String[] NICK_SUFFIXES = new String[]{
      "Byte",
      "Craft",
      "Rush",
      "Fox",
      "Rose",
      "Wing",
      "Strike",
      "Core",
      "Drift",
      "Zap",
      "Wolf",
      "Hawk",
      "Reign",
      "Spark",
      "Flux",
      "Dash",
      "Rage",
      "Snipe",
      "King",
      "Lord",
      "Punch",
      "Shot",
      "Fang",
      "Peak",
      "Vibe",
      "Loop",
      "Crypt",
      "Gaze",
      "Husk",
      "Riot"
   };
   private final List<Component> children = new ArrayList<>();
   private final List<AccountCard> cards = new ArrayList<>();
   private MenuPage displayedPage = MenuPage.NONE;
   private AccountCard selectedCard;
   private boolean oldestFirst;
   private final CardGrid grid = new CardGrid(188, 320, 73, 624);
   private int nextAvatarIndex;
   private String pendingNickname = "";
   private final InputComponent nicknameInput = new InputComponent(() -> this.pendingNickname, value -> this.pendingNickname = value)
      .placeholder("Type Nickname")
      .filter(val -> val.length() <= 16 && val.matches("^[a-zA-Z0-9_]*$"));
   private final IconButton addButton = new IconButton(Textures.Icons.CIRCLE_PLUS, 16, this::openModal);
   private final IconButton dicesButton = new IconButton(Textures.Icons.DICES, 16, () -> this.pendingNickname = this.randomNickname());
   private final ModalDialog createModal = this.createModal();

   public AccountPage() {
      this.loadAccounts();
      this.rebuildChildren();
      this.oldestFirst = MenuConfigStore.getBoolean("accountsOldestFirst", false);
      String selectedName = MenuConfigStore.getString("selectedAccount", "");
      this.selectedCard = this.cards.stream().filter(card -> card.name().equals(selectedName)).findFirst().orElse(this.cards.get(0));
   }

   private ModalDialog createModal() {
      return new ModalDialog(
            Textures.Icons.PLUS,
            "Create Account",
            new String[]{"Enter a new account nickname.", "Use only letters, numbers and underscores."},
            this.nicknameInput,
            this::saveModal,
            new ModalDialog.Extras() {
               @Override
               public void render(ModalDialog modal, float alpha) {
                  AccountPage.this.dicesButton.render(Minecraft.getInstance(), null);
               }

               @Override
               public boolean click(ModalDialog modal, int mouseX, int mouseY) {
                  return AccountPage.this.dicesButton.handleClick(mouseX, mouseY);
               }
            }
         )
         .inputRightInset(34);
   }

   private void loadAccounts() {
      JsonArray saved = MenuConfigStore.getArray("accounts");
      if (saved != null) {
         for (JsonElement element : saved) {
            if (element.isJsonObject()) {
               JsonObject account = element.getAsJsonObject();
               String name = account.has("name") ? account.get("name").getAsString() : "";
               if (!name.isEmpty() && name.matches("^[a-zA-Z0-9_]{3,16}$")) {
                  String activity = account.has("activity") ? account.get("activity").getAsString() : "Never played";
                  int avatar = account.has("avatar") ? account.get("avatar").getAsInt() : this.cards.size();
                  boolean pinned = account.has("pinned") && account.get("pinned").getAsBoolean();
                  this.cards.add(new AccountCard(name, activity, avatar, pinned));
                  this.nextAvatarIndex = Math.max(this.nextAvatarIndex, avatar + 1);
               }
            }
         }
      }

      if (this.cards.isEmpty()) {
         this.cards.add(new AccountCard("XRose", "Playing now", 0));
         this.nextAvatarIndex = 1;
      }
   }

   private void persistAccounts() {
      JsonArray array = new JsonArray();

      for (AccountCard card : this.cards) {
         JsonObject account = new JsonObject();
         account.addProperty("name", card.name());
         account.addProperty("activity", card.activity());
         account.addProperty("avatar", card.avatarIndex());
         account.addProperty("pinned", card.pinned());
         array.add(account);
      }

      String selectedName = this.selectedCard != null ? this.selectedCard.name() : "";
      boolean oldest = this.oldestFirst;
      MenuConfigStore.save(data -> {
         data.add("accounts", array);
         data.addProperty("selectedAccount", selectedName);
         data.addProperty("accountsOldestFirst", oldest);
      });
   }

   @Override
   protected void onLayout() {
      this.displayedPage = this.state.displayPage();
      if (this.displayedPage != MenuPage.ACCOUNT_SWITCHER && this.createModal.isOpen()) {
         this.closeModal();
      }

      this.grid.update(this.cards.size());
      String activeName = activeProfileName();
      List<AccountCard> display = this.displayOrder();

      for (int index = 0; index < display.size(); index++) {
         AccountCard card = display.get(index);
         boolean selected = card == this.selectedCard;
         card.setSelected(selected);
         card.setShowGamepad(!selected || !card.name().equals(activeName));
         card.place(this, this.grid.x(index), this.grid.y(index), this.mouseX, this.mouseY, this.progress);
      }

      this.addButton.place(this, 968.0F, 110.0F, 24, this.mouseX, this.mouseY).alpha(this.progress);
      this.createModal.place(this, this.mouseX, this.mouseY, this.progress);
      this.dicesButton
         .place(
            this,
            this.createModal.contentX() + this.createModal.contentWidth() + 6,
            this.createModal.inputY() + (this.createModal.inputHeight() - 24) / 2.0F,
            24,
            this.mouseX,
            this.mouseY
         )
         .alpha(this.progress);
   }

   public void handleScroll(int mouseX, int mouseY, double vertical) {
      if (!this.createModal.isOpen()) {
         if (this.contains(mouseX, mouseY) && mouseY >= this.sy(168.0F)) {
            this.grid.scroll(vertical, this.cards.size());
         }
      }
   }

   @Override
   public boolean handleClick(int mouseX, int mouseY) {
      if (!this.contentContains(mouseX, mouseY)) {
         return false;
      }

      if (this.createModal.isOpen()) {
         return this.createModal.handleClick(mouseX, mouseY);
      }

      if (this.hit(mouseX, mouseY, 852.0F, 104.0F, 108.0F, 36.0F)) {
         this.oldestFirst = !this.oldestFirst;
         Collections.reverse(this.cards);
         this.persistAccounts();
         return true;
      }

      if (this.addButton.handleClick(mouseX, mouseY)) {
         return true;
      }

      if (mouseY < this.sy(168.0F)) {
         return true;
      }

      for (AccountCard card : this.cards) {
         if (card.isPinAt(mouseX, mouseY)) {
            card.togglePinned();
            this.persistAccounts();
            return true;
         }

         if (card.isDeleteAt(mouseX, mouseY) && this.cards.size() > 1) {
            this.cards.remove(card);
            this.rebuildChildren();
            if (this.selectedCard == card) {
               this.selectedCard = this.cards.get(0);
            }

            this.persistAccounts();
            return true;
         }

         if (card.isGamepadAt(mouseX, mouseY)) {
            this.selectedCard = card;
            this.persistAccounts();
            MenuOverlay.close(Minecraft.getInstance());
            AccountSwitcher.relogin(card.name());
            return true;
         }

         if (card.handleClick(mouseX, mouseY)) {
            this.selectedCard = card;
            AccountSwitcher.switchTo(card.name());
            this.persistAccounts();
            return true;
         }
      }

      return true;
   }

   private static String activeProfileName() {
      Minecraft mc = Minecraft.getInstance();
      return NameProtectUtil.protect(mc.player != null ? mc.player.getGameProfile().name() : mc.getUser().getName());
   }

   private List<AccountCard> displayOrder() {
      List<AccountCard> display = new ArrayList<>(this.cards);
      display.sort(Comparator.comparing(AccountCard::pinned).reversed().thenComparing(card -> card != this.selectedCard));
      return display;
   }

   public boolean handleKey(int key) {
      return this.createModal.handleKey(key);
   }

   public boolean handleCharacter(int codePoint) {
      return this.createModal.handleCharacter(codePoint);
   }

   @Override
   public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
      if (this.displayedPage == MenuPage.ACCOUNT_SWITCHER && !(this.progress <= 0.001F)) {
         this.pageHeader("Accounts", "Switch between your saved Minecraft accounts.");
         String sortLabel = MenuText.ui(this.oldestFirst ? "Oldest" : "Recent");
         boolean sortHovered = this.hit(this.mouseX, this.mouseY, 852.0F, 104.0F, 108.0F, 36.0F);
         Render2DUtil.rect(this.sx(852.0F), this.sy(104.0F), this.px(108.0F), this.px(36.0F))
            .color(this.alpha(sortHovered ? Theme.Colors.CONTROL_HOVER : Theme.Colors.CONTROL, this.progress))
            .radius(999.0F)
            .border(Math.max(0.5F, this.px(0.5F)), this.alpha(Theme.Colors.OUTLINES_MEDIUM, this.progress))
            .draw();
         float sortTextMax = 68.0F;
         String shownSort = UiFonts.sfProDisplay().ellipsize(sortLabel, 14.0F, 14.0F * UiFontStyle.MEDIUM.letterSpacingEm(), sortTextMax);
         this.text(866.0F, this.centeredTextY(122.0F, 14.0F), 14.0F, shownSort, -1, this.progress, UiFontStyle.MEDIUM);
         this.texture(940.0F, 117.0F, 10.0F, Textures.Icons.CHEVRON_DOWN, Theme.Colors.ICON, this.progress);
         this.addButton.render(minecraft, guiGraphicsExtractor);
         Render2DUtil.pushScissor(this.x(), this.sy(168.0F), this.width(), this.height() - this.px(168.0F));

         for (AccountCard card : this.cards) {
            card.render(minecraft, guiGraphicsExtractor);
         }

         Render2DUtil.popScissor();
         this.createModal.render(minecraft, guiGraphicsExtractor);
      }
   }

   private void openModal() {
      this.pendingNickname = "";
      this.createModal.open();
   }

   private void closeModal() {
      this.pendingNickname = "";
      this.createModal.close();
   }

   private void saveModal() {
      String name = this.pendingNickname.trim();
      if (name.length() >= 3 && name.length() <= 16 && name.matches("^[a-zA-Z0-9_]+$")) {
         this.cards.add(0, new AccountCard(name, "Never played", this.nextAvatarIndex++, false));
         this.rebuildChildren();
         this.persistAccounts();
         this.closeModal();
      }
   }

   private String randomNickname() {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      for (int attempt = 0; attempt < 20; attempt++) {
         String prefix = NICK_PREFIXES[random.nextInt(NICK_PREFIXES.length)];
         String suffix = NICK_SUFFIXES[random.nextInt(NICK_SUFFIXES.length)];

         String name = switch (random.nextInt(6)) {
            case 0 -> prefix + suffix + random.nextInt(10, 100);
            case 1 -> prefix + "_" + suffix;
            case 2 -> prefix.toLowerCase(Locale.ROOT) + suffix + random.nextInt(100, 1000);
            case 3 -> prefix.toLowerCase(Locale.ROOT) + "_" + suffix.toLowerCase(Locale.ROOT);
            case 4 -> prefix + suffix;
            default -> suffix + prefix + random.nextInt(10, 100);
         };
         if (name.length() <= 16 && this.cards.stream().noneMatch(card -> card.name().equals(name))) {
            return name;
         }
      }

      return NICK_PREFIXES[random.nextInt(NICK_PREFIXES.length)] + random.nextInt(1000, 10000);
   }

   private void rebuildChildren() {
      this.children.clear();
      this.children.addAll(this.cards);
   }
}

