package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.ProjectAdvanceResponse;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectAdvance;
import com.luismunozse.reservalago.model.ProjectStatus;
import com.luismunozse.reservalago.repo.ProjectAdvanceRepository;
import com.luismunozse.reservalago.repo.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAdvanceServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectAdvanceRepository projectAdvanceRepository;

    @Mock
    private MediaAssetService mediaAssetService;

    @InjectMocks
    private ProjectAdvanceService projectAdvanceService;

    @Test
    void shouldListPublicAdvancesInChronologicalOrder() {
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId, "restauracion-alerce-lago-escondido", ProjectStatus.PUBLISHED);
        ProjectAdvance may20 = advance(project, "Mayo", LocalDate.of(2026, 5, 20), "2026-09-21T12:00:00Z");
        ProjectAdvance june6 = advance(project, "Junio 6", LocalDate.of(2026, 6, 6), "2026-09-21T19:56:00Z");
        ProjectAdvance june8 = advance(project, "Junio 8", LocalDate.of(2026, 6, 8), "2026-09-21T19:54:00Z");

        when(projectRepository.findBySlugAndStatus(project.getSlug(), ProjectStatus.PUBLISHED)).thenReturn(Optional.of(project));
        when(projectAdvanceRepository.findByProjectIdOrderByAdvanceDateAscCreatedAtAscIdAsc(projectId))
                .thenReturn(List.of(may20, june6, june8));

        List<ProjectAdvanceResponse> responses = projectAdvanceService.listPublicByProjectSlug(project.getSlug());

        assertThat(responses).extracting(ProjectAdvanceResponse::title)
                .containsExactly("Mayo", "Junio 6", "Junio 8");
    }

    @Test
    void shouldUseCreatedAtAsTieBreakerForSameAdvanceDate() {
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId, "restauracion-alerce-lago-escondido", ProjectStatus.PUBLISHED);
        ProjectAdvance firstCreated = advance(project, "Primero creado", LocalDate.of(2026, 6, 6), "2026-09-21T12:00:00Z");
        ProjectAdvance secondCreated = advance(project, "Segundo creado", LocalDate.of(2026, 6, 6), "2026-09-21T13:00:00Z");

        when(projectRepository.findBySlugAndStatus(project.getSlug(), ProjectStatus.PUBLISHED)).thenReturn(Optional.of(project));
        when(projectAdvanceRepository.findByProjectIdOrderByAdvanceDateAscCreatedAtAscIdAsc(projectId))
                .thenReturn(List.of(firstCreated, secondCreated));

        List<ProjectAdvanceResponse> responses = projectAdvanceService.listPublicByProjectSlug(project.getSlug());

        assertThat(responses).extracting(ProjectAdvanceResponse::title)
                .containsExactly("Primero creado", "Segundo creado");
    }

    private Project project(UUID id, String slug, ProjectStatus status) {
        Project project = new Project();
        project.setId(id);
        project.setTitle("Proyecto");
        project.setSummary("Resumen");
        project.setContent("Contenido");
        project.setSlug(slug);
        project.setStatus(status);
        return project;
    }

    private ProjectAdvance advance(Project project, String title, LocalDate advanceDate, String createdAt) {
        ProjectAdvance advance = new ProjectAdvance();
        advance.setId(UUID.randomUUID());
        advance.setProject(project);
        advance.setAdvanceDate(advanceDate);
        advance.setTitle(title);
        advance.setDescription("Descripcion");
        advance.setCreatedAt(Instant.parse(createdAt));
        advance.setUpdatedAt(Instant.parse(createdAt));
        return advance;
    }
}
