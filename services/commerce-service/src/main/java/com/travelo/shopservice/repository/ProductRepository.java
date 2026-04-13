package com.travelo.shopservice.repository;

import com.travelo.shopservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    Page<Product> findByShopId(UUID shopId, Pageable pageable);
    
    Page<Product> findByShopIdAndIsAvailableTrue(UUID shopId, Pageable pageable);
    
    Page<Product> findByShopIdAndCategoryAndIsAvailableTrue(UUID shopId, String category, Pageable pageable);
    
    Page<Product> findByShopIdAndIsFeaturedTrueAndIsAvailableTrue(UUID shopId, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.shopId = :shopId AND p.isAvailable = true " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findProductsWithFilters(
            @Param("shopId") UUID shopId,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
    
    long countByShopId(UUID shopId);
    
    long countByShopIdAndIsAvailableTrue(UUID shopId);
}

