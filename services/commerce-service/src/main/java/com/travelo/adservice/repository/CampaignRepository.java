package com.travelo.adservice.repository;

import com.travelo.adservice.entity.Campaign;
import com.travelo.adservice.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    
    @Query("SELECT c FROM Campaign c WHERE c.businessAccountId = :businessAccountId AND c.deletedAt IS NULL")
    Page<Campaign> findByBusinessAccountId(UUID businessAccountId, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.businessAccountId = :businessAccountId AND c.status = :status AND c.deletedAt IS NULL")
    Page<Campaign> findByBusinessAccountIdAndStatus(UUID businessAccountId, CampaignStatus status, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.businessAccountId = :businessAccountId " +
           "AND c.deletedAt IS NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Campaign> findByBusinessAccountIdAndSearch(UUID businessAccountId, String search, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.businessAccountId = :businessAccountId AND c.status = :status " +
           "AND c.deletedAt IS NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Campaign> findByBusinessAccountIdAndStatusAndSearch(UUID businessAccountId, CampaignStatus status, String search, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.businessAccountId = :businessAccountId AND c.status = :status " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "AND c.deletedAt IS NULL")
    List<Campaign> findActiveCampaignsByBusinessAccount(UUID businessAccountId, CampaignStatus status, OffsetDateTime now);
    
    @Query("SELECT c FROM Campaign c WHERE c.status = :status " +
           "AND (c.startDate IS NULL OR c.startDate <= :now) AND (c.endDate IS NULL OR c.endDate >= :now) " +
           "AND c.deletedAt IS NULL")
    List<Campaign> findActiveCampaigns(CampaignStatus status, OffsetDateTime now);
}

