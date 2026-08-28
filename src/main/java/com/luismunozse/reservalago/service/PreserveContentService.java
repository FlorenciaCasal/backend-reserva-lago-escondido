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
    private static final String DEFAULT_INTRO = "Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.";
    private static final String DEFAULT_TEXT = "Trabajamos en la proteccion de especies nativas, la restauracion de ambientes y la investigacion cientifica para comprender y cuidar nuestro entorno. Nuestro compromiso es integral y se basa en pilares tecnicos y educativos.";
    private static final List<String> DEFAULT_BULLETS = List.of(
            "Conservacion de especies nativas",
            "Restauracion de ecosistemas",
            "Investigacion y monitoreo",
            "Educacion ambiental"
    );

    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    public PreserveContentResponse getContent() {
        return new PreserveContentResponse(
                systemConfigService.getValue(INTRO, DEFAULT_INTRO),
                systemConfigService.getValue(WHAT_WE_DO_TEXT, DEFAULT_TEXT),
                readBullets(systemConfigService.getValue(WHAT_WE_DO_BULLETS, "[]"))
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

        systemConfigService.setValue(INTRO, request.intro().trim());
        systemConfigService.setValue(WHAT_WE_DO_TEXT, request.whatWeDoText().trim());
        systemConfigService.setValue(WHAT_WE_DO_BULLETS, writeBullets(bullets));

        return new PreserveContentResponse(request.intro().trim(), request.whatWeDoText().trim(), bullets);
    }

    private List<String> readBullets(String value) {
        try {
            List<String> bullets = objectMapper.readValue(value, new TypeReference<>() {});
            return bullets.isEmpty() ? DEFAULT_BULLETS : bullets;
        } catch (JsonProcessingException ex) {
            return DEFAULT_BULLETS;
        }
    }

    private String writeBullets(List<String> bullets) {
        try {
            return objectMapper.writeValueAsString(bullets);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "No se pudo guardar el listado");
        }
    }
}
