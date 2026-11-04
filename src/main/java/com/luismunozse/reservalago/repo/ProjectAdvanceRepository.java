package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.ProjectAdvance;
import com.luismunozse.reservalago.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectAdvanceRepository extends JpaRepository<ProjectAdvance, UUID> {
    List<ProjectAdvance> findByProjectIdOrderByAdvanceDateAscCreatedAtAscIdAsc(UUID projectId);
    Optional<ProjectAdvance> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("select count(a) > 0 from ProjectAdvance a where a.imageAsset.id = :mediaAssetId and a.project.status = :status")
    boolean existsPublishedImageAssociation(@Param("mediaAssetId") UUID mediaAssetId, @Param("status") ProjectStatus status);
    @Query("select count(a) > 0 from ProjectAdvance a where a.videoAsset.id = :mediaAssetId and a.project.status = :status")
    boolean existsPublishedVideoAssociation(@Param("mediaAssetId") UUID mediaAssetId, @Param("status") ProjectStatus status);
}
