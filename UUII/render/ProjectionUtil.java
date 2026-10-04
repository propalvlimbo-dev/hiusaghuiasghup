package wtf.expensive.client.util.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class ProjectionUtil {
    private static final double NEAR = 0.05;

    private ProjectionUtil() {
    }

    public record Point(float x, float y, double depth) {
        public boolean onScreen(GuiGraphicsExtractor graphics, float margin) {
            return depth > NEAR && x >= -margin && y >= -margin
                    && x <= graphics.guiWidth() + margin && y <= graphics.guiHeight() + margin;
        }
    }

    public record Bounds(float minX, float minY, float maxX, float maxY, double depth) {
        public float width() {
            return maxX - minX;
        }

        public float height() {
            return maxY - minY;
        }
    }

    public static Point project(GuiGraphicsExtractor graphics, Vec3 worldPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.mainCamera();
        Vec3 relative = worldPosition.subtract(camera.position());

        Vector3fc forward = camera.forwardVector();
        Vector3fc left = camera.leftVector();
        Vector3fc up = camera.upVector();
        double depth = relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z();
        if (depth <= NEAR) {
            return null;
        }

        double horizontal = relative.x * left.x() + relative.y * left.y() + relative.z * left.z();
        double vertical = relative.x * up.x() + relative.y * up.y() + relative.z * up.z();
        double fov = Math.toRadians(Math.clamp(camera.getFov(), 30f, 170f));
        double focal = graphics.guiHeight() * 0.5 / Math.tan(fov * 0.5);
        float x = (float) (graphics.guiWidth() * 0.5 - horizontal * focal / depth);
        float y = (float) (graphics.guiHeight() * 0.5 - vertical * focal / depth);
        return new Point(x, y, depth);
    }

    public static Bounds projectEntity(GuiGraphicsExtractor graphics, Entity entity, float partialTick) {
        Vec3 position = entity.getPosition(partialTick);
        AABB current = entity.getBoundingBox();
        AABB box = current.move(position.x - entity.getX(), position.y - entity.getY(), position.z - entity.getZ())
                .inflate(0.05);
        return projectBox(graphics, box);
    }

    public static Bounds projectBox(GuiGraphicsExtractor graphics, AABB box) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        double nearest = Double.POSITIVE_INFINITY;
        int visible = 0;

        for (int corner = 0; corner < 8; corner++) {
            Point point = project(graphics, new Vec3(
                    (corner & 1) == 0 ? box.minX : box.maxX,
                    (corner & 2) == 0 ? box.minY : box.maxY,
                    (corner & 4) == 0 ? box.minZ : box.maxZ));
            if (point == null) {
                continue;
            }
            visible++;
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            nearest = Math.min(nearest, point.depth);
        }
        if (visible < 2 || !Float.isFinite(minX) || maxX - minX < 0.2f || maxY - minY < 0.2f) {
            return null;
        }
        return new Bounds(minX, minY, maxX, maxY, nearest);
    }
}
