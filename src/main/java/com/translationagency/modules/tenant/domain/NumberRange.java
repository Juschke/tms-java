package com.translationagency.modules.tenant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "number_range", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "entity_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NumberRange {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // offer, order, invoice, vendor_invoice, customer, partner

    private String prefix;

    @Column(name = "current_number", nullable = false)
    private int currentNumber = 0;

    @Column(nullable = false)
    private int increment = 1;

    @Column(nullable = false)
    private int padding = 4;

    @Column(name = "year_based")
    private boolean yearBased = false;

    @Column(name = "reset_yearly")
    private boolean resetYearly = false;

    @Column(name = "last_year")
    private Integer lastYear;

    private String separator = "-";

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
