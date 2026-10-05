package com.github.weisj.jsvg.logging.impl;

import aQute.bnd.annotation.spi.ServiceConsumer;
import com.github.weisj.jsvg.logging.LogManager;
import com.github.weisj.jsvg.logging.Logger;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ServiceConsumer(LogManager.class)
public final class LogManagerImpl {
   @NotNull
   private static final LogManager logManager;

   private LogManagerImpl() {
   }

   @NotNull
   public static LogManager logManager() {
      return logManager;
   }

   static {
      Iterator<LogManager> managers = ServiceLoader.load(LogManager.class).iterator();
      if (managers.hasNext()) {
         logManager = managers.next();
      } else {
         logManager = new LogManagerImpl.DefaultLogManager();
      }
   }

   private static class DefaultLogManager implements LogManager {
      private DefaultLogManager() {
      }

      @NotNull
      @Override
      public Logger createLogger(@NotNull String name) {
         return new LogManagerImpl.JavaUtilLoggerWrapper(java.util.logging.Logger.getLogger(name));
      }
   }

   private static final class JavaUtilLoggerWrapper implements Logger {
      @NotNull
      private final java.util.logging.Logger logger;

      private JavaUtilLoggerWrapper(@NotNull java.util.logging.Logger logger) {
         this.logger = logger;
      }

      @Override
      public void log(Logger.Level level, @Nullable String message) {
         this.logger.log(new LogManagerImpl.WrapperLogRecord(toJavaUtilLevel(level), message));
      }

      @Override
      public void log(Logger.Level level, @Nullable String message, @NotNull Throwable e) {
         LogManagerImpl.WrapperLogRecord logRecord = new LogManagerImpl.WrapperLogRecord(toJavaUtilLevel(level), message);
         logRecord.setThrown(e);
         this.logger.log(logRecord);
      }

      @Override
      public void log(Logger.Level level, @NotNull Supplier<@Nullable String> messageSupplier) {
         java.util.logging.Level javaUtilLevel = toJavaUtilLevel(level);
         if (this.logger.isLoggable(javaUtilLevel)) {
            this.logger.log(new LogManagerImpl.WrapperLogRecord(javaUtilLevel, messageSupplier.get()));
         }
      }

      static java.util.logging.Level toJavaUtilLevel(@NotNull Logger.Level level) {
         switch (level) {
            case DEBUG:
               return java.util.logging.Level.FINE;
            case INFO:
               return java.util.logging.Level.INFO;
            case WARNING:
               return java.util.logging.Level.WARNING;
            case ERROR:
               return java.util.logging.Level.SEVERE;
            default:
               throw new IllegalArgumentException("Unknown log level: " + level);
         }
      }
   }

   private static final class WrapperLogRecord extends LogRecord {
      private static final String DISPATCHER_CLASS_NAME = LogManagerImpl.JavaUtilLoggerWrapper.class.getName();
      private boolean needToInferCaller = true;

      public WrapperLogRecord(Level level, String msg) {
         super(level, msg);
      }

      @Override
      public String getSourceClassName() {
         if (this.needToInferCaller) {
            this.customInferCaller();
         }

         return super.getSourceClassName();
      }

      @Override
      public String getSourceMethodName() {
         if (this.needToInferCaller) {
            this.customInferCaller();
         }

         return super.getSourceMethodName();
      }

      private void customInferCaller() {
         this.needToInferCaller = false;
         new com.github.weisj.jsvg.logging.impl.CallerFinder(DISPATCHER_CLASS_NAME).findCaller().ifPresent(st -> {
            this.setSourceClassName(st.getClassName());
            this.setSourceMethodName(st.getMethodName());
         });
      }
   }
}

