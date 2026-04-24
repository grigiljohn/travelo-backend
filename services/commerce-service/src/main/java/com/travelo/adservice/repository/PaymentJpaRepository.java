package com.travelo.adservice.repository;

import com.travelo.adservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBillingProfileIdOrderByPaymentDateDesc(UUID billingProfileId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
