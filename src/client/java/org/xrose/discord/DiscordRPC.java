package org.xrose.discord;

import com.sun.jna.Library;

public interface DiscordRPC extends Library {
   void Discord_Initialize(String var1, DiscordEventHandlers var2, boolean var3, String var4);

   void Discord_Shutdown();

   void Discord_UpdatePresence(DiscordRichPresence var1);

   void Discord_ClearPresence();

   void Discord_RunCallbacks();

   void Discord_Respond(String var1, int var2);
}
