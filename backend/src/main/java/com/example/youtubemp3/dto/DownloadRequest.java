package com.example.youtubemp3.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DownloadRequest(
        @NotBlank(message = "YouTube URL is required")
        String url,

        @Min(value = 64, message = "Bitrate must be at least 64 kbps")
        @Max(value = 320, message = "Bitrate cannot exceed 320 kbps")
        int bitrate
) {}
