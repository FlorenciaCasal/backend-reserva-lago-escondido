package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateNewsRequest(
        @NotBlank(message = "El brief es obligatorio")
        String brief,

        @Size(max = 500, message = "El objetivo no puede superar 500 caracteres")
        String objective,

        @Size(max = 500, message = "El publico objetivo no puede superar 500 caracteres")
        String targetAudience,

        @Size(max = 3000, message = "Los aspectos a destacar no pueden superar 3000 caracteres")
        String highlights,

        @Size(max = 500, message = "La URL de imagen no puede superar 500 caracteres")
        String imageUrl
) {
}
