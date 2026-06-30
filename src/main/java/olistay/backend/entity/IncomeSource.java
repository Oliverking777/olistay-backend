package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One additional income line for a tenant, on top of their main
 * TenantFinancialProfile.monthlyIncome. Matches IncomeSource in
 * financial/profiler.py exactly.
 *
 * True @Entity (not @Embeddable) because TenantFinancialProfile holds a
 * LIST of these — each needs its own identity to be added/removed/queried
 * independently, unlike the single ExpenseBreakdown/AvailableFundsBreakdown
 * value objects.
 */
@Entity
@Table(name = "income_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_financial_profile_id", nullable = false)
    private TenantFinancialProfile financialProfile;

    /**
     * e.g. side_business, family_support, rental_income, freelance,
     * pension, scholarship, other — free-form string matching Python's
     * untyped str field (not an enum, since profiler.py treats this as
     * descriptive text rather than a constrained set).
     */
    @Column(nullable = false)
    private String incomeType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double monthlyAmount;
}