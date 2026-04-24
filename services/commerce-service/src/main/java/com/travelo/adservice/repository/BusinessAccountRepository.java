package com.travelo.adservice.repository;

import com.travelo.adservice.entity.BusinessAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessAccountRepository extends JpaRepository<BusinessAccount, UUID> {
    List<BusinessAccount> findByUserIdAndDeletedAtIsNullOrderByIsDefaultDesc(UUID userId);

    Optional<BusinessAccount> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT b FROM BusinessAccount b WHERE b.billingProfileId = :bp AND b.deletedAt IS NULL")
    Optional<BusinessAccount> findByBillingProfileIdAndDeletedAtIsNull(@Param("bp") UUID billingProfileId);
}
