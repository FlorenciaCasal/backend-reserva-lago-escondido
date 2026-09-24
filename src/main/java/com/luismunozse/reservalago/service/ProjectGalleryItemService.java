package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.MediaGalleryItemRequest;
import com.luismunozse.reservalago.dto.MediaGalleryItemResponse;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectGalleryItem;
import com.luismunozse.reservalago.repo.ProjectGalleryItemRepository;
import com.luismunozse.reservalago.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectGalleryItemService {

    private final ProjectRepository projectRepository;
    private final ProjectGalleryItemRepository repository;
    private final MediaGalleryItemMapper mapper;

    public List<MediaGalleryItemResponse> listByProjectId(UUID projectId) {
        findProject(projectId);
        return listResponses(projectId);
    }

    public List<MediaGalleryItemResponse> listResponses(UUID projectId) {
        return repository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)
                .stream()
                .map(item -> mapper.toResponse(item, projectId))
                .toList();
    }

    public MediaGalleryItemResponse create(UUID projectId, MediaGalleryItemRequest request) {
        Project project = findProject(projectId);
        ProjectGalleryItem item = new ProjectGalleryItem();
        item.setProject(project);
        mapper.apply(item, request);
        return mapper.toResponse(repository.save(item), projectId);
    }

    public MediaGalleryItemResponse update(UUID projectId, UUID itemId, MediaGalleryItemRequest request) {
        ProjectGalleryItem item = findItem(projectId, itemId);
        mapper.apply(item, request);
        return mapper.toResponse(repository.save(item), projectId);
    }

    public void delete(UUID projectId, UUID itemId) {
        repository.delete(findItem(projectId, itemId));
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
    }

    private ProjectGalleryItem findItem(UUID projectId, UUID itemId) {
        return repository.findByIdAndProjectId(itemId, projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Item multimedia no encontrado"));
    }
}
