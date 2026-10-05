package com.github.weisj.jsvg.util;

import java.util.Locale;

public final class SystemUtil {
   public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
   public static final boolean isMacOS = OS_NAME.contains("mac");

   private SystemUtil() {
   }
}

