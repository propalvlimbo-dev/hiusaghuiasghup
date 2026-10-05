package platform.client.utils.media;

import static platform.api.module.Interface.aM_;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileInputStream;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * The album art of whatever is playing, as a texture the hud can draw. Windows hands the picture over as a
 * stream, the poller writes it next to the client's files, and it is read in here once per track.
 */
public final class CoverTexture {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("xivivide", "media/cover");
    private static String loadedFor = "";
    private static boolean ready;

    private CoverTexture() {
    }

    /**
     * The texture for the track playing now, or null when there is no cover for it. Reading it happens on
     * the game thread, and only when the track has changed.
     */
    public static Identifier get() {
        // Ask the internet for a cover whenever the player gives none of its own
        if (MediaSession.artwork() == null && !MediaSession.title().isEmpty()) {
            CoverArt.ensure(MediaSession.artist(), MediaSession.title());
        }
        String key = MediaSession.coverKey();
        if (key.isEmpty()) {
            byte[] fetched = CoverArt.bytes();
            if (fetched == null || fetched.length == 0) {
                return null;
            }
            key = CoverArt.loadedFor();
        }
        if (key.equals(loadedFor)) {
            return ready ? ID : null;
        }
        loadedFor = key;
        ready = false;
        // The library hands the art over as png bytes; the script leaves it in a file instead
        byte[] bytes = MediaSession.artwork();
        if (bytes == null || bytes.length == 0) {
            bytes = CoverArt.bytes();
        }
        try {
            NativeImage image;
            if (bytes != null && bytes.length > 0) {
                image = decode(bytes);
            } else {
                File file = MediaSession.coverFile();
                if (!file.exists() || file.length() == 0L) {
                    return null;
                }
                try (FileInputStream stream = new FileInputStream(file)) {
                    image = NativeImage.read(stream);
                }
            }
            if (image == null) {
                return null;
            }
            aM_.getTextureManager().register(ID, new DynamicTexture(() -> "xivivide-cover", image));
            ready = true;
            System.out.println("Xivivide: cover loaded " + image.getWidth() + "x" + image.getHeight() + " for " + key);
            return ID;
        } catch (Exception exception) {
            System.err.println("Xivivide: album art could not be read: " + exception);
            return null;
        }
    }

    /**
     * Reads the picture whatever it came as. The game's own reader takes png; covers arrive as jpeg just as
     * often, and those are decoded the long way and copied over pixel by pixel.
     */
    private static NativeImage decode(byte[] bytes) {
        try {
            return NativeImage.read(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception ignored) {
            try {
                java.awt.image.BufferedImage source = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if (source == null) {
                    return null;
                }
                NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), false);
                for (int y = 0; y < source.getHeight(); y++) {
                    for (int x = 0; x < source.getWidth(); x++) {
                        int argb = source.getRGB(x, y);
                        int alpha = (argb >> 24) & 0xFF;
                        int red = (argb >> 16) & 0xFF;
                        int green = (argb >> 8) & 0xFF;
                        int blue = argb & 0xFF;
                        image.setPixel(x, y, (alpha << 24) | (blue << 16) | (green << 8) | red);
                    }
                }
                return image;
            } catch (Exception exception) {
                System.err.println("Xivivide: album art could not be decoded: " + exception);
                return null;
            }
        }
    }
}
