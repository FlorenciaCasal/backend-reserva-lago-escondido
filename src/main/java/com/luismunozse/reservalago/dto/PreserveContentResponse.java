package com.luismunozse.reservalago.dto;

import java.util.List;

public record PreserveContentResponse(
        String intro,
        List<String> heroAsideLines,
        String whatEyebrow,
        String whatWeDoText,
        List<String> bullets,
        String territoryEyebrow,
        String territoryTitle,
        String territoryText,
        String territoryMetricValue,
        String territoryMetricDescription,
        String territoryResearchTitle,
        String territoryResearchDescription,
        String territoryEducationTitle,
        String territoryEducationDescription
) {
}
