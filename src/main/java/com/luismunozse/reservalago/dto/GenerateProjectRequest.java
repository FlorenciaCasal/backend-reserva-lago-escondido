package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateProjectRequest(
        @NotBlank(message = "La descripcion es obligatoria")
        String description,

        @NotBlank(message = "El objetivo es obligatorio")
        String objective,

        @NotBlank(message = "El publico objetivo es obligatorio")
        String targetAudience,

        @Size(max = 50000, message = "Los aspectos a destacar no pueden superar 50.000 caracteres.")
        String highlights,

        @Size(max = 500, message = "La URL de imagen no puede superar 500 caracteres")
        String imageUrl
) {
}
