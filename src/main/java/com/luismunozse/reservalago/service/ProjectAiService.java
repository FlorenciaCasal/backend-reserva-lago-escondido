package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.GenerateProjectRequest;
import com.luismunozse.reservalago.dto.GeneratedProjectDraft;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
public class ProjectAiService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final int MAX_SUMMARY_LENGTH = 600;
    private static final int TARGET_SUMMARY_LENGTH = 550;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]+[\"')\\]]*");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;

    @Value("${app.openai.model:${OPENAI_MODEL:gpt-4o-mini}}")
    private String model;

    public GeneratedProjectDraft generate(GenerateProjectRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Falta configurar OPENAI_API_KEY en el entorno del backend"
            );
        }

        String prompt = buildPrompt(request);

        try {
            HttpResponse<String> response = callOpenAi(prompt, true);
            try {
                return parseResponse(response, request.imageUrl());
            } catch (Exception structuredError) {
                HttpResponse<String> fallbackResponse = callOpenAi(prompt, false);
                return parseResponse(fallbackResponse, request.imageUrl());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "No se pudo generar el proyecto con IA");
        }
    }

    private HttpResponse<String> callOpenAi(String prompt, boolean structured) throws Exception {
        Map<String, Object> body = structured
                ? Map.of(
                        "model", model == null || model.isBlank() ? DEFAULT_MODEL : model,
                        "input", prompt,
                        "text", Map.of(
                                "format", Map.of(
                                        "type", "json_schema",
                                        "name", "generated_project",
                                        "strict", true,
                                        "schema", projectSchema()
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

    private GeneratedProjectDraft parseResponse(HttpResponse<String> response, String imageUrl) throws Exception {
        JsonNode root = objectMapper.readTree(response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = root.path("error").path("message").asText("OpenAI no pudo generar el proyecto");
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

        JsonNode draft = objectMapper.readTree(extractJson(text));
        String title = draft.path("title").asText("").trim();
        String summary = normalizeSummary(draft.path("summary").asText("").trim());
        String content = draft.path("content").asText("").trim();
        String slug = draft.path("slug").asText("").trim();

        if (title.isBlank() || summary.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI devolvio un proyecto incompleto");
        }

        return new GeneratedProjectDraft(
                title,
                summary,
                content,
                slug.isBlank() ? slugify(title) : slugify(slug),
                imageUrl
        );
    }

    private String buildPrompt(GenerateProjectRequest request) {
        return """
                Sos un redactor institucional especializado en conservacion ambiental para reservas naturales.

                Genera un borrador de proyecto de conservacion para el sitio web de la Reserva Natural Lago Escondido.

                Reglas:
                - Usar lenguaje institucional, claro, calido y amigable.
                - Evitar tono comercial exagerado.
                - Antes de redactar, identificar internamente un foco narrativo concreto del proyecto, pero no devolverlo como campo separado.
                - Evitar textos planos, genericos o intercambiables; cada proyecto debe sentirse especifico segun su especie, ambiente, problema, accion o territorio.
                - Considerar "Material fuente / resumen tecnico" como la base tecnica del proyecto; puede ser una sintesis generada con NotebookLM a partir de documentos cientificos.
                - Considerar "Aspectos a destacar" como informacion prioritaria desde el punto de vista cientifico y comunicacional; puede ser una sintesis generada con NotebookLM a partir de respuestas de investigadores.
                - Utilizar ambos textos de forma complementaria. Si hay informacion en ambos, integrarla sin repetir contenido.
                - Nunca ignorar los aspectos a destacar cuando esten presentes.
                - No inventar datos concretos, cifras, instituciones, resultados ni promesas que no esten en el material fuente o en los aspectos a destacar.
                - Toda URL http(s) presente en el material fuente o en los aspectos a destacar debe conservarse exactamente.
                - Las URLs relevantes deben aparecer en el content generado; no eliminarlas, reescribirlas, resumirlas ni cambiar su formato.
                - Si hay mas de una URL, conservarlas todas salvo que esten claramente duplicadas.
                - Si una URL corresponde a una institucion, organismo, fundacion o fuente de respaldo, integrarla de forma natural en el texto.
                - Si el material fuente o los aspectos a destacar contienen informacion legal, normativa, autorizaciones, resoluciones, leyes, categorias de conservacion, permisos, prohibiciones o restricciones de uso, incluir una mencion clara y comprensible cuando sea relevante.
                - No inventar datos legales, no exagerar su alcance y no transformar su sentido.
                - No ocultar informacion legal relevante si esta presente en el material fuente o en los aspectos a destacar.
                - Transmitir conservacion, educacion ambiental, cuidado del territorio y participacion responsable.
                - Escribir en espanol de Argentina.
                - Tomar el title desde el elemento mas concreto disponible: especie, ambiente, problema, accion o territorio.
                - El title debe ser breve, natural y editorial, preferentemente entre 3 y 8 palabras.
                - El title no debe empezar siempre con "Proyecto de" ni terminar con "Reserva Natural Lago Escondido".
                - Evitar titulos repetitivos o burocraticos; usar nombres claros como "Bosque de Alerces", "Monitoreo del Huemul" o "Restauracion de Humedales".
                - El summary debe ser una introduccion rapida de 1 o 2 frases breves, pensada para ocupar pocas lineas en tarjetas y listados.
                - El summary debe tener como maximo 600 caracteres incluyendo espacios.
                - Idealmente el summary debe quedar entre 450 y 550 caracteres para no quedar justo en el limite.
                - El summary debe ser realmente un resumen breve del proyecto, no una version extensa ni una repeticion del content.
                - Evitar summaries extensos, explicaciones completas o repeticion literal del titulo.
                - El content debe estar estructurado narrativamente, aunque sin subtitulos obligatorios, cubriendo: contexto, problema o desafio, acciones del proyecto, impacto esperado e importancia para la reserva o la comunidad.
                - Devolver unicamente JSON valido, sin markdown ni texto adicional:
                {
                  "title": "",
                  "summary": "",
                  "content": "",
                  "slug": ""
                }

                Material fuente / resumen tecnico: %s
                Aspectos a destacar: %s
                Objetivo de comunicacion: %s
                Publico objetivo: %s
                """.formatted(
                request.description(),
                blankToDefault(request.highlights(), "No indicado"),
                request.objective(),
                request.targetAudience()
        );
    }

    private Map<String, Object> projectSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", new String[]{"title", "summary", "content", "slug"},
                "properties", Map.of(
                        "title", Map.of("type", "string"),
                        "summary", Map.of(
                                "type", "string",
                                "maxLength", MAX_SUMMARY_LENGTH,
                                "description", "Resumen breve de maximo 600 caracteres incluyendo espacios; idealmente entre 450 y 550 caracteres."
                        ),
                        "content", Map.of("type", "string"),
                        "slug", Map.of("type", "string")
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
        return slug.isBlank() ? "proyecto" : slug;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    String normalizeSummary(String summary) {
        if (summary.length() <= MAX_SUMMARY_LENGTH) {
            return summary;
        }

        String normalized = summary.replaceAll("\\s+", " ").trim();
        String targetSummary = completeSentencesWithin(normalized, TARGET_SUMMARY_LENGTH);
        if (!targetSummary.isBlank()) {
            return targetSummary;
        }

        String maxSummary = completeSentencesWithin(normalized, MAX_SUMMARY_LENGTH);
        if (!maxSummary.isBlank()) {
            return maxSummary;
        }

        return truncateAtWordBoundary(normalized);
    }

    private String completeSentencesWithin(String value, int maxLength) {
        Matcher matcher = SENTENCE_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String sentence = matcher.group().trim();
            int nextLength = result.isEmpty() ? sentence.length() : result.length() + 1 + sentence.length();
            if (nextLength > maxLength) {
                break;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(sentence);
        }

        return result.toString().trim();
    }

    private String truncateAtWordBoundary(String value) {
        int maxBodyLength = MAX_SUMMARY_LENGTH - 1;
        int end = Math.min(maxBodyLength, value.length());
        int lastSpace = value.lastIndexOf(' ', end);
        if (lastSpace >= 120) {
            end = lastSpace;
        }

        String truncated = value.substring(0, end).stripTrailing().replaceAll("[,;:]+$", "");
        return truncated + ".";
    }
}
