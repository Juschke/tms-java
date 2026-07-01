package com.translationagency.modules.billing.domain;

import com.translationagency.modules.order.domain.TranslationOrder;
import com.translationagency.modules.partner.domain.Partner;
import com.translationagency.modules.tenant.domain.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vendor_invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorInvoice {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private TranslationOrder order;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    private String reference;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "received_at")
    private LocalDate receivedAt;

    @Column(name = "amount_net", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountNet = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.valueOf(19.00);

    @Column(name = "amount_tax", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountTax = BigDecimal.ZERO;

    @Column(name = "amount_gross", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountGross = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(length = 50)
    private String status = "open"; // open, paid, overdue, cancelled

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        syncStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        syncStatus();
    }

    public void syncStatus() {
        if ("cancelled".equalsIgnoreCase(status)) {
            return;
        }
        if (amountGross.compareTo(BigDecimal.ZERO) > 0 && paidAmount.compareTo(amountGross) >= 0) {
            status = "paid";
            return;
        }
        if (dueDate != null && ("open".equalsIgnoreCase(status) || "overdue".equalsIgnoreCase(status))) {
            if (dueDate.isBefore(LocalDate.now())) {
                status = "overdue";
            } else {
                status = "open";
            }
        }
    }
}
