package com.luismunozse.reservalago.dto;

public record HomeContentResponse(
        String heroTitle,
        String heroSubtitle,
        String heroImageUrl,
        String introText,
        String actionTitle,
        HomePillarContentResponse conservar,
        HomePillarContentResponse habitar,
        HomePillarContentResponse producir,
        String projectsTitle,
        String projectsCtaLabel,
        String newsTitle,
        String newsCtaLabel,
        String visitsEyebrow,
        String visitsTitle,
        String visitsText,
        String visitsCtaLabel,
        String visitsImageUrl
) {
}
