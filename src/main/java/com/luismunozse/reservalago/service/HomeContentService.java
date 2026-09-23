package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.HomeContentRequest;
import com.luismunozse.reservalago.dto.HomeContentResponse;
import com.luismunozse.reservalago.dto.HomePillarContentRequest;
import com.luismunozse.reservalago.dto.HomePillarContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeContentService {

    private static final String HERO_TITLE = "home_hero_title";
    private static final String HERO_SUBTITLE = "home_hero_subtitle";
    private static final String HERO_IMAGE_URL = "home_hero_image_url";
    private static final String INTRO_TEXT = "home_intro_text";
    private static final String ACTION_TITLE = "home_action_title";
    private static final String PROJECTS_TITLE = "home_projects_title";
    private static final String PROJECTS_CTA_LABEL = "home_projects_cta_label";
    private static final String NEWS_TITLE = "home_news_title";
    private static final String NEWS_CTA_LABEL = "home_news_cta_label";
    private static final String VISITS_EYEBROW = "home_visits_eyebrow";
    private static final String VISITS_TITLE = "home_visits_title";
    private static final String VISITS_TEXT = "home_visits_text";
    private static final String VISITS_CTA_LABEL = "home_visits_cta_label";
    private static final String VISITS_IMAGE_URL = "home_visits_image_url";

    private static final String DEFAULT_HERO_TITLE = "Conservar, habitar y producir de manera sostenible";
    private static final String DEFAULT_HERO_SUBTITLE = "Área natural privada, Paraje El Foyel, Río Negro.";
    private static final String DEFAULT_HERO_IMAGE_URL = "/img/home.jpg";
    private static final String DEFAULT_INTRO_TEXT = "Trabajamos para conservar los ecosistemas, desarrollar actividades productivas responsables y promover una forma sostenible de habitar el territorio.";
    private static final String DEFAULT_ACTION_TITLE = "Nuestras líneas de acción";
    private static final String DEFAULT_PROJECTS_TITLE = "Proyectos destacados";
    private static final String DEFAULT_PROJECTS_CTA_LABEL = "Ver todos los proyectos";
    private static final String DEFAULT_NEWS_TITLE = "Novedades";
    private static final String DEFAULT_NEWS_CTA_LABEL = "Ver todas las novedades";
    private static final String DEFAULT_VISITS_EYEBROW = "Visitas";
    private static final String DEFAULT_VISITS_TITLE = "Vivi la reserva y conoce su entorno natural";
    private static final String DEFAULT_VISITS_TEXT = "Organizamos visitas para acercar la experiencia de la Reserva Natural Lago Escondido a quienes desean conocer, aprender y disfrutar este territorio con responsabilidad.";
    private static final String DEFAULT_VISITS_CTA_LABEL = "Reservar visita";
    private static final String DEFAULT_VISITS_IMAGE_URL = "/img/particular.jpg";

    private final SystemConfigService systemConfigService;

    public HomeContentResponse getContent() {
        return new HomeContentResponse(
                systemConfigService.getValue(HERO_TITLE, DEFAULT_HERO_TITLE),
                systemConfigService.getValue(HERO_SUBTITLE, DEFAULT_HERO_SUBTITLE),
                systemConfigService.getValue(HERO_IMAGE_URL, DEFAULT_HERO_IMAGE_URL),
                systemConfigService.getValue(INTRO_TEXT, DEFAULT_INTRO_TEXT),
                systemConfigService.getValue(ACTION_TITLE, DEFAULT_ACTION_TITLE),
                readPillar("conservar", "Conservar", "42", "%", "del territorio destinado a conservación", "Protegemos áreas de alto valor natural a través de la investigación, el monitoreo y la educación ambiental.", "CONOCÉ MÁS"),
                readPillar("habitar", "Habitar", "200", "", "personas habitan la reserva", "Promovemos una forma responsable de habitar el territorio, en convivencia con el entorno natural.", "CONOCÉ MÁS"),
                readPillar("producir", "Producir", "58", "%", "del territorio destinado a producción sostenible", "Desarrollamos actividades productivas responsables, compatibles con la conservación y el cuidado del territorio.", "CONOCÉ MÁS"),
                systemConfigService.getValue(PROJECTS_TITLE, DEFAULT_PROJECTS_TITLE),
                systemConfigService.getValue(PROJECTS_CTA_LABEL, DEFAULT_PROJECTS_CTA_LABEL),
                systemConfigService.getValue(NEWS_TITLE, DEFAULT_NEWS_TITLE),
                systemConfigService.getValue(NEWS_CTA_LABEL, DEFAULT_NEWS_CTA_LABEL),
                systemConfigService.getValue(VISITS_EYEBROW, DEFAULT_VISITS_EYEBROW),
                systemConfigService.getValue(VISITS_TITLE, DEFAULT_VISITS_TITLE),
                systemConfigService.getValue(VISITS_TEXT, DEFAULT_VISITS_TEXT),
                systemConfigService.getValue(VISITS_CTA_LABEL, DEFAULT_VISITS_CTA_LABEL),
                systemConfigService.getValue(VISITS_IMAGE_URL, DEFAULT_VISITS_IMAGE_URL)
        );
    }

    @Transactional
    public HomeContentResponse updateContent(HomeContentRequest request) {
        systemConfigService.setValue(HERO_TITLE, request.heroTitle().trim());
        systemConfigService.setValue(HERO_SUBTITLE, request.heroSubtitle().trim());
        systemConfigService.setValue(HERO_IMAGE_URL, request.heroImageUrl().trim());
        systemConfigService.setValue(INTRO_TEXT, request.introText().trim());
        systemConfigService.setValue(ACTION_TITLE, request.actionTitle().trim());
        writePillar("conservar", request.conservar());
        writePillar("habitar", request.habitar());
        writePillar("producir", request.producir());
        systemConfigService.setValue(PROJECTS_TITLE, request.projectsTitle().trim());
        systemConfigService.setValue(PROJECTS_CTA_LABEL, request.projectsCtaLabel().trim());
        systemConfigService.setValue(NEWS_TITLE, request.newsTitle().trim());
        systemConfigService.setValue(NEWS_CTA_LABEL, request.newsCtaLabel().trim());
        systemConfigService.setValue(VISITS_EYEBROW, request.visitsEyebrow().trim());
        systemConfigService.setValue(VISITS_TITLE, request.visitsTitle().trim());
        systemConfigService.setValue(VISITS_TEXT, request.visitsText().trim());
        systemConfigService.setValue(VISITS_CTA_LABEL, request.visitsCtaLabel().trim());
        systemConfigService.setValue(VISITS_IMAGE_URL, request.visitsImageUrl().trim());

        return getContent();
    }

    private HomePillarContentResponse readPillar(
            String prefix,
            String defaultTitle,
            String defaultValue,
            String defaultSuffix,
            String defaultStatLabel,
            String defaultText,
            String defaultCtaLabel
    ) {
        return new HomePillarContentResponse(
                systemConfigService.getValue("home_" + prefix + "_title", defaultTitle),
                systemConfigService.getValue("home_" + prefix + "_value", defaultValue),
                systemConfigService.getValue("home_" + prefix + "_suffix", defaultSuffix),
                systemConfigService.getValue("home_" + prefix + "_stat_label", defaultStatLabel),
                systemConfigService.getValue("home_" + prefix + "_text", defaultText),
                systemConfigService.getValue("home_" + prefix + "_cta_label", defaultCtaLabel)
        );
    }

    private void writePillar(String prefix, HomePillarContentRequest pillar) {
        systemConfigService.setValue("home_" + prefix + "_title", pillar.title().trim());
        systemConfigService.setValue("home_" + prefix + "_value", pillar.value().trim());
        systemConfigService.setValue("home_" + prefix + "_suffix", pillar.suffix() == null ? "" : pillar.suffix().trim());
        systemConfigService.setValue("home_" + prefix + "_stat_label", pillar.statLabel().trim());
        systemConfigService.setValue("home_" + prefix + "_text", pillar.text().trim());
        systemConfigService.setValue("home_" + prefix + "_cta_label", pillar.ctaLabel().trim());
    }
}
