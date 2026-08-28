package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.NewsSocialContentRequest;
import com.luismunozse.reservalago.dto.NewsSocialContentResponse;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsSocialContent;
import com.luismunozse.reservalago.model.SocialPlatform;
import com.luismunozse.reservalago.repo.NewsRepository;
import com.luismunozse.reservalago.repo.NewsSocialContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsSocialContentServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsSocialContentRepository newsSocialContentRepository;

    @InjectMocks
    private NewsSocialContentService service;

    @Test
    void shouldCreateSocialContentForPlatform() {
        UUID newsId = UUID.randomUUID();
        News news = news(newsId);
        when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));
        when(newsSocialContentRepository.findByNewsIdAndPlatform(newsId, SocialPlatform.INSTAGRAM))
                .thenReturn(Optional.empty());
        when(newsSocialContentRepository.save(any(NewsSocialContent.class))).thenAnswer(invocation -> {
            NewsSocialContent content = invocation.getArgument(0);
            content.setId(UUID.randomUUID());
            return content;
        });

        NewsSocialContentResponse response = service.save(newsId, SocialPlatform.INSTAGRAM, new NewsSocialContentRequest(
                "Caption",
                null,
                "#Reserva",
                "Leer mas",
                "Alt text"
        ));

        assertThat(response.newsId()).isEqualTo(newsId);
        assertThat(response.platform()).isEqualTo(SocialPlatform.INSTAGRAM);
        assertThat(response.caption()).isEqualTo("Caption");
        assertThat(response.body()).isNull();
    }

    @Test
    void shouldUpdateCurrentVersionForPlatform() {
        UUID newsId = UUID.randomUUID();
        News news = news(newsId);
        NewsSocialContent existing = new NewsSocialContent();
        existing.setId(UUID.randomUUID());
        existing.setNews(news);
        existing.setPlatform(SocialPlatform.FACEBOOK);
        existing.setBody("Anterior");

        when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));
        when(newsSocialContentRepository.findByNewsIdAndPlatform(newsId, SocialPlatform.FACEBOOK))
                .thenReturn(Optional.of(existing));
        when(newsSocialContentRepository.save(any(NewsSocialContent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NewsSocialContentResponse response = service.save(newsId, SocialPlatform.FACEBOOK, new NewsSocialContentRequest(
                null,
                "Nuevo texto",
                null,
                "Comentar",
                null
        ));

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.body()).isEqualTo("Nuevo texto");
        assertThat(response.callToAction()).isEqualTo("Comentar");
    }

    @Test
    void shouldListSocialContentsByNews() {
        UUID newsId = UUID.randomUUID();
        News news = news(newsId);
        NewsSocialContent content = new NewsSocialContent();
        content.setId(UUID.randomUUID());
        content.setNews(news);
        content.setPlatform(SocialPlatform.INSTAGRAM);
        content.setCaption("Caption");

        when(newsRepository.existsById(newsId)).thenReturn(true);
        when(newsSocialContentRepository.findByNewsIdOrderByPlatformAsc(newsId)).thenReturn(List.of(content));

        List<NewsSocialContentResponse> response = service.listByNews(newsId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).platform()).isEqualTo(SocialPlatform.INSTAGRAM);
    }

    private News news(UUID id) {
        News news = new News();
        news.setId(id);
        news.setTitle("Novedad");
        news.setSummary("Resumen");
        news.setContent("Contenido");
        news.setSlug("novedad");
        return news;
    }
}
