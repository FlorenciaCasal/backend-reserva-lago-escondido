package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateProjectRequest;
import com.luismunozse.reservalago.dto.ProjectResponse;
import com.luismunozse.reservalago.dto.UpdateProjectRequest;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectStatus;
import com.luismunozse.reservalago.repo.ProjectRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectGalleryItemService projectGalleryItemService;
    private final ProjectDocumentService projectDocumentService;
    private final MediaAssetService mediaAssetService;

    public List<ProjectResponse> listPublished() {
        return projectRepository.findByStatusOrderByFeaturedDescPublishedAtDescCreatedAtDesc(ProjectStatus.PUBLISHED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getPublishedBySlug(String slug) {
        return projectRepository.findBySlugAndStatus(slug, ProjectStatus.PUBLISHED)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
    }

    public List<ProjectResponse> listAdmin() {
        return projectRepository.findAllByOrderByFeaturedDescCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getAdmin(UUID id) {
        return toResponse(findProject(id));
    }

    public ProjectResponse create(CreateProjectRequest request) {
        String slug = normalizeSlug(request.slug() == null || request.slug().isBlank() ? request.title() : request.slug());
        if (projectRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un proyecto con ese slug");
        }

        Project project = new Project();
        project.setTitle(request.title().trim());
        project.setSummary(request.summary().trim());
        project.setContent(request.content().trim());
        project.setSlug(slug);
        applyImage(project, request.imageUrl(), request.imageAssetId());
        applyVideo(project, request.videoUrl(), request.videoAssetId());
        project.setFeatured(Boolean.TRUE.equals(request.featured()));
        applyStatus(project, request.status() == null ? ProjectStatus.PUBLISHED : request.status());

        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = findProject(id);
        String slug = normalizeSlug(request.slug() == null || request.slug().isBlank() ? request.title() : request.slug());
        if (projectRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(CONFLICT, "Ya existe un proyecto con ese slug");
        }

        project.setTitle(request.title().trim());
        project.setSummary(request.summary().trim());
        project.setContent(request.content().trim());
        project.setSlug(slug);
        applyImage(project, request.imageUrl(), request.imageAssetId());
        applyVideo(project, request.videoUrl(), request.videoAssetId());
        project.setFeatured(Boolean.TRUE.equals(request.featured()));
        if (request.status() != null) {
            applyStatus(project, request.status());
        }

        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse publish(UUID id) {
        Project project = findProject(id);
        applyStatus(project, ProjectStatus.PUBLISHED);
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse unpublish(UUID id) {
        Project project = findProject(id);
        applyStatus(project, ProjectStatus.DRAFT);
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse feature(UUID id, boolean featured) {
        Project project = findProject(id);
        project.setFeatured(featured);
        return toResponse(projectRepository.save(project));
    }

    public ProjectResponse archive(UUID id) {
        Project project = findProject(id);
        applyStatus(project, ProjectStatus.ARCHIVED);
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void deletePermanently(UUID id) {
        Project project = findProject(id);
        if (project.getStatus() != ProjectStatus.ARCHIVED) {
            throw new ResponseStatusException(BAD_REQUEST, "Solo se pueden eliminar definitivamente proyectos archivados");
        }

        projectRepository.delete(project);
    }
    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getSummary(),
                project.getContent(),
                project.getSlug(),
                project.getImageAsset() == null ? null : project.getImageAsset().getId(),
                project.getImageUrl(),
                project.getVideoAsset() == null ? null : project.getVideoAsset().getId(),
                project.getVideoUrl(),
                project.isFeatured(),
                project.getStatus(),
                project.getPublishedAt(),
                project.getArchivedAt(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                projectGalleryItemService.listResponses(project.getId()),
                projectDocumentService.listResponses(project.getId())
        );
    }

    private Project findProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
    }

    private void applyStatus(Project project, ProjectStatus status) {
        project.setStatus(status);
        if (status == ProjectStatus.PUBLISHED && project.getPublishedAt() == null) {
            project.setPublishedAt(Instant.now());
        }
        if (status == ProjectStatus.DRAFT) {
            project.setPublishedAt(null);
        }
        if (status == ProjectStatus.ARCHIVED) {
            project.setArchivedAt(Instant.now());
            project.setFeatured(false);
        } else {
            project.setArchivedAt(null);
        }
    }

    private void applyImage(Project project, String imageUrl, UUID imageAssetId) {
        MediaAsset imageAsset = imageAssetId == null ? null : mediaAssetService.findImageAsset(imageAssetId);
        project.setImageAsset(imageAsset);
        project.setImageUrl(imageAsset == null ? blankToNull(imageUrl) : "/api/media/" + imageAsset.getId());
    }

    private void applyVideo(Project project, String videoUrl, UUID videoAssetId) {
        MediaAsset videoAsset = videoAssetId == null ? null : mediaAssetService.findVideoAsset(videoAssetId);
        project.setVideoAsset(videoAsset);
        project.setVideoUrl(videoAsset == null ? blankToNull(videoUrl) : "/api/media/" + videoAsset.getId());
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
        if (slug.isBlank()) slug = "proyecto";
        return slug.length() > 160 ? slug.substring(0, 160).replaceAll("-+$", "") : slug;
    }
}
