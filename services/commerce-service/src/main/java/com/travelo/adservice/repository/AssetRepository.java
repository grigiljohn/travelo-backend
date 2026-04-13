package com.travelo.adservice.repository;

import com.travelo.adservice.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    @Query("SELECT a FROM Asset a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Asset> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

    @Query("SELECT a FROM Asset a WHERE a.businessAccountId = :businessAccountId AND a.deletedAt IS NULL")
    List<Asset> findByBusinessAccountIdAndDeletedAtIsNull(@Param("businessAccountId") UUID businessAccountId);

    @Query("SELECT a FROM Asset a WHERE a.businessAccountId = :businessAccountId AND a.type = :type AND a.deletedAt IS NULL")
    List<Asset> findByBusinessAccountIdAndTypeAndDeletedAtIsNull(
            @Param("businessAccountId") UUID businessAccountId,
            @Param("type") com.travelo.adservice.entity.enums.AssetType type
    );
}

