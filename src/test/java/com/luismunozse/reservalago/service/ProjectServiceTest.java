package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectStatus;
import com.luismunozse.reservalago.repo.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectImageService projectImageService;

    @Mock
    private ProjectDocumentService projectDocumentService;

    @Mock
    private MediaAssetService mediaAssetService;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldDeleteArchivedProjectPermanently() {
        UUID id = UUID.randomUUID();
        Project project = project(id, ProjectStatus.ARCHIVED);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        projectService.deletePermanently(id);

        verify(projectRepository).delete(project);
    }

    @Test
    void shouldRejectPermanentDeleteForPublishedProject() {
        UUID id = UUID.randomUUID();
        Project project = project(id, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.deletePermanently(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Solo se pueden eliminar definitivamente proyectos archivados");

        verify(projectRepository, never()).delete(project);
    }

    @Test
    void shouldRejectPermanentDeleteForDraftProject() {
        UUID id = UUID.randomUUID();
        Project project = project(id, ProjectStatus.DRAFT);
        when(projectRepository.findById(id)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.deletePermanently(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Solo se pueden eliminar definitivamente proyectos archivados");

        verify(projectRepository, never()).delete(project);
    }

    private Project project(UUID id, ProjectStatus status) {
        Project project = new Project();
        project.setId(id);
        project.setTitle("Proyecto");
        project.setSummary("Resumen");
        project.setContent("Contenido");
        project.setSlug("proyecto");
        project.setStatus(status);
        return project;
    }
}