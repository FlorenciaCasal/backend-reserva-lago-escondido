package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.HomeContentRequest;
import com.luismunozse.reservalago.dto.HomeContentResponse;
import com.luismunozse.reservalago.dto.HomePillarContentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeContentServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Test
    void shouldReturnConfiguredContent() {
        HomeContentService service = new HomeContentService(systemConfigService);
        stubConfiguredContent();

        HomeContentResponse response = service.getContent();

        assertThat(response.heroTitle()).isEqualTo("Conservar, habitar y producir de manera sostenible");
        assertThat(response.heroImageUrl()).isEqualTo("/img/home.jpg");
        assertThat(response.introText()).isEqualTo("Trabajamos para conservar los ecosistemas, desarrollar actividades productivas responsables y promover una forma sostenible de habitar el territorio.");
        assertThat(response.conservar().value()).isEqualTo("42");
        assertThat(response.conservar().suffix()).isEqualTo("%");
        assertThat(response.visitsImageUrl()).isEqualTo("/img/particular.jpg");
    }

    @Test
    void shouldTrimAndSaveContent() {
        HomeContentService service = new HomeContentService(systemConfigService);
        stubConfiguredContent();

        service.updateContent(request());

        verify(systemConfigService).setValue("home_hero_title", "Hero");
        verify(systemConfigService).setValue("home_hero_image_url", "/img/home.jpg");
        verify(systemConfigService).setValue("home_intro_text", "Intro");
        verify(systemConfigService).setValue("home_conservar_title", "Conservar");
        verify(systemConfigService).setValue("home_conservar_value", "42");
        verify(systemConfigService).setValue("home_conservar_suffix", "%");
        verify(systemConfigService).setValue("home_projects_title", "Proyectos destacados");
        verify(systemConfigService).setValue("home_news_cta_label", "Ver todas las novedades");
        verify(systemConfigService).setValue("home_visits_image_url", "/img/particular.jpg");
    }

    private void stubConfiguredContent() {
        when(systemConfigService.getValue("home_hero_title", "Conservar, habitar y producir de manera sostenible"))
                .thenReturn("Conservar, habitar y producir de manera sostenible");
        when(systemConfigService.getValue("home_hero_subtitle", "Área natural privada, Paraje El Foyel, Río Negro."))
                .thenReturn("Área natural privada, Paraje El Foyel, Río Negro.");
        when(systemConfigService.getValue("home_hero_image_url", "/img/home.jpg")).thenReturn("/img/home.jpg");
        when(systemConfigService.getValue("home_intro_text", "Trabajamos para conservar los ecosistemas, desarrollar actividades productivas responsables y promover una forma sostenible de habitar el territorio."))
                .thenReturn("Trabajamos para conservar los ecosistemas, desarrollar actividades productivas responsables y promover una forma sostenible de habitar el territorio.");
        when(systemConfigService.getValue("home_action_title", "Nuestras líneas de acción"))
                .thenReturn("Nuestras líneas de acción");
        stubPillar("conservar", "Conservar", "42", "%", "del territorio destinado a conservación", "Protegemos áreas de alto valor natural a través de la investigación, el monitoreo y la educación ambiental.", "CONOCÉ MÁS");
        stubPillar("habitar", "Habitar", "200", "", "personas habitan la reserva", "Promovemos una forma responsable de habitar el territorio, en convivencia con el entorno natural.", "CONOCÉ MÁS");
        stubPillar("producir", "Producir", "58", "%", "del territorio destinado a producción sostenible", "Desarrollamos actividades productivas responsables, compatibles con la conservación y el cuidado del territorio.", "CONOCÉ MÁS");
        when(systemConfigService.getValue("home_projects_title", "Proyectos destacados")).thenReturn("Proyectos destacados");
        when(systemConfigService.getValue("home_projects_cta_label", "Ver todos los proyectos")).thenReturn("Ver todos los proyectos");
        when(systemConfigService.getValue("home_news_title", "Novedades")).thenReturn("Novedades");
        when(systemConfigService.getValue("home_news_cta_label", "Ver todas las novedades")).thenReturn("Ver todas las novedades");
        when(systemConfigService.getValue("home_visits_eyebrow", "Visitas")).thenReturn("Visitas");
        when(systemConfigService.getValue("home_visits_title", "Vivi la reserva y conoce su entorno natural")).thenReturn("Vivi la reserva y conoce su entorno natural");
        when(systemConfigService.getValue("home_visits_text", "Organizamos visitas para acercar la experiencia de la Reserva Natural Lago Escondido a quienes desean conocer, aprender y disfrutar este territorio con responsabilidad."))
                .thenReturn("Organizamos visitas para acercar la experiencia de la Reserva Natural Lago Escondido a quienes desean conocer, aprender y disfrutar este territorio con responsabilidad.");
        when(systemConfigService.getValue("home_visits_cta_label", "Reservar visita")).thenReturn("Reservar visita");
        when(systemConfigService.getValue("home_visits_image_url", "/img/particular.jpg")).thenReturn("/img/particular.jpg");
    }

    private void stubPillar(String prefix, String title, String value, String suffix, String statLabel, String text, String ctaLabel) {
        when(systemConfigService.getValue("home_" + prefix + "_title", title)).thenReturn(title);
        when(systemConfigService.getValue("home_" + prefix + "_value", value)).thenReturn(value);
        when(systemConfigService.getValue("home_" + prefix + "_suffix", suffix)).thenReturn(suffix);
        when(systemConfigService.getValue("home_" + prefix + "_stat_label", statLabel)).thenReturn(statLabel);
        when(systemConfigService.getValue("home_" + prefix + "_text", text)).thenReturn(text);
        when(systemConfigService.getValue("home_" + prefix + "_cta_label", ctaLabel)).thenReturn(ctaLabel);
    }

    private HomeContentRequest request() {
        return new HomeContentRequest(
                " Hero ",
                " Subtitulo ",
                " /img/home.jpg ",
                " Intro ",
                " Lineas ",
                pillar("Conservar", "42", "%"),
                pillar("Habitar", "200", ""),
                pillar("Producir", "58", "%"),
                " Proyectos destacados ",
                " Ver todos los proyectos ",
                " Novedades ",
                " Ver todas las novedades ",
                " Visitas ",
                " Vivi la reserva y conoce su entorno natural ",
                " Texto visitas ",
                " Reservar visita ",
                " /img/particular.jpg "
        );
    }

    private HomePillarContentRequest pillar(String title, String value, String suffix) {
        return new HomePillarContentRequest(
                " " + title + " ",
                " " + value + " ",
                " " + suffix + " ",
                " Label ",
                " Texto ",
                " CONOCÉ MÁS "
        );
    }
}
