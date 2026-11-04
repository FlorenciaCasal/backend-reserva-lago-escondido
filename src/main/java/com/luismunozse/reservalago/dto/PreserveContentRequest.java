package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreserveContentRequest(
        @NotBlank
        @Size(max = 500)
        String intro,

        @NotEmpty
        @Size(max = 6)
        List<@NotBlank @Size(max = 40) String> heroAsideLines,

        @NotBlank
        @Size(max = 80)
        String whatEyebrow,

        @NotBlank
        @Size(max = 1200)
        String whatWeDoText,

        @NotEmpty
        @Size(max = 12)
        List<@NotBlank @Size(max = 180) String> bullets,

        @NotBlank
        @Size(max = 100)
        String territoryEyebrow,

        @NotBlank
        @Size(max = 160)
        String territoryTitle,

        @NotBlank
        @Size(max = 700)
        String territoryText,

        @NotBlank
        @Size(max = 30)
        String territoryMetricValue,

        @NotBlank
        @Size(max = 140)
        String territoryMetricDescription,

        @NotBlank
        @Size(max = 80)
        String territoryResearchTitle,

        @NotBlank
        @Size(max = 180)
        String territoryResearchDescription,

        @NotBlank
        @Size(max = 80)
        String territoryEducationTitle,

        @NotBlank
        @Size(max = 180)
        String territoryEducationDescription
) {
}
