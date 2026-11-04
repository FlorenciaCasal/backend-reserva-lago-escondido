package com.luismunozse.reservalago.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectImageResponse(
        UUID id,
        UUID projectId,
        UUID mediaAssetId,
        String imageUrl,
        String altText,
        String caption,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
