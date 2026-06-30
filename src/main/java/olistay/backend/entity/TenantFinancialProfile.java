package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A tenant's financial profile — input data only, matching
 * TenantProfileRequest in financial/profiler.py field-for-field. The
 * actual affordability computation (max_sustainable_rent, emergency_fund_target,
 * financial_health, etc.) is NOT stored here; it's computed on-demand by
 * calling FastAPI's /financial/profile endpoint with this data, since that
 * logic can evolve independently of the schema and shouldn't go stale in
 * a cached DB column.
 *
 * One-to-one with User: a tenant has exactly one financial profile, which
 * they can update at any time (e.g. after a raise, or before reassessing
 * their housing budget).
 *
 * Aggregate vs itemised override semantics mirror profiler.py exactly:
 *   - expenseBreakdown, when non-null, REPLACES fixedObligations entirely
 *   - availableFundsBreakdown, when non-null, REPLACES currentSavings entirely
 *   - additionalIncomeSources is summed on top of monthlyIncome, always
 * Spring does not pre-resolve these overrides — the raw data is forwarded
 * to FastAPI as-is, and FastAPI applies the same override logic it always
 * has (compute_financial_profile's Step 1).
 */
@Entity
@Table(name = "tenant_financial_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFinancialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── Obligatory ───────────────────────────────────────────────────────────
    @Column(nullable = false)
    private Double monthlyIncome;

    @Column(nullable = false)
    @Builder.Default
    private Double savingsGoal = 0.0;

    // ── Income detail (optional) ────────────────────────────────────────────
    @OneToMany(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IncomeSource> additionalIncomeSources = new ArrayList<>();

    /** stable | variable | seasonal | irregular */
    private String incomeStability;

    // ── Employment / job side (optional) ────────────────────────────────────
    /** formal_private | formal_public | informal_self_employed |
     *  informal_employee | business_owner | student | unemployed | retired */
    private String jobSector;

    private String employerName;

    private String jobTitle;

    // ── Geographic location (optional) ──────────────────────────────────────
    /** yaounde | douala | other */
    private String currentCity;

    private String currentNeighbourhood;

    private Double gpsLat;

    private Double gpsLon;

    // ── Household (optional, has defaults) ──────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Integer householdSize = 1;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasDependents = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer numDependents = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer numRoommates = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean sharesHousingCosts = false;

    // ── Expenses — aggregate OR itemised override ───────────────────────────
    /**
     * Aggregate figure, used only when expenseBreakdown is null —
     * mirrors profiler.py's override rule exactly.
     */
    @Column(nullable = false)
    @Builder.Default
    private Double fixedObligations = 0.0;

    /**
     * When non-null, this REPLACES fixedObligations in the FastAPI
     * computation. Null means "tenant provided only the aggregate figure".
     */
    @Embedded
    private ExpenseBreakdown expenseBreakdown;

    // ── Savings goal timeline & funds — aggregate OR itemised override ─────
    @Column(nullable = false)
    @Builder.Default
    private Integer goalTimelineMonths = 12;

    /**
     * Aggregate figure, used only when availableFundsBreakdown is null.
     */
    @Column(nullable = false)
    @Builder.Default
    private Double currentSavings = 0.0;

    /**
     * When non-null, this REPLACES currentSavings in the FastAPI computation.
     */
    @Embedded
    private AvailableFundsBreakdown availableFundsBreakdown;

    // ── Situational flag ─────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean hasFinancialEmergency = false;

    // ── Housing preferences (content_based.py + pipeline.py) ─────────────────
    // Not part of TenantProfileRequest in profiler.py — these only affect
    // content-based ranking and the recommendation pipeline, not the
    // affordability computation. Kept on this entity rather than a separate
    // table since it's just four booleans, naturally part of "what this
    // tenant needs in a home", queried together with the rest of their
    // profile on every scoring/recommend call.
    @Column(nullable = false)
    @Builder.Default
    private Boolean needsParking = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean needsSchoolNearby = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean needsHospitalNearby = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean needsGenerator = false;

    // ── Audit ────────────────────────────────────────────────────────────────
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
