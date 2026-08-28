package com.luismunozse.reservalago.dto;

import java.util.List;

public record PreserveContentResponse(
        String intro,
        String whatWeDoText,
        List<String> bullets
) {
}
