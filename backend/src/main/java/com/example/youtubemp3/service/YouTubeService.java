package com.example.youtubemp3.service;

import com.example.youtubemp3.dto.VideoInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class YouTubeService {

    private static final Pattern YOUTUBE_HOST = Pattern.compile(
            "^(www\\.)?(youtube\\.com|m\\.youtube\\.com|music\\.youtube\\.com|youtu\\.be)$",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.tools.yt-dlp}")
    private String ytDlpPath;

    @Value("${app.tools.ffmpeg}")
    private String ffmpegPath;

    @Value("${app.download-dir}")
    private String downloadDir;

    public VideoInfo getInfo(String url) {
        validateYouTubeUrl(url);

        List<String> command = List.of(
                ytDlpPath,
                "--dump-single-json",
                "--no-warnings",
                "--skip-download",
                "--no-playlist",
                url
        );

        String json = runAndCapture(command, 90);

        try {
            JsonNode root = objectMapper.readTree(json);
            return new VideoInfo(
                    root.path("id").asText(""),
                    root.path("title").asText("Unknown title"),
                    root.path("channel").asText(
                            root.path("uploader").asText("Unknown channel")
                    ),
                    root.path("thumbnail").asText(""),
                    root.path("duration").asLong(0)
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Could not parse yt-dlp metadata",
                    e
            );
        }
    }

    public DownloadedFile downloadMp3(String url, int bitrate) {
        validateYouTubeUrl(url);

        try {
            Path output = Paths.get(downloadDir).toAbsolutePath().normalize();
            Files.createDirectories(output);

            String jobId = UUID.randomUUID().toString();
            Path template = output.resolve(jobId + ".%(ext)s");

            List<String> command = List.of(
                    ytDlpPath,
                    "--no-playlist",
                    "--restrict-filenames",
                    "-f", "bestaudio/best",
                    "-x",
                    "--audio-format", "mp3",
                    "--audio-quality", bitrate + "K",
                    "--ffmpeg-location", ffmpegPath,
                    "-o", template.toString(),
                    url
            );

            runAndCapture(command, 15 * 60);

            Path mp3 = output.resolve(jobId + ".mp3");

            if (!Files.exists(mp3)) {
                throw new IOException("yt-dlp did not produce an MP3 file.");
            }

            String safeName = sanitizeFileName(getInfo(url).title());
            Path finalPath = output.resolve(jobId + "-" + safeName + ".mp3");

            Files.move(mp3, finalPath, StandardCopyOption.REPLACE_EXISTING);

            return new DownloadedFile(
                    finalPath,
                    new FileSystemResource(finalPath)
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "MP3 conversion failed: " + e.getMessage(),
                    e
            );
        }
    }

    private void validateYouTubeUrl(String value) {
        try {
            java.net.URI uri = java.net.URI.create(value.trim());

            if (!"https".equalsIgnoreCase(uri.getScheme())
                    && !"http".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }

            String host = uri.getHost();
            if (host == null || !YOUTUBE_HOST.matcher(host).matches()) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Only valid YouTube URLs are supported."
            );
        }
    }

    private String runAndCapture(List<String> command, long timeoutSeconds) {
        Process process = null;

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            process = builder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Process timed out.");
            }

            if (process.exitValue() != 0) {
                throw new IOException(output.toString().trim());
            }

            return output.toString();

        } catch (IOException e) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "yt-dlp/FFmpeg execution failed. Check the tool paths and installation.",
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Process was interrupted.",
                    e
            );
        }
    }

    private String sanitizeFileName(String name) {
        String cleaned = name
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            cleaned = "audio";
        }

        return cleaned.length() > 120
                ? cleaned.substring(0, 120)
                : cleaned;
    }

    public record DownloadedFile(Path path, Resource resource) {}
}
