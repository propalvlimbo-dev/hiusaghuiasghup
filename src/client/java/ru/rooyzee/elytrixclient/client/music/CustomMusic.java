package ru.rooyzee.elytrixclient.client.music;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import ru.rooyzee.elytrixclient.client.ElytrixclientClient;
import ru.rooyzee.elytrixclient.client.config.ElytrixConfig;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Своя музыка вместо ванильной: треки {@code .ogg} и {@code .mp3} из папки
 * {@code .minecraft/elytrix/music}. Играет по кругу в случайном порядке.
 *
 * <p>OGG декодируется потоково через stb_vorbis (LWJGL).
 * MP3 декодируется через javax.sound.sampled (требует mp3spi в classpath).
 * Громкость — ползунок «Музыка» в настройках звука игры × громкость в панели.
 * Пока играет своя музыка, ванильная глушится ({@code MusicManagerMixin}).
 */
public final class CustomMusic {
    private CustomMusic() {
    }

    private static final int BUFFERS = 4;
    private static final int CHUNK_FRAMES = 8192;

    private static volatile Player player;
    private static final List<Path> queue = new ArrayList<>();
    private static List<Path> tracks = List.of();
    private static long lastScan;
    private static long nextStartAt;
    private static volatile String nowPlaying = "";

    /** Папка: .minecraft/elytrix/music */
    public static Path folder() {
        return FabricLoader.getInstance().getGameDir().resolve("elytrix").resolve("music");
    }

    /** Своя музыка включена и в папке есть треки — ванильную не играем. */
    public static boolean active() {
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        return cfg != null && cfg.customMusic && !tracks.isEmpty();
    }

    public static int trackCount() {
        return tracks.size();
    }

    public static String nowPlaying() {
        Player p = player;
        return p != null && p.isAlive() ? nowPlaying : "—";
    }

    public static void openFolder() {
        try {
            Files.createDirectories(folder());
            net.minecraft.util.Util.getPlatform().openFile(folder().toFile());
        } catch (Throwable t) {
            ElytrixclientClient.LOG.add("[Elytrix] Не удалось открыть папку музыки: " + t);
        }
        lastScan = 0;
    }

    public static void next() {
        stop();
        nextStartAt = 0;
    }

    public static void stop() {
        Player p = player;
        player = null;
        if (p != null) {
            p.halt();
        }
    }

