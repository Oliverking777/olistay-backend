package olistay.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object matching ExpenseBreakdown in financial/profiler.py
 * exactly. When present on TenantFinancialProfile, this REPLACES the
 * aggregate fixedObligations figure entirely in the Python computation —
 * same override semantics as the Pydantic model.
 *
 * @Embeddable (not its own entity/table) because this has no independent
 * identity or queryable lifecycle separate from its owning
 * TenantFinancialProfile — it's a value object, one per profile, never
 * shared or referenced independently.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseBreakdown {

    @Column(name = "expense_housing_utilities")
    @Builder.Default
    private Double housingUtilities = 0.0;

    @Column(name = "expense_food_household_supplies")
    @Builder.Default
    private Double foodHouseholdSupplies = 0.0;

    @Column(name = "expense_transportation")
    @Builder.Default
    private Double transportation = 0.0;

    @Column(name = "expense_personal_health_insurance")
    @Builder.Default
    private Double personalHealthInsurance = 0.0;

    @Column(name = "expense_debt_repayments")
    @Builder.Default
    private Double debtRepayments = 0.0;

    @Column(name = "expense_dependents_support")
    @Builder.Default
    private Double dependentsSupport = 0.0;

    @Column(name = "expense_other")
    @Builder.Default
    private Double other = 0.0;

    /**
     * Mirrors ExpenseBreakdown.total() in profiler.py — used by Spring-side
     * validation/display only; the authoritative computation still happens
     * in FastAPI when this breakdown is forwarded as JSON.
     */
    public double total() {
        return housingUtilities + foodHouseholdSupplies + transportation
                + personalHealthInsurance + debtRepayments + dependentsSupport + other;
    }
}