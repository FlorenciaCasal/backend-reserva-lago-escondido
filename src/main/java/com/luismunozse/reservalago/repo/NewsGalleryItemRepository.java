package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.NewsGalleryItem;
import com.luismunozse.reservalago.model.NewsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsGalleryItemRepository extends JpaRepository<NewsGalleryItem, UUID> {
    List<NewsGalleryItem> findByNewsIdOrderBySortOrderAscCreatedAtAsc(UUID newsId);

    Optional<NewsGalleryItem> findByIdAndNewsId(UUID id, UUID newsId);

    @Query("""
            select count(item) > 0
            from NewsGalleryItem item
            where item.mediaAsset.id = :mediaAssetId
              and item.news.status = :status
            """)
    boolean existsPublishedAssociation(@Param("mediaAssetId") UUID mediaAssetId, @Param("status") NewsStatus status);
}
