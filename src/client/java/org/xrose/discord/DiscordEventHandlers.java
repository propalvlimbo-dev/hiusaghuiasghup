package org.xrose.discord;

import com.sun.jna.Callback;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class DiscordEventHandlers extends Structure {
   public DiscordEventHandlers.ReadyCallback ready;
   public DiscordEventHandlers.DisconnectedCallback disconnected;
   public DiscordEventHandlers.ErroredCallback errored;
   public DiscordEventHandlers.JoinGameCallback joinGame;
   public DiscordEventHandlers.SpectateGameCallback spectateGame;
   public DiscordEventHandlers.JoinRequestCallback joinRequest;

   protected List<String> getFieldOrder() {
      return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }

   public interface DisconnectedCallback extends Callback {
      void accept(int var1, String var2);
   }

   public interface ErroredCallback extends Callback {
      void accept(int var1, String var2);
   }

   public interface JoinGameCallback extends Callback {
      void accept(String var1);
   }

   public interface JoinRequestCallback extends Callback {
      void accept(DiscordUser var1);
   }

   public interface ReadyCallback extends Callback {
      void accept(DiscordUser var1);
   }

   public interface SpectateGameCallback extends Callback {
      void accept(String var1);
   }
}

