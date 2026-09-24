package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.NewsStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NewsResponse(
        UUID id,
        String title,
        String summary,
        String content,
        String slug,
        UUID imageAssetId,
        String imageUrl,
        UUID videoAssetId,
        String videoUrl,
        NewsStatus status,
        Instant publishedAt,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt,
        List<NewsImageResponse> images,
        List<MediaGalleryItemResponse> gallery
) {
}
