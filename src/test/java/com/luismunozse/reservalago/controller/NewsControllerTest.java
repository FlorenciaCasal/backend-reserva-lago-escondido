package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.CreateNewsRequest;
import com.luismunozse.reservalago.dto.GenerateNewsRequest;
import com.luismunozse.reservalago.dto.GenerateNewsSocialContentRequest;
import com.luismunozse.reservalago.dto.GeneratedNewsDraft;
import com.luismunozse.reservalago.dto.NewsResponse;
import com.luismunozse.reservalago.dto.NewsSocialContentRequest;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.model.NewsStatus;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.NewsAiService;
import com.luismunozse.reservalago.service.NewsImageService;
import com.luismunozse.reservalago.service.NewsService;
import com.luismunozse.reservalago.service.NewsSocialContentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private NewsSocialContentService newsSocialContentService;

    @MockitoBean
    private NewsAiService newsAiService;

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

    @Test
    void shouldGenerateNewsDraft() throws Exception {
        GenerateNewsRequest request = new GenerateNewsRequest(
                "Brief",
                "Informar",
                "Publico general",
                "Aspectos",
                null
        );
        when(newsAiService.generate(any())).thenReturn(new GeneratedNewsDraft(
                "Titulo IA",
                "Resumen IA",
                "Contenido IA",
                "titulo-ia",
                null
        ));

        mockMvc.perform(post("/api/admin/news/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("titulo-ia"));
    }

    @Test
    void shouldListSocialContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(newsSocialContentService.listByNews(eq(id))).thenReturn(List.of(socialResponse(id, SocialPlatform.INSTAGRAM)));

        mockMvc.perform(get("/api/admin/news/{id}/social", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platform").value("INSTAGRAM"));
    }

    @Test
    void shouldSaveSocialContent() throws Exception {
        UUID id = UUID.randomUUID();
        NewsSocialContentRequest request = new NewsSocialContentRequest(
                "Caption",
                null,
                "#Reserva",
                "Leer mas",
                "Imagen de la reserva"
        );
        when(newsSocialContentService.save(eq(id), eq(SocialPlatform.INSTAGRAM), any()))
                .thenReturn(socialResponse(id, SocialPlatform.INSTAGRAM));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/news/{id}/social/{platform}", id, "INSTAGRAM")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("Caption"));
    }

    @Test
    void shouldGenerateSocialContentWithoutPersisting() throws Exception {
        UUID id = UUID.randomUUID();
        GenerateNewsSocialContentRequest request = new GenerateNewsSocialContentRequest("Mas cercano");
        when(newsAiService.generateSocialContent(eq(id), eq(SocialPlatform.FACEBOOK), any()))
                .thenReturn(socialResponse(id, SocialPlatform.FACEBOOK));

        mockMvc.perform(post("/api/admin/news/{id}/social/{platform}/generate", id, "FACEBOOK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platform").value("FACEBOOK"));
    }


    @Test
    void shouldDeleteArchivedNewsPermanently() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/news/{id}", id))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(newsService).deletePermanently(id);
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

    private NewsSocialContentResponse socialResponse(UUID newsId, SocialPlatform platform) {
        return new NewsSocialContentResponse(
                UUID.randomUUID(),
                newsId,
                platform,
                platform == SocialPlatform.INSTAGRAM ? "Caption" : null,
                platform == SocialPlatform.FACEBOOK ? "Texto Facebook" : null,
                "#Reserva",
                "Leer mas",
                "Imagen de la reserva",
                Instant.now(),
                Instant.now()
        );
    }
}
