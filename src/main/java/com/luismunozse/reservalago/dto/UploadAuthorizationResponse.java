package com.luismunozse.reservalago.dto;

import java.time.Instant;

public record UploadAuthorizationResponse(String token, Instant expiresAt) {
}
