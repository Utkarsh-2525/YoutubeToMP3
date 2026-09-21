package com.example.youtubemp3.controller;

import com.example.youtubemp3.dto.DownloadRequest;
import com.example.youtubemp3.dto.VideoInfo;
import com.example.youtubemp3.service.YouTubeService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    private final YouTubeService service;

    public YouTubeController(YouTubeService service) {
        this.service = service;
    }

    @PostMapping("/info")
    public ResponseEntity<VideoInfo> info(
            @RequestBody UrlRequest request) {

        return ResponseEntity.ok(service.getInfo(request.url()));
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> download(
            @Valid @RequestBody DownloadRequest request) {

        YouTubeService.DownloadedFile file =
                service.downloadMp3(request.url(), request.bitrate());

        String filename = file.path().getFileName().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .contentLength(file.path().toFile().length())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .body(file.resource());
    }

    public record UrlRequest(String url) {}
}
