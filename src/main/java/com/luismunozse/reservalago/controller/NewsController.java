package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateNewsImageRequest;
import com.luismunozse.reservalago.dto.CreateNewsRequest;
import com.luismunozse.reservalago.dto.NewsImageResponse;
import com.luismunozse.reservalago.dto.NewsResponse;
import com.luismunozse.reservalago.dto.UpdateNewsImageRequest;
import com.luismunozse.reservalago.dto.UpdateNewsRequest;
import com.luismunozse.reservalago.service.NewsImageService;
import com.luismunozse.reservalago.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NewsImageService newsImageService;

    @GetMapping("/api/news")
    public List<NewsResponse> listPublishedNews() {
        return newsService.listPublished();
    }

    @GetMapping("/api/news/{slug}")
    public NewsResponse getPublishedNewsBySlug(@PathVariable String slug) {
        return newsService.getPublishedBySlug(slug);
    }

    @GetMapping("/api/admin/news")
    public List<NewsResponse> listAdminNews() {
        return newsService.listAdmin();
    }

    @GetMapping("/api/admin/news/{id}")
    public NewsResponse getAdminNews(@PathVariable UUID id) {
        return newsService.getAdmin(id);
    }

    @PostMapping("/api/admin/news")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsResponse createNews(@Valid @RequestBody CreateNewsRequest request) {
        return newsService.create(request);
    }

    @PutMapping("/api/admin/news/{id}")
    public NewsResponse updateNews(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNewsRequest request
    ) {
        return newsService.update(id, request);
    }

    @PostMapping("/api/admin/news/{id}/publish")
    public NewsResponse publishNews(@PathVariable UUID id) {
        return newsService.publish(id);
    }

    @PostMapping("/api/admin/news/{id}/archive")
    public NewsResponse archiveNews(@PathVariable UUID id) {
        return newsService.archive(id);
    }

    @GetMapping("/api/admin/news/{newsId}/images")
    public List<NewsImageResponse> listAdminNewsImages(@PathVariable UUID newsId) {
        return newsImageService.listByNewsId(newsId);
    }

    @PostMapping("/api/admin/news/{newsId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsImageResponse createNewsImage(
            @PathVariable UUID newsId,
            @Valid @RequestBody CreateNewsImageRequest request
    ) {
        return newsImageService.create(newsId, request);
    }

    @PutMapping("/api/admin/news/{newsId}/images/{imageId}")
    public NewsImageResponse updateNewsImage(
            @PathVariable UUID newsId,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateNewsImageRequest request
    ) {
        return newsImageService.update(newsId, imageId, request);
    }

    @DeleteMapping("/api/admin/news/{newsId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNewsImage(
            @PathVariable UUID newsId,
            @PathVariable UUID imageId
    ) {
        newsImageService.delete(newsId, imageId);
    }
}
