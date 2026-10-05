package platform.client.utils.media;

import static platform.api.module.Interface.aM_;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * What Windows itself is playing. The system keeps one media session for whatever app is in charge of the
 * play button - Spotify, a browser on SoundCloud or YouTube, anything that shows up on the volume overlay -
 * and hands out its title, artist, position and source. That is read here through a small PowerShell script
 * on a thread of its own, so a slow answer never holds up a frame.
 */
public final class MediaSession {

    /** The library answers in a moment, so the track is asked for often - the closer the reads, the tighter the lyrics sit on the song. */
    private static final long POLL_MS = 350L;
    private static final String SCRIPT = """
            $ErrorActionPreference = 'SilentlyContinue'
            [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            $null = [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions]
            $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
                $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
            function Await($WinRtTask, $ResultType) {
                $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
                $netTask = $asTask.Invoke($null, @($WinRtTask))
                $netTask.Wait(-1) | Out-Null
                $netTask.Result
            }
            [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null
            $manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
            $session = $manager.GetCurrentSession()
            if ($session -eq $null) { exit }
            $props = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            $timeline = $session.GetTimelineProperties()
            $playback = $session.GetPlaybackInfo()
            $title = $props.Title
            $artist = $props.Artist
            $source = $session.SourceAppUserModelId
            $position = [int]$timeline.Position.TotalMilliseconds
            $duration = [int]$timeline.EndTime.TotalMilliseconds
            $status = $playback.PlaybackStatus
            # The cover travels as a stream; it is written next to the client so the game can load it as a texture
            $coverPath = Join-Path $PSScriptRoot 'cover.png'
            $stampPath = Join-Path $PSScriptRoot 'cover.txt'
            $key = "$artist|$title"
            $coverStamp = ''
            $known = ''
            if (Test-Path $stampPath) { $known = (Get-Content $stampPath -Raw).Trim() }
            if ($known -eq $key -and (Test-Path $coverPath)) {
                $coverStamp = $key
            } elseif ($props.Thumbnail -ne $null) {
                try {
                    $ref = Await ($props.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                    $size = [uint32]$ref.Size
                    $reader = [Windows.Storage.Streams.DataReader]::new($ref.GetInputStreamAt(0))
                    Await ($reader.LoadAsync($size)) ([uint32]) | Out-Null
                    $buffer = $reader.ReadBuffer($size)
                    $bytes = [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions]::ToArray($buffer)
                    [System.IO.File]::WriteAllBytes($coverPath, $bytes)
                    Set-Content -Path $stampPath -Value $key -NoNewline
                    $coverStamp = $key
                } catch {
                    Set-Content -Path (Join-Path $PSScriptRoot 'cover_error.txt') -Value $_.Exception.Message
                }
            }
            Write-Output "$title`t$artist`t$source`t$position`t$duration`t$status`t$coverStamp"
            """;

    private static volatile String title = "";
    private static volatile String artist = "";
    private static volatile String source = "";
    private static volatile long positionMs;
    private static volatile long durationMs;
    private static volatile boolean playing;
    /** When the position above was read; playback is counted on from it between reads. */
    private static volatile long readAt;
    private static volatile boolean present;
    /** Which track the cover on disk belongs to, so the texture is only re-read when it changes. */
    private static volatile String coverKey = "";
    /** The position last shown, so the bar only ever moves forward within one track. */
    private static volatile long smoothed;
    /** When the smooth clock was last advanced. */
    private static volatile long lastFrame;
    /** When a position behind our clock was first reported, to tell a seek from a stray reading. */
    private static volatile long backwardSince;
    private static volatile String previousTitle = "";
    /** The album art of the track playing, as png bytes, when the player gives one. */
    private static volatile byte[] artwork;
    /** Set once the media library cannot be used at all, so it is not tried again every second. */
    private static volatile boolean libraryBroken;
    private static Thread worker;
    private static volatile long lastWanted;

    private MediaSession() {
    }

    /** Called by whatever wants to show the track; the poller only runs while something is asking. */
    public static void keepAlive() {
        lastWanted = System.currentTimeMillis();
        start();
    }

    public static boolean present() {
        return present;
    }

    public static String title() {
        return title;
    }

    public static String artist() {
        return artist;
    }

    /** The app it is coming from, as a name to show: Spotify, SoundCloud, Chrome and so on. */
    public static String source() {
        return prettySource(source);
    }

