package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.MediaAssetKind;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        MediaAssetKind kind,
        String url,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksum,
        Instant createdAt
) {
}