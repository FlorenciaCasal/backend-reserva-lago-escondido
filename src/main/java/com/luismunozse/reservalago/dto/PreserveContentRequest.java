package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreserveContentRequest(
        @NotBlank
        @Size(max = 500)
        String intro,

        @NotBlank
        @Size(max = 1200)
        String whatWeDoText,

        @NotEmpty
        @Size(max = 12)
        List<@NotBlank @Size(max = 180) String> bullets
) {
}
