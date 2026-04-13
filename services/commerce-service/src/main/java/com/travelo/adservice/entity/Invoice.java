package com.travelo.adservice.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_billing_profile_id", columnList = "billing_profile_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_invoice_date", columnList = "invoice_date")
})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "billing_profile_id", nullable = false)
    private UUID billingProfileId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false)
    private OffsetDateTime invoiceDate;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Column(nullable = false)
    private Double total;

    @Column(length = 10)
    private String currency = "USD";

    @Column(length = 20)
    private String status = "PENDING"; // PENDING, PAID, OVERDUE, CANCELLED

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.invoiceDate == null) {
            this.invoiceDate = OffsetDateTime.now();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBillingProfileId() {
        return billingProfileId;
    }

    public void setBillingProfileId(UUID billingProfileId) {
        this.billingProfileId = billingProfileId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public OffsetDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(OffsetDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

