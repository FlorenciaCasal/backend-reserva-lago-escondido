package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.ProjectDocument;
import com.luismunozse.reservalago.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, UUID> {
    List<ProjectDocument> findByProjectIdOrderBySortOrderAscCreatedAtAsc(UUID projectId);
    Optional<ProjectDocument> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("select count(d) > 0 from ProjectDocument d where d.mediaAsset.id = :mediaAssetId and d.project.status = :status")
    boolean existsPublishedAssociation(@Param("mediaAssetId") UUID mediaAssetId, @Param("status") ProjectStatus status);
}