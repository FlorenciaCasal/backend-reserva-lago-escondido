package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.PreserveContentRequest;
import com.luismunozse.reservalago.dto.PreserveContentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreserveContentServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturnConfiguredContent() {
        PreserveContentService service = new PreserveContentService(systemConfigService, objectMapper);
        stubConfiguredContent("[\"Uno\",\"Dos\"]");

        PreserveContentResponse response = service.getContent();

        assertThat(response.intro()).isEqualTo("Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.");
        assertThat(response.heroAsideLines()).containsExactly("Naturaleza", "hoy,", "mañana,", "siempre");
        assertThat(response.whatEyebrow()).isEqualTo("Nuestra tarea");
        assertThat(response.whatWeDoText()).isEqualTo("Texto configurado");
        assertThat(response.bullets()).containsExactly("Uno", "Dos");
        assertThat(response.territoryMetricValue()).isEqualTo("42%");
    }

    @Test
    void shouldFallbackToDefaultBulletsWhenJsonIsInvalid() {
        PreserveContentService service = new PreserveContentService(systemConfigService, objectMapper);
        stubConfiguredContent("invalid-json");

        PreserveContentResponse response = service.getContent();

        assertThat(response.bullets()).contains("Conservación de especies nativas");
    }

    @Test
    void shouldTrimAndSaveContent() throws Exception {
        PreserveContentService service = new PreserveContentService(systemConfigService, objectMapper);

        PreserveContentResponse response = service.updateContent(request(" Intro ", " Texto ", List.of(" Uno ", "", " Dos ")));

        assertThat(response.intro()).isEqualTo("Intro");
        assertThat(response.heroAsideLines()).containsExactly("Naturaleza", "hoy");
        assertThat(response.whatEyebrow()).isEqualTo("Eyebrow");
        assertThat(response.whatWeDoText()).isEqualTo("Texto");
        assertThat(response.bullets()).containsExactly("Uno", "Dos");
        verify(systemConfigService).setValue("preservar_intro", "Intro");
        verify(systemConfigService).setValue("preservar_hero_aside_lines", "[\"Naturaleza\",\"hoy\"]");
        verify(systemConfigService).setValue("preservar_what_eyebrow", "Eyebrow");
        verify(systemConfigService).setValue("preservar_what_we_do_text", "Texto");
        verify(systemConfigService).setValue("preservar_what_we_do_bullets", "[\"Uno\",\"Dos\"]");
        verify(systemConfigService).setValue("preservar_territory_metric_value", "42%");
    }

    private String defaultText() {
        return "Trabajamos en la protección de especies nativas, la restauración de ambientes y la investigación científica para comprender y cuidar nuestro entorno. Nuestro compromiso es integral y se basa en pilares técnicos y educativos.";
    }

    private void stubConfiguredContent(String bulletsValue) {
        when(systemConfigService.getValue("preservar_intro", "Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras."))
                .thenReturn("Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.");
        when(systemConfigService.getValue("preservar_hero_aside_lines", "[]"))
                .thenReturn("[\"Naturaleza\",\"hoy,\",\"mañana,\",\"siempre\"]");
        when(systemConfigService.getValue("preservar_what_eyebrow", "Nuestra tarea"))
                .thenReturn("Nuestra tarea");
        when(systemConfigService.getValue("preservar_what_we_do_text", defaultText()))
                .thenReturn("Texto configurado");
        when(systemConfigService.getValue("preservar_what_we_do_bullets", "[]"))
                .thenReturn(bulletsValue);
        when(systemConfigService.getValue("preservar_territory_eyebrow", "Un territorio con sentido"))
                .thenReturn("Un territorio con sentido");
        when(systemConfigService.getValue("preservar_territory_title", "Un territorio destinado a la conservación"))
                .thenReturn("Un territorio destinado a la conservación");
        when(systemConfigService.getValue("preservar_territory_text", "El 42% de la Reserva Natural Lago Escondido está destinado a la protección de ambientes naturales, sus especies y los procesos ecológicos que los sostienen. Conservar es garantizar que este patrimonio natural perdure en el tiempo."))
                .thenReturn("Texto territorio");
        when(systemConfigService.getValue("preservar_territory_metric_value", "42%"))
                .thenReturn("42%");
        when(systemConfigService.getValue("preservar_territory_metric_description", "Del territorio destinado a conservación"))
                .thenReturn("Del territorio destinado a conservación");
        when(systemConfigService.getValue("preservar_territory_research_title", "Investigación"))
                .thenReturn("Investigación");
        when(systemConfigService.getValue("preservar_territory_research_description", "Para conocer y monitorear los ecosistemas"))
                .thenReturn("Para conocer y monitorear los ecosistemas");
        when(systemConfigService.getValue("preservar_territory_education_title", "Educación ambiental"))
                .thenReturn("Educación ambiental");
        when(systemConfigService.getValue("preservar_territory_education_description", "Para acercar el conocimiento y promover su cuidado"))
                .thenReturn("Para acercar el conocimiento y promover su cuidado");
    }

    private PreserveContentRequest request(String intro, String whatWeDoText, List<String> bullets) {
        return new PreserveContentRequest(
                intro,
                List.of(" Naturaleza ", "", " hoy "),
                " Eyebrow ",
                whatWeDoText,
                bullets,
                " Un territorio con sentido ",
                " Un territorio destinado a la conservación ",
                " Texto territorio ",
                " 42% ",
                " Del territorio destinado a conservación ",
                " Investigación ",
                " Para conocer y monitorear los ecosistemas ",
                " Educación ambiental ",
                " Para acercar el conocimiento y promover su cuidado "
        );
    }
}
