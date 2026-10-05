package org.xrose.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.xrose.command.ClientCommand;
import org.xrose.context.MinecraftContext;
import org.xrose.mixin.accessor.PlayerTabOverlayAccessor;
import org.xrose.utils.text.ChatUtil;
import sdk.api.invoke.api.invoke;
import sdk.api.invoke.impl.PackerMode;
import sdk.api.invoke.impl.PackerType;

@invoke(ivirtualiz = PackerType.MUTATION, imode = PackerMode.BLOCK)
public final class RctCommand extends ClientCommand implements MinecraftContext {
   private static final Pattern ANARCHY_PATTERN = Pattern.compile("Р°РЅР°СЂС…РёСЏ-(\\d+)");
   private static final Pattern GRIEF_PATTERN = Pattern.compile("РіСЂРёС„-(\\d+)");
   private static final long REJOIN_DELAY_MS = 1500L;

   public RctCommand() {
      super("rct", "Rejoins the current anarchy (FunTime/SpookyTime)", ":arrows_counterclockwise:");
   }

   @Override
   public void build(LiteralArgumentBuilder<Object> builder) {
      builder.executes(context -> this.execute());
   }

   private int execute() {
      if (this.player() == null) {
         return 0;
      } else if (!isOnSupportedServer(mc)) {
         ChatUtil.error("Rct only works on FunTime and SpookyTime");
         return 0;
      } else {
         String rejoinCommand = resolveRejoinCommand(tabHeader(mc));
         if (rejoinCommand == null) {
            ChatUtil.error("Failed to detect the anarchy number from the tab list");
            return 0;
         } else {
            scheduleRejoin(mc, rejoinCommand);
            ChatUtil.info("Reconnecting via /" + rejoinCommand + "...");
            return 1;
         }
      }
   }

   public static boolean requestRejoin(Minecraft mc) {
      if (mc != null && mc.player != null && isOnSupportedServer(mc)) {
         String rejoinCommand = resolveRejoinCommand(tabHeader(mc));
         if (rejoinCommand == null) {
            return false;
         }

         scheduleRejoin(mc, rejoinCommand);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isOnSupportedServer(Minecraft mc) {
      ServerData server = mc.getCurrentServer();
      if (server != null && server.ip != null) {
         String ip = server.ip.toLowerCase(Locale.ROOT);
         return ip.contains("funtime") || ip.contains("spookytime");
      } else {
         return false;
      }
   }

   public static String tabHeader(Minecraft mc) {
      Component header = ((PlayerTabOverlayAccessor)mc.gui.hud.getTabList()).getHeader();
      return header == null ? "" : header.getString().toLowerCase(Locale.ROOT).replaceAll("В§.", "");
   }

   public static String resolveRejoinCommand(String header) {
      Matcher anarchy = ANARCHY_PATTERN.matcher(header);
      if (anarchy.find()) {
         return "an" + anarchy.group(1);
      }

      Matcher grief = GRIEF_PATTERN.matcher(header);
      return grief.find() ? "grief" + grief.group(1) : null;
   }

   public static void scheduleRejoin(Minecraft mc, String rejoinCommand) {
      mc.player.connection.sendCommand("hub");
      CompletableFuture.delayedExecutor(1500L, TimeUnit.MILLISECONDS).execute(() -> mc.execute(() -> {
         if (mc.player != null) {
            mc.player.connection.sendCommand(rejoinCommand);
         }
      }));
   }
}

