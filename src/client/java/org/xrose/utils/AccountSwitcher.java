package org.xrose.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import org.xrose.menu.core.MenuConfigStore;
import org.xrose.mixin.accessor.MinecraftAccessor;
import org.xrose.mixin.accessor.MinecraftServerAccessor;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class AccountSwitcher {
   public static final String DEFAULT_NAME = "XRose";
   private static boolean startupApplied;

   private AccountSwitcher() {
   }

   public static void applyStartupAccount() {
      if (!startupApplied) {
         startupApplied = true;
         String name = MenuConfigStore.getString("selectedAccount", "");
         if (name.isEmpty() || !name.matches("^[a-zA-Z0-9_]{3,16}$")) {
            name = "XRose";
         }

         switchTo(name);
      }
   }

   public static void switchTo(String name) {
      Minecraft mc = Minecraft.getInstance();
      UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
      User user = new User(name, uuid, "0", Optional.empty(), Optional.empty());
      MinecraftAccessor accessor = (MinecraftAccessor)mc;
      accessor.setUser(user);
      accessor.setProfileFuture(CompletableFuture.completedFuture(new ProfileResult(new GameProfile(uuid, name))));
      mc.updateTitle();
   }

   public static void relogin(String name) {
      Minecraft mc = Minecraft.getInstance();
      ServerData server = mc.getCurrentServer();
      String levelId = null;
      IntegratedServer integrated = mc.getSingleplayerServer();
      if (mc.isLocalServer() && integrated != null) {
         levelId = ((MinecraftServerAccessor)integrated).getStorageSource().getLevelId();
      }

      switchTo(name);
      if (mc.level != null) {
         mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
         if (levelId != null) {
            mc.createWorldOpenFlows().openWorld(levelId, () -> mc.setScreenAndShow(new TitleScreen()));
         } else if (server != null && !server.isLan() && !server.isRealm()) {
            ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(server.ip), server, false, null);
         }
      }
   }
}

