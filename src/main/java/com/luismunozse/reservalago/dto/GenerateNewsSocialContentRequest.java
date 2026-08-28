package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.Size;

public record GenerateNewsSocialContentRequest(
        @Size(max = 1000, message = "Las indicaciones no pueden superar 1000 caracteres")
        String instructions
) {
}
