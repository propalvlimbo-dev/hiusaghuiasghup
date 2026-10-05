package org.xrose.utils.blockesp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xrose.utils.ConfigIO;

public final class BlockEspConfig {
   public static final BlockEspConfig INSTANCE = new BlockEspConfig();
   private static final Logger LOGGER = LoggerFactory.getLogger(BlockEspConfig.class);
   private final Path path = ConfigIO.resolve("blockesp.json");
   private final Set<String> blocks = new CopyOnWriteArraySet<>();
   private boolean initialized;

   private BlockEspConfig() {
   }

   public synchronized void initialize() {
      if (!this.initialized) {
         this.load();
         this.initialized = true;
      }
   }

   public Set<String> getBlocks() {
      return this.blocks;
   }

   public List<String> getBlockList() {
      return new ArrayList<>(this.blocks);
   }

   public boolean hasBlock(String block) {
      return block != null && this.blocks.contains(block);
   }

   public boolean addBlock(String block) {
      if (block != null && !block.isBlank()) {
         boolean added = this.blocks.add(block);
         if (added) {
            this.save();
         }

         return added;
      } else {
         return false;
      }
   }

   public boolean removeBlock(String block) {
      if (block == null) {
         return false;
      }

      boolean removed = this.blocks.remove(block);
      if (removed) {
         this.save();
      }

      return removed;
   }

   public boolean clear() {
      if (this.blocks.isEmpty()) {
         return false;
      }

      this.blocks.clear();
      this.save();
      return true;
   }

   public int size() {
      return this.blocks.size();
   }

   private void load() {
      JsonObject root = ConfigIO.read(this.path);
      if (root != null) {
         try {
            JsonElement element = root.get("blocks");
            if (element == null || !element.isJsonArray()) {
               return;
            }

            for (JsonElement blockElement : element.getAsJsonArray()) {
               if (blockElement.isJsonPrimitive()) {
                  this.blocks.add(blockElement.getAsString());
               }
            }
         } catch (Exception exception) {
            LOGGER.error("Failed to load blockesp config from {}", this.path, exception);
         }
      }
   }

   private void save() {
      JsonArray array = new JsonArray();

      for (String block : this.blocks) {
         array.add(block);
      }

      JsonObject root = new JsonObject();
      root.add("blocks", array);
      ConfigIO.write(this.path, root);
   }
}

