package org.xrose.feature.impl.visual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.xrose.context.MinecraftContext;
import org.xrose.event.EventTarget;
import org.xrose.event.events.game.GameTickEvent;
import org.xrose.feature.Feature;
import org.xrose.feature.FeatureCategory;
import org.xrose.feature.FeatureManager;
import org.xrose.feature.setting.BooleanSetting;
import org.xrose.feature.setting.ColorSetting;
import org.xrose.feature.setting.NumberSetting;
import org.xrose.utils.blockesp.BlockEspConfig;
import org.xrose.utils.render.world.WorldMeshRenderer;
import org.xrose.utils.text.ChatUtil;
import sdk.api.optimize.optimize;

@optimize
public final class BlockEspFeature extends Feature implements MinecraftContext {
   public static final int DEFAULT_COLOR = -8391937;
   public final ColorSetting color = this.register(new ColorSetting("Color", -8391937));
   public final NumberSetting range = this.register(new NumberSetting("Range", 32.0, 1.0, 128.0, 1.0, ""));
   public final BooleanSetting notifyInChat = this.register(new BooleanSetting("Notify", false));
   private final Set<String> blocksToHighlight = new CopyOnWriteArraySet<>();
   private final Set<Block> highlightBlocks = new HashSet<>();
   private final Map<BlockPos, BlockState> renderBlocks = new ConcurrentHashMap<>();
   private final Set<BlockPos> notifiedBlocks = new CopyOnWriteArraySet<>();
   private long lastScanTime;
   private int checkCounter;

   public BlockEspFeature() {
      super("BlockESP", "Highlights selected blocks in the world", FeatureCategory.VISUAL, -1);
   }

   public static BlockEspFeature getInstance() {
      return FeatureManager.INSTANCE.getFeature(BlockEspFeature.class);
   }

   public static BlockEspFeature getEnabled() {
      return FeatureManager.INSTANCE.getEnabled(BlockEspFeature.class);
   }

   public Set<String> getBlocksToHighlight() {
      return this.blocksToHighlight;
   }

   @Override
   protected void onEnable() {
      this.blocksToHighlight.clear();
      this.blocksToHighlight.addAll(BlockEspConfig.INSTANCE.getBlocks());
      this.renderBlocks.clear();
      this.notifiedBlocks.clear();
      this.lastScanTime = System.nanoTime() / 1000000L;
      this.checkCounter = 0;
   }

   @Override
   protected void onDisable() {
      this.renderBlocks.clear();
      this.notifiedBlocks.clear();
   }

   @EventTarget
   public void onTick(GameTickEvent event) {
      if (mc.level == null || mc.player == null) {
         this.renderBlocks.clear();
      } else if (this.blocksToHighlight.isEmpty()) {
         this.renderBlocks.clear();
      } else {
         BlockPos playerPos = mc.player.blockPosition();
         long currentTime = System.nanoTime() / 1000000L;
         if (currentTime - this.lastScanTime >= 2000L) {
            this.scanChunkArea(playerPos, this.chunkRange(), 24);
            this.lastScanTime = currentTime;
         }

         if (this.checkCounter % 20 == 0) {
            this.scanChunkArea(playerPos, this.chunkRange(), 16);
         }

         if (this.checkCounter % 100 == 0) {
            this.renderBlocks.entrySet().removeIf(entry -> {
               boolean shouldRemove = !this.highlightBlocks.contains(entry.getValue().getBlock());
               if (shouldRemove) {
                  this.notifiedBlocks.remove(entry.getKey());
               }

               return shouldRemove;
            });
         }

         this.checkCounter++;
      }
   }

