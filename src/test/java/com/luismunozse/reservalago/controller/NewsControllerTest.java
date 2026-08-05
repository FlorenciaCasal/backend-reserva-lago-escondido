package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.CreateNewsRequest;
import com.luismunozse.reservalago.dto.NewsResponse;
import com.luismunozse.reservalago.model.NewsStatus;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.NewsImageService;
import com.luismunozse.reservalago.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
@Import(TestSecurityConfig.class)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NewsService newsService;

    @MockitoBean
    private NewsImageService newsImageService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnPublishedNews() throws Exception {
        when(newsService.listPublished()).thenReturn(List.of(response(NewsStatus.PUBLISHED)));

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    void shouldReturnPublishedNewsBySlug() throws Exception {
        when(newsService.getPublishedBySlug("novedad")).thenReturn(response(NewsStatus.PUBLISHED));

        mockMvc.perform(get("/api/news/novedad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("novedad"));
    }

    @Test
    void shouldCreateAdminNews() throws Exception {
        CreateNewsRequest request = new CreateNewsRequest(
                "Novedad",
                "Resumen",
                "Contenido",
                "novedad",
                null,
                null,
                null,
                null,
                NewsStatus.DRAFT
        );
        when(newsService.create(any())).thenReturn(response(NewsStatus.DRAFT));

        mockMvc.perform(post("/api/admin/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldPublishAdminNews() throws Exception {
        UUID id = UUID.randomUUID();
        when(newsService.publish(eq(id))).thenReturn(response(NewsStatus.PUBLISHED));

        mockMvc.perform(post("/api/admin/news/{id}/publish", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    private NewsResponse response(NewsStatus status) {
        return new NewsResponse(
                UUID.randomUUID(),
                "Novedad",
                "Resumen",
                "Contenido",
                "novedad",
                null,
                "/img/novedad.jpg",
                null,
                null,
                status,
                status == NewsStatus.PUBLISHED ? Instant.now() : null,
                status == NewsStatus.ARCHIVED ? Instant.now() : null,
                Instant.now(),
                Instant.now(),
                List.of()
        );
    }
}
