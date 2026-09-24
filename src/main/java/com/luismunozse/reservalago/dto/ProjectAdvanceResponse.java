package com.luismunozse.reservalago.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectAdvanceResponse(
        UUID id,
        UUID projectId,
        LocalDate advanceDate,
        String title,
        String description,
        String imageUrl,
        UUID imageAssetId,
        String videoUrl,
        UUID videoAssetId,
        Instant createdAt,
        Instant updatedAt,
        List<MediaGalleryItemResponse> gallery
) {
}
