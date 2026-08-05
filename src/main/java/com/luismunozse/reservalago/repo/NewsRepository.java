package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.News;
import com.luismunozse.reservalago.model.NewsStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    Optional<News> findBySlugAndStatus(String slug, NewsStatus status);
    List<News> findByStatusOrderByPublishedAtDescCreatedAtDesc(NewsStatus status);
    List<News> findAllByOrderByCreatedAtDesc();
    boolean existsByImageAssetIdAndStatus(UUID imageAssetId, NewsStatus status);
    boolean existsByVideoAssetIdAndStatus(UUID videoAssetId, NewsStatus status);
}
