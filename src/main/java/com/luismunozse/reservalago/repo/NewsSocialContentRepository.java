package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.NewsSocialContent;
import com.luismunozse.reservalago.model.SocialPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NewsSocialContentRepository extends JpaRepository<NewsSocialContent, UUID> {
    Optional<NewsSocialContent> findByNewsIdAndPlatform(UUID newsId, SocialPlatform platform);
}
