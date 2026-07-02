package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProjectAdvanceRequest(
        @NotNull(message = "La fecha del avance es obligatoria")
        LocalDate advanceDate,

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 180, message = "El titulo no puede superar 180 caracteres")
        String title,

        @NotBlank(message = "La descripcion es obligatoria")
        String description,

        @Size(max = 500, message = "La URL de imagen no puede superar 500 caracteres")
        String imageUrl,

        UUID imageAssetId,

        @Size(max = 500, message = "La URL de video no puede superar 500 caracteres")
        String videoUrl,

        UUID videoAssetId
) {
}