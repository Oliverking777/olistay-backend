package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import olistay.backend.enums.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A rental property listed on OLISTAY by a HOST.
 *
 * Field set is deliberately exhaustive — every field maps to at least one
 * ML engine input across rent_predictor.py, content_based.py,
 * hidden_costs.py, and occupancy_forecaster.py. Removing or renaming a
 * field here without updating the corresponding Python feature vector will
 * silently break ML predictions.
 *
 * Field groupings follow the ML feature column order in
 * ml_models/rent_predictor.py for easy cross-reference.
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Ownership ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // ── Listing meta ─────────────────────────────────────────────────────────
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.UNDER_REVIEW;

    // ── Location (rent_predictor: neighbourhood, city, gps_lat/lon, infra_zone)
    @Column(nullable = false)
    private String neighbourhood;

    @Column(nullable = false)
    private String city;

    private Double gpsLat;
    private Double gpsLon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InfraZone infraZone = InfraZone.III;

    // ── Property type (rent_predictor: property_type) ────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyType propertyType;

    /**
     * Unit type label used by occupancy_forecaster.py vacancy heuristics.
     * Values: chambre, T1, T2, T3, T4, T5
     * Stored as plain String since the forecaster reads it as a string key.
     */
    @Column(nullable = false)
    @Builder.Default
    private String unitType = "T2";

    // ── Dimensions (rent_predictor: length_m, width_m, area_m2) ─────────────
    private Double lengthM;
    private Double widthM;

    /**
     * Derived: lengthM × widthM. Computed and stored so the ML engine
     * doesn't have to re-derive it, and so area-based searches are indexable.
     */
    private Double areaM2;

    // ── Rooms (rent_predictor: num_bedrooms, num_bathrooms, floor_level, shared_wc)
    @Builder.Default
    private Integer numBedrooms = 0;

    @Builder.Default
    private Integer numBathrooms = 0;

    @Builder.Default
    private Integer floorLevel = 0;

    @Builder.Default
    private Boolean sharedWc = false;

    // ── Amenities — universal
    // (rent_predictor: has_parking, has_generator, has_water_meter,
    //  fiber_internet, security_gate)
    @Builder.Default
    private Boolean hasParking = false;

    @Builder.Default
    private Boolean hasGenerator = false;

    @Builder.Default
    private Boolean hasWaterMeter = true;

    @Builder.Default
    private Boolean fiberInternet = false;

    @Builder.Default
    private Boolean securityGate = false;

    /**
     * hidden_costs.py: gardien/watchman contribution charged to tenants.
     */
    @Builder.Default
    private Boolean hasGardien = false;

    // ── Amenities — commercial
    // (rent_predictor: road_frontage_m, shopfront_quality, loading_bay, standby_power_kva)
    @Builder.Default
    private Double roadFrontageM = 0.0;

    @Builder.Default
    private Integer shopfrontQuality = 0;   // 0–5

    @Builder.Default
    private Boolean loadingBay = false;

    @Builder.Default
    private Double standbyPowerKva = 0.0;

    // ── Proximity (rent_predictor + content_based)
    @Builder.Default
    private Boolean nearSchool = false;

    @Builder.Default
    private Boolean nearMarket = false;

    @Builder.Default
    private Boolean nearHospital = false;

    @Builder.Default
    private Boolean nearHighway = false;

    @Builder.Default
    private Boolean nearUniversity = false;

    // ── Quality / age / risk (rent_predictor)
    @Builder.Default
    private Integer structuralQuality = 5;  // 1–10

    @Builder.Default
    private Integer conditionScore = 5;      // 1–10

    private Integer buildYear;

    @Builder.Default
    private Boolean floodRisk = false;

    @Builder.Default
    private Integer noiseLevel = 5;          // 1–10

    // ── Legal / contractual
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TitleType titleType = TitleType.OCCUPATION;

    /**
     * Months of advance payment required by the landlord.
     * Cameroonian market norm: 3 months standard, up to 6 in prestige areas.
     * rent_predictor.py feature: advance_months
     * hidden_costs.py field: advance_months
     */
    @Builder.Default
    private Integer advanceMonths = 3;

    /**
     * Months of caution (security deposit) required.
     * Typically 1 month. Refundable — tracked separately from advance.
     * hidden_costs.py field: caution_months
     */
    @Builder.Default
    private Integer cautionMonths = 1;

    // ── Pricing ──────────────────────────────────────────────────────────────
    /**
     * Monthly rent in XAF (CFA Franc). Null until the HOST sets it; the
     * ML rent predictor can suggest a value before the HOST commits.
     */
    private Double rentXaf;

    // ── Scoring signals (content_based.py: landlord_reputation, lease_security)
    /**
     * Landlord reputation score 1–10, set by admin or derived from tenant
     * ratings over time. content_based.py stability composite uses this.
     */
    @Builder.Default
    private Integer landlordReputation = 5;

    /**
     * Lease security score 1–10 — reflects how standard/enforceable the
     * lease terms are. content_based.py stability composite uses this.
     */
    @Builder.Default
    private Integer leaseSecurity = 5;

    /**
     * Transport score 1–10 — ease of reaching the property by public
     * transport. Used in content_based.py transport_score_norm feature.
     * Can be auto-populated from occupancy_forecaster neighbourhood data.
     */
    @Builder.Default
    private Integer transportScore = 5;

    // ── Audit ────────────────────────────────────────────────────────────────
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Images attached to this listing. mappedBy="property" means PropertyImage
     * owns the foreign key — Property never writes to property_images.property_id
     * directly. orphanRemoval=true so removing an image from this list deletes
     * the row; cascade=ALL so saving/deleting a Property cascades to its images.
     */
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Auto-derive area_m2 if dimensions are provided
        if (this.lengthM != null && this.widthM != null) {
            this.areaM2 = this.lengthM * this.widthM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // Re-derive area_m2 if dimensions were updated
        if (this.lengthM != null && this.widthM != null) {
            this.areaM2 = this.lengthM * this.widthM;
        }
    }
}