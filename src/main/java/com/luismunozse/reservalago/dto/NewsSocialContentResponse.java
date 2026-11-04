package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.SocialPlatform;

import java.time.Instant;
import java.util.UUID;

public record NewsSocialContentResponse(
        UUID id,
        UUID newsId,
        SocialPlatform platform,
        String caption,
        String body,
        String hashtags,
        String callToAction,
        String altText,
        Instant createdAt,
        Instant updatedAt
) {
}
