package com.travelo.adservice.repository;

import com.travelo.adservice.entity.Ad;
import com.travelo.adservice.entity.AdStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID> {
    
    @Query("SELECT a FROM Ad a WHERE a.adGroup.id = :adGroupId AND a.deletedAt IS NULL")
    Page<Ad> findByAdGroupId(UUID adGroupId, Pageable pageable);
    
    @Query("SELECT a FROM Ad a WHERE a.adGroup.id = :adGroupId AND a.status = :status AND a.deletedAt IS NULL")
    Page<Ad> findByAdGroupIdAndStatus(UUID adGroupId, AdStatus status, Pageable pageable);
    
    @Query("SELECT a FROM Ad a WHERE a.status = :status AND a.deletedAt IS NULL")
    List<Ad> findActiveAds(AdStatus status);
    
    @Query("SELECT a FROM Ad a WHERE a.adGroup.id = :adGroupId AND a.status = :status AND a.deletedAt IS NULL")
    List<Ad> findByAdGroupIdAndStatus(UUID adGroupId, AdStatus status);
    
    @Query("SELECT a FROM Ad a WHERE a.adGroup.campaign.id = :campaignId AND a.status = :status AND a.deletedAt IS NULL")
    List<Ad> findByCampaignIdAndStatus(UUID campaignId, AdStatus status);
    
    @Query("SELECT a FROM Ad a WHERE a.adGroup.campaign.id = :campaignId AND a.deletedAt IS NULL")
    Page<Ad> findByCampaignId(UUID campaignId, Pageable pageable);
}

