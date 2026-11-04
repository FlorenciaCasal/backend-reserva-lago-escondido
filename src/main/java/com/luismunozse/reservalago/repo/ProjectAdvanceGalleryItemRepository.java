package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.ProjectAdvanceGalleryItem;
import com.luismunozse.reservalago.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectAdvanceGalleryItemRepository extends JpaRepository<ProjectAdvanceGalleryItem, UUID> {
    List<ProjectAdvanceGalleryItem> findByAdvanceIdOrderBySortOrderAscCreatedAtAsc(UUID advanceId);

    Optional<ProjectAdvanceGalleryItem> findByIdAndAdvanceId(UUID id, UUID advanceId);

    @Query("""
            select count(item) > 0
            from ProjectAdvanceGalleryItem item
            where item.mediaAsset.id = :mediaAssetId
              and item.advance.project.status = :status
            """)
    boolean existsPublishedAssociation(@Param("mediaAssetId") UUID mediaAssetId, @Param("status") ProjectStatus status);
}
