package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProjectImageRequest(
        @Size(max = 500, message = "La URL de imagen no puede superar 500 caracteres")
        String imageUrl,

        UUID mediaAssetId,

        @Size(max = 180, message = "El texto alternativo no puede superar 180 caracteres")
        String altText,

        @Size(max = 300, message = "El epigrafe no puede superar 300 caracteres")
        String caption,

        @PositiveOrZero(message = "El orden no puede ser negativo")
        Integer sortOrder
) {
}
