package com.translationagency.modules.partner.domain;

import com.translationagency.modules.pricing.domain.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "partner_language_pair")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartnerLanguagePair {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_language_id")
    private Language sourceLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_language_id")
    private Language targetLanguage;

    @Column(nullable = false)
    private String activity;

    @Column(name = "subject_area")
    private String subjectArea;

    @Column(name = "price_per_line", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerLine = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
