package wtf.expensive.client.ui.visual;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import wtf.expensive.client.events.EventManager;
import wtf.expensive.client.events.impl.render.EventRender;
import net.minecraft.resources.Identifier;
import wtf.expensive.client.Expensive;
import wtf.expensive.client.managment.Managment;
import wtf.expensive.client.util.FreeCamera;
import wtf.expensive.client.modules.Function;
import wtf.expensive.client.modules.impl.render.BlockESP;
import wtf.expensive.client.modules.impl.render.ItemESP;
import wtf.expensive.client.modules.impl.render.JumpCircleFunction;
import wtf.expensive.client.modules.impl.render.Tracers;
import wtf.expensive.client.modules.impl.render.XRayFunction;
import wtf.expensive.client.util.font.Fonts;
import wtf.expensive.client.util.font.StyledFontRenderer;
import wtf.expensive.client.util.ClientUtil;
import wtf.expensive.client.util.render.ColorUtil;
import wtf.expensive.client.util.render.ProjectionUtil;
import wtf.expensive.client.util.render.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public final class VisualRenderer implements HudElement {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xB0000000;
    private static final int GREEN = 0xFF35E877;
    private static final int RED = 0xFFEF4836;
    private static final Identifier JUMP_CIRCLE =
            Identifier.fromNamespaceAndPath(Expensive.MOD_ID, "textures/images/circle.png");

    private final List<TimedPoint> jumpCircles = new ArrayList<>();
    private final List<BlockMarker> blockMarkers = new ArrayList<>();
    private final List<BlockMarker> oreMarkers = new ArrayList<>();
    private long lastScan;
    private BlockPos lastScanCenter = BlockPos.ZERO;
    private boolean wasOnGround = true;

    private record TimedPoint(Vec3 position, long time) {
    }

    private record BlockMarker(BlockPos position, int color) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || Managment.FUNCTION_MANAGER == null) {
            return;
        }

        if (ClientUtil.legitMode) {
            return;
        }

        RenderUtil.resetScissorStack();
        EventManager.call(new EventRender(graphics, delta, EventRender.Type.RENDER2D));
        float partialTick = delta.getGameTimeDeltaPartialTick(false);

        renderDeferredBlockBoxes(graphics);
        renderPlayers(graphics, minecraft, partialTick);
        renderItems(graphics, minecraft, partialTick);
        renderBlockEspEntities(graphics, minecraft, partialTick);
        updateAndRenderWorldMarkers(graphics, minecraft);
        renderJumpCircles(graphics, minecraft, partialTick);
    }

    private Function function(String name) {
        return Managment.FUNCTION_MANAGER.get(name);
    }

    private boolean enabled(String name) {
        Function function = function(name);
        return function != null && function.isState();
    }

    private int style(int index) {
        return Managment.STYLE_MANAGER == null ? WHITE : Managment.STYLE_MANAGER.getColor(index);
    }

    private void renderPlayers(GuiGraphicsExtractor graphics, Minecraft minecraft, float partialTick) {
        boolean arrows = enabled("Arrows");
        Tracers tracers = function("Tracers") instanceof Tracers value && value.isState() ? value : null;
        if (!arrows && tracers == null) {
            return;
        }

        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (player == minecraft.player || !player.isAlive() || player instanceof FreeCamera) {
                continue;
            }
            boolean friend = Managment.FRIEND_MANAGER != null
                    && Managment.FRIEND_MANAGER.isFriend(player.getName().getString());
            int color = friend ? GREEN : style(player.getId() * 17);
            ProjectionUtil.Point center = ProjectionUtil.project(graphics,
                    player.getPosition(partialTick).add(0, player.getBbHeight() * 0.5, 0));
            ProjectionUtil.Bounds bounds = ProjectionUtil.projectEntity(graphics, player, partialTick);

            if (arrows && (center == null || !center.onScreen(graphics, 8))) {
                renderArrow(graphics, minecraft, player, partialTick, color);
            }
            if (bounds == null || bounds.maxX() < 0 || bounds.minX() > graphics.guiWidth()
                    || bounds.maxY() < 0 || bounds.minY() > graphics.guiHeight()) {
                continue;
            }
            if (tracers != null && (!tracers.ignoreNaked.get() || player.getArmorValue() > 0)) {
                int tracerColor = friend ? tracers.friendColor.get() : tracers.color.get();
                tracerColor = RenderUtil.withAlpha(tracerColor, tracers.alpha.getValue().intValue());
                RenderUtil.line(graphics, graphics.guiWidth() * 0.5f, graphics.guiHeight(),
                        (bounds.minX() + bounds.maxX()) * 0.5f, bounds.maxY(),
                        tracers.width.getValue().floatValue(), tracerColor);
            }
        }
    }

    private void renderArrow(GuiGraphicsExtractor graphics, Minecraft minecraft, Entity entity,
                             float partialTick, int color) {
        Vec3 relative = entity.getPosition(partialTick).subtract(minecraft.gameRenderer.mainCamera().position());
        double yaw = Math.toRadians(minecraft.gameRenderer.mainCamera().yRot());
        double angle = Math.atan2(relative.z, relative.x) - yaw;
        float radius = Math.min(graphics.guiWidth(), graphics.guiHeight()) * 0.19f;
        float cx = graphics.guiWidth() * 0.5f;
        float cy = graphics.guiHeight() * 0.5f;
        float x = cx + (float) Math.cos(angle) * radius;
        float y = cy + (float) Math.sin(angle) * radius;
        float dx = (float) Math.cos(angle);
        float dy = (float) Math.sin(angle);
        float px = -dy;
        float py = dx;
        RenderUtil.triangle(graphics, x + dx * 6, y + dy * 6,
                x - dx * 4 + px * 4, y - dy * 4 + py * 4,
                x - dx * 4 - px * 4, y - dy * 4 - py * 4, BLACK);
        RenderUtil.triangle(graphics, x + dx * 5, y + dy * 5,
                x - dx * 3 + px * 3, y - dy * 3 + py * 3,
                x - dx * 3 - px * 3, y - dy * 3 - py * 3, color);
    }

    private void drawBox(GuiGraphicsExtractor graphics, ProjectionUtil.Bounds bounds, int color, float width) {
        RenderUtil.line(graphics, bounds.minX(), bounds.minY(), bounds.maxX(), bounds.minY(), width, color);
        RenderUtil.line(graphics, bounds.maxX(), bounds.minY(), bounds.maxX(), bounds.maxY(), width, color);
        RenderUtil.line(graphics, bounds.maxX(), bounds.maxY(), bounds.minX(), bounds.maxY(), width, color);
        RenderUtil.line(graphics, bounds.minX(), bounds.maxY(), bounds.minX(), bounds.minY(), width, color);
    }

    private void renderItems(GuiGraphicsExtractor graphics, Minecraft minecraft, float partialTick) {
        ItemESP itemEsp = function("ItemESP") instanceof ItemESP value && value.isState() ? value : null;
        if (itemEsp == null) {
            return;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
                continue;
            }
            ProjectionUtil.Bounds bounds = ProjectionUtil.projectEntity(graphics, item, partialTick);
            if (bounds == null) {
                continue;
            }
            int color = style(item.getId() * 9);
            if (itemEsp.elements.get(0)) {
                drawBox(graphics, bounds, color, 1f);
            }
            if (itemEsp.elements.get(1)) {
                String text = item.getItem().getHoverName().getString()
                        + (item.getItem().getCount() > 1 ? " x" + item.getItem().getCount() : "");
                StyledFontRenderer.drawCentered(graphics, minecraft.font,
                        Component.literal(text).withStyle(Fonts.MEDIUM_12),
                        (bounds.minX() + bounds.maxX()) * 0.5f, bounds.minY() - 9, WHITE);
            }
        }
    }

    private void renderBlockEspEntities(GuiGraphicsExtractor graphics, Minecraft minecraft, float partialTick) {
        BlockESP blockEsp = function("BlockESP") instanceof BlockESP value && value.isState() ? value : null;
        if (blockEsp == null || !blockEsp.blocks.get(5)) return;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractMinecart)) continue;
            ProjectionUtil.Bounds bounds = ProjectionUtil.projectEntity(graphics, entity, partialTick);
            if (bounds != null) drawBox(graphics, bounds, 0xFFFFFF40, 1.4f);
        }
    }

    private void updateAndRenderWorldMarkers(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        BlockESP blockEsp = function("BlockESP") instanceof BlockESP value && value.isState() ? value : null;
        XRayFunction xray = function("XRay") instanceof XRayFunction value && value.isState() ? value : null;
        if (blockEsp == null && xray == null) {
            blockMarkers.clear();
            oreMarkers.clear();
            return;
        }
        long now = System.currentTimeMillis();
        BlockPos center = minecraft.player.blockPosition();
        if (now - lastScan > 750 || center.distManhattan(lastScanCenter) > 4) {
            lastScan = now;
            lastScanCenter = center.immutable();
            scanBlocks(minecraft, center, blockEsp, xray);
        }
        if (blockEsp != null) {
            for (BlockMarker marker : blockMarkers) {
                renderBlockMarker(graphics, marker, 1.4f);
            }
        }
        if (xray != null) {
            for (BlockMarker marker : oreMarkers) {
                renderBlockMarker(graphics, marker, 1f);
            }
        }
    }

    private void scanBlocks(Minecraft minecraft, BlockPos center, BlockESP blockEsp, XRayFunction xray) {
        blockMarkers.clear();
        oreMarkers.clear();
        int radius = xray == null ? 24 : xray.radius.getValue().intValue();
        int up = xray == null ? 12 : xray.up.getValue().intValue();
        int down = xray == null ? 12 : xray.down.getValue().intValue();
        int step = xray == null ? 1 : Math.max(1, xray.skip.getValue().intValue());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -down; y <= up; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!minecraft.level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = minecraft.level.getBlockState(cursor);
                    if (blockEsp != null && blockMarkers.size() < 512) {
                        int blockColor = blockEspColor(state, blockEsp);
                        if (blockColor != 0) {
                            blockMarkers.add(new BlockMarker(cursor.immutable(), blockColor));
                        }
                    }
                    if (xray != null && oreMarkers.size() < 1500
                            && Math.floorMod(x + y + z, step) == 0) {
                        int oreColor = oreColor(state, xray);
                        if (oreColor != 0) {
                            oreMarkers.add(new BlockMarker(cursor.immutable(), oreColor));
                        }
                    }
                }
            }
        }
    }

    private int blockEspColor(BlockState state, BlockESP esp) {
        if (esp.blocks.get(0) && (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST))) return 0xFFFFA000;
        if (esp.blocks.get(1) && state.is(Blocks.ENDER_CHEST)) return 0xFFA020F0;
        if (esp.blocks.get(2) && state.is(Blocks.SPAWNER)) return 0xFFFF4040;
        if (esp.blocks.get(3) && state.is(BlockTags.SHULKER_BOXES)) return 0xFFFF40FF;
        if (esp.blocks.get(4) && state.is(BlockTags.BEDS)) return 0xFF40A0FF;
        return 0;
    }

    private int oreColor(BlockState state, XRayFunction xray) {
        if (xray.ores.get(0) && (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE))) return 0xFF555555;
        if (xray.ores.get(1) && state.is(BlockTags.IRON_ORES)) return 0xFFD8B080;
        if (xray.ores.get(2) && (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE))) return 0xFFFF3030;
        if (xray.ores.get(3) && state.is(BlockTags.GOLD_ORES)) return 0xFFFFD83D;
        if (xray.ores.get(4) && (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE))) return 0xFF20E080;
        if (xray.ores.get(5) && (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE))) return 0xFF40E0E0;
        if (xray.ores.get(6) && state.is(Blocks.ANCIENT_DEBRIS)) return 0xFF9B6045;
        return 0;
    }

    private void renderBlockMarker(GuiGraphicsExtractor graphics, BlockMarker marker, float width) {
        BlockPos position = marker.position();
        ProjectionUtil.Bounds bounds = ProjectionUtil.projectBox(graphics,
                new AABB(position.getX(), position.getY(), position.getZ(),
                        position.getX() + 1, position.getY() + 1, position.getZ() + 1));
        if (bounds != null && bounds.depth() < 160) {
            drawBox(graphics, bounds, RenderUtil.withAlpha(marker.color(), 220), width);
        }
    }

    private void renderDeferredBlockBoxes(GuiGraphicsExtractor graphics) {
        RenderUtil.drainBlockBoxes(request -> {
            BlockPos position = request.position();
            ProjectionUtil.Bounds bounds = ProjectionUtil.projectBox(graphics,
                    new AABB(position.getX(), position.getY(), position.getZ(),
                            position.getX() + 1, position.getY() + 1, position.getZ() + 1));
            if (bounds == null || bounds.depth() >= 160) {
                return;
            }
            int color = request.color();
            if ((color >>> 24) == 0) {
                color |= 0xFF000000;
            }
            drawBox(graphics, bounds, color, 1.4f);
        });
    }

    private void renderJumpCircles(GuiGraphicsExtractor graphics, Minecraft minecraft, float partialTick) {
        long now = System.currentTimeMillis();
        boolean onGround = minecraft.player.onGround();
        if (enabled("Jump Circle") && !onGround && wasOnGround) {
            jumpCircles.add(new TimedPoint(minecraft.player.getPosition(partialTick).add(0, 0.03, 0), now));
        }
        wasOnGround = onGround;
        if (!enabled("Jump Circle")) {
            jumpCircles.clear();
            return;
        }
        JumpCircleFunction settings = (JumpCircleFunction) function("Jump Circle");
        jumpCircles.removeIf(circle -> now - circle.time() > 1200);
        for (TimedPoint circle : jumpCircles) {
            float progress = Math.clamp((now - circle.time()) / 1200f * settings.speed.getValue().floatValue(), 0f, 1f);
            double radius = settings.radius.getValue().doubleValue() * progress;
            int alpha = (int) (220 * (1f - progress));
            if (alpha <= 0 || radius <= 0) {
                continue;
            }

            float[] corners = new float[8];
            int written = 0;
            boolean visible = true;
            double[][] offsets = {{-radius, -radius}, {-radius, radius}, {radius, radius}, {radius, -radius}};
            for (double[] offset : offsets) {
                ProjectionUtil.Point projected = ProjectionUtil.project(graphics,
                        circle.position().add(offset[0], 0, offset[1]));
                if (projected == null) {
                    visible = false;
                    break;
                }
                corners[written++] = projected.x();
                corners[written++] = projected.y();
            }
            if (visible) {
                RenderUtil.texturedQuad(graphics, JUMP_CIRCLE, corners,
                        RenderUtil.withAlpha(style(0), alpha));
            }
        }
    }
}
