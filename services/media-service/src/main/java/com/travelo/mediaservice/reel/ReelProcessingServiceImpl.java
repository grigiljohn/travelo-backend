package com.travelo.mediaservice.reel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.DoubleConsumer;

/**
 * FFmpeg pipeline optimized for short clips: {@code veryfast}, CRF 23, {@code +faststart}.
 */
@Service
public class ReelProcessingServiceImpl implements ReelProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ReelProcessingServiceImpl.class);

    private static final String FFMPEG = "ffmpeg";
    private static final String FFPROBE = "ffprobe";
    private static final int OUT_W = 720;
    private static final int OUT_H = 1280;

    // Stage-percent ranges used for smoothing within each ffmpeg run.
    // Keep in sync with MediaController#percentForStage (those values are the
    // coarse defaults used when no fine-grained percent is available).
    private static final int PCT_OPTIMIZING_START = 15;
    private static final int PCT_OPTIMIZING_END = 30;
    private static final int PCT_FILTERING_START = 30;
    private static final int PCT_FILTERING_END = 78;
    private static final int PCT_MUSIC_START = 78;
    private static final int PCT_MUSIC_END = 92;

    private final ReelFilterService filterService;
    private final ReelMusicCatalogService musicCatalog;
    private final ReelJobProgressTracker progressTracker;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public ReelProcessingServiceImpl(ReelFilterService filterService,
                                     ReelMusicCatalogService musicCatalog,
                                     ReelJobProgressTracker progressTracker) {
        this.filterService = filterService;
        this.musicCatalog = musicCatalog;
        this.progressTracker = progressTracker;
    }

    @Override
    public File processReelDelivery(File inputVideo,
                                    UUID jobId,
                                    ReelFilterType filterType,
                                    boolean musicEnabled,
                                    String progressJobId)
            throws IOException, InterruptedException {
        Path tmp = Path.of(System.getProperty("java.io.tmpdir"), "reel_pipeline_" + jobId);
        Files.createDirectories(tmp);
        File trimmed = tmp.resolve("trim_" + jobId + ".mp4").toFile();
        File scaled = tmp.resolve("scaled_" + jobId + ".mp4").toFile();
        File musicFile = tmp.resolve("music_" + jobId + ".mp3").toFile();
        File output = tmp.resolve("reel_out_" + jobId + ".mp4").toFile();

        try {
            report(progressJobId, ReelJobStage.OPTIMIZING, "Optimizing video", PCT_OPTIMIZING_START);
            double duration = probeDurationSeconds(inputVideo);
            TrimWindow tw = computeTrim(duration);
            runFfmpegTrim(inputVideo, trimmed, tw.startSec(), tw.lenSec());
            report(progressJobId, ReelJobStage.OPTIMIZING, "Optimizing video", PCT_OPTIMIZING_END);

            report(progressJobId, ReelJobStage.FILTERING, "Applying filter", PCT_FILTERING_START);
            String vf = buildScaleFilterChain(filterType);
            boolean trimmedAudio = hasAudioStream(trimmed);
            runFfmpegScaleEncode(
                    trimmed, scaled, vf, trimmedAudio, tw.lenSec(),
                    progressJobId, ReelJobStage.FILTERING,
                    PCT_FILTERING_START, PCT_FILTERING_END);

            double outDur = probeDurationSeconds(scaled);
            boolean srcAudio = trimmedAudio && hasAudioStream(scaled);

            if (musicEnabled) {
                report(progressJobId, ReelJobStage.MUSIC, "Adding music", PCT_MUSIC_START);
                ReelMusicCategory cat = musicCatalog.categoryForFilter(filterType);
                ReelMusicTrack track = musicCatalog.pickRandom(cat);
                downloadToFile(track.url(), musicFile);
                runFfmpegMuxMusic(
                        scaled, musicFile, output, outDur, srcAudio,
                        progressJobId, PCT_MUSIC_START, PCT_MUSIC_END);
            } else {
                Files.copy(scaled.toPath(), output.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            if (!output.exists() || output.length() < 1024) {
                throw new IOException("reel output missing or too small");
            }
            return output;
        } finally {
            deleteQuiet(trimmed);
            deleteQuiet(scaled);
            deleteQuiet(musicFile);
        }
    }

    private void report(String jobId, ReelJobStage stage, String message, Integer percent) {
        if (jobId == null || jobId.isBlank() || progressTracker == null) {
            return;
        }
        progressTracker.report(jobId, stage, message, percent);
    }

    private void downloadToFile(String url, File dest) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() / 100 != 2) {
            throw new IOException("music download failed http=" + res.statusCode());
        }
        Files.write(dest.toPath(), res.body());
    }

    private String buildScaleFilterChain(ReelFilterType type) {
        String fx = filterService.buildVideoFilter(type);
        String scale = String.format(
                "scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,format=yuv420p",
                OUT_W, OUT_H, OUT_W, OUT_H);
        if (fx == null || fx.isBlank()) {
            return scale;
        }
        return fx + "," + scale;
    }

    record TrimWindow(double startSec, double lenSec) {}

    static TrimWindow computeTrim(double duration) {
        if (duration <= 0) {
            return new TrimWindow(0, 0.5);
        }
        if (duration > 30) {
            return new TrimWindow(0, 30);
        }
        if (duration > 15) {
            double seg = Math.min(20, Math.max(5, duration - 4));
            double start = Math.max(0, (duration - seg) / 2.0);
            return new TrimWindow(start, seg);
        }
        return new TrimWindow(0, duration);
    }

    private void runFfmpegTrim(File in, File out, double start, double len) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(FFMPEG);
        cmd.add("-y");
        cmd.add("-ss");
        cmd.add(String.format(java.util.Locale.US, "%.3f", start));
        cmd.add("-i");
        cmd.add(in.getAbsolutePath());
        cmd.add("-t");
        cmd.add(String.format(java.util.Locale.US, "%.3f", len));
        cmd.add("-c");
        cmd.add("copy");
        cmd.add("-avoid_negative_ts");
        cmd.add("make_zero");
        cmd.add(out.getAbsolutePath());
        int code = runProcess(cmd);
        if (code != 0 || !out.exists()) {
            throw new IOException("ffmpeg trim failed exit=" + code);
        }
    }

    private void runFfmpegScaleEncode(File in,
                                      File out,
                                      String vf,
                                      boolean withAudio,
                                      double expectedDurationSec,
                                      String progressJobId,
                                      ReelJobStage stage,
                                      int pctStart,
                                      int pctEnd) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(FFMPEG);
        cmd.add("-y");
        cmd.add("-hide_banner");
        cmd.add("-nostats");
        cmd.add("-i");
        cmd.add(in.getAbsolutePath());
        cmd.add("-vf");
        cmd.add(vf);
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("veryfast");
        cmd.add("-crf");
        cmd.add("23");
        cmd.add("-pix_fmt");
        cmd.add("yuv420p");
        cmd.add("-movflags");
        cmd.add("+faststart");
        if (withAudio) {
            cmd.add("-c:a");
            cmd.add("aac");
            cmd.add("-b:a");
            cmd.add("128k");
            cmd.add("-ar");
            cmd.add("48000");
        } else {
            cmd.add("-an");
        }
        cmd.add("-progress");
        cmd.add("pipe:1");
        cmd.add(out.getAbsolutePath());
        int code = runFfmpegWithProgress(
                cmd, expectedDurationSec,
                fraction -> reportInterpolated(progressJobId, stage, pctStart, pctEnd, fraction));
        if (code != 0 || !out.exists()) {
            throw new IOException("ffmpeg scale/filter failed exit=" + code);
        }
    }

    private void runFfmpegMuxMusic(File video,
                                   File music,
                                   File out,
                                   double videoDurSec,
                                   boolean videoHasAudio,
                                   String progressJobId,
                                   int pctStart,
                                   int pctEnd)
            throws IOException, InterruptedException {
        String durStr = String.format(java.util.Locale.US, "%.3f", Math.max(0.5, videoDurSec));
        String filter;
        if (videoHasAudio) {
            filter = "[0:a]volume=0.12[a0];[1:a]atrim=0:" + durStr
                    + ",volume=0.92,asetpts=PTS-STARTPTS[a1];[a0][a1]amix=inputs=2:duration=first:dropout_transition=0[mx];[mx]dynaudnorm=f=150:g=15[aout]";
        } else {
            filter = "[1:a]atrim=0:" + durStr + ",volume=0.96,asetpts=PTS-STARTPTS[mx];[mx]dynaudnorm=f=150:g=15[aout]";
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(FFMPEG);
        cmd.add("-y");
        cmd.add("-hide_banner");
        cmd.add("-nostats");
        cmd.add("-i");
        cmd.add(video.getAbsolutePath());
        cmd.add("-i");
        cmd.add(music.getAbsolutePath());
        cmd.add("-filter_complex");
        cmd.add(filter);
        cmd.add("-map");
        cmd.add("0:v");
        cmd.add("-map");
        cmd.add("[aout]");
        cmd.add("-c:v");
        cmd.add("copy");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add("160k");
        cmd.add("-shortest");
        cmd.add("-progress");
        cmd.add("pipe:1");
        cmd.add(out.getAbsolutePath());
        int code = runFfmpegWithProgress(
                cmd, videoDurSec,
                fraction -> reportInterpolated(progressJobId, ReelJobStage.MUSIC, pctStart, pctEnd, fraction));
        if (code != 0 || !out.exists()) {
            throw new IOException("ffmpeg music mux failed exit=" + code);
        }
    }

    private void reportInterpolated(String jobId, ReelJobStage stage,
                                    int pctStart, int pctEnd, double fraction) {
        if (jobId == null || jobId.isBlank()) {
            return;
        }
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        int pct = (int) Math.round(pctStart + (pctEnd - pctStart) * clamped);
        progressTracker.report(jobId, stage, null, pct);
    }

    /**
     * Runs ffmpeg with {@code -progress pipe:1} and parses the key=value stream to
     * emit fractional progress (0.0 → 1.0). stderr is drained concurrently and only
     * the tail is kept for logging. Returns the ffmpeg exit code.
     */
    private static int runFfmpegWithProgress(List<String> cmd,
                                             double expectedDurationSec,
                                             DoubleConsumer fractionSink)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();

        StringBuilder errTail = new StringBuilder();
        Thread errReader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    synchronized (errTail) {
                        if (errTail.length() < 8000) {
                            errTail.append(line).append('\n');
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }, "ffmpeg-stderr");
        errReader.setDaemon(true);
        errReader.start();

        long expectedUs = expectedDurationSec > 0
                ? (long) (expectedDurationSec * 1_000_000d)
                : 0L;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (fractionSink == null || expectedUs <= 0) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0 || eq >= line.length() - 1) {
                    continue;
                }
                String key = line.substring(0, eq);
                String value = line.substring(eq + 1).trim();
                long outTimeUs = -1;
                if ("out_time_us".equals(key) || "out_time_ms".equals(key)) {
                    // ffmpeg's -progress uses "out_time_ms" but actually emits microseconds.
                    outTimeUs = parseLongSafe(value);
                } else if ("progress".equals(key) && "end".equals(value)) {
                    try {
                        fractionSink.accept(1.0);
                    } catch (Exception ignored) {
                    }
                }
                if (outTimeUs > 0) {
                    double f = Math.min(0.99, (double) outTimeUs / (double) expectedUs);
                    try {
                        fractionSink.accept(f);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        int exit = p.waitFor();
        errReader.join(2_000);
        if (exit != 0) {
            log.warn("ffmpeg exit={} cmd={} tail=\n{}", exit, String.join(" ", cmd), errTail);
        }
        return exit;
    }

    private static long parseLongSafe(String s) {
        if (s == null || s.isEmpty() || "N/A".equalsIgnoreCase(s)) {
            return -1;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int runProcess(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder err = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (err.length() < 8000) {
                    err.append(line).append('\n');
                }
            }
        }
        int exit = p.waitFor();
        if (exit != 0) {
            log.warn("ffmpeg exit={} cmd={} tail=\n{}", exit, String.join(" ", cmd), err);
        }
        return exit;
    }

    private static double probeDurationSeconds(File f) throws IOException, InterruptedException {
        List<String> cmd = List.of(
                FFPROBE, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", f.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = r.readLine();
        }
        int code = p.waitFor();
        if (code != 0 || out == null) {
            return 0;
        }
        try {
            return Double.parseDouble(out.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean hasAudioStream(File f) throws IOException, InterruptedException {
        List<String> cmd = List.of(
                FFPROBE, "-v", "error", "-select_streams", "a",
                "-show_entries", "stream=index", "-of", "csv=p=0", f.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        String out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = r.readLine();
        }
        int code = p.waitFor();
        return code == 0 && out != null && !out.isBlank();
    }

    private static void deleteQuiet(File f) {
        try {
            if (f != null && f.exists()) {
                Files.delete(f.toPath());
            }
        } catch (Exception ignored) {
        }
    }
}
