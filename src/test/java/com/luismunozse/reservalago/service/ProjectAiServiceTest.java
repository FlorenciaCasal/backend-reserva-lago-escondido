package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.GeneratedProjectDraft;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectAiServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProjectAiService service = new ProjectAiService(objectMapper);

    @Test
    void shouldKeepSummaryBelowSixHundredCharacters() throws Exception {
        String summary = "Resumen breve del proyecto, pensado para tarjetas y listados publicos.";

        GeneratedProjectDraft draft = parseDraft(summary, "Contenido principal del proyecto.");

        assertThat(draft.summary()).isEqualTo(summary);
    }

    @Test
    void shouldKeepSummaryWithExactlySixHundredCharacters() throws Exception {
        String summary = "a".repeat(600);

        GeneratedProjectDraft draft = parseDraft(summary, "Contenido principal del proyecto.");

        assertThat(draft.summary()).isEqualTo(summary);
        assertThat(draft.summary()).hasSize(600);
    }

    @Test
    void shouldReduceSummaryAboveSixHundredCharactersWithoutCuttingSentence() throws Exception {
        String firstSentence = "El proyecto busca fortalecer el conocimiento sobre los ambientes de la Reserva mediante investigacion aplicada y monitoreo continuo.";
        String secondSentence = "Tambien promueve acciones de conservacion, educacion ambiental y participacion responsable con equipos tecnicos y comunidad local.";
        String thirdSentence = "Esta frase agrega detalles operativos, antecedentes, alcances, actividades complementarias, objetivos secundarios, explicaciones extensas, referencias territoriales, consideraciones institucionales, posibles acciones educativas, lineas de trabajo complementarias, criterios tecnicos y matices narrativos que llevarian el resumen por encima del limite definido para tarjetas y listados publicos del sitio institucional.";
        String summary = firstSentence + " " + secondSentence + " " + thirdSentence + " " + thirdSentence + " " + thirdSentence;

        GeneratedProjectDraft draft = parseDraft(summary, "Contenido principal del proyecto.");

        assertThat(draft.summary()).hasSizeLessThanOrEqualTo(600);
        assertThat(draft.summary()).isEqualTo(firstSentence + " " + secondSentence);
        assertThat(draft.summary()).endsWith(".");
    }

    @Test
    void shouldNotModifyMainContentWhenSummaryIsReduced() throws Exception {
        String summary = "Primera frase valida para el resumen del proyecto. " +
                "Segunda frase valida para mantener el sentido general. " +
                "Tercera frase demasiado extensa ".repeat(40);
        String content = "Contenido principal con desarrollo narrativo, contexto, acciones e impacto esperado. " +
                "Este campo no debe reducirse ni truncarse aunque el resumen generado sea demasiado largo.";

        GeneratedProjectDraft draft = parseDraft(summary, content);

        assertThat(draft.summary()).hasSizeLessThanOrEqualTo(600);
        assertThat(draft.content()).isEqualTo(content);
    }

    @SuppressWarnings("unchecked")
    private GeneratedProjectDraft parseDraft(String summary, String content) throws Exception {
        String outputText = objectMapper.writeValueAsString(Map.of(
                "title", "Proyecto de prueba",
                "summary", summary,
                "content", content,
                "slug", "proyecto-de-prueba"
        ));
        String body = objectMapper.writeValueAsString(Map.of("output_text", outputText));

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);

        return ReflectionTestUtils.invokeMethod(service, "parseResponse", response, null);
    }
}
