package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateNewsRequest;
import com.luismunozse.reservalago.dto.NewsResponse;
import com.luismunozse.reservalago.dto.UpdateNewsRequest;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsStatus;
import com.luismunozse.reservalago.repo.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsImageService newsImageService;
    private final NewsGalleryItemService newsGalleryItemService;
    private final MediaAssetService mediaAssetService;

    public List<NewsResponse> listPublished() {
        return newsRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(NewsStatus.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NewsResponse getPublishedBySlug(String slug) {
        return newsRepository.findBySlugAndStatus(slug, NewsStatus.PUBLISHED)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Novedad no encontrada"));
    }

    public List<NewsResponse> listAdmin() {
        return newsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NewsResponse getAdmin(UUID id) {
        return toResponse(findNews(id));
    }

    public NewsResponse create(CreateNewsRequest request) {
        String slug = normalizeSlug(request.slug() == null || request.slug().isBlank() ? request.title() : request.slug());
        if (newsRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe una novedad con ese slug");
        }

        News news = new News();
        news.setTitle(request.title().trim());
        news.setSummary(request.summary().trim());
        news.setContent(request.content().trim());
        news.setSlug(slug);
        applyImage(news, request.imageUrl(), request.imageAssetId());
        applyVideo(news, request.videoUrl(), request.videoAssetId());
        NewsStatus requestedStatus = request.status() == null ? NewsStatus.DRAFT : request.status();
        if (requestedStatus == NewsStatus.ARCHIVED) {
            throw new ResponseStatusException(BAD_REQUEST, "Una novedad nueva debe guardarse como borrador o publicada");
        }
        applyStatus(news, requestedStatus);

        return toResponse(newsRepository.save(news));
    }

    public NewsResponse update(UUID id, UpdateNewsRequest request) {
        News news = findNews(id);
        String slug = normalizeSlug(request.slug() == null || request.slug().isBlank() ? request.title() : request.slug());
        if (newsRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe una novedad con ese slug");
        }

        news.setTitle(request.title().trim());
        news.setSummary(request.summary().trim());
        news.setContent(request.content().trim());
        news.setSlug(slug);
        applyImage(news, request.imageUrl(), request.imageAssetId());
        applyVideo(news, request.videoUrl(), request.videoAssetId());
        if (request.status() != null) {
            validateTransition(news.getStatus(), request.status());
            applyStatus(news, request.status());
        }

        return toResponse(newsRepository.save(news));
    }

    public NewsResponse publish(UUID id) {
        News news = findNews(id);
        applyStatus(news, NewsStatus.PUBLISHED);
        return toResponse(newsRepository.save(news));
    }

    public NewsResponse archive(UUID id) {
        News news = findNews(id);
        applyStatus(news, NewsStatus.ARCHIVED);
        return toResponse(newsRepository.save(news));
    }

    @Transactional
    public void deletePermanently(UUID id) {
        News news = findNews(id);
        if (news.getStatus() != NewsStatus.ARCHIVED) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden eliminar definitivamente novedades archivadas");
        }

        newsRepository.delete(news);
    }
    private NewsResponse toResponse(News news) {
        return new NewsResponse(
                news.getId(),
                news.getTitle(),
                news.getSummary(),
                news.getContent(),
                news.getSlug(),
                news.getImageAsset() == null ? null : news.getImageAsset().getId(),
                news.getImageUrl(),
                news.getVideoAsset() == null ? null : news.getVideoAsset().getId(),
                news.getVideoUrl(),
                news.getStatus(),
                news.getPublishedAt(),
                news.getArchivedAt(),
                news.getCreatedAt(),
                news.getUpdatedAt(),
                newsImageService.listResponses(news.getId()),
                newsGalleryItemService.listResponses(news.getId())
        );
    }

    private News findNews(UUID id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Novedad no encontrada"));
    }

    private void validateTransition(NewsStatus currentStatus, NewsStatus requestedStatus) {
        if (currentStatus == NewsStatus.PUBLISHED && requestedStatus == NewsStatus.DRAFT) {
            throw new ResponseStatusException(BAD_REQUEST, "Una novedad publicada no puede volver a borrador");
        }
    }

    private void applyStatus(News news, NewsStatus status) {
        news.setStatus(status);
        if (status == NewsStatus.PUBLISHED && news.getPublishedAt() == null) {
            news.setPublishedAt(Instant.now());
        }
        if (status == NewsStatus.DRAFT) {
            news.setPublishedAt(null);
        }
        if (status == NewsStatus.ARCHIVED) {
            news.setArchivedAt(Instant.now());
        } else {
            news.setArchivedAt(null);
        }
    }

    private void applyImage(News news, String imageUrl, UUID imageAssetId) {
        MediaAsset imageAsset = imageAssetId == null ? null : mediaAssetService.findImageAsset(imageAssetId);
        news.setImageAsset(imageAsset);
        news.setImageUrl(imageAsset == null ? blankToNull(imageUrl) : "/api/media/" + imageAsset.getId());
    }

    private void applyVideo(News news, String videoUrl, UUID videoAssetId) {
        MediaAsset videoAsset = videoAssetId == null ? null : mediaAssetService.findVideoAsset(videoAssetId);
        news.setVideoAsset(videoAsset);
        news.setVideoUrl(videoAsset == null ? blankToNull(videoUrl) : "/api/media/" + videoAsset.getId());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeSlug(String value) {
        String slug = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isBlank()) slug = "novedad";
        return slug.length() > 160 ? slug.substring(0, 160).replaceAll("-+$", "") : slug;
    }
}
