package com.translationagency.modules.order.domain;

import com.translationagency.modules.pricing.domain.Language;
import com.translationagency.modules.partner.domain.Partner;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "translation_order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TranslationOrderItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private TranslationOrder order;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false)
    private String unit = "FLAT_RATE"; // WORDS, PAGES, HOURS, FLAT_RATE

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    // Extended Teilauftrag Info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_language_id")
    private Language sourceLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_language_id")
    private Language targetLanguage;

    private String activity; // e.g. "Übersetzen"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_partner_id")
    private Partner assignedPartner;

    private String status = "CREATED"; // CREATED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "page_count", precision = 10, scale = 2)
    private BigDecimal pageCount;

    @Column(name = "price_per_line", precision = 10, scale = 2)
    private BigDecimal pricePerLine;

    @Column(name = "surcharge_or_discount", precision = 10, scale = 2)
    private BigDecimal surchargeOrDiscount;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