    /** The track the cover on hand belongs to; empty when there is no cover. */
    public static String coverKey() {
        return artwork != null && artwork.length > 0 ? title + "|" + artist : coverKey;
    }

    /** The album art as png bytes, or null when the player gives none. */
    public static byte[] artwork() {
        return artwork;
    }

    /** Where the cover was written by the script. */
    public static java.io.File coverFile() {
        return new java.io.File(new java.io.File(aM_.gameDirectory, "xivivide"), "cover.png");
    }

    public static boolean playing() {
        return playing;
    }

    public static long durationMs() {
        return durationMs;
    }

    /**
     * Where the track is now. Between reads the clock is simply counted on, and a read that comes back a
     * little behind that count is ignored - the reported position lags by a moment and would otherwise pull
     * the bar backwards every time. Only a real jump, from skipping or a new track, is taken as it comes.
     */
    public static long positionMs() {
        long now = System.currentTimeMillis();
        if (lastFrame == 0L) {
            lastFrame = now;
            smoothed = positionMs;
        }
        // A clock of our own, running in real time. What the player reports comes in whole seconds, so
        // following it directly makes the bar and the lyrics lurch forward once a second; it is used only
        // to catch a real jump - a seek, a new track, a pause that drifted.
        if (playing) {
            smoothed += now - lastFrame;
        }
        lastFrame = now;
        long reported = positionMs + (playing ? now - readAt : 0L);
        // Only a real jump is followed. The reported value lags and is rounded down, so treating every
        // small difference as truth used to drag the bar backwards a step at a time.
        if (reported > smoothed + 1200L) {
            smoothed = reported;
            backwardSince = 0L;
        } else if (reported < smoothed - 3000L) {
            // Going back means a seek - but players also report a stray zero for a moment while they think.
            // A step back is only taken once it has been reported for a while.
            if (backwardSince == 0L) {
                backwardSince = now;
            } else if (now - backwardSince > 700L) {
                smoothed = reported;
                backwardSince = 0L;
            }
        } else {
            backwardSince = 0L;
        }
        if (smoothed < 0L) {
            smoothed = 0L;
        }
        return durationMs > 0L ? Math.min(smoothed, durationMs) : smoothed;
    }

