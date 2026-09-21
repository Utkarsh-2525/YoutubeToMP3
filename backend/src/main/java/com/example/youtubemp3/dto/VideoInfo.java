package com.example.youtubemp3.dto;

public record VideoInfo(
        String id,
        String title,
        String channel,
        String thumbnail,
        long duration
) {}
