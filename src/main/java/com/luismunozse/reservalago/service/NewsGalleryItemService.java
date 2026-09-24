package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.MediaGalleryItemRequest;
import com.luismunozse.reservalago.dto.MediaGalleryItemResponse;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsGalleryItem;
import com.luismunozse.reservalago.repo.NewsGalleryItemRepository;
import com.luismunozse.reservalago.repo.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NewsGalleryItemService {

    private final NewsRepository newsRepository;
    private final NewsGalleryItemRepository repository;
    private final MediaGalleryItemMapper mapper;

    public List<MediaGalleryItemResponse> listByNewsId(UUID newsId) {
        findNews(newsId);
        return listResponses(newsId);
    }

    public List<MediaGalleryItemResponse> listResponses(UUID newsId) {
        return repository.findByNewsIdOrderBySortOrderAscCreatedAtAsc(newsId)
                .stream()
                .map(item -> mapper.toResponse(item, newsId))
                .toList();
    }

    public MediaGalleryItemResponse create(UUID newsId, MediaGalleryItemRequest request) {
        News news = findNews(newsId);
        NewsGalleryItem item = new NewsGalleryItem();
        item.setNews(news);
        mapper.apply(item, request);
        return mapper.toResponse(repository.save(item), newsId);
    }

    public MediaGalleryItemResponse update(UUID newsId, UUID itemId, MediaGalleryItemRequest request) {
        NewsGalleryItem item = findItem(newsId, itemId);
        mapper.apply(item, request);
        return mapper.toResponse(repository.save(item), newsId);
    }

    public void delete(UUID newsId, UUID itemId) {
        repository.delete(findItem(newsId, itemId));
    }

    private News findNews(UUID newsId) {
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Novedad no encontrada"));
    }

    private NewsGalleryItem findItem(UUID newsId, UUID itemId) {
        return repository.findByIdAndNewsId(itemId, newsId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item multimedia no encontrado"));
    }
}
