package com.travelo.adservice.repository;

import com.travelo.adservice.entity.AdGroup;
import com.travelo.adservice.entity.AdGroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdGroupRepository extends JpaRepository<AdGroup, UUID> {
    
    @Query("SELECT ag FROM AdGroup ag WHERE ag.campaign.id = :campaignId AND ag.deletedAt IS NULL")
    Page<AdGroup> findByCampaignId(UUID campaignId, Pageable pageable);
    
    @Query("SELECT ag FROM AdGroup ag WHERE ag.campaign.id = :campaignId AND ag.status = :status AND ag.deletedAt IS NULL")
    Page<AdGroup> findByCampaignIdAndStatus(UUID campaignId, AdGroupStatus status, Pageable pageable);
    
    @Query("SELECT ag FROM AdGroup ag WHERE ag.campaign.id = :campaignId AND ag.deletedAt IS NULL")
    List<AdGroup> findAllByCampaignId(UUID campaignId);
    
    @Query("SELECT ag FROM AdGroup ag WHERE ag.campaign.id = :campaignId AND ag.status = :status AND ag.deletedAt IS NULL")
    List<AdGroup> findAllByCampaignIdAndStatus(UUID campaignId, AdGroupStatus status);
}

