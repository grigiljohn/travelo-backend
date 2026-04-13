package com.travelo.authservice.repository;

import com.travelo.authservice.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByUserIdAndIsUsedFalse(UUID userId);
    
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.userId = :userId AND p.isUsed = false")
    void invalidateAllTokensForUser(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") OffsetDateTime now);
}

