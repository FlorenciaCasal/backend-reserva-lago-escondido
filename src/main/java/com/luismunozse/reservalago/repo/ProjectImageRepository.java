package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.ProjectImage;
import com.luismunozse.reservalago.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, UUID> {
    List<ProjectImage> findByProjectIdOrderBySortOrderAscCreatedAtAsc(UUID projectId);
    Optional<ProjectImage> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("select count(i) > 0 from ProjectImage i where i.mediaAsset.id = :mediaAssetId and i.project.status = :status")
    boolean existsPublishedAssociation(
            @Param("mediaAssetId") UUID mediaAssetId,
            @Param("status") ProjectStatus status
    );
}