package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.PreserveContentRequest;
import com.luismunozse.reservalago.dto.PreserveContentResponse;
import com.luismunozse.reservalago.service.JwtService;
import com.luismunozse.reservalago.service.PreserveContentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreserveContentController.class)
@Import(TestSecurityConfig.class)
class PreserveContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PreserveContentService preserveContentService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnPublicPreserveContent() throws Exception {
        when(preserveContentService.getContent()).thenReturn(response());

        mockMvc.perform(get("/api/preservar/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatWeDoText").value("Texto"))
                .andExpect(jsonPath("$.bullets[0]").value("Uno"));
    }

    @Test
    void shouldReturnAdminPreserveContent() throws Exception {
        when(preserveContentService.getContent()).thenReturn(response());

        mockMvc.perform(get("/api/admin/preservar/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bullets[1]").value("Dos"));
    }

    @Test
    void shouldUpdateAdminPreserveContent() throws Exception {
        PreserveContentRequest request = new PreserveContentRequest("Nueva bajada", "Nuevo texto", List.of("Uno"));
        when(preserveContentService.updateContent(any())).thenReturn(new PreserveContentResponse("Nueva bajada", "Nuevo texto", List.of("Uno")));

        mockMvc.perform(put("/api/admin/preservar/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatWeDoText").value("Nuevo texto"));
    }

    private PreserveContentResponse response() {
        return new PreserveContentResponse("Bajada", "Texto", List.of("Uno", "Dos"));
    }
}
