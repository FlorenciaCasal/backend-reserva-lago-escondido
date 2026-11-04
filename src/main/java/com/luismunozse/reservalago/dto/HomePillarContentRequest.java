package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HomePillarContentRequest(
        @NotBlank
        @Size(max = 80)
        String title,

        @NotBlank
        @Size(max = 20)
        String value,

        @Size(max = 10)
        String suffix,

        @NotBlank
        @Size(max = 120)
        String statLabel,

        @NotBlank
        @Size(max = 220)
        String text,

        @NotBlank
        @Size(max = 40)
        String ctaLabel
) {
}
