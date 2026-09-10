package com.bankops.repository;

import com.bankops.model.Asset;
import com.bankops.model.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findAllByOrderByUpdatedAtDesc();
    boolean existsByAssetCodeIgnoreCase(String assetCode);
    long countByStatus(AssetStatus status);
}
