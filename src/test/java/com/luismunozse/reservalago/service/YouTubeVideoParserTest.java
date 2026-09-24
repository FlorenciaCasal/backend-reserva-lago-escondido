package com.luismunozse.reservalago.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubeVideoParserTest {

    private final YouTubeVideoParser parser = new YouTubeVideoParser();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "dQw4w9WgXcQ"
    })
    void shouldNormalizeSupportedYoutubeUrls(String value) {
        assertThat(parser.normalizeVideoId(value)).isEqualTo("dQw4w9WgXcQ");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<iframe src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\"></iframe>",
            "javascript:alert(1)",
            "https://youtube.evil.com/watch?v=dQw4w9WgXcQ",
            "https://example.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/channel/dQw4w9WgXcQ"
    })
    void shouldRejectUnsafeOrUnsupportedValues(String value) {
        assertThatThrownBy(() -> parser.normalizeVideoId(value))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldBuildPrivacyEnhancedEmbedAndThumbnailUrls() {
        assertThat(parser.embedUrl("dQw4w9WgXcQ"))
                .isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        assertThat(parser.thumbnailUrl("dQw4w9WgXcQ"))
                .isEqualTo("https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
    }
}
