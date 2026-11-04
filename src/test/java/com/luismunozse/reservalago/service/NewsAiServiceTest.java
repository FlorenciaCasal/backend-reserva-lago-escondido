package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.GeneratedNewsDraft;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.repo.NewsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsAiServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NewsAiService service = new NewsAiService(objectMapper, mock(NewsRepository.class));

    @Test
    void shouldKeepSummaryBelowSixHundredCharacters() throws Exception {
        String summary = "Resumen breve de la novedad, pensado para tarjetas y listados publicos.";

        GeneratedNewsDraft draft = parseDraft(summary, "Contenido principal de la novedad.");

        assertThat(draft.summary()).isEqualTo(summary);
    }

    @Test
    void shouldKeepSummaryWithExactlySixHundredCharacters() throws Exception {
        String summary = "a".repeat(600);

        GeneratedNewsDraft draft = parseDraft(summary, "Contenido principal de la novedad.");

        assertThat(draft.summary()).isEqualTo(summary);
        assertThat(draft.summary()).hasSize(600);
    }

    @Test
    void shouldReduceSummaryAboveSixHundredCharactersWithoutCuttingSentence() throws Exception {
        String firstSentence = "La novedad comunica una actividad relevante para la Reserva mediante un enfoque institucional, claro y cercano.";
        String secondSentence = "Tambien resume el sentido ambiental, educativo y comunitario de la informacion para listados y tarjetas publicas.";
        String thirdSentence = "Esta frase agrega detalles operativos, antecedentes, alcances, actividades complementarias, objetivos secundarios, explicaciones extensas, referencias territoriales, consideraciones institucionales, posibles acciones educativas, lineas de trabajo complementarias, criterios tecnicos y matices narrativos que llevarian el resumen por encima del limite definido para tarjetas y listados publicos del sitio institucional.";
        String summary = firstSentence + " " + secondSentence + " " + thirdSentence + " " + thirdSentence + " " + thirdSentence;

        GeneratedNewsDraft draft = parseDraft(summary, "Contenido principal de la novedad.");

        assertThat(draft.summary()).hasSizeLessThanOrEqualTo(600);
        assertThat(draft.summary()).isEqualTo(firstSentence + " " + secondSentence);
        assertThat(draft.summary()).endsWith(".");
    }

    @Test
    void shouldNotModifyMainContentWhenSummaryIsReduced() throws Exception {
        String summary = "Primera frase valida para el resumen de la novedad. " +
                "Segunda frase valida para mantener el sentido general. " +
                "Tercera frase demasiado extensa ".repeat(40);
        String content = "Contenido principal con desarrollo narrativo, contexto, informacion y cierre editorial. " +
                "Este campo no debe reducirse ni truncarse aunque el resumen generado sea demasiado largo.";

        GeneratedNewsDraft draft = parseDraft(summary, content);

        assertThat(draft.summary()).hasSizeLessThanOrEqualTo(600);
        assertThat(draft.content()).isEqualTo(content);
    }

    @Test
    void shouldNormalizeInstagramHashtagsGeneratedWithoutHash() throws Exception {
        NewsSocialContentResponse response = parseSocialResponse(
                SocialPlatform.INSTAGRAM,
                "ReservaNaturalLagoEscondido Biodiversidad EducacionAmbiental"
        );

        assertThat(response.hashtags())
                .isEqualTo("#ReservaNaturalLagoEscondido #Biodiversidad #EducacionAmbiental");
    }

    @Test
    void shouldNotNormalizeFacebookHashtags() throws Exception {
        NewsSocialContentResponse response = parseSocialResponse(
                SocialPlatform.FACEBOOK,
                "ReservaNaturalLagoEscondido Biodiversidad EducacionAmbiental"
        );

        assertThat(response.hashtags())
                .isEqualTo("ReservaNaturalLagoEscondido Biodiversidad EducacionAmbiental");
    }

    private NewsSocialContentResponse parseSocialResponse(SocialPlatform platform, String hashtags) throws Exception {
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "output_text": "{\\"caption\\":\\"Caption\\",\\"body\\":\\"Texto Facebook\\",\\"hashtags\\":\\"%s\\",\\"callToAction\\":\\"CTA\\",\\"altText\\":\\"Alt text\\"}"
                }
                """.formatted(hashtags));

        News news = new News();
        news.setId(UUID.randomUUID());
        news.setTitle("Novedad");

        Method method = NewsAiService.class.getDeclaredMethod(
                "parseSocialResponse",
                HttpResponse.class,
                News.class,
                SocialPlatform.class
        );
        method.setAccessible(true);
        return (NewsSocialContentResponse) method.invoke(service, httpResponse, news, platform);
    }

    private GeneratedNewsDraft parseDraft(String summary, String content) throws Exception {
        String outputText = objectMapper.writeValueAsString(Map.of(
                "title", "Novedad de prueba",
                "summary", summary,
                "content", content,
                "slug", "novedad-de-prueba"
        ));
        String body = objectMapper.writeValueAsString(Map.of("output_text", outputText));

        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);

        return ReflectionTestUtils.invokeMethod(service, "parseNewsResponse", response, null);
    }
}
