package com.luismunozse.reservalago.service;

import com.luismunozse.reservalago.dto.CreateNewsRequest;
import com.luismunozse.reservalago.dto.NewsResponse;
import com.luismunozse.reservalago.dto.UpdateNewsRequest;
import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsStatus;
import com.luismunozse.reservalago.repo.NewsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsImageService newsImageService;

    @Mock
    private MediaAssetService mediaAssetService;

    @InjectMocks
    private NewsService newsService;

    @Test
    void shouldCreateDraftNewsByDefault() {
        when(newsRepository.existsBySlug("titulo-de-prueba")).thenReturn(false);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(UUID.randomUUID());
            return news;
        });
        when(newsImageService.listResponses(any())).thenReturn(List.of());

        NewsResponse response = newsService.create(new CreateNewsRequest(
                "Titulo de prueba",
                "Resumen de prueba",
                "Contenido de prueba",
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(response.status()).isEqualTo(NewsStatus.DRAFT);
        assertThat(response.slug()).isEqualTo("titulo-de-prueba");
        assertThat(response.publishedAt()).isNull();
    }

    @Test
    void shouldSetPublishedAtWhenCreatingPublishedNews() {
        when(newsRepository.existsBySlug("novedad-publicada")).thenReturn(false);
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> {
            News news = invocation.getArgument(0);
            news.setId(UUID.randomUUID());
            return news;
        });
        when(newsImageService.listResponses(any())).thenReturn(List.of());

        NewsResponse response = newsService.create(new CreateNewsRequest(
                "Novedad publicada",
                "Resumen",
                "Contenido",
                null,
                null,
                null,
                null,
                null,
                NewsStatus.PUBLISHED
        ));

        assertThat(response.status()).isEqualTo(NewsStatus.PUBLISHED);
        assertThat(response.publishedAt()).isNotNull();
    }

    @Test
    void shouldRejectPublishedToDraftTransitionOnUpdate() {
        UUID id = UUID.randomUUID();
        News news = existingNews(id, NewsStatus.PUBLISHED);
        when(newsRepository.findById(id)).thenReturn(Optional.of(news));
        when(newsRepository.existsBySlugAndIdNot("novedad-publicada", id)).thenReturn(false);

        UpdateNewsRequest request = new UpdateNewsRequest(
                "Novedad publicada",
                "Resumen",
                "Contenido",
                "novedad-publicada",
                null,
                null,
                null,
                null,
                NewsStatus.DRAFT
        );

        assertThatThrownBy(() -> newsService.update(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no puede volver a borrador");

        verify(newsRepository, never()).save(any());
    }

    @Test
    void shouldArchiveAndRepublishNews() {
        UUID id = UUID.randomUUID();
        News news = existingNews(id, NewsStatus.ARCHIVED);
        when(newsRepository.findById(id)).thenReturn(Optional.of(news));
        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(newsImageService.listResponses(id)).thenReturn(List.of());

        NewsResponse response = newsService.publish(id);

        assertThat(response.status()).isEqualTo(NewsStatus.PUBLISHED);
        assertThat(response.archivedAt()).isNull();
    }

    @Test
    void shouldListOnlyPublishedNewsOrderedByRepository() {
        News published = existingNews(UUID.randomUUID(), NewsStatus.PUBLISHED);
        when(newsRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(NewsStatus.PUBLISHED))
                .thenReturn(List.of(published));
        when(newsImageService.listResponses(published.getId())).thenReturn(List.of());

        List<NewsResponse> response = newsService.listPublished();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(NewsStatus.PUBLISHED);
    }

    private News existingNews(UUID id, NewsStatus status) {
        News news = new News();
        news.setId(id);
        news.setTitle("Novedad publicada");
        news.setSummary("Resumen");
        news.setContent("Contenido");
        news.setSlug("novedad-publicada");
        news.setStatus(status);
        return news;
    }
}
