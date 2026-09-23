package com.luismunozse.reservalago.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HomeContentRequest(
        @NotBlank
        @Size(max = 180)
        String heroTitle,

        @NotBlank
        @Size(max = 220)
        String heroSubtitle,

        @NotBlank
        @Size(max = 500)
        String heroImageUrl,

        @NotBlank
        @Size(max = 500)
        String introText,

        @NotBlank
        @Size(max = 100)
        String actionTitle,

        @Valid
        @NotNull
        HomePillarContentRequest conservar,

        @Valid
        @NotNull
        HomePillarContentRequest habitar,

        @Valid
        @NotNull
        HomePillarContentRequest producir,

        @NotBlank
        @Size(max = 100)
        String projectsTitle,

        @NotBlank
        @Size(max = 80)
        String projectsCtaLabel,

        @NotBlank
        @Size(max = 100)
        String newsTitle,

        @NotBlank
        @Size(max = 80)
        String newsCtaLabel,

        @NotBlank
        @Size(max = 80)
        String visitsEyebrow,

        @NotBlank
        @Size(max = 160)
        String visitsTitle,

        @NotBlank
        @Size(max = 500)
        String visitsText,

        @NotBlank
        @Size(max = 80)
        String visitsCtaLabel,

        @NotBlank
        @Size(max = 500)
        String visitsImageUrl
) {
}
