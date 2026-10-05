package ru.rooyzee.elytrixclient.client.media;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Обложка текущего трека как текстура Minecraft.
 */
public final class CoverTexture {

    private static final Identifier ID = Identifier.fromNamespaceAndPath("elytrixclient", "media/cover");
    private static String loadedFor = "";
    private static boolean ready;

    private CoverTexture() {}

    public static Identifier get() {
        if (MediaSession.artwork() == null && !MediaSession.title().isEmpty()) {
            CoverArt.ensure(MediaSession.artist(), MediaSession.title());
        }
        String key = MediaSession.coverKey();
        if (key.isEmpty()) {
            byte[] fetched = CoverArt.bytes();
            if (fetched == null || fetched.length == 0) return null;
            key = CoverArt.loadedFor();
        }
        if (key.equals(loadedFor)) return ready ? ID : null;
        loadedFor = key; ready = false;
        byte[] bytes = MediaSession.artwork();
        if (bytes == null || bytes.length == 0) bytes = CoverArt.bytes();
        try {
            NativeImage image;
            if (bytes != null && bytes.length > 0) {
                image = decode(bytes);
            } else {
                File file = MediaSession.coverFile();
                if (!file.exists() || file.length() == 0) return null;
                try (FileInputStream stream = new FileInputStream(file)) { image = NativeImage.read(stream); }
            }
            if (image == null) return null;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            mc.getTextureManager().register(ID, new DynamicTexture(() -> "elytrix-cover", image));
            ready = true;
            return ID;
        } catch (Exception e) { return null; }
    }

    private static NativeImage decode(byte[] bytes) {
        try { return NativeImage.read(new java.io.ByteArrayInputStream(bytes)); }
        catch (Exception ignored) {
            try {
                java.awt.image.BufferedImage source = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if (source == null) return null;
                NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), false);
                for (int y = 0; y < source.getHeight(); y++) {
                    for (int x = 0; x < source.getWidth(); x++) {
                        int argb = source.getRGB(x, y);
                        int a = (argb >> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
                        image.setPixel(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }
                return image;
            } catch (Exception e) { return null; }
        }
    }
}