package org.xrose.discord;

import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class DiscordRichPresence extends Structure {
   public String state;
   public String details;
   public long startTimestamp;
   public long endTimestamp;
   public String largeImageKey;
   public String largeImageText;
   public String smallImageKey;
   public String smallImageText;
   public String partyId;
   public int partySize;
   public int partyMax;
   public String partyPrivacy;
   public String matchSecret;
   public String joinSecret;
   public String spectateSecret;
   public int instance;
   public String button_label_1;
   public String button_url_1;
   public String button_label_2;
   public String button_url_2;

   public DiscordRichPresence() {
      this.setStringEncoding("UTF-8");
   }

   protected List<String> getFieldOrder() {
      return Arrays.asList(
         "state",
         "details",
         "startTimestamp",
         "endTimestamp",
         "largeImageKey",
         "largeImageText",
         "smallImageKey",
         "smallImageText",
         "partyId",
         "partySize",
         "partyMax",
         "partyPrivacy",
         "matchSecret",
         "joinSecret",
         "spectateSecret",
         "button_label_1",
         "button_url_1",
         "button_label_2",
         "button_url_2",
         "instance"
      );
   }
}

