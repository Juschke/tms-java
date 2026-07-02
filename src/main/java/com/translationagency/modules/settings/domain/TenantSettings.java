package com.translationagency.modules.settings.domain;

import com.translationagency.modules.tenant.domain.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettings {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "company_name")
    private String companyName;

    private String street;

    private String zip;

    private String city;

    private String country = "DE";

    private String phone;

    private String email;

    private String website;

    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "vat_id")
    private String vatId;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency = "EUR";

    @Column(name = "vat_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal vatPercent = BigDecimal.valueOf(19.00);

    @Column(name = "default_payment_terms_days", nullable = false)
    private int defaultPaymentTermsDays = 14;

    @Column(name = "default_quote_validity_days", nullable = false)
    private int defaultQuoteValidityDays = 14;

    @Column(name = "default_delivery_method", nullable = false, length = 50)
    private String defaultDeliveryMethod = "EMAIL";

    @Enumerated(EnumType.STRING)
    @Column(name = "accounting_scheme", nullable = false, length = 10)
    private AccountingScheme accountingScheme = AccountingScheme.SKR03;

    @Column(name = "email_sender_name")
    private String emailSenderName;

    @Column(name = "email_sender_address")
    private String emailSenderAddress;

    @Column(name = "bank_name")
    private String bankName;

    private String iban;

    private String bic;

    @Column(name = "quote_footer", columnDefinition = "TEXT")
    private String quoteFooter;

    @Column(name = "invoice_footer", columnDefinition = "TEXT")
    private String invoiceFooter;

    @Column(name = "order_footer", columnDefinition = "TEXT")
    private String orderFooter;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
