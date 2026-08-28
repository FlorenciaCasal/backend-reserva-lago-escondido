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
        when(systemConfigService.getValue("preservar_intro", "Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras."))
                .thenReturn("Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.");
        when(systemConfigService.getValue("preservar_what_we_do_text", defaultText()))
                .thenReturn("Texto configurado");
        when(systemConfigService.getValue("preservar_what_we_do_bullets", "[]"))
                .thenReturn("[\"Uno\",\"Dos\"]");

        PreserveContentResponse response = service.getContent();

        assertThat(response.intro()).isEqualTo("Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.");
        assertThat(response.whatWeDoText()).isEqualTo("Texto configurado");
        assertThat(response.bullets()).containsExactly("Uno", "Dos");
    }

    @Test
    void shouldFallbackToDefaultBulletsWhenJsonIsInvalid() {
        PreserveContentService service = new PreserveContentService(systemConfigService, objectMapper);
        when(systemConfigService.getValue("preservar_intro", "Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras."))
                .thenReturn("Conservamos la biodiversidad y los ecosistemas para las generaciones presentes y futuras.");
        when(systemConfigService.getValue("preservar_what_we_do_text", defaultText()))
                .thenReturn("Texto configurado");
        when(systemConfigService.getValue("preservar_what_we_do_bullets", "[]"))
                .thenReturn("invalid-json");

        PreserveContentResponse response = service.getContent();

        assertThat(response.bullets()).contains("Conservacion de especies nativas");
    }

    @Test
    void shouldTrimAndSaveContent() throws Exception {
        PreserveContentService service = new PreserveContentService(systemConfigService, objectMapper);

        PreserveContentResponse response = service.updateContent(new PreserveContentRequest(
                " Intro ",
                " Texto ",
                List.of(" Uno ", "", " Dos ")
        ));

        assertThat(response.intro()).isEqualTo("Intro");
        assertThat(response.whatWeDoText()).isEqualTo("Texto");
        assertThat(response.bullets()).containsExactly("Uno", "Dos");
        verify(systemConfigService).setValue("preservar_intro", "Intro");
        verify(systemConfigService).setValue("preservar_what_we_do_text", "Texto");
        verify(systemConfigService).setValue("preservar_what_we_do_bullets", "[\"Uno\",\"Dos\"]");
    }

    private String defaultText() {
        return "Trabajamos en la proteccion de especies nativas, la restauracion de ambientes y la investigacion cientifica para comprender y cuidar nuestro entorno. Nuestro compromiso es integral y se basa en pilares tecnicos y educativos.";
    }
}
