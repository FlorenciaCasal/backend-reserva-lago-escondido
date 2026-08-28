package com.luismunozse.reservalago.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.repo.NewsRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsAiServiceTest {

    @Test
    void shouldNormalizeInstagramHashtagsGeneratedWithoutHash() throws Exception {
        NewsSocialContentResponse response = parseSocialResponse(
                SocialPlatform.INSTAGRAM,
                "ReservaNaturalLagoEscondido Biodiversidad EducacionAmbiental"
        );

        assertThat(response.hashtags())
                .isEqualTo("#ReservaNaturalLagoEscondido #Biodiversidad #EducacionAmbiental");
    }

    @Test
    void shouldNotNormalizeFacebookHashtags() throws Exception {
        NewsSocialContentResponse response = parseSocialResponse(
                SocialPlatform.FACEBOOK,
                "ReservaNaturalLagoEscondido Biodiversidad EducacionAmbiental"
        );

        assertThat(response.hashtags())
                .isEqualTo("ReservaNaturalLagoEscondido Biodiversidad EducacionAmbiental");
    }

    private NewsSocialContentResponse parseSocialResponse(SocialPlatform platform, String hashtags) throws Exception {
        NewsAiService service = new NewsAiService(new ObjectMapper(), mock(NewsRepository.class));
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("""
                {
                  "output_text": "{\\"caption\\":\\"Caption\\",\\"body\\":\\"Texto Facebook\\",\\"hashtags\\":\\"%s\\",\\"callToAction\\":\\"CTA\\",\\"altText\\":\\"Alt text\\"}"
                }
                """.formatted(hashtags));

        News news = new News();
        news.setId(UUID.randomUUID());
        news.setTitle("Novedad");

        Method method = NewsAiService.class.getDeclaredMethod(
                "parseSocialResponse",
                HttpResponse.class,
                News.class,
                SocialPlatform.class
        );
        method.setAccessible(true);
        return (NewsSocialContentResponse) method.invoke(service, httpResponse, news, platform);
    }
}
