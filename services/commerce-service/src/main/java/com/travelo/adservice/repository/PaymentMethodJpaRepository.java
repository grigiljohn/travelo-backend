package com.travelo.adservice.repository;

import com.travelo.adservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentMethodJpaRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByBillingProfileIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(
            UUID billingProfileId);
}