    private static synchronized void start() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        worker = new Thread(MediaSession::loop, "Xivivide-Media");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loop() {
        while (System.currentTimeMillis() - lastWanted < 5000L) {
            poll();
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException interrupted) {
                return;
            }
        }
        present = false;
    }

    /**
     * Asks Windows through the media session library: it hands over the track, where it is, and the album
     * art as png bytes in one call. When that library is missing or says nothing, the script below is used
     * instead - it knows the same things, only slower and without the picture.
     */
    private static boolean pollLibrary() {
        try {
            java.util.List<dev.redstones.mediaplayerinfo.IMediaSession> sessions =
                    dev.redstones.mediaplayerinfo.MediaPlayerInfo.Instance.getMediaSessions();
            if (sessions == null || sessions.isEmpty()) {
                return false;
            }
            dev.redstones.mediaplayerinfo.IMediaSession session = sessions.get(0);
            dev.redstones.mediaplayerinfo.MediaInfo media = session.getMedia();
            if (media == null || media.getTitle() == null || media.getTitle().isBlank()) {
                return false;
            }
            String newTitle = media.getTitle().trim();
            artist = media.getArtist() == null ? "" : media.getArtist().trim();
            source = session.getOwner() == null ? "" : session.getOwner();
            // The library counts in seconds
            // The library counts whole seconds, so the real position is half a second further on average -
            // without that half second the lyrics and the bar always trail the song
            positionMs = media.getPosition() * 1000L + 500L;
            durationMs = media.getDuration() * 1000L;
            playing = media.getPlaying();
            readAt = System.currentTimeMillis();
            byte[] art = media.getArtworkPng();
            if (art != null && art.length > 0) {
                artwork = art;
            } else if (artwork != null && !newTitle.equals(previousTitle)) {
                // A track without art keeps no picture from the one before it
                artwork = null;
            }
            if (art == null && !newTitle.equals(previousTitle)) {
                System.out.println("Xivivide: no album art for " + newTitle);
            }
            if (!newTitle.equals(previousTitle)) {
                previousTitle = newTitle;
                smoothed = positionMs;
                lastFrame = System.currentTimeMillis();
            }
            title = newTitle;
            present = true;
            return true;
        } catch (Throwable throwable) {
            libraryBroken = true;
            return false;
        }
    }

    private static void poll() {
        if (!libraryBroken && pollLibrary()) {
            return;
        }
        try {
            File script = scriptFile();
            ProcessBuilder builder = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-File", script.getAbsolutePath());
            builder.redirectErrorStream(false);
            Process process = builder.start();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                line = reader.readLine();
            }
            process.waitFor();
            if (line == null || line.isBlank()) {
                fallback();
                return;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 6) {
                fallback();
                return;
            }
            coverKey = parts.length > 6 ? parts[6].trim() : "";
            title = parts[0].trim();
            artist = parts[1].trim();
            source = parts[2].trim();
            positionMs = parseLong(parts[3]);
            durationMs = parseLong(parts[4]);
            playing = parts[5].trim().equalsIgnoreCase("Playing");
            readAt = System.currentTimeMillis();
            if (!title.equals(previousTitle)) {
                // A different song: start the clock over instead of carrying the last one's position
                previousTitle = title;
                smoothed = positionMs;
            }
            present = !title.isEmpty();
        } catch (Exception exception) {
            fallback();
        }
    }

    /**
     * When Windows gives out no media session - an older build, or a player that does not report one - the
     * window title of a running player is read instead. Those read "Artist - Title", which is enough for the
     * name and for looking the lyrics up; there is no position to be had that way, so the bar stays still.
     */
    private static void fallback() {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", "Get-Process spotify,aimp,foobar2000,vlc,yandexmusic,'VK Музыка' "
                    + "-ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle } | "
                    + "Select-Object -First 1 | ForEach-Object { \"$($_.ProcessName)`t$($_.MainWindowTitle)\" }");
            Process process = builder.start();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                line = reader.readLine();
            }
            process.waitFor();
            if (line == null || line.isBlank()) {
                present = false;
                return;
            }
            String[] parts = line.split("\t", -1);
            String window = parts.length > 1 ? parts[1].trim() : "";
            // A player with nothing playing shows its own name as the title
            if (window.isEmpty() || window.equalsIgnoreCase(parts[0].trim())) {
                present = false;
                return;
            }
            int dash = window.indexOf(" - ");
            artist = dash > 0 ? window.substring(0, dash).trim() : "";
            title = dash > 0 ? window.substring(dash + 3).trim() : window;
            source = parts[0].trim();
            positionMs = 0L;
            durationMs = 0L;
            playing = true;
            readAt = System.currentTimeMillis();
            present = !title.isEmpty();
        } catch (Exception exception) {
            present = false;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    /** The script lives next to the client's own files and is written once. */
    private static File scriptFile() throws Exception {
        File folder = new File(aM_.gameDirectory, "xivivide");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File script = new File(folder, "media.ps1");
        // Written every time: an older copy from a previous build would otherwise stay in place forever
        String current = script.exists() ? Files.readString(script.toPath(), StandardCharsets.UTF_8) : "";
        if (!current.equals(SCRIPT)) {
            Files.writeString(script.toPath(), SCRIPT, StandardCharsets.UTF_8);
        }
        return script;
    }

    /**
     * Turns the app id Windows reports into the name of the service behind it. A browser is named by the
     * site it is playing from when that can be told, since that is what the music is really coming from.
     */
    private static String prettySource(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.contains("spotify")) return "Spotify";
        if (lower.contains("soundcloud")) return "SoundCloud";
        if (lower.contains("yandex") || lower.contains("music.yandex")) return "Яндекс Музыка";
        if (lower.contains("vk")) return "VK Музыка";
        if (lower.contains("deezer")) return "Deezer";
        if (lower.contains("tidal")) return "TIDAL";
        if (lower.contains("apple") || lower.contains("itunes")) return "Apple Music";
        if (lower.contains("youtube")) return "YouTube";
        if (lower.contains("chrome")) return "Chrome";
        if (lower.contains("msedge") || lower.contains("edge")) return "Edge";
        if (lower.contains("firefox")) return "Firefox";
        if (lower.contains("opera")) return "Opera";
        if (lower.contains("zen")) return "Zen";
        if (lower.contains("aimp")) return "AIMP";
        if (lower.contains("foobar")) return "foobar2000";
        if (lower.contains("vlc")) return "VLC";
        int dot = id.indexOf('.');
        String name = dot > 0 ? id.substring(0, dot) : id;
        name = name.replace("exe", "").replace("!", "").trim();
        return name.isEmpty() ? "Музыка" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
