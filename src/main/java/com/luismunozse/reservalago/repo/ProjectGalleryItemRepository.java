package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.ProjectGalleryItem;
import com.luismunozse.reservalago.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectGalleryItemRepository extends JpaRepository<ProjectGalleryItem, UUID> {
    List<ProjectGalleryItem> findByProjectIdOrderBySortOrderAscCreatedAtAsc(UUID projectId);

    Optional<ProjectGalleryItem> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("""
            select count(item) > 0
            from ProjectGalleryItem item
            where item.mediaAsset.id = :mediaAssetId
              and item.project.status = :status
            """)
    boolean existsPublishedAssociation(@Param("mediaAssetId") UUID mediaAssetId, @Param("status") ProjectStatus status);
}
