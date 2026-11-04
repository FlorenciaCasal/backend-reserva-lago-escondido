package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.MediaGalleryItemRequest;
import com.luismunozse.reservalago.dto.MediaGalleryItemResponse;
import com.luismunozse.reservalago.model.AbstractMediaGalleryItem;
import com.luismunozse.reservalago.model.ExternalMediaProvider;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.MediaGalleryKind;
import com.luismunozse.reservalago.model.MediaGallerySourceType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class MediaGalleryItemMapper {

    private final MediaAssetService mediaAssetService;
    private final YouTubeVideoParser youTubeVideoParser;

    public MediaGalleryItemMapper(MediaAssetService mediaAssetService, YouTubeVideoParser youTubeVideoParser) {
        this.mediaAssetService = mediaAssetService;
        this.youTubeVideoParser = youTubeVideoParser;
    }

    public void apply(AbstractMediaGalleryItem item, MediaGalleryItemRequest request) {
        MediaGalleryKind kind = request.kind();
        MediaGallerySourceType sourceType = request.sourceType();
        if (kind == null || sourceType == null) {
            throw new ResponseStatusException(BAD_REQUEST, "El tipo y origen del item multimedia son obligatorios");
        }
        if (kind == MediaGalleryKind.IMAGE && sourceType != MediaGallerySourceType.MEDIA_ASSET) {
            throw new ResponseStatusException(BAD_REQUEST, "Las imágenes deben usar un archivo subido");
        }

        item.setKind(kind);
        item.setSourceType(sourceType);
        item.setCaption(blankToNull(request.caption()));
        item.setAltText(kind == MediaGalleryKind.IMAGE ? blankToNull(request.altText()) : null);
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());

        if (sourceType == MediaGallerySourceType.MEDIA_ASSET) {
            MediaAsset asset = resolveAsset(kind, request.mediaAssetId());
            item.setMediaAsset(asset);
            item.setSourceUrl("/api/media/" + asset.getId());
            item.setExternalProvider(null);
            item.setExternalVideoId(null);
            return;
        }

        if (kind != MediaGalleryKind.VIDEO || sourceType != MediaGallerySourceType.EXTERNAL_YOUTUBE) {
            throw new ResponseStatusException(BAD_REQUEST, "La combinación de tipo y origen multimedia no es válida");
        }

        String raw = request.youtubeUrl() == null || request.youtubeUrl().isBlank()
                ? request.externalVideoId()
                : request.youtubeUrl();
        String videoId = youTubeVideoParser.normalizeVideoId(raw);
        item.setMediaAsset(null);
        item.setSourceUrl("https://www.youtube.com/watch?v=" + videoId);
        item.setExternalProvider(ExternalMediaProvider.YOUTUBE);
        item.setExternalVideoId(videoId);
    }

    public MediaGalleryItemResponse toResponse(AbstractMediaGalleryItem item, UUID ownerId) {
        String embedUrl = null;
        String thumbnailUrl = null;
        if (item.getSourceType() == MediaGallerySourceType.EXTERNAL_YOUTUBE && item.getExternalVideoId() != null) {
            embedUrl = youTubeVideoParser.embedUrl(item.getExternalVideoId());
            thumbnailUrl = youTubeVideoParser.thumbnailUrl(item.getExternalVideoId());
        }
        return new MediaGalleryItemResponse(
                item.getId(),
                ownerId,
                item.getKind(),
                item.getSourceType(),
                item.getMediaAsset() == null ? null : item.getMediaAsset().getId(),
                item.getSourceUrl(),
                item.getExternalProvider(),
                item.getExternalVideoId(),
                embedUrl,
                thumbnailUrl,
                item.getCaption(),
                item.getAltText(),
                item.getSortOrder(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private MediaAsset resolveAsset(MediaGalleryKind kind, UUID mediaAssetId) {
        if (mediaAssetId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Seleccioná un archivo multimedia válido");
        }
        return kind == MediaGalleryKind.IMAGE
                ? mediaAssetService.findImageAsset(mediaAssetId)
                : mediaAssetService.findVideoAsset(mediaAssetId);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