    private static void scan() {
        long now = System.currentTimeMillis();
        if (now - lastScan < 3000L) {
            return;
        }
        lastScan = now;
        List<Path> found = new ArrayList<>();
        Path dir = folder();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(f -> {
                    String n = f.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".ogg") || n.endsWith(".mp3");
                })
                .sorted()
                .forEach(found::add);
            } catch (IOException ignored) {
                // папка недоступна — просто нет треков
            }
        }
        if (!found.equals(tracks)) {
            tracks = List.copyOf(found);
            queue.clear();
        }
    }

    /** Вызывается каждый клиентский тик. */
    public static void tick(Minecraft mc) {
        ElytrixConfig cfg = ElytrixclientClient.CONFIG;
        if (cfg == null || !cfg.customMusic) {
            if (player != null) {
                stop();
            }
            return;
        }
        scan();
        if (tracks.isEmpty()) {
            stop();
            return;
        }
        Player p = player;
        if (p != null && p.isAlive()) {
            p.volume = volume(mc, cfg);
            return;
        }
        long now = System.currentTimeMillis();
        if (p != null) {
            // трек закончился — небольшая пауза перед следующим
            player = null;
            nextStartAt = now + 2500L;
            return;
        }
        if (now < nextStartAt) {
            return;
        }
        mc.getMusicManager().stopPlaying();
        if (queue.isEmpty()) {
            queue.addAll(tracks);
            Collections.shuffle(queue);
        }
        Path track = queue.remove(0);
        Player np = new Player(track, volume(mc, cfg));
        player = np;
        String name = track.getFileName().toString();
        // убираем расширение (.ogg или .mp3)
        int dot = name.lastIndexOf('.');
        nowPlaying = dot > 0 ? name.substring(0, dot) : name;
        np.start();
    }

    private static float volume(Minecraft mc, ElytrixConfig cfg) {
        float music = mc.options.getSoundSourceVolume(SoundSource.MUSIC);
        return Math.max(0f, Math.min(1f, music * cfg.musicVolume / 100f));
    }

    /** Поток воспроизведения одного трека. */
    private static final class Player extends Thread {
        private final Path file;
        volatile float volume;
        private volatile boolean halted;

        Player(Path file, float volume) {
            super("Elytrix music");
            setDaemon(true);
            this.file = file;
            this.volume = volume;
        }

        void halt() {
            halted = true;
        }

        @Override
        public void run() {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".ogg")) {
                runOgg();
            } else {
                runPcm();
            }
        }

        // ── OGG через STBVorbis (потоково) ─────────────────────────────
        private void runOgg() {
            long handle = 0;
            int source = 0;
            int[] buffers = new int[BUFFERS];
            ShortBuffer pcm = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                handle = STBVorbis.stb_vorbis_open_filename(file.toAbsolutePath().toString(), error, null);
                if (handle == 0) {
                    ElytrixclientClient.LOG.add("[Elytrix] Не удалось открыть " + file.getFileName()
                            + " (ошибка vorbis " + error.get(0) + ")");
                    return;
                }
                int channels;
                int rate;
                try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
                    STBVorbis.stb_vorbis_get_info(handle, info);
                    channels = info.channels();
                    rate = info.sample_rate();
                }
                if (channels < 1 || channels > 2) {
                    ElytrixclientClient.LOG.add("[Elytrix] " + file.getFileName() + ": поддерживаются моно и стерео");
                    return;
                }
                int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                pcm = MemoryUtil.memAllocShort(CHUNK_FRAMES * channels);

                source = AL10.alGenSources();
                AL10.alGenBuffers(buffers);
                AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, 0f);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0f);
                AL10.alSourcef(source, AL10.AL_GAIN, 0f);

                boolean eof = false;
                for (int b : buffers) {
                    if (!fillOgg(handle, channels, rate, format, b, pcm)) {
                        eof = true;
                        break;
                    }
                    AL10.alSourceQueueBuffers(source, b);
                }
                AL10.alSourcePlay(source);

                float gain = 0f;
                while (!halted) {
                    gain += (volume - gain) * 0.2f;
                    AL10.alSourcef(source, AL10.AL_GAIN, gain);

                    int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0 && !eof) {
                        int b = AL10.alSourceUnqueueBuffers(source);
                        if (fillOgg(handle, channels, rate, format, b, pcm)) {
                            AL10.alSourceQueueBuffers(source, b);
                        } else {
                            eof = true;
                        }
                    }
                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    if (state != AL10.AL_PLAYING) {
                        int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                        if (eof && queued - AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED) <= 0) {
                            break;
                        }
                        AL10.alSourcePlay(source);
                    }
                    Thread.sleep(40L);
                }
                if (halted) {
                    for (int i = 0; i < 8; i++) {
                        gain *= 0.6f;
                        AL10.alSourcef(source, AL10.AL_GAIN, gain);
                        Thread.sleep(20L);
                    }
                }
            } catch (Throwable t) {
                ElytrixclientClient.LOG.add("[Elytrix] Музыка (ogg): " + t);
            } finally {
                try {
                    if (source != 0) {
                        AL10.alSourceStop(source);
                        AL10.alSourcei(source, AL10.AL_BUFFER, 0);
                        AL10.alDeleteSources(source);
                        AL10.alDeleteBuffers(buffers);
                    }
                } catch (Throwable ignored) {
                }
                if (handle != 0) {
                    STBVorbis.stb_vorbis_close(handle);
                }
                if (pcm != null) {
                    MemoryUtil.memFree(pcm);
                }
            }
        }

        private static boolean fillOgg(long handle, int channels, int rate, int format, int buffer, ShortBuffer pcm) {
            pcm.clear();
            int frames = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, pcm);
            if (frames <= 0) {
                return false;
            }
            pcm.limit(frames * channels);
            AL10.alBufferData(buffer, format, pcm, rate);
            return true;
        }

        // ── MP3/другие форматы через javax.sound.sampled → OpenAL ──────
        private void runPcm() {
            int source = 0;
            int[] buffers = new int[BUFFERS];
            AudioInputStream ais = null;
            try {
                ais = AudioSystem.getAudioInputStream(file.toFile());
                AudioFormat fmt = ais.getFormat();

                // конвертируем в PCM signed 16-bit little-endian если нужно
                if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED
                        || fmt.getSampleSizeInBits() != 16) {
                    AudioFormat target = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            fmt.getSampleRate(), 16, fmt.getChannels(),
                            fmt.getChannels() * 2, fmt.getSampleRate(), false);
                    ais = AudioSystem.getAudioInputStream(target, ais);
                    fmt = target;
                }

                int channels = fmt.getChannels();
                int rate = (int) fmt.getSampleRate();
                int alFormat = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                int frameBytes = channels * 2;

                source = AL10.alGenSources();
                AL10.alGenBuffers(buffers);
                AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
                AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, 0f);
                AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0f);
                AL10.alSourcef(source, AL10.AL_GAIN, 0f);

                byte[] chunk = new byte[CHUNK_FRAMES * frameBytes];
                boolean eof = false;

                for (int b : buffers) {
                    if (!fillPcm(ais, chunk, b, alFormat, rate)) {
                        eof = true;
                        break;
                    }
                    AL10.alSourceQueueBuffers(source, b);
                }
                AL10.alSourcePlay(source);

                float gain = 0f;
                while (!halted) {
                    gain += (volume - gain) * 0.2f;
                    AL10.alSourcef(source, AL10.AL_GAIN, gain);

                    int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
                    while (processed-- > 0 && !eof) {
                        int b = AL10.alSourceUnqueueBuffers(source);
                        if (fillPcm(ais, chunk, b, alFormat, rate)) {
                            AL10.alSourceQueueBuffers(source, b);
                        } else {
                            eof = true;
                        }
                    }
                    int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                    if (state != AL10.AL_PLAYING) {
                        int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                        if (eof && queued - AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED) <= 0) {
                            break;
                        }
                        AL10.alSourcePlay(source);
                    }
                    Thread.sleep(40L);
                }
                if (halted) {
                    for (int i = 0; i < 8; i++) {
                        gain *= 0.6f;
                        AL10.alSourcef(source, AL10.AL_GAIN, gain);
                        Thread.sleep(20L);
                    }
                }
            } catch (Throwable t) {
                ElytrixclientClient.LOG.add("[Elytrix] Музыка (mp3): " + t);
            } finally {
                try {
                    if (source != 0) {
                        AL10.alSourceStop(source);
                        AL10.alSourcei(source, AL10.AL_BUFFER, 0);
                        AL10.alDeleteSources(source);
                        AL10.alDeleteBuffers(buffers);
                    }
                } catch (Throwable ignored) {
                }
                try {
                    if (ais != null) ais.close();
                } catch (Throwable ignored) {
                }
            }
        }

        private static boolean fillPcm(AudioInputStream ais, byte[] chunk, int buffer, int alFormat, int rate) throws IOException {
            int total = 0;
            while (total < chunk.length) {
                int r = ais.read(chunk, total, chunk.length - total);
                if (r <= 0) break;
                total += r;
            }
            if (total <= 0) return false;
            ByteBuffer bb = MemoryUtil.memAlloc(total);
            try {
                bb.put(chunk, 0, total);
                bb.flip();
                AL10.alBufferData(buffer, alFormat, bb, rate);
            } finally {
                MemoryUtil.memFree(bb);
            }
            return true;
        }
    }
}