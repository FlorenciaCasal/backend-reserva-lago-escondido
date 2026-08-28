package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.NewsSocialContentRequest;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsSocialContent;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.repo.NewsRepository;
import com.luismunozse.reservalago.repo.NewsSocialContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NewsSocialContentService {

    private final NewsRepository newsRepository;
    private final NewsSocialContentRepository newsSocialContentRepository;

    public List<NewsSocialContentResponse> listByNews(UUID newsId) {
        ensureNewsExists(newsId);
        return newsSocialContentRepository.findByNewsIdOrderByPlatformAsc(newsId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NewsSocialContentResponse save(UUID newsId, SocialPlatform platform, NewsSocialContentRequest request) {
        News news = findNews(newsId);
        NewsSocialContent content = newsSocialContentRepository.findByNewsIdAndPlatform(newsId, platform)
                .orElseGet(() -> {
                    NewsSocialContent created = new NewsSocialContent();
                    created.setNews(news);
                    created.setPlatform(platform);
                    return created;
                });

        content.setCaption(blankToNull(request.caption()));
        content.setBody(blankToNull(request.body()));
        content.setHashtags(blankToNull(request.hashtags()));
        content.setCallToAction(blankToNull(request.callToAction()));
        content.setAltText(blankToNull(request.altText()));

        return toResponse(newsSocialContentRepository.save(content));
    }

    private void ensureNewsExists(UUID newsId) {
        if (!newsRepository.existsById(newsId)) {
            throw new ResponseStatusException(NOT_FOUND, "Novedad no encontrada");
        }
    }

    private News findNews(UUID newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Novedad no encontrada"));
    }

    private NewsSocialContentResponse toResponse(NewsSocialContent content) {
        return new NewsSocialContentResponse(
                content.getId(),
                content.getNews().getId(),
                content.getPlatform(),
                content.getCaption(),
                content.getBody(),
                content.getHashtags(),
                content.getCallToAction(),
                content.getAltText(),
                content.getCreatedAt(),
                content.getUpdatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
