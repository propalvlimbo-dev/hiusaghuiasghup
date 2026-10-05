package org.xrose.feature.impl.player;

import com.sun.jna.Native;
import net.minecraft.client.Minecraft;
import org.xrose.discord.DiscordEventHandlers;
import org.xrose.discord.DiscordRPC;
import org.xrose.discord.DiscordRichPresence;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.utils.NativeUtils;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class DiscordRPCFeature extends Feature {
   private static final String DISCORD_ID = "1359591386927595551";
   private static final String VERSION_LABEL = "XRose Client | 26.2";
   private static final String LARGE_IMAGE_KEY = "https://i.postimg.cc/kGxLz1T0/ava-gif.gif";
   private static DiscordRPC discordRPC;
   private static DiscordRichPresence presence;
   private static Thread rpcThread;
   private static volatile boolean running;
   private static volatile boolean available;

   public DiscordRPCFeature() {
      super("Discord RPC", "Shows Discord Rich Presence for XRose Client.", FeatureCategory.PLAYER, -1);
      this.setVisible(false);
      initRPC();
      startRPC();
   }

   private static void initRPC() {
      try {
         NativeUtils.extractNativeLibrary("discord-rpc");
         discordRPC = (DiscordRPC)Native.load("discord-rpc", DiscordRPC.class);
         presence = new DiscordRichPresence();
         available = true;
      } catch (Throwable t) {
         available = false;
         System.err.println("[DiscordRPC] Failed to initialize: " + t.getMessage());
      }
   }

   private static void startRPC() {
      if (!running && available) {
         try {
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            discordRPC.Discord_Initialize("1359591386927595551", handlers, true, null);
            presence.startTimestamp = System.currentTimeMillis() / 1000L;
            presence.largeImageKey = "https://i.postimg.cc/kGxLz1T0/ava-gif.gif";
            presence.largeImageText = "XRose Client | 26.2";
            running = true;
            rpcThread = new Thread(() -> {
               while (running) {
                  try {
                     presence.state = "User: " + sessionName();
                     presence.details = "XRose Client | 26.2";
                     discordRPC.Discord_UpdatePresence(presence);
                     discordRPC.Discord_RunCallbacks();
                     Thread.sleep(5000L);
                  } catch (InterruptedException e) {
                     break;
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            }, "Discord-RPC-Thread");
            rpcThread.setDaemon(true);
            rpcThread.start();
         } catch (Exception e) {
            e.printStackTrace();
            running = false;
         }
      }
   }

   private static void stopRPC() {
      running = false;
      if (rpcThread != null && rpcThread.isAlive()) {
         rpcThread.interrupt();
      }

      if (available) {
         try {
            discordRPC.Discord_ClearPresence();
            discordRPC.Discord_Shutdown();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private static String sessionName() {
      try {
         if (Class.forName("ru.xd.Session").getMethod("getName").invoke(null) instanceof String s && !s.isBlank()) {
            return s;
         }
      } catch (Throwable var2) {
      }

      Minecraft mc = Minecraft.getInstance();
      return mc.getUser() != null ? mc.getUser().getName() : "Player";
   }
}

