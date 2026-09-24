package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.MediaGalleryItemRequest;
import com.luismunozse.reservalago.dto.MediaGalleryItemResponse;
import com.luismunozse.reservalago.model.ProjectAdvance;
import com.luismunozse.reservalago.model.ProjectAdvanceGalleryItem;
import com.luismunozse.reservalago.repo.ProjectAdvanceGalleryItemRepository;
import com.luismunozse.reservalago.repo.ProjectAdvanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectAdvanceGalleryItemService {

    private final ProjectAdvanceRepository projectAdvanceRepository;
    private final ProjectAdvanceGalleryItemRepository repository;
    private final MediaGalleryItemMapper mapper;

    public List<MediaGalleryItemResponse> listByAdvanceId(UUID projectId, UUID advanceId) {
        findAdvance(projectId, advanceId);
        return listResponses(advanceId);
    }

    public List<MediaGalleryItemResponse> listResponses(UUID advanceId) {
        return repository.findByAdvanceIdOrderBySortOrderAscCreatedAtAsc(advanceId)
                .stream()
                .map(item -> mapper.toResponse(item, advanceId))
                .toList();
    }

    public MediaGalleryItemResponse create(UUID projectId, UUID advanceId, MediaGalleryItemRequest request) {
        ProjectAdvance advance = findAdvance(projectId, advanceId);
        ProjectAdvanceGalleryItem item = new ProjectAdvanceGalleryItem();
        item.setAdvance(advance);
        mapper.apply(item, request);
        return mapper.toResponse(repository.save(item), advanceId);
    }

    public MediaGalleryItemResponse update(UUID projectId, UUID advanceId, UUID itemId, MediaGalleryItemRequest request) {
        findAdvance(projectId, advanceId);
        ProjectAdvanceGalleryItem item = findItem(advanceId, itemId);
        mapper.apply(item, request);
        return mapper.toResponse(repository.save(item), advanceId);
    }

    public void delete(UUID projectId, UUID advanceId, UUID itemId) {
        findAdvance(projectId, advanceId);
        repository.delete(findItem(advanceId, itemId));
    }

    private ProjectAdvance findAdvance(UUID projectId, UUID advanceId) {
        return projectAdvanceRepository.findByIdAndProjectId(advanceId, projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Avance no encontrado"));
    }

    private ProjectAdvanceGalleryItem findItem(UUID advanceId, UUID itemId) {
        return repository.findByIdAndAdvanceId(itemId, advanceId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item multimedia no encontrado"));
    }
}
