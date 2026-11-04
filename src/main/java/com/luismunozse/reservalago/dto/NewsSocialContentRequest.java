package com.luismunozse.reservalago.dto;

import jakarta.validation.constraints.Size;

public record NewsSocialContentRequest(
        String caption,
        String body,
        String hashtags,

        @Size(max = 300, message = "El CTA no puede superar 300 caracteres")
        String callToAction,

        String altText
) {
}
