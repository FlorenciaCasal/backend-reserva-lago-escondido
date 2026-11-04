package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProjectDocumentRequest(
        @jakarta.validation.constraints.NotBlank(message = "El titulo es obligatorio")
        @Size(max = 180, message = "El titulo no puede superar 180 caracteres")
        String title,

        @Size(max = 600, message = "La descripcion no puede superar 600 caracteres")
        String description,

        @Size(max = 500, message = "La URL del archivo no puede superar 500 caracteres")
        String fileUrl,

        UUID mediaAssetId,

        @Size(max = 80, message = "El tipo de archivo no puede superar 80 caracteres")
        String fileType,

        @PositiveOrZero(message = "El orden no puede ser negativo")
        Integer sortOrder
) {
}