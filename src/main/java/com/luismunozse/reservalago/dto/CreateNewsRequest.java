package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateNewsRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 160, message = "El titulo no puede superar 160 caracteres")
        String title,

        @NotBlank(message = "El resumen es obligatorio")
        @Size(max = 600, message = "El resumen no puede superar 600 caracteres")
        String summary,

        @NotBlank(message = "El contenido es obligatorio")
        String content,

        @Size(max = 160, message = "El slug no puede superar 160 caracteres")
        String slug,

        @Size(max = 500, message = "La URL de imagen no puede superar 500 caracteres")
        String imageUrl,

        UUID imageAssetId,

        @Size(max = 500, message = "La URL de video no puede superar 500 caracteres")
        String videoUrl,

        UUID videoAssetId,

        NewsStatus status
) {
}
