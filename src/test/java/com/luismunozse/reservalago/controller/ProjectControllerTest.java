package com.luismunozse.reservalago.controller;

import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.MediaAssetService;
import com.luismunozse.reservalago.service.ProjectAdvanceAiService;
import com.luismunozse.reservalago.service.ProjectAdvanceService;
import com.luismunozse.reservalago.service.ProjectAiService;
import com.luismunozse.reservalago.service.ProjectDocumentService;
import com.luismunozse.reservalago.service.ProjectImageService;
import com.luismunozse.reservalago.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(TestSecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ProjectAiService projectAiService;

    @MockitoBean
    private ProjectAdvanceAiService projectAdvanceAiService;

    @MockitoBean
    private ProjectAdvanceService projectAdvanceService;

    @MockitoBean
    private ProjectImageService projectImageService;

    @MockitoBean
    private ProjectDocumentService projectDocumentService;

    @MockitoBean
    private MediaAssetService mediaAssetService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldDeleteArchivedProjectPermanently() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/projects/{id}", id))
                .andExpect(status().isNoContent());

        verify(projectService).deletePermanently(id);
    }
}