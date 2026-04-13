package com.travelo.shopservice.repository;

import com.travelo.shopservice.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {
    
    Optional<Shop> findByBusinessAccountId(UUID businessAccountId);
    
    Page<Shop> findByIsActiveTrue(Pageable pageable);
    
    Page<Shop> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    boolean existsByBusinessAccountId(UUID businessAccountId);
}

