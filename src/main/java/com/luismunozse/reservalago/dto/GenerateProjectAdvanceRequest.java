package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GenerateProjectAdvanceRequest(
        @NotBlank(message = "El brief del avance es obligatorio")
        String whatHappened,

        LocalDate advanceDate,

        @Size(max = 1200, message = "Los datos relevantes no pueden superar 1200 caracteres")
        String relevantData,

        @Size(max = 120, message = "El tono no puede superar 120 caracteres")
        String tone
) {
}
