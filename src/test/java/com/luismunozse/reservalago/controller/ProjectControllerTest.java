package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.GenerateProjectRequest;
import com.luismunozse.reservalago.dto.GeneratedProjectDraft;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.MediaAssetService;
import com.luismunozse.reservalago.service.ProjectAdvanceAiService;
import com.luismunozse.reservalago.service.ProjectAdvanceService;
import com.luismunozse.reservalago.service.ProjectAiService;
import com.luismunozse.reservalago.service.ProjectDocumentService;
import com.luismunozse.reservalago.service.ProjectImageService;
import com.luismunozse.reservalago.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(TestSecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

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

    @Test
    void shouldServeVideoWithoutRangeAsFullResponse() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] bytes = videoBytes();
        when(mediaAssetService.loadForRequest(id)).thenReturn(servedVideo(bytes));

        mockMvc.perform(get("/api/media/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/mp4"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, bytes.length))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void shouldServeVideoRangeFromBeginning() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] bytes = videoBytes();
        when(mediaAssetService.loadForRequest(id)).thenReturn(servedVideo(bytes));

        mockMvc.perform(get("/api/media/{id}", id).header(HttpHeaders.RANGE, "bytes=0-1023"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/4096"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 1024))
                .andExpect(content().bytes(slice(bytes, 0, 1024)));
    }

    @Test
    void shouldServeVideoRangeFromOffset() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] bytes = videoBytes();
        when(mediaAssetService.loadForRequest(id)).thenReturn(servedVideo(bytes));

        mockMvc.perform(get("/api/media/{id}", id).header(HttpHeaders.RANGE, "bytes=1024-2047"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 1024-2047/4096"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 1024))
                .andExpect(content().bytes(slice(bytes, 1024, 2048)));
    }

    @Test
    void shouldAcceptProjectAiHighlightsUpToFiftyThousandCharacters() throws Exception {
        when(projectAiService.generate(any(GenerateProjectRequest.class)))
                .thenReturn(new GeneratedProjectDraft("Titulo", "Resumen", "Contenido", "titulo", null));

        GenerateProjectRequest request = new GenerateProjectRequest(
                "Material fuente",
                "Objetivo",
                "Publico objetivo",
                "a".repeat(50_000),
                null
        );

        mockMvc.perform(post("/api/admin/projects/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProjectAiHighlightsAboveFiftyThousandCharacters() throws Exception {
        GenerateProjectRequest request = new GenerateProjectRequest(
                "Material fuente",
                "Objetivo",
                "Publico objetivo",
                "a".repeat(50_001),
                null
        );

        mockMvc.perform(post("/api/admin/projects/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private MediaAssetService.ServedMedia servedVideo(byte[] bytes) throws Exception {
        Path video = tempDir.resolve("video.mp4");
        Files.write(video, bytes);
        return new MediaAssetService.ServedMedia(new UrlResource(video.toUri()), "video/mp4", bytes.length, "video.mp4");
    }

    private byte[] videoBytes() {
        byte[] bytes = new byte[4096];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (index % 251);
        }
        return bytes;
    }

    private byte[] slice(byte[] bytes, int startInclusive, int endExclusive) {
        return java.util.Arrays.copyOfRange(bytes, startInclusive, endExclusive);
    }
}
