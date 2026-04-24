package com.travelo.adservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.travelo.adservice.config.StripeProperties;
import com.travelo.adservice.entity.BusinessAccount;
import com.travelo.adservice.entity.Payment;
import com.travelo.adservice.entity.Wallet;
import com.travelo.adservice.repository.BusinessAccountRepository;
import com.travelo.adservice.repository.PaymentJpaRepository;
import com.travelo.adservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Verifies and applies Stripe webhooks (wallet top-up completion, failures).
 */
@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties stripeProperties;
    private final PaymentJpaRepository paymentJpaRepository;
    private final WalletRepository walletRepository;
    private final BusinessAccountRepository businessAccountRepository;

    public StripeWebhookService(
            StripeProperties stripeProperties,
            PaymentJpaRepository paymentJpaRepository,
            WalletRepository walletRepository,
            BusinessAccountRepository businessAccountRepository) {
        this.stripeProperties = stripeProperties;
        this.paymentJpaRepository = paymentJpaRepository;
        this.walletRepository = walletRepository;
        this.businessAccountRepository = businessAccountRepository;
    }

    public Event parseEvent(String payload, String sigHeader) throws StripeException {
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET is not configured");
        }
        return Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
    }

    public void processEventPayload(String payload, String sigHeader) throws StripeException {
        if (!stripeProperties.isReady()) {
            return;
        }
        Event event = parseEvent(payload, sigHeader);
        if ("payment_intent.succeeded".equals(event.getType())) {
            Optional<StripeObject> o = event.getDataObjectDeserializer().getObject();
            o.filter(PaymentIntent.class::isInstance)
                    .map(PaymentIntent.class::cast)
                    .ifPresent(pi -> onPaymentIntentSucceeded(pi.getId()));
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject()
                    .filter(PaymentIntent.class::isInstance)
                    .map(PaymentIntent.class::cast)
                    .ifPresent(pi -> onPaymentIntentFailed(pi.getId()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "adTransactionManager")
    public void onPaymentIntentSucceeded(String paymentIntentId) {
        log.info("Stripe payment_intent.succeeded: {}", paymentIntentId);
        Optional<Payment> opt = paymentJpaRepository.findByStripePaymentIntentId(paymentIntentId);
        if (opt.isEmpty()) {
            log.warn("No local Payment for stripe id {}", paymentIntentId);
            return;
        }
        Payment p = opt.get();
        if ("COMPLETED".equalsIgnoreCase(p.getStatus())) {
            return;
        }
        p.setStatus("COMPLETED");
        p.setTransactionId(paymentIntentId);
        paymentJpaRepository.save(p);
        creditWalletForTopUp(p);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, transactionManager = "adTransactionManager")
    public void onPaymentIntentFailed(String paymentIntentId) {
        log.warn("Stripe payment_intent failed: {}", paymentIntentId);
        paymentJpaRepository
                .findByStripePaymentIntentId(paymentIntentId)
                .ifPresent(pay -> {
                    if (!"COMPLETED".equalsIgnoreCase(pay.getStatus())) {
                        pay.setStatus("FAILED");
                    }
                    paymentJpaRepository.save(pay);
                });
    }

    private void creditWalletForTopUp(Payment p) {
        Optional<BusinessAccount> b =
                businessAccountRepository.findByBillingProfileIdAndDeletedAtIsNull(
                        p.getBillingProfileId());
        if (b.isEmpty()) {
            log.error("No business for billing profile {}", p.getBillingProfileId());
            return;
        }
        UUID businessId = b.get().getId();
        Optional<Wallet> w = walletRepository.findByBusinessAccountId(businessId);
        if (w.isEmpty()) {
            log.error("No wallet for business {}", businessId);
            return;
        }
        double add = p.getAmount() != null ? p.getAmount() : 0.0;
        Wallet wallet = w.get();
        double bal = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        wallet.setBalance(bal + add);
        walletRepository.save(wallet);
    }
}
