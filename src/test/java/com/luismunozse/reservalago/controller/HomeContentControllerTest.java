package com.luismunozse.reservalago.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.HomeContentRequest;
import com.luismunozse.reservalago.dto.HomeContentResponse;
import com.luismunozse.reservalago.dto.HomePillarContentRequest;
import com.luismunozse.reservalago.dto.HomePillarContentResponse;
import com.luismunozse.reservalago.service.HomeContentService;
import com.luismunozse.reservalago.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeContentController.class)
@Import(TestSecurityConfig.class)
class HomeContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HomeContentService homeContentService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturnPublicHomeContent() throws Exception {
        when(homeContentService.getContent()).thenReturn(response());

        mockMvc.perform(get("/api/home/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroTitle").value("Conservar, habitar y producir de manera sostenible"))
                .andExpect(jsonPath("$.conservar.value").value("42"));
    }

    @Test
    void shouldReturnAdminHomeContent() throws Exception {
        when(homeContentService.getContent()).thenReturn(response());

        mockMvc.perform(get("/api/admin/home/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitsCtaLabel").value("Reservar visita"));
    }

    @Test
    void shouldUpdateAdminHomeContent() throws Exception {
        HomeContentRequest request = request();
        when(homeContentService.updateContent(any())).thenReturn(response());

        mockMvc.perform(put("/api/admin/home/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectsTitle").value("Proyectos destacados"));
    }

    private HomeContentResponse response() {
        return new HomeContentResponse(
                "Conservar, habitar y producir de manera sostenible",
                "Área natural privada, Paraje El Foyel, Río Negro.",
                "/img/home.jpg",
                "Trabajamos para conservar los ecosistemas, desarrollar actividades productivas responsables y promover una forma sostenible de habitar el territorio.",
                "Nuestras líneas de acción",
                pillarResponse("Conservar", "42", "%"),
                pillarResponse("Habitar", "200", ""),
                pillarResponse("Producir", "58", "%"),
                "Proyectos destacados",
                "Ver todos los proyectos",
                "Novedades",
                "Ver todas las novedades",
                "Visitas",
                "Vivi la reserva y conoce su entorno natural",
                "Organizamos visitas para acercar la experiencia de la Reserva Natural Lago Escondido a quienes desean conocer, aprender y disfrutar este territorio con responsabilidad.",
                "Reservar visita",
                "/img/particular.jpg"
        );
    }

    private HomePillarContentResponse pillarResponse(String title, String value, String suffix) {
        return new HomePillarContentResponse(
                title,
                value,
                suffix,
                "Label",
                "Texto",
                "CONOCÉ MÁS"
        );
    }

    private HomeContentRequest request() {
        return new HomeContentRequest(
                "Conservar, habitar y producir de manera sostenible",
                "Área natural privada, Paraje El Foyel, Río Negro.",
                "/img/home.jpg",
                "Trabajamos para conservar los ecosistemas, desarrollar actividades productivas responsables y promover una forma sostenible de habitar el territorio.",
                "Nuestras líneas de acción",
                pillarRequest("Conservar", "42", "%"),
                pillarRequest("Habitar", "200", ""),
                pillarRequest("Producir", "58", "%"),
                "Proyectos destacados",
                "Ver todos los proyectos",
                "Novedades",
                "Ver todas las novedades",
                "Visitas",
                "Vivi la reserva y conoce su entorno natural",
                "Organizamos visitas para acercar la experiencia de la Reserva Natural Lago Escondido a quienes desean conocer, aprender y disfrutar este territorio con responsabilidad.",
                "Reservar visita",
                "/img/particular.jpg"
        );
    }

    private HomePillarContentRequest pillarRequest(String title, String value, String suffix) {
        return new HomePillarContentRequest(
                title,
                value,
                suffix,
                "Label",
                "Texto",
                "CONOCÉ MÁS"
        );
    }
}
