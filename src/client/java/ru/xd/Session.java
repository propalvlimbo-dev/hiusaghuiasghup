package ru.xd;

import sdk.api.enums.VirtualizationMode;
import sdk.api.enums.VirtualizationType;
import sdk.api.virtualization.Virtualization;

public class Session {
   private static Session instance;
   private int uid = 1488;
   private int role = 4;
   private String name = "xardis1488";
   private String subTime = "2027-01-01";
   private int sub = 4;

   private Session() {
   }

   public static Session getInstance() {
      if (instance == null) {
         instance = new Session();
      }

      return instance;
   }

   public String profile(String key) {
      switch (key.toLowerCase()) {
         case "uid":
            return getUid();
         case "role":
            return getRole();
         case "name":
            return getName();
         case "subtime":
            return getSubTime();
         case "sub":
            return getSub();
         case "hwid":
            return getHwid();
         case "token":
            return getToken();
         default:
            return null;
      }
   }

   @Virtualization(virtualization = VirtualizationType.VIRTUALIZATION, mode = VirtualizationMode.GRAPH)
   public static native String getHwid();

   @Virtualization(virtualization = VirtualizationType.VIRTUALIZATION, mode = VirtualizationMode.GRAPH)
   public static native String getUid();

   @Virtualization(virtualization = VirtualizationType.VIRTUALIZATION, mode = VirtualizationMode.GRAPH)
   public static native String getSub();

   @Virtualization(virtualization = VirtualizationType.MUTATION, mode = VirtualizationMode.BLOCK)
   public static native String getSubTime();

   @Virtualization(virtualization = VirtualizationType.VIRTUALIZATION, mode = VirtualizationMode.GRAPH)
   public static native String getName();

   @Virtualization(virtualization = VirtualizationType.VIRTUALIZATION, mode = VirtualizationMode.GRAPH)
   public static native String getRole();

   @Virtualization(virtualization = VirtualizationType.MUTATION, mode = VirtualizationMode.BLOCK)
   public static native String getRoleName();

   @Virtualization(virtualization = VirtualizationType.VIRTUALIZATION, mode = VirtualizationMode.GRAPH)
   public static native String getToken();
}

