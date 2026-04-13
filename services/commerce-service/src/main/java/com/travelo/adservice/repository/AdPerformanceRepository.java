package com.travelo.adservice.repository;

import com.travelo.adservice.entity.AdPerformance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdPerformanceRepository extends JpaRepository<AdPerformance, Long> {
    
    @Query("SELECT ap FROM AdPerformance ap WHERE ap.ad.id = :adId ORDER BY ap.date DESC")
    Page<AdPerformance> findByAdId(UUID adId, Pageable pageable);
    
    @Query("SELECT ap FROM AdPerformance ap WHERE ap.ad.id = :adId " +
           "AND ap.date >= :startDate AND ap.date <= :endDate ORDER BY ap.date DESC")
    List<AdPerformance> findByAdIdAndDateRange(UUID adId, OffsetDateTime startDate, OffsetDateTime endDate);
    
    @Query("SELECT ap FROM AdPerformance ap WHERE ap.ad.id = :adId AND ap.date = :date")
    Optional<AdPerformance> findByAdIdAndDate(UUID adId, OffsetDateTime date);
    
    @Query("SELECT SUM(ap.impressions), SUM(ap.clicks), SUM(ap.conversions), SUM(ap.spend), SUM(ap.revenue) " +
           "FROM AdPerformance ap WHERE ap.ad.id = :adId " +
           "AND ap.date >= :startDate AND ap.date <= :endDate")
    Object[] getAggregatedMetrics(UUID adId, OffsetDateTime startDate, OffsetDateTime endDate);
}

