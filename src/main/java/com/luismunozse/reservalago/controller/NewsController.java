package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateNewsImageRequest;
import com.luismunozse.reservalago.dto.CreateNewsRequest;
import com.luismunozse.reservalago.dto.GenerateNewsRequest;
import com.luismunozse.reservalago.dto.GenerateNewsSocialContentRequest;
import com.luismunozse.reservalago.dto.GeneratedNewsDraft;
import com.luismunozse.reservalago.dto.NewsImageResponse;
import com.luismunozse.reservalago.dto.NewsResponse;
import com.luismunozse.reservalago.dto.NewsSocialContentRequest;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.dto.UpdateNewsImageRequest;
import com.luismunozse.reservalago.dto.UpdateNewsRequest;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.service.NewsAiService;
import com.luismunozse.reservalago.service.NewsImageService;
import com.luismunozse.reservalago.service.NewsService;
import com.luismunozse.reservalago.service.NewsSocialContentService;
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
    private final NewsSocialContentService newsSocialContentService;
    private final NewsAiService newsAiService;

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

    @PostMapping("/api/admin/news/generate")
    public GeneratedNewsDraft generateNews(@Valid @RequestBody GenerateNewsRequest request) {
        return newsAiService.generate(request);
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

    @DeleteMapping("/api/admin/news/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNewsPermanently(@PathVariable UUID id) {
        newsService.deletePermanently(id);
    }
    @GetMapping("/api/admin/news/{newsId}/social")
    public List<NewsSocialContentResponse> listNewsSocialContent(@PathVariable UUID newsId) {
        return newsSocialContentService.listByNews(newsId);
    }

    @PutMapping("/api/admin/news/{newsId}/social/{platform}")
    public NewsSocialContentResponse saveNewsSocialContent(
            @PathVariable UUID newsId,
            @PathVariable SocialPlatform platform,
            @Valid @RequestBody NewsSocialContentRequest request
    ) {
        return newsSocialContentService.save(newsId, platform, request);
    }

    @PostMapping("/api/admin/news/{newsId}/social/{platform}/generate")
    public NewsSocialContentResponse generateNewsSocialContent(
            @PathVariable UUID newsId,
            @PathVariable SocialPlatform platform,
            @Valid @RequestBody GenerateNewsSocialContentRequest request
    ) {
        return newsAiService.generateSocialContent(newsId, platform, request);
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
