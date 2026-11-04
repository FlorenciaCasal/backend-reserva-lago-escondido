package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateProjectRequest(
        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 140, message = "El titulo no puede superar 140 caracteres")
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

        Boolean featured,

        ProjectStatus status
) {
}