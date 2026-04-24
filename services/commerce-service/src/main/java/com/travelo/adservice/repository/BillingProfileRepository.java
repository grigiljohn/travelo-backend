package com.travelo.adservice.repository;

import com.travelo.adservice.entity.BillingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingProfileRepository extends JpaRepository<BillingProfile, UUID> {
    Optional<BillingProfile> findByBusinessAccountId(UUID businessAccountId);
}
