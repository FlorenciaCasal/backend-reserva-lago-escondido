package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.ProjectStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String title,
        String summary,
        String content,
        String slug,
        UUID imageAssetId,
        String imageUrl,
        UUID videoAssetId,
        String videoUrl,
        boolean featured,
        ProjectStatus status,
        Instant publishedAt,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt,
        List<MediaGalleryItemResponse> gallery,
        List<ProjectDocumentResponse> documents
) {
}
