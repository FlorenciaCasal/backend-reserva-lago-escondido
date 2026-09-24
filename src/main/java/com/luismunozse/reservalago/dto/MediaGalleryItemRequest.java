package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.MediaGalleryKind;
import com.luismunozse.reservalago.model.MediaGallerySourceType;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MediaGalleryItemRequest(
        MediaGalleryKind kind,
        MediaGallerySourceType sourceType,
        UUID mediaAssetId,

        @Size(max = 500, message = "La URL de YouTube no puede superar 500 caracteres")
        String youtubeUrl,

        @Size(max = 100, message = "El ID externo no puede superar 100 caracteres")
        String externalVideoId,

        @Size(max = 300, message = "El epigrafe no puede superar 300 caracteres")
        String caption,

        @Size(max = 180, message = "El texto alternativo no puede superar 180 caracteres")
        String altText,

        @PositiveOrZero(message = "El orden no puede ser negativo")
        Integer sortOrder
) {
}
