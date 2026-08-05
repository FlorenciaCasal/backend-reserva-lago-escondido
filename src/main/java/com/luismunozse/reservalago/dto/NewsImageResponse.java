package com.luismunozse.reservalago.dto;

import java.time.Instant;
import java.util.UUID;

public record NewsImageResponse(
        UUID id,
        UUID newsId,
        UUID mediaAssetId,
        String imageUrl,
        String altText,
        String caption,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
