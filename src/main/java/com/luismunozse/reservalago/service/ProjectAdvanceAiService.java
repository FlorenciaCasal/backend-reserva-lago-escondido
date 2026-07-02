package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.GenerateProjectAdvanceRequest;
import com.luismunozse.reservalago.dto.GeneratedProjectAdvanceDraft;
import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.repo.ProjectAdvanceRepository;
import com.luismunozse.reservalago.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProjectAdvanceAiService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final ObjectMapper objectMapper;
    private final ProjectRepository projectRepository;
    private final ProjectAdvanceRepository projectAdvanceRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;

    @Value("${app.openai.model:${OPENAI_MODEL:gpt-4o-mini}}")
    private String model;

    public GeneratedProjectAdvanceDraft generate(UUID projectId, GenerateProjectAdvanceRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    INTERNAL_SERVER_ERROR,
                    "Falta configurar OPENAI_API_KEY en el entorno del backend"
            );
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Proyecto no encontrado"));

        String prompt = buildPrompt(project, request);

        try {
            HttpResponse<String> response = callOpenAi(prompt, true);
            try {
                return parseResponse(response, request.advanceDate());
            } catch (Exception structuredError) {
                HttpResponse<String> fallbackResponse = callOpenAi(prompt, false);
                return parseResponse(fallbackResponse, request.advanceDate());
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_GATEWAY, "No se pudo generar el avance con IA");
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
                                        "name", "generated_project_advance",
                                        "strict", true,
                                        "schema", advanceSchema()
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

    private GeneratedProjectAdvanceDraft parseResponse(HttpResponse<String> response, LocalDate fallbackDate) throws Exception {
        JsonNode root = objectMapper.readTree(response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = root.path("error").path("message").asText("OpenAI no pudo generar el avance");
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
        String description = draft.path("description").asText("").trim();
        String advanceDateValue = draft.path("advanceDate").asText("").trim();

        if (title.isBlank() || description.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI devolvio un avance incompleto");
        }

        LocalDate advanceDate = fallbackDate == null ? LocalDate.now() : fallbackDate;
        if (!advanceDateValue.isBlank()) {
            try {
                advanceDate = LocalDate.parse(advanceDateValue);
            } catch (Exception ignored) {
                advanceDate = fallbackDate == null ? LocalDate.now() : fallbackDate;
            }
        }

        return new GeneratedProjectAdvanceDraft(advanceDate, title, description);
    }

    private String buildPrompt(Project project, GenerateProjectAdvanceRequest request) {
        String previousAdvances = projectAdvanceRepository.findByProjectIdOrderByAdvanceDateDescCreatedAtDesc(project.getId())
                .stream()
                .limit(5)
                .map(advance -> "- %s | %s: %s".formatted(advance.getAdvanceDate(), advance.getTitle(), advance.getDescription()))
                .reduce("", (current, line) -> current + line + "\n");

        return """
                Sos un redactor institucional especializado en conservacion ambiental para reservas naturales.

                Genera un borrador de avance para un proyecto existente de la Reserva Natural Lago Escondido.

                Reglas:
                - Usar lenguaje institucional, claro, calido y concreto.
                - Mantener tono informativo y de seguimiento cronologico.
                - No inventar datos tecnicos, cifras, instituciones ni resultados que no esten en el brief o contexto.
                - Escribir en espanol de Argentina.
                - Devolver unicamente JSON valido, sin markdown ni texto adicional:
                {
                  "advanceDate": "YYYY-MM-DD",
                  "title": "",
                  "description": ""
                }

                Proyecto:
                Titulo: %s
                Resumen: %s
                Contenido: %s

                Avances anteriores:
                %s

                Brief del avance:
                Que ocurrio: %s
                Fecha sugerida por admin: %s
                Datos relevantes: %s
                Tono deseado: %s
                """.formatted(
                project.getTitle(),
                project.getSummary(),
                project.getContent(),
                previousAdvances.isBlank() ? "Sin avances anteriores cargados." : previousAdvances,
                request.whatHappened(),
                request.advanceDate() == null ? "No indicada" : request.advanceDate(),
                blankToDefault(request.relevantData(), "No indicado"),
                blankToDefault(request.tone(), "Institucional")
        );
    }

    private Map<String, Object> advanceSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", new String[]{"advanceDate", "title", "description"},
                "properties", Map.of(
                        "advanceDate", Map.of("type", "string"),
                        "title", Map.of("type", "string"),
                        "description", Map.of("type", "string")
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

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
