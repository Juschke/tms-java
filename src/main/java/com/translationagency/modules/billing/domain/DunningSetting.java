package com.translationagency.modules.billing.domain;

import com.translationagency.modules.tenant.domain.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dunning_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DunningSetting {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    private boolean enabled = true;

    @Column(name = "fee_per_level", precision = 10, scale = 2)
    private BigDecimal feePerLevel = BigDecimal.valueOf(5.00);

    @Column(name = "days_overdue_level1")
    private int daysOverdueLevel1 = 3;

    @Column(name = "days_overdue_level2")
    private int daysOverdueLevel2 = 10;

    @Column(name = "days_overdue_level3")
    private int daysOverdueLevel3 = 20;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
