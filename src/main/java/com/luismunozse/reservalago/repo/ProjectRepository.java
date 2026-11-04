package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.Project;
import com.luismunozse.reservalago.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    Optional<Project> findBySlug(String slug);
    Optional<Project> findBySlugAndStatus(String slug, ProjectStatus status);
    List<Project> findByStatusOrderByFeaturedDescPublishedAtDescCreatedAtDesc(ProjectStatus status);
    List<Project> findAllByOrderByFeaturedDescCreatedAtDesc();
    boolean existsByImageAssetIdAndStatus(UUID imageAssetId, ProjectStatus status);
    boolean existsByVideoAssetIdAndStatus(UUID videoAssetId, ProjectStatus status);
}
