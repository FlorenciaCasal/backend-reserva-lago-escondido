package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.PreserveContentRequest;
import com.luismunozse.reservalago.dto.PreserveContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class PreserveContentService {

    private static final String WHAT_WE_DO_TEXT = "preservar_what_we_do_text";
    private static final String WHAT_WE_DO_BULLETS = "preservar_what_we_do_bullets";
    private static final String INTRO = "preservar_intro";
    private static final String HERO_ASIDE_LINES = "preservar_hero_aside_lines";
    private static final String WHAT_EYEBROW = "preservar_what_eyebrow";
    private static final String TERRITORY_EYEBROW = "preservar_territory_eyebrow";
    private static final String TERRITORY_TITLE = "preservar_territory_title";
    private static final String TERRITORY_TEXT = "preservar_territory_text";
    private static final String TERRITORY_METRIC_VALUE = "preservar_territory_metric_value";
    private static final String TERRITORY_METRIC_DESCRIPTION = "preservar_territory_metric_description";
    private static final String TERRITORY_RESEARCH_TITLE = "preservar_territory_research_title";
    private static final String TERRITORY_RESEARCH_DESCRIPTION = "preservar_territory_research_description";
    private static final String TERRITORY_EDUCATION_TITLE = "preservar_territory_education_title";
    private static final String TERRITORY_EDUCATION_DESCRIPTION = "preservar_territory_education_description";
    private static final String DEFAULT_INTRO = "Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.";
    private static final String DEFAULT_TEXT = "Trabajamos en la protección de especies nativas, la restauración de ambientes y la investigación científica para comprender y cuidar nuestro entorno. Nuestro compromiso es integral y se basa en pilares técnicos y educativos.";
    private static final String DEFAULT_WHAT_EYEBROW = "Nuestra tarea";
    private static final String DEFAULT_TERRITORY_EYEBROW = "Un territorio con sentido";
    private static final String DEFAULT_TERRITORY_TITLE = "Un territorio destinado a la conservación";
    private static final String DEFAULT_TERRITORY_TEXT = "El 42% de la Reserva Natural Lago Escondido está destinado a la protección de ambientes naturales, sus especies y los procesos ecológicos que los sostienen. Conservar es garantizar que este patrimonio natural perdure en el tiempo.";
    private static final String DEFAULT_TERRITORY_METRIC_VALUE = "42%";
    private static final String DEFAULT_TERRITORY_METRIC_DESCRIPTION = "Del territorio destinado a conservación";
    private static final String DEFAULT_TERRITORY_RESEARCH_TITLE = "Investigación";
    private static final String DEFAULT_TERRITORY_RESEARCH_DESCRIPTION = "Para conocer y monitorear los ecosistemas";
    private static final String DEFAULT_TERRITORY_EDUCATION_TITLE = "Educación ambiental";
    private static final String DEFAULT_TERRITORY_EDUCATION_DESCRIPTION = "Para acercar el conocimiento y promover su cuidado";
    private static final List<String> DEFAULT_HERO_ASIDE_LINES = List.of(
            "Naturaleza",
            "hoy,",
            "mañana,",
            "siempre"
    );
    private static final List<String> DEFAULT_BULLETS = List.of(
            "Conservación de especies nativas",
            "Restauración de ecosistemas",
            "Investigación y monitoreo",
            "Educación ambiental"
    );

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    public PreserveContentResponse getContent() {
        return new PreserveContentResponse(
                systemConfigService.getValue(INTRO, DEFAULT_INTRO),
                readList(systemConfigService.getValue(HERO_ASIDE_LINES, "[]"), DEFAULT_HERO_ASIDE_LINES),
                systemConfigService.getValue(WHAT_EYEBROW, DEFAULT_WHAT_EYEBROW),
                systemConfigService.getValue(WHAT_WE_DO_TEXT, DEFAULT_TEXT),
                readList(systemConfigService.getValue(WHAT_WE_DO_BULLETS, "[]"), DEFAULT_BULLETS),
                systemConfigService.getValue(TERRITORY_EYEBROW, DEFAULT_TERRITORY_EYEBROW),
                systemConfigService.getValue(TERRITORY_TITLE, DEFAULT_TERRITORY_TITLE),
                systemConfigService.getValue(TERRITORY_TEXT, DEFAULT_TERRITORY_TEXT),
                systemConfigService.getValue(TERRITORY_METRIC_VALUE, DEFAULT_TERRITORY_METRIC_VALUE),
                systemConfigService.getValue(TERRITORY_METRIC_DESCRIPTION, DEFAULT_TERRITORY_METRIC_DESCRIPTION),
                systemConfigService.getValue(TERRITORY_RESEARCH_TITLE, DEFAULT_TERRITORY_RESEARCH_TITLE),
                systemConfigService.getValue(TERRITORY_RESEARCH_DESCRIPTION, DEFAULT_TERRITORY_RESEARCH_DESCRIPTION),
                systemConfigService.getValue(TERRITORY_EDUCATION_TITLE, DEFAULT_TERRITORY_EDUCATION_TITLE),
                systemConfigService.getValue(TERRITORY_EDUCATION_DESCRIPTION, DEFAULT_TERRITORY_EDUCATION_DESCRIPTION)
        );
    }

    @Transactional
    public PreserveContentResponse updateContent(PreserveContentRequest request) {
        List<String> bullets = request.bullets().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (bullets.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debe indicar al menos un item");
        }

        List<String> heroAsideLines = request.heroAsideLines().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (heroAsideLines.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Debe indicar al menos una linea editorial");
        }

        systemConfigService.setValue(INTRO, request.intro().trim());
        systemConfigService.setValue(HERO_ASIDE_LINES, writeList(heroAsideLines));
        systemConfigService.setValue(WHAT_EYEBROW, request.whatEyebrow().trim());
        systemConfigService.setValue(WHAT_WE_DO_TEXT, request.whatWeDoText().trim());
        systemConfigService.setValue(WHAT_WE_DO_BULLETS, writeList(bullets));
        systemConfigService.setValue(TERRITORY_EYEBROW, request.territoryEyebrow().trim());
        systemConfigService.setValue(TERRITORY_TITLE, request.territoryTitle().trim());
        systemConfigService.setValue(TERRITORY_TEXT, request.territoryText().trim());
        systemConfigService.setValue(TERRITORY_METRIC_VALUE, request.territoryMetricValue().trim());
        systemConfigService.setValue(TERRITORY_METRIC_DESCRIPTION, request.territoryMetricDescription().trim());
        systemConfigService.setValue(TERRITORY_RESEARCH_TITLE, request.territoryResearchTitle().trim());
        systemConfigService.setValue(TERRITORY_RESEARCH_DESCRIPTION, request.territoryResearchDescription().trim());
        systemConfigService.setValue(TERRITORY_EDUCATION_TITLE, request.territoryEducationTitle().trim());
        systemConfigService.setValue(TERRITORY_EDUCATION_DESCRIPTION, request.territoryEducationDescription().trim());

        return new PreserveContentResponse(
                request.intro().trim(),
                heroAsideLines,
                request.whatEyebrow().trim(),
                request.whatWeDoText().trim(),
                bullets,
                request.territoryEyebrow().trim(),
                request.territoryTitle().trim(),
                request.territoryText().trim(),
                request.territoryMetricValue().trim(),
                request.territoryMetricDescription().trim(),
                request.territoryResearchTitle().trim(),
                request.territoryResearchDescription().trim(),
                request.territoryEducationTitle().trim(),
                request.territoryEducationDescription().trim()
        );
    }

    private List<String> readList(String value, List<String> fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            List<String> items = objectMapper.readValue(value, new TypeReference<>() {});
            return items.isEmpty() ? fallback : items;
        } catch (JsonProcessingException ex) {
            return fallback;
        }
    }

    private String writeList(List<String> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo guardar el listado");
        }
    }
}
