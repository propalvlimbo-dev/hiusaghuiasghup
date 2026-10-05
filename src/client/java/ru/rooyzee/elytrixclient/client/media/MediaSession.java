package ru.rooyzee.elytrixclient.client.media;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Windows Media Session — читает текущий трек из системы (Spotify, YouTube, браузер и т.д.)
 * через PowerShell скрипт или mediaplayerinfo библиотеку.
 */
public final class MediaSession {

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
                } catch {}
            }
            Write-Output "$title`t$artist`t$source`t$position`t$duration`t$status`t$coverStamp"
            """;

    private static volatile String title = "";
    private static volatile String artist = "";
    private static volatile String source = "";
    private static volatile long positionMs;
    private static volatile long durationMs;
    private static volatile boolean playing;
    private static volatile long readAt;
    private static volatile boolean present;
    private static volatile String coverKey = "";
    private static volatile long smoothed;
    private static volatile long lastFrame;
    private static volatile long backwardSince;
    private static volatile String previousTitle = "";
    private static volatile byte[] artwork;
    private static volatile boolean libraryBroken;
    private static Thread worker;
    private static volatile long lastWanted;

    private MediaSession() {}

    public static void keepAlive() {
        lastWanted = System.currentTimeMillis();
        start();
    }

    public static boolean present() { return present; }
    public static String title() { return title; }
    public static String artist() { return artist; }

    public static String source() { return prettySource(source); }

    public static String coverKey() {
        return artwork != null && artwork.length > 0 ? title + "|" + artist : coverKey;
    }

    public static byte[] artwork() { return artwork; }

    public static File coverFile() {
        return new File(FabricLoader.getInstance().getGameDir().toFile(), "elytrix/cover.png");
    }

    public static boolean playing() { return playing; }
    public static long durationMs() { return durationMs; }

    public static long positionMs() {
        long now = System.currentTimeMillis();
        if (lastFrame == 0L) { lastFrame = now; smoothed = positionMs; }
        if (playing) smoothed += now - lastFrame;
        lastFrame = now;
        long reported = positionMs + (playing ? now - readAt : 0L);
        if (reported > smoothed + 1200L) { smoothed = reported; backwardSince = 0; }
        else if (reported < smoothed - 3000L) {
            if (backwardSince == 0) backwardSince = now;
            else if (now - backwardSince > 700L) { smoothed = reported; backwardSince = 0; }
        } else backwardSince = 0;
        if (smoothed < 0) smoothed = 0;
        return durationMs > 0 ? Math.min(smoothed, durationMs) : smoothed;
    }

    private static synchronized void start() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(MediaSession::loop, "Elytrix-Media");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loop() {
        while (System.currentTimeMillis() - lastWanted < 5000L) {
            poll();
            try { Thread.sleep(POLL_MS); } catch (InterruptedException e) { return; }
        }
        present = false;
    }

    private static boolean pollLibrary() {
        try {
            java.util.List<dev.redstones.mediaplayerinfo.IMediaSession> sessions =
                    dev.redstones.mediaplayerinfo.MediaPlayerInfo.Instance.getMediaSessions();
            if (sessions == null || sessions.isEmpty()) return false;
            dev.redstones.mediaplayerinfo.IMediaSession session = sessions.get(0);
            dev.redstones.mediaplayerinfo.MediaInfo media = session.getMedia();
            if (media == null || media.getTitle() == null || media.getTitle().isBlank()) return false;
            String newTitle = media.getTitle().trim();
            artist = media.getArtist() == null ? "" : media.getArtist().trim();
            source = session.getOwner() == null ? "" : session.getOwner();
            positionMs = media.getPosition() * 1000L + 500L;
            durationMs = media.getDuration() * 1000L;
            playing = media.getPlaying();
            readAt = System.currentTimeMillis();
            byte[] art = media.getArtworkPng();
            if (art != null && art.length > 0) artwork = art;
            else if (artwork != null && !newTitle.equals(previousTitle)) artwork = null;
            if (!newTitle.equals(previousTitle)) {
                previousTitle = newTitle; smoothed = positionMs; lastFrame = System.currentTimeMillis();
            }
            title = newTitle; present = true; return true;
        } catch (Throwable t) { libraryBroken = true; return false; }
    }

    private static void poll() {
        if (!libraryBroken && pollLibrary()) return;
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
            if (line == null || line.isBlank()) { fallback(); return; }
            String[] parts = line.split("\t", -1);
            if (parts.length < 6) { fallback(); return; }
            coverKey = parts.length > 6 ? parts[6].trim() : "";
            title = parts[0].trim(); artist = parts[1].trim(); source = parts[2].trim();
            positionMs = parseLong(parts[3]); durationMs = parseLong(parts[4]);
            playing = parts[5].trim().equalsIgnoreCase("Playing");
            readAt = System.currentTimeMillis();
            if (!title.equals(previousTitle)) { previousTitle = title; smoothed = positionMs; }
            present = !title.isEmpty();
        } catch (Exception e) { fallback(); }
    }

    private static void fallback() {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-Command", "Get-Process spotify,aimp,foobar2000,vlc,yandexmusic -ErrorAction SilentlyContinue | "
                    + "Where-Object { $_.MainWindowTitle } | Select-Object -First 1 | ForEach-Object { \"$($_.ProcessName)`t$($_.MainWindowTitle)\" }");
            Process process = builder.start();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                line = reader.readLine();
            }
            process.waitFor();
            if (line == null || line.isBlank()) { present = false; return; }
            String[] parts = line.split("\t", -1);
            String window = parts.length > 1 ? parts[1].trim() : "";
            if (window.isEmpty() || window.equalsIgnoreCase(parts[0].trim())) { present = false; return; }
            int dash = window.indexOf(" - ");
            artist = dash > 0 ? window.substring(0, dash).trim() : "";
            title = dash > 0 ? window.substring(dash + 3).trim() : window;
            source = parts[0].trim();
            positionMs = 0; durationMs = 0; playing = true;
            readAt = System.currentTimeMillis();
            present = !title.isEmpty();
        } catch (Exception e) { present = false; }
    }

    private static long parseLong(String v) { try { return Long.parseLong(v.trim()); } catch (NumberFormatException e) { return 0; } }

    private static File scriptFile() throws Exception {
        File folder = FabricLoader.getInstance().getGameDir().resolve("elytrix").toFile();
        if (!folder.exists()) folder.mkdirs();
        File script = new File(folder, "media.ps1");
        String current = script.exists() ? Files.readString(script.toPath(), StandardCharsets.UTF_8) : "";
        if (!current.equals(SCRIPT)) Files.writeString(script.toPath(), SCRIPT, StandardCharsets.UTF_8);
        return script;
    }

    private static String prettySource(String id) {
        if (id == null || id.isEmpty()) return "";
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
        if (lower.contains("aimp")) return "AIMP";
        if (lower.contains("foobar")) return "foobar2000";
        if (lower.contains("vlc")) return "VLC";
        int dot = id.indexOf('.');
        String name = dot > 0 ? id.substring(0, dot) : id;
        name = name.replace("exe", "").replace("!", "").trim();
        return name.isEmpty() ? "Музыка" : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}