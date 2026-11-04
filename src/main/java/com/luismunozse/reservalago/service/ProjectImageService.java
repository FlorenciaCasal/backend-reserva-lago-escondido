package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateProjectImageRequest;
import com.luismunozse.reservalago.dto.ProjectImageResponse;
import com.luismunozse.reservalago.dto.UpdateProjectImageRequest;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectImage;
import com.luismunozse.reservalago.repo.ProjectImageRepository;
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
public class ProjectImageService {

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final MediaAssetService mediaAssetService;

    public List<ProjectImageResponse> listByProjectId(UUID projectId) {
        findProject(projectId);
        return listResponses(projectId);
    }

    public ProjectImageResponse create(UUID projectId, CreateProjectImageRequest request) {
        Project project = findProject(projectId);

        ProjectImage image = new ProjectImage();
        image.setProject(project);
        applyFields(image, request.imageUrl(), request.mediaAssetId(), request.altText(), request.caption(), request.sortOrder());

        return toResponse(projectImageRepository.save(image));
    }

    public ProjectImageResponse update(UUID projectId, UUID imageId, UpdateProjectImageRequest request) {
        ProjectImage image = findImage(projectId, imageId);
        applyFields(image, request.imageUrl(), request.mediaAssetId(), request.altText(), request.caption(), request.sortOrder());
        return toResponse(projectImageRepository.save(image));
    }

    public void delete(UUID projectId, UUID imageId) {
        ProjectImage image = findImage(projectId, imageId);
        projectImageRepository.delete(image);
    }

    public List<ProjectImageResponse> listResponses(UUID projectId) {
        return projectImageRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
    }

    private ProjectImage findImage(UUID projectId, UUID imageId) {
        return projectImageRepository.findByIdAndProjectId(imageId, projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Imagen no encontrada"));
    }

    private void applyFields(ProjectImage image, String imageUrl, UUID mediaAssetId, String altText, String caption, Integer sortOrder) {
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
                "La imagen debe ser un upload válido, http(s) o un asset interno /img/..."
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ProjectImageResponse toResponse(ProjectImage image) {
        return new ProjectImageResponse(
                image.getId(),
                image.getProject().getId(),
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