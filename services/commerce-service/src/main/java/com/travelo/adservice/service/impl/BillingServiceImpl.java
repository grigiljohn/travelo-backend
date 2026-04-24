package com.travelo.adservice.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SetupIntentCreateParams;
import com.travelo.adservice.config.StripeProperties;
import com.travelo.adservice.dto.billing.*;
import com.travelo.adservice.entity.BusinessAccount;
import com.travelo.adservice.entity.BillingProfile;
import com.travelo.adservice.entity.Payment;
import com.travelo.adservice.entity.PaymentMethod;
import com.travelo.adservice.entity.Wallet;
import com.travelo.adservice.repository.BillingProfileRepository;
import com.travelo.adservice.repository.BusinessAccountRepository;
import com.travelo.adservice.repository.PaymentJpaRepository;
import com.travelo.adservice.repository.PaymentMethodJpaRepository;
import com.travelo.adservice.repository.WalletRepository;
import com.travelo.adservice.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillingServiceImpl implements BillingService {

    public static final String METADATA_TYPE = "type";
    public static final String TYPE_WALLET_TOPUP = "wallet_topup";
    public static final String METADATA_PAYMENT_ID = "paymentId";
    public static final String METADATA_BUSINESS = "businessAccountId";

    private static final double MIN_WALLET_USD = 0.5;

    private final StripeProperties stripeProperties;
    private final BusinessAccountRepository businessAccountRepository;
    private final BillingProfileRepository billingProfileRepository;
    private final WalletRepository walletRepository;
    private final PaymentMethodJpaRepository paymentMethodJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;

    public BillingServiceImpl(
            StripeProperties stripeProperties,
            BusinessAccountRepository businessAccountRepository,
            BillingProfileRepository billingProfileRepository,
            WalletRepository walletRepository,
            PaymentMethodJpaRepository paymentMethodJpaRepository,
            PaymentJpaRepository paymentJpaRepository) {
        this.stripeProperties = stripeProperties;
        this.businessAccountRepository = businessAccountRepository;
        this.billingProfileRepository = billingProfileRepository;
        this.walletRepository = walletRepository;
        this.paymentMethodJpaRepository = paymentMethodJpaRepository;
        this.paymentJpaRepository = paymentJpaRepository;
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public BillingSummaryResponse getSummary(UUID businessAccountId) {
        BusinessAccount acc = requireBusiness(businessAccountId);
        BillingProfile profile = getOrCreateBillingAndWalletNoStripe(acc);
        Wallet w = walletRepository.findByBusinessAccountId(businessAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No wallet row"));

        List<PaymentMethod> methods = paymentMethodJpaRepository
                .findByBillingProfileIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(profile.getId());
        List<PaymentMethodResponse> pms = methods.stream()
                .map(this::toPmResponse)
                .collect(Collectors.toList());

        List<Payment> all = paymentJpaRepository.findByBillingProfileIdOrderByPaymentDateDesc(profile.getId());
        List<InvoiceItemResponse> inv = all.stream()
                .limit(100)
                .map(this::toInvoiceItem)
                .toList();

        return BillingSummaryResponse.of(
                profile.getId(),
                acc.getId(),
                w.getBalance() != null ? w.getBalance() : 0.0,
                w.getCurrency() != null ? w.getCurrency() : "USD",
                Boolean.TRUE.equals(w.getAutoPayEnabled()),
                w.getAutoPayThreshold(),
                profile.getInvoiceProfile() != null ? profile.getInvoiceProfile() : Map.of(),
                pms,
                new ArrayList<>(inv));
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager", readOnly = true)
    public BusinessAccount requireBusinessAccount(UUID businessAccountId) {
        return requireBusiness(businessAccountId);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public SetupIntentResponse createSetupIntent(UUID businessAccountId) throws StripeException {
        requireStripe();
        BusinessAccount acc = requireBusiness(businessAccountId);
        BillingProfile profile = getOrCreateBillingAndWalletWithStripeCustomer(acc);

        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .addPaymentMethodType("card")
                .setCustomer(profile.getStripeCustomerId())
                .build();
        SetupIntent si = SetupIntent.create(params);
        return new SetupIntentResponse(
                si.getClientSecret(),
                profile.getStripeCustomerId(),
                effectivePublishableKey());
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public PaymentMethodResponse savePaymentMethod(UUID businessAccountId, SavePaymentMethodRequest request)
            throws StripeException {
        requireStripe();
        BusinessAccount acc = requireBusiness(businessAccountId);
        BillingProfile profile = getOrCreateBillingAndWalletWithStripeCustomer(acc);

        com.stripe.model.PaymentMethod spm = com.stripe.model.PaymentMethod.retrieve(request.paymentMethodId());
        if (spm.getCustomer() == null
                || !profile.getStripeCustomerId().equals(spm.getCustomer())) {
            spm = spm.attach(
                    PaymentMethodAttachParams.builder()
                            .setCustomer(profile.getStripeCustomerId())
                            .build());
        }

        var alreadyHave = new ArrayList<>(
                paymentMethodJpaRepository.findByBillingProfileIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(
                        profile.getId()));
        int existingCount = alreadyHave.size();
        for (PaymentMethod p : alreadyHave) {
            p.setIsDefault(false);
        }
        if (!alreadyHave.isEmpty()) {
            paymentMethodJpaRepository.saveAll(alreadyHave);
        }

        com.stripe.model.PaymentMethod.Card card = spm.getCard();

        PaymentMethod entity = new PaymentMethod();
        entity.setBillingProfileId(profile.getId());
        entity.setType("CARD");
        if (card != null) {
            entity.setCardLastFour(card.getLast4());
            if (card.getBrand() != null) {
                entity.setCardBrand(card.getBrand().toUpperCase());
            }
            Long exM = card.getExpMonth();
            Long exY = card.getExpYear();
            if (exM != null) {
                entity.setExpiryMonth(exM.intValue());
            }
            if (exY != null) {
                entity.setExpiryYear(exY.intValue());
            }
        } else if (spm.getType() != null) {
            entity.setType(spm.getType().toUpperCase());
        }
        if (spm.getBillingDetails() != null) {
            entity.setNameOnCard(spm.getBillingDetails().getName());
        }
        entity.setStripePaymentMethodId(spm.getId());
        boolean isDefault = Boolean.TRUE.equals(request.setAsDefault()) || existingCount == 0;
        entity.setIsDefault(isDefault);
        entity = paymentMethodJpaRepository.save(entity);
        if (isDefault) {
            clearDefaultExcept(profile.getId(), entity.getId());
        }
        return toPmResponse(
                paymentMethodJpaRepository.findById(entity.getId()).orElseThrow());
    }

    private void clearDefaultExcept(UUID billingProfileId, UUID keepId) {
        for (PaymentMethod p : paymentMethodJpaRepository
                .findByBillingProfileIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(billingProfileId)) {
            p.setIsDefault(keepId.equals(p.getId()));
        }
        paymentMethodJpaRepository.saveAll(
                paymentMethodJpaRepository.findByBillingProfileIdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtDesc(
                        billingProfileId));
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public PaymentIntentResponse createWalletTopUpIntent(UUID businessAccountId, WalletTopUpRequest request)
            throws StripeException {
        requireStripe();
        if ("usd".equalsIgnoreCase(request.currency()) && request.amount() < MIN_WALLET_USD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Amount must be at least $" + MIN_WALLET_USD + " USD");
        }
        BusinessAccount acc = requireBusiness(businessAccountId);
        BillingProfile profile = getOrCreateBillingAndWalletWithStripeCustomer(acc);
        long cents = Math.round(request.amount() * 100);
        if (cents < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount");
        }

        Payment p = new Payment();
        p.setBillingProfileId(profile.getId());
        p.setAmount(request.amount());
        p.setCurrency(request.currency().toUpperCase());
        p.setStatus("PENDING");
        p.setDescription("Wallet top-up");
        p = paymentJpaRepository.save(p);

        PaymentIntentCreateParams.Builder b = PaymentIntentCreateParams.builder()
                .setAmount(cents)
                .setCurrency(request.currency().toLowerCase())
                .setCustomer(profile.getStripeCustomerId())
                .putMetadata(METADATA_TYPE, TYPE_WALLET_TOPUP)
                .putMetadata(METADATA_PAYMENT_ID, p.getId().toString())
                .putMetadata(METADATA_BUSINESS, businessAccountId.toString())
                .setDescription("Wallet top-up");
        if (request.internalPaymentMethodId() != null) {
            PaymentMethod pm = paymentMethodJpaRepository.findById(request.internalPaymentMethodId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found"));
            if (!pm.getBillingProfileId().equals(profile.getId()) || pm.getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment method");
            }
            p.setPaymentMethodId(pm.getId());
            b.setPaymentMethod(pm.getStripePaymentMethodId())
                    .setConfirm(false);
        } else {
            b.setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build());
        }
        PaymentIntent pi = PaymentIntent.create(b.build());
        p.setStripePaymentIntentId(pi.getId());
        paymentJpaRepository.save(p);
        return new PaymentIntentResponse(pi.getClientSecret(), p.getId());
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public void updateAutoPay(UUID businessAccountId, AutoPayRequest request) {
        BusinessAccount acc = requireBusiness(businessAccountId);
        BillingProfile profile = getOrCreateBillingAndWalletNoStripe(acc);
        Wallet w = walletRepository.findByBusinessAccountId(businessAccountId)
                .orElseThrow();
        w.setAutoPayEnabled(Boolean.TRUE.equals(request.enabled()));
        w.setAutoPayThreshold(request.threshold());
        if (request.internalPaymentMethodId() != null) {
            var pm = paymentMethodJpaRepository.findById(request.internalPaymentMethodId());
            if (pm.isEmpty()
                    || !pm.get().getBillingProfileId().equals(profile.getId())
                    || pm.get().getDeletedAt() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment method");
            }
            w.setAutoPayPaymentMethodId(request.internalPaymentMethodId());
        }
        if (!Boolean.TRUE.equals(request.enabled())) {
            w.setAutoPayPaymentMethodId(null);
        }
        walletRepository.save(w);
    }

    @Override
    @Transactional(transactionManager = "adTransactionManager")
    public void removePaymentMethod(UUID businessAccountId, UUID localPaymentMethodId) throws StripeException {
        requireStripe();
        BusinessAccount acc = requireBusiness(businessAccountId);
        BillingProfile profile = getOrCreateBillingAndWalletNoStripe(acc);
        Optional<PaymentMethod> pm = paymentMethodJpaRepository.findById(localPaymentMethodId);
        if (pm.isEmpty() || !pm.get().getBillingProfileId().equals(profile.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment method not found");
        }
        if (pm.get().getDeletedAt() != null) {
            return;
        }
        if (pm.get().getStripePaymentMethodId() != null) {
            try {
                com.stripe.model.PaymentMethod.retrieve(pm.get().getStripePaymentMethodId()).detach();
            } catch (Exception ignored) {
                // already detached
            }
        }
        pm.get().setDeletedAt(OffsetDateTime.now());
        pm.get().setIsDefault(false);
        paymentMethodJpaRepository.save(pm.get());
    }

    private void requireStripe() {
        if (!stripeProperties.isReady()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not configured (STRIPE_SECRET_KEY)");
        }
    }

    private String effectivePublishableKey() {
        return stripeProperties.getPublishableKey() != null
                ? stripeProperties.getPublishableKey()
                : "";
    }

    private BusinessAccount requireBusiness(UUID id) {
        return businessAccountRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business account not found"));
    }

    private BillingProfile getOrCreateBillingAndWalletNoStripe(BusinessAccount acc) {
        if (acc.getBillingProfileId() == null) {
            BillingProfile bp = new BillingProfile();
            bp.setBusinessAccountId(acc.getId());
            billingProfileRepository.save(bp);
            acc.setBillingProfileId(bp.getId());
            businessAccountRepository.save(acc);
            ensureWallet(acc.getId());
            return billingProfileRepository.findById(bp.getId()).orElseThrow();
        }
        return billingProfileRepository
                .findById(acc.getBillingProfileId())
                .orElseGet(() -> {
                    BillingProfile bp = new BillingProfile();
                    bp.setBusinessAccountId(acc.getId());
                    billingProfileRepository.save(bp);
                    acc.setBillingProfileId(bp.getId());
                    businessAccountRepository.save(acc);
                    return bp;
                });
    }

    private BillingProfile getOrCreateBillingAndWalletWithStripeCustomer(BusinessAccount acc)
            throws StripeException {
        requireStripe();
        BillingProfile p = getOrCreateBillingAndWalletNoStripe(acc);
        ensureWallet(acc.getId());
        if (p.getStripeCustomerId() == null) {
            Customer c = Customer.create(
                    CustomerCreateParams.builder()
                            .setName(acc.getName())
                            .putMetadata("business_account_id", acc.getId().toString())
                            .putMetadata("billing_profile_id", p.getId().toString())
                            .build());
            p.setStripeCustomerId(c.getId());
            billingProfileRepository.save(p);
        }
        return p;
    }

    private void ensureWallet(UUID businessId) {
        if (walletRepository.findByBusinessAccountId(businessId).isEmpty()) {
            Wallet w = new Wallet();
            w.setBusinessAccountId(businessId);
            w.setBalance(0.0);
            w.setCurrency("USD");
            walletRepository.save(w);
        }
    }

    private PaymentMethodResponse toPmResponse(PaymentMethod p) {
        return new PaymentMethodResponse(
                p.getId(),
                p.getType() != null && p.getType().equals("CARD") ? "card" : "bank",
                "stripe",
                p.getCardLastFour() != null ? p.getCardLastFour() : "****",
                p.getCardBrand() != null ? p.getCardBrand() : p.getType(),
                p.getExpiryMonth(),
                p.getExpiryYear(),
                Boolean.TRUE.equals(p.getIsDefault()),
                p.getDeletedAt() != null ? "expired" : "active");
    }

    private InvoiceItemResponse toInvoiceItem(Payment p) {
        String s = p.getStatus() == null ? "PENDING" : p.getStatus();
        String status;
        if ("COMPLETED".equalsIgnoreCase(s) || "SUCCEEDED".equalsIgnoreCase(s)) {
            status = "paid";
        } else if ("FAILED".equalsIgnoreCase(s)) {
            status = "overdue";
        } else {
            status = "pending";
        }
        OffsetDateTime pd = p.getPaymentDate() != null ? p.getPaymentDate() : p.getCreatedAt() != null
                ? p.getCreatedAt() : OffsetDateTime.now();
        return new InvoiceItemResponse(
                p.getId(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(pd),
                p.getAmount() != null ? p.getAmount() : 0.0,
                status,
                p.getDescription() != null ? p.getDescription() : "Payment");
    }
}
