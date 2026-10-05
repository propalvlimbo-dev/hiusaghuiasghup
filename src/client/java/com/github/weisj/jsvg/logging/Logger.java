package com.github.weisj.jsvg.logging;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Logger {
   void log(Logger.Level var1, @Nullable String var2);

   void log(Logger.Level var1, @Nullable String var2, @NotNull Throwable var3);

   void log(Logger.Level var1, @NotNull Supplier<@Nullable String> var2);

   enum Level {
      DEBUG,
      INFO,
      WARNING,
      ERROR;

      // $VF: synthetic method
      private static Logger.Level[] $values() {
         return new Logger.Level[]{DEBUG, INFO, WARNING, ERROR};
      }
   }
}
