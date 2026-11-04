package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateProjectDocumentRequest;
import com.luismunozse.reservalago.dto.ProjectDocumentResponse;
import com.luismunozse.reservalago.dto.UpdateProjectDocumentRequest;
import com.luismunozse.reservalago.model.MediaAsset;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectDocument;
import com.luismunozse.reservalago.model.ProjectStatus;
import com.luismunozse.reservalago.repo.ProjectDocumentRepository;
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
public class ProjectDocumentService {

    private final ProjectRepository projectRepository;
    private final ProjectDocumentRepository projectDocumentRepository;
    private final MediaAssetService mediaAssetService;

    public List<ProjectDocumentResponse> listPublicByProjectSlug(String slug) {
        Project project = projectRepository.findBySlugAndStatus(slug, ProjectStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));

        return listResponses(project.getId());
    }

    public List<ProjectDocumentResponse> listByProjectId(UUID projectId) {
        findProject(projectId);
        return listResponses(projectId);
    }

    public ProjectDocumentResponse create(UUID projectId, CreateProjectDocumentRequest request) {
        Project project = findProject(projectId);

        ProjectDocument document = new ProjectDocument();
        document.setProject(project);
        applyFields(document, request.title(), request.description(), request.fileUrl(), request.mediaAssetId(), request.fileType(), request.sortOrder());

        return toResponse(projectDocumentRepository.save(document));
    }

    public ProjectDocumentResponse update(UUID projectId, UUID documentId, UpdateProjectDocumentRequest request) {
        ProjectDocument document = findDocument(projectId, documentId);
        applyFields(document, request.title(), request.description(), request.fileUrl(), request.mediaAssetId(), request.fileType(), request.sortOrder());
        return toResponse(projectDocumentRepository.save(document));
    }

    public void delete(UUID projectId, UUID documentId) {
        ProjectDocument document = findDocument(projectId, documentId);
        projectDocumentRepository.delete(document);
    }

    public List<ProjectDocumentResponse> listResponses(UUID projectId) {
        return projectDocumentRepository.findByProjectIdOrderBySortOrderAscCreatedAtAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));
    }

    private ProjectDocument findDocument(UUID projectId, UUID documentId) {
        return projectDocumentRepository.findByIdAndProjectId(documentId, projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Documento no encontrado"));
    }

    private void applyFields(
            ProjectDocument document,
            String title,
            String description,
            String fileUrl,
            UUID mediaAssetId,
            String fileType,
            Integer sortOrder
    ) {
        document.setTitle(title.trim());
        document.setDescription(blankToNull(description));
        document.setSortOrder(sortOrder == null ? 0 : sortOrder);

        MediaAsset mediaAsset = mediaAssetId == null ? null : mediaAssetService.findDocumentAsset(mediaAssetId);
        document.setMediaAsset(mediaAsset);
        if (mediaAsset != null) {
            document.setFileUrl("/api/media/" + mediaAsset.getId());
            document.setFileType(blankToNull(fileType) == null ? documentTypeLabel(mediaAsset.getContentType()) : blankToNull(fileType));
            return;
        }

        document.setFileUrl(normalizePublicFileUrl(fileUrl));
        document.setFileType(blankToNull(fileType));
    }

    private String normalizePublicFileUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (
                trimmed.startsWith("/docs/") ||
                trimmed.startsWith("/files/") ||
                trimmed.startsWith("/api/media/") ||
                trimmed.startsWith("https://") ||
                trimmed.startsWith("http://")
        ) {
            return trimmed;
        }

        throw new ResponseStatusException(
                BAD_REQUEST,
                "La URL del documento debe ser http(s), /docs/..., /files/... o /api/media/..."
        );
    }

    private String documentTypeLabel(String contentType) {
        if ("application/pdf".equals(contentType)) return "PDF";
        if ("application/msword".equals(contentType)) return "DOC";
        if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) return "DOCX";
        return null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ProjectDocumentResponse toResponse(ProjectDocument document) {
        return new ProjectDocumentResponse(
                document.getId(),
                document.getProject().getId(),
                document.getTitle(),
                document.getDescription(),
                document.getFileUrl(),
                document.getMediaAsset() == null ? null : document.getMediaAsset().getId(),
                document.getFileType(),
                document.getSortOrder(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}