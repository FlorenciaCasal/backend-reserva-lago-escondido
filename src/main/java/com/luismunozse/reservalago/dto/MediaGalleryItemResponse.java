package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.ExternalMediaProvider;
import com.luismunozse.reservalago.model.MediaGalleryKind;
import com.luismunozse.reservalago.model.MediaGallerySourceType;

import java.time.Instant;
import java.util.UUID;

public record MediaGalleryItemResponse(
        UUID id,
        UUID ownerId,
        MediaGalleryKind kind,
        MediaGallerySourceType sourceType,
        UUID mediaAssetId,
        String url,
        ExternalMediaProvider externalProvider,
        String externalVideoId,
        String embedUrl,
        String thumbnailUrl,
        String caption,
        String altText,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
