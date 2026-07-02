package com.luismunozse.reservalago.repo;

import com.luismunozse.reservalago.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
}