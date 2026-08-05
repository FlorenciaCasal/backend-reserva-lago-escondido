package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.NewsImage;
import com.luismunozse.reservalago.model.NewsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsImageRepository extends JpaRepository<NewsImage, UUID> {
    List<NewsImage> findByNewsIdOrderBySortOrderAscCreatedAtAsc(UUID newsId);
    Optional<NewsImage> findByIdAndNewsId(UUID id, UUID newsId);

    @Query("select count(i) > 0 from NewsImage i where i.mediaAsset.id = :mediaAssetId and i.news.status = :status")
    boolean existsPublishedAssociation(
            @Param("mediaAssetId") UUID mediaAssetId,
            @Param("status") NewsStatus status
    );
}
