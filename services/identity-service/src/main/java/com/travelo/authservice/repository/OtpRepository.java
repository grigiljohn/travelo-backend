package com.travelo.authservice.repository;

import com.travelo.authservice.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {
    
    Optional<Otp> findByEmailAndOtpAndIsUsedFalse(String email, String otp);
    
    List<Otp> findByEmailAndIsUsedFalse(String email);
    
    @Query("SELECT o FROM Otp o WHERE o.email = :email AND o.isUsed = false AND o.expiresAt > :now ORDER BY o.createdAt DESC")
    List<Otp> findActiveOtpsByEmail(@Param("email") String email, @Param("now") OffsetDateTime now);
    
    @Query("SELECT COUNT(o) FROM Otp o WHERE o.email = :email AND o.createdAt > :since")
    long countByEmailAndCreatedAtAfter(@Param("email") String email, @Param("since") OffsetDateTime since);
}

