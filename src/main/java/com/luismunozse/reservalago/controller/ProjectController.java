package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.dto.CreateProjectRequest;
import com.luismunozse.reservalago.dto.CreateProjectAdvanceRequest;
import com.luismunozse.reservalago.dto.CreateProjectDocumentRequest;
import com.luismunozse.reservalago.dto.CreateProjectImageRequest;
import com.luismunozse.reservalago.dto.GenerateProjectAdvanceRequest;
import com.luismunozse.reservalago.dto.GenerateProjectRequest;
import com.luismunozse.reservalago.dto.MediaAssetResponse;
import com.luismunozse.reservalago.dto.GeneratedProjectAdvanceDraft;
import com.luismunozse.reservalago.dto.GeneratedProjectDraft;
import com.luismunozse.reservalago.dto.ProjectAdvanceResponse;
import com.luismunozse.reservalago.dto.ProjectDocumentResponse;
import com.luismunozse.reservalago.dto.ProjectImageResponse;
import com.luismunozse.reservalago.dto.ProjectResponse;
import com.luismunozse.reservalago.dto.UpdateProjectAdvanceRequest;
import com.luismunozse.reservalago.dto.UpdateProjectDocumentRequest;
import com.luismunozse.reservalago.dto.UpdateProjectImageRequest;
import com.luismunozse.reservalago.dto.UpdateProjectRequest;
import com.luismunozse.reservalago.service.MediaAssetService;
import com.luismunozse.reservalago.service.ProjectAdvanceAiService;
import com.luismunozse.reservalago.service.ProjectAdvanceService;
import com.luismunozse.reservalago.service.ProjectAiService;
import com.luismunozse.reservalago.service.ProjectDocumentService;
import com.luismunozse.reservalago.service.ProjectImageService;
import com.luismunozse.reservalago.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectAiService projectAiService;
    private final ProjectAdvanceAiService projectAdvanceAiService;
    private final ProjectAdvanceService projectAdvanceService;
    private final ProjectImageService projectImageService;
    private final ProjectDocumentService projectDocumentService;
    private final MediaAssetService mediaAssetService;

    @GetMapping("/api/projects")
    public List<ProjectResponse> listPublishedProjects() {
        return projectService.listPublished();
    }

    @GetMapping("/api/projects/{slug}")
    public ProjectResponse getPublishedProjectBySlug(@PathVariable String slug) {
        return projectService.getPublishedBySlug(slug);
    }

    @GetMapping("/api/projects/{slug}/advances")
    public List<ProjectAdvanceResponse> listPublishedProjectAdvances(@PathVariable String slug) {
        return projectAdvanceService.listPublicByProjectSlug(slug);
    }

    @GetMapping("/api/projects/{slug}/documents")
    public List<ProjectDocumentResponse> listPublishedProjectDocuments(@PathVariable String slug) {
        return projectDocumentService.listPublicByProjectSlug(slug);
    }


    @PostMapping(value = "/api/admin/media/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaAssetResponse uploadProjectImage(@RequestParam("file") MultipartFile file) {
        return mediaAssetService.uploadImage(file);
    }

    @PostMapping(value = "/api/admin/media/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaAssetResponse uploadProjectDocument(@RequestParam("file") MultipartFile file) {
        return mediaAssetService.uploadDocument(file);
    }

    @PostMapping(value = "/api/admin/media/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaAssetResponse uploadProjectVideo(@RequestParam("file") MultipartFile file) {
        return mediaAssetService.uploadVideo(file);
    }

    @GetMapping("/api/media/{id}")
    public ResponseEntity<Resource> getMedia(@PathVariable UUID id) {
        MediaAssetService.ServedMedia media = mediaAssetService.loadForRequest(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .contentLength(media.sizeBytes())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.originalFilename() + "\"")
                .body(media.resource());
    }
    @GetMapping("/api/admin/projects")
    public List<ProjectResponse> listAdminProjects() {
        return projectService.listAdmin();
    }

    @GetMapping("/api/admin/projects/{id}")
    public ProjectResponse getAdminProject(@PathVariable UUID id) {
        return projectService.getAdmin(id);
    }

    @PostMapping("/api/admin/projects/generate")
    public GeneratedProjectDraft generateProjectDraft(@Valid @RequestBody GenerateProjectRequest request) {
        return projectAiService.generate(request);
    }

    @PostMapping("/api/admin/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/api/admin/projects/{id}")
    public ProjectResponse updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return projectService.update(id, request);
    }

    @PostMapping("/api/admin/projects/{id}/publish")
    public ProjectResponse publishProject(@PathVariable UUID id) {
        return projectService.publish(id);
    }

    @PostMapping("/api/admin/projects/{id}/unpublish")
    public ProjectResponse unpublishProject(@PathVariable UUID id) {
        return projectService.unpublish(id);
    }

    @PostMapping("/api/admin/projects/{id}/feature")
    public ProjectResponse featureProject(@PathVariable UUID id) {
        return projectService.feature(id, true);
    }

    @PostMapping("/api/admin/projects/{id}/unfeature")
    public ProjectResponse unfeatureProject(@PathVariable UUID id) {
        return projectService.feature(id, false);
    }

    @PostMapping("/api/admin/projects/{id}/archive")
    public ProjectResponse archiveProject(@PathVariable UUID id) {
        return projectService.archive(id);
    }

    @GetMapping("/api/admin/projects/{projectId}/advances")
    public List<ProjectAdvanceResponse> listAdminProjectAdvances(@PathVariable UUID projectId) {
        return projectAdvanceService.listAdminByProjectId(projectId);
    }

    @PostMapping("/api/admin/projects/{projectId}/advances/generate")
    public GeneratedProjectAdvanceDraft generateProjectAdvanceDraft(
            @PathVariable UUID projectId,
            @Valid @RequestBody GenerateProjectAdvanceRequest request
    ) {
        return projectAdvanceAiService.generate(projectId, request);
    }

    @PostMapping("/api/admin/projects/{projectId}/advances")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectAdvanceResponse createProjectAdvance(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectAdvanceRequest request
    ) {
        return projectAdvanceService.create(projectId, request);
    }

    @PutMapping("/api/admin/projects/{projectId}/advances/{advanceId}")
    public ProjectAdvanceResponse updateProjectAdvance(
            @PathVariable UUID projectId,
            @PathVariable UUID advanceId,
            @Valid @RequestBody UpdateProjectAdvanceRequest request
    ) {
        return projectAdvanceService.update(projectId, advanceId, request);
    }

    @DeleteMapping("/api/admin/projects/{projectId}/advances/{advanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectAdvance(
            @PathVariable UUID projectId,
            @PathVariable UUID advanceId
    ) {
        projectAdvanceService.delete(projectId, advanceId);
    }

    @GetMapping("/api/admin/projects/{projectId}/images")
    public List<ProjectImageResponse> listAdminProjectImages(@PathVariable UUID projectId) {
        return projectImageService.listByProjectId(projectId);
    }

    @PostMapping("/api/admin/projects/{projectId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectImageResponse createProjectImage(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectImageRequest request
    ) {
        return projectImageService.create(projectId, request);
    }

    @PutMapping("/api/admin/projects/{projectId}/images/{imageId}")
    public ProjectImageResponse updateProjectImage(
            @PathVariable UUID projectId,
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateProjectImageRequest request
    ) {
        return projectImageService.update(projectId, imageId, request);
    }

    @DeleteMapping("/api/admin/projects/{projectId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectImage(
            @PathVariable UUID projectId,
            @PathVariable UUID imageId
    ) {
        projectImageService.delete(projectId, imageId);
    }

    @GetMapping("/api/admin/projects/{projectId}/documents")
    public List<ProjectDocumentResponse> listAdminProjectDocuments(@PathVariable UUID projectId) {
        return projectDocumentService.listByProjectId(projectId);
    }

    @PostMapping("/api/admin/projects/{projectId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDocumentResponse createProjectDocument(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectDocumentRequest request
    ) {
        return projectDocumentService.create(projectId, request);
    }

    @PutMapping("/api/admin/projects/{projectId}/documents/{documentId}")
    public ProjectDocumentResponse updateProjectDocument(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateProjectDocumentRequest request
    ) {
        return projectDocumentService.update(projectId, documentId, request);
    }

    @DeleteMapping("/api/admin/projects/{projectId}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectDocument(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId
    ) {
        projectDocumentService.delete(projectId, documentId);
    }
}
