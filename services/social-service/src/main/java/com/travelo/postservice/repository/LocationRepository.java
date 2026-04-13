package com.travelo.postservice.repository;

import com.travelo.postservice.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {
    Optional<Location> findByPlaceId(String placeId);
    
    List<Location> findByIsActiveTrue();
    
    @Query("SELECT l FROM Location l WHERE l.isActive = true AND " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.address) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY l.name")
    List<Location> searchLocations(String query);
    
    @Query("SELECT l FROM Location l WHERE l.isActive = true AND " +
           "l.latitude IS NOT NULL AND l.longitude IS NOT NULL AND " +
           "SQRT(POWER(l.latitude - :lat, 2) + POWER(l.longitude - :lon, 2)) < :radius " +
           "ORDER BY SQRT(POWER(l.latitude - :lat, 2) + POWER(l.longitude - :lon, 2))")
    Page<Location> findNearbyLocations(BigDecimal lat, BigDecimal lon, double radius, Pageable pageable);
}

