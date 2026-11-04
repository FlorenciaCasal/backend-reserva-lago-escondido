package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateNewsImageRequest;
import com.luismunozse.reservalago.dto.NewsImageResponse;
import com.luismunozse.reservalago.dto.UpdateNewsImageRequest;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsImage;
import com.luismunozse.reservalago.repo.NewsImageRepository;
import com.luismunozse.reservalago.repo.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NewsImageService {

    private final NewsRepository newsRepository;
    private final NewsImageRepository newsImageRepository;
    private final MediaAssetService mediaAssetService;

    public List<NewsImageResponse> listByNewsId(UUID newsId) {
        findNews(newsId);
        return listResponses(newsId);
    }

    public NewsImageResponse create(UUID newsId, CreateNewsImageRequest request) {
        News news = findNews(newsId);

        NewsImage image = new NewsImage();
        image.setNews(news);
        applyFields(image, request.imageUrl(), request.mediaAssetId(), request.altText(), request.caption(), request.sortOrder());

        return toResponse(newsImageRepository.save(image));
    }

    public NewsImageResponse update(UUID newsId, UUID imageId, UpdateNewsImageRequest request) {
        NewsImage image = findImage(newsId, imageId);
        applyFields(image, request.imageUrl(), request.mediaAssetId(), request.altText(), request.caption(), request.sortOrder());
        return toResponse(newsImageRepository.save(image));
    }

    public void delete(UUID newsId, UUID imageId) {
        NewsImage image = findImage(newsId, imageId);
        newsImageRepository.delete(image);
    }

    public List<NewsImageResponse> listResponses(UUID newsId) {
        return newsImageRepository.findByNewsIdOrderBySortOrderAscCreatedAtAsc(newsId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private News findNews(UUID newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Novedad no encontrada"));
    }

    private NewsImage findImage(UUID newsId, UUID imageId) {
        return newsImageRepository.findByIdAndNewsId(imageId, newsId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Imagen no encontrada"));
    }

    private void applyFields(NewsImage image, String imageUrl, UUID mediaAssetId, String altText, String caption, Integer sortOrder) {
        MediaAsset mediaAsset = mediaAssetId == null ? null : mediaAssetService.findImageAsset(mediaAssetId);
        image.setMediaAsset(mediaAsset);
        image.setImageUrl(mediaAsset == null ? normalizePublicImageUrl(imageUrl) : "/api/media/" + mediaAsset.getId());
        image.setAltText(blankToNull(altText));
        image.setCaption(blankToNull(caption));
        image.setSortOrder(sortOrder == null ? 0 : sortOrder);
    }

    private String normalizePublicImageUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("/img/") || trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            return trimmed;
        }

        throw new ResponseStatusException(
                BAD_REQUEST,
                "La imagen debe ser un upload valido, http(s) o un asset interno /img/..."
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private NewsImageResponse toResponse(NewsImage image) {
        return new NewsImageResponse(
                image.getId(),
                image.getNews().getId(),
                image.getMediaAsset() == null ? null : image.getMediaAsset().getId(),
                image.getImageUrl(),
                image.getAltText(),
                image.getCaption(),
                image.getSortOrder(),
                image.getCreatedAt(),
                image.getUpdatedAt()
        );
    }
}
