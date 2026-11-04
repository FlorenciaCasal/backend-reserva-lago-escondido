package com.luismunozse.reservalago.dto;

import com.luismunozse.reservalago.model.MediaAssetKind;
import jakarta.validation.constraints.NotNull;

public record UploadAuthorizationRequest(@NotNull MediaAssetKind kind) {
}
