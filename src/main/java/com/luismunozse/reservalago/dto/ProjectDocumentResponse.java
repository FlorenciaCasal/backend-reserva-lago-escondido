package com.luismunozse.reservalago.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectDocumentResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String fileUrl,
        UUID mediaAssetId,
        String fileType,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}