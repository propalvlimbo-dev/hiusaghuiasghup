package org.xrose.menu.pages.configs;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.menu.core.MenuOverlay;
import org.xrose.utils.ConfigIO;

public final class ConfigPreviewManager {
   public static final ConfigPreviewManager INSTANCE = new ConfigPreviewManager();
   private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPreviewManager.class);
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm", Locale.ENGLISH);
   private static final int CAPTURE_DELAY_TICKS = 14;
   private final Map<String, ConfigPreviewManager.Preview> previews = new ConcurrentHashMap<>();
   private final Set<String> loading = ConcurrentHashMap.newKeySet();
   private final AtomicLong versions = new AtomicLong();
   private String pendingCapture;
   private int captureDelay;

   private ConfigPreviewManager() {
   }

   public static Path configsDir() {
      return ConfigIO.resolve("configs");
   }

   public static Path jsonPath(String name) {
      return configsDir().resolve(name + ".json");
   }

   public static Path previewPath(String name) {
      return configsDir().resolve(name + ".png");
   }

   public String savedDateText(String name) {
      try {
         FileTime time = Files.getLastModifiedTime(jsonPath(name));
         LocalDateTime local = LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault());
         return "Saved " + DATE_FORMAT.format(local);
      } catch (IOException exception) {
         return "";
      }
   }

   public ConfigPreviewManager.Preview preview(Minecraft minecraft, String name) {
      ConfigPreviewManager.Preview cached = this.previews.get(name);
      if (cached != null) {
         return cached;
      }

      if (!Files.exists(previewPath(name))) {
         return null;
      }

      if (this.loading.add(name)) {
         CompletableFuture.<ConfigPreviewManager.Preview>supplyAsync(() -> this.load(minecraft, name), Util.nonCriticalIoPool())
            .whenComplete((unused, throwable) -> this.loading.remove(name));
      }

      return null;
   }

   public void invalidate(String name) {
      ConfigPreviewManager.Preview removed = this.previews.remove(name);
      if (removed != null) {
         Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(removed.texture()));
      }
   }

   public void scheduleCapture(String name) {
      this.pendingCapture = name;
      this.captureDelay = 14;
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (this.pendingCapture != null) {
         if (--this.captureDelay <= 0) {
            String name = this.pendingCapture;
            this.pendingCapture = null;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() == null && !MenuOverlay.isVisible() && minecraft.gameRenderer.mainRenderTarget() != null) {
               Path target = previewPath(name);
               Screenshot.takeScreenshot(minecraft.gameRenderer.mainRenderTarget(), image -> {
                  try {
                     Files.createDirectories(target.getParent());
                     image.writeToFile(target);
                  } catch (IOException exception) {
                     LOGGER.error("Failed to write config preview {}", target, exception);
                  } finally {
                     if (!image.isClosed()) {
                        image.close();
                     }
                  }

                  this.invalidate(name);
               });
            }
         }
      }
   }

   private ConfigPreviewManager.Preview load(Minecraft minecraft, String name) {
      NativeImage image;
      try (InputStream stream = Files.newInputStream(previewPath(name))) {
         image = NativeImage.read(stream);
      } catch (Exception exception) {
         LOGGER.error("Failed to read config preview for {}", name, exception);
         return null;
      }

      int width = image.getWidth();
      int height = image.getHeight();
      if (width > 0 && height > 0) {
         long version = this.versions.incrementAndGet();
         Identifier id = Identifier.parse("xrose:configpreview/" + name.toLowerCase(Locale.ROOT) + "_" + version);
         float side = Math.min(width, height);
         float u0 = (width - side) / 2.0F / width;
         float v0 = (height - side) / 2.0F / height;
         ConfigPreviewManager.Preview preview = new ConfigPreviewManager.Preview(id, u0, v0, u0 + side / width, v0 + side / height);
         minecraft.execute(() -> {
            try {
               minecraft.getTextureManager().register(id, new DynamicTexture(() -> id.toString(), image));
               this.previews.put(name, preview);
            } catch (Exception exception) {
               image.close();
            }
         });
         return preview;
      } else {
         image.close();
         return null;
      }
   }

   public record Preview(Identifier texture, float u0, float v0, float u1, float v1) {
   }
}

