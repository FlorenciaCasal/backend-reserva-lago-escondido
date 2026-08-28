package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.GenerateNewsRequest;
import com.luismunozse.reservalago.dto.GenerateNewsSocialContentRequest;
import com.luismunozse.reservalago.dto.GeneratedNewsDraft;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.repo.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NewsAiService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final ObjectMapper objectMapper;
    private final NewsRepository newsRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;

    @Value("${app.openai.model:${OPENAI_MODEL:gpt-4o-mini}}")
    private String model;

    public GeneratedNewsDraft generate(GenerateNewsRequest request) {
        String prompt = buildNewsPrompt(request);

        try {
            HttpResponse<String> response = callOpenAi(prompt, true, newsSchema(), "generated_news");
            try {
                return parseNewsResponse(response, request.imageUrl());
            } catch (Exception structuredError) {
                HttpResponse<String> fallbackResponse = callOpenAi(prompt, false, null, null);
                return parseNewsResponse(fallbackResponse, request.imageUrl());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "No se pudo generar la novedad con IA");
        }
    }

    public NewsSocialContentResponse generateSocialContent(
            UUID newsId,
            SocialPlatform platform,
            GenerateNewsSocialContentRequest request
    ) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Novedad no encontrada"));
        String prompt = buildSocialPrompt(news, platform, request);

        try {
            HttpResponse<String> response = callOpenAi(prompt, true, socialSchema(), "generated_news_social_content");
            try {
                return parseSocialResponse(response, news, platform);
            } catch (Exception structuredError) {
                HttpResponse<String> fallbackResponse = callOpenAi(prompt, false, null, null);
                return parseSocialResponse(fallbackResponse, news, platform);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "No se pudo generar el contenido social con IA");
        }
    }

    private HttpResponse<String> callOpenAi(
            String prompt,
            boolean structured,
            Map<String, Object> schema,
            String schemaName
    ) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Falta configurar OPENAI_API_KEY en el entorno del backend"
            );
        }

        Map<String, Object> body = structured
                ? Map.of(
                        "model", model == null || model.isBlank() ? DEFAULT_MODEL : model,
                        "input", prompt,
                        "text", Map.of(
                                "format", Map.of(
                                        "type", "json_schema",
                                        "name", schemaName,
                                        "strict", true,
                                        "schema", schema
                                )
                        )
                )
                : Map.of(
                        "model", model == null || model.isBlank() ? DEFAULT_MODEL : model,
                        "input", prompt
                );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/responses"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    }

    private GeneratedNewsDraft parseNewsResponse(HttpResponse<String> response, String imageUrl) throws Exception {
        JsonNode draft = parseOpenAiJson(response, "OpenAI no pudo generar la novedad");
        String title = draft.path("title").asText("").trim();
        String summary = draft.path("summary").asText("").trim();
        String content = draft.path("content").asText("").trim();
        String slug = draft.path("slug").asText("").trim();

        if (title.isBlank() || summary.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI devolvio una novedad incompleta");
        }

        return new GeneratedNewsDraft(
                title,
                summary,
                content,
                slug.isBlank() ? slugify(title) : slugify(slug),
                imageUrl
        );
    }

    private NewsSocialContentResponse parseSocialResponse(
            HttpResponse<String> response,
            News news,
            SocialPlatform platform
    ) throws Exception {
        JsonNode draft = parseOpenAiJson(response, "OpenAI no pudo generar el contenido social");
        String caption = draft.path("caption").asText("").trim();
        String body = draft.path("body").asText("").trim();
        String hashtags = draft.path("hashtags").asText("").trim();
        String callToAction = draft.path("callToAction").asText("").trim();
        String altText = draft.path("altText").asText("").trim();
        if (platform == SocialPlatform.INSTAGRAM) {
            hashtags = normalizeInstagramHashtags(hashtags);
        }

        if (platform == SocialPlatform.INSTAGRAM && caption.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI devolvio un caption incompleto");
        }
        if (platform == SocialPlatform.FACEBOOK && body.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI devolvio un texto de Facebook incompleto");
        }

        return new NewsSocialContentResponse(
                null,
                news.getId(),
                platform,
                blankToNull(caption),
                blankToNull(body),
                blankToNull(hashtags),
                blankToNull(callToAction),
                blankToNull(altText),
                null,
                null
        );
    }

    private JsonNode parseOpenAiJson(HttpResponse<String> response, String defaultError) throws Exception {
        JsonNode root = objectMapper.readTree(response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = root.path("error").path("message").asText(defaultError);
            throw new ResponseStatusException(BAD_GATEWAY, message);
        }

        String text = root.path("output_text").asText(null);
        if (text == null || text.isBlank()) {
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.path("content");
                    if (!content.isArray()) continue;
                    for (JsonNode part : content) {
                        String maybeText = part.path("text").asText(null);
                        if (maybeText != null && !maybeText.isBlank()) {
                            text = maybeText;
                            break;
                        }
                    }
                    if (text != null) break;
                }
            }
        }

        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI respondio sin texto utilizable");
        }

        return objectMapper.readTree(extractJson(text));
    }

    private String buildNewsPrompt(GenerateNewsRequest request) {
        return """
                Sos un redactor institucional especializado en comunicacion ambiental para reservas naturales.

                Genera un borrador de novedad para el sitio web de la Reserva Natural Lago Escondido.

                Reglas:
                - Usar lenguaje institucional, claro, calido y preciso.
                - Escribir en espanol de Argentina.
                - La novedad debe informar un hecho, avance, actividad, comunicacion o contenido editorial concreto.
                - No convertir la novedad en proyecto, timeline, documento tecnico ni gacetilla extensa.
                - No inventar datos concretos, fechas, instituciones, personas, cifras, resultados ni promesas que no esten en el brief.
                - Si falta informacion, redactar de forma prudente sin completar con datos inventados.
                - El title debe ser breve, natural y editorial, preferentemente entre 4 y 10 palabras.
                - El summary debe tener 1 o 2 frases breves para tarjetas y listados.
                - El content debe poder leerse como detalle publico de la novedad, con parrafos claros y sin markdown.
                - El slug debe ser amigable para URL.
                - Devolver unicamente JSON valido, sin markdown ni texto adicional:
                {
                  "title": "",
                  "summary": "",
                  "content": "",
                  "slug": ""
                }

                Tema / informacion base: %s
                Tono sugerido: %s
                Publico objetivo: %s
                Contexto adicional: %s
                """.formatted(
                request.brief(),
                blankToDefault(request.objective(), "Institucional, claro y cercano"),
                blankToDefault(request.targetAudience(), "Publico general"),
                blankToDefault(request.highlights(), "No indicado")
        );
    }

    private String buildSocialPrompt(News news, SocialPlatform platform, GenerateNewsSocialContentRequest request) {
        String platformRules = platform == SocialPlatform.INSTAGRAM
                ? """
                Plataforma: Instagram.
                - Crear un caption natural, breve y visual.
                - Incluir hashtags relevantes separados por espacios, sin exagerar cantidad.
                - Cada hashtag debe venir listo para copiar, empezando con #, por ejemplo: #ReservaLagoEscondido #Biodiversidad.
                - Proponer un CTA concreto para interaccion manual.
                - Proponer alt text descriptivo para la imagen principal cuando corresponda.
                - El body puede quedar vacio; el contenido principal debe estar en caption.
                """
                : """
                Plataforma: Facebook.
                - Crear un texto/cuerpo adaptado a Facebook, mas conversacional e informativo que el caption de Instagram.
                - Proponer un CTA concreto para interaccion manual.
                - Usar hashtags solo si aportan contexto y con moderacion.
                - El caption puede quedar vacio; el contenido principal debe estar en body.
                """;

        return """
                Sos un editor de redes sociales para la Reserva Natural Lago Escondido.

                Prepara contenido para publicacion manual, sin integrar APIs externas.

                Reglas:
                - Escribir en espanol de Argentina.
                - Adaptar el texto a la plataforma indicada; no copiar literalmente el texto web.
                - No inventar datos concretos, fechas, cifras, instituciones ni promesas.
                - Mantener tono institucional, cercano, ambiental y responsable.
                - No incluir instrucciones tecnicas ni mencionar IA.
                - Devolver unicamente JSON valido, sin markdown ni texto adicional:
                {
                  "caption": "",
                  "body": "",
                  "hashtags": "",
                  "callToAction": "",
                  "altText": ""
                }

                %s
                Indicaciones adicionales del administrador: %s

                Novedad web:
                Titulo: %s
                Resumen: %s
                Contenido: %s
                URL imagen principal: %s
                """.formatted(
                platformRules,
                blankToDefault(request == null ? null : request.instructions(), "No indicado"),
                news.getTitle(),
                news.getSummary(),
                news.getContent(),
                blankToDefault(news.getImageUrl(), "Sin imagen principal")
        );
    }

    private Map<String, Object> newsSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", new String[]{"title", "summary", "content", "slug"},
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "summary", Map.of("type", "string"),
                        "content", Map.of("type", "string"),
                        "slug", Map.of("type", "string")
                )
        );
    }

    private Map<String, Object> socialSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", new String[]{"caption", "body", "hashtags", "callToAction", "altText"},
                "properties", Map.of(
                        "caption", Map.of("type", "string"),
                        "body", Map.of("type", "string"),
                        "hashtags", Map.of("type", "string"),
                        "callToAction", Map.of("type", "string"),
                        "altText", Map.of("type", "string")
                )
        );
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String slugify(String value) {
        String slug = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isBlank()) slug = "novedad";
        return slug.length() > 160 ? slug.substring(0, 160).replaceAll("-+$", "") : slug;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeInstagramHashtags(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.replace(",", " ")
                .replace(";", " ")
                .lines()
                .flatMap(line -> java.util.Arrays.stream(line.trim().split("\\s+")))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(token -> token.startsWith("#") ? token : "#" + token)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
