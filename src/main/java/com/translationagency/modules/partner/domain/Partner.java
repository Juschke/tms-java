package com.translationagency.modules.partner.domain;

import com.translationagency.modules.tenant.domain.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Partner {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    // Extended Personendaten
    private String salutation;
    private String title;
    
    @Column(name = "display_name")
    private String displayName;

    // Extended Organisation
    private String organization;
    
    @Column(name = "organization_unit")
    private String organizationUnit;

    // Typen & Status Checkboxen
    @Column(name = "is_translator")
    private boolean isTranslator = true;

    @Column(name = "is_interpreter")
    private boolean isInterpreter = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_recommended")
    private boolean isRecommended = false;

    private String classification = "extern";

    // Anschrift & Bemerkungen
    private String street;
    private String zip;
    private String city;
    private String country;
    
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Skills / Sprachprofil
    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartnerLanguagePair> languagePairs = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

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

    public String getFullName() {
        if (companyName != null && !companyName.isBlank()) {
            return companyName + " (" + ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim() + ")";
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}
