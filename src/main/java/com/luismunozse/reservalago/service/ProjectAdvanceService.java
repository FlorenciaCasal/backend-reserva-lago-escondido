package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateProjectAdvanceRequest;
import com.luismunozse.reservalago.dto.ProjectAdvanceResponse;
import com.luismunozse.reservalago.dto.UpdateProjectAdvanceRequest;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectAdvance;
import com.luismunozse.reservalago.model.ProjectStatus;
import com.luismunozse.reservalago.repo.ProjectAdvanceRepository;
import com.luismunozse.reservalago.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectAdvanceService {

    private final ProjectRepository projectRepository;
    private final ProjectAdvanceRepository projectAdvanceRepository;
    private final MediaAssetService mediaAssetService;
    private final ProjectAdvanceGalleryItemService projectAdvanceGalleryItemService;

    public List<ProjectAdvanceResponse> listPublicByProjectSlug(String slug) {
        Project project = projectRepository.findBySlugAndStatus(slug, ProjectStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
        return listByProject(project.getId());
    }

    public List<ProjectAdvanceResponse> listAdminByProjectId(UUID projectId) {
        findProject(projectId);
        return listByProject(projectId);
    }

    public ProjectAdvanceResponse create(UUID projectId, CreateProjectAdvanceRequest request) {
        Project project = findProject(projectId);
        ProjectAdvance advance = new ProjectAdvance();
        advance.setProject(project);
        applyFields(advance, request.advanceDate(), request.title(), request.description(), request.imageUrl(), request.imageAssetId(), request.videoUrl(), request.videoAssetId());
        return toResponse(projectAdvanceRepository.save(advance));
    }

    public ProjectAdvanceResponse update(UUID projectId, UUID advanceId, UpdateProjectAdvanceRequest request) {
        ProjectAdvance advance = findAdvance(projectId, advanceId);
        applyFields(advance, request.advanceDate(), request.title(), request.description(), request.imageUrl(), request.imageAssetId(), request.videoUrl(), request.videoAssetId());
        return toResponse(projectAdvanceRepository.save(advance));
    }

    public void delete(UUID projectId, UUID advanceId) {
        ProjectAdvance advance = findAdvance(projectId, advanceId);
        projectAdvanceRepository.delete(advance);
    }

    private List<ProjectAdvanceResponse> listByProject(UUID projectId) {
        return projectAdvanceRepository.findByProjectIdOrderByAdvanceDateAscCreatedAtAscIdAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
    }

    private ProjectAdvance findAdvance(UUID projectId, UUID advanceId) {
        return projectAdvanceRepository.findByIdAndProjectId(advanceId, projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Avance no encontrado"));
    }

    private void applyFields(ProjectAdvance advance, java.time.LocalDate advanceDate, String title, String description, String imageUrl, UUID imageAssetId, String videoUrl, UUID videoAssetId) {
        advance.setAdvanceDate(advanceDate);
        advance.setTitle(title.trim());
        advance.setDescription(description.trim());

        MediaAsset imageAsset = imageAssetId == null ? null : mediaAssetService.findImageAsset(imageAssetId);
        advance.setImageAsset(imageAsset);
        advance.setImageUrl(imageAsset == null ? normalizePublicUrl(imageUrl, "imagen") : "/api/media/" + imageAsset.getId());

        MediaAsset videoAsset = videoAssetId == null ? null : mediaAssetService.findVideoAsset(videoAssetId);
        advance.setVideoAsset(videoAsset);
        advance.setVideoUrl(videoAsset == null ? normalizePublicUrl(videoUrl, "video") : "/api/media/" + videoAsset.getId());
    }

    private String normalizePublicUrl(String value, String fieldLabel) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.startsWith("/") || trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            return trimmed;
        }
        throw new ResponseStatusException(BAD_REQUEST, "La URL de " + fieldLabel + " debe ser publica o un asset interno");
    }

    private ProjectAdvanceResponse toResponse(ProjectAdvance advance) {
        return new ProjectAdvanceResponse(
                advance.getId(),
                advance.getProject().getId(),
                advance.getAdvanceDate(),
                advance.getTitle(),
                advance.getDescription(),
                advance.getImageUrl(),
                advance.getImageAsset() == null ? null : advance.getImageAsset().getId(),
                advance.getVideoUrl(),
                advance.getVideoAsset() == null ? null : advance.getVideoAsset().getId(),
                advance.getCreatedAt(),
                advance.getUpdatedAt(),
                projectAdvanceGalleryItemService.listResponses(advance.getId())
        );
    }
}