   public void renderWorld() {
      if (this.isEnabled() && mc.level != null && mc.player != null) {
         if (!this.renderBlocks.isEmpty()) {
            int highlightColor = this.color.getValue();
            List<WorldMeshRenderer.Line> lines = new ArrayList<>(this.renderBlocks.size() * 12);

            for (BlockPos pos : this.renderBlocks.keySet()) {
               addBlockOutline(lines, pos, highlightColor);
            }

            WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, List.of(), List.of()));
         }
      }
   }

   private static void addBlockOutline(List<WorldMeshRenderer.Line> lines, BlockPos pos, int color) {
      Vec3 min = Vec3.atLowerCornerOf(pos);
      Vec3 max = min.add(1.0, 1.0, 1.0);
      Vec3 minMinMin = min;
      Vec3 minMinMax = new Vec3(min.x, min.y, max.z);
      Vec3 minMaxMin = new Vec3(min.x, max.y, min.z);
      Vec3 minMaxMax = new Vec3(min.x, max.y, max.z);
      Vec3 maxMinMin = new Vec3(max.x, min.y, min.z);
      Vec3 maxMinMax = new Vec3(max.x, min.y, max.z);
      Vec3 maxMaxMin = new Vec3(max.x, max.y, min.z);
      Vec3 maxMaxMax = max;
      lines.add(new WorldMeshRenderer.Line(minMinMin, minMinMax, color));
      lines.add(new WorldMeshRenderer.Line(minMinMin, minMaxMin, color));
      lines.add(new WorldMeshRenderer.Line(minMinMin, maxMinMin, color));
      lines.add(new WorldMeshRenderer.Line(minMinMax, minMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(minMinMax, maxMinMax, color));
      lines.add(new WorldMeshRenderer.Line(minMaxMin, minMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(minMaxMin, maxMaxMin, color));
      lines.add(new WorldMeshRenderer.Line(maxMinMin, maxMinMax, color));
      lines.add(new WorldMeshRenderer.Line(maxMinMin, maxMaxMin, color));
      lines.add(new WorldMeshRenderer.Line(maxMinMax, maxMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(minMaxMax, maxMaxMax, color));
      lines.add(new WorldMeshRenderer.Line(maxMaxMin, maxMaxMax, color));
   }

   private int chunkRange() {
      return (int)Math.ceil(this.range.getFloat() / 16.0) + 1;
   }

   private void scanChunkArea(BlockPos playerPos, int chunkRange, int yRange) {
      if (mc.level != null && mc.player != null) {
         this.highlightBlocks.clear();

         for (String name : this.blocksToHighlight) {
            Block block = (Block)BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
            if (block != Blocks.AIR) {
               this.highlightBlocks.add(block);
            }
         }

         if (!this.highlightBlocks.isEmpty()) {
            double maxDistanceSqr = (double)this.range.getFloat() * this.range.getFloat();
            double playerX = mc.player.getX();
            double playerZ = mc.player.getZ();
            int minYBase = mc.level.getMinY();
            MutableBlockPos mutablePos = new MutableBlockPos();

            for (int x = -chunkRange; x <= chunkRange; x++) {
               for (int z = -chunkRange; z <= chunkRange; z++) {
                  int chunkX = (playerPos.getX() >> 4) + x;
                  int chunkZ = (playerPos.getZ() >> 4) + z;
                  if (mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                     LevelChunk chunk = mc.level.getChunkSource().getChunkNow(chunkX, chunkZ);
                     if (chunk != null) {
                        int cx = chunk.getPos().getMinBlockX();
                        int cz = chunk.getPos().getMinBlockZ();

                        for (int bx = 0; bx < 16; bx++) {
                           for (int bz = 0; bz < 16; bz++) {
                              int columnX = cx + bx;
                              int columnZ = cz + bz;
                              double columnDx = columnX + 0.5 - playerX;
                              double columnDz = columnZ + 0.5 - playerZ;
                              if (!(columnDx * columnDx + columnDz * columnDz > maxDistanceSqr)) {
                                 int minY = Math.max(minYBase, playerPos.getY() - yRange);
                                 int maxY = Math.min(mc.level.getHeight(Types.WORLD_SURFACE, columnX, columnZ), playerPos.getY() + yRange);

                                 for (int by = minY; by <= maxY; by++) {
                                    mutablePos.set(columnX, by, columnZ);
                                    BlockState state = mc.level.getBlockState(mutablePos);
                                    if (this.highlightBlocks.contains(state.getBlock())) {
                                       BlockPos immutablePos = mutablePos.immutable();
                                       this.renderBlocks.put(immutablePos, state);
                                       if (this.notifyInChat.getValue() && !this.notifiedBlocks.contains(immutablePos)) {
                                          this.notifyBlockFound(immutablePos, state);
                                          this.notifiedBlocks.add(immutablePos);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void notifyBlockFound(BlockPos pos, BlockState state) {
      ChatUtil.info("Block found: " + BuiltInRegistries.BLOCK.getKey(state.getBlock()) + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
   }
}

